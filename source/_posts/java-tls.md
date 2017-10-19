---
title: Java-JSSE-SSL/TLS编程代码实例-双向认证
date: 2016-5-27 20:46:25
categories:
  - 微服务
tags:
  - Java
  - ssl
  - TLS
---

## 使用keytool创建密钥库
使用双向认证的SSL/TLS协议通信，客户端和服务器端都要设置用于证实自己身份的安全证书，并且还要设置信任对方的哪些安全证书。
理论上一共需要准备四个文件，两个keystore文件和两个truststore文件。
通信双方分别拥有一个keystore和一个truststore，keystore用于存放自己的密钥和公钥，truststore用于存放所有需要信任方的公钥。


### 生成keystore和truststore
首先使用JDK自带的keytool工具来生成keystore和truststore。
- 1）创建server的keystore文件，生成server的公钥/私钥密钥对。需要指定keystore的密码(storepass)和密钥对的密码(keypass)。
访问keystore需要storepass。访问密钥对需要keypass。

```
xis-macbook-pro:~ xiningwang$ keytool -genkey -alias myserver -keyalg rsa -keysize 1024 -sigalg sha256withrsa -keypass myserver -keystore ~/tempkey
Enter keystore password:
Re-enter new password:
What is your first and last name?
  [Unknown]:  osswangxining
What is the name of your organizational unit?
  [Unknown]:  osswangxn
What is the name of your organization?
  [Unknown]:  oss
What is the name of your City or Locality?
  [Unknown]:  bj
What is the name of your State or Province?
  [Unknown]:  bj
What is the two-letter country code for this unit?
  [Unknown]:  CN
Is CN=osswangxining, OU=osswangxn, O=oss, L=bj, ST=bj, C=CN correct?
  [no]:  yes
```

- 2) 创建client的keystore文件。同样需要指定keystore的密码和密钥对的密码。

```
xis-macbook-pro:~ xiningwang$ keytool -genkey -alias myclient -keyalg rsa -keysize 1024 -sigalg sha256withrsa -keypass myclient -keystore ~/tempkey4client  -storepass 123456
What is your first and last name?
  [Unknown]:  myclient
What is the name of your organizational unit?
  [Unknown]:  mmyyclient
What is the name of your organization?
  [Unknown]:  mmyy
What is the name of your City or Locality?
  [Unknown]:  nyc
What is the name of your State or Province?
  [Unknown]:  nyc
What is the two-letter country code for this unit?
  [Unknown]:  US
Is CN=myclient, OU=mmyyclient, O=mmyy, L=nyc, ST=nyc, C=US correct?
  [no]:  yes
```

- 3）从server的keystore中导出server的证书（其中包括server的公钥）。

```
keytool -export -alias myserver -keystore ~/tempkey -storepass 123456 -file ~/temp20151019/myserver.cer  
Certificate stored in file </Users/xiningwang/temp20151019/myserver.cer>
```

- 4）从client的keystore中导出client的证书（其中包括client的公钥）。

```
keytool -export -alias myclient -keystore ~/tempkey4client -storepass 123456 -file ~/temp20151019/myclient.cer  
Certificate stored in file </Users/xiningwang/temp20151019/myclient.cer>
```

- 5）创建server的truststore文件并导入client的证书（其中包括client的公钥）。

```
xis-macbook-pro:~ xiningwang$ keytool -import -alias myclient -keystore ~/temp20151019/myservertrust.keystore -storepass 12345678 -file ~/temp20151019/myclient.cer
Owner: CN=myclient, OU=mmyyclient, O=mmyy, L=nyc, ST=nyc, C=US
Issuer: CN=myclient, OU=mmyyclient, O=mmyy, L=nyc, ST=nyc, C=US
Serial number: 6a74a7e6
Valid from: Thu Oct 19 15:09:11 CST 2017 until: Wed Jan 17 15:09:11 CST 2018
Certificate fingerprints:
       	 MD5:  3F:4D:D7:79:AC:A7:4B:D1:15:2B:CC:19:8C:08:66:CC
       	 SHA1: 2A:16:D8:5A:0B:BE:D8:D0:49:B6:82:1D:29:31:46:B4:5F:B1:9A:88
       	 SHA256: 84:E2:FC:F2:7B:E6:81:DB:A2:D8:52:5C:7C:35:FF:69:D0:46:8D:A0:07:42:F1:DC:69:CE:85:4B:1F:2F:FC:2F
       	 Signature algorithm name: SHA256withRSA
       	 Version: 3

Extensions:

#1: ObjectId: 2.5.29.14 Criticality=false
SubjectKeyIdentifier [
KeyIdentifier [
0000: 83 E7 88 05 77 C5 50 80   22 CA 2E 12 B3 A7 AA C7  ....w.P.".......
0010: 15 86 FC DA                                        ....
]
]

Trust this certificate? [no]:  yes
Certificate was added to keystore
```

