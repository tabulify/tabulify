package net.bytle.tower.eraldy.module.organization.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlConnection;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.module.organization.inputs.OrganizationInputProps;
import net.bytle.tower.eraldy.module.organization.jackson.JacksonOrgaGuidDeserializer;
import net.bytle.tower.eraldy.module.organization.jackson.JacksonOrgaGuidSerializer;
import net.bytle.tower.eraldy.module.organization.model.OrgaGuid;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.tower.eraldy.module.organization.model.Organization;
import net.bytle.tower.eraldy.module.realm.model.Realm;
import net.bytle.type.Handle;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.db.*;
import net.bytle.vertx.guid.GuidDeSer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

import static net.bytle.vertx.db.JdbcSchemaManager.COLUMN_PART_SEP;

public class OrganizationProvider {


  protected static final Logger LOGGER = LoggerFactory.getLogger(OrganizationProvider.class);
  public static final String TABLE_NAME = "realm_orga";

  /**
   * Lower case is important
   */
  private static final String TABLE_PREFIX = "orga";

  public static final String ORGA_ID_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + "id";
  public static final String GUID_PREFIX = "org";
  private final EraldyApiApp apiApp;
  private final Pool jdbcPool;
  private final JdbcTable orgaTable;


  public OrganizationProvider(EraldyApiApp apiApp, JdbcSchema jdbcSchema) {
    this.apiApp = apiApp;
    this.jdbcPool = apiApp.getHttpServer().getServer().getPostgresClient().getPool();
    this.orgaTable = JdbcTable.build(jdbcSchema, TABLE_NAME, OrganizationCols.values())
      .addPrimaryKeyColumn(OrganizationCols.ID)
      .addPrimaryKeyColumn(OrganizationCols.REALM_ID)
      .addUniqueKeyColumns(OrganizationCols.REALM_ID, OrganizationCols.HANDLE)
      .build();

    GuidDeSer orgaGuidDeser = this.apiApp.getHttpServer().getServer().getHashId().getGuidDeSer(GUID_PREFIX, 2);
    this.apiApp.getJackson()
      .addSerializer(OrgaGuid.class, new JacksonOrgaGuidSerializer(orgaGuidDeser))
      .addDeserializer(OrgaGuid.class, new JacksonOrgaGuidDeserializer(orgaGuidDeser));
  }


  public Future<Organization> getByGuid(OrgaGuid orgaId, Realm realm) {

    return jdbcPool.withConnection(sqlConnection -> getByGuid(orgaId, realm, sqlConnection));
  }

