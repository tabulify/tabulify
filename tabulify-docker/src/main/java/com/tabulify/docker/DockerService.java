package com.tabulify.docker;

import com.eraldy.docker.DockerContainer;
import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.service.Service;
import com.tabulify.service.ServiceAttributeBase;
import com.tabulify.service.ServiceAttributeEnum;
import net.bytle.exception.CastException;
import net.bytle.type.Casts;
import net.bytle.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.tabulify.docker.DockerAttribute.*;

public class DockerService extends Service {

  DockerContainer dockerContainer;

  /**
   * We store ports as docker compose as a list of string,
   * but we allow only for now a map of integer
   * This variable contains all runtime ports
   */
  private Map<Integer, Integer> ports = new HashMap<>();

  public DockerService(Tabular tabular, Attribute name) {
    super(tabular, name);
    this.addAttributesFromEnumAttributeClass(DockerAttribute.class);
    this.addAttribute(ServiceAttributeBase.TYPE, Origin.DEFAULT, DockerProvider.DOCKER_TYPE);
  }

  @Override
  public Service addAttribute(KeyNormalizer keyNormalizer, Origin origin, Object value) {
    DockerAttribute dockerAttribute;
    try {
      dockerAttribute = Casts.cast(keyNormalizer, DockerAttribute.class);
    } catch (CastException e) {
      return super.addAttribute(keyNormalizer, origin, value);
    }
    if (dockerAttribute == PORTS) {
      // the map object value is create at constructor time
      List<String> portBindings;
      try {
        portBindings = Casts.castToNewList(value, String.class);
      } catch (CastException e) {
        throw new IllegalArgumentException("The port binding value (" + value + ") of the service (" + this + ") is not a list");
      }
      for (String portBinding : portBindings) {
        String[] portsAsString = portBinding.split(":");
        switch (portsAsString.length) {
          case 1:
            String portAsString = portsAsString[0];
            try {
              Integer port = Casts.cast(portAsString, Integer.class);
              ports.put(port, port);
            } catch (CastException e) {
              throw new IllegalArgumentException("The port binding value (" + portBinding + ") of the service (" + this + ") is not an integer");
            }
            break;
          case 2:
            Integer hostPort;
            String hostPortString = portsAsString[0];
            try {
              hostPort = Casts.cast(hostPortString, Integer.class);
            } catch (CastException e) {
              throw new IllegalArgumentException("The port binding value (" + portBinding + ") of the service (" + this + ") contains a host port value (" + hostPortString + ") that is not an integer");
            }
            Integer containerPort;
            String containerPortString = portsAsString[1];
            try {
              containerPort = Casts.cast(containerPortString, Integer.class);
            } catch (CastException e) {
              throw new IllegalArgumentException("The port binding value (" + portBinding + ") of the service (" + this + ") contains a container port value (" + containerPortString + ") that is not an integer");
            }
            ports.put(hostPort, containerPort);
            break;
          default:
            throw new IllegalArgumentException("The port binding value (" + portBinding + ") of the service (" + this + ") should be a map of integer.");
        }
      }

    }
    return addAttribute(dockerAttribute, origin, value);
  }

  @Override
  public List<Class<? extends ServiceAttributeEnum>> getAttributeEnums() {
    List<Class<? extends ServiceAttributeEnum>> list = new ArrayList<>(super.getAttributeEnums());
    list.add(DockerAttribute.class);
    return list;
  }

  @Override
  public void start() {

    getOrBuildDockerContainer().run();

  }

  @Override
  public void drop() {
    getOrBuildDockerContainer().rm();
  }

  @Override
  public boolean isStarted() {
    return getOrBuildDockerContainer().isRunning();
  }

  @Override
  public void stop() {
    getOrBuildDockerContainer().stop();
  }

  private DockerContainer getOrBuildDockerContainer() {
    if (dockerContainer != null) {
      return dockerContainer;
    }
    String imageName = this.getImageName();
    if (imageName == null) {
      throw new RuntimeException("The image name attribute " + IMAGE + " is mandatory for the docker service " + this);
    }
    dockerContainer = DockerContainer.createConf(imageName)
      .setContainerName(this.getName().toDockerCase())
      .setPortBindings(this.getPortBindings())
      .setEnvs(this.getEnvs())
      .setCommand(this.getCommand())
      .build();
    return dockerContainer;
  }

  private List<String> getCommand() {
    return Casts.castToNewListSafe(this.getAttribute(COMMAND).getValueOrDefault(), String.class);
  }

  public Map<String, String> getEnvs() {
    return Casts.castToSameMapSafe(this.getAttribute(ENVIRONMENT).getValueOrDefault(), String.class, String.class);
  }


  public Map<Integer, Integer> getPortBindings() {
    return ports;
  }

  private String getImageName() {
    return (String) this.getAttribute(IMAGE).getValueOrDefault();
  }

  public DockerService addPortBinding(int hostPort, int containerPort) {

    this.getPortBindings().put(hostPort, containerPort);

    return this;
  }

  public DockerService setImage(String s) {
    this.getAttribute(DockerAttribute.IMAGE).setPlainValue(s);
    return this;
  }

  @Override
  public String toString() {
    return this.getName().toDockerCase();
  }

  @Override
  public boolean exists() {
    return getOrBuildDockerContainer().exists();
  }

  public DockerService setPorts(Map<Integer, Integer> hostContainerPortMap) {

    this.getAttribute(PORTS).setPlainValue(
      hostContainerPortMap.entrySet()
        .stream()
        .map(entry -> entry.getKey() + ":" + entry.getValue())
        .collect(Collectors.toList())
    );
    this.ports = hostContainerPortMap;
    return this;
  }

  public DockerService setVolumes(Map<String, String> volumes) {
    this.getAttribute(VOLUMES).setPlainValue(
      volumes
        .entrySet()
        .stream()
        .map(entry -> entry.getKey() + ":" + entry.getValue())
        .collect(Collectors.toList())
    );
    return this;
  }

  public DockerService setEnvs(Map<String, String> envs) {
    this.getAttribute(ENVIRONMENT).setPlainValue(envs);
    return this;
  }

  public DockerService setCommand(String... command) {
    this.getAttribute(COMMAND).setPlainValue(Casts.castToNewListSafe(command, String.class));
    return this;
  }

  public String getImage() {
    return this.getAttribute(IMAGE).getValueOrDefaultAsStringNotNull();
  }
}
