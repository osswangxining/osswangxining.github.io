package hello;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class HomeController {
  private static final Log log = LogFactory.getLog(HomeController.class);
  @Autowired
  private RestTemplate restTemplate;
  @Value("${app.url:http://localhost:${local.server.port:${server.port:8080}}}")
  private String url;
  
  @RequestMapping("/home")
  public String index() {
    log.info("Home");
    String s = this.restTemplate.getForObject(url + "/hi", String.class);
    return "hi/" + s;
  }

}
