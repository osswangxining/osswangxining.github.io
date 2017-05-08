package hello;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
  private static final Log log = LogFactory.getLog(HelloController.class);

  @RequestMapping("/hi")
  public String index() {
    log.info("Hi,Greetings from Spring Boot!");

    return "Hi,Greetings from Spring Boot!";
  }

}
