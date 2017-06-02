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

![Istio Arch](/images/istio-arch.png)

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
![](/images/TrafficManagementOverview-2.png)

![](/images/ManagerAdapters.png)

### Istio-Auth
提供服务间以及用户之间的认证,确保不需要修改服务code的前提下增强服务之间的安全性. 主要包括以下3个组件:
- 身份识别
  - 当Istio运行在Kubernetes时,Auth会使用Kubernetes提供的服务账号来识别运行服务的主体是谁.
- key管理
  - Auth提供了一个CA自动化生成和管理key和证书.
- 通讯安全
  - 服务间的通讯通过Envoy在客户端和服务端提供tunnel来保证服务调用的安全.

![](/images/Istio_Auth.png)


## 分布式跟踪
Istio的分布式跟踪是基于Twitter开源的[zipkin](zipkin/)分布式跟踪系统,理论模型来自于Google Dapper 论文.
### 启动zipkin
安装Istio时会启动zipkin addon,当然也可以使用如下命令启动:
```
kubectl apply -f install/kubernetes/addons/zipkin.yaml
```
### 访问zipkin
访问zipkin dashboard: http://localhost:9411
```
kubectl port-forward $(kubectl get pod -l app=zipkin -o jsonpath='{.items[0].metadata.name}') 9411:9411
```

### 在服务中enable trace
服务本身实现需要做一定的改动,即从最初始的HTTP请求中获取以下header并传递给其他的请求:
```
x-request-id
x-b3-traceid
x-b3-spanid
x-b3-parentspanid
x-b3-sampled
x-b3-flags
x-ot-span-context
```

## 启用Ingress
在Kubernetes环境下, Istio使用了内置的Ingress来暴露服务,目前支持HTTP和HTTPS两种方式. 具体的Ingress,参见[Kubernetes Ingress](/kubernetes-ingress/).

### 配置HTTP服务

```
cat <<EOF | kubectl create -f -
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: simple-ingress
  annotations:
    kubernetes.io/ingress.class: istio
spec:
  rules:
  - http:
      paths:
      - path: /headers
        backend:
          serviceName: httpbin
          servicePort: 8000
      - path: /delay/.*
        backend:
          serviceName: httpbin
          servicePort: 8000
EOF
```

### 配置HTTPS服务
```
cat <<EOF | kubectl create -f -
apiVersion: extensions/v1beta1
kind: Ingress
metadata:
  name: secured-ingress
  annotations:
    kubernetes.io/ingress.class: istio
spec:
  tls:
    - secretName: ingress-secret
  rules:
  - http:
      paths:
      - path: /ip
        backend:
          serviceName: httpbin
          servicePort: 8000
EOF
```

## 启用Egress
默认情况下,Istio中的服务是无法访问cluster之外的服务的,这是因为pod中的iptable设置为所有的对外请求都指向sidecar proxy. 而为了能访问外部服务, Istio提供了两种方式来解决这个问题.

### 配置外部服务
注册一个HTTP和HTTPS服务, 如下:
```
cat <<EOF | kubectl create -f -
apiVersion: v1
kind: Service
metadata:
 name: externalbin
spec:
 type: ExternalName
 externalName: httpbin.org
 ports:
 - port: 80
   # important to set protocol name
   name: http
EOF
```
或者
```
cat <<EOF | kubectl create -f -
apiVersion: v1
kind: Service
metadata:
 name: securegoogle
spec:
 type: ExternalName
 externalName: www.google.com
 ports:
 - port: 443
   # important to set protocol name
   name: https
EOF
```
其中, metadata.name 就是内部服务所需要访问的外部服务的名称, spec.externalName则是外部服务的DNS名称.

执行如下命令可以尝试访问外部服务,
```
export SOURCE_POD=$(kubectl get pod -l app=sleep -o jsonpath={.items..metadata.name})
kubectl exec -it $SOURCE_POD -c sleep bash
curl http://externalbin/headers
curl http://securegoogle:443
```
### 直接访问外部服务
Istio Egress目前只支持访问HTTP/HTTPS请求,而为了支持其他协议请求(如mqtt, mongo等), 就需要配置服务的Envoy sidecar来避免截取外部请求.
最简单的方式是使用参数--includeIPRanges来指定内部cluster服务所使用的IP范围.
> Note: 不同的cloud provider所支持的IP范围和获取方式不尽相同.

