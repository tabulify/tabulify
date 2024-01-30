package net.bytle.tower;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.model.openapi.App;

import java.net.URI;

/**
 * A client that represents an app
 * that makes calls to the api.
 * (Web Client, Mobile, ...)
 * This is the same concept as
 * the <a href="https://datacadamia.com/iam/oauth/client">OAuth Client</a>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthClient {

  private String guid;
  private App app;

  private long localId;
  private URI uri;

  public AuthClient() {
  }

  @JsonProperty("guid")
  public String getGuid() {
    return this.guid;
  }

  @JsonProperty("app")
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

  public Long getLocalId() {
    return this.localId;
  }
  public void setGuid(String guid) {
    this.guid = guid;
  }

}
