package com.tabulify.docker;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.service.Service;
import com.tabulify.service.ServiceProvider;

public class DockerProvider extends ServiceProvider {

  public static final String DOCKER_TYPE = "docker";

  @Override
  public Service createService(Tabular tabular, Attribute name) {
    return new DockerService(tabular, name);
  }

  @Override
  public boolean accept(String type) {
    return type.equalsIgnoreCase(DOCKER_TYPE);
  }
}
