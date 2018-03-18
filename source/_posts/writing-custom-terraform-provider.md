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
实现一个Terraform provider,首先需要做的就是实现一个 [terraform.ResourceProvider]（https://godoc.org/github.com/hashicorp/terraform/helper/schema#Provider）

```
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
```
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
