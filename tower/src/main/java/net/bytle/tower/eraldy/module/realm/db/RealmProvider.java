package net.bytle.tower.eraldy.module.realm.db;


import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.SqlConnection;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.java.JavaEnvs;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.module.organization.db.OrganizationCols;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.eraldy.module.realm.inputs.RealmInputProps;
import net.bytle.tower.eraldy.module.realm.jackson.JacksonRealmGuidDeserializer;
import net.bytle.tower.eraldy.module.realm.jackson.JacksonRealmGuidSerializer;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.tower.eraldy.module.realm.model.RealmGuid;
import net.bytle.type.Handle;
import net.bytle.type.HandleCastException;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.db.*;
import net.bytle.vertx.guid.GuidDeSer;
import net.bytle.vertx.jackson.JacksonMapperManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manage the get/upsert of a {@link Realm} object asynchronously
 * in the database
 */
public class RealmProvider {

  public static final String TABLE_PREFIX = "realm";

  public static final String REALM_GUID_PREFIX = "rea";

  protected static final Logger LOGGER = LoggerFactory.getLogger(RealmProvider.class);


  private final EraldyApiApp apiApp;


  private final JdbcTable realmTable;


  public RealmProvider(EraldyApiApp apiApp, JdbcSchema schema) {

    this.apiApp = apiApp;
    JacksonMapperManager jacksonMapperManager = this.apiApp.getHttpServer().getServer().getJacksonMapperManager();

    this.realmTable = JdbcTable.build(schema, "realm", RealmCols.values())
      .addPrimaryKeyColumn(RealmCols.ID)
      .addUniqueKeyColumn(RealmCols.HANDLE)
      .addForeignKeyColumn(RealmCols.OWNER_ORGA_ID, OrganizationCols.ID)
      .build();

    /**
     * Realm Guid
     */
    GuidDeSer realmGuidDeser = this.apiApp.getHttpServer().getServer().getHashId().getGuidDeSer(REALM_GUID_PREFIX, 1);
    jacksonMapperManager
      .addDeserializer(RealmGuid.class, new JacksonRealmGuidDeserializer(realmGuidDeser))
      .addSerializer(RealmGuid.class, new JacksonRealmGuidSerializer(realmGuidDeser));

  }


  /**
   * @param realm - the realm
   * @return the name or the handle of null
   */
  public static String getNameOrHandle(Realm realm) {
    if (realm.getName() != null) {
      return realm.getName();
    }
    return realm.getHandle().getValue();
  }


  /**
   *
   * @deprecated should use/implement a map cache instead
   */
  @Deprecated
  public Future<Realm> getRealmFromLocalIdOrAutCli(long realmId, RoutingContext routingContext) {
    Realm authRealmClient = this.apiApp.getAuthClientProvider().getRequestingClient(routingContext).getApp().getRealm();
    if (authRealmClient.getGuid().getLocalId() == realmId) {
      return Future.succeededFuture(authRealmClient);
    }
    return this.getRealmFromLocalId(realmId);
  }


  public Future<Realm> getRealmByLocalIdWithAuthorizationCheck(long realmId, AuthUserScope scope, RoutingContext routingContext) {
    return this.apiApp.getAuthProvider().getRealmByLocalIdWithAuthorizationCheck(realmId, scope, routingContext);
  }

  /**
   * @param routingContext - the http routing context
   * @return the realm of the request (ie of the client)
   */
  public Realm getRequestingRealm(RoutingContext routingContext) {
    return this.apiApp.getAuthClientIdHandler().getRequestingApp(routingContext).getRealm();
  }


  public JdbcTable getTable() {
    return this.realmTable;
  }

  public EraldyApiApp getApp() {
    return this.apiApp;
  }


