package hello;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SampleBackground {

  @Autowired
  private Tracer tracer;
  @Autowired
  private Random random;

  @Async
  public void background() throws InterruptedException {
    int millis = this.random.nextInt(1000);
    Thread.sleep(millis);
    this.tracer.addTag("background-sleep-millis", String.valueOf(millis));
  }

}