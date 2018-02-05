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


### Envoy是什么？

Envoy 是一个面向服务架构的L7代理和通信总线而设计的，这个项目诞生是出于以下目标：
> 对于应用程序而言，网络应该是透明的，当发生网络和应用程序故障时，能够很容易定位出问题的根源。

实际上，实现前面提到的目标是非常困难的，Envoy 试图通过提供以下高级特性：

- **外置进程架构**：Envoy是一个独立的进程，与应用程序一起运行。所有的Envoy形成一个对应用透明的通信网格，每个应用程序通过本地发送和接受消息，并不感知网络拓扑结构。这个外置进程的架构相比传统的基于library库服务通信，有两个优势；

  - Envoy可以与任何语言开发的应用一起工作。Java, C++, Go, PHP, Python等都可以基于Envoy部署成一个服务网格，在面向服务的架构体系中，使用多语言开发应用越来越普遍，Envoy填补了这一空白。
  - 任何参与面向服务的大型架构里工作的人都知道，部署升级library是非常令人痛苦的，而现在可以在整个基础设施上快速升级Envoy。

- **基于新C++11编码**：Envoy是基于C++11编写的，之所以这样选择，是因为我们认为，Envoy这样的体系结构组件能够快速有效的开发出来。而现代应用程序开发者已经很难在云环境部署和使用它；通常会选择性能不高，但是能够快速提升开发效率的PHP、Python、Ruby、Scala等语言，并且能够解决复杂的外部环境依赖。并不像本地开发代码那样使用如C、C++能够提供高效的性能。

- **L3/L4过滤器**：Envoy其核心是一个L3/L4网络代理，能够作为一个可编程过滤器实现不同TCP代理任务，插入到主服务当中。通过编写过滤器来支持各种任务，如原始TCP代理、HTTP代理、TLS客户端证书身份验证等。

- **HTTP L7过滤器**：在现代应用架构中，HTTP是非常关键的部件，Envoy支持一个额外的HTTP L7过滤层。HTTP过滤器作为一个插件，插入到HTTP链接管理子系统中，从而执行不同的任务，如缓冲，速率限制，路由/转发，嗅探Amazon的DynamoDB等等。

- **支持HTTP/2**：在HTTP模式下，Envoy支持HTTP/1.1、HTTP/2，并且支持HTTP/1.1、HTTP/2双向代理。这意味着HTTP/1.1和HTTP/2，在客户机和目标服务器的任何组合都可以桥接。建议在服务间的配置使用时，Envoy之间采用HTTP/2来创建持久网络连接，这样请求和响应可以被多路复用。Envoy并不支持被淘汰SPDY协议。

- **HTTP L7路由**：在HTTP模式下运行时，Envoy支持根据content type、runtime values等，基于path的路由和重定向。当服务构建到服务网格时，Envoy为前端/边缘代理，这个功能是非常有用的。

- **支持gRPC**：gRPC是一个来自谷歌的RPC框架，使用HTTP/2作为底层的多路传输。HTTP/2承载的gRPC请求和应答，都可以使用Envoy的路由和LB能力。所以两个系统非常互补。

- **支持MongoDB L7**：现代Web应用程序中，MongoDB是一个非常流行的数据库应用，Envoy支持获取统计和连接记录等信息。

- **支持DynamoDB L7**：DynamoDB是亚马逊托管的NoSQL Key/Value存储。Envoy支持获取统计和连接等信息。

- **服务发现**：服务发现是面向服务体系架构的重要组成部分。Envoy支持多种服务发现方法，包括异步DNS解析和通过REST调用服务发现(Service discovery)服务。

- **健康检查**：构建Envoy网格的推荐方法，是将服务发现视为一个最终一致的方法。Envoy含有一个健康检查子系统，它可以对上游服务集群进行主动的健康检查。然后，Envoy联合服务发现、健康检查信息来判定健康的LB对象。Envoy作为一个外置健康检查子系统，也支持被动健康检查。

- **高级LB**：在分布式系统中，不同组件之间LB也是一个复杂的问题。Envoy是一个独立的代理进程，不是一个lib库，所以他能够在一个地方实现高级LB，并且能够被任何应用程序访问。目前，包括自动重试、断路器，全局限速，阻隔请求，异常检测。将来还会支持按计划进行请求速率控制。

