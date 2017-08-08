---
title: IoT Platform
date: 2017-5-27 20:46:25
categories:
  - 分布式&云计算
  - IOT
tags:
  - IOT
  - docker
  - container
  - 容器
  - PaaS
---

## 环境准备
因为本人的开发环境是Mac，所以需要使用socat工具来转发，从而可以访问Remote Docker API。
```
socat TCP-LISTEN:1234,reuseaddr,fork UNIX-CONNECT:/var/run/docker.sock
```

启用Docker Swarm Mode:
```
docker swarm init
```
编译整个项目，进入项目根目录，运行：
```
mvn clean install -Ddocker.host=tcp://${host}:${port} -Pbuild-docker-image
```
其中，${host}是docker host的IP地址或域名，${port}是可以访问Remote Docker API的端口。如前面所述，Mac环境下使用了socat进行端口转发，因此这儿的port则是socat参数中的监听端口。例如：
```
mvn clean install -Ddocker.host=tcp://127.0.0.1:1234 -Pbuild-docker-image
```
如果不执行测试用例、也不编译测试用例类，执行：
```
mvn clean install -Ddocker.host=tcp://127.0.0.1:1234 -Pbuild-docker-image -Dmaven.test.skip=true
```

## 启动服务
使用docker stack deploy启动整个服务：
```
~/hono/example/target/hono$ docker stack deploy -c docker-compose.yml hono
```

运行结束之后会启动一个overlay网络和若干service，结果如下:
```
Creating network hono_hono-net
Creating service hono_hono
Creating service hono_kafka
Creating service hono_qdrouter
Creating service hono_zookeeper
Creating service hono_rest-adapter
Creating service hono_auth-server
Creating service hono_influxdb
Creating service hono_mqtt-adapter
Creating service hono_grafana
Creating service hono_device-registry
Creating service hono_artemis
```
