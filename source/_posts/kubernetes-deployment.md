---
title: 容器集群、网络连接、自动化部署
date: 2017-5-12 20:46:25
categories:
  - 分布式&云计算
  - Kubernetes
tags:
  - 分布式
  - Kubernetes
  - container
  - 容器
  - PaaS
---

## 容器与编排
毋庸置疑,容器是未来的最为重要的配置编排格式之一, 打包应用程序也将会变得更加容易。虽然像Docker这样的工具提供真实的容器，但是也需要其他工具来处理如replication，failover以及API来自动化部署到多个机器。

## 用Kubernetes来进行负载均衡
Service在部署之前存在一个IP地址，但是这个地址只存在于Kubernetes集群之内。这也就意味着service对于网络来说根本不可用！当运行在谷歌GCE上的时候（像我们一样），Kubernetes能够自动配置一个负载均衡器来访问应用程序。如果你不是在谷歌GCE上面的话，你就需要做些额外的工作来使负载均衡运行起来。

将service直接暴露到一个主机端口也可以，这就是经常使用的方式，但这会令很多Kubernetes的优势无法充分发挥。如果依赖主机上的端口，那么当部署多个应用程序的时候，就会陷入端口冲突,这也使得调度集群或者替代主机变得更加困难。

参见--> [Kubernetes Ingress](/kubernetes-ingress/)

![ingress-nginx](/images/ingress-nginx.png)
