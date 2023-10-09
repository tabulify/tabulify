# Cache

## About

As of now, we use Caffeine as cache library.

## Vertx - Infinispan

In the vertx realm, they advertise InfiniSpan that can be used:
  * in embedded
  * as server mode

[Embedded mode]https://infinispan.org/docs/stable/titles/embedding/embedding.html#configuring-embedded-caches_creating-embedded-caches
can be persisted with the PERMANENT flag (to survive restarts).

There is also a [Asynchronous API](https://infinispan.org/docs/stable/titles/embedding/embedding.html#cache_asynchronous_api)


## Others
### JCache
JCache specifies a standard Java API for caching temporary Java objects in memory.
http://www.jcp.org/en/jsr/detail?id=107

https://github.com/infinispan/infinispan-simple-tutorials/blob/main/infinispan-embedded/jcache/src/main/java/org/infinispan/tutorial/simple/jcache/InfinispanJCache.java

Caffeine
Infinispan
