---
title: Consul
date: 2017-1-9 20:46:25
---

### Consul 服务注册管理
#### 启动Consul Docker

```
docker run -d -p 8400:8400 -p 8500:8500/tcp -p 8600:53/udp -e 'CONSUL_LOCAL_CONFIG={"acl_datacenter":"dc1","acl_default_policy”:"write","acl_down_policy":"extend-cache","acl_master_token":"the_one_ring","bootstrap_expect":1,"datacenter":"dc1","data_dir":"/usr/local/bin/consul.d/data","server":true}' consul agent -server -bind=127.0.0.1 -client=0.0.0.0
```

or

```
docker run -d --name myconsul -p 8400:8400 -p 8500:8500/tcp -p 8600:53/udp -e 'CONSUL_LOCAL_CONFIG={"acl_datacenter":"dc1","acl_default_policy":"allow","acl_down_policy":"extend-cache","acl_master_token":"the_one_ring","bootstrap_expect":1,"datacenter":"dc1","data_dir":"/usr/local/bin/consul.d/data","server":true}' consul agent -server -bind=127.0.0.1 -client=0.0.0.0 -ui
```

#### 1. 注册服务:

http://{IP}:8500/v1/agent/service/register

PUT
```
{
    "id": "Analytics_Job_Free_JobID0001",
    "name": "Analytics_Job_Free_JobID0001",
    "tags": [
        "RTI"
    ]
}
```

#### 2. 注销服务：

http://{IP}:8500/v1/agent/service/deregister/jetty


#### 3. 注册check

http://{IP}:8500/v1/agent/check/register

PUT
```
{  
            "http": "http://192.168.1.200:8080/health?appId=app001",  
            "interval": “20s",
            "id": "app001",
              "name": "Analytics YARN Application 001",
            "applicationId":"app001",
            "service_id":"Analytics_Job_Free_JobID0001"
        }

{  
            "http": "http://192.168.1.200:8080/health?appId=app002",  
            "interval": “20s",
            "id": "app002",
              "name": "Analytics YARN Application 002",
            "applicationId":"app002”,
        "status": "passing",
            "service_id":"Analytics_Job_Free_JobID0001"
        }
```
#### 4. 注销check

http://{IP}:8500/v1/agent/check/deregister/app002


### 例子

```
<dependency>
			<groupId>org.springframework.cloud</groupId>
			<artifactId>spring-cloud-starter-consul-discovery</artifactId>
			<version>1.1.2.RELEASE</version>
		</dependency
```

application.properties
```
server.port=9955  

spring.application.name=SampleClient
spring.cloud.consul.host=127.0.0.1
spring.cloud.consul.port=8500
spring.cloud.consul.enabled=true
spring.cloud.consul.discovery.register=false
spring.cloud.consul.discovery.enabled=true
spring.cloud.consul.discovery.instanceId=tomcat1
spring.cloud.consul.discovery.serviceName=tomcat1  
spring.cloud.consul.discovery.hostname=127.0.0.1
spring.cloud.consul.discovery.port=${server.port}
spring.cloud.consul.discovery.healthCheckUrl=http://127.0.0.1:9955/health  
spring.cloud.consul.discovery.healthCheckInterval=10s  
spring.cloud.consul.discovery.tags=dev
```

application.yml
```
say-hello:
 ribbon:
  okhttp:
    enabled: true
```  
Enable
```
@SpringBootApplication
@EnableDiscoveryClient
@RibbonClient(name = "say-hello", configuration = SayHelloConfiguration.class)
public class Application {
```

Full code
```
package hello;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class HelloController {

  @Autowired
  private LoadBalancerClient loadBalancer;
  @Autowired
  private DiscoveryClient discoveryClient;

  @LoadBalanced
  @Bean
  RestTemplate restTemplate() {
    return new RestTemplate();
  }

  @Autowired
  RestTemplate restTemplate;

  /**
   * 从所有服务中选择一个服务（轮询）
   */
  @RequestMapping("/discover")
  public Object discover() {
    // return loadBalancer.choose("application").getUri().toString();
    String greeting = this.restTemplate.getForObject("http://application", String.class);
    return String.format("%s!", greeting);
  }

  @RequestMapping("/d")
  public Object d() {
    return loadBalancer.choose("application").getUri().toString();

  }

  /**
   * 获取所有服务
   */
  @RequestMapping("/services")
  public Object services() {
    return discoveryClient.getInstances("application").stream().findFirst().get().getUri();
  }

}

```
