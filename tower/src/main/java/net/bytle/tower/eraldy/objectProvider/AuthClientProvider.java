package net.bytle.tower.eraldy.objectProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.exception.NotFoundException;
import net.bytle.tower.AuthClient;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithRealm;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.util.Guid;
import net.bytle.type.UriEnhanced;

public class AuthClientProvider {
  private static final String GUID_CLI_PREFIX = "cli";

  private final EraldyApiApp apiApp;
  private AuthClient interactAuthClient;
  /**
   * A special client that is the open api client
   * used with a api key
   */
  private AuthClient apiKeyRootClient;
  private final ObjectMapper publicJsonMapper;

  public AuthClientProvider(EraldyApiApp eraldyApiApp) {

    this.apiApp = eraldyApiApp;
    this.publicJsonMapper = eraldyApiApp.getApexDomain().getHttpServer().getServer().getJacksonMapperManager()
      .jsonMapperBuilder()
      .addMixIn(App.class, AppPublicMixinWithRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class)
      .build();
  }

  public AuthClient getClientFromRedirectUri(UriEnhanced redirectUriEnhanced) throws NotFoundException {

    UriEnhanced interactAppUri = UriEnhanced.createFromUri(this.interactAuthClient.getUri());

    if (redirectUriEnhanced.getHostWithPort().equals(interactAppUri.getHostWithPort())) {
      return this.interactAuthClient;
    }
    throw new NotFoundException("The client api could not found for the redirect URI (" + redirectUriEnhanced + ")");

  }

  public void setInteractAppClient(AuthClient authClient) {

    this.interactAuthClient = authClient;

    // The API key root client is a dummy client
    // for now a client of the interact app
    this.apiKeyRootClient = new AuthClient();
    this.apiKeyRootClient.setApp(this.interactAuthClient.getApp());
    this.apiKeyRootClient.setGuid("cli-root");

  }

  public Future<AuthClient> getClientFromClientId(String clientId) {
    if (this.interactAuthClient.getGuid().equals(clientId)) {
      return Future.succeededFuture(this.interactAuthClient);
    }
    return Future.failedFuture(new NotFoundException("The client (" + clientId + ") is unknown"));
  }

  public void updateGuid(AuthClient authClient) {
    Long localId = authClient.getApp().getRealm().getLocalId();
    Guid guid = this.apiApp.createGuidFromRealmAndObjectId(GUID_CLI_PREFIX, localId, authClient.getLocalId());
    authClient.setGuid(guid.toString());
  }

  public AuthClient getFromRoutingContextKeyStore(RoutingContext routingContext) {
    return this.apiApp.getAuthClientHandler().getApiClientStoredOnContext(routingContext);
  }

  public AuthClient getApiKeyRootClient() {
    return apiKeyRootClient;
  }

  public ObjectMapper getPublicJsonMapper() {
    return this.publicJsonMapper;
  }
}
