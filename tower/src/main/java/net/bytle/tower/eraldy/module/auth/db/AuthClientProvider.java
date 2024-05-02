package net.bytle.tower.eraldy.module.auth.db;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import net.bytle.tower.AuthClient;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithRealm;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.App;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.module.auth.jackson.JacksonCliGuidDeserializer;
import net.bytle.tower.eraldy.module.auth.jackson.JacksonCliGuidSerializer;
import net.bytle.tower.eraldy.module.auth.model.CliGuid;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.vertx.guid.GuidDeSer;

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
    this.publicJsonMapper = eraldyApiApp.getHttpServer().getServer().getJacksonMapperManager()
      .jsonMapperBuilder()
      .addMixIn(App.class, AppPublicMixinWithRealm.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class)
      .build();

    GuidDeSer cliDeSer = this.apiApp.getHttpServer().getServer().getHashId().getGuidDeSer(GUID_CLI_PREFIX, 2);
    this.apiApp.getJackson()
      .addSerializer(CliGuid.class,new JacksonCliGuidSerializer(cliDeSer))
      .addDeserializer(CliGuid.class,new JacksonCliGuidDeserializer(cliDeSer));

  }

  public void addEraldyClient(AuthClient authClient) {

    String cliGuidHash = this.apiApp.getJackson().getSerializer(CliGuid.class).serialize(authClient.getGuid());
    this.interactAuthClients.put(cliGuidHash, authClient);
    if (authClient.getGuid().getLocalId() == 2L) {
      // The API key root client is a dummy client
      // for now a client of the interact app
      this.apiKeyRootClient = new AuthClient();
      this.apiKeyRootClient.setApp(authClient.getApp());
      CliGuid cliGuid = new CliGuid();
      cliGuid.setLocalId(0);
      cliGuid.setRealmId(this.apiKeyRootClient.getApp().getRealm().getGuid().getLocalId());
      this.apiKeyRootClient.setGuid(cliGuid);
      this.apiKeyRootClient.setName("cli-root");
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

  public void updateGuid(AuthClient authClient, long localId) {
    long realmGuid = authClient.getApp().getRealm().getGuid().getLocalId();
    CliGuid cliGuid = new CliGuid();
    cliGuid.setRealmId(realmGuid);
    cliGuid.setLocalId(localId);
    authClient.setGuid(cliGuid);
  }

  public AuthClient getRequestingClient(RoutingContext routingContext) {
    return this.apiApp.getAuthClientIdHandler().getRequestingClient(routingContext);
  }

  public AuthClient getApiKeyRootClient() {
    return apiKeyRootClient;
  }

  public ObjectMapper getPublicJsonMapper() {
    return this.publicJsonMapper;
  }
}
