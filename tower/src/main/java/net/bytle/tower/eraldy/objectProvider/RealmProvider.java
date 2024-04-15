package net.bytle.tower.eraldy.objectProvider;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;
import io.vertx.json.schema.ValidationException;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NotFoundException;
import net.bytle.java.JavaEnvs;
import net.bytle.tower.EraldyModel;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.auth.AuthUserScope;
import net.bytle.tower.eraldy.mixin.AppPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.mixin.OrganizationPublicMixin;
import net.bytle.tower.eraldy.mixin.RealmPublicMixin;
import net.bytle.tower.eraldy.mixin.UserPublicMixinWithoutRealm;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.db.*;
import net.bytle.vertx.jackson.JacksonMapperManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Manage the get/upsert of a {@link Realm} object asynchronously
 * in the database
 */
public class RealmProvider {

  public static final String TABLE_PREFIX = "realm";

  public static final String REALM_GUID_PREFIX = "rea";

  protected static final Logger LOGGER = LoggerFactory.getLogger(RealmProvider.class);


  private final EraldyApiApp apiApp;

  /**
   * The json mapper for public realm
   * to create json without any localId and
   * double realm information
   */
  private final ObjectMapper publicRealmJsonMapper;
  private final JdbcTable realmTable;


  public RealmProvider(EraldyApiApp apiApp, JdbcSchema schema) {

    this.apiApp = apiApp;
    JacksonMapperManager jacksonMapperManager = this.apiApp.getHttpServer().getServer().getJacksonMapperManager();
    this.publicRealmJsonMapper = jacksonMapperManager
      .jsonMapperBuilder()
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(Realm.class, RealmPublicMixin.class)
      .addMixIn(Organization.class, OrganizationPublicMixin.class)
      .addMixIn(User.class, UserPublicMixinWithoutRealm.class)
      .addMixIn(App.class, AppPublicMixinWithoutRealm.class)
      .build();

    this.realmTable = JdbcTable.build(schema, "realm")
      .addPrimaryKeyColumn(RealmCols.ID)
      .addUniqueKeyColumn(RealmCols.HANDLE)
      .build();

  }


  /**
   * @param realm - the realm
   * @return the name or the handle of null
   */
  public static String getNameOrHandle(Realm realm) {
    if (realm.getName() != null) {
      return realm.getName();
    }
    return realm.getHandle();
  }


  /**
   * Compute the guid. This is another function
   * to be sure that the object id and guid are consistent
   *
   * @param realm - the realm
   */
  void updateGuid(Realm realm) {
    if (realm.getGuid() != null) {
      return;
    }
    String guid = this.getGuidFromLong(realm.getLocalId()).toString();
    realm.setGuid(guid);
  }


  /**
   * @param realm - the realm to update
   * @throws CastException     - if the guid is not valid
   * @throws NotFoundException if the guid is empty
   */
  void updateIdFromGuid(Realm realm) throws CastException, NotFoundException {
    String guid = realm.getGuid();
    if (guid == null) {
      throw new NotFoundException("The guid is empty");
    }
    Long id = apiApp.createGuidFromRealmOrOrganizationId(REALM_GUID_PREFIX, guid).getRealmOrOrganizationId();
    realm.setLocalId(id);
  }


  public Guid getGuidFromHash(String guid) throws CastException {
    return apiApp.createGuidFromHashWithOneId(REALM_GUID_PREFIX, guid);
  }


  Guid getGuidFromLong(Long realmId) {
    return apiApp.createGuidFromObjectId(REALM_GUID_PREFIX, realmId);
  }


  public boolean isRealmGuidIdentifier(String realmIdentifier) {
    return realmIdentifier.startsWith(REALM_GUID_PREFIX + Guid.GUID_SEPARATOR);
  }


  public ObjectMapper getPublicJsonMapper() {
    return this.publicRealmJsonMapper;
  }


