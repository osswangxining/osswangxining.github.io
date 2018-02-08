---
title: Istio
date: 2018-1-28 20:46:25
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
基于之前的[版本](/istio)更新.

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
Mixer负责在服务网格上执行访问控制和使用策略，并从Envoy代理和其他服务收集遥测数据。代理提取请求级属性，发送到Mixer进行评估。Mixer包括一个灵活的插件模型，使其能够接入到各种主机环境和基础设施后端，从这些细节中抽象出Envoy代理和Istio管理的服务。

后端的基础设施常常被设计用于提供建立服务支持的功能，包括访问控制系统、遥测数据捕获系统、配额执行系统以及计费系统等。传统服务会直接和这些后端系统打交道，和后端紧密耦合，并集成其中的个性化语义以及用法。

Mixer在应用程序代码和基础架构后端之间提供通用中介层。它的设计将策略执行部分移出应用层，而是用运维人员能够控制的配置取而代之。应用程序代码不再将应用程序代码与特定后端集成在一起，而是与Mixer进行相当简单的集成，然后Mixer负责与后端系统连接。

Mixer的设计目的是改变层次之间的边界，以此来降低总体的复杂性。从服务代码中剔除策略逻辑，改由运维人员进行控制。
#### Mixer架构
![Mixer](/images/istio-mixer-architecture.png)

Mixer 提供三个核心功能：
- **前提条件检查** 允许服务在响应来自服务消费者的传入请求之前验证一些前提条件。前提条件可以包括服务使用者是否被正确认证，是否在服务的白名单上，是否通过ACL检查等等。
- **配额管理**  使服务能够在分配和释放多个维度上的配额，配额这一简单的资源管理工具可以在服务消费者对有限资源发生争用时，提供相对公平的竞争手段。限流控制就是配额的一个实例。
- **遥测报告** 使服务能够上报日志和监控，以及还将启用针对服务生产者以及服务消费者的跟踪和计费流。

这些机制的应用是基于一组 **属性** 的，每个请求都会将这些属性呈现给Mixer。在Istio中，这些属性来自于Sidecar代理（Envoy）的每一次请求。
  > Istio使用 属性 来控制在服务网格中运行的服务的运行时行为。属性是具有名称和类型的元数据片段，用以描述入口和出口流量，以及这些流量所属的环境。Istio属性携带特定信息片段，例如API请求的错误代码，API请求的延迟或TCP连接的原始IP地址. 例如：

  ```
  request.path: xyz/abc
  request.size: 234
  request.time: 12:34:56.789 04/17/2017
  source.ip: 192.168.0.1
  target.service: example
  ```

#### 基于适配器与模板的配置
  ![mixer architecture](/images/mixer%20architecture.svg)

Mixer是一种属性处理引擎，请求到达Mixer时带有一组属性 ，并且基于这些属性，Mixer会生成对各种基础设施后端的调用。该属性集确定Mixer为给定的请求用哪些参数调用哪个后端。为了隐藏各个后端的细节，Mixer使用称为适配器的模块。

Mixer的配置有几个中心职责：
- 描述哪些适配器正在使用以及它们的操作方式。
- 描述如何将请求属性映射到适配器参数中。
- 描述使用特定参数调用适配器的时机。

配置基于适配器和模板来完成：
- **适配器** 封装了Mixer和特定基础设施后端之间的接口。
- **模板** 定义了从特定请求的属性到适配器输入的映射关系。一个适配器可以支持任意数量的模板。

配置使用YAML格式来表示,围绕几个核心抽象构建：
<| 概念           | 描述           |
| :------------- | :------------- |
| Handler        | Handlers就是一个配置完成的适配器。适配器的构造器参数就是Handler的配置。   |
| 实例            | 一个（请求）实例就是请求属性到一个模板的映射结果。这种映射来自于实例的配置。 |
| 规则            |规则确定了何时使用一个特定的模板配置来调用一个Handler                    |

#### Handler
适配器封装了Mixer和特定外部基础设施后端进行交互的必要接口，例如Prometheus、New Relic或者Stackdriver。各种适配器都需要参数配置才能工作。例如日志适配器可能需要IP地址和端口来进行日志的输出。

这里的例子配置了一个类型为 listchecker 的适配器。listchecker适配器使用一个列表来检查输入。如果配置的是白名单模式且输入值存在于列表之中，就会返回成功的结果。
```
apiVersion: config.istio.io/v1alpha2
kind: listchecker
metadata:
  name: staticversion
  namespace: istio-system
spec:
  providerUrl: http://white_list_registry/
  blacklist: false
```
 > {metadata.name}.{kind}.{metadata.namespace} 是Handler的完全限定名

#### 实例
配置实例将请求中的属性映射成为适配器的输入, 注意Handler配置中需要的所有维度都定义在这一映射之中。
```
apiVersion: config.istio.io/v1alpha2
kind: metric
metadata:
  name: requestduration
  namespace: istio-system
spec:
  value: response.duration | "0ms"
  dimensions:
    destination_service: destination.service | "unknown"
    destination_version: destination.labels["version"] | "unknown"
    response_code: response.code | 200
  monitored_resource_type: '"UNSPECIFIED"'
```

