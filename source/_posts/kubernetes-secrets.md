---
title: Kubernetes Secrets
categories:
  - 分布式&云计算
  - Kubernetes
tags:
  - 分布式
  - Kubernetes
  - 配置
  - 容器
  - Secrets
---

## Secrets描述
在Kubernetes中，Secret对象类型主要目的是保存一些私密数据，比如密码, tokens, ssh keys等信息。将这些信息放在Secret对象中比直接放在pod或docker image中更安全，也更方便使用。

创建Secrets对象的方式有两种，一种是用户手动创建，另一种是集群自动创建。
一个已经创建好的Secrets对象有两种方式被pod对象使用，其一，在container中的volume对象里以file的形式被使用，其二，在pull images时被kubelet使用。

为了使用Secret对象，pod必须引用这个Secret，同样可以手动或者自动来执行引用操作。

## Built-in Secrets
Kubernetes会自动创建包含证书信息的Secret，并且使用它来访问api, Kubernetes也将自动修改pod来使用这个Secret。

自动创建的Secret以及所使用的api证书,可以根据需要disable或者override。如果仅仅需要安全访问apiserver，那么上述的流程是推荐的方式。

## 自定义Secrets
### 通过kubectl自定义Secrets
数据中的字段为map类型。其中keys必须符合dns_subdomain规则，values可以为任意类型，使用base64编码。上述例子中，username和password的数据值在base64编码前的值为value-1 和 value-2。
```
xis-macbook-pro:~ xiningwang$ echo -n "admin" > ./username.txt
xis-macbook-pro:~ xiningwang$  echo -n "1f2d1e2e67df" > ./password.txt
xis-macbook-pro:~ xiningwang$ kubectl create secret generic db-user-pass --from-file=./username.txt --from-file=./password.txt
secret "db-user-pass" created
```

查看创建的Secrets:
```
xis-macbook-pro:~ xiningwang$ kubectl get secrets
NAME                  TYPE                                  DATA      AGE
db-user-pass          Opaque                                2         6m
default-token-t5bk8   kubernetes.io/service-account-token   3         4d
xis-macbook-pro:~ xiningwang$ kubectl describe secrets/db-user-pass
Name:  		db-user-pass
Namespace:     	default
Labels:		<none>
Annotations:   	<none>

Type:  	Opaque

Data
====
password.txt:  	12 bytes
username.txt:  	5 bytes
```

### 手工创建Secrets
```
xis-macbook-pro:~ xiningwang$ echo -n "admin" | base64
YWRtaW4=
xis-macbook-pro:~ xiningwang$ echo -n "1f2d1e2e67df" | base64
MWYyZDFlMmU2N2Rm
xis-macbook-pro:~ xiningwang$ vi mysecrect.yaml
apiVersion: v1
kind: Secret
metadata:
  name: mysecret
type: Opaque
data:
  username: YWRtaW4=
  password: MWYyZDFlMmU2N2Rm
```
使用yaml文件创建Secrets:
```
xis-macbook-pro:~ xiningwang$ kubectl create -f ./mysecrect.yaml
secret "mysecret" created
```   

## 查看Secrets
使用如下命令获取创建的Secrets内容:
```
xis-macbook-pro:~ xiningwang$ kubectl get secret db-user-pass -o yaml
apiVersion: v1
data:
  password.txt: MWYyZDFlMmU2N2Rm
  username.txt: YWRtaW4=
kind: Secret
metadata:
  creationTimestamp: 2017-05-09T07:54:29Z
  name: db-user-pass
  namespace: default
  resourceVersion: "84609"
  selfLink: /api/v1/namespaces/default/secrets/db-user-pass
  uid: bb23a365-348c-11e7-ab36-080027fd8883
type: Opaque
```
base64解密:
```
$ echo "MWYyZDFlMmU2N2Rm" | base64 --decode
1f2d1e2e67df
```
## 使用Secrets
### 手动为pod绑定secret
必须有spec.volumes才能使用secret。 如果一个pod中有多个container，每个container需要他们单独对应的volumeMounts ，但是一个secret只能对应一个spec.volumes。
```
{
 "apiVersion": "v1",
 "kind": "Pod",
  "metadata": {
    "name": "mypod",
    "namespace": "myns"
  },
  "spec": {
    "containers": [{
      "name": "mypod",
      "image": "redis",
      "volumeMounts": [{
        "name": "foo",
        "mountPath": "/etc/foo",
        "readOnly": true
      }]
    }],
    "volumes": [{
      "name": "foo",
      "secret": {
        "secretName": "mysecret"
      }
    }]
  }
}
```
查看Secrets的值:
```
$ ls /etc/foo/
username
password
$ cat /etc/foo/username
admin
$ cat /etc/foo/password
1f2d1e2e67df
```

### 使用Secrets作为环境变量
>  定义对Secrets的引用: env[x].valueFrom.secretKeyRef.

```
apiVersion: v1
kind: Pod
metadata:
  name: secret-env-pod
spec:
  containers:
    - name: mycontainer
      image: redis
      env:
        - name: SECRET_USERNAME
          valueFrom:
            secretKeyRef:
              name: mysecret
              key: username
        - name: SECRET_PASSWORD
          valueFrom:
            secretKeyRef:
              name: mysecret
              key: password
  restartPolicy: Never
```
查看Secrets的值:
```
$ echo $SECRET_USERNAME
admin
$ echo $SECRET_PASSWORD
1f2d1e2e67df
```

## Secret与Pod的生命周期
当通过api创建一个pod后，不会去检查所引用的secret是否存在。一旦这个pod被使用，kubelet将会尝试去获取引用的secret的值。如果这个secret不存在，或者kubelet暂时链接不上apiserver，kubelet将会定期重试，并发送一个event来解释pod没有启动的原因。如果获取到了对应的secret，kubelet将会创建对应的volume并绑定到container。

一旦kubelet创建了一个pod，则container使用的相关secret volume不会在改变，即使对应的secret对象被修改。如果为了改变使用的secret，则必须删除旧的pod，并重新创建一个新的pod。

##限制
在使用之前，Secret volume 资源被验证，以确保指定的对象引用真是指向一个secret对象。因此，在pod使用它之前必须保证需要的secret被成功创建。Secret api对象从属于namespace，一个Secret对象只能被同namespace的pod所使用。

单个secret限制在1Mb之内，防止过大的secret耗尽apiserver & kubelet的内存。然而，创建许多类似的secret同样也会无用的消耗掉apiserver&kubelet的内存。

kubelet目前只支持pod使用来自于apiserver的secret。pods包括了被 kubectl创建的pod 或者 被replication controller间接创建的。
