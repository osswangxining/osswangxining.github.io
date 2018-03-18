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
