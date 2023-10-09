package net.bytle.test;

import net.bytle.os.Oss;
import net.bytle.type.Strings;
import org.testcontainers.containers.GenericContainer;

import java.util.Arrays;
import java.util.List;

/**
 *
 * Implementation of a singleton container
 * based on the <a href="https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers">documentation</a>
 *
 * Features, if the port is not available:
 *   * the container will not be started
 *   * otherwise, it will be started and the docker command will be printed (in order to help the dev)
 *
 */
public class TestContainerWrapper {

  private GenericContainer container;
  private String image;
  private String hostName = "localhost";
  private Integer port;
  private String name;

  public TestContainerWrapper(String name, String dockerImageName) {
    this.name = name;
    this.image = dockerImageName;
    this.container = new GenericContainer(dockerImageName);
  }

  public TestContainerWrapper(String name, GenericContainer container) {
    this.container = container;
    this.name = name;
    this.image = container.getDockerImageName();
  }

  public TestContainerWrapper startContainer() {

    /**
     * Do we need to start the container
     */
    Boolean startContainer = true;
    if (!Oss.portAvailable(this.port)){
      startContainer = false;
      System.out.println("The port is already busy, the container will not start.");
    } else {
      System.out.println("The port is available, the container will start");
    }


    if (startContainer) {

      System.out.println("Starting the container");
      System.out.println("If you don't want to start and stop the container for each test.");
      System.out.println("You can start it with the following command on Windows:");
      System.out.println();
      System.out.println(this.createDockerCommand());
      System.out.println();
      container.start();
      this.hostName = container.getContainerIpAddress();
      this.port = container.getFirstMappedPort();

    } else {

      System.out.println("The container is already started");


    }

    return this;
  }

  public String createDockerCommand() {
    String windowsLineSeparator = "^"+ Strings.EOL;
    String bashLineSeparator = "\\"+ Strings.EOL;
    List<String> separators = Arrays.asList(windowsLineSeparator,bashLineSeparator);
    String spaces = "    ";

    StringBuilder stringBuilder = new StringBuilder();
    for (String separator : separators) {
      stringBuilder.append(Strings.EOL);
      if (separator.equals(windowsLineSeparator)){
        stringBuilder.append("Cmd:").append(Strings.EOL);
      } else {
        stringBuilder.append("Bash:").append(Strings.EOL);
      }
      stringBuilder.append("docker run ").append(separator);
      for (Object env : container.getEnv()) {
        stringBuilder.append(spaces).append("-e ").append(env).append(" ").append(separator);
      }
      stringBuilder
        .append(spaces).append("-p ").append(port).append(":").append(port).append(" ").append(separator)
        .append(spaces).append("-d ").append(separator)
        .append(spaces).append("--name ").append(this.name).append(" ").append(separator)
        .append(spaces).append(this.image).append(Strings.EOL);
    }


    return stringBuilder.toString();
  }

  public TestContainerWrapper withEnv(String key, String value) {
    this.container.withEnv(key, value);
    return this;
  }

  public TestContainerWrapper withPort(Integer port) {
    this.port = port;
    this.container.withExposedPorts(port);
    return this;
  }

  public String getHostName() {
    return this.hostName;
  }

  public Integer getPort() {
    return this.port;
  }

  public boolean isRunning() {
    return container.isRunning();
  }

  public void stop() {
    container.stop();
  }


}
