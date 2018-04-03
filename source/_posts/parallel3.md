---
title: Java并发编程(3) - Timer和TimerTask
date: 2016-1-12 20:46:25
categories:
- 分布式&云计算
- Java并发编程
tags:
  - 分布式
  - Java并发编程
---

# Timer和TimerTask
使用 Timer 实现任务调度的核心类是 Timer 和 TimerTask。其中 Timer 负责设定 TimerTask 的起始与间隔执行时间。使用者只需要创建一个 TimerTask 的继承类，实现自己的 run 方法，然后将其丢给 Timer 去执行即可。
Timer 的设计核心是一个 TaskList 和一个 TaskThread。Timer 将接收到的任务丢到自己的 TaskList 中，TaskList 按照 Task 的最初执行时间进行排序。TimerThread 在创建 Timer 时会启动成为一个守护线程。这个线程会轮询所有任务，找到一个最近要执行的任务，然后休眠，当到达最近要执行任务的开始时间点，TimerThread 被唤醒并执行该任务。之后 TimerThread 更新最近一个要执行的任务，继续休眠。

Timer 的优点在于简单易用，但由于所有任务都是由同一个线程来调度，因此所有任务都是串行执行的，同一时间只能有一个任务在执行，前一个任务的延迟或异常都将会影响到之后的任务。
<!-- more -->
下面的例子，job1执行时延迟5s，job2的执行间隔虽然设置为了2s，但实际由于单线程导致了相应的延迟，间隔时间变成了10s。

```
package parallel;

import java.util.Timer;
import java.util.TimerTask;

public class TimerTest extends TimerTask {
  private String jobName = "";
  private boolean busy;
  public TimerTest(String jobName, boolean busy) {
    super();
    this.jobName = jobName;
    this.busy = busy;
  }

  @Override
  public void run() {
    if(busy) {
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    System.out.println("Job: " + jobName + ", time:" + (System.currentTimeMillis()/1000));

  }

  public static void main(String[] args) {
    Timer timer = new Timer();  
    timer.schedule(new TimerTest("job1", true), 1000, 1000);
    timer.schedule(new TimerTest("job2",false), 2000, 2000);
  }

}

```
执行结果，如下：
```
Job: job1, time:1497336419
Job: job2, time:1497336419
Job: job1, time:1497336424
Job: job1, time:1497336429
Job: job2, time:1497336429
Job: job1, time:1497336434
Job: job1, time:1497336439
Job: job2, time:1497336439
Job: job1, time:1497336444
Job: job1, time:1497336449
Job: job2, time:1497336449
```

补充一点，鉴于 Timer 的上述缺陷，Java 5 推出了基于线程池设计的 ScheduledExecutor。其设计思想是，每一个被调度的任务都会由线程池中一个线程去执行，因此任务是并发执行的，相互之间不会受到干扰。需 要注意的是，只有当任务的执行时间到来时，ScheduedExecutor 才会真正启动一个线程，其余时间 ScheduledExecutor 都是在轮询任务的状态。
```
package parallel;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduledExecutorTest implements Runnable {

  private String jobName = "";
  private boolean busy;

  public ScheduledExecutorTest(String jobName, boolean busy) {
    super();
    this.jobName = jobName;
    this.busy = busy;
  }

  @Override
  public void run() {
    if (busy) {
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    System.out.println("Job: " + jobName + ", time:" + (System.currentTimeMillis() / 1000));

  }

  public static void main(String[] args) {
    ScheduledExecutorService service = Executors.newScheduledThreadPool(2);
    service.scheduleAtFixedRate(new ScheduledExecutorTest("job1", true), 1000, 1000, TimeUnit.MILLISECONDS);
    service.scheduleAtFixedRate(new ScheduledExecutorTest("job2", false), 2000, 2000, TimeUnit.MILLISECONDS);
  }
}

```
执行结果，如下：
```
Job: job2, time:1497339174
Job: job2, time:1497339176
Job: job2, time:1497339178
Job: job1, time:1497339178
Job: job2, time:1497339180
Job: job2, time:1497339182
Job: job1, time:1497339183
Job: job2, time:1497339184
Job: job2, time:1497339186
Job: job2, time:1497339188
Job: job1, time:1497339188
Job: job2, time:1497339190
Job: job2, time:1497339192
```
