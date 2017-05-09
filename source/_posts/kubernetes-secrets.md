---
title: Kubernetes Secrets
categories:
  - 分布式&云计算
tags:
  - 分布式
  - Kubernetes
  - 配置
  - 容器
  - Secrets
---

## Secrets描述
在Kubernetes中，Secret对象类型主要目的是保存一些私密数据，比如密码, tokens, ssh keys等信息。将这些信息放在Secret对象中比直接放在pod或docker image中更安全，也更方便使用。

创建Secrets对象的方式有两种，一种是用户手动创建，另一种是集群自动创建。
一个已经创建好的Secrets对象有两种方式被pod对象使用，其一，在container中的volume对象里以file的形式被使用，其二，在pull images时被kubelet使用。

为了使用Secret对象，pod必须引用这个Secret，同样可以手动或者自动来执行引用操作。

## Built-in Secrets
Kubernetes会自动创建包含证书信息的Secret，并且使用它来访问api, Kubernetes也将自动修改pod来使用这个Secret。

自动创建的Secret以及所使用的api证书,可以根据需要disable或者override。如果仅仅需要安全访问apiserver，那么上述的流程是推荐的方式。

## 自定义Secrets
