---
title: 巧用Envoy+Docker打造轻量级Sidecar代理
date: 2018-2-4 16:36:25
categories:
  - 分布式&云计算
  - 微服务
tags:
  - 微服务
  - Kubernetes
  - container
  - 容器
  - 代理
---


## Envoy
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

## Envoy Front Proxy示例
### (可选)安装Docker环境
确保docker, docker-compose以及docker-machine可用，建议使用最新的稳定版本。
  > 最便捷的安装方式可以参考 https://www.docker.com/products/docker-toolbox

### 创建Docker Machine
通过使用命令 *docker-machine* 创建用于运行docker容器的环境：
```
$ docker-machine create --driver virtualbox default
$ eval $(docker-machine env default)
```

### 克隆Envoy代码库
Envoy代码库中包含了我们所需的例子源码，直接通过git命令可以克隆其代码库：
```
git clone https://github.com/envoyproxy/envoy.git
```

### 使用docker-compose启动容器
切换到Envoy安装根目录下的examples/front-proxy，可以看到里面包含了所需的主docker-compose.yml、Envoy配置文件以及相应的Dockerfile文件、脚本文件等。

![Envoy Front Proxy Arch](/images/envoy-front-proxy-topology.png)

```
$ pwd
/Users/xiningwang/localgit/envoy/examples/front-proxy
$ docker-compose up --build -d
$ docker-compose ps
Name                        Command               State                      Ports
----------------------------------------------------------------------------------------------------------------
frontproxy_front-envoy_1   /bin/sh -c /usr/local/bin/ ...   Up      0.0.0.0:8000->80/tcp, 0.0.0.0:8001->8001/tcp
frontproxy_service1_1      /bin/sh -c /usr/local/bin/ ...   Up      80/tcp
frontproxy_service2_1      /bin/sh -c /usr/local/bin/ ...   Up      80/tcp
```
