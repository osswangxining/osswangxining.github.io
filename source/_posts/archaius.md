---
title: Archaius动态管理
date: 2017-1-10 20:46:25
categories:
- 分布式&云计算
- 分布式技术架构
tags:
  - 分布式
  - 架构设计
---

### 配置动态管理
配置的值无论是存在Consul、etcd或者其他地方，一旦改变，仍然需要一些额外的动作才能加载更新后的值，例如重启server等等。而使用了Archaius动态管理的机制，尤其配合上Spring，修改了consul上的配置信息后，相应的项目不需要重启，也会读到最新的值。

```
package com.microservice.config;

import java.util.HashMap;
import java.util.Map;

import com.netflix.config.PollResult;
import com.netflix.config.PolledConfigurationSource;

public class ConsulConfigurationSource implements PolledConfigurationSource {
  private String keyName;

  public ConsulConfigurationSource(String keyName) {
    this.keyName = keyName;
  }

  /**
   * 默认情况下，每隔60s，该方法会执行一次
   *
   */
  @Override
  public PollResult poll(boolean initial, Object checkPoint) throws Exception {
    // get value from consul/etcd,etc
    Map<String, Object> propMap = new HashMap<>();

    return PollResult.createFull(propMap);

  }

}
```

上边这个过程默认每隔60s执行一次（也就是说，consul上修改的配置项最多过60s就会被读取到新值），这个值可以通过在system.setproperty中设置读取时间来改变archaius.fixedDelayPollingScheduler.delayMills.


```
package com.microservice.config;

import java.util.Iterator;
import java.util.Map;

import org.springframework.core.env.MapPropertySource;

import com.netflix.config.AbstractPollingScheduler;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicConfiguration;
import com.netflix.config.FixedDelayPollingScheduler;
import com.netflix.config.PolledConfigurationSource;

public class ConsulPropertySource extends MapPropertySource {
  /**
   * @param name
   *          属性源名称：这里就是consul KV中的K
   * @param source
   *          属性源：这里就是consul KV中的V
   */
  public ConsulPropertySource(String name, Map<String, Object> source) {
    super(name, source);// 初始化

    /**
     * 从consul上读取属性并存入netflix config
     */
    PolledConfigurationSource configSource = new ConsulConfigurationSource(name);// 定义读取配置的源头
    AbstractPollingScheduler scheduler = new FixedDelayPollingScheduler();// 设置读取配置文件的
    DynamicConfiguration configuration = new DynamicConfiguration(configSource, scheduler);
    ConfigurationManager.install(configuration);

    /**
     * 将属性存入PropertySource
     */
    @SuppressWarnings("rawtypes")
    Iterator it = configuration.getKeys();
    while (it.hasNext()) {
      String key = (String) it.next();
      this.source.put(key, configuration.getProperty(key));
    }
  }

}
```

可以看出，一个微服务项目的配置信息会存两份：一份在PollResult，一份存在spring的PropertySource，前者动态改变，后者固定不变.
