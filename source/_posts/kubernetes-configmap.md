---
title: Kubernetes ConfigMap
date: 2017-3-9 20:46:25
categories:
  - 分布式&云计算
  - Kubernetes
tags:
  - 分布式
  - Kubernetes
  - 路由
  - 容器
  - ConfigMap
---

## 相关术语
很多应用程序的配置需要通过配置文件，命令行参数和环境变量的组合配置来完成。这些配置应该从image内容中解耦，以此来保持容器化应用程序的便携性。ConfigMap API资源提供了将配置数据注入容器的方式，同时保持容器是不知道Kubernetes的。ConfigMap可以被用来保存单个属性，也可以用来保存整个配置文件或者JSON二进制大对象。

>kubernetes通过ConfigMap来实现对容器中应用的配置管理。

从数据角度来看，ConfigMap的类型只是键值组。应用可以从不同角度来配置，所以关于给用户如何存储和使用配置数据，我们需要给他们一些弹性。在一个pod里面使用ConfigMap大致有三种方式：
- 环境变量
- 命令行参数
- 数据卷文件

## 创建ConfigMap
创建ConfigMap的方式有两种，一种是通过yaml文件来创建，另一种是通过kubectl直接在命令行下创建。

### yaml
在yaml文件中，配置文件以key-value键值对的形式保存，当然也可以直接放一个完整的配置文件，在下面的示例中，cache_hst、cache_port、cache_prefix即是key-value键值对，而app.properties和my.cnf都是配置文件：
```
apiVersion: v1
kind: ConfigMap
metadata:
  name: test-cfg
  namespace: default
data:
  cache_host: memcached-gcxt
  cache_port: "11211"
  cache_prefix: gcxt
  my.cnf: |
    [mysqld]
    log-bin = mysql-bin
  app.properties: |
    property.1 = value-1
 property.2 = value-2
 property.3 = value-3
 ```

创建ConfigMap：
```
kubectl create -f test-cfg.yml
```

### kubectl
直接将一个目录下的所有配置文件创建为一个ConfigMap：
```
kubectl create configmap test-config --from-file=./configs
```

直接将一个配置文件创建为一个ConfigMap：
```
kubectl create configmap test-config2 --from-file=./configs/db.conf --from-file=./configs/cache.conf
```

在使用kubectl创建的时候，通过在命令行直接传递键值对创建：
```
kubectl create configmap test-config3 --from-literal=db.host=10.5.10.116 --from-listeral=db.port='3306'
```
可以通过如下方式查看创建的ConfigMap：
```
kubectl get configmaps
kubectl get configmap test-config -o yaml
kubectl describe configmap test-config
```

## 使用ConfigMap
### 环境变量
通过环境变量的方式，直接传递pod.
ConfigMap文件：
```
apiVersion: v1
kind: ConfigMap
metadata:
  name: special-config
  namespace: default
data:
  special.how: very
  special.type: charm
```

第一个pod示例：
```
apiVersion: v1
kind: Pod
metadata:
  name: dapi-test-pod
spec:
  containers:
    - name: test-container
      image: gcr.io/google_containers/busybox
      command: [ "/bin/sh", "-c", "env" ]
      env:
        - name: SPECIAL_LEVEL_KEY
          valueFrom:
            configMapKeyRef:
              name: special-config
              key: special.how
        - name: SPECIAL_TYPE_KEY
          valueFrom:
            configMapKeyRef:
              name: special-config
              key: special.type
  restartPolicy: Never
```

第二个pod示例：
```
apiVersion: v1
kind: Pod
metadata:
  name: dapi-test-pod
spec:
  containers:
    - name: test-container
      image: gcr.io/google_containers/busybox
      command: [ "/bin/sh", "-c", "env" ]
      env:
        - name: CACHE_HOST
          valueFrom:
            configMapKeyRef:
              name: test-cfg
              key: cache_host
              optional: true
  restartPolicy: Never
```

### 命令行参数
在命令行下引用时，需要先设置为环境变量，之后 可以通过$(VAR_NAME)设置容器启动命令的启动参数.
ConfigMap文件示例：
```
apiVersion: v1
kind: ConfigMap
metadata:
  name: special-config
  namespace: default
data:
  special.how: very
  special.type: charm
```

Pod示例：
```
apiVersion: v1
kind: Pod
metadata:
  name: dapi-test-pod
spec:
  containers:
    - name: test-container
      image: gcr.io/google_containers/busybox
      command: [ "/bin/sh", "-c", "echo $(SPECIAL_LEVEL_KEY) $(SPECIAL_TYPE_KEY)" ]
      env:
        - name: SPECIAL_LEVEL_KEY
          valueFrom:
            configMapKeyRef:
              name: special-config
              key: special.how
        - name: SPECIAL_TYPE_KEY
          valueFrom:
            configMapKeyRef:
              name: special-config
              key: special.type
  restartPolicy: Never
```

### 数据卷文件
使用volume将ConfigMap作为文件或目录直接挂载，其中每一个key-value键值对都会生成一个文件，key为文件名，value为内容.
ConfigMap示例：
```
apiVersion: v1
kind: ConfigMap
metadata:
  name: special-config
  namespace: default
data:
  special.how: very
  special.type: charm
```

第一个pod示例，简单的将上面创建的ConfigMap直接挂载至pod的/etc/config目录下：
```
apiVersion: v1
kind: Pod
metadata:
  name: dapi-test-pod
spec:
  containers:
    - name: test-container
      image: gcr.io/google_containers/busybox
      command: [ "/bin/sh", "-c", "cat /etc/config/special.how" ]
      volumeMounts:
      - name: config-volume
        mountPath: /etc/config
  volumes:
    - name: config-volume
      configMap:
        name: special-config
  restartPolicy: Never
```

第二个pod示例，只将ConfigMap的special.how这个key挂载到/etc/config目录下的一个相对路径path/to/special-key，如果存在同名文件，直接覆盖。其他的key不挂载：
```
apiVersion: v1
kind: Pod
metadata:
  name: dapi-test-pod
spec:
  containers:
    - name: test-container
      image: gcr.io/google_containers/busybox
      command: [ "/bin/sh","-c","cat /etc/config/path/to/special-key" ]
      volumeMounts:
      - name: config-volume
        mountPath: /etc/config
  volumes:
    - name: config-volume
      configMap:
        name: special-config
        items:
        - key: special.how
          path: path/to/special-key
  restartPolicy: Never
```

## Note
- ConfigMap必须在Pod之前创建
- 只有与当前ConfigMap在同一个namespace内的pod才能使用这个ConfigMap，换句话说，ConfigMap不能跨命名空间调用。
- 很多生产环境中的应用程序配置较为复杂，可能需要多个config文件、命令行参数和环境变量的组合。并且，这些配置信息应该从应用程序镜像中解耦出来，以保证镜像的可移植性以及配置信息不被泄露。社区引入ConfigMap这个API资源来满足这一需求。
- ConfigMap包含了一系列的键值对，用于存储被Pod或者系统组件（如controller）访问的信息。这与secret的设计理念有异曲同工之妙，它们的主要区别在于ConfigMap通常不用于存储敏感信息，而只存储简单的文本信息。
