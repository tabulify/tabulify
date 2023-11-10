package net.bytle.vertx.auth;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.providers.GoogleAuth;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.InternalException;
import net.bytle.java.JavaEnvs;
import net.bytle.vertx.TowerApp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

/**
 * The Google Oauth
 * <a href="https://developers.google.com/identity/gsi/web/guides/get-google-api-clientid">Setup</a>
 * <p>
 * Oauth credentials can be managed here: <a href="https://console.cloud.google.com/apis/credentials/oauthclient/">...</a>
 * <p>
 * Playground
 * <a href="https://developers.google.com/oauthplayground/">Playground</a>
 */
public class OAuthExternalGoogle extends OAuthExternalProviderAbs {


  public OAuthExternalGoogle(TowerApp towerApp, String clientId, String clientSecret) {

    super(GoogleAuth.create(towerApp.getApexDomain().getHttpServer().getServer().getVertx(), clientId, clientSecret, new HttpClientOptions()));

  }


  @Override
  public List<String> getRequestedScopes() {
    // Note that when asking the `profile` scope from https://developers.google.com/identity/protocols/oauth2/scopes#google-sign-in,
    // we get `https://www.googleapis.com/auth/userinfo.profile` from https://developers.google.com/identity/protocols/oauth2/scopes#oauth2
    return Arrays.asList(
      "https://www.googleapis.com/auth/userinfo.email", // see the email
      "https://www.googleapis.com/auth/userinfo.profile", // see profile info
      "openid" // Just FYI, this scope is added when the first two (userinfo.email, userinfo.profile) are requested
      // date of birth triggers a choice for the user (to grant or not the birthday), we don't want that
      // "https://www.googleapis.com/auth/user.birthday.read" // to get the birthday
    );
  }


  @Override
  public Future<AuthUser> getEnrichedUser(RoutingContext ctx, JsonObject userInfo, String accessToken) {

    /**
     *
     * <p>
     * We don't need an extra call as GitHub to get the email and its validation.
     * <p>
     * More data are available in the <a href="https://developers.google.com/people/api/rest/v1/people/get">People Api</a>,
     * but we need to enable it in the Google dashboard.
     * <p>
     * We got it via the `https://developers.google.com/oauthplayground/`
     */

    /**
     * userInfo
     * The documentation is: https://developers.google.com/identity/openid-connect/openid-connect
     * The userinfo endpoint is: "https://www.googleapis.com/oauth2/v1/userinfo"
     *
     */
    // String id = userInfo.getString("id");
    // Name: The user's full name, in a displayable form (might be null)
    String name = userInfo.getString("name"); // FOO bar
    // GivenName: The user's given name(s) or first name(s). Might be provided when a name claim is present.
    String givenName = userInfo.getString("given_name"); // FOO
    // FamilyName: The user's surname(s) or last name(s). Might be provided when a name claim is present.
    String familyName = userInfo.getString("family_name"); // bar
    String pictureUrl = userInfo.getString("picture"); // https://lh3.googleusercontent.com/a/AAcHTtfH4w1QKaMRObFDWA83mUDYqgkSJ1IKr1aQET8n3Bz5CP7J=s96-c
    String email = userInfo.getString("email");
    // VerifiedEmail: True if the user's e-mail address has been verified; otherwise false.
    Boolean verifiedEmail = userInfo.getBoolean("verified_email");
    if (!verifiedEmail) {
      return Future.failedFuture("The email is not verified.");
    }

    URI googleUserAvatarUri = null;
    try {
      googleUserAvatarUri = new URI(pictureUrl);
    } catch (URISyntaxException e) {
      if (JavaEnvs.IS_DEV) {
        throw new InternalException("Google Picture URL (" + pictureUrl + ") is not valid", e);
      }
    }
    AuthUser user = new AuthUser();
    user.setSubjectEmail(email);
    user.setSubjectAvatar(googleUserAvatarUri);
    user.setSubjectGivenName(givenName);
    user.setSubjectFamilyName(givenName);
    return Future.succeededFuture(user);

  }

}
