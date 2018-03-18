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
