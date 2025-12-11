package com.tabulify.niofs.http;

import com.tabulify.test.SingletonTestContainer;
import org.junit.jupiter.api.BeforeEach;

public class HttpBaseTest {

   String httpBinUrl;

  @BeforeEach
  void setUp() {
    if (this.httpBinUrl == null) {
      // .withBindMount(Path.of("src", "test", "resources"), "/httpbin/static/resources")
      // Does not work unfortunately, we can mount
        SingletonTestContainer singletonTestContainer = new SingletonTestContainer("httpbin", "kennethreitz/httpbin:latest")
        .withPort(8085, 80)
        .startContainer();
      this.httpBinUrl = "http://" + singletonTestContainer.getHostName() + ":" + singletonTestContainer.getHostPort();
    }
  }

}