- **前端代理**：虽然Envoy作为服务间的通信系统而设计，但是（调试，管理、服务发现、LB算法等）同样可以适用于前端，Envoy提供足够的特性，能够作为绝大多数Web应用的前端代理，包括TLS、HTTP/1.1、HTTP/2，以及HTTP L7路由。

- **极好的可观察性**：如上所述，Envoy目标是使得网络更加透明。而然，无论是网络层还是应用层，都可能会出现问题。Envoy包括对所有子系统，提供了可靠的统计能力。目前只支持statsd以及兼容的统计库，虽然支持另一种并不复杂。还可以通过管理端口查看统计信息，Envoy还支持第三方的分布式跟踪机制。

- **动态配置**：Envoy提供分层的动态配置API，用户可以使用这些API构建复杂的集中管理部署。

- **生态集成**：Envoy将作为一个独立的sidecar与相关微服务部署在同一个Kubernetes的pod上,并提供一系列的属性给Mixer.Mixer以此作为依据执行策略,并发送到监控系统.这种sidecar代理模型不需要改变任何服务本身的逻辑,并能增加一系列的功能.

## Envoy Front Proxy示例
下面是一个基于Envoy+Docker构造的Front Proxy的示例。下面是docker-compose部署拓扑，其中这些容器所依赖的虚拟网络为envoymesh。

所有的请求都会被接入到Front Proxy，这个Front Proxy扮演了位于整个envoymesh网络前端的反向代理。其中这个Front Proxy容器的端口80被映射到所在宿主机的端口8000。

![Envoy Front Proxy Arch](/images/envoy-front-proxy-topology.png)

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

![](/images/envoy-front-proxy-config-files.png)

其中，docker-compose.yml内容如下：
```
version: '2'
services:

  front-envoy:
    build:
      context: ../
      dockerfile: front-proxy/Dockerfile-frontenvoy
    volumes:
      - ./front-envoy.yaml:/etc/front-envoy.yaml
    networks:
      - envoymesh
    expose:
      - "80"
      - "8001"
    ports:
      - "8000:80"
      - "8001:8001"

  service1:
    build:
      context: .
      dockerfile: Dockerfile-service
    volumes:
      - ./service-envoy.yaml:/etc/service-envoy.yaml
    networks:
      envoymesh:
        aliases:
          - service1
    environment:
      - SERVICE_NAME=1
    expose:
      - "80"

  service2:
    build:
      context: .
      dockerfile: Dockerfile-service
    volumes:
      - ./service-envoy.yaml:/etc/service-envoy.yaml
    networks:
      envoymesh:
        aliases:
          - service2
    environment:
      - SERVICE_NAME=2
    expose:
      - "80"

networks:
  envoymesh: {}

```
通过该docker-compose.yml文件可以很清晰地看到，会有3个容器被创建，它们分别对应服务 *front-envoy*、*service1* 以及 *service2* 。

其中，服务 *front-envoy* 的Dockerfile文件为Dockerfile-frontenvoy，基于envoyproxy/envoy最新的镜像文件，并安装了curl，最后启动了envoy代理，参数文件为front-envoy.yaml（后面会相信解释），服务集群名称定为front-proxying。

```
FROM envoyproxy/envoy:latest

RUN apt-get update && apt-get -q install -y \
    curl
CMD /usr/local/bin/envoy -c /etc/front-envoy.yaml --service-cluster front-proxy

```

服务 *service1* 与 *service2* 使用了相同的Dockerfile（如下），基于envoyproxy/envoy-alpine的最新镜像，并依次安装了python3、Flask以及requests等。
并把脚本service.py及start_service.sh分别复制到了容器目录/code及/usr/local/bin/下，而且给/usr/local/bin/start_service.sh添加了执行权限并启动。

Dockerfile文件：
```
FROM envoyproxy/envoy-alpine:latest

RUN apk update && apk add python3 bash
RUN python3 --version && pip3 --version
RUN pip3 install -q Flask==0.11.1 requests==2.18.4
RUN mkdir /code
ADD ./service.py /code
ADD ./start_service.sh /usr/local/bin/start_service.sh
RUN chmod u+x /usr/local/bin/start_service.sh
ENTRYPOINT /usr/local/bin/start_service.sh

```

