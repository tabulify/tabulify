package net.bytle.vertx.flow;

import java.net.URI;

/**
 * A pojo that defines the SMTP sender
 */
public class SmtpSender {
  private String name;
  private String email;
  private String fullName;
  private URI avatar;
  private String title;

  public String getEmail() {
    return this.email;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public String getFullname() {
    return this.fullName;
  }

  public URI getAvatar() {
    return this.avatar;
  }

  public String getTitle() {
    return this.title;
  }

  public void setFullName(String fullname) {
    this.fullName = fullname;
  }

  public void setAvatar(URI avatar) {
    this.avatar = avatar;
  }

  public void setTitle(String title) {
    this.title = title;
  }

}
