---
title: Redis
---

docker run --name myredis -v /Users/xiningwang/nosql/redis/redis-3.2.8/data:/data -p 6379:6379 -d redis redis-server --appendonly yes