Python脚本service.py：
```
from flask import Flask
from flask import request
import socket
import os
import sys
import requests

app = Flask(__name__)

TRACE_HEADERS_TO_PROPAGATE = [
    'X-Ot-Span-Context',
    'X-Request-Id',
    'X-B3-TraceId',
    'X-B3-SpanId',
    'X-B3-ParentSpanId',
    'X-B3-Sampled',
    'X-B3-Flags'
]

@app.route('/service/<service_number>')
def hello(service_number):
    return ('Hello from behind Envoy (service {})! hostname: {} resolved'
            'hostname: {}\n'.format(os.environ['SERVICE_NAME'],
                                    socket.gethostname(),
                                    socket.gethostbyname(socket.gethostname())))

@app.route('/trace/<service_number>')
def trace(service_number):
    headers = {}
    # call service 2 from service 1
    if int(os.environ['SERVICE_NAME']) == 1 :
        for header in TRACE_HEADERS_TO_PROPAGATE:
            if header in request.headers:
                headers[header] = request.headers[header]
        ret = requests.get("http://localhost:9000/trace/2", headers=headers)
    return ('Hello from behind Envoy (service {})! hostname: {} resolved'
            'hostname: {}\n'.format(os.environ['SERVICE_NAME'],
                                    socket.gethostname(),
                                    socket.gethostbyname(socket.gethostname())))

if __name__ == "__main__":
    app.run(host='127.0.0.1', port=8080, debug=True)

```

启动服务脚本start_service.sh，注意的是最后一行启动了envoy代理，参数文件为service-envoy.yaml（后面会相信解释），服务集群名称定为service${SERVICE_NAME}。
```
#!/usr/bin/env bash
python3 /code/service.py &
envoy -c /etc/service-envoy.yaml --service-cluster service${SERVICE_NAME}

```
虚拟网络envoymesh会被创建用于支持上述3个容器。

通过docker-compose命令启动容器，过程如下：

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

## 路由及负载均衡规则解析
### envoy配置文件
front-envoy.yaml文件如下定义：
```
static_resources:
  listeners:
  - address:
      socket_address:
        address: 0.0.0.0
        port_value: 80
    filter_chains:
    - filters:
      - name: envoy.http_connection_manager
        config:
          codec_type: auto
          stat_prefix: ingress_http
          route_config:
            name: local_route
            virtual_hosts:
            - name: backend
              domains:
              - "*"
              routes:
              - match:
                  prefix: "/service/1"
                route:
                  cluster: service1
              - match:
                  prefix: "/service/2"
                route:
                  cluster: service2
          http_filters:
          - name: envoy.router
            config: {}
  clusters:
  - name: service1
    connect_timeout: 0.25s
    type: strict_dns
    lb_policy: round_robin
    http2_protocol_options: {}
    hosts:
    - socket_address:
        address: service1
        port_value: 80
  - name: service2
    connect_timeout: 0.25s
    type: strict_dns
    lb_policy: round_robin
    http2_protocol_options: {}
    hosts:
    - socket_address:
        address: service2
        port_value: 80
admin:
  access_log_path: "/dev/null"
  address:
    socket_address:
      address: 0.0.0.0
      port_value: 8001

```

service-envoy.yaml文件如下定义：
```
static_resources:
  listeners:
  - address:
      socket_address:
        address: 0.0.0.0
        port_value: 80
    filter_chains:
    - filters:
      - name: envoy.http_connection_manager
        config:
          codec_type: auto
          stat_prefix: ingress_http
          route_config:
            name: local_route
            virtual_hosts:
            - name: service
              domains:
              - "*"
              routes:
              - match:
                  prefix: "/service"
                route:
                  cluster: local_service
          http_filters:
          - name: envoy.router
            config: {}
  clusters:
  - name: local_service
    connect_timeout: 0.25s
    type: strict_dns
    lb_policy: round_robin
    hosts:
    - socket_address:
        address: 127.0.0.1
        port_value: 8080
admin:
  access_log_path: "/dev/null"
  address:
    socket_address:
      address: 0.0.0.0
      port_value: 8081

```
### 测试Envoy的路由能力

