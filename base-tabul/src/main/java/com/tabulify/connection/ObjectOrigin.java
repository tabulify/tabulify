package com.tabulify.connection;

import com.tabulify.conf.AttributeValue;

/**
 * The origin of the object
 * (Connection or system)
 * Old, now that we can create them in the conf file
 * We should delete this class in order to use {@link com.tabulify.conf.Origin}
 */
public enum ObjectOrigin implements AttributeValue {

  BUILT_IN("Built-in"),
  CONF("Configuration vault"),
  // Runtime data created on the fly. For instance, with the URL of a {@link Path}
  // or connection added later such as resources for test
  RUNTIME("Runtime Creation");


  private final String description;

  ObjectOrigin(String description) {
    this.description = description;
  }

  @Override
  public String getDescription() {
    return this.description;
  }


}
