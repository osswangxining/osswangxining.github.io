---
title: 基于haproxy搭建MQTT服务器集群
date: 2017-1-28 20:46:25
categories:
  - 分布式&云计算
tags:
  - Kubernetes
  - container
  - 容器
  - 代理
---

## 为什么选择haproxy?
apache、nginx之类的反向代理(转发)功能，通常只能用于http协议，其它协议只有在商业版才能得到支持。而haproxy可以弥补这方面的不足，它不仅可以支持http协议，也可以支持tcp多种协议。因此haproxy常常被当用来作为rpc(thrift/gRPC/avro)框架前端的负载均衡转发中间件。

下面介绍基本使用， 备注：仅为以下环境均为开发环境mac OSX。

## 安装haproxy
```
brew install haproxy
```
安装成功之后，运行如下命令验证：
```
haproxy -v
HA-Proxy version 1.8.4-1deb90d 2018/02/08
Copyright 2000-2018 Willy Tarreau <willy@haproxy.org>
```
显示安装的haproxy版本为 *1.8.4*。

安装后的路径为：
```
xis-macbook-pro:1.8.4 xiningwang$ pwd
/usr/local/Cellar/haproxy/1.8.4
xis-macbook-pro:1.8.4 xiningwang$ ls
CHANGELOG      			README 				share
INSTALL_RECEIPT.json   		bin
LICENSE				homebrew.mxcl.haproxy.plist
```

## haproxy配置
任意目录下创建文件haproxy.cfg，如下：
```
global
    daemon
    maxconn 256

defaults
    mode http
    timeout connect 5000ms
    timeout client 50000ms
    timeout server 50000ms

# Listen to all MQTT requests (port 1883)
listen mqtt
  # MQTT binding to port 2883
  bind *:2883
  # communication mode (MQTT works on top of TCP)
  mode tcp
  option tcplog
  # balance mode (to choose which MQTT server to use)
  balance leastconn
  # MQTT broker 1
  server broker_1 127.0.0.1:1883 check
```

## 启动haproxy
```
haproxy -f haproxy.cfg -d
```

启动成功之后，应当输出如下类似的内容：
```
WARNING] 040/202637 (80343) : config : log format ignored for proxy 'mqtt' since it has no log address.
Available polling systems :
     kqueue : pref=300,  test result OK
       poll : pref=200,  test result OK
     select : pref=150,  test result OK
Total: 3 (3 usable), will use kqueue.

Available filters :
       	[SPOE] spoe
       	[COMP] compression
       	[TRACE] trace
Using kqueue() as the polling mechanism.
```

命令 lsof -i tcp:port可以查看该端口被什么程序占用，并显示pid，方便kill进程。
```
xis-macbook-pro:~ xiningwang$ lsof -i tcp:2883
COMMAND   PID       USER   FD   TYPE             DEVICE SIZE/OFF NODE NAME
haproxy 80343 xiningwang    4u  IPv4 0xe2455e21c408b6b7      0t0  TCP *:ndnp (LISTEN)
```


```
[WARNING] 040/202835 (80343) : Server mqtt/broker_1 is DOWN, reason: Layer4 connection problem, info: "Connection refused", check duration: 0ms. 0 active and 0 backup servers left. 0 sessions active, 0 requeued, 0 remaining in queue.
[ALERT] 040/202835 (80343) : proxy 'mqtt' has no server available!
[WARNING] 040/202847 (80343) : Server mqtt/broker_1 is UP, reason: Layer4 check passed, check duration: 0ms. 1 active and 0 backup servers online. 0 sessions requeued, 0 total in queue.
```
/Users/xiningwang/localgit/iotplatform/iothub/target
java -jar iothub-1.0.0-boot.jar --mqtt.bind_port=1884 --server.port=18082 --coap.bind_port=5684