测试service1：
```
Xis-MacBook-Pro:front-proxy xiningwang$ curl -v $(docker-machine ip default):8000/service/1
*   Trying 192.168.99.101...
* TCP_NODELAY set
* Connected to 192.168.99.101 (192.168.99.101) port 8000 (#0)
> GET /service/1 HTTP/1.1
> Host: 192.168.99.101:8000
> User-Agent: curl/7.54.0
> Accept: */*
>
< HTTP/1.1 200 OK
< content-type: text/html; charset=utf-8
< content-length: 89
< server: envoy
< date: Sun, 04 Feb 2018 09:16:07 GMT
< x-envoy-upstream-service-time: 2
<
Hello from behind Envoy (service 1)! hostname: 326cdcbf4dcf resolvedhostname: 172.18.0.3
* Connection #0 to host 192.168.99.101 left intact
```

测试service2:
```
Xis-MacBook-Pro:front-proxy xiningwang$ curl -v $(docker-machine ip default):8000/service/2
*   Trying 192.168.99.101...
* TCP_NODELAY set
* Connected to 192.168.99.101 (192.168.99.101) port 8000 (#0)
> GET /service/2 HTTP/1.1
> Host: 192.168.99.101:8000
> User-Agent: curl/7.54.0
> Accept: */*
>
< HTTP/1.1 200 OK
< content-type: text/html; charset=utf-8
< content-length: 89
< server: envoy
< date: Sun, 04 Feb 2018 09:17:47 GMT
< x-envoy-upstream-service-time: 3
<
Hello from behind Envoy (service 2)! hostname: 6abdb62aa858 resolvedhostname: 172.18.0.2
* Connection #0 to host 192.168.99.101 left intact
```

可以看到，不同的请求路径会被准确地路由到后端的不同服务上。这种路由能力正是Envoy所擅长提供的，而是是以一种非常灵活的配置方式来提供可变的路由匹配路径。
本例子中，front-envoy.yaml文件中包括了如下的路由匹配规则：
```
> - match:
>    prefix: "/service/1"
>  route:
>    cluster: service1
> - match:
>    prefix: "/service/2"
>  route:
>    cluster: service2

```


### 测试Envoy的负载均衡能力
针对service1进行扩容，增加到3个container：
```
$ docker-compose up --scale service1=3
$ docker-compose ps
          Name                        Command               State                      Ports
----------------------------------------------------------------------------------------------------------------
frontproxy_front-envoy_1   /bin/sh -c /usr/local/bin/ ...   Up      0.0.0.0:8000->80/tcp, 0.0.0.0:8001->8001/tcp
frontproxy_service1_1      /bin/sh -c /usr/local/bin/ ...   Up      80/tcp
frontproxy_service1_2      /bin/sh -c /usr/local/bin/ ...   Up      80/tcp
frontproxy_service1_3      /bin/sh -c /usr/local/bin/ ...   Up      80/tcp
frontproxy_service2_1      /bin/sh -c /usr/local/bin/ ...   Up      80/tcp


$ docker ps
CONTAINER ID        IMAGE                    COMMAND                  CREATED              STATUS              PORTS                                          NAMES
06f3c0d14b1a        frontproxy_service1      "/bin/sh -c /usr/loc…"   About a minute ago   Up About a minute   80/tcp                                         frontproxy_service1_2
658f44d72f00        frontproxy_service1      "/bin/sh -c /usr/loc…"   About a minute ago   Up About a minute   80/tcp                                         frontproxy_service1_3
326cdcbf4dcf        frontproxy_service1      "/bin/sh -c /usr/loc…"   3 hours ago          Up 3 hours          80/tcp                                         frontproxy_service1_1
b8bbdc349469        frontproxy_front-envoy   "/bin/sh -c '/usr/lo…"   3 hours ago          Up 3 hours          0.0.0.0:8001->8001/tcp, 0.0.0.0:8000->80/tcp   frontproxy_front-envoy_1
6abdb62aa858        frontproxy_service2      "/bin/sh -c /usr/loc…"   3 hours ago          Up 3 hours          80/tcp                                         frontproxy_service2_1

```

