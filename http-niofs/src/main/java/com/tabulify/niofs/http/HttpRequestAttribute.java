package com.tabulify.niofs.http;


public enum HttpRequestAttribute {

  USER("Basic authentication user"),
  PASSWORD("Basic authentication password");


  private final String description;

  HttpRequestAttribute(String description) {

    this.description = description;
  }


  public String getDescription() {
    return this.description;
  }

  public Class<?> getValueClazz() {
    return String.class;
  }

  public Object getDefaultValue() {
    return null;
  }
}