例如, minikube支持如下:
```
kubectl apply -f <(istioctl kube-inject -f samples/apps/sleep/sleep.yaml --includeIPRanges=10.0.0.1/24)
```


## 配置请求路由
默认配置下,Istio会将所有的请求路由到同一个服务的所有版本上.此外,Istio提供了根据请求内容的路由规则,如下规则描述了所有的请求都会指向服务的版本v1:
```
type: route-rule
name: ratings-default
namespace: default
spec:
  destination: ratings.default.svc.cluster.local
  precedence: 1
  route:
  - tags:
      version: v1
    weight: 100
---
type: route-rule
name: reviews-default
namespace: default
spec:
  destination: reviews.default.svc.cluster.local
  precedence: 1
  route:
  - tags:
      version: v1
    weight: 100
---
type: route-rule
name: details-default
namespace: default
spec:
  destination: details.default.svc.cluster.local
  precedence: 1
  route:
  - tags:
      version: v1
    weight: 100
---
type: route-rule
name: productpage-default
namespace: default
spec:
  destination: productpage.default.svc.cluster.local
  precedence: 1
  route:
  - tags:
      version: v1
    weight: 100
---
```

如果需要将某些请求指向其他版本的服务,如根据请求的cookie进行路由:
```
destination: reviews.default.svc.cluster.local
match:
  httpHeaders:
    cookie:
      regex: ^(.*?;)?(user=jason)(;.*)?$
precedence: 2
route:
- tags:
    version: v2
```

其他具体的规则,参见: https://istio.io/docs/reference/config/traffic-rules/routing-rules.html#routerule

## 错误注入
Istio提供2种错误类型可以注入到请求中:
- delays: 属于时序故障,模拟网络延迟或者上游服务负载过重等;
- aborts: 属于崩溃故障,模拟上游服务出现故障;一般返回HTTP Error Code或者TCP连接错误代码;

故障注入规则描述如下例子所述:

```
destination: ratings.default.svc.cluster.local
httpFault:
  delay:
    fixedDelay: 7s
    percent: 100
match:
  httpHeaders:
    cookie:
      regex: "^(.*?;)?(user=jason)(;.*)?$"
precedence: 2
route:
 - tags:
    version: v1
```

## 设置请求超时
HTTP请求超时可以通过在路由规则中设置字段httpReqTimeout实现.
具体例子如下:
```
cat <<EOF | istioctl replace
type: route-rule
name: reviews-default
spec:
  destination: reviews.default.svc.cluster.local
  route:
  - tags:
      version: v2
  httpReqTimeout:
    simpleTimeout:
      timeout: 1s
EOF
```
## 限流
在Istio的mixer中配置限流规则,如下ratelimit.yaml:
```
rules:
- selector: source.labels["app"]=="reviews" && source.labels["version"] == "v3"  
- aspects:
  - kind: quotas
    params:
      quotas:
      - descriptorName: RequestCount
        maxAmount: 5000
        expiration: 5s
        labels:
          label1: target.service
```

如果target.service=rating, 那么计数器的key则为:
```
$aspect_id;RequestCount;maxAmount=5000;expiration=5s;label1=ratings
```

执行如下命令可以使得rating服务的请求控制在每5秒5000次(限定在reviews v3服务在调用时生效):
```
istioctl mixer rule create global ratings.default.svc.cluster.local -f ratelimit.yaml
```

## 简单的访问控制
Istio可以通过设置规则实现简单的访问控制.
### 使用denials属性
例如:
```
rules:
- aspects:
  - kind: denials
  selector: source.labels["app"]=="reviews" && source.labels["version"] == "v3"
```

执行如下命令可以使rating服务拒绝来自reviews v3服务的任何请求.
```
istioctl mixer rule create global ratings.default.svc.cluster.local -f samples/apps/bookinfo/mixer-rule-ratings-denial.yaml
```  
### 使用黑白名单
使用黑白名单之前需要先定义一个adapter,如下:
```
- name: versionList
  impl: genericListChecker
  params:
    listEntries: ["v1", "v2"]
```

启用白名单时blacklist设置为false,反之为true.
```
rules:
  aspects:
  - kind: lists
    adapter: versionList
    params:
      blacklist: false
      checkExpression: source.labels["version"]
```
