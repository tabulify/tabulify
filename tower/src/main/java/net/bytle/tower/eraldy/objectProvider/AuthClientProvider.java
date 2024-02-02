package net.bytle.tower.eraldy.objectProvider;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.AuthClient;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithRealm;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.Realm;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.util.Guid;

import java.util.HashMap;
import java.util.Map;

public class AuthClientProvider {
  private static final String GUID_CLI_PREFIX = "cli";

  private final EraldyApiApp apiApp;
  private final Map<String, AuthClient> interactAuthClients = new HashMap<>();
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

  public void addEraldyClient(AuthClient authClient) {

    this.interactAuthClients.put(authClient.getGuid(), authClient);
    if (authClient.getLocalId() == 2L) {
      // The API key root client is a dummy client
      // for now a client of the interact app
      this.apiKeyRootClient = new AuthClient();
      this.apiKeyRootClient.setApp(authClient.getApp());
      this.apiKeyRootClient.setGuid("cli-root");
    }


  }

  /**
   * @param clientId - the client id
   * @return the auth client, null if not found (to not advertise a failure)
   */
  public Future<AuthClient> getClientFromClientId(String clientId) {
    AuthClient authClient = this.interactAuthClients.get(clientId);
    if (authClient != null) {
      return Future.succeededFuture(authClient);
    }
    return Future.succeededFuture();
  }

  public void updateGuid(AuthClient authClient) {
    Long localId = authClient.getApp().getRealm().getLocalId();
    Guid guid = this.apiApp.createGuidFromRealmAndObjectId(GUID_CLI_PREFIX, localId, authClient.getLocalId());
    authClient.setGuid(guid.toString());
  }

  public AuthClient getFromRoutingContextKeyStore(RoutingContext routingContext) {
    return this.apiApp.getAuthClientIdHandler().getApiClientStoredOnContext(routingContext);
  }

  public AuthClient getApiKeyRootClient() {
    return apiKeyRootClient;
  }

  public ObjectMapper getPublicJsonMapper() {
    return this.publicJsonMapper;
  }
}
