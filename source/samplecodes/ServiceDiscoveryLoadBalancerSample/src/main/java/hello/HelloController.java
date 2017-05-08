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
