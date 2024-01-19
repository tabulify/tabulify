package net.bytle.vertx.auth;

import io.vertx.core.Future;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.OAuth2Options;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.codec.BodyCodec;
import net.bytle.exception.InternalException;
import net.bytle.java.JavaEnvs;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

/**
 * Oauth GitHub app settings can be seen at <a href="https://github.com/settings/developers">Settings developers</a>
 * as told here: <a href="https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/creating-an-oauth-app">Creating an OAuth App</a>
 */
public class OAuthExternalGithub extends OAuthExternalProviderAbs {


  /**
   * For GitHub, we’re requesting `user:email` scope
   * for reading private email addresses as with UserInfo (ie user endpoint),
   * you may not have any email
   * <a href="https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/scopes-for-oauth-apps">...</a>
   * If the user login is known, we can add the `login` parameter
   */
  public static final String USER_EMAIL_SCOPE = "user:email";
  public static final String GITHUB_TENANT = "github";

  public OAuthExternalGithub(OAuthExternal oAuthExternal, String clientId, String clientSecret) {

    super(
      oAuthExternal,
      /**
       * same as GithubAuth.create(verticle.getVertx(), this.clientId, this.clientSecret);
       */
      OAuth2Auth.create(oAuthExternal.getFlow().getApp().getApexDomain().getHttpServer().getServer().getVertx(), new OAuth2Options()
        .setHttpClientOptions(new HttpClientOptions())
        .setFlow(OAuth2FlowType.AUTH_CODE)
        .setClientId(clientId)
        .setClientSecret(clientSecret)
        .setSite("https://github.com/login")
        .setTokenPath("/oauth/access_token")
        .setTenant("github")
        .setAuthorizationPath("/oauth/authorize")
        .setUserInfoPath("https://api.github.com/user")
        .setHeaders(new JsonObject().put("User-Agent", "eraldy-oauth")))
    );

  }

  @Override
  public List<String> getRequestedScopes() {
    return Collections.singletonList(OAuthExternalGithub.USER_EMAIL_SCOPE);
  }

  @Override
  public Future<AuthUser> getEnrichedUser(RoutingContext ctx, JsonObject userInfo, String accessToken) {

    String type = userInfo.getString("type");
    if (!type.equals("User")) {
      return Future.failedFuture("GitHub says that you are not a user but a " + type + ", authenticate via another method");
    }

    String githubUserAvatarUrl = userInfo.getString("avatar_url");
    URI githubUserAvatarUri = null;
    try {
      githubUserAvatarUri = new URI(githubUserAvatarUrl);
    } catch (URISyntaxException e) {
      if (JavaEnvs.IS_DEV) {
        throw new InternalException("GitHub Avatar URL (" + githubUserAvatarUrl + ") is not valid", e);
      }
      String githubUserGravatarId = userInfo.getString("gravatar_id");
      if (githubUserGravatarId != null && !githubUserGravatarId.equals("")) {
        String gravatarUrl = "https://www.gravatar.com/avatar/" + githubUserGravatarId;
        try {
          githubUserAvatarUri = new URI(gravatarUrl);
        } catch (URISyntaxException ex) {
          // ok, we are not in dev here, we don't throw
        }
      }
    }

    // Option ? Sync user profiles and attributes on sign in.
    String githubUserName = userInfo.getString("name");
    String githubBio = userInfo.getString("bio");
    URI githubUserWebsite = null;
    String githubBlogAsString = userInfo.getString("blog");
    if (githubBlogAsString != null) {
      try {
        githubUserWebsite = new URI(githubBlogAsString);
      } catch (URISyntaxException e) {
        if (JavaEnvs.IS_DEV) {
          throw new InternalException("GitHub Blog URL (" + githubBlogAsString + ") is not valid", e);
        }
      }
    }
    /**
     * Twitter is used as website if the blog is unknown
     */
    String githubUserTwitterUsername = userInfo.getString("twitter_username");
    if (githubBlogAsString == null && githubUserTwitterUsername != null) {
      String twitterUrl = "https://twitter.com/" + githubUserTwitterUsername;
      try {
        githubUserWebsite = new URI(twitterUrl);
      } catch (URISyntaxException e) {
        if (JavaEnvs.IS_DEV) {
          throw new InternalException("GitHub Twitter URL (" + twitterUrl + ") is not valid", e);
        }
      }
    }
    String githubUserLocation = userInfo.getString("location");

    /**
     * Fetch the user information (emails) from the GitHub API
     * https://docs.github.com/en/rest/users/emails?apiVersion=2022-11-28#list-email-addresses-for-the-authenticated-user
     */
    URI finalGithubUserAvatarUri = githubUserAvatarUri;
    URI finalGithubUserBlog = githubUserWebsite;
    return WebClient.create(ctx.vertx())
      .getAbs("https://api.github.com/user/emails")
      .putHeader("X-GitHub-Api-Version", "2022-11-28")
      .putHeader("Accept", "application/vnd.github+json")
      .authentication(new TokenCredentials(accessToken))
      .as(BodyCodec.jsonArray())
      .send()
      .onFailure(err -> {
        ctx.session().destroy();
        ctx.fail(err);
      })
      .compose(res2 -> {

        JsonArray githubResponse = res2.body();
        String email = null;
        Boolean verified = false;
        for (int i = 0; i < githubResponse.size(); i++) {
          JsonObject privateEmail = githubResponse.getJsonObject(i);
          email = privateEmail.getString("email");
          Boolean primary = privateEmail.getBoolean("primary");
          verified = privateEmail.getBoolean("verified");
          if (primary) {
            break;
          }
        }
        if (email == null) {
          return Future.failedFuture("A primary email was not found on GitHub, authenticate via another method");
        }
        if (verified == null || !verified) {
          return Future.failedFuture("Your primary email is not verified on GitHub, authenticate via another method or verify the primary email (" + email + ")");
        }

        AuthUser user = new AuthUser();
        user.setSubjectEmail(email);
        user.setSubjectGivenName(githubUserName);
        user.setSubjectBio(githubBio);
        user.setSubjectBlog(finalGithubUserBlog);
        user.setSubjectLocation(githubUserLocation);
        user.setSubjectAvatar(finalGithubUserAvatarUri);
        return Future.succeededFuture(user);

      });
  }

  @Override
  public String getName() {
    return GITHUB_TENANT;
  }

}
