---
title: StatsD - Graphite - Grafana
---
#1. StatsD - metrics data collecting

  - In your application code, use the StatsD client (e.g. Java client library) to collect and send the statistics and aggregation data to StatsD server;
  - Not need to pre-define the metrics in anywhere, just place them into your application code;
  - By default, stats data are aggregated and sent to Graphite server by every 10 seconds, so think this near-realtime;

#2. Graphite - metrics data graphing and storage
  - Store numeric time-series data: The metric data would be stored into Graphite server (include a Whisper database);
  - Render the graph for metrics data per the metrics demand;
  - The pre-built UI dashboard is not powerful shown as below, but can easily use it to view the metrics data;
  -  For production environment, Graphite server should be running as cluster instead of stand-alone server;



#3. Grafana - powerful dashboard for visualizing the metrics data
  - easy to integrate with Graphite to visualize the metrics data;
  - input the Graphite HTTP URL to link to Graphite as data source;
  - powerful pre-built reporting charts and dashboard;

#4.  Sample Code:
 - easy to feed the data to StatsD -> Graphite -> Grafana
```
 private static final StatsDClient statsd = new NonBlockingStatsDClient("app.api-analytics.sample", "127.0.0.1",
      8125);
 statsd.count("get.request", (long)(Math.random()* 10)); // Request count of this API
```
