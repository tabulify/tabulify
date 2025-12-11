package com.tabulify.test;


import com.tabulify.test.util.DockerContainer;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static com.tabulify.test.PortAvailability.MAX_PORT_NUMBER;
import static com.tabulify.test.PortAvailability.MIN_PORT_NUMBER;


/**
 * A wrapper around test-container
 * * to implement a singleton container based on port busy-ness
 * * to print the command so that the container is long-lived
 * <p>
 * Features, if the port is not available:
 * * the container will not be started
 * * otherwise, it will be started and the docker command will be printed (in order to help the dev)
 * <p>
 * WaitingFor is not part of the api.
 * You can also use the correspondent test container in the constructor
 * that will have most of this default value.
 * <p>
 * It's not the same as <a href="https://www.testcontainers.org/test_framework_integration/manual_lifecycle_control/#singleton-containers">documentation</a>
 * where the container is static and therefore created only when the abstract class is loaded
 */
public class SingletonTestContainer {

  private final GenericContainer<?> container;
  private final String image;
  private final DockerContainer.Conf dockerContainerCommand;
  private String hostName = "localhost";
  private Integer hostPort;
  private Integer containerPort;
  private final String name;

  /**
   * By default, you should use this constructor as the pre-created container
   * of test container have also defaults
   * such as the WaitingFor conditions against logs to check if the container has started.
   */
  @SuppressWarnings("unused")
  public SingletonTestContainer(String name, GenericContainer<?> container) {
    this.container = container;
    this.name = name;
    this.image = container.getDockerImageName();
    this.dockerContainerCommand = DockerContainer.createConf(image).setContainerName(name);
  }

  public SingletonTestContainer(String containerName, String dockerImageName) {
    this(containerName, new GenericContainer<>(dockerImageName));
  }


  /**
   * Start the container if it's not detected already running (by port)
   */
  public SingletonTestContainer startContainer() {

    /**
     * Container port check
     */
    if (containerPort == null) {
      throw new RuntimeException("The container port should not be null");
    }
    /**
     * Host port init
     */
    if (this.hostPort == null) {
      if (this.containerPort >= MIN_PORT_NUMBER && this.containerPort <= MAX_PORT_NUMBER) {
        this.hostPort = containerPort;
        container.setPortBindings(List.of(hostPort + ":" + containerPort));
      }
    } else {
      if (this.hostPort < MIN_PORT_NUMBER || this.hostPort > MAX_PORT_NUMBER) {
        throw new RuntimeException("The host port is a privileged port" + hostPort + ". Choose one above " + MIN_PORT_NUMBER + " and below " + MAX_PORT_NUMBER);
      }
    }

    /*
     * Do we need to start the container
     */
    boolean startContainer = true;
    if (hostPort != null) {
      if (!PortAvailability.portAvailable(hostPort)) {
        startContainer = false;
        System.out.println("The port " + hostPort + " is already busy, the container will not start.");
      } else {
        System.out.println("The port is available, the container will start");
      }
    }


    if (startContainer) {

      System.out.println("Starting the container");
      System.out.println("If you don't want to start and stop the container for each test.");
      if (this.hostPort == null) {
        System.out.println("Set a host port. Your container port " + this.containerPort + " is a privileged port and cannot be used on the host. Choose one above " + MIN_PORT_NUMBER + " and below " + MAX_PORT_NUMBER);
      } else {
        System.out.println("You can start it with the following command:");
        System.out.println();
        System.out.println(this.dockerContainerCommand.build().createDockerCommand());
      }
      System.out.println();

      container.start();
      this.hostName = container.getHost();
      if (this.hostPort == null) {
        this.hostPort = container.getFirstMappedPort();
      }


    } else {

      System.out.println("The container is already started");


    }

    return this;
  }


  public SingletonTestContainer withEnv(String key, String value) {
    this.container.withEnv(key, value);
    this.dockerContainerCommand.setEnv(key, value);
    return this;
  }

  public SingletonTestContainer withEnvs(Map<String, String> envs) {
    for (Map.Entry<String, String> entry : envs.entrySet()) {
      withEnv(entry.getKey(), entry.getValue());
    }
    return this;
  }

  /**
   * @param containerPort - the container port
   *                      When the container starts, it will try to bind it to this port or use another one
   */
  public SingletonTestContainer withPort(Integer containerPort) {
    this.containerPort = containerPort;
    this.container.addExposedPort(containerPort);
    this.dockerContainerCommand.setPortBonding(containerPort, containerPort);
    return this;
  }

  public SingletonTestContainer withPort(Integer hostPort, Integer containerPort) {
    this.hostPort = hostPort;
    this.containerPort = containerPort;
    container.setPortBindings(List.of(hostPort + ":" + containerPort));
    container.withExposedPorts(containerPort);
    this.dockerContainerCommand.setPortBonding(hostPort, containerPort);
    return this;
  }

  @SuppressWarnings("unused")
  public String getHostName() {
    return this.hostName;
  }

  @SuppressWarnings("unused")
  public Integer getHostPort() {
    return this.hostPort;
  }

  @SuppressWarnings("unused")
  public boolean isRunning() {
    return container.isRunning();
  }

  /**
   * Note that Test Container Ryuk monitor and terminate Testcontainers containers on JVM exit
   */
  public void stop() {
    container.stop();
  }


  /**
   * [Bind Mount](https://docs.docker.com/engine/storage/bind-mounts/)
   *
   * @param containerPath - the container path
   * @param hostPath      - the host path
   */
  public SingletonTestContainer withBindMount(Path hostPath, String containerPath) {
    if (!Files.exists(hostPath)) {
      throw new RuntimeException("The host path (" + hostPath + ") does not exists");
    }
    this.container.withFileSystemBind(hostPath.toAbsolutePath().toString(), containerPath, BindMode.READ_WRITE);
    this.dockerContainerCommand.setVolumeBonding(hostPath, Path.of(containerPath));
    return this;
  }

  public GenericContainer<?> getContainer() {
    return this.container;
  }


  public String createDockerCommand() {
    return this.dockerContainerCommand.build().createDockerCommand();
  }
}
