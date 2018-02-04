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

## 路由规则解析
### front-envoy.yaml文件
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