#### 规则
规则用于指定使用特定实例配置调用某一Handler的时机。比如我们想要把 service1 服务中，请求头中带有 x-user 的请求的 requestduration 指标发送给Prometheus Handler:
```
apiVersion: config.istio.io/v1alpha2
kind: rule
metadata:
  name: promhttp
  namespace: istio-system
spec:
  match: destination.service == "service1.ns.svc.cluster.local" && request.headers["xuser"] == "user1"
  actions:
  - handler: handler.prometheus
    instances:
    - requestduration.metric.istio-system
```

#### Adapter适配器定制开发
- Go开发环境
如果缺少依赖的库，执行如下命令:

```
go get -u github.com/golang/dep/cmd/dep
```
将会在另外文章中详细介绍如何定制化开发适配器，此处不做赘述。

![operator, adapter and template devs](/images/operator-template-adapter-dev.svg)

### Pilot (原Istio-Manager)
Pilot负责收集和验证配置并将其传播到各种Istio组件。它从Mixer和Envoy中抽取环境特定的实现细节，为他们提供用户服务的抽象表示，独立于底层平台。此外，流量管理规则（即通用4层规则和7层HTTP/gRPC路由规则）可以在运行时通过Pilot进行编程。

* 用户与Istio之间的接口, 收集并验证配置信息并发送给其他组件.作为流量管理的核心组件, Pilot管理所有配置的Envoy代理实例,并提供如下流量管理方式:
![](/images/TrafficManagementOverview-2.png)

* Pilot提供了一个用于适配底层集群管理平台的抽象层，如Kubernetes适配层。
![](/images/ManagerAdapters.png)

* 此外还提供了一个代理控制器，用于Istio代理的动态配置。

![architecture](/images/istio-pilot-architecture.png)

#### Service Model服务模型
服务Service本身并不是Istio特有或新提出的概念，例如K8s早已经提供了类似的service概念和能力。但在Istio中为了支持更细粒度的路由能力，针对服务模型提供了版本的机制，例如可以通过附加label的方式描述版本等。
作为一个逻辑抽象，每一个服务通常会有FQDN全域名及若干个端口，或者也可能会有一个单独的负载均衡器、虚拟IP地址与之对应。
  > 例如，k8s中，一个服务foo就会有一个域名foo.default.svc.cluster.local hostname，虚拟IP10.0.1.1以及可能的监听端口。

一个服务往往会对应多个实例，每一个实例可以是一个container,pod或者vm. 每一个实例都有一个网络端点以及暴露的监听端口。
Istio本身不会提供服务注册以及发现的能力，而是依赖于底层平台提供的现成能力来完成服务注册发现。另外，istio也不会提供DNS能力，也是通过底层平台提供的dns能力（如kube-dns)解析域名。

基于规则的路由能力则是由Istio proxy sidecar 进程来实现，如envoy, nginx等。这些代理通常会同时提供4层与7层的路由能力。这些规则定义可以基于label，或者权重，亦或是基于http header、url等。

#### Configuration Model配置模型
Istio的规则配置提供了一个基于pb的模式定义。这些规则内容存储在一个KVstore中，pilot订阅了这些配置信息的变化，以便更新istio其他组件的配置内容。
```
apiVersion: config.istio.io/v1alpha2
kind: RouteRule
metadata:
  name: reviews-default
spec:
  destination:
    name: reviews
  route:
  - labels:
    version: v1
    weight: 100
```

#### 代理控制器
显然，代理控制器是pilot的核心模块，用于控制管理istio中的proxy。当前istio采用了envoy作为proxy，因为istio提供了一套标准的抽象api来对接proxy，所以将来用户可以自定制proxy，不见得必须采用envoy。
针对envoy，istio当前提供了2个组件：
- **proxy agent** 一套用于从抽象服务模型和规则配置生成envoy配置信息的脚本命令，同时也会触发proxy的重启；
- **discovery service** 实现了envoy的服务发现API，从而可以发布信息到envoy代理；

```
GET /v1/registration/(string: service_name)

请求发现服务返回指定service_name的所有主机, 返回以下JSON格式的响应：
{
  "hosts": []
}

const std::string Json::Schema::SDS_SCHEMA(R"EOF(
  {
    "$schema": "http://json-schema.org/schema#",
    "definitions" : {
      "host" : {
        "type" : "object",
        "properties" : {
          "ip_address" : {"type" : "string"},
          "port" : {"type" : "integer"},
          "tags" : {
            "type" : "object",
            "properties" : {
              "az" : {"type" : "string"},
              "canary" : {"type" : "boolean"},
              "load_balancing_weight": {
                "type" : "integer",
                "minimum" : 1,
                "maximum" : 100
              }
            }
          }
        },
        "required" : ["ip_address", "port"]
      }
    },
    "type" : "object",
    "properties" : {
      "hosts" : {
        "type" : "array",
        "items" : {"$ref" : "#/definitions/host"}
      }
    },
    "required" : ["hosts"]
  }
  )EOF");
```  


这儿不得不提的是pilot的 **proxy injection** 能力，你可能已经想到了它是基于iptable规则来实现的。这样所有的服务交互都会被pilot捕获并重新转发。

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

istioctl mixer rule create global ratings.default.svc.cluster.local -f samples/apps/bookinfo/mixer-rule-ratings-denial.yaml

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
## 结论
本文主要从概念、架构到每个组件原理介绍了istio，作为一个用于连接、管理以及安全化微服务的开放平台, istio的确提供了一种简单的方式用于创建微服务网络。后续将会提供一系列的文章分别介绍具体的案例以及涉及的核心技术。
