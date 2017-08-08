---
title: Java并发编程(2)- Callable、Future和FutureTask | CountDownLatch、CyclicBarrier和Semaphore
date: 2016-1-11 20:46:25
categories:
- 分布式&云计算
- Java并发编程
tags:
  - 分布式
  - Java并发编程
---

# Callable、Future和FutureTask
无论是直接继承Thread，还是实现Runnable接口来创建线程，在执行完任务之后都无法获取执行结果。而自从Java 1.5开始，就提供了Callable和Future，通过它们可以在任务执行完毕之后得到任务执行结果。
## Callable
Callable位于java.util.concurrent包下，它也是一个接口，在它里面也只声明了一个方法call();
```
public interface Callable<V> {
    /**
     * Computes a result, or throws an exception if unable to do so.
     *
     * @return computed result
     * @throws Exception if unable to compute a result
     */
    V call() throws Exception;
}
```
一般情况下Callable是配合ExecutorService来使用的，在ExecutorService接口中声明了若干个submit方法的重载版本：
```
<T> Future<T> submit(Callable<T> task);
```

### 示例
```
import java.util.concurrent.Callable;

public class Task implements Callable<Integer> {

  @Override
  public Integer call() throws Exception {
    System.out.println("This is the task from Sub-Task....");
    Thread.sleep(2000);
    int sum = 0;
    for(int i=0;i<100;i++)
        sum += i;
    return sum;
  }

}
```
## Future
Future就是对于具体的Runnable或者Callable任务的执行结果进行取消、查询是否完成、获取结果。必要时可以通过get方法获取执行结果，该方法会阻塞直到任务返回结果。
```
public interface Future<V> {

    /**
     * Attempts to cancel execution of this task.  This attempt will
     * fail if the task has already completed, has already been cancelled,
     * or could not be cancelled for some other reason. If successful,
     * and this task has not started when {@code cancel} is called,
     * this task should never run.  If the task has already started,
     * then the {@code mayInterruptIfRunning} parameter determines
     * whether the thread executing this task should be interrupted in
     * an attempt to stop the task.
     *
     * <p>After this method returns, subsequent calls to {@link #isDone} will
     * always return {@code true}.  Subsequent calls to {@link #isCancelled}
     * will always return {@code true} if this method returned {@code true}.
     *
     * @param mayInterruptIfRunning {@code true} if the thread executing this
     * task should be interrupted; otherwise, in-progress tasks are allowed
     * to complete
     * @return {@code false} if the task could not be cancelled,
     * typically because it has already completed normally;
     * {@code true} otherwise
     */
    boolean cancel(boolean mayInterruptIfRunning);

    /**
     * Returns {@code true} if this task was cancelled before it completed
     * normally.
     *
     * @return {@code true} if this task was cancelled before it completed
     */
    boolean isCancelled();

    /**
     * Returns {@code true} if this task completed.
     *
     * Completion may be due to normal termination, an exception, or
     * cancellation -- in all of these cases, this method will return
     * {@code true}.
     *
     * @return {@code true} if this task completed
     */
    boolean isDone();

    /**
     * Waits if necessary for the computation to complete, and then
     * retrieves its result.
     *
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an
     * exception
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     */
    V get() throws InterruptedException, ExecutionException;

    /**
     * Waits if necessary for at most the given time for the computation
     * to complete, and then retrieves its result, if available.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * @return the computed result
     * @throws CancellationException if the computation was cancelled
     * @throws ExecutionException if the computation threw an
     * exception
     * @throws InterruptedException if the current thread was interrupted
     * while waiting
     * @throws TimeoutException if the wait timed out
     */
    V get(long timeout, TimeUnit unit)
        throws InterruptedException, ExecutionException, TimeoutException;
}
```

在Future接口中声明了5个方法，下面依次解释每个方法的作用：
- cancel方法用来取消任务，如果取消任务成功则返回true，如果取消任务失败则返回false。参数mayInterruptIfRunning表示是否允许取消正在执行却没有执行完毕的任务，如果设置true，则表示可以取消正在执行过程中的任务。如果任务已经完成，则无论mayInterruptIfRunning为true还是false，此方法肯定返回false，即如果取消已经完成的任务会返回false；如果任务正在执行，若mayInterruptIfRunning设置为true，则返回true，若mayInterruptIfRunning设置为false，则返回false；如果任务还没有执行，则无论mayInterruptIfRunning为true还是false，肯定返回true。
- isCancelled方法表示任务是否被取消成功，如果在任务正常完成前被取消成功，则返回 true。
- isDone方法表示任务是否已经完成，若任务完成，则返回true；
- get()方法用来获取执行结果，这个方法会产生阻塞，会一直等到任务执行完毕才返回；
- get(long timeout, TimeUnit unit)用来获取执行结果，如果在指定时间内，还没获取到结果，就直接返回null。

