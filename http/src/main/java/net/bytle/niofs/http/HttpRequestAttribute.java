package net.bytle.niofs.http;

import net.bytle.type.Attribute;

public enum HttpRequestAttribute implements Attribute {

  USER( "Basic authentication user"),
  PASSWORD( "Basic authentication password")

  ;


  private final String description;

  HttpRequestAttribute(String description) {

    this.description = description;
  }



  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return String.class;
  }

  @Override
  public Object getDefaultValue() {
    return null;
  }
}
