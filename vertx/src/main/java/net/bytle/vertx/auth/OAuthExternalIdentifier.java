package net.bytle.vertx.auth;

public enum OAuthExternalIdentifier {

  GITHUB(0),
  GOOGLE(1);

  private final int id;

  OAuthExternalIdentifier(int id) {
    this.id = id;
  }

  public String getHandle() {
    return this.name().toLowerCase();
  }

  public Integer getGuid() {
    return id;
  }

  @Override
  public String toString() {
    return this.name().toLowerCase();
  }

}
