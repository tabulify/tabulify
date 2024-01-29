package net.bytle.vertx.analytics.event;

public enum AnalyticsEventType {


  SIGN_IN(0,"Sign In", true),
  SIGN_UP(1,"Sign Up", true),
  USER_UPDATE(2,"User Update", true);

  private final int guid;
  private final String name;

  /**
   * Generally, you send the user info (ie make an Identify call)
   * * After a user first registers {@link SignUpEvent}
   * * After a user logs in {@link SignInEvent}
   * * When a user updates their info (for example, they change or add a new address) {@link UserProfileUpdateEvent}
   */
  private final boolean sendUserProfile;

  /**
   *
   * @param guid - the guid
   * @param name - the name of the type
   * @param isUserProfileUpdate - do we need to update the user profile
   */

  AnalyticsEventType(int guid, String name, boolean isUserProfileUpdate) {
    this.guid = guid;
    this.name = name;
    this.sendUserProfile = isUserProfileUpdate;
  }

  public String getName() {
    return this.name;
  }


  public Integer getGuid() {
    return this.guid;
  }

  public boolean isSendUserProfile() {
    return sendUserProfile;
  }
}