连续访问多次service1，会看到3个不同的容器会被轮询调用：
```
Xis-MacBook-Pro:front-proxy xiningwang$ curl -v $(docker-machine ip default):8000/service/1
*   Trying 192.168.99.101...
* TCP_NODELAY set
* Connected to 192.168.99.101 (192.168.99.101) port 8000 (#0)
> GET /service/1 HTTP/1.1
> Host: 192.168.99.101:8000
> User-Agent: curl/7.54.0
> Accept: */*
>
< HTTP/1.1 200 OK
< content-type: text/html; charset=utf-8
< content-length: 89
< server: envoy
< date: Sun, 04 Feb 2018 10:05:18 GMT
< x-envoy-upstream-service-time: 4
<
Hello from behind Envoy (service 1)! hostname: 658f44d72f00 resolvedhostname: 172.18.0.5
* Connection #0 to host 192.168.99.101 left intact
Xis-MacBook-Pro:front-proxy xiningwang$ curl -v $(docker-machine ip default):8000/service/1
*   Trying 192.168.99.101...
* TCP_NODELAY set
* Connected to 192.168.99.101 (192.168.99.101) port 8000 (#0)
> GET /service/1 HTTP/1.1
> Host: 192.168.99.101:8000
> User-Agent: curl/7.54.0
> Accept: */*
>
< HTTP/1.1 200 OK
< content-type: text/html; charset=utf-8
< content-length: 89
< server: envoy
< date: Sun, 04 Feb 2018 10:05:22 GMT
< x-envoy-upstream-service-time: 3
<
Hello from behind Envoy (service 1)! hostname: 06f3c0d14b1a resolvedhostname: 172.18.0.6
* Connection #0 to host 192.168.99.101 left intact
Xis-MacBook-Pro:front-proxy xiningwang$
Xis-MacBook-Pro:front-proxy xiningwang$ curl -v $(docker-machine ip default):8000/service/1
*   Trying 192.168.99.101...
* TCP_NODELAY set
* Connected to 192.168.99.101 (192.168.99.101) port 8000 (#0)
> GET /service/1 HTTP/1.1
> Host: 192.168.99.101:8000
> User-Agent: curl/7.54.0
> Accept: */*
>
< HTTP/1.1 200 OK
< content-type: text/html; charset=utf-8
< content-length: 89
< server: envoy
< date: Sun, 04 Feb 2018 10:05:26 GMT
< x-envoy-upstream-service-time: 2
<
Hello from behind Envoy (service 1)! hostname: 326cdcbf4dcf resolvedhostname: 172.18.0.3
* Connection #0 to host 192.168.99.101 left intact
```


可以看到，多次请求会被路由到后端的不同容器上。这种负载均衡能力正是Envoy所擅长提供的，而是是以一种非常灵活的配置方式来提供可变的路由匹配路径。
本例子中，front-envoy.yaml文件中包括了如下的负载均衡规则：
```
clusters:
 - name: service1
   connect_timeout: 0.25s
   type: strict_dns
   lb_policy: round_robin
   http2_protocol_options: {}
   hosts:
   - socket_address:
       address: service1
       port_value: 80

```

## Envoy的请求跟踪能力解析
### 添加zipkin服务

为了启用Zipkin请求跟踪能力，需要在docker-compose.yml文件中添加zipkin服务，如下：

```
version: '2'
services:

  front-envoy:
    ....

  service1:
    ....

  service2:
    ....

  zipkin:
    image: openzipkin/zipkin
    networks:
      envoymesh:
        aliases:
          - zipkin
    expose:
      - "9411"
    ports:
      - "9411:9411"

networks:
  envoymesh: {}

```

在front-proxy.yaml文件中需要启用tracing能力，包括启用生成请求ID、zipkin服务及配置envoy的跟踪项等；注意的一点是对于front-proxy容器来说，operation_name应当设置为egress；而对service1|service2容器来说，operation_name应当设置为ingress。

![front-proxy](/images/envoy_examples_front-proxy_front-envoy_yaml-envoy_examples_zipkin-tracing_front-envoy-zipkin_yaml.png)

具体如下：
```
static_resources:
  listeners:
  - address:
      socket_address:
        address: 0.0.0.0
        port_value: 80
    filter_chains:
    - filters:
      - name: envoy.http_connection_manager
        config:
          generate_request_id: true
          tracing:
            operation_name: egress

....
- name: zipkin
    connect_timeout: 1s
    type: strict_dns
    lb_policy: round_robin
    hosts:
    - socket_address:
        address: zipkin
        port_value: 9411
tracing:
  http:
    name: envoy.zipkin
    config:
      collector_cluster: zipkin
      collector_endpoint: "/api/v1/spans"
```

### 可视化zipkin服务
zipkin提供了一个默认的UI界面，用于展示服务调用之间的跟踪路径。

![zipkin](/images/envoy-zipkin-service1-service2.png)
