---
title: 分布式服务弹性框架
date: 2016-5-9 20:46:25
---

### Overview
在复杂的分布式 架构 的应用程序有很多的依赖，都会不可避免地在某些时候失败。高并发的依赖失败时如果没有隔离措施，当前应用服务就有被拖垮的风险。
Hystrix 是Netflix开源的一个针对分布式系统的延迟和容错库，由Java写成。
```Example
例如:一个依赖30个SOA服务的系统,每个服务99.99%可用。
99.99%的30次方 ≈ 99.7%
0.3% 意味着一亿次请求 会有 3,000,00次失败
换算成时间大约每月有2个小时服务不稳定.
随着服务依赖数量的变多，服务不稳定的概率会成指数性提高.
解决问题方案:对依赖做隔离,Hystrix就是处理依赖隔离的框架,同时也是可以帮我们做依赖服务的治理和监控.

```
<!-- more -->
1）Hystrix使用命令模式HystrixCommand(Command)包装依赖调用逻辑，每个命令在单独线程中/信号 授权 下执行

2）提供熔断器组件,可以自动运行或手动调用,停止当前依赖一段时间(10秒)，熔断器默认 错误 率阈值为50%,超过将自动运行。

3）可配置依赖调用 超时 时间,超时时间一般设为比99.5%平均时间略高即可.当调用超时时，直接返回或执行fallback逻辑。

4）为每个依赖提供一个小的线程池（或信号），如果线程池已满调用将被立即拒绝，默认不采用排队.加速失败判定时间。

5）依赖调用结果分:成功，失败（抛出 异常 ），超时，线程拒绝，短路。 请求失败(异常，拒绝，超时，短路)时执行fallback(降级)逻辑。

