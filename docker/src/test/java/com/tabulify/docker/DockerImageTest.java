package com.tabulify.docker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DockerImageTest {

  @Test
  void baseline() {

    DockerImage busyBox = DockerImage.create("busybox");
    Assertions.assertEquals("docker.io/library/busybox:latest", busyBox.getCanonicalName());

    DockerImage smtp = DockerImage.create("axllent/mailpit:v1.25.1");
    Assertions.assertEquals("docker.io/axllent/mailpit:v1.25.1", smtp.getCanonicalName());

  }

}