  /**
   *
   * @deprecated should use/implement a map cache instead
   */
  @Deprecated
  public Future<Realm> getRealmFromLocalIdOrAutCli(long realmId, RoutingContext routingContext) {
    Realm authRealmClient = this.apiApp.getAuthClientProvider().getRequestingClient(routingContext).getApp().getRealm();
    if (authRealmClient.getLocalId() == realmId) {
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
   * @param realm the realm to upsert
   * @return the realm with the id
   */
  public Future<Realm> upsertRealm(Realm realm) {

    if (realm.getLocalId() != null || realm.getGuid() != null) {
      return updateRealm(realm);
    }
    String handle = realm.getHandle();
    if (handle == null) {
      InternalException internalException = new InternalException("The realm handle is mandatory to upsert a realm");
      return Future.failedFuture(internalException);
    }
    /**
     * We don't use the SQL upsert statement
     * to no create a gap in the sequence
     * See identifier.md for more info
     */
    return updateRealmByHandleAndGetRowSet(realm)
      .compose(rowSet -> {
        if (rowSet.size() == 0) {
          return insertRealm(realm);
        }
        Long realmId = rowSet.iterator().next().getLong(RealmCols.ID);
        realm.setLocalId(realmId);
        this.updateGuid(realm);
        return Future.succeededFuture(realm);
      });
  }

  /**
   * @param realm - the realm to check
   * @return true or false
   */
  @SuppressWarnings("unused")
  private Future<Boolean> exists(Realm realm) {
    String sql;
    JdbcSelect jdbcSelect = JdbcSelect.from(this.realmTable);
    if (realm.getLocalId() != null || realm.getGuid() != null) {
      if (realm.getLocalId() == null) {
        try {
          this.updateIdFromGuid(realm);
        } catch (CastException e) {
          return Future.failedFuture(ValidationException.create("The Guid is not valid", "guid", realm.getGuid()));
        } catch (NotFoundException e) {
          return Future.failedFuture(new InternalException("The Guid was not found (should not happen)"));
        }
      }
      jdbcSelect.addEqualityPredicate(RealmCols.ID, realm.getLocalId());
    } else {
      String handle = realm.getHandle();
      if (handle == null) {
        String failureMessage = "An id, guid or handle should be given to check the existence of a realm";
        InternalException internalException = new InternalException(failureMessage);
        return Future.failedFuture(internalException);
      }
      jdbcSelect.addEqualityPredicate(RealmCols.HANDLE, handle);
    }
    return jdbcSelect
      .execute()
      .compose(rows -> {
        if (rows.size() == 1) {
          return Future.succeededFuture(true);
        } else {
          return Future.succeededFuture(false);
        }
      });
  }

  private Future<Realm> insertRealm(Realm realm) {
    return this.realmTable.getSchema().getJdbcClient().getPool().withConnection(sqlConnection -> this.insertRealm(realm,sqlConnection));
  }
  private Future<Realm> insertRealm(Realm realm, SqlConnection sqlConnection) {

    String handle = realm.getHandle();
    if (handle == null) {
      throw new InternalException("The realm handle cannot be null on realm insertion");
    }
    OrganizationUser ownerUser = realm.getOwnerUser();
    if (ownerUser == null) {
      throw new InternalException("The owner user of the realm (handle: " + handle + ") cannot be null on realm insertion");
    }

    /**
     * Create the insert
     * (the id may be known as it's the case
     * when inserting the fist Eraldy realm with the id 1)
     */


    return JdbcInsert.into(this.realmTable)
      .addColumn(RealmCols.HANDLE, realm.getHandle())
      .addColumn(RealmCols.ORGA_ID, realm.getOrganization().getLocalId())
      .addColumn(RealmCols.NAME, realm.getHandle())
      .addColumn(RealmCols.OWNER_ID, realm.getOwnerUser().getLocalId())
      .addColumn(RealmCols.CREATION_TIME, DateTimeService.getNowInUtc())
      .addReturningColumn(RealmCols.ID)
      .execute(sqlConnection)
      .compose(rows -> {
        Long realmId = rows.iterator().next().getLong(RealmCols.ID);
        Long askedRealmLocalIdOnInsert = realm.getLocalId();
        if (askedRealmLocalIdOnInsert != null && !askedRealmLocalIdOnInsert.equals(realmId)) {
          /**
           * Case when we insert a realm when we want the id
           * The Eraldy realm should be 1 is the main case
           */
          String error = "The asked realm id (" + askedRealmLocalIdOnInsert + ") did not get the same id but the id (" + realmId + ")";
          if (JavaEnvs.IS_DEV) {
            error += "In Dev, delete the SQL schema and restart. An error like that is due to an error on start between the 2 inserts";
          }
          return Future.failedFuture(new InternalException(error));
        }
        realm.setLocalId(realmId);
        this.updateGuid(realm);
        return Future.succeededFuture(realm);
      });
  }


  private Future<Realm> updateRealm(Realm realm) {


    if (realm.getLocalId() != null) {


      return JdbcUpdate.into(this.realmTable)
        .addPredicateColumn(RealmCols.ID, realm.getLocalId())
        .addUpdatedColumn(RealmCols.HANDLE, realm.getHandle())
        .addUpdatedColumn(RealmCols.ORGA_ID, realm.getOrganization().getLocalId())
        .addUpdatedColumn(RealmCols.NAME, realm.getName())
        .addUpdatedColumn(RealmCols.MODIFICATION_TIME, DateTimeService.getNowInUtc())
        .execute()
        .compose(ok -> {
            // Compute the guid: A realm may have an id without guid
            // This is the case for the Eraldy realm where the database id is known
            // as instantiation but not the guid
            this.updateGuid(realm);
            return Future.succeededFuture(realm);
          }
        );
    }

    String handle = realm.getHandle();
    if (handle == null) {
      InternalException internalException = new InternalException("To update a realm, an id or handle is mandatory");
      return Future.failedFuture(internalException);
    }

    return updateRealmByHandleAndGetRowSet(realm)
      .compose(rows -> {
        Long realmId = rows.iterator().next().getLong(RealmCols.ID);
        realm.setLocalId(realmId);
        this.updateGuid(realm);
        return Future.succeededFuture(realm);
      });

  }

  private Future<JdbcRowSet> updateRealmByHandleAndGetRowSet(Realm realm) {

    Organization organization = realm.getOrganization();
    if (organization == null) {
      throw new IllegalArgumentException("The organization of realm can not be null");
    }

    return JdbcUpdate.into(this.realmTable)
      .addPredicateColumn(RealmCols.HANDLE, realm.getHandle())
      .addUpdatedColumn(RealmCols.ORGA_ID, realm.getOrganization().getLocalId())
      .addUpdatedColumn(RealmCols.NAME, realm.getName())
      .addUpdatedColumn(RealmCols.MODIFICATION_TIME, DateTimeService.getNowInUtc())
      .addReturningColumn(RealmCols.ID)
      .execute();

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
        return this.getRealmFromDatabaseRow(row);
      });
  }

