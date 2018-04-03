---
title: Running Spark on YARN
date: 2016-3-9 20:46:25
---
### To launch a Spark application in cluster/yarn mode:
```
$ ./bin/spark-submit --class path.to.your.Class --master yarn --deploy-mode cluster [options] <app jar> [app options]
```

For example:
```
$ ./bin/spark-submit --class org.apache.spark.examples.SparkPi \
    --master yarn \
    --deploy-mode cluster \
    --driver-memory 4g \
    --executor-memory 2g \
    --executor-cores 1 \
    --queue thequeue \
    lib/spark-examples*.jar \
    10
```
<!-- more -->
See the detail from:
http://spark.apache.org/docs/latest/running-on-yarn.html
