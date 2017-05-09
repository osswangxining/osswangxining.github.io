---
title: 分布式架构
categories: 分布式&云计算
tags:
  - 分布式
  - 架构设计
---

## 分布式配置管理
  - [Consul](/2017/03/08/consul/)
  - Archaius
  - [Kubernetes ConfigMap](/2017/05/09/kubernetes-configmap/)
  - [Kubernetes Secrets](/2017/05/09/kubernetes-secrets/)

配置的集中管理：采用consul的KV，将所有微服务的application.properties中的配置内容存入consul。

配置的动态管理：采用archaius，将consul上的配置信息读到spring的PropertySource和archaius的PollResult中，当修改了配置信息后，经常改变的值通过DynamicFactory来获取，不经常改变的值可以通过其他方式获取. 大部分情况下，修改了consul上的配置信息后，相应的项目不需要重启，也会读到最新的值。

## 服务注册与发现
  - [Consul](/2017/03/08/consul/)
  - Eureka
  - [Kubernetes Service](/2017/05/09/kubernetes/#service)

## 负载平衡
  - Netflix Ribbon（Spring Cloud）
  - [Kubernetes Service](/2017/05/09/kubernetes/#service)

  [Client Side Load Balancing](/2017/03/08/consul/)

## API网关与智能路由
  - Netflix Zuul（SpringCloud）
  - [Kubernetes Service](/2017/05/09/kubernetes/#service)
  - [Kubernetes Ingress](/2017/05/09/kubernetes-ingress/)

  [Gateway](/2017/05/04/gateway/)

## 分布式服务弹性与容错
 - 弹性服务
 - 服务降级
 - 线程池/信号隔离
 - 快速解决依赖隔离

 [Hystrix架构设计](/2017/03/08/Hystrix/)

## 日志管理
  - ELK Stack（LogStash -> ES -> Kibana）

## 分布式跟踪
  - Zipkin
  - SpringCloud Sleuth

  [Zipkin](/2017/03/08/zipkin/) is a distributed tracing system. It helps gather timing data needed to troubleshoot latency problems in microservice architectures. It manages both the collection and lookup of this data. Zipkin’s design is based on the Google Dapper paper.

  Applications are instrumented to report timing data to Zipkin. The Zipkin UI also presents a Dependency diagram showing how many traced requests went through each application. If you are troubleshooting latency problems or errors, you can filter or sort all traces based on the application, length of trace, annotation, or timestamp. Once you select a trace, you can see the percentage of the total trace time each span takes which allows you to identify the problem application.

## 监控与度量
  - Application/Infrastructure monitoring using StatsD + Graphite + Grafana

  [StatsD + Graphite + Grafana](/2017/03/08/sgg/)

## 服务安全
  - SpringCloud Security

## Auto Scaling
  - [Kubernetes Autoscaling](/2017/05/09/kubernetes/#Autoscaling)

## 打包部署和调度部署
  - Spring Boot；
  - [Docker／Rkt、Kubernetes Scheduler&Deployment](/2017/05/09/kubernetes/#Deployment)

## 任务工作管理
  - Spring Batch
  - [Kubernetes Jobs](/2017/05/09/kubernetes/#Job)


## 分布式存储
### 持久化 - 分布式文件系统
- [HDFS分布式文件系统](/2017/03/26/hdfs/)

### 持久化 - 分布式数据库
- [传统关系型数据库集群,如MySQL Cluster]
- [Mongo](/2017/03/24/mongo/)
- [Cassandra,HBase](/2017/05/03/hbase/)

### 非持久化 - 分布式缓存/消息系统
- [Kafka](/2017/04/01/kafka/)
- [Redis]

## 分布式计算框架
  - [YARN分布式计算框架](/2017/03/26/yarn/)
  - [YARN应用开发的几种方式](/2017/03/28/yarn-appdev/)
  - [Running Spark on YARN](/2017/04/01/running-spark-on-yarn/)
  - [Spark Big Data Analytics](spark.md)