  private Future<Organization> getByGuid(OrgaGuid orgaGuid, Realm realm, SqlConnection sqlConnection) {

    return JdbcSelect.from(this.orgaTable)
      .addEqualityPredicate(OrganizationCols.ID, orgaGuid.getOrgaId())
      .addEqualityPredicate(OrganizationCols.REALM_ID, orgaGuid.getRealmId())
      .execute(sqlConnection)
      .onFailure(e -> LOGGER.error("Error: " + e.getMessage() + ", while retrieving the orga by id", e))
      .compose(orgRows -> {

        if (orgRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (orgRows.size() != 1) {
          return Future.failedFuture(new InternalException("the orga guid (" + orgaGuid.toStringLocalIds() + ") returns more than one row"));
        }
        JdbcRow row = orgRows.iterator().next();
        return this.getOrganizationFromDatabaseRow(row, realm);
      });
  }

  private Future<Organization> getOrganizationFromDatabaseRow(JdbcRow row, Realm realm) {

    Organization organization = new Organization();
    OrgaGuid orgaGuid = new OrgaGuid();
    organization.setGuid(orgaGuid);
    orgaGuid.setOrgaId(row.getLong(OrganizationCols.ID));
    orgaGuid.setRealmId(row.getLong(OrganizationCols.REALM_ID));

    organization.setRealm(realm);
    organization.setCreationTime(row.getLocalDateTime(OrganizationCols.CREATION_TIME));
    organization.setModificationTime(row.getLocalDateTime(OrganizationCols.MODIFICATION_TIME));
    organization.setHandle(Handle.ofFailSafe(row.getString(OrganizationCols.HANDLE)));
    organization.setName(row.getString(OrganizationCols.NAME));

    return Future.succeededFuture(organization);

  }

  /**
   * Getsert: Get or insert the user
   */
  public Future<Organization> getsert(Realm realm, OrganizationInputProps organizationInputProps, SqlConnection sqlConnection) {

    return this.getByHandle(organizationInputProps.getHandle(), realm, sqlConnection)
      .recover(t -> Future.failedFuture(new InternalException("Error while selecting the organization", t)))
      .compose(storedOrganization -> {
        if (storedOrganization != null) {
          return Future.succeededFuture(storedOrganization);
        } else {
          return this.insert(realm, organizationInputProps, sqlConnection);
        }
      });

  }

  private Future<Organization> getByHandle(Handle handle, Realm realm, SqlConnection sqlConnection) {
    return JdbcSelect.from(this.orgaTable)
      .addEqualityPredicate(OrganizationCols.HANDLE, handle.getValue())
      .addEqualityPredicate(OrganizationCols.REALM_ID, realm.getGuid().getLocalId())
      .execute(sqlConnection)
      .onFailure(e -> LOGGER.error("Error: " + e.getMessage() + ", while retrieving the orga by id", e))
      .compose(orgRows -> {

        if (orgRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (orgRows.size() != 1) {
          return Future.failedFuture(new InternalException("the orga handle (" + handle + ") returns more than one row"));
        }
        JdbcRow row = orgRows.iterator().next();
        return this.getOrganizationFromDatabaseRow(row, realm);
      });
  }

  private Future<Organization> insert(Realm realm, OrganizationInputProps organizationInputProps, SqlConnection sqlConnection) {


    JdbcInsert jdbcInsert = JdbcInsert.into(this.orgaTable);
    Organization organization = new Organization();

    LocalDateTime nowInUtc = DateTimeService.getNowInUtc();
    organization.setCreationTime(nowInUtc);
    jdbcInsert.addColumn(OrganizationCols.CREATION_TIME, organization.getCreationTime());
    organization.setModificationTime(nowInUtc);
    jdbcInsert.addColumn(OrganizationCols.MODIFICATION_TIME, organization.getModificationTime());

    if (organizationInputProps.getHandle() != null) {
      organization.setHandle(organizationInputProps.getHandle());
      jdbcInsert.addColumn(OrganizationCols.HANDLE, organization.getHandle().getValue());
    }

    organization.setName(organizationInputProps.getName());
    jdbcInsert.addColumn(OrganizationCols.NAME, organization.getName());

    /**
     * The owner should be in the same realm as the organization
     */
    OrgaUserGuid ownerGuid = organizationInputProps.getOwnerUserGuid();
    if (ownerGuid.getRealmId() != realm.getGuid().getLocalId()) {
      return Future.failedFuture(
        TowerFailureException
          .builder()
          .setMessage("The realm id (" + realm.getGuid().getLocalId() + ") and the owner realm id (" + ownerGuid.getRealmId() + ") are not the same")
          .build()
      );
    }
    OrgaUser ownerUser = this.apiApp.getOrganizationUserProvider().toNewOwnerFromGuid(ownerGuid);
    organization.setOwnerUser(ownerUser);
    jdbcInsert.addColumn(OrganizationCols.OWNER_ID, organization.getOwnerUser().getGuid().getUserId());

    return this.apiApp.getRealmSequenceProvider()
      .getNextIdForTableAndRealm(sqlConnection, realm, this.orgaTable)
      .compose(nextId -> {
        OrgaGuid orgaGuid = new OrgaGuid();
        organization.setGuid(orgaGuid);
        orgaGuid.setRealmId(realm.getGuid().getLocalId());
        orgaGuid.setOrgaId(nextId);
        jdbcInsert.addColumn(OrganizationCols.ID, organization.getGuid().getOrgaId());
        jdbcInsert.addColumn(OrganizationCols.REALM_ID, organization.getGuid().getRealmId());
        return jdbcInsert
          .execute(sqlConnection)
          .compose(v -> Future.succeededFuture(organization));
      });
  }


}
