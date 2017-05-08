---
title: 分布式架构
categories: 分布式
tags:
  - 分布式
  - 架构设计
---

### 1. 分布式配置管理
  - Consul
  - Archaius
  - Kubernetes ConfigMap & Secrets

配置的集中管理：采用consul的KV，将所有微服务的application.properties中的配置内容存入consul。

配置的动态管理：采用archaius，将consul上的配置信息读到spring的PropertySource和archaius的PollResult中，当修改了配置信息后，经常改变的值通过DynamicFactory来获取，不经常改变的值可以通过其他方式获取. 大部分情况下，修改了consul上的配置信息后，相应的项目不需要重启，也会读到最新的值。

### 2. 服务注册与发现
  - [Consul](/2017/03/08/consul/)
  - Eureka
  - [Kubernetes Service](kubernetes.md#service) & Ingress Resource

### 3. 负载平衡
  - Netflix Ribbon（Spring Cloud）
  - [Kubernetes Service](kubernetes.md#service)

  [Client Side Load Balancing](consul.md)

### 4. API网关与智能路由
  - Netflix Zuul（SpringCloud）
  - [Kubernetes Service](kubernetes.md#service) &Ingress Resource

  [Gateway](gateway.md)

### 5. 分布式服务弹性与容错
 - 弹性服务
 - 服务降级
 - 线程池/信号隔离
 - 快速解决依赖隔离

 [Hystrix架构设计](Hystrix.md)

### 6. 日志管理
  - ELK Stack（LogStash -> ES -> Kibana）

### 7. 分布式跟踪
  - Zipkin
  - SpringCloud Sleuth

  [Zipkin](zipkin) is a distributed tracing system. It helps gather timing data needed to troubleshoot latency problems in microservice architectures. It manages both the collection and lookup of this data. Zipkin’s design is based on the Google Dapper paper.

  Applications are instrumented to report timing data to Zipkin. The Zipkin UI also presents a Dependency diagram showing how many traced requests went through each application. If you are troubleshooting latency problems or errors, you can filter or sort all traces based on the application, length of trace, annotation, or timestamp. Once you select a trace, you can see the percentage of the total trace time each span takes which allows you to identify the problem application.

### 8. 监控与度量
  - Application/Infrastructure monitoring using StatsD + Graphite + Grafana

  [StatsD + Graphite + Grafana](sgg.md)

### 9. 服务安全
  - SpringCloud Security

### 10. Auto Scaling
  - Kubernetes Health Check、SelfHealing、Autoscaling

### 11. 打包部署和调度部署
  - Spring Boot；
  - Docker／Rkt、Kubernetes Scheduler&Deployment

### 12. 任务工作管理
  - Spring Batch
  - Kubernetes Jobs&Scheduled Jobs


## 分布式存储
#### 1.持久化 - 分布式文件系统
- [HDFS分布式文件系统](hdfs.md)

#### 2.持久化 - 分布式数据库
- [传统关系型数据库集群,如MySQL Cluster]
- [Mongo](mongo.md)
- [Cassandra,HBase](hbase.md)

#### 3.非持久化 - 分布式缓存/消息系统
- [Kafka](kafka.md)
- [Redis]

## 分布式计算框架
  - [YARN分布式计算框架](yarn.md)
  - [YARN应用开发的几种方式](yarn-appdev.md)
  - [Running Spark on YARN](running-spark-on-yarn.md)

### Spark Big Data Analytics
  - [Spark](spark.md)
