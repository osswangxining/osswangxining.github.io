---
title: Java并发编程(4) - ThreadLocal | volatile | Lock
date: 2016-1-13 20:46:25
categories:
- 分布式&云计算
- Java并发编程
tags:
  - 分布式
  - Java并发编程
---

# ThreadLocal
ThreadLocal为变量在每个线程中都创建了一个副本，那么每个线程可以访问自己内部的副本变量。
<!-- more -->

## 使用场景
先来看一个例子：
```
class ConnectionManager {

    private static Connection connect = null;

    public static Connection openConnection() {
        if(connect == null){
            connect = DriverManager.getConnection();
        }
        return connect;
    }

    public static void closeConnection() {
        if(connect!=null)
            connect.close();
    }
}
```
假设有这样一个数据库链接管理类，这段代码在单线程中使用是没有任何问题的，但是如果在多线程中使用呢？很显然，在多线程中使用会存在线程安全问题：第一，这里面的2个方法都没有进行同步，很可能在openConnection方法中会多次创建connect；第二，由于connect是共享变量，那么必然在调用connect的地方需要使用到同步来保障线程安全，因为很可能一个线程在使用connect进行数据库操作，而另外一个线程调用closeConnection关闭链接。


既然不需要在线程之间共享这个变量，可以直接这样处理，在每个需要使用数据库连接的方法中具体使用时才创建数据库链接，然后在方法调用完毕再释放这个连接。比如下面这样：
```
class ConnectionManager {

    private  Connection connect = null;

    public Connection openConnection() {
        if(connect == null){
            connect = DriverManager.getConnection();
        }
        return connect;
    }

    public void closeConnection() {
        if(connect!=null)
            connect.close();
    }
}
```
由于每次都是在方法内部创建的连接，那么线程之间自然不存在线程安全问题。但是这样会有一个致命的影响：导致服务器压力非常大，并且严重影响程序执行性能。由于在方法中需要频繁地开启和关闭数据库连接，这样不尽严重影响程序执行效率，还可能导致服务器压力巨大。

那么这种情况下使用ThreadLocal是再适合不过的了，因为ThreadLocal在每个线程中对该变量会创建一个副本，即每个线程内部都会有一个该变量，且在线程内部任何地方都可以使用，线程之间互不影响，这样一来就不存在线程安全问题，也不会严重影响程序执行性能。

## 示例
通过一个例子来证明通过ThreadLocal能达到在每个线程中创建变量副本的效果：
```
package parallel;

public class ThreadLocalTest {

  ThreadLocal<Long> longLocal = new ThreadLocal<Long>();
  ThreadLocal<String> stringLocal = new ThreadLocal<String>();

  public void set() {
    longLocal.set(Thread.currentThread().getId());
    stringLocal.set(Thread.currentThread().getName());
  }

  public long getLong() {
    return longLocal.get();
  }

  public String getString() {
    return stringLocal.get();
  }

  public static void main(String[] args) throws InterruptedException {
    final ThreadLocalTest test = new ThreadLocalTest();

    test.set();
    System.out.println(test.getLong());
    System.out.println(test.getString());

    Thread thread1 = new Thread() {
      public void run() {
        test.set();
        System.out.println(test.getLong());
        System.out.println(test.getString());
      };
    };
    thread1.start();
    thread1.join();

    System.out.println(test.getLong());
    System.out.println(test.getString());
  }

}

```

运行结果如下：
```
1
main
10
Thread-0
1
main
```

# volatile
一旦一个共享变量（类的成员变量、类的静态成员变量）被volatile修饰之后，保证了不同线程对这个变量进行操作时的可见性，即一个线程修改了某个变量的值，这新值对其他线程来说是立即可见的。

synchronized关键字是防止多个线程同时执行一段代码，那么就会很影响程序执行效率，而volatile关键字在某些情况下性能要优于synchronized，但是要注意volatile关键字是无法替代synchronized关键字的，因为volatile关键字无法保证操作的原子性。

# Lock
Lock与synchronized的一些区别：
- Lock是一个接口，而synchronized是Java中的关键字，synchronized是内置的语言实现；
- synchronized在发生异常时，会自动释放线程占有的锁，因此不会导致死锁现象发生；而Lock在发生异常时，如果没有主动通过unLock()去释放锁，则很可能造成死锁现象，因此使用Lock时需要在finally块中释放锁；
- Lock可以让等待锁的线程响应中断，而synchronized却不行，使用synchronized时，等待的线程会一直等待下去，不能够响应中断；
- 通过Lock可以知道有没有成功获取锁，而synchronized却无法办到。
- Lock可以提高多个线程进行读操作的效率。

## ReentrantLock
ReentrantLock，意思是“可重入锁”，是唯一实现了Lock接口的类，并且ReentrantLock提供了更多的方法。
使用tryLock试图获取锁，示例如下：
```
package parallel;

import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReentrantLockTest {
  private ArrayList<Integer> arrayList = new ArrayList<Integer>();
  private Lock lock = new ReentrantLock();

  public static void main(String[] args) {
    final ReentrantLockTest test = new ReentrantLockTest();

    new Thread() {
      public void run() {
        test.insert(Thread.currentThread());
      };
    }.start();

    new Thread() {
      public void run() {
        test.insert(Thread.currentThread());
      };
    }.start();
  }

  public void insert(Thread thread) {
    if (lock.tryLock()) {
      try {
        System.out.println(thread.getName() + "得到了锁");
        for (int i = 0; i < 5; i++) {
          arrayList.add(i);
        }
      } catch (Exception e) {
        // TODO: handle exception
      } finally {
        System.out.println(thread.getName() + "释放了锁");
        lock.unlock();
      }
    } else {
      System.out.println(thread.getName() + "获取锁失败");
    }
  }
}

```

执行结果如下：
```
Thread-0得到了锁
Thread-1获取锁失败
Thread-0释放了锁
```
## ReadWriteLock | ReentrantReadWriteLock
ReadWriteLock也是一个接口，在它里面只定义了两个方法：一个用来获取读锁，一个用来获取写锁。也就是说将文件的读写操作分开，分成2个锁来分配给线程，从而使得多个线程可以同时进行读操作。

ReentrantReadWriteLock类实现了接口ReadWriteLock， 里面提供了很多丰富的方法，不过最主要的有两个方法：readLock()和writeLock()用来获取读锁和写锁。
