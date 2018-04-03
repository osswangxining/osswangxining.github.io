---
title: Kubernetes Ingress
date: 2017-3-9 20:46:25
categories:
  - 分布式&云计算
  - Kubernetes
tags:
  - 分布式
  - Kubernetes
  - 路由
  - 容器
  - Ingress
---

## 相关术语
- 节点：Kubernetes集群中的一台物理机或者虚拟机。
- 集群：位于Internet防火墙后的节点，这是kubernetes管理的主要计算资源。
- 边界路由器：为集群强制执行防火墙策略的路由器。 这可能是由云提供商或物理硬件管理的网关。
- 集群网络：一组逻辑或物理链接，可根据Kubernetes 网络模型 实现群集内的通信。 集群网络的实现包括Overlay模型的 flannel 和基于SDN的 OVS 。
- 服务：使用标签选择器标识一组pod成为的Kubernetes 服务 。 除非另有说明，否则服务假定在集群网络内仅可通过虚拟IP访问。

毋庸置疑,容器是未来的最为重要的配置编排格式之一, 打包应用程序也将会变得更加容易。虽然像Docker这样的工具提供真实的容器，但是也需要其他工具来处理如replication，failover以及API来自动化部署到多个机器。

Service在部署之前存在一个IP地址，但是这个地址只存在于Kubernetes集群之内。这也就意味着service对于网络来说根本不可用！当运行在谷歌GCE上的时候（像我们一样），Kubernetes能够自动配置一个负载均衡器来访问应用程序。如果你不是在谷歌GCE上面的话，你就需要做些额外的工作来使负载均衡运行起来。

将service直接暴露到一个主机端口也可以，这就是经常使用的方式，但这会令很多Kubernetes的优势无法充分发挥。如果依赖主机上的端口，那么当部署多个应用程序的时候，就会陷入端口冲突,这也使得调度集群或者替代主机变得更加困难。

<!-- more -->
## 什么是Ingress
通常情况下，service和pod仅可在集群内部网络中通过IP地址访问。所有到达边界路由器的流量或被丢弃或被转发到其他地方。从概念上讲，可能像下面这样：
```
    internet
        |
  ------------
  [ Services ]
```
Ingress是授权入站连接到达集群服务的规则集合。

```
    internet
        |
   [ Ingress ]
   --|-----|--
   [ Services ]
```
可以给Ingress配置提供外部可访问的URL、负载均衡、SSL、基于名称的虚拟主机等。用户通过POST Ingress资源到API server的方式来请求ingress。 Ingress controller 负责实现Ingress，通常使用负载平衡器，它还可以配置边界路由和其他前端，这有助于以HA方式处理流量。

## 自定义Secrets
最简化的Ingress配置：

```
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: test-ingress
spec:
  rules:
  - http:
      paths:
      - path: /testpath
        backend:
          serviceName: test
          servicePort: 80
```
1-4行：跟Kubernetes的其他配置一样，ingress的配置也需要 apiVersion ， kind 和 metadata 字段。配置文件的详细说明请查看 部署应用 , 配置容器 和 使用resources .

5-7行: Ingress spec 中包含配置一个loadbalancer或proxy server的所有信息。最重要的是，它包含了一个匹配所有入站请求的规则列表。目前ingress只支持http规则。

8-9行：每条http规则包含以下信息：一个 host 配置项（比如for.bar.com，在这个例子中默认是*）， path 列表（比如：/testpath），每个path都关联一个 backend (比如test:80)。在loadbalancer将流量转发到backend之前，所有的入站请求都要先匹配host和path。

10-12行：正如 services doc 中描述的那样，backend是一个 service:port 的组合。Ingress的流量被转发到它所匹配的backend。

## Ingress类型
### 单Service Ingress
Kubernetes中已经存在一些概念可以暴露单个service（查看 替代方案 ），但是你仍然可以通过Ingress来实现，通过指定一个没有rule的默认backend的方式。

ingress.yaml定义文件：
```
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: test-ingress
spec:
  backend:
    serviceName: testsvc
    servicePort: 80
```

使用 kubectl create -f 命令创建，然后查看ingress：
```
$ kubectl get ing
NAME                RULE          BACKEND        ADDRESS
test-ingress        -             testsvc:80     107.178.254.228
```

### 简单展开
如前面描述的那样，kubernete pod中的IP只在集群网络内部可见，我们需要在边界设置一个东西，让它能够接收ingress的流量并将它们转发到正确的端点上。这个东西一般是高可用的loadbalancer。使用Ingress能够允许你将loadbalancer的个数降低到最少，例如，嫁入你想要创建这样的一个设置：
```
foo.bar.com -> 178.91.123.132 -> / foo    s1:80
                                 / bar    s2:80
```
需要一个这样的ingress：
```
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: test
spec:
  rules:
  - host: foo.bar.com
    http:
      paths:
      - path: /foo
        backend:
          serviceName: s1
          servicePort: 80
      - path: /bar
        backend:
          serviceName: s2
          servicePort: 80
```

使用 kubectl create -f 创建完ingress后：
```
$ kubectl get ing
NAME      RULE          BACKEND   ADDRESS
test      -
          foo.bar.com
          /foo          s1:80
          /bar          s2:80
```

### 基于名称的虚拟主机
Name-based的虚拟主机在同一个IP地址下拥有多个主机名。
```
foo.bar.com --|                 |-> foo.bar.com s1:80
              | 178.91.123.132  |
bar.foo.com --|                 |-> bar.foo.com s2:80
```
下面这个ingress说明基于 Host header 的后端loadbalancer的路由请求：
```
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: test
spec:
  rules:
  - host: foo.bar.com
    http:
      paths:
      - backend:
          serviceName: s1
          servicePort: 80
  - host: bar.foo.com
    http:
      paths:
      - backend:
          serviceName: s2
          servicePort: 80
```
## 使用Nginx设置负载均衡
在任意情况下，当创建新的Kubernetes services的时候，需要一个机制来动态地重新部署负载均衡器.

通过使用confd来检测配置在etcd内的修改，并基于一个模版生成一个新的nginx配置文件.

![ingress-nginx](/images/ingress-nginx.png)

## 替代方案
可以通过很多种方式暴露service而不必直接使用ingress：
- 使用 Service.Type=LoadBalancer
- 使用 Service.Type=NodePort
- 使用 Port Proxy
- 部署一个 Service loadbalancer 这允许你在多个service之间共享单个IP，并通过Service Annotations实现更高级的负载平衡。
