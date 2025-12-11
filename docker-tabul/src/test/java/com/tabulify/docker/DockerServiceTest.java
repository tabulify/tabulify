package com.tabulify.docker;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.service.Service;
import com.tabulify.service.ServiceAttributeBase;
import com.tabulify.service.Services;
import com.tabulify.os.Oss;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class DockerServiceTest {


  @Test
  void baseline() {

    try (Tabular tabular = Tabular.tabularWithCleanEnvironment()) {

      // docker run -d -v /path/to/files:/var/www/ -p 80:80 busybox httpd -f -h /var/www/
      Attribute serviceName = tabular.getVault().createAttribute(ServiceAttributeBase.NAME, "httpbin", Origin.DEFAULT);
      Attribute serviceType = tabular.getVault().createAttribute(ServiceAttributeBase.TYPE, "docker", Origin.DEFAULT);
      Service service = Services.createService(tabular, serviceName, serviceType);
      Assertions.assertEquals(DockerService.class, service.getClass());

      Integer port = Oss.getRandomAvailablePort();
      DockerService dockerService = ((DockerService) service)
        .setImage("kennethreitz/httpbin")
        .addPortBinding(port, 80)
        .setEnvs(Map.of("ENV", "value"))
        .setVolumes(Map.of("ENV", "value"))
        .setCommand("httpd", "-f", "-h", "/var/www");
      dockerService.start();

      Assertions.assertTimeoutPreemptively(java.time.Duration.ofMillis(3000), () -> {
        while (!dockerService.isStarted()) {
          //noinspection BusyWait
          Thread.sleep(100);
        }
      });

      dockerService.stop();


    }

  }
}
