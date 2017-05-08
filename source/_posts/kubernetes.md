---
title: Kubernetes
---

## MiniKube
环境: MacOS, virtualbox, minikube v0.18.0, kubectl v1.6.0

Kubernetes将底层的计算资源连接在一起对外体现为一个计算集群，并将资源高度抽象化。部署应用时Kubernetes会以更高效的方式自动的将应用分发到集群内的机器上面，并调度运行。

## 搭建Kubernetes集群
Kubernetes集群包含两种类型的资源：
- Master节点：协调控制整个集群。Master负责管理整个集群，协调集群内的所有行为。比如调度应用，监控应用的状态等。
- Nodes节点：运行应用的工作节点。Node节点负责运行应用，一般是一台物理机或者虚机。每个Node节点上面都有一个Kubelet，它是一个代理程序，用来管理该节点以及和Master节点通信。除此以外，Node节点上还会有一些管理容器的工具，比如Docker或者rkt等。生产环境中一个Kubernetes集群至少应该包含三个Nodes节点。

当部署应用的时候，我们通知Master节点启动应用容器。然后Master会调度这些应用将它们运行在Node节点上面。Node节点和Master节点通过Master节点暴露的Kubernetes API通信。当然我们也可以直接通过这些API和集群交互。
![kubernetes Cluster](http://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_01_cluster.svg)

Kubernetes提供了一个轻量级的Minikube应用，利用它我们可以很容器的创建一个只包含一个Node节点的Kubernetes Cluster用于日常的开发测试。

### 安装
Minikube的安装可以参考: Minikube的Github：https://github.com/kubernetes/minikube

要正常使用，还必须安装kubectl，并且放在PATH里面。kubectl是一个通过Kubernetes API和Kubernetes集群交互的命令行工具。

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

## Service
通过proxy实现集群外部可以访问的方式是不太适用于实际生产环境的。[Service](id:service)是Kubernetes里面抽象出来的一层，它定义了由多个Pods组成的逻辑组（logical set），可以对组内的Pod做一些事情：
- 对外暴露流量
- 做负载均衡（load balancing）
- 服务发现（service-discovery）

> A Kubernetes Service is an abstraction layer which defines a logical set of Pods and enables external traffic exposure, load balancing and service discovery for those Pods.

而且每个Service都有一个集群内唯一的私有IP和对外的端口，用于接收流量。如果我们想将一个Service暴露到集群外，有两种方法：
- LoadBalancer - 提供一个公网的IP
- NodePort - 使用NAT将Service的端口暴露出去。Minikube只支持这种方式。

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
## Scale
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
## Rolling Update
滚动更新（Rolling update）特性的好处就是我们不用停止服务就可以实现应用更新。默认更新的时候是一个Pod一个Pod更新的，所以整个过程服务不会中断。当然你也可以设置一次更新的Pod的百分比。而且更新过程中，Service只会将流量转发到可用的节点上面。更加重要的是，我们可以随时回退到旧版本。
>Rolling updates allow Deployments' update to take place with zero downtime by incrementally updating Pods instances with new ones.
If a Deployment is exposed publicly, the Service will load-balance the traffic only to available Pods during the update.

![rollingupdate](https://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_06_rollingupdates1.svg)
![rollingupdate](https://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_06_rollingupdates2.svg)
![rollingupdate](https://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_06_rollingupdates3.svg)
![rollingupdate](https://kubernetes.io/docs/tutorials/kubernetes-basics/public/images/module_06_rollingupdates4.svg)

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

使用kubectl rollout undo命令回滚到之前的版本：
```
xis-macbook-pro:~ xiningwang$ kubectl rollout undo deployment/helloworld
deployment "helloworld" rolled back
```
