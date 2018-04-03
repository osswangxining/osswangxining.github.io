---
title: Kubernetes
date: 2017-2-12 20:46:25
categories:
  - 分布式&云计算
  - Kubernetes
tags:
  - 分布式
  - Kubernetes
  - container
  - 容器
  - PaaS
---

## 基础架构
Kubernetes将底层的计算资源连接在一起对外体现为一个计算集群，并将资源高度抽象化。部署应用时Kubernetes会以更高效的方式自动的将应用分发到集群内的机器上面，并调度运行。

Kubernetes集群包含两种类型的资源：
- Master节点：协调控制整个集群。Master负责管理整个集群，协调集群内的所有行为。比如调度应用，监控应用的状态等。
- Nodes节点：运行应用的工作节点。Node节点负责运行应用，一般是一台物理机或者虚机。每个Node节点上面都有一个Kubelet，它是一个代理程序，用来管理该节点以及和Master节点通信。除此以外，Node节点上还会有一些管理容器的工具，比如Docker或者rkt等。生产环境中一个Kubernetes集群至少应该包含三个Nodes节点。

当部署应用的时候，我们通知Master节点启动应用容器。然后Master会调度这些应用将它们运行在Node节点上面。Node节点和Master节点通过Master节点暴露的Kubernetes API通信。当然我们也可以直接通过这些API和集群交互。
![kubernetes Cluster](http://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_01_cluster.svg)
<!-- more -->
### Master
Master节点上面主要由四个模块组成：APIServer、scheduler、controller manager、etcd。
- APIServer
APIServer负责对外提供RESTful的Kubernetes API服务，它是系统管理指令的统一入口，任何对资源进行增删改查的操作都要交给APIServer处理后再提交给etcd。如架构图中所示，kubectl（Kubernetes提供的客户端工具，该工具内部就是对Kubernetes API的调用）是直接和APIServer交互的。
- Scheduler
Scheduler的职责很明确，就是负责调度pod到合适的Node上。如果把scheduler看成一个黑匣子，那么它的输入是pod和由多个Node组成的列表，输出是Pod和一个Node的绑定，即将这个pod部署到这个Node上。Kubernetes目前提供了调度算法，但是同样也保留了接口，用户可以根据自己的需求定义自己的调度算法。
- Controller manager
如果说APIServer做的是“前台”的工作的话，那controller manager就是负责“后台”的。每个资源一般都对应有一个控制器，而controller manager就是负责管理这些控制器的。比如我们通过APIServer创建一个pod，当这个pod创建成功后，APIServer的任务就算完成了。而后面保证Pod的状态始终和我们预期的一样的重任就由controller manager去保证了。
- etcd
etcd是一个高可用的键值存储系统，Kubernetes使用它来存储各个资源的状态，从而实现了Restful的API。

### Node
每个Node节点主要由三个模块组成：kubelet、kube-proxy、runtime。
- kubelet。Kubelet是Master在每个Node节点上面的agent，是Node节点上面最重要的模块，它负责维护和管理该Node上面的所有容器，但是如果容器不是通过Kubernetes创建的，它并不会管理。本质上，它负责使Pod得运行状态与期望的状态一致。
- kube-proxy。该模块实现了Kubernetes中的服务发现和反向代理功能。反向代理方面：kube-proxy支持TCP和UDP连接转发，默认基于Round Robin算法将客户端流量转发到与service对应的一组后端pod。服务发现方面，kube-proxy使用etcd的watch机制，监控集群中service和endpoint对象数据的动态变化，并且维护一个service到endpoint的映射关系，从而保证了后端pod的IP变化不会对访问者造成影响。另外kube-proxy还支持session affinity。
- runtime。runtime指的是容器运行环境，目前Kubernetes支持docker和rkt两种容器。


## 搭建MiniKube
环境: MacOS, virtualbox, minikube v0.18.0, kubectl v1.6.0

Kubernetes提供了一个轻量级的Minikube应用，利用它我们可以很容器的创建一个只包含一个Node节点的Kubernetes Cluster用于日常的开发测试。

### 安装
Minikube的安装可以参考: Minikube的Github：https://github.com/kubernetes/minikube

要正常使用，还必须安装kubectl，并且放在PATH里面。kubectl是一个通过Kubernetes API和Kubernetes集群交互的命令行工具。

### 启动
可以通过minikube查看cluster运行状态,启动或者停止cluster.例如:

```
minikube start
```

### ONLY FOR CHINESE
Kubernetes在部署容器应用的时候会先拉一个pause镜像，这个是一个基础容器，主要是负责网络部分的功能的，具体这里不展开讨论。最关键的是Kubernetes里面镜像默认都是从Google的镜像仓库拉的（就跟docker默认从docker hub拉的一样），但是因为GFW的原因，中国用户是访问不了Google的镜像仓库gcr.io的（如果你可以ping通，那恭喜你）。庆幸的是这个镜像被传到了docker hub上面，虽然中国用户访问后者也非常艰难，但通过一些加速器之类的还是可以pull下来的。如果没有VPN等科学上网的工具的话，请先做如下操作：

See: https://github.com/kubernetes/kubernetes/issues/6888

```
minikube ssh    # 登录到我们的Kubernetes VM里面去
docker pull registry.hnaresearch.com/public/pause-amd64:3.0  
docker tag registry.hnaresearch.com/public/pause-amd64:3.0 gcr.io/google_containers/pause-amd64:3.0  
```
这样Kubernetes VM就不会从gcr.io拉镜像了，而是会直接使用本地的镜像。

## 部署应用

在Kubernetes Cluster上面部署应用，我们需要先创建一个Kubernetes Deployment。这个Deployment负责创建和更新我们的应用实例。当这个Deployment创建之后，Kubernetes master就会将这个Deployment创建出来的应用实例部署到集群内某个Node节点上。而且自应用实例创建后，Deployment controller还会持续监控应用，直到应用被删除或者部署应用的Node节点不存在。
>A Deployment is responsible for creating and updating instances of your application.

![](http://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_02_first_app.svg)

使用kubectl来创建Deployment，创建的时候需要制定容器镜像以及我们要启动的个数（replicas），当然这些信息后面可以再更新。这里我用Go写了一个简单的Webserver，返回“Hello World”，监听端口是8090.我们就来启动这个应用.
```
kubectl run helloworld --image=registry.hnaresearch.com/public/hello-world:v1.0 --port=8090
```
执行后master寻找一个合适的node来部署我们的应用实例（我们只有一个node）。我们可以使用kubectl get deployment来查看我们创建的Deployment：
```
kubectl get deployment
```
默认应用部署好之后是只在Kubernetes Cluster内部可见的，有多种方法可以让我们的应用暴露到外部，这里先介绍一种简单的：我们可以通过kubectl proxy命令在我们的终端和Kubernetes Cluster直接创建一个代理。然后，打开一个新的终端，通过Pod名(Pod后面会有讲到，可以通过kubectl get pod查看Pod名字)就可以访问了：
```
xis-macbook-pro:~ xiningwang$ kubectl get pod
NAME                             READY     STATUS             RESTARTS   AGE
hello-minikube-938614450-xjl4s   0/1       ImagePullBackOff   0          15h
helloworld-2790924137-bvfhn      1/1       Running            0          2m

xis-macbook-pro:~ xiningwang$ curl http://localhost:8001/api/v1/proxy/namespaces/default/pods/helloworld-2790924137-bvfhn/
Hello world !
hostname:helloworld-2790924137-bvfhn
```

## Pod
Pod是Kubernetes中一个非常重要的概念，也是区别于其他编排系统的一个设计. Deployment执行时并不是直接创建了容器实例，而是先在Node上面创建了Pod，然后再在Pod里面创建容器。那Pod到底是什么？Pod是Kubernetes里面抽象出来的一个概念，它是能够被创建、调度和管理的最小单元；每个Pod都有一个独立的IP；一个Pod由若干个容器构成。一个Pod之内的容器共享Pod的所有资源，这些资源主要包括：共享存储（以Volumes的形式）、共享网络、共享端口等。Kubernetes虽然也是一个容器编排系统，但不同于其他系统，它的最小操作单元不是单个容器，而是Pod。这个特性给Kubernetes带来了很多优势，比如最显而易见的是同一个Pod内的容器可以非常方便的互相访问（通过localhost就可以访问）和共享数据。

> A Pod is a group of one or more application containers (such as Docker or rkt) and includes shared storage (volumes), IP address and information about how to run them.

![pod](http://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_03_pods.svg)

>A Node is a group of one ore more pods and includes the kubelet and container engine.  
>
>Containers should only be scheduled together in a single Pod if they are tightly coupled and need to share resources such as disk.

![node](http://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_03_nodes.svg)

## Job
从程序的运行形态上来区分，我们可以将Pod分为两类：长时运行服务（jboss、mysql等）和一次性任务（数据计算、测试）。Replication Controller创建的Pod都是长时运行的服务，而Job创建的Pod都是一次性任务。

在Job的定义中，restartPolicy（重启策略）只能是Never和OnFailure。Job可以控制一次性任务的Pod的完成次数（Job-->spec-->completions）和并发执行数（Job-->spec-->parallelism），当Pod成功执行指定次数后，即认为Job执行完毕。

## Replication Controller
Replication Controller（RC）是Kubernetes中的另一个核心概念，应用托管在Kubernetes之后，Kubernetes需要保证应用能够持续运行，这是RC的工作内容，它会确保任何时间Kubernetes中都有指定数量的Pod在运行。在此基础上，RC还提供了一些更高级的特性，比如滚动升级、升级回滚等。

### Replica Set
新一代副本控制器replica set，可以被认为 是“升级版”的Replication Controller。也就是说。replica set也是用于保证与label selector匹配的pod数量维持在期望状态。区别在于，replica set引入了对基于子集的selector查询条件，而Replication Controller仅支持基于值相等的selecto条件查询。这是目前从用户角度肴，两者唯一的显著差异。 社区引入这一API的初衷是用于取代vl中的Replication Controller，也就是说．当v1版本被废弃时，Replication Controller就完成了它的历史使命，而由replica set来接管其工作。虽然replica set可以被单独使用，但是目前它多被Deployment用于进行pod的创建、更新与删除。

## Service
### 原理
[Service](id:service)是Kubernetes里面抽象出来的一层，它定义了由多个Pods组成的逻辑组（logical set），可以对组内的Pod做一些事情：
- 对外暴露流量
- 做负载均衡（load balancing）
- 服务发现（service-discovery）

> A Kubernetes Service is an abstraction layer which defines a logical set of Pods and enables external traffic exposure, load balancing and service discovery for those Pods.

在Kubernetes中，在受到Replication Controller调控的时候，Pod副本是变化的，对于的虚拟IP也是变化的，比如发生迁移或者伸缩的时候。这对于Pod的访问者来说是不可接受的。Kubernetes中的Service是一种抽象概念，它定义了一个Pod逻辑集合以及访问它们的策略，Service同Pod的关联同样是居于Label来完成的。Service的目标是提供一种桥梁， 它会为访问者提供一个固定访问地址，用于在访问时重定向到相应的后端，这使得非 Kubernetes原生应用程序，在无须为Kubemces编写特定代码的前提下，轻松访问后端。

Service同RC一样，都是通过Label来关联Pod的。当你在Service的yaml文件中定义了该Service的selector中的label为app:my-web，那么这个Service会将Pod-->metadata-->labeks中label为app:my-web的Pod作为分发请求的后端。当Pod发生变化时（增加、减少、重建等），Service会及时更新。这样一来，Service就可以作为Pod的访问入口，起到代理服务器的作用，而对于访问者来说，通过Service进行访问，无需直接感知Pod。

需要注意的是，Kubernetes分配给Service的固定IP是一个虚拟IP，并不是一个真实的IP，在外部是无法寻址的。真实的系统实现上，Kubernetes是通过Kube-proxy组件来实现的虚拟IP路由及转发。所以在之前集群部署的环节上，我们在每个Node上均部署了Proxy这个组件，从而实现了Kubernetes层级的虚拟转发网络。

### Service代理服务

![service](http://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_04_services.svg)


使用kubectl get service可以查看目前已有的service，Minikube默认创建了一个kubernetes Service。我们使用expose命令再创建一个Service：
```
xis-macbook-pro:~ xiningwang$ kubectl get service
NAME             CLUSTER-IP   EXTERNAL-IP   PORT(S)          AGE
hello-minikube   10.0.0.188   <nodes>       8080:32710/TCP   16h
kubernetes       10.0.0.1     <none>        443/TCP          16h
xis-macbook-pro:~ xiningwang$ kubectl expose deployment/helloworld --type="NodePort" --port 8090
service "helloworld" exposed
xis-macbook-pro:~ xiningwang$ kubectl get service
NAME             CLUSTER-IP   EXTERNAL-IP   PORT(S)          AGE
hello-minikube   10.0.0.188   <nodes>       8080:32710/TCP   16h
helloworld       10.0.0.249   <nodes>       8090:31240/TCP   2m
kubernetes       10.0.0.1     <none>        443/TCP          16h
xis-macbook-pro:~ xiningwang$ kubectl delete service hello-minikube
service "hello-minikube" deleted
xis-macbook-pro:~ xiningwang$ kubectl get service
NAME         CLUSTER-IP   EXTERNAL-IP   PORT(S)          AGE
helloworld   10.0.0.249   <nodes>       8090:31240/TCP   2m
kubernetes   10.0.0.1     <none>        443/TCP          16h
xis-macbook-pro:~ xiningwang$ kubectl describe service/helloworld
Name:  			helloworld
Namespace:     		default
Labels:			run=helloworld
Annotations:   		<none>
Selector:      		run=helloworld
Type:  			NodePort
IP:    			10.0.0.249
Port:  			<unset>	8090/TCP
NodePort:      		<unset>	31240/TCP
Endpoints:     		172.17.0.3:8090
Session Affinity:      	None
Events:			<none>
xis-macbook-pro:~ xiningwang$ minikube docker-env
export DOCKER_TLS_VERIFY="1"
export DOCKER_HOST="tcp://192.168.99.100:2376"
export DOCKER_CERT_PATH="/Users/xiningwang/.minikube/certs"
export DOCKER_API_VERSION="1.23"
# Run this command to configure your shell:
# eval $(minikube docker-env)
xis-macbook-pro:~ xiningwang$ curl http://192.168.99.100:31240
Hello world !
hostname:helloworld-2790924137-bvfhn
```

### Service内部负载均衡
当Service的Endpoints包含多个IP的时候，及服务代理存在多个后端，将进行请求的负载均衡。默认的负载均衡策略是轮训或者随机（有kube-proxy的模式决定）。同时，Service上通过设置Service-->spec-->sessionAffinity=ClientIP，来实现基于源IP地址的会话保持。

### 发布Service
Service的虚拟IP是由Kubernetes虚拟出来的内部网络，外部是无法寻址到的。但是有些服务又需要被外部访问到，例如web前段。这时候就需要加一层网络转发，即外网到内网的转发。Kubernetes提供了NodePort、LoadBalancer、Ingress三种方式。
- NodePort
在之前的Guestbook示例中，已经延时了NodePort的用法。NodePort的原理是，Kubernetes会在每一个Node上暴露出一个端口：nodePort，外部网络可以通过（任一Node）[NodeIP]:[NodePort]访问到后端的Service。Minikube只支持这种方式。
- LoadBalancer
在NodePort基础上，Kubernetes可以请求底层云平台创建一个负载均衡器，将每个Node作为后端，进行服务分发。该模式需要底层云平台（例如GCE）支持。
- Ingress
是一种HTTP方式的路由转发机制，由Ingress Controller和HTTP代理服务器组合而成。Ingress Controller实时监控Kubernetes API，实时更新HTTP代理服务器的转发规则。HTTP代理服务器有GCE Load-Balancer、HaProxy、Nginx等开源方案。

### 自发性
Kubernetes中有一个很重要的服务自发现特性。一旦一个service被创建，该service的service IP和service port等信息都可以被注入到pod中供它们使用。Kubernetes主要支持两种service发现 机制：环境变量和DNS。
- 环境变量方式
Kubernetes创建Pod时会自动添加所有可用的service环境变量到该Pod中，如有需要．这些环境变量就被注入Pod内的容器里。需要注意的是，环境变量的注入只发送在Pod创建时，且不会被自动更新。这个特点暗含了service和访问该service的Pod的创建时间的先后顺序，即任何想要访问service的pod都需要在service已经存在后创建，否则与service相关的环境变量就无法注入该Pod的容器中，这样先创建的容器就无法发现后创建的service。
- DNS方式
Kubernetes集群现在支持增加一个可选的组件——DNS服务器。这个DNS服务器使用Kubernetes的watchAPI，不间断的监测新的service的创建并为每个service新建一个DNS记录。如果DNS在整个集群范围内都可用，那么所有的Pod都能够自动解析service的域名。

### 其他
- 多个service如何避免地址和端口冲突
此处设计思想是，Kubernetes通过为每个service分配一个唯一的ClusterIP，所以当使用ClusterIP：port的组合访问一个service的时候，不管port是什么，这个组合是一定不会发生重复的。另一方面，kube-proxy为每个service真正打开的是一个绝对不会重复的随机端口，用户在service描述文件中指定的访问端口会被映射到这个随机端口上。这就是为什么用户可以在创建service时随意指定访问端口。

- 目前存在的不足
Kubernetes使用iptables和kube-proxy解析service的入口地址，在中小规模的集群中运行良好，但是当service的数量超过一定规模时，仍然有一些小问题。首当其冲的便是service环境变量泛滥，以及service与使用service的pod两者创建时间先后的制约关系。目前来看，很多使用者在使用Kubernetes时往往会开发一套自己的Router组件来替代service，以便更好地掌控和定制这部分功能。

## Label
Service就是靠Label选择器（Label Selectors）来匹配组内的Pod的，而且很多命令都可以操作Label。Label是绑定在对象上（比如Pod）的键值对，主要用来把一些相关的对象组织在一起，并且对于用户来说label是有含义的，比如：
- Production environment (production, test, dev)
- Application version (beta, v1.3)
- Type of service/server (frontend, backend, database)

>    Labels are key/value pairs that are attached to objects

![Label](http://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_04_labels.svg)

### Pod创建时默认创建的Label
```
xis-macbook-pro:~ xiningwang$ kubectl describe pod helloworld-2790924137-bvfhn
Name:  		helloworld-2790924137-bvfhn
Namespace:     	default
Node:  		minikube/192.168.99.100
Start Time:    	Fri, 05 May 2017 14:03:56 +0800
Labels:		pod-template-hash=2790924137
       		run=helloworld
```

### 新增一个Label

```
xis-macbook-pro:~ xiningwang$ kubectl label pod helloworld-2790924137-bvfhn app=v1
pod "helloworld-2790924137-bvfhn" labeled
xis-macbook-pro:~ xiningwang$ kubectl describe pod helloworld-2790924137-bvfhn
Name:  		helloworld-2790924137-bvfhn
Namespace:     	default
Node:  		minikube/192.168.99.100
Start Time:    	Fri, 05 May 2017 14:03:56 +0800
Labels:		app=v1
       		pod-template-hash=2790924137
       		run=helloworld
```

### 使用Label的例子

```
xis-macbook-pro:~ xiningwang$ kubectl get service
NAME         CLUSTER-IP   EXTERNAL-IP   PORT(S)          AGE
helloworld   10.0.0.249   <nodes>       8090:31240/TCP   46m
kubernetes   10.0.0.1     <none>        443/TCP          17h
xis-macbook-pro:~ xiningwang$ kubectl get service  -l run=helloworld
NAME         CLUSTER-IP   EXTERNAL-IP   PORT(S)          AGE
helloworld   10.0.0.249   <nodes>       8090:31240/TCP   46m
xis-macbook-pro:~ xiningwang$ kubectl get pod -l app=v1
NAME                          READY     STATUS    RESTARTS   AGE
helloworld-2790924137-bvfhn   1/1       Running   0          1h
xis-macbook-pro:~ xiningwang$ kubectl get pod
NAME                             READY     STATUS             RESTARTS   AGE
hello-minikube-938614450-b7xwm   0/1       ImagePullBackOff   0          38m
helloworld-2790924137-bvfhn      1/1       Running            0          1h
```
## Pet Sets/StatefulSet
K8s在1.3版本里发布了Alpha版的PetSet功能。在云原生应用的体系里，有下面两组近义词；第一组是无状态（stateless）、牲畜（cattle）、无名（nameless）、可丢弃（disposable）；第二组是有状态（stateful）、宠物（pet）、有名（having name）、不可丢弃（non-disposable）。RC和RS主要是控制提供无状态服务的，其所控制的Pod的名字是随机设置的，一个Pod出故障了就被丢弃掉，在另一个地方重启一个新的Pod，名字变了、名字和启动在哪儿都不重要，重要的只是Pod总数；而PetSet是用来控制有状态服务，PetSet中的每个Pod的名字都是事先确定的，不能更改。PetSet中Pod的名字的作用，是用来关联与该Pod对应的状态。

对于RC和RS中的Pod，一般不挂载存储或者挂载共享存储，保存的是所有Pod共享的状态，Pod像牲畜一样没有分别；对于PetSet中的Pod，每个Pod挂载自己独立的存储，如果一个Pod出现故障，从其他节点启动一个同样名字的Pod，要挂在上原来Pod的存储继续以它的状态提供服务。

适合于PetSet的业务包括数据库服务MySQL和PostgreSQL，集群化管理服务Zookeeper、etcd等有状态服务。PetSet的另一种典型应用场景是作为一种比普通容器更稳定可靠的模拟虚拟机的机制。传统的虚拟机正是一种有状态的宠物，运维人员需要不断地维护它，容器刚开始流行时，我们用容器来模拟虚拟机使用，所有状态都保存在容器里，而这已被证明是非常不安全、不可靠的。使用PetSet，Pod仍然可以通过漂移到不同节点提供高可用，而存储也可以通过外挂的存储来提供高可靠性，PetSet做的只是将确定的Pod与确定的存储关联起来保证状态的连续性。

## Volume
在Docker的设计实现中，容器中的数据是临时的，即当容器被销毁时，其中的数据将会丢失。如果需要持久化数据，需要使用Docker数据卷挂载宿主机上的文件或者目录到容器中。在Kubernetes中，当Pod重建的时候，数据是会丢失的，Kubernetes也是通过数据卷挂载来提供Pod数据的持久化的。Kubernetes数据卷是对Docker数据卷的扩展，Kubernetes数据卷是Pod级别的，可以用来实现Pod中容器的文件共享。目前，Kubernetes支持的数据卷类型如下：
-  本地数据卷
EmptyDir、HostPath这两种类型的数据卷，只能最用于本地文件系统。本地数据卷中的数据只会存在于一台机器上，所以当Pod发生迁移的时候，数据便会丢失。该类型Volume的用途是：Pod中容器间的文件共享、共享宿主机的文件系统。
- 网络数据卷
Kubernetes提供了很多类型的数据卷以集成第三方的存储系统，包括一些非常流行的分布式文件系统，也有在IaaS平台上提供的存储支持，这些存储系统都是分布式的，通过网络共享文件系统，因此我们称这一类数据卷为网络数据卷。
网络数据卷能够满足数据的持久化需求，Pod通过配置使用网络数据卷，每次Pod创建的时候都会将存储系统的远端文件目录挂载到容器中，数据卷中的数据将被水久保存，即使Pod被删除，只是除去挂载数据卷，数据卷中的数据仍然保存在存储系统中，且当新的Pod被创建的时候，仍是挂载同样的数据卷。网络数据卷包含以下几种：NFS、iSCISI、GlusterFS、RBD（Ceph Block Device）、Flocker、AWS Elastic Block Store、GCE Persistent Disk.
- 信息数据卷
Kubernetes中有一些数据卷，主要用来给容器传递配置信息，我们称之为信息数据卷，比如Secret（处理敏感配置信息，密码、Token等）、Downward API（通过环境变量的方式告诉容器Pod的信息）、Git Repo（将Git仓库下载到Pod中），都是将Pod的信息以文件形式保存，然后以数据卷方式挂载到容器中，容器通过读取文件获取相应的信息。

## Deployment
Kubernetes提供了一种更加简单的更新RC和Pod的机制，叫做Deployment。通过在Deployment中描述你所期望的集群状态，Deployment Controller会将现在的集群状态在一个可控的速度下逐步更新成你所期望的集群状态。Deployment主要职责同样是为了保证pod的数量和健康，90%的功能与Replication Controller完全一样，可以看做新一代的Replication Controller。但是，它又具备了Replication Controller之外的新特性：
- Replication Controller全部功能：Deployment继承了上面描述的Replication Controller全部功能。
- 事件和状态查看：可以查看Deployment的升级详细进度和状态。
- 回滚：当升级pod镜像或者相关参数的时候发现问题，可以使用回滚操作回滚到上一个稳定的版本或者指定的版本。
- 版本记录: 每一次对Deployment的操作，都能保存下来，给予后续可能的回滚使用。
- 暂停和启动：对于每一次升级，都能够随时暂停和启动。
- 多种升级方案：Recreate----删除所有已存在的pod,重新创建新的; RollingUpdate----滚动升级，逐步替换的策略，同时滚动升级时，支持更多的附加参数，例如设置最大不可用pod数量，最小升级间隔时间等等。


### Scale
随着流量的增加，我们可能需要增加我们应用的规模来满足用户的需求。Kubernetes的Scale功能就可以实现这个需求。
>    Scaling is accomplished by changing the number of replicas in a Deployment.

扩大应用的规模时，Kubernetes将会在Nodes上面使用可用的资源来创建新的Pod，并运行新增加的应用，缩小规模时做相反的操作。Kubernetes也支持自动规模化Pod。当然我们也可以将应用的数量变为0，这样就会终止所有部署该应用的Pods。应用数量增加后，Service内的负载均衡就会变得非常有用了.

![scale](https://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_05_scaling1.svg)


![scale](https://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_05_scaling2.svg)

```
xis-macbook-pro:~ xiningwang$ kubectl get deployment
NAME         DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
helloworld   1         1         1            1           2h
```
可以看到，现在我们只有一个Pod，
- DESIRED字段表示我们配置的replicas的个数，即实例的个数。
- CURRENT字段表示目前处于running状态的replicas的个数。
- UP-TO-DATE字段表示表示和预先配置的期望状态相符的replicas的个数。
- AVAILABLE字段表示目前实际对用户可用的replicas的个数。

下面我们使用kubectl scale命令将启动4个复制品，语法规则是kubectl scale deployment-type name replicas-number：
```
xis-macbook-pro:~ xiningwang$ kubectl scale deployment/helloworld --replicas=4
deployment "helloworld" scaled
xis-macbook-pro:~ xiningwang$ kubectl get deployment
NAME         DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
helloworld   4         4         4            4           2h

xis-macbook-pro:~ xiningwang$ kubectl get pod -o wide
NAME                          READY     STATUS    RESTARTS   AGE       IP           NODE
helloworld-2790924137-2kg70   1/1       Running   0          3m        172.17.0.4   minikube
helloworld-2790924137-bvfhn   1/1       Running   0          2h        172.17.0.3   minikube
helloworld-2790924137-jg15m   1/1       Running   0          3m        172.17.0.5   minikube
helloworld-2790924137-tgqr9   1/1       Running   0          3m        172.17.0.2   minikube
```
验证一下这个Service是有负载均衡的：
```
xis-macbook-pro:~ xiningwang$ kubectl get deployment
NAME         DESIRED   CURRENT   UP-TO-DATE   AVAILABLE   AGE
helloworld   4         4         4            4           2h
xis-macbook-pro:~ xiningwang$ curl 192.168.99.100:31240
Hello world !
hostname:helloworld-2790924137-bvfhn
xis-macbook-pro:~ xiningwang$ curl 192.168.99.100:31240
Hello world !
hostname:helloworld-2790924137-tgqr9
xis-macbook-pro:~ xiningwang$ curl 192.168.99.100:31240
Hello world !
hostname:helloworld-2790924137-2kg70
xis-macbook-pro:~ xiningwang$ curl 192.168.99.100:31240
Hello world !
hostname:helloworld-2790924137-bvfhn
xis-macbook-pro:~ xiningwang$ curl 192.168.99.100:31240
Hello world !
hostname:helloworld-2790924137-tgqr9
```
### Rolling Update
滚动更新（Rolling update）特性的好处就是我们不用停止服务就可以实现应用更新。默认更新的时候是一个Pod一个Pod更新的，所以整个过程服务不会中断。当然你也可以设置一次更新的Pod的百分比。而且更新过程中，Service只会将流量转发到可用的节点上面。更加重要的是，我们可以随时回退到旧版本。
>Rolling updates allow Deployments' update to take place with zero downtime by incrementally updating Pods instances with new ones.
If a Deployment is exposed publicly, the Service will load-balance the traffic only to available Pods during the update.

![rollingupdate](https://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_06_rollingupdates1.svg)
![rollingupdate](https://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_06_rollingupdates2.svg)
![rollingupdate](https://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_06_rollingupdates3.svg)
![rollingupdate](https://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_06_rollingupdates4.svg)

### Set image
在原来程序的基础上，多输出一个v2作为新版本，使用set image命令指定新版本镜像.
```
xis-macbook-pro:~ xiningwang$ kubectl set image deployments/helloworld helloworld=registry.hnaresearch.com/public/hello-world:v2.0
deployment "helloworld" image updated
xis-macbook-pro:~ xiningwang$ kubectl get pods
NAME                          READY     STATUS              RESTARTS   AGE
helloworld-2790924137-2kg70   1/1       Running             0          54m
helloworld-2790924137-bvfhn   1/1       Running             0          3h
helloworld-2790924137-tgqr9   1/1       Running             0          54m
helloworld-2889228138-65lmj   0/1       ContainerCreating   0          9m
helloworld-2889228138-q68vx   0/1       ContainerCreating   0          9m
xis-macbook-pro:~ xiningwang$ kubectl get pods
NAME                          READY     STATUS              RESTARTS   AGE
helloworld-2790924137-2kg70   1/1       Terminating         0          54m
helloworld-2790924137-bvfhn   1/1       Running             0          3h
helloworld-2790924137-tgqr9   0/1       Terminating         0          54m
helloworld-2889228138-65lmj   0/1       ContainerCreating   0          9m
helloworld-2889228138-bj38m   0/1       Pending             0          9m
helloworld-2889228138-dv3ch   1/1       Running             0          9m
helloworld-2889228138-q68vx   1/1       Running             0          9m
xis-macbook-pro:~ xiningwang$ kubectl get pods
NAME                          READY     STATUS    RESTARTS   AGE
helloworld-2889228138-65lmj   1/1       Running   0          10m
helloworld-2889228138-bj38m   1/1       Running   0          10m
helloworld-2889228138-dv3ch   1/1       Running   0          10m
helloworld-2889228138-q68vx   1/1       Running   0          10m
```
### Rollout undo
使用kubectl rollout undo命令回滚到之前的版本：
```
xis-macbook-pro:~ xiningwang$ kubectl rollout undo deployment/helloworld
deployment "helloworld" rolled back
```

## Autoscaling
系统能够根据负载的变化对计算资源的分配进行自动的扩增或者收缩，无疑是一个非常吸引人的特征，它能够最大可能地减少费用或者其他代价（如电力损耗）。自动扩展主要分为两种，其一为水平扩展，针对于实例数目的增减；其二为垂直扩展，即单个实例可以使用的资源的增减。Horizontal Pod Autoscaler（HPA）属于前者。

Horizontal Pod Autoscaler的操作对象是Replication Controller、ReplicaSet或Deployment对应的Pod，根据观察到的CPU实际使用量与用户的期望值进行比对，做出是否需要增减实例数量的决策。controller目前使用heapSter来检测CPU使用量，检测周期默认是30秒。

### 决策策略
在HPA Controller检测到CPU的实际使用量之后，会求出当前的CPU使用率（实际使用量与pod 请求量的比率)。然后，HPA Controller会通过调整副本数量使得CPU使用率尽量向期望值靠近．

另外，考虑到自动扩展的决策可能需要一段时间才会生效，甚至在短时间内会引入一些噪声．
- 例如当pod所需要的CPU负荷过大，从而运行一个新的pod进行分流，在创建的过程中，系统的CPU使用量可能会有一个攀升的过程。所以，在每一次作出决策后的一段时间内，将不再进行扩展决策。对于ScaleUp而言，这个时间段为3分钟，Scaledown为5分钟。
- 再者HPA Controller允许一定范围内的CPU使用量的不稳定，也就是说，只有当aVg（CurrentPodConsumption／Target低于0.9或者高于1.1时才进行实例调整，这也是出于维护系统稳定性的考虑。
