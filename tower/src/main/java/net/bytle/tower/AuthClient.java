package net.bytle.tower;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.bytle.tower.eraldy.module.app.model.App;
import net.bytle.tower.eraldy.module.auth.model.CliGuid;

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

  private CliGuid guid;
  private App app;
  private URI uri;
  private String name;

  public AuthClient() {
  }

  @JsonProperty("guid")
  public CliGuid getGuid() {
    return this.guid;
  }

  @JsonProperty("app")
  public App getApp() {
    return this.app;
  }

  public void setApp(App app) {
    this.app = app;
  }

  public void addUri(URI uri) {
    this.uri = uri;
  }

  public URI getUri() {
    return this.uri;
  }

  public void setGuid(CliGuid guid) {
    this.guid = guid;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
