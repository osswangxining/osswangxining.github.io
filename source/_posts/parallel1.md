---
title: Java并发编程(1) - 同步/并发容器
date: 2016-1-12 20:46:25
categories:
- 分布式&云计算
- Java并发编程
tags:
  - 分布式
  - Java并发编程
---

# 同步/并发容器
## 同步容器
在Java的集合容器框架中，主要有四大类别：List、Set、Queue、Map。其中，Collection和Map是一个顶层接口，而List、Set、Queue则继承了Collection接口，分别代表数组、集合和队列这三大类容器。
- ArrayList、LinkedList都是实现了List接口; - 非线程安全的
- HashSet实现了Set接口; - 非线程安全的
- Deque（双向队列，允许在队首、队尾进行入队和出队操作）继承了Queue接口，PriorityQueue实现了Queue接口;
- LinkedList（实际上是双向链表）实现了了Deque接口;

如果有多个线程并发地访问这些容器时，就会出现问题。因此，在编写程序时，必须要求程序员手动地在任何访问到这些容器的地方进行同步处理，这样导致在使用这些容器的时候非常地不方便。Java提供了同步容器供用户使用：
- Vector、Stack、HashTable
  - Vector实现了List接口，Vector实际上就是一个数组，和ArrayList类似，但是Vector中的方法都是synchronized方法，即进行了同步措施。
  - Stack也是一个同步容器，它的方法也用synchronized进行了同步，它实际上是继承于Vector类。
  - HashTable实现了Map接口，它和HashMap很相似，但是HashTable进行了同步处理，而HashMap没有。

- Collections类中提供的静态工厂方法创建的类
  - 在Collections类中提供了大量的方法，比如对集合或者容器进行排序、查找等操作。最重要的是，在它里面提供了几个静态工厂方法来创建同步容器类;
<!-- more -->
### 同步容器的问题
同步容器中的方法采用了synchronized进行了同步，这必然会影响到执行性能.同步容器将所有对容器状态的访问都串行化了，这样保证了线程的安全性，但代价就是严重降低了并发性，当多个线程竞争容器时，吞吐量严重降低。

如下的测试示例，进行同样多的插入操作，Vector的耗时是ArrayList的两倍。
```
public class VectorAndArrayListSample {
  static Vector<Integer> vector = new Vector<Integer>();

  public static void main(String[] args) {
    ArrayList<Integer> list = new ArrayList<Integer>();

    long start = System.currentTimeMillis();
    int count = 1000* 1000;
    for (int i = 0; i < count; i++)
      list.add(i);
    long end = System.currentTimeMillis();
    System.out.println("ArrayList进行" + count + "次插入操作耗时：" + (end - start) + "ms");
    start = System.currentTimeMillis();
    for (int i = 0; i < count; i++)
      vector.add(i);
    end = System.currentTimeMillis();
    System.out.println("Vector进行" + count + "次插入操作耗时：" + (end - start) + "ms");

  }
```

执行结果说明了两者的执行效率:
```
ArrayList进行1000000次插入操作耗时：27ms
Vector进行1000000次插入操作耗时：42ms
```

此外，对Vector等容器并发地进行迭代修改时，会报ConcurrentModificationException异常。

## 并发容器
Java5.0开始针对多线程并发访问设计，提供了并发性能较好的并发容器，引入了java.util.concurrent包。主要解决了两个问题：
- 1）根据具体场景进行设计，尽量避免synchronized，提供并发性。
- 2）定义了一些并发安全的复合操作，并且保证并发环境下的迭代操作不会出错。

### ConcurrentHashMap
ConcurrentHashMap可以做到读取数据不加锁，并且其内部的结构可以让其在进行写操作的时候能够将锁的粒度保持地尽量地小，不用对整个ConcurrentHashMap加锁。HashMap是根据散列值分段存储的，同步Map在同步的时候锁住了所有的段，而ConcurrentHashMap加锁的时候根据散列值锁住了散列值锁对应的那段，因此提高了并发性能。ConcurrentHashMap也增加了对常用复合操作的支持，比如"若没有则添加"：putIfAbsent()，替换：replace()。这2个操作都是原子操作。

ConcurrentHashMap为了提高本身的并发能力，在内部采用了一个叫做Segment的结构，一个Segment其实就是一个类Hash Table的结构，Segment内部维护了一个链表数组.ConcurrentHashMap定位一个元素的过程需要进行两次Hash操作，第一次Hash定位到Segment，第二次Hash定位到元素所在的链表的头部，因此，这一种结构的带来的副作用是Hash的过程要比普通的HashMap要长，但是带来的好处是写操作的时候可以只对元素所在的Segment进行加锁即可，不会影响到其他的Segment，这样，在最理想的情况下，ConcurrentHashMap可以最高同时支持Segment数量大小的写操作（刚好这些写操作都非常平均地分布在所有的Segment上），所以，通过这一种结构，ConcurrentHashMap的并发能力可以大大的提高。

CurrentHashMap的初始化一共有三个参数，一个initialCapacity，表示初始的容量，一个loadFactor，表示负载参数，最后一个是concurrentLevel，代表ConcurrentHashMap内部的Segment的数量，ConcurrentLevel一经指定，不可改变，后续如果ConcurrentHashMap的元素数量增加导致ConrruentHashMap需要扩容，ConcurrentHashMap不会增加Segment的数量，而只会增加Segment中链表数组的容量大小，这样的好处是扩容过程不需要对整个ConcurrentHashMap做rehash，而只需要对Segment里面的元素做一次rehash就可以了。
```
public ConcurrentHashMap(int initialCapacity,
                         float loadFactor, int concurrencyLevel)
```

### CopyOnWrite容器
CopyOnWrite容器即写时复制的容器。通俗的理解是当我们往一个容器添加元素的时候，不直接往当前容器添加，而是先将当前容器进行Copy，复制出一个新的容器，然后新的容器里添加元素，添加完元素之后，再将原容器的引用指向新的容器。这样做的好处是我们可以对CopyOnWrite容器进行并发的读，而不需要加锁，因为当前容器不会添加任何元素。所以CopyOnWrite容器也是一种读写分离的思想，读和写不同的容器。
CopyOnWrite容器有很多优点，但是同时也存在两个问题，即内存占用问题和数据一致性问题：
- 内存占用问题。

  因为CopyOnWrite的写时复制机制，所以在进行写操作的时候，内存里会同时驻扎两个对象的内存，旧的对象和新写入的对象（注意:在复制的时候只是复制容器里的引用，只是在写的时候会创建新对象添加到新容器里，而旧容器的对象还在使用，所以有两份对象内存）。如果这些对象占用的内存比较大，比如说200M左右，那么再写入100M数据进去，内存就会占用300M，那么这个时候很有可能造成频繁的Yong GC和Full GC。之前我们系统中使用了一个服务由于每晚使用CopyOnWrite机制更新大对象，造成了每晚15秒的Full GC，应用响应时间也随之变长。

  针对内存占用问题，可以通过压缩容器中的元素的方法来减少大对象的内存消耗，比如，如果元素全是10进制的数字，可以考虑把它压缩成36进制或64进制。或者不使用CopyOnWrite容器，而使用其他的并发容器，如ConcurrentHashMap。

- 数据一致性问题。

  CopyOnWrite容器只能保证数据的最终一致性，不能保证数据的实时一致性。