- 6）创建client的truststore文件并导入server的证书（其中包括server的公钥）。

```
keytool -import -alias myserver -keystore ~/temp20151019/myclienttrust.keystore -storepass 12345678 -file ~/temp20151019/myserver.cer

xis-macbook-pro:~ xiningwang$ keytool -import -alias myserver -keystore ~/temp20151019/myclienttrust.keystore -storepass 12345678 -file ~/temp20151019/myserver.cer
Owner: CN=osswangxining, OU=osswangxn, O=oss, L=bj, ST=bj, C=CN
Issuer: CN=osswangxining, OU=osswangxn, O=oss, L=bj, ST=bj, C=CN
Serial number: 4f223106
Valid from: Thu Oct 19 15:04:57 CST 2017 until: Wed Jan 17 15:04:57 CST 2018
Certificate fingerprints:
       	 MD5:  3B:0E:F6:5A:E8:00:FF:04:86:83:2D:5E:1D:C1:89:8C
       	 SHA1: 34:EB:12:16:94:B9:24:B5:7D:76:BF:BF:C6:8C:06:B7:4A:FA:34:23
       	 SHA256: 99:92:BB:4B:23:58:27:EF:B6:64:3C:14:64:EA:14:79:1B:B3:D9:8B:90:78:40:3A:BA:CC:00:55:11:AE:EE:A2
       	 Signature algorithm name: SHA256withRSA
       	 Version: 3

Extensions:

#1: ObjectId: 2.5.29.14 Criticality=false
SubjectKeyIdentifier [
KeyIdentifier [
0000: 30 9E DC 33 B3 FF 6A EA   02 3C 88 21 E9 ED 94 D0  0..3..j..<.!....
0010: 6B B7 17 A7                                        k...
]
]

Trust this certificate? [no]:  yes
Certificate was added to keystore
```

## server端处理流程和代码
- 1)加载server的keystore文件，需要指定keystore的密码(storepass)。
KeyStore类型有如下三种：
jceks - The proprietary keystore implementation provided by the SunJCE provider.
jks - The proprietary keystore implementation provided by the SUN provider.
pkcs12 - The transfer syntax for personal identity information as defined in PKCS #12.

- 2)加载server的truststore文件，需要指定truststore的密码(storepass)。

- 3)创建KeyManagerFactory对象并用1）中加载的keystore和server密钥对的密码(keypass)来初始化。

- 4)创建TrustManagerFactory对象并用2）中加载的truststore来初始化。truststore中存的是client的公钥，不需要keypass也可以访问。

- 5）创建SSLContext并用3）和4）中创建的KeyManagerFactory和TrustManagerFactory对象来初始化。

创建SSLContext是需要给出SSLContext Algorithms。上面这个链接中给出了合法的SSLContext Algorithms，有如下可用值。
SSL - Supports some version of SSL; may support other versions
SSLv2 - Supports SSL version 2 or later; may support other versions
SSLv3 - Supports SSL version 3; may support other versions
TLS - Supports some version of TLS; may support other versions
TLSv1 - Supports RFC 2246: TLS version 1.0 ; may support other versions
TLSv1.1 - Supports RFC 4346: TLS version 1.1 ; may support other versions
TLSv1.2 - Supports RFC 5246: TLS version 1.2 ; may support other versions

- 6）创建SSLServerSocketFactory，在指定的端口上创建SSLServerSocket并设定需要客户端证书：setNeedClientAuth(true)

- 7）在SSLServerSocket对象上调用accept()方法等待客户端的连接。
客户端连上来之后这个函数会返回一个SSLSocket对象，在这个对象的输入输出流上进行读写。
在这个SSLSocket对象上可以添加一个HandshakeCompletedListener的监听器，SSL/TLS握手结束后这个监听器的handshakeCompleted方法就会被调用。
客户端有三种方法会触发握手：
  - 显式调用startHandshake方法
  - 在socket对象上进行read或write操作
  - 在socket对象上调用getSession方法
