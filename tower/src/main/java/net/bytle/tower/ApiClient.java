package net.bytle.tower;

import net.bytle.tower.eraldy.model.openapi.App;

import java.net.URI;

public class ApiClient {
  @SuppressWarnings("unused")
  private String guid;
  private App app;
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private long localId;
  private URI uri;

  public String getGuid() {
    return this.guid;
  }


  public App getApp() {

    return this.app;

  }

  public void setApp(App app) {
    this.app = app;
  }

  public void setLocalId(long localId) {
    this.localId = localId;
  }

  public void addUri(URI uri) {
    this.uri = uri;
  }

  public URI getUri() {
    return this.uri;
  }
}
