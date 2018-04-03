---
title: 在Ubuntu16.04上构建第一个fabric网络
date: 2018-4-1 16:36:25
categories:
  - 分布式&云计算
  - blockchain
tags:
  - blockchain
  - Kubernetes
  - container
  - 容器
  - 区块链
---

![](https://raw.githubusercontent.com/osswangxining/myimages/master/blockchain-3277336_1280.png)
图片来自https://pixabay.com

## 前言

本篇我们将按照[Hyperledger Doc: Building Your First Network](http://hyperledger-fabric.readthedocs.io/en/latest/build_network.html)，体验[fabric-samples-1.1.0](https://github.com/hyperledger/fabric-samples/releases/tag/v1.1.0)中的first-network示例。

first-network这个示例的场景是由两个组织组成Hyperledger Fabric的网络，其中只有一个orderer节点，每个组织有2个peer节点。

使用Ubuntu16.04主机作为运行环境，已经安装好了docker和docker-compose。

<!-- more -->

## 安装Hyperledger Fabric
下载源码并解压缩：
```sh
wget https://github.com/hyperledger/fabric/archive/v1.1.0.tar.gz -o fabric-1.1.0.tar.gz
tar -zxvf fabric-1.1.0.tar.gz
cd fabric-1.1.0/scripts/
```

执行bootstrap.sh脚本可下载对应平台的fabric二进制版本和pull相关的docker镜像。
```sh
./bootstrap.sh
===> Downloading platform specific fabric binaries
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
 11 35.4M   11 4332k    0     0   169k      0  0:03:33  0:00:25  0:03:08  163k
```

查看下载fabric二进制文件：
```sh
cd bin
ls
configtxgen  configtxlator  cryptogen  get-byfn.sh  get-docker-images.sh  orderer  peer
```
并将这些二进制文件设置到环境变量PATH中，例如：
```sh
vi ~/.profile
.......
export PATH="$PATH:$HOME/.rvm/bin:/usr/local/Cellar/graphviz/2.40.1/bin:/Users/xiningwang/Downloads/fabric-release-1.1/scripts/bin"
.......

source ~/.profile
```

查看pull的docker镜像：
```sh
docker images
REPOSITORY                     TAG                 IMAGE ID            CREATED             SIZE
hyperledger/fabric-ca          latest              72617b4fa9b4        2 weeks ago         299MB
hyperledger/fabric-ca          x86_64-1.1.0        72617b4fa9b4        2 weeks ago         299MB
hyperledger/fabric-tools       latest              b7bfddf508bc        2 weeks ago         1.46GB
hyperledger/fabric-tools       x86_64-1.1.0        b7bfddf508bc        2 weeks ago         1.46GB
hyperledger/fabric-orderer     latest              ce0c810df36a        2 weeks ago         180MB
hyperledger/fabric-orderer     x86_64-1.1.0        ce0c810df36a        2 weeks ago         180MB
hyperledger/fabric-peer        latest              b023f9be0771        2 weeks ago         187MB
hyperledger/fabric-peer        x86_64-1.1.0        b023f9be0771        2 weeks ago         187MB
hyperledger/fabric-javaenv     latest              82098abb1a17        2 weeks ago         1.52GB
hyperledger/fabric-javaenv     x86_64-1.1.0        82098abb1a17        2 weeks ago         1.52GB
hyperledger/fabric-ccenv       latest              c8b4909d8d46        2 weeks ago         1.39GB
hyperledger/fabric-ccenv       x86_64-1.1.0        c8b4909d8d46        2 weeks ago         1.39GB
hyperledger/fabric-zookeeper   latest              92cbb952b6f8        6 weeks ago         1.39GB
hyperledger/fabric-zookeeper   x86_64-0.4.6        92cbb952b6f8        6 weeks ago         1.39GB
hyperledger/fabric-kafka       latest              554c591b86a8        6 weeks ago         1.4GB
hyperledger/fabric-kafka       x86_64-0.4.6        554c591b86a8        6 weeks ago         1.4GB
hyperledger/fabric-couchdb     latest              7e73c828fc5b        6 weeks ago         1.56GB
hyperledger/fabric-couchdb     x86_64-0.4.6        7e73c828fc5b        6 weeks ago         1.56GB
```

## 安装fabric-samples-1.1.0
下载fabric-samples-1.1.0：

```sh
wget https://github.com/hyperledger/fabric-samples/archive/v1.1.0.tar.gz -o fabric-samples-v1.1.0.tar.gz
tar -zxvf fabric-samples-1.1.0.tar.gz

cd fabric-samples-1.1.0/first-network/
ls
README.md  channel-artifacts   docker-compose-cli.yaml         docker-compose-e2e-template.yaml  org3-artifacts
base       configtx.yaml       docker-compose-couch-org3.yaml  docker-compose-org3.yaml          scripts
byfn.sh    crypto-config.yaml  docker-compose-couch.yaml       eyfn.sh

```
其中byfn.sh是一个完整的脚本，提供了快速引导启动4个分别属于两个不同组织的peer节点容器和一个排序服务orderer节点容器成的Hyperledger Fabric网络。这个脚本还将启动一个容器来运行将peer节点加入channel，部署实例化chaincode以及驱动以及部署的chaincode执行交易的脚本。
```sh
./byfn.sh
Usage:
 byfn.sh up|down|restart|generate|upgrade [-c <channel name>] [-t <timeout>] [-d <delay>] [-f <docker-compose-file>] [-s <dbtype>] [-i <imagetag>]
 byfn.sh -h|--help (print this message)
   <mode> - one of 'up', 'down', 'restart' or 'generate'
     - 'up' - bring up the network with docker-compose up
     - 'down' - clear the network with docker-compose down
     - 'restart' - restart the network
     - 'generate' - generate required certificates and genesis block
     - 'upgrade'  - upgrade the network from v1.0.x to v1.1
   -c <channel name> - channel name to use (defaults to "mychannel")
   -t <timeout> - CLI timeout duration in seconds (defaults to 10)
   -d <delay> - delay duration in seconds (defaults to 3)
   -f <docker-compose-file> - specify which docker-compose file use (defaults to docker-compose-cli.yaml)
   -s <dbtype> - the database backend to use: goleveldb (default) or couchdb
   -l <language> - the chaincode language: golang (default) or node
   -i <imagetag> - the tag to be used to launch the network (defaults to "latest")

Typically, one would first generate the required certificates and
genesis block, then bring up the network. e.g.:

 byfn.sh generate -c mychannel
 byfn.sh up -c mychannel -s couchdb
       byfn.sh up -c mychannel -s couchdb -i 1.1.0-alpha
 byfn.sh up -l node
 byfn.sh down -c mychannel
       byfn.sh upgrade -c mychannel

Taking all defaults:
 byfn.sh generate
 byfn.sh up
 byfn.sh down
```

## 生成证书和区块
下载的二进制文件cryptogen、configtxgen拷贝到first-network目录下。
执行./byfn.sh generate命令就可以生成证书和区块。
```sh
./byfn.sh generate
Generating certs and genesis block for with channel 'mychannel' and CLI timeout of '10' seconds and CLI delay of '3' seconds
Continue? [Y/n] y
proceeding ...
/root/blockchain/fabric-samples-1.1.0/first-network/cryptogen

##########################################################
##### Generate certificates using cryptogen tool #########
##########################################################
+ cryptogen generate --config=./crypto-config.yaml
org1.example.com
org2.example.com
+ res=0
+ set +x

/root/blockchain/fabric-samples-1.1.0/first-network/configtxgen
##########################################################
#########  Generating Orderer Genesis block ##############
##########################################################
+ configtxgen -profile TwoOrgsOrdererGenesis -outputBlock ./channel-artifacts/genesis.block
2018-04-02 13:45:17.133 CST [common/tools/configtxgen] main -> INFO 001 Loading configuration
2018-04-02 13:45:17.140 CST [msp] getMspConfig -> INFO 002 Loading NodeOUs
2018-04-02 13:45:17.140 CST [msp] getMspConfig -> INFO 003 Loading NodeOUs
2018-04-02 13:45:17.141 CST [common/tools/configtxgen] doOutputBlock -> INFO 004 Generating genesis block
2018-04-02 13:45:17.141 CST [common/tools/configtxgen] doOutputBlock -> INFO 005 Writing genesis block
+ res=0
+ set +x

#################################################################
### Generating channel configuration transaction 'channel.tx' ###
#################################################################
+ configtxgen -profile TwoOrgsChannel -outputCreateChannelTx ./channel-artifacts/channel.tx -channelID mychannel
2018-04-02 13:45:17.158 CST [common/tools/configtxgen] main -> INFO 001 Loading configuration
2018-04-02 13:45:17.165 CST [common/tools/configtxgen] doOutputChannelCreateTx -> INFO 002 Generating new channel configtx
2018-04-02 13:45:17.165 CST [msp] getMspConfig -> INFO 003 Loading NodeOUs
2018-04-02 13:45:17.166 CST [msp] getMspConfig -> INFO 004 Loading NodeOUs
2018-04-02 13:45:17.187 CST [common/tools/configtxgen] doOutputChannelCreateTx -> INFO 005 Writing new channel tx
+ res=0
+ set +x

#################################################################
#######    Generating anchor peer update for Org1MSP   ##########
#################################################################
+ configtxgen -profile TwoOrgsChannel -outputAnchorPeersUpdate ./channel-artifacts/Org1MSPanchors.tx -channelID mychannel -asOrg Org1MSP
2018-04-02 13:45:17.204 CST [common/tools/configtxgen] main -> INFO 001 Loading configuration
2018-04-02 13:45:17.211 CST [common/tools/configtxgen] doOutputAnchorPeersUpdate -> INFO 002 Generating anchor peer update
2018-04-02 13:45:17.211 CST [common/tools/configtxgen] doOutputAnchorPeersUpdate -> INFO 003 Writing anchor peer update
+ res=0
+ set +x

#################################################################
#######    Generating anchor peer update for Org2MSP   ##########
#################################################################
+ configtxgen -profile TwoOrgsChannel -outputAnchorPeersUpdate ./channel-artifacts/Org2MSPanchors.tx -channelID mychannel -asOrg Org2MSP
2018-04-02 13:45:17.228 CST [common/tools/configtxgen] main -> INFO 001 Loading configuration
2018-04-02 13:45:17.235 CST [common/tools/configtxgen] doOutputAnchorPeersUpdate -> INFO 002 Generating anchor peer update
2018-04-02 13:45:17.235 CST [common/tools/configtxgen] doOutputAnchorPeersUpdate -> INFO 003 Writing anchor peer update
+ res=0
+ set +x

```
如果遇到dns解析错误，请参考如下：
>> 原因分析：
  近期新创建的ECS主机中的resolv.conf内容发生了变化 -> 导致Hyperledger Fabric的容器内域名解析从pure Go resolver切换至cgo resolver -> 触发了一个已知的由静态链接cgo导致的SIGSEGV错误 -> 导致Hyperledger Fabric部署失败
  解决方案：
  更新Hyperledger Fabric的docker compose yaml模板，为所有Hyperledger Fabric的节点（如orderer, peer, ca, cli等）添加环境变量GODEBUG=netdns=go以强制使用pure Go resolver。


## 体验Fabric
执行下面的命令:
```sh
./byfn.sh up -i 1.1.0

Starting with channel 'mychannel' and CLI timeout of '10' seconds and CLI delay of '3' seconds
Continue? [Y/n] y
proceeding ...
2018-04-02 15:21:28.301 UTC [main] main -> INFO 001 Exiting.....
LOCAL_VERSION=1.1.0
DOCKER_IMAGE_VERSION=1.1.0

Creating network "net_byfn" with the default driver
Creating volume "net_peer0.org2.example.com" with default driver
Creating volume "net_peer1.org2.example.com" with default driver
Creating volume "net_peer1.org1.example.com" with default driver
Creating volume "net_peer0.org1.example.com" with default driver
Creating peer1.org1.example.com ... done
Creating cli ... done
Creating peer1.org1.example.com ...
Creating peer0.org2.example.com ...
Creating orderer.example.com ...
Creating peer0.org1.example.com ...
Creating cli ...

 ____    _____      _      ____    _____
/ ___|  |_   _|    / \    |  _ \  |_   _|
\___ \    | |     / _ \   | |_) |   | |
 ___) |   | |    / ___ \  |  _ <    | |
|____/    |_|   /_/   \_\ |_| \_\   |_|

Build your first network (BYFN) end-to-end test
....
========= All GOOD, BYFN execution completed ===========


 _____   _   _   ____
| ____| | \ | | |  _ \
|  _|   |  \| | | | | |
| |___  | |\  | | |_| |
|_____| |_| \_| |____/

```
