---
title: 写一个Terraform Provider
date: 2018-3-17 20:46:25
categories:
  - 分布式&云计算
tags:
  - Kubernetes
  - container
  - 容器
  - Terraform
---

![](../images/devops-3148393_1920.png)
图片来自https://pixabay.com

## Terraform

Terraform 是一个 IT 基础架构自动化编排工具，它的口号是 "Write, Plan, and create Infrastructure as Code", 基础架构即代码。具体的说就是可以用代码来管理维护 IT 资源，并且在真正运行之前可以看到执行计划(即dryrun)。由于状态保存到文件中，因此能够离线方式查看资源情况 -- 当然，前提是不要在 Terraform 之外对资源进行修改。

Terraform 是一个高度可扩展的工具，通过 Provider 来支持新的基础架构，譬如本文中介绍的用于支持Kafka topic管理的provider。

## Writing a Terraform provider
怎么编写一个定制的Terraform provider, 官方网站给出了一定的开发指导，但略感简单。本文试图从零开始整理一遍整个开发的过程以及涉及的一些关键点。

一个Terraform provider最基本的两个要素就是provider本身以及所涉及到的resource。

```hcl
provider "kafka" {
  bootstrap_servers = ["localhost:9092"]
}

resource "kafka_topic" "my_test_topic" {
  name               = "my_test_topic"
  replication_factor = 1
  partitions         = 1

  config = {
    "segment.ms"   = "4000"
    "retention.ms" = "86400000"
  }
}
```

## Project Structure
目前官方内建了60多个Provider，你可以从中看到一个基本的Terraform provider大致会有以下几个部分组成：
- provider.go : Implement the “core” of the Provider.
- config.go : Configure the API client with the credentials from the Provider.
- resource_<resource_name>.go : Implement a specific resource handler with the CRUD functions.
- import_<resource_name>.go : Make possible to import existing resources.
- data_source_<resource_name>.go : Used to fetch data from outside of Terraform to be used in other resources.

当然，这些部分也不是必须的，例如import_<resource_name>.go，data_source_<resource_name>.go等在本文的例子中就没有用到。具体还是取决于你要实现的Provider逻辑本身。

