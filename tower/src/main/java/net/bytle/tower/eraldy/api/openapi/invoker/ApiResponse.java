package net.bytle.tower.eraldy.api.openapi.invoker;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ApiResponse<T> {
  private final T data;
  private final int statusCode;

  public ApiResponse() {
  this(200, null);
  }

  public ApiResponse(T data) {
  this(200, data);
  }

  public ApiResponse(int statusCode) {
  this(statusCode, null);
  }

  public ApiResponse(int statusCode, T data) {
  this.statusCode = statusCode;
  this.data = data;
  }

  public boolean hasData() {
  return data != null;
  }

  public T getData() {
  return data;
  }

  public int getStatusCode() {
  return statusCode;
  }

  private ObjectMapper jsonMapper;
  public ApiResponse<T> setMapper(ObjectMapper jsonMapper) {
    this.jsonMapper = jsonMapper;
    return this;
  }

  public ObjectMapper getJsonMapper() {
    return this.jsonMapper;
  }

}
