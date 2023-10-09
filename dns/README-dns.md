# Dns


## About
Wraps DNSJava




## Vertx

### Interop
DNS Java is reactive/future ready and can use [CompletionInterop](https://github.com/eclipse-vertx/vert.x/blob/master/src/main/java/examples/CompletionStageInteropExamples.java)

### Vertx Doc and example
[Vertx example for info](https://github.com/eclipse-vertx/vert.x/blob/master/src/main/asciidoc/dns.adoc)

Example code from:
  * [DNSExamples.java](https://github.com/eclipse-vertx/vert.x/blob/master/src/main/java/examples/DNSExamples.java)
  * [CoreExamples.java](https://github.com/eclipse-vertx/vert.x/blob/master/src/main/java/examples/CoreExamples.java)

```java
DnsClient client = vertx.createDnsClient(new DnsClientOptions()
      .setPort(53)
      .setHost("10.0.0.1")
      .setQueryTimeout(10000)
    );
```

```java
Vertx vertx = Vertx.vertx(
  new VertxOptions().
    setAddressResolverOptions(
    new AddressResolverOptions()
        .addServer("192.168.0.1")
        .addServer("192.168.0.2:40000")
        .setHostsPath("/path/to/hosts")
        .addSearchDomain("foo.com").addSearchDomain("bar.com")
    )
);
```

### Test Fake DNS

https://github.com/eclipse-vertx/vert.x/blob/master/src/test/java/io/vertx/test/fakedns/FakeDNSServer.java