## The provider
实现一个Terraform provider,首先需要做的就是实现一个 [terraform.ResourceProvider](https://godoc.org/github.com/hashicorp/terraform/helper/schema#Provider)

```go
// File : provider.go
package kafka

import (
  "github.com/hashicorp/terraform/helper/schema"
  "github.com/hashicorp/terraform/terraform"
)

func Provider() terraform.ResourceProvider {
  return &schema.Provider{
    Schema: map[string]*schema.Schema{ },
    ResourcesMap: map[string]*schema.Resource{ },
    ConfigureFunc: providerConfigure,
  }
}

func providerConfigure(d *schema.ResourceData) (interface{}, error) {
  return nil, nil
}
```

如上述代码所示，函数Provider()会返回一个带有必要configuration的terraform.ResourceProvider：
- Schema: provider所需的参数列表，map类型；
- ResourcesMap：provider所管理的resource；
- ConfigureFunc：提供了实例化、配置客户端API调用的函数；

相应地，我们提供了一个provider测试的如下代码：

```go
// File : provider_test.go
package kafka

import (
	"testing"

	"github.com/hashicorp/terraform/helper/schema"
	"github.com/hashicorp/terraform/terraform"
)

var testAccProvider *schema.Provider

func init() {
	testAccProvider = Provider().(*schema.Provider)
}

func TestProvider(t *testing.T) {
	if err := Provider().(*schema.Provider).InternalValidate(); err != nil {
		t.Fatalf("err: %s", err)
	}
}

func TestProvider_impl(t *testing.T) {
	var _ terraform.ResourceProvider = Provider()
}

func testAccPreCheck(t *testing.T) {
	// We will use this function later on to make sure our test environment is valid.
	// For example, you can make sure here that some environment variables are set.
}

```

运行一下看看，是不是如下的结果。

```
xis-macbook-pro:kafka xiningwang$ go test -v
=== RUN   TestProvider
--- PASS: TestProvider (0.00s)
=== RUN   TestProvider_impl
--- PASS: TestProvider_impl (0.00s)
PASS
ok      github.com/terraform-providers/terraform-provider-kafka/kafka   0.020s
```

### Schema
我们的例子中Schema定义了2个变量，分别是TypeList类型的bootstrap_servers和TypeInt类型的timeout。
其中，前者是required,后者则是optional.

```go
Schema: map[string]*schema.Schema{
	"bootstrap_servers": &schema.Schema{
		Type:        schema.TypeList,
		Elem:        &schema.Schema{Type: schema.TypeString},
		Required:    true,
		Description: "The list of kafka brokers",
	},
	"timeout": &schema.Schema{
		Type:        schema.TypeInt,
		Required:    false,
		Optional:    true,
		Default:     90,
		Description: "Timeout in seconds",
	},
},
```

### providerConfigure
接下来，就是获取Schema中定义的变量值，创建管理resource的Client API.

```go
func providerConfigure(d *schema.ResourceData) (interface{}, error) {
	var brokers *[]string

	if brokersRaw, ok := d.GetOk("bootstrap_servers"); ok {
		brokerI := brokersRaw.([]interface{})
		log.Printf("[DEBUG] configuring provider with Brokers of size %d", len(brokerI))
		b := make([]string, len(brokerI))
		for i, v := range brokerI {
			b[i] = v.(string)
		}
		log.Printf("[DEBUG] b of size %d", len(b))
		brokers = &b
	} else {
		log.Printf("[ERROR] something wrong? %v , ", d.Get("timeout"))
		return nil, fmt.Errorf("brokers was not set")
	}

	log.Printf("[DEBUG] configuring provider with Brokers @ %v", brokers)
	timeout := d.Get("timeout").(int)

	config := &Config{
		BootstrapServers: brokers,
		Timeout:          timeout,
	}

	log.Printf("[DEBUG] Config @ %v", config)

	return NewClient(config)
}
```

## Resource
当然，Terraform Provider最精彩的部分其实是resource的CRUD逻辑，也就是这个部分要讲述的内容。

```go
package kafka

import (
	"fmt"
	"log"
	"time"

	"github.com/hashicorp/terraform/helper/resource"
	"github.com/hashicorp/terraform/helper/schema"
)

func resourceKafkaTopic() *schema.Resource {
	return &schema.Resource{
		Create: topicCreate,
		Read:   topicRead,
		Update: topicUpdate,
		Delete: topicDelete,
		Importer: &schema.ResourceImporter{
			State: schema.ImportStatePassthrough,
		},
		CustomizeDiff: customPartitionDiff,
		Schema: map[string]*schema.Schema{
			"name": {
				Type:        schema.TypeString,
				Required:    true,
				ForceNew:    true,
				Description: "The name of the topic",
			},
			"partitions": {
				Type:        schema.TypeInt,
				Required:    true,
				Description: "number of partitions",
			},
			"replication_factor": {
				Type:        schema.TypeInt,
				Required:    true,
				ForceNew:    true,
				Description: "number of replicas",
			},
			"config": {
				Type:        schema.TypeMap,
				Optional:    true,
				ForceNew:    false,
				Description: "A map of string k/v attributes",
			},
		},
	}
}
```

其中，
- 里面最关键的逻辑就是CRUD部分，它会调用Kafka Go SDK API来管理topic；
- Schema部分使用逻辑跟上述provider中使用Schema的方式是一样的，里面定义的变量是跟你最终定义terraform tf文件的结果保持一致的。例如，我们例子中的sample文件：

```hcl
resource "kafka_topic" "my_test_topic" {
  name               = "my_test_topic"
  replication_factor = 1
  partitions         = 1

  config = {
    "segment.ms"   = "4000"
    "retention.ms" = "86400000"
  }
}
```

## 编译与运行
创建main.go,如下:

```go
package main

import (
	"github.com/hashicorp/terraform/plugin"
	"github.com/terraform-providers/terraform-provider-kafka/kafka"
)

func main() {
	plugin.Serve(&plugin.ServeOpts{
		ProviderFunc: kafka.Provider})
}
```

编译：

```
go build -o terraform-provider-kafka
```


## 源代码 & TODO
到这儿你可能已经明白了如何搭建一个定制的Terraform Provider, 我想你也一定认为一个provider的复杂逻辑其实是取决于你要管理的resource的CRUD的逻辑。
本文中的resource是指kafka中的topic，逻辑不是很复杂，但一点我们没有提及的是security - 也就是说，如果kafka enable了ssl，那么管理topic的client API也需要相应地加上credential信息。

最后，完整的源代码@ https://github.com/osswangxining/terraform-provider-kafka