![依赖架构](https://github.com/Netflix/Hystrix/wiki/images/soa-4-isolation-640.png)
### 设计思想
HystrixCommand.execute方法实际上是调用了HystrixCommand.queue().get()，而queue方法除了最终调用run之外，还需要为run方法提供超时和异常等保护功能，外部也不能直接调用非安全的run方法.

1.Hystrix可以为分布式服务提供弹性保护

2.Hystrix通过命令模式封装调用，来实现弹性保护，继承HystrixCommand并且实现run方法，就完成了最简单的封装。

3.实现getFallBack方法可以为熔断或者异常提供后备处理方法。

![Command设计模式](http://img2.tuicool.com/JrQNFzN.png!web)
```Sample
public class CommandHelloWorld extends HystrixCommand<String> {

  private final String name;

  public CommandHelloWorld(String name) {
    super(Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("HelloServiceGroup"))
        .andCommandPropertiesDefaults(HystrixCommandProperties.Setter().withExecutionTimeoutInMilliseconds(500)));
    this.name = name;
  }

  @Override
  protected String run() throws InterruptedException {
    Thread.sleep(600);

    return "Hello " + name + "!";
  }

  @Override
  protected String getFallback() {
    return String.format("[FallBack]Hello %s!", name);
  }
}
```

Enable Hystrix in Spring Boot Application
```
@EnableHystrix
@EnableHystrixDashboard
public class Application {
```
### How It works

![9个步骤](https://github.com/Netflix/Hystrix/wiki/images/hystrix-command-flow-chart.png)

流程说明:
- 1:每次调用创建一个新的HystrixCommand,把依赖调用封装在run()方法中.
- 2:执行execute()/queue做同步或异步调用.
- 3:判断熔断器(circuit-breaker)是否打开,如果打开跳到步骤8,进行降级策略,如果关闭进入步骤.
- 4:判断线程池/队列/信号量是否跑满，如果跑满进入降级步骤8,否则继续后续步骤.
- 5:调用HystrixCommand的run方法.运行依赖逻辑
 - 5a:依赖逻辑调用超时,进入步骤8.
- 6:判断逻辑是否调用成功
 - 6a:返回成功调用结果
 - 6b:调用出错，进入步骤8.
- 7:计算熔断器状态,所有的运行状态(成功, 失败, 拒绝,超时)上报给熔断器，用于统计从而判断熔断器状态.
- 8:getFallback()降级逻辑.
  以下四种情况将触发getFallback调用：
 (1):run()方法抛出非HystrixBadRequestException异常。
 (2):run()方法调用超时
 (3):熔断器开启拦截调用
 (4):线程池/队列/信号量是否跑满
 - 8a:没有实现getFallback的Command将直接抛出异常
 - 8b:fallback降级逻辑调用成功直接返回
 - 8c:降级逻辑调用失败抛出异常
- 9:返回执行成功结果

### Circuit Breaker 流程架构和统计
每个熔断器默认维护10个bucket,每秒一个bucket,每个blucket记录成功,失败,超时,拒绝的状态，
默认错误超过50%且10秒内超过20个请求进行中断拦截.

![](https://github.com/Netflix/Hystrix/wiki/images/circuit-breaker-640.png)

### 隔离(Isolation)分析
#### 线程隔离

把执行依赖代码的线程与请求线程(如:jetty线程)分离，请求线程可以自由控制离开的时间(异步过程)。
通过线程池大小可以控制并发量，当线程池饱和时可以提前拒绝服务,防止依赖问题扩散。
线上建议线程池不要设置过大，否则大量堵塞线程有可能会拖慢服务器。
- 线程隔离的优点:

  - 使用线程可以完全隔离第三方代码,请求线程可以快速放回。当一个失败的依赖再次变成可用时，线程池将清理，并立即恢复可用，而不是一个长时间的恢复。
 - 可以完全模拟异步调用，方便异步编程。

- 线程隔离的缺点:

  - 线程池的主要缺点是它增加了cpu，因为每个命令的执行涉及到排队(默认使用SynchronousQueue避免排队)，调度和上下文切换。
  - 对使用ThreadLocal等依赖线程状态的代码增加复杂性，需要手动传递和清理线程状态。

  - NOTE: Netflix公司内部认为线程隔离开销足够小，不会造成重大的成本或性能的影响。
  - Netflix 内部API 每天100亿的HystrixCommand依赖请求使用线程隔，每个应用大约40多个线程池，每个线程池大约5-20个线程。      

#### 信号隔离
信号隔离也可以用于限制并发访问，防止阻塞扩散, 与线程隔离最大不同在于执行依赖代码的线程依然是请求线程（该线程需要通过信号申请）.

如果客户端是可信的且可以快速返回，可以使用信号隔离替换线程隔离,降低开销.
信号量的大小可以动态调整, 线程池大小不可以.

线程隔离与信号隔离区别如下图:

![](https://github.com/Netflix/Hystrix/wiki/images/isolation-options-640.png)

### Monitor Dashboard

Docker Image for this dashboard:

$ docker run -d -p 7979:7979 kennedyoliveira/hystrix-dashboard hystrix-dashboard

在Hystrix Dashboard中输入Hystrix Stream: http://localhost:8080/hystrix.stream

#### Hystrix 指标流(Hystrix Metrics Stream)
To enable the Hystrix metrics stream include a dependency on spring-boot-starter-actuator. This will expose the /hystrix.stream as a management endpoint.

使Hystrix指标流包括依赖于spring-boot-starter-actuator。这将使/hystrix.stream流作为一个管理端点。
```
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
```
#### 断路器: Hystrix 仪表盘(Circuit Breaker: Hystrix Dashboard)

Hystrix的主要作用是会采集每一个HystrixCommand的信息指标,把每一个断路器的信息指标显示的Hystrix仪表盘上。
运行Hystrix仪表板需要在spring boot主类上标注@EnableHystrixDashboard。然后访问/hystrix查看仪表盘，在hystrix客户端应用使用/hystrix.stream监控。

![dashboard](https://github.com/Netflix/Hystrix/wiki/images/dashboard-annoted-circuit-640.png)