### 示例
```

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class CallableFutureSample {

  public static void main(String[] args) {
    ExecutorService executor = Executors.newCachedThreadPool();
    Task task = new Task();
    Future<Integer> result = executor.submit(task);
    executor.shutdown();

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }

    System.out.println("This task is from main thread....");

    try {
      System.out.println("task execution result from sub-task:" + result.get());
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }

    System.out.println("All tasks have been done.");
  }

}
```
执行结果如下：
```
This is the task from Sub-Task....
This task is from main thread....
task execution result from sub-task:4950
All tasks have been done.
```
## FutureTask
FutureTask类实现了RunnableFuture接口，RunnableFuture继承了Runnable接口和Future接口，所以FutureTask既可以作为Runnable被线程执行，又可以作为Future得到Callable的返回值。
```
public class FutureTask<V> implements RunnableFuture<V> {

}
```

### 示例
```

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class CallableFutureTaskSample {
  public static void main(String[] args) {
    ExecutorService executor = Executors.newCachedThreadPool();
    Task task = new Task();
    FutureTask<Integer> futureTask = new FutureTask<Integer>(task);
    executor.submit(futureTask);
    executor.shutdown();

    try {
      Thread.sleep(1000);
    } catch (InterruptedException e1) {
      e1.printStackTrace();
    }

    System.out.println("This task is from main thread....");

    try {
      System.out.println("task execution result from sub-task:" + futureTask.get());
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    }

    System.out.println("All tasks have been done.");
  }
}

```
执行结果如下：
```
This is the task from Sub-Task....
This task is from main thread....
task execution result from sub-task:4950
All tasks have been done.
```

# CountDownLatch、CyclicBarrier和Semaphore
## CountDownLatch
CountDownLatch类位于java.util.concurrent包下，利用它可以实现类似计数器的功能。

### 示例
```
package parallel;

import java.util.concurrent.CountDownLatch;

public class CountDownLatchSample {
  public static void main(String[] args) {
    final CountDownLatch latch = new CountDownLatch(2);
    new Thread() {
      public void run() {
        try {
          System.out.println("子线程" + Thread.currentThread().getName() + "正在执行");
          Thread.sleep(3000);
          System.out.println("子线程" + Thread.currentThread().getName() + "执行完毕");
          latch.countDown();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      };
    }.start();

    new Thread() {
      public void run() {
        try {
          System.out.println("子线程" + Thread.currentThread().getName() + "正在执行");
          Thread.sleep(3000);
          System.out.println("子线程" + Thread.currentThread().getName() + "执行完毕");
          latch.countDown();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      };
    }.start();

    try {
      System.out.println("等待2个子线程执行完毕...");
      latch.await();
      System.out.println("2个子线程已经执行完毕");
      System.out.println("继续执行主线程");
    } catch (InterruptedException e) {
      e.printStackTrace();
    }

  }
}

```
执行结果如下：
```
子线程Thread-0正在执行
等待2个子线程执行完毕...
子线程Thread-1正在执行
子线程Thread-1执行完毕
子线程Thread-0执行完毕
2个子线程已经执行完毕
继续执行主线程
```

