---
title: 分布式系统架构设计
date: 2017-5-1 20:46:25
categories:
- 分布式&云计算
- 分布式技术架构
tags:
  - 分布式
  - 架构设计
---

## 分布式配置管理
  - [Consul](/consul) | etcd
  - [Archaius动态管理](/archaius)
  - [Kubernetes ConfigMap](/kubernetes-configmap/)
  - [Kubernetes Secrets](/kubernetes-secrets/)

配置的集中管理：采用consul的KV，将所有微服务的application.properties中的配置内容存入consul。

配置的动态管理：采用archaius，将consul上的配置信息读到spring的PropertySource和archaius的PollResult中，当修改了配置信息后，经常改变的值通过DynamicFactory来获取，不经常改变的值可以通过其他方式获取. 大部分情况下，修改了consul上的配置信息后，相应的项目不需要重启，也会读到最新的值。

### 配置的自动刷新
尽管使用/refresh 端点手动刷新配置，但是如果所有微服务节点的配置都需要手动去刷新的话，那必然是一个繁琐的工作，并且随着系统的不断扩张，会变得越来越难以维护。因此，实现配置的自动刷新是很有必要的，本节我们讨论使用Spring Cloud Bus实现配置的自动刷新。
Spring Cloud Bus提供了批量刷新配置的机制，它使用轻量级的消息代理（例如RabbitMQ、Kafka等）连接分布式系统的节点，这样就可以通过Spring Cloud Bus广播配置的变化或者其他的管理指令。
![](/images/spring-config-bus.png)

启动RabiitMQ:
```
docker run -d -p 5671:5671 -p 5672:5672 -p 4369:4369 -p 25672:25672  --hostname my-rabbit --name myrabbit rabbitmq

```

## 服务注册与发现
  - [Consul](/consul/)
  - Eureka | Zookeeper | etcd [对比](/servicediscovery)
  - [Kubernetes Service](/kubernetes/#service)

## 分布式锁
  - [分布式锁](/distributed-lock/)

## 负载平衡
  - Netflix Ribbon（Spring Cloud）
  - [Kubernetes Service](/kubernetes/#service)

  [Client Side Load Balancing](/consul/)

## API网关与智能路由
  - Netflix Zuul（SpringCloud）
  - [Kubernetes Service](/kubernetes/#service)
  - [Kubernetes Ingress](/kubernetes-ingress/)

  [Gateway](/gateway/)

## 分布式服务弹性与容错
 - 弹性服务
 - 服务降级
 - 线程池/信号隔离
 - 快速解决依赖隔离

 [Hystrix架构设计](/Hystrix/)

## 日志管理
  - ELK Stack（LogStash -> ES -> Kibana）

## 分布式跟踪
  - Zipkin
  - SpringCloud Sleuth

  [Zipkin](/zipkin/) is a distributed tracing system. It helps gather timing data needed to troubleshoot latency problems in microservice architectures. It manages both the collection and lookup of this data. Zipkin’s design is based on the Google Dapper paper.

  Applications are instrumented to report timing data to Zipkin. The Zipkin UI also presents a Dependency diagram showing how many traced requests went through each application. If you are troubleshooting latency problems or errors, you can filter or sort all traces based on the application, length of trace, annotation, or timestamp. Once you select a trace, you can see the percentage of the total trace time each span takes which allows you to identify the problem application.

## 监控与度量
  - Application/Infrastructure monitoring using StatsD + Graphite + Grafana

  [StatsD + Graphite + Grafana](/sgg/)

## 服务安全
  - SpringCloud Security

## Auto Scaling
  - [Kubernetes Autoscaling](/kubernetes/#Autoscaling)

## 打包部署和调度部署
  - Spring Boot；
  - [Docker／Rkt、Kubernetes Scheduler&Deployment](/kubernetes/#Deployment)

## 任务工作管理
  - Spring Batch
  - [Kubernetes Jobs](/kubernetes/#Job)


## 分布式存储
### 持久化 - 分布式文件系统
- [HDFS分布式文件系统](/hdfs/)

### 持久化 - 分布式数据库
- [传统关系型数据库集群,如MySQL Cluster]
- [Mongo](/mongo/)
- [Cassandra,HBase](/hbase/)

### 非持久化 - 分布式缓存/消息系统
- [Kafka](/kafka/)
- [Redis]

## 分布式计算框架
  - [YARN分布式计算框架](/yarn/)
  - [YARN应用开发的几种方式](/yarn-appdev/)
  - [Running Spark on YARN](/running-spark-on-yarn/)
  - [Spark Big Data Analytics](/spark/)
