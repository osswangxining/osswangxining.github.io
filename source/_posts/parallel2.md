---
title: Java并发编程(2)
date: 2016-1-12 20:46:25
categories:
- 分布式&云计算
- Java并发编程
tags:
  - 分布式
  - Java并发编程
---

# Timer和TimerTask
## Timer里面的一些常见方法
```
public void schedule(TimerTask task, long delay)  //经过delay(ms)后开始进行调度，仅仅调度一次

```
