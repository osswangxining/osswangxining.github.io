---
title: HBase
---

## HBase QuickStart
HBase是一个开源的分布式存储系统。他可以看作是Google的Bigtable的开源实现。如同Google的Bigtable使用Google File System一样，HBase构建于和Google File System类似的Hadoop HDFS之上。

With the completed config, issue the command: bin/start-hbase.sh

```
2017-05-03 11:54:25,802 INFO  [main] http.HttpServer: Jetty bound to port 16010
2017-05-03 11:54:25,802 INFO  [main] mortbay.log: jetty-6.1.26
2017-05-03 11:54:25,998 INFO  [main] mortbay.log: Started SelectChannelConnector@0.0.0.0:16010
```
Go to http://localhost:16010 to view the HBase Web UI.


## HBase连接池管理
ConnectionFactory 是一个不可实例化的类，专门用于创建HBase的Connection。最简单的创建Connection实例的方式是ConnectionFactory.createConnection(config)，该方法创建了一个连接到集群的Connection实例，该实例被创建的程序管理。通过这个Connection实例，可以使用Connection.getTable()方法取得Table，例如:
```
 Connection connection = ConnectionFactory.createConnection(config);
 Table table = connection.getTable(TableName.valueOf("table1"));
 try {
  // Use the table as needed, for a single operation and a single thread
 } finally {
  table.close();
  connection.close();
 }
 ```
 **** HConnectionManager在新版本中已经不建议使用。

## HBase客户端的使用

 [客户端的使用Sample](samplecodes/hbaseclient/HBaseSample.java)

## Cassandra
Apache Cassandra是高度可扩展的，高性能的分布式NoSQL数据库。 Cassandra旨在处理许多商品服务器上的大量数据，提供高可用性而无需担心单点故障。

Cassandra具有能够处理大量数据的分布式架构。 数据放置在具有多个复制因子的不同机器上，以获得高可用性，而无需担心单点故障。它在其节点之间具有对等分布式系统，数据分布在集群中的所有节点上。
- 在Cassandra中，每个节点是独立的，同时与其他节点互连。 集群中的所有节点都扮演着相同的角色。
- 集群中的每个节点都可以接受读取和写入请求，而不管数据实际位于集群中的位置。
- 在一个节点发生故障的情况下，可以从网络中的其他节点提供读/写请求。

## Cassandra和HBase对比

Cassandra可以看作是Amazon Dynamo的开源实现。和Dynamo不同之处在于，Cassandra结合了Google Bigtable的ColumnFamily的数据模型。可以简单地认为，Cassandra是一个P2P的，高可靠性并具有丰富的数据模型的分布式文件系统。

- Cassandra部署更简单。Cassandra只有一种角色，而HBase除了Region Server外还需要Zookeeper来同步集群状态
- 数据一致性是否可配置。Cassandra的数据一致性是可配置的，可以更改为最终一致性，而HBase是强一致性的
- 负载均衡算法不同。Cassandra通过一致性哈希来决定数据存储的位置，而HBase靠Master节点管理数据的分配，将过热的节点上的Region动态分配给负载较低的节点。因此Cassandra的平均性能会优于HBase，但是HBase有Master节点，热数据的负载更均衡。
- 单点问题。正是由于HBase存在Master节点，因此会存在单点问题。

<table>
    <th>
        <td>HBase</td>
        <td>Cassandra</td>
    </th>
    <tr>
        <td>语言/License/交互协议</td>
        <td>Java/Apache/HTTP/REST (also Thrift)	</td>
        <td>Java/Apache/Custom, binary (Thrift)</td>
    </tr>
    <tr>
        <td>出发点</td>
        <td>BigTable</td>
        <td>BigTable and Dynamo</td>
    </tr>
    <tr>
        <td>架构</td>
        <td>master/slave	</td>
        <td>p2p</td>
    </tr>
    <tr>
        <td>高可用性</td>
        <td>NameNode是HDFS的单点故障点	</td>
        <td>P2P和去中心化设计，不会出现单点故障</td>
    </tr>
    <tr>
        <td>伸缩性	</td>
        <td>Region Server扩容，通过将自身发布到Master，Master均匀分布Region	</td>
        <td>扩容需在Hash Ring上多个节点间调整数据分布</td>
    </tr>
    <tr>
        <td>一致性	</td>
        <td>强一致性	</td>
        <td>最终一致性，Quorum NRW策略</td>
    </tr>
    <tr>
        <td>存储目标/数据分布</td>
        <td>大文件/表划分为多个region存在不同region server上	</td>
        <td>小文件/改进的一致性哈希（虚拟节点）</td>
    </tr>
    <tr>
        <td>成员通信及错误检测	</td>
        <td>Zookeeper	</td>
        <td>基于Gossip/P2P</td>
    </tr>
    <tr>
        <td>读写性能	</td>
        <td>数据读写定位可能要通过最多6次的网络RPC，性能较低。		</td>
        <td>数据读写定位非常快</td>
    </tr>
    <tr>
        <td>数据冲突处理	</td>
        <td>乐观并发控制（optimistic concurrency control）		</td>
        <td>向量时钟</td>
    </tr>
    <tr>
        <td>临时故障处理	</td>
        <td>Region Server宕机，重做HLog		</td>
        <td>数据回传机制：某节点宕机，hash到该节点的新数据自动路由到下一节点做 hinted handoff，源节点恢复后，推送回源节点。</td>
    </tr>
    <tr>
        <td>永久故障恢复		</td>
        <td>Region Server恢复，master重新给其分配region	</td>
        <td>Merkle 哈希树，通过Gossip协议同步Merkle Tree，维护集群节点间的数据一致性</td>
    </tr>
    <tr>
        <td>CAP	</td>
        <td>1，强一致性，0数据丢失。2，可用性低。3，扩容方便。			</td>
        <td>1，弱一致性，数据可能丢失。2，可用性高。3，扩容方便。</td>
    </tr>

</table>
