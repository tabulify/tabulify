package net.bytle.vertx.auth;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authorization.Authorization;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;
import net.bytle.type.Casts;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A wrapper around a {@link User}
 * It implements our own claims that identifies a user
 * <p>
 * A AuthUser Claims may be created for user registration, meaning that the user
 * does not exist in the database yet and has therefore no id
 */
public class AuthUser {


  private final JsonObject claims;
  private final User user;


  private AuthUser(User user) {
    this.claims = user.principal().mergeIn(user.attributes());
    this.user = user;
  }

  public static AuthUser createUserFromJsonClaims(JsonObject jsonObject) {
    return new Builder(jsonObject)
      .build();
  }

  public static Builder builder() {
    return new AuthUser.Builder(null);
  }

  public static AuthUser createFromUser(User user) {
    return new AuthUser(user);
  }


  public String getAudience() {
    return claims.getString(AuthUserJwtClaims.AUDIENCE.toString());
  }

  public String getRealmGuid() {
    return getAudience();
  }


  public String getSubject() {
    return claims.getString(AuthUserJwtClaims.SUBJECT.toString());
  }

  @SuppressWarnings("unused")
  public String getSubjectHandle() throws NullValueException {
    String userHandle = claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_HANDLE.toString());
    if (userHandle == null) {
      throw new NullValueException("No subject handle");
    }
    return userHandle;
  }

  public String getSubjectEmail() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_EMAIL.getJwtKey());
  }



  public User getVertxUser() {


    return this.user;


  }


  public String getSubjectGivenName() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_GIVEN_NAME.toString());
  }

  public String getSubjectBio() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_BIO.toString());
  }

  public String getSubjectFamilyName() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_FAMILY_NAME.toString());
  }

  public String getSubjectLocation() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_LOCATION.toString());
  }

  public URI getSubjectBlog() {
    try {
      String blogUrl = claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_BLOG.toString());
      if (blogUrl == null) {
        return null;
      }
      return new URI(blogUrl);
    } catch (URISyntaxException e) {
      throw new InternalException("Should not happen because the setter is an URI", e);
    }
  }

  public URI getSubjectAvatar() {
    try {
      String avatarUrl = claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_AVATAR.toString());
      if (avatarUrl == null) {
        return null;
      }
      return new URI(avatarUrl);
    } catch (URISyntaxException e) {
      throw new InternalException("Avatar should be an URI because the setter is an URI", e);
    }
  }

  public String getAudienceHandle() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_AUDIENCE_HANDLE.toString());
  }

  public String getOrganizationHandle() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_ORG_HANDLE.toString());

  }

  public <T> Set<T> getSet(String key, Class<T> clazz) {
    JsonArray jsonArray = claims.getJsonArray(key);
    if (jsonArray == null) {
      return new HashSet<>();
    }
    return jsonArray.stream()
      .map(e -> {
        try {
          return Casts.cast(e, clazz);
        } catch (CastException ex) {
          throw new InternalException("The value (" + e + ") of the claims key (" + key + ") is not a " + clazz, ex);
        }
      })
      .collect(Collectors.toSet());
  }

  @Override
  public String toString() {

    String toString = "";
    String subjectEmail = this.getSubjectEmail();
    if (subjectEmail != null) {
      toString = subjectEmail;
    }
    String subject = this.getSubject();
    if (subject != null) {
      toString = ", " + subject;
    }
    return toString;

  }


  public String getOrganizationGuid() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_ORG_GUID.toString());
  }

  public String getRealmHandle() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_AUDIENCE_HANDLE.toString());
  }

  public String getSubjectName() {
    return claims.getString(AuthUserJwtClaims.CUSTOM_SUBJECT_NAME.toString());
  }

  public JsonObject getClaims() {
    return this.claims;
  }

  public AuthJwtClaims toJwtClaims() {
    return AuthJwtClaims.createFromAuthUser(this);
  }


  public static class Builder {

    protected final JsonObject claims;
    private final Map<String, Set<Authorization>> authorizations = new HashMap<>();

    public Builder(JsonObject claims) {
        this.claims = Objects.requireNonNullElseGet(claims, JsonObject::new);
    }

    public Builder setOrganizationGuid(String orgGuid) {
      claims.put(AuthUserJwtClaims.CUSTOM_ORG_GUID.toString(), orgGuid);
      return this;
    }

    public Builder setOrganizationHandle(String orgHandle) {
      claims.put(AuthUserJwtClaims.CUSTOM_ORG_HANDLE.toString(), orgHandle);
      return this;
    }

    /**
     * @param audience - a namespace where the subject is unique (an application, a realm, ...)
     */
    public Builder setRealmGuid(String audience) {
      claims.put(AuthUserJwtClaims.AUDIENCE.toString(), audience);
      return this;
    }

    public Builder setRealmHandle(String audienceHandle) {
      claims.put(AuthUserJwtClaims.CUSTOM_AUDIENCE_HANDLE.toString(), audienceHandle);
      return this;
    }

    /**
     * @param subject - a subject/user identifier for the audience
     */
    public Builder setSubject(String subject) {
      claims.put(AuthUserJwtClaims.SUBJECT.toString(), subject);
      return this;
    }

    /**
     * @param subjectHandle - a handle is a unique descriptif name for the subject
     */
    public Builder setSubjectHandle(String subjectHandle) {
      claims.put(AuthUserJwtClaims.CUSTOM_SUBJECT_HANDLE.toString(), subjectHandle);
      return this;
    }

    public Builder setSubjectGivenName(String subjectGivenName) {
      claims.put(AuthUserJwtClaims.CUSTOM_SUBJECT_GIVEN_NAME.toString(), subjectGivenName);
      return this;
    }

    public Builder setSubjectBio(String bio) {

      claims.put(AuthUserJwtClaims.CUSTOM_SUBJECT_BIO.toString(), bio);
      return this;

    }

    public Builder setSubjectBlog(URI blogUri) {

      claims.put(AuthUserJwtClaims.CUSTOM_SUBJECT_BLOG.toString(), blogUri);
      return this;

    }

    public Builder setSubjectLocation(String location) {
      claims.put(AuthUserJwtClaims.CUSTOM_SUBJECT_LOCATION.toString(), location);
      return this;
    }

    public Builder setSubjectAvatar(URI avatarUri) {
      claims.put(AuthUserJwtClaims.CUSTOM_SUBJECT_AVATAR.toString(), avatarUri);
      return this;
    }

    public Builder setSubjectFamilyName(String familyName) {
      claims.put(AuthUserJwtClaims.CUSTOM_SUBJECT_FAMILY_NAME.toString(), familyName);
      return this;
    }

    /**
     * @param email - the email of the subject
     */
    public Builder setSubjectEmail(String email) {

      claims.put(AuthUserJwtClaims.CUSTOM_SUBJECT_EMAIL.toString(), email);
      return this;

    }


    /**
     * Put free-form applicative claims
     * @param key - a free-form claims key
     * @param obj - a free-form claims value
     */
    public void put(String key, Object obj) {
      claims.put(key, obj);
    }

    public AuthUser build() {
      /**
       * Not completely finish
       * A second argument attributes can be provided to provide extra metadata for later usage.
       * One example are the following attributes:
       * * exp - Expires at in seconds.
       * * iat - Issued at in seconds.
       * * nbf - Not before in seconds.
       * * leeway - clock drift leeway in seconds.
       * <p>
       * The first 3 control how the expired method will compute the expiration of the user,
       * the last can be used to allow clock drifting compensation while computing the expiration time.
       */
      User user = User.create(claims);
      for(Map.Entry<String,Set<Authorization>> entry: authorizations.entrySet()){
        user.authorizations().add(entry.getKey(), entry.getValue());
      }
      return new AuthUser(user);
    }

    public Builder addAuthorization(String providerId, Authorization authorization) {
      Set<Authorization> authorizationSet = authorizations.computeIfAbsent(providerId, k -> ConcurrentHashMap.newKeySet());
      authorizationSet.add(authorization);
      return this;
    }

  }

}