## CyclicBarrier
通过CyclicBarrier可以实现让一组线程等待至某个状态之后再全部同时执行。CyclicBarrier类位于java.util.concurrent包下，CyclicBarrier提供2个构造器;其中：参数parties指让多少个线程或者任务等待至barrier状态；参数barrierAction为当这些线程都达到barrier状态时会执行的内容。
```
public CyclicBarrier(int parties, Runnable barrierAction) {
}

public CyclicBarrier(int parties) {
}
```
### 示例
```
package parallel;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class CyclicBarrierSample {

  public static void main(String[] args) {
    CyclicBarrier barrier = new CyclicBarrier(5, new Runnable() {

      @Override
      public void run() {
        System.out.println("当前线程"+Thread.currentThread().getName());  
        System.out.println("当这些线程都达到barrier状态时会执行的内容.....");

      }
    });
    for (int i = 0; i < 5; i++) {
      new Writer(barrier).start();
    }
  }

  static class Writer extends Thread {
    private CyclicBarrier cyclicBarrier;

    public Writer(CyclicBarrier cyclicBarrier) {
      this.cyclicBarrier = cyclicBarrier;
    }

    @Override
    public void run() {
      System.out.println("线程" + Thread.currentThread().getName() + "正在写入数据...");
      try {
        Thread.sleep(5000);
        System.out.println("线程" + Thread.currentThread().getName() + "写入数据完毕，等待其他线程写入完毕");
        cyclicBarrier.await();
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (BrokenBarrierException e) {
        e.printStackTrace();
      }
      System.out.println("所有线程写入完毕，继续处理其他任务...");
    }
  }
}

```
执行结果如下：
```
线程Thread-0正在写入数据...
线程Thread-4正在写入数据...
线程Thread-3正在写入数据...
线程Thread-1正在写入数据...
线程Thread-2正在写入数据...
线程Thread-3写入数据完毕，等待其他线程写入完毕
线程Thread-4写入数据完毕，等待其他线程写入完毕
线程Thread-2写入数据完毕，等待其他线程写入完毕
线程Thread-1写入数据完毕，等待其他线程写入完毕
线程Thread-0写入数据完毕，等待其他线程写入完毕
当前线程Thread-1
当这些线程都达到barrier状态时会执行的内容.....
所有线程写入完毕，继续处理其他任务...
所有线程写入完毕，继续处理其他任务...
所有线程写入完毕，继续处理其他任务...
所有线程写入完毕，继续处理其他任务...
所有线程写入完毕，继续处理其他任务...
```

## Semaphore
Semaphore信号量可以控同时访问的线程个数，通过 acquire() 获取一个许可，如果没有就等待，而 release() 释放一个许可。　　Semaphore类位于java.util.concurrent包下，它提供了2个构造器：
```
public Semaphore(int permits) {          //参数permits表示许可数目，即同时可以允许多少线程进行访问
    sync = new NonfairSync(permits);
}
public Semaphore(int permits, boolean fair) {    //这个多了一个参数fair表示是否是公平的，即等待时间越久的越先获取许可
    sync = (fair)? new FairSync(permits) : new NonfairSync(permits);
}

public void acquire() throws InterruptedException {  }     //获取一个许可
public void acquire(int permits) throws InterruptedException { }    //获取permits个许可
public void release() { }          //释放一个许可
public void release(int permits) { }    //释放permits个许可

public boolean tryAcquire() { };    //尝试获取一个许可，若获取成功，则立即返回true，若获取失败，则立即返回false
public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException { };  //尝试获取一个许可，若在指定的时间内获取成功，则立即返回true，否则则立即返回false
public boolean tryAcquire(int permits) { }; //尝试获取permits个许可，若获取成功，则立即返回true，若获取失败，则立即返回false
public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException { }; //尝试获取permits个许可，若在指定的时间内获取成功，则立即返回true，否则则立即返回false
```

### 示例
一间可以容纳N人的房间，如果人不满就可以进去，如果人满了，就要等待有人出来。
```

import java.util.concurrent.Semaphore;

public class SemaphoreSample {

  public static void main(String[] args) {
    int CountOfRooms = 10;
    int CountOfPersons = 15;
    Semaphore semaphore = new Semaphore(CountOfRooms);
    for (int i = 0; i < CountOfPersons; i++) {
      new Person(i, semaphore).start();
    }
  }

  static class Person extends Thread {
    private int num;
    private Semaphore semaphore;

    public Person(int num, Semaphore semaphore) {
      this.num = num;
      this.semaphore = semaphore;
    }

    @Override
    public void run() {
      try {
        semaphore.acquire();
        System.out.println("Person - " + this.num + "使用了Room...");
        Thread.sleep(2000);
        System.out.println("Person - " + this.num + "退出了Room");
        semaphore.release();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
```

