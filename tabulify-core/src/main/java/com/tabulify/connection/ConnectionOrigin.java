package com.tabulify.connection;

import com.tabulify.conf.AttributeValue;

public enum ConnectionOrigin implements AttributeValue {

  BUILT_IN("The built-in connections"),
  CONF("The conf file connections"),
  // Runtime data store created on the fly. For instance, with the URL of a {@link Path}
  // or connection added later such as resources for test
  RUNTIME("Connection created at runtime");


  private final String description;

  ConnectionOrigin(String description) {
    this.description = description;
  }

  @Override
  public String getDescription() {
    return this.description;
  }


}