  /**
   *
   * @param realmInputProps - the input props
   * @param sqlConnection - the connection
   * @param askedRealmId - use for Eraldy Realm only - the asked realm id (ie 1)
   * @return the inserted realm
   */
  private Future<Realm> insertRealm(RealmInputProps realmInputProps, SqlConnection sqlConnection, Long askedRealmId) {


    JdbcInsert jdbcInsert = JdbcInsert.into(this.realmTable)
      .addReturningColumn(RealmCols.ID);

    Realm realm;
    /**
     * the id may be known as it's the case
     * when inserting the fist Eraldy realm with the id 1
     */
    if (askedRealmId != null) {
      realm = Realm.createFromAnyId(askedRealmId);
      jdbcInsert.addColumn(RealmCols.ID, realm.getGuid().getLocalId());
    } else {
      realm = new Realm();
    }
    Handle handle = realmInputProps.getHandle();

    if (handle != null) {
      realm.setHandle(handle);
      jdbcInsert.addColumn(RealmCols.HANDLE, realm.getHandle().getValue());
    }

    LocalDateTime nowInUtc = DateTimeService.getNowInUtc();
    realm.setCreationTime(nowInUtc);
    jdbcInsert.addColumn(RealmCols.CREATION_TIME, realm.getCreationTime());
    realm.setModificationTime(nowInUtc);
    jdbcInsert.addColumn(RealmCols.MODIFICATION_TIME, realm.getModificationTime());

    realm.setName(realmInputProps.getName());
    jdbcInsert.addColumn(RealmCols.NAME, realm.getName());

    /**
     * Ownership
     */
    OrgaUser orgaUser = this.apiApp.getOrganizationUserProvider().toNewOwnerFromGuid(realmInputProps.getOwnerUserGuid());
    realm.setOwnerUser(orgaUser);
    jdbcInsert.addColumn(RealmCols.OWNER_USER_ID, realm.getOwnerUser().getGuid().getUserId());
    jdbcInsert.addColumn(RealmCols.OWNER_ORGA_ID, realm.getOwnerUser().getGuid().getOrganizationId());
    jdbcInsert.addColumn(RealmCols.OWNER_REALM_ID, realm.getOwnerUser().getGuid().getRealmId());

    return jdbcInsert
      .execute(sqlConnection)
      .compose(rows -> {
        Long realmIdAfterInsertion = rows.iterator().next().getLong(RealmCols.ID);
        if (askedRealmId != null && !askedRealmId.equals(realmIdAfterInsertion)) {
          /**
           * Case when we insert a realm when we want the id
           * The Eraldy realm should be 1 is the main case
           */
          String error = "The asked realm id (" + askedRealmId + ") did not get the same id but the id (" + askedRealmId + ")";
          if (JavaEnvs.IS_DEV) {
            error += "In Dev, delete the SQL schema and restart. An error like that is due to an error on start between the 2 inserts";
          }
          return Future.failedFuture(new InternalException(error));
        }
        realm.setGuid(new RealmGuid(realmIdAfterInsertion));
        return Future.succeededFuture(realm);
      });
  }


  @SuppressWarnings("unused")
  private Future<Realm> updateRealm(Realm realm, RealmInputProps realmInputProps) {

    JdbcUpdate jdbcUpdate = JdbcUpdate.into(this.realmTable)
      .addPredicateColumn(RealmCols.ID, realm.getGuid().getLocalId())
      .addReturningColumn(RealmCols.ID)
      .setUpdatedColumnWithValue(RealmCols.MODIFICATION_TIME, DateTimeService.getNowInUtc());

    Handle newHandle = realmInputProps.getHandle();
    if (newHandle != null && !Objects.equals(newHandle, realm.getHandle())) {
      realm.setHandle(newHandle);
      jdbcUpdate.setUpdatedColumnWithValue(RealmCols.HANDLE, realm.getHandle());
    }


    String newName = realmInputProps.getName();
    if (newName != null && !Objects.equals(newName, realm.getName())) {
      realm.setName(newName);
      jdbcUpdate.setUpdatedColumnWithValue(RealmCols.NAME, realm.getName());
    }

    OrgaUserGuid newOwnerUserGuid = realmInputProps.getOwnerUserGuid();
    if (newOwnerUserGuid != null && !Objects.equals(newOwnerUserGuid, realm.getOwnerUser().getGuid())) {
      OrgaUser newOwner = this.apiApp.getOrganizationUserProvider().toNewOwnerFromActualOwner(newOwnerUserGuid, realm.getOwnerUser());
      realm.setOwnerUser(newOwner);
      jdbcUpdate.setUpdatedColumnWithValue(RealmCols.OWNER_USER_ID, realm.getOwnerUser().getGuid().getUserId());
    }

    return jdbcUpdate
      .execute()
      .compose(rowSet -> {
          if (rowSet.size() != 1) {
            return Future.failedFuture(
              TowerFailureException.builder()
                .setMessage("The realm update (" + realm + ") updated not 1 row but " + rowSet.size())
                .build()
            );
          }
          return Future.succeededFuture(realm);
        }
      );

  }

  public Future<Realm> getRealmFromLocalId(Long realmId) {
    return this.realmTable.getSchema().getJdbcClient().getPool()
      .withConnection(sqlConnection -> this.getRealmFromLocalId(realmId, sqlConnection));
  }