执行结果如下：
```
Person - 0使用了Room...
Person - 4使用了Room...
Person - 3使用了Room...
Person - 1使用了Room...
Person - 2使用了Room...
Person - 6使用了Room...
Person - 5使用了Room...
Person - 7使用了Room...
Person - 8使用了Room...
Person - 9使用了Room...
Person - 0退出了Room
Person - 7退出了Room
Person - 1退出了Room
Person - 11使用了Room...
Person - 2退出了Room
Person - 6退出了Room
Person - 5退出了Room
Person - 8退出了Room
Person - 4退出了Room
Person - 3退出了Room
Person - 14使用了Room...
Person - 13使用了Room...
Person - 12使用了Room...
Person - 10使用了Room...
Person - 9退出了Room
Person - 11退出了Room
Person - 10退出了Room
Person - 12退出了Room
Person - 14退出了Room
Person - 13退出了Room
```

# 线程间协作
线程之间的协作最经典的就是生产者-消费者模型。

使用Object的wait()和notify()实现：

```
package parallel;

import java.util.PriorityQueue;

public class ProducerConsumerSample {
  private int queueSize = 10;
  private PriorityQueue<Integer> queue = new PriorityQueue<Integer>(queueSize);

  public static void main(String[] args) {
    ProducerConsumerSample sample = new ProducerConsumerSample();
    Producer producer = sample.new Producer();
    Consumer consumer = sample.new Consumer();

    producer.start();
    consumer.start();
  }

  class Consumer extends Thread {

    @Override
    public void run() {
      while (true) {
        synchronized (queue) {
          while (queue.size() == 0) {
            try {
              System.out.println("队列空，等待数据");
              queue.wait();
            } catch (InterruptedException e) {
              e.printStackTrace();
              queue.notify();
            }
          }
          queue.poll(); // 每次移走队首元素
          queue.notify();
          System.out.println("从队列取走一个元素，队列剩余" + queue.size() + "个元素");
        }
      }
    }
  }

  class Producer extends Thread{
    @Override
    public void run() {
      while(true){
        synchronized (queue) {
            while(queue.size() == queueSize){
                try {
                    System.out.println("队列满，等待有空余空间");
                    queue.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    queue.notify();
                }
            }
            queue.offer(1);        //每次插入一个元素
            queue.notify();
            System.out.println("向队列取中插入一个元素，队列剩余空间："+(queueSize-queue.size()));
        }
    }
    }
  }
}

```

Condition是在java 1.5中才出现的，它用来替代传统的Object的wait()、notify()实现线程间的协作，相比使用Object的wait()、notify()，使用Condition1的await()、signal()这种方式实现线程间协作更加安全和高效。

- Condition是个接口，基本的方法就是await()和signal()方法;
- Condition依赖于Lock接口，生成一个Condition的基本代码是lock.newCondition();
- 调用Condition的await()和signal()方法，都必须在lock保护之内，就是说必须在lock.lock()和lock.unlock之间才可以使用;

　　- Conditon中的await()对应Object的wait()；
　　- Condition中的signal()对应Object的notify()；
　　- Condition中的signalAll()对应Object的notifyAll()。

使用Condition实现:
```
package parallel;

import java.util.PriorityQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ProducerConsumerConditionSample2 {
  private int queueSize = 10;
  private PriorityQueue<Integer> queue = new PriorityQueue<Integer>(queueSize);
  private Lock lock = new ReentrantLock();
  private Condition notFull = lock.newCondition();
  private Condition notEmpty = lock.newCondition();

  public static void main(String[] args) {
    ProducerConsumerConditionSample2 sample = new ProducerConsumerConditionSample2();
    Producer producer = sample.new Producer();
    Consumer consumer = sample.new Consumer();

    producer.start();
    consumer.start();
  }

  class Consumer extends Thread {

    @Override
    public void run() {
      while (true) {
        lock.lock();
        try {
          while (queue.size() == 0) {
            try {
              System.out.println("队列空，等待数据");
              notEmpty.await();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
          queue.poll(); // 每次移走队首元素
          notFull.signal();
          System.out.println("从队列取走一个元素，队列剩余" + queue.size() + "个元素");
        } finally {
          lock.unlock();
        }
      }
    }
  }

  class Producer extends Thread {
    @Override
    public void run() {
      while (true) {
        lock.lock();
        try {
          while (queue.size() == queueSize) {
            try {
              System.out.println("队列满，等待有空余空间");
              notFull.await();
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
          }
          queue.offer(1); // 每次插入一个元素
          notEmpty.signal();
          System.out.println("向队列取中插入一个元素，队列剩余空间：" + (queueSize - queue.size()));
        } finally {
          lock.unlock();
        }
      }
    }
  }
}

```