  private Future<Realm> getRealmFromHandle(String realmHandle) {
    return this.realmTable.getSchema().getJdbcClient().getPool()
      .withConnection(sqlConnection -> this.getRealmFromHandle(realmHandle, sqlConnection));
  }

  /**
   * @param realmHandle   - the handle
   * @return the realm or null if not found
   */
  private Future<Realm> getRealmFromHandle(String realmHandle, SqlConnection sqlConnection) {

    return JdbcSelect.from(this.realmTable)
      .addEqualityPredicate(RealmCols.HANDLE, realmHandle)
      .execute(sqlConnection)
      .compose(userRows -> {

        if (userRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (userRows.size() != 1) {
          return Future.failedFuture(new InternalException("the realm handle (" + realmHandle + ") returns more than one application"));
        }
        JdbcRow row = userRows.iterator().next();
        return this.getRealmFromDatabaseRow(row);

      });
  }

  public Future<List<Realm>> getRealmsForOwner(OrganizationUser user) {

    return JdbcSelect.from(this.realmTable)
      .addEqualityPredicate(RealmCols.ORGA_ID, user.getOrganization().getLocalId())
      .addEqualityPredicate(RealmCols.OWNER_ID, user.getLocalId())
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
    List<Future<Realm>> futureRealms = new ArrayList<>();
    for (JdbcRow row : realmRows) {

      Future<Realm> futureRealm = this.getRealmFromDatabaseRow(row);
      futureRealms.add(futureRealm);

    }
    return Future.all(futureRealms)
      .recover(t -> Future.failedFuture(new InternalException("Error while getting the future realms", t)))
      .compose(compositeFuture -> {
        if (compositeFuture.failed()) {
          return Future.failedFuture(new InternalException("getRealms For row composite failure", compositeFuture.cause()));
        }
        return Future.succeededFuture(compositeFuture.list());
      });
  }


  public Future<Realm> getRealmFromDatabaseRow(JdbcRow row) {

    Realm realm = new Realm();

    /**
     * Identifiers
     */
    Long realmId = row.getLong(RealmCols.ID);
    realm.setLocalId(realmId);
    this.updateGuid(realm);
    realm.setHandle(row.getString(RealmCols.HANDLE));

    /**
     * Analytics
     */
    realm.setUserCount(Objects.requireNonNullElse(row.getLong(RealmCols.USER_COUNT), 0L));
    realm.setAppCount(Objects.requireNonNullElse(row.getLong(RealmCols.APP_COUNT), 0L));
    realm.setListCount(Objects.requireNonNullElse(row.getLong(RealmCols.LIST_COUNT), 0L));

    /**
     * Scalar Properties
     */
    realm.setName(row.getString(RealmCols.NAME));

    /**
     * Eraldy Realm
     */
    EraldyApiApp apiApp = this.getApp();
    EraldyModel eraldyModel = apiApp.getEraldyModel();
    Realm eraldyRealm;
    if (eraldyModel.isRealmLocalId(realmId)) {
      // Special case when we build the eraldy realm itself
      // on start
      // otherwise we got a recursion
      eraldyRealm = realm;
    } else {
      eraldyRealm = eraldyModel.getRealm();
    }

    /**
     * Future Org
     */
    Long orgaId = row.getLong(RealmCols.ORGA_ID);
    Future<Organization> futureOrganization = apiApp.getOrganizationProvider().getById(orgaId);
    return futureOrganization
      .recover(t -> Future.failedFuture(new InternalException("Future organization for building the realm failed", t)))
      .compose(organization -> {
        realm.setOrganization(organization);
        /**
         * Future Owner
         */
        Long ownerUserLocalId = row.getLong(RealmCols.OWNER_ID);
        return apiApp.getOrganizationUserProvider()
          .getOrganizationUserByLocalId(ownerUserLocalId, eraldyRealm.getLocalId(), eraldyRealm);
      })
      .recover(t -> Future.failedFuture(new InternalException("Future user for building the realm failed", t)))
      .compose(user -> {
        realm.setOwnerUser(user);
        return Future.succeededFuture(realm);
      });

  }

  private Future<Realm> getRealmFromGuid(String guid) {
    long realmId;
    try {
      realmId = this.getGuidFromHash(guid).getRealmOrOrganizationId();
    } catch (CastException e) {
      return Future.failedFuture(e);
    }
    return getRealmFromLocalId(realmId);
  }

  public Future<List<RealmWithAppUris>> getRealmsWithAppUris() {
    String aliasAppUris = "app_uris";
    String selectRealmSql = "select " +
      RealmCols.ID.getColumnName() + ",\n" +
      RealmCols.HANDLE.getColumnName() + ",\n" +
      "STRING_AGG(app_uri,', ') as " + aliasAppUris + "\n" +
      "  from \n" +
      this.realmTable.getSchema().getSchemaName() + "." + AppProvider.APP_TABLE_NAME + " app\n" +
      "right join " + this.realmTable.getFullName() + " realm\n" +
      "on app.app_realm_id = realm.realm_id\n" +
      "group by " + RealmCols.ID.getColumnName() + ", " + RealmCols.HANDLE.getColumnName() + "\n" +
      "order by " + RealmCols.HANDLE.getColumnName();
    return this.realmTable.getSchema().getJdbcClient().getPool().preparedQuery(selectRealmSql)
      .execute()
      .onFailure(e -> LOGGER.error("select realms error. Error: " + e.getMessage() + ". With the sql:\n" + selectRealmSql, e))
      .compose(realmRows -> {

        List<RealmWithAppUris> realmsWithDomains = new ArrayList<>();
        for (Row row : realmRows) {

          RealmWithAppUris realmWithDomains = new RealmWithAppUris();
          realmsWithDomains.add(realmWithDomains);

          Long realmId = row.getLong(RealmCols.ID.getColumnName());
          realmWithDomains.setGuid(this.getGuidFromLong(realmId).toString());

          String realmHandle = row.getString(RealmCols.HANDLE.getColumnName());
          realmWithDomains.setHandle(realmHandle);

          String app_domains = row.getString(aliasAppUris);
          List<URI> appDomains = new ArrayList<>();
          if (app_domains != null) {
            appDomains = Arrays.stream(app_domains
                .split(", "))
              .map(URI::create)
              .collect(Collectors.toList());
          }
          realmWithDomains.setAppUris(appDomains);

        }
        return Future.succeededFuture(realmsWithDomains);
      });
  }


  public Future<Realm> getRealmFromIdentifier(String realmIdentifier) {

    if (this.isRealmGuidIdentifier(realmIdentifier)) {
      return getRealmFromGuid(realmIdentifier);
    }
    return getRealmFromHandle(realmIdentifier);

  }

  /**
   * Wrapper around {@link #getRealmFromIdentifier(String)}
   * and fail if the realm was not found (ie null)
   *
   * @param realmIdentifier - the realm identifier
   * @return the realm
   */
  public Future<Realm> getRealmFromIdentifierNotNull(String realmIdentifier) {
    return this.getRealmFromIdentifier(realmIdentifier)
      .compose(realm -> {
        if (realm == null) {
          return Future.failedFuture(
            TowerFailureException.builder()
              .setType(TowerFailureTypeEnum.NOT_FOUND_404)
              .setMessage("The realm identifier (" + realmIdentifier + ") was not found")
              .build()
          );
        }
        return Future.succeededFuture(realm);
      });
  }

  /**
   * Getsert: get or insert a realm with a local id or a handle
   *
   * @param realm         - the realm to insert
   * @param sqlConnection - the insertion connection to defer constraint on transaction
   * @return the realm inserted
   */
  public Future<Realm> getsertOnServerStartup(Realm realm, SqlConnection sqlConnection) {
    Future<Realm> selectRealmFuture;
    if (realm.getLocalId() != null) {
      selectRealmFuture = this.getRealmFromLocalId(realm.getLocalId(), sqlConnection);
    } else {
      String realmHandle = realm.getHandle();
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
          futureRealm = this.insertRealm(realm,sqlConnection);
        }
        return futureRealm;
      });
  }

  public Future<Realm> getRealmAnalyticsFromIdentifier(String realmIdentifier) {
    return getRealmFromIdentifier(realmIdentifier);
  }

}
