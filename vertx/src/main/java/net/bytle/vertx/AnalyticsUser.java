package net.bytle.vertx;

import jakarta.mail.internet.AddressException;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.NotFoundException;
import net.bytle.type.time.Timestamp;

import java.net.URI;

public class AnalyticsUser {
  private String id;
  private String email;
  private String name;
  private Timestamp creationTime;
  private URI avatar;

  public String getId() {
    return this.id;
  }

  public String getEmail() {
    return this.email;
  }

  public String getName() {
    return this.name;
  }

  public Timestamp getCreationTime() {
    return this.creationTime;
  }

  public URI getAvatar() {
    return this.avatar;
  }

  public String getNameOrEmail() throws NotFoundException, AddressException {
    if (name != null) {
      return name;
    }
    if (this.email == null) {
      throw new NotFoundException("No name could be found for this user");
    }
    return BMailInternetAddress.of(email)
      .getLocalPart();
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setGivenName(String name) {
    this.name = name;
  }

  public void setAvatar(URI avatar) {
    this.avatar = avatar;
  }

}
