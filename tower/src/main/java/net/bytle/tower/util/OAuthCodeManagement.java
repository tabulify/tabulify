package net.bytle.tower.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.vertx.ext.auth.VertxContextPRNG;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.eraldy.model.openapi.User;

import java.util.concurrent.TimeUnit;

/**
 * The class that handles the code authentication
 */
public class OAuthCodeManagement {


  private static OAuthCodeManagement OAuthCodeManagement;
  private final VertxContextPRNG prng;
  private final Cache<String, OAuthAuthorization> cache;


  public OAuthCodeManagement() {
    this.prng = VertxContextPRNG.current();
    TimeUnit timeUnit;
    if (!Env.IS_DEV) {
      timeUnit = TimeUnit.MINUTES;
    } else {
      timeUnit = TimeUnit.HOURS;
    }
    cache = Caffeine.newBuilder()
      .expireAfterWrite(1, timeUnit)
      .maximumSize(1000)
      .build();
  }

  public static OAuthCodeManagement createOrGet() {
    if (OAuthCodeManagement == null) {
      OAuthCodeManagement = new OAuthCodeManagement();
    }
    return OAuthCodeManagement;
  }

  public String createAuthorizationAndGetCode(String redirectUri, User contextUser) {
    String authCode = prng.nextString(10);
    OAuthAuthorization OAuthAuthorization = new OAuthAuthorization();
    OAuthAuthorization.setRedirectUri(redirectUri);
    OAuthAuthorization.setUser(contextUser);
    cache.put(authCode, OAuthAuthorization);
    return authCode;
  }

  public OAuthAuthorization getAuthorization(String code) throws NotFoundException {
    OAuthAuthorization authorization = cache.getIfPresent(code);
    if (authorization == null) {
      throw new NotFoundException("The code (" + code + ") was not found");
    }
    return authorization;
  }

}
