# Infini Span Embedded



Tried but the cache manager start method takes 3 minutes.
And no-one answered: https://github.com/infinispan/infinispan/discussions/11773

InfiniSpan as key-value store is only good as third database service then.

For map database cache, we developed the CaffeinePersistenceMap.

## Demo


```kotlin
// Infinispan (Cache)
// https://infinispan.org/docs/stable/titles/embedding/embedding.html
implementation("org.infinispan:infinispan-bom:$infinispanVersion")
implementation("org.infinispan:infinispan-core:$infinispanVersion")
```
Basic Code: https://gist.github.com/gerardnico/b17baa72d6f486a14546b993c7c71885
