---
title: Istio
date: 2017-5-27 20:46:25
categories:
  - 分布式&云计算
  - 微服务
tags:
  - 微服务
  - Kubernetes
  - container
  - 容器
  - PaaS
---

## 概述
Istio是一个用于连接/管理以及安全化微服务的开放平台, 提供了一种简单的方式用于创建微服务网络,并提供负载均衡/服务间认证/监控等能力,关键的是并不需要修改服务本身. 主要提供以下功能:
- Traffic Management: 控制服务之间调用的流量和API调用;
- Observability: 获取服务之间的依赖,以及服务调用的流量走向;
- Policy Enforcement: 控制服务的访问策略,不需要改动服务本身;
- Service Identity and Security: 服务身份与安全相关的功能;

## 架构
Istio从架构上看,主要分为2个部分,即:
- 控制面板: 管理代理,用于支持流量路由/运行时执行策略等;
- 数据面板: 由一系列的智能代理(Envoy)构成,用于仲裁和控制服务之间的网络交互;

![Istio Arch](images/istio-arch.png)

### Envoy
Built-in features:
- dynamic service discovery,
- load balancing,
- TLS termination,
- HTTP/2 & gRPC proxying,
- circuit breakers,
- health checks,
- staged rollouts with %-based traffic split,
- fault injection,
- rich metrics

Envoy将作为一个独立的sidecar与相关微服务部署在同一个Kubernetes的pod上,并提供一系列的属性给Mixer.Mixer以此作为依据执行策略,并发送到监控系统.

这种sidecar代理模型不需要改变任何服务本身的逻辑,并能增加一系列的功能.

### Mixer
Mixer负责执行访问控制与使用的策略,并从Envoy收集数据.提供了一个plugin,可以扩展支持多种host环境和backend.

### Istio-Manager
用户与Istio之间的接口, 收集并验证配置信息并发送给其他组件.作为流量管理的核心附件, Istio-Manager管理所有配置的Envoy代理实例,并提供如下流量管理方式:
![](images/TrafficManagementOverview-2.png)

![](images/ManagerAdapters.png)

### Istio-Auth
提供服务间以及用户之间的认证,确保不需要修改服务code的前提下增强服务之间的安全性. 主要包括以下3个组件:
- 身份识别
  - 当Istio运行在Kubernetes时,Auth会使用Kubernetes提供的服务账号来识别运行服务的主体是谁.
- key管理
  - Auth提供了一个CA自动化生成和管理key和证书.
- 通讯安全
  - 服务间的通讯通过Envoy在客户端和服务端提供tunnel来保证服务调用的安全.

![](images/Istio_Auth.png)