  public Future<Realm> getRealmFromLocalId(Long realmId, SqlConnection sqlConnection) {

    return JdbcSelect.from(this.realmTable)
      .addEqualityPredicate(RealmCols.ID, realmId)
      .execute(sqlConnection)
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (userRows.size() != 1) {
          return Future.failedFuture(new InternalException("the realm id (" + realmId + ") returns  more than one application"));
        }
        JdbcRow row = userRows.iterator().next();
        return Future.succeededFuture(this.getRealmFromDatabaseRow(row));
      });
  }

  private Future<Realm> getRealmFromHandle(Handle realmHandle) {
    return this.realmTable.getSchema().getJdbcClient().getPool()
      .withConnection(sqlConnection -> this.getRealmFromHandle(realmHandle, sqlConnection));
  }

  /**
   * @param realmHandle   - the handle
   * @return the realm or null if not found
   */
  private Future<Realm> getRealmFromHandle(Handle realmHandle, SqlConnection sqlConnection) {

    return JdbcSelect.from(this.realmTable)
      .addEqualityPredicate(RealmCols.HANDLE, realmHandle.getValue())
      .execute(sqlConnection)
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (userRows.size() != 1) {
          return Future.failedFuture(new InternalException("the realm handle (" + realmHandle + ") returns more than one application"));
        }
        JdbcRow row = userRows.iterator().next();
        return Future.succeededFuture(this.getRealmFromDatabaseRow(row));

      });
  }

  public Future<List<Realm>> getRealmsForOwner(OrgaUser user) {

    return JdbcSelect.from(this.realmTable)
      .addEqualityPredicate(RealmCols.OWNER_ORGA_ID, user.getGuid().getOrganizationId())
      .addEqualityPredicate(RealmCols.OWNER_USER_ID, user.getGuid().getUserId())
      .execute()
      .compose(this::getRealmsFromRows,
        err -> Future.failedFuture(
          TowerFailureException.builder()
            .setType(TowerFailureTypeEnum.INTERNAL_ERROR_500)
            .setMessage("Unable to get the realm of the organization user (" + user + ")")
            .build()
        ));
  }


  private Future<List<Realm>> getRealmsFromRows(JdbcRowSet realmRows) {
    List<Realm> realms = new ArrayList<>();
    for (JdbcRow row : realmRows) {

      Realm futureRealm = this.getRealmFromDatabaseRow(row);
      realms.add(futureRealm);

    }
    return Future.succeededFuture(realms);
  }


  public Realm getRealmFromDatabaseRow(JdbcRow row) {


    /**
     * Identifiers
     */
    Long realmId = row.getLong(RealmCols.ID);
    Realm realm = Realm.createFromAnyId(realmId);
    realm.setHandle(Handle.ofFailSafe(row.getString(RealmCols.HANDLE)));

    /**
     * Analytics
     */
    realm.setUserCount(row.getLong(RealmCols.USER_COUNT));
    realm.setUserInCount(row.getInteger(RealmCols.USER_IN_COUNT));
    realm.setAppCount(row.getInteger(RealmCols.APP_COUNT));
    realm.setListCount(row.getInteger(RealmCols.LIST_COUNT));

    /**
     * Scalar Properties
     */
    realm.setName(row.getString(RealmCols.NAME));


    /**
     * User Owner
     */
    OrgaUserGuid orgaUserGuid = new OrgaUserGuid.Builder()
      .setRealmId(row.getLong(RealmCols.OWNER_REALM_ID))
      .setOrgaId(row.getLong(RealmCols.OWNER_ORGA_ID))
      .setUserId(row.getLong(RealmCols.OWNER_USER_ID))
      .build();
    OrgaUser ownerUser = this.apiApp.getOrganizationUserProvider().toNewOwnerFromGuid(orgaUserGuid);
    realm.setOwnerUser(ownerUser);

    /**
     * Time
     */
    realm.setCreationTime(row.getLocalDateTime(RealmCols.CREATION_TIME));
    realm.setModificationTime(row.getLocalDateTime(RealmCols.MODIFICATION_TIME));

    return realm;

  }

  public Future<Realm> getRealmFromGuid(RealmGuid guid) {

    return getRealmFromLocalId(guid.getLocalId());
  }


  public Future<Realm> getRealmFromIdentifier(String realmIdentifier) {

    try {
      RealmGuid realmGuid = this.apiApp.getJackson().getDeserializer(RealmGuid.class).deserialize(realmIdentifier);
      return getRealmFromGuid(realmGuid);
    } catch (CastException e) {
      //
    }

    try {
      return getRealmFromHandle(Handle.of(realmIdentifier));
    } catch (HandleCastException e) {
      return Future.failedFuture(TowerFailureException
        .builder()
        .setMessage("The realm identifier (" + realmIdentifier + ") is not a guid, nor a valid handle. Message: " + e.getMessage())
        .build()
      );
    }

  }

  /**
   * Getsert: get or insert a realm with a local id or a handle
   * @param realmInputProps         - the realm to insert
   * @param sqlConnection - the insertion connection to defer constraint on transaction
   * @return the realm inserted
   */
  public Future<Realm> getsertOnServerStartup(Long realmId, RealmInputProps realmInputProps, SqlConnection sqlConnection) {
    Future<Realm> selectRealmFuture;
    if (realmId != null) {
      selectRealmFuture = this.getRealmFromLocalId(realmId, sqlConnection);
    } else {
      Handle realmHandle = realmInputProps.getHandle();
      if (realmHandle == null) {
        return Future.failedFuture(new InternalException("The realm to getsert should have an identifier (id, or handle)"));
      }
      selectRealmFuture = this.getRealmFromHandle(realmHandle, sqlConnection);
    }
    return selectRealmFuture
      .compose(selectedRealm -> {
        Future<Realm> futureRealm;
        if (selectedRealm != null) {
          futureRealm = Future.succeededFuture(selectedRealm);
        } else {
          futureRealm = this.insertRealm(realmInputProps, sqlConnection, realmId);
        }
        return futureRealm;
      });
  }

}
