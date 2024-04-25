package net.bytle.tower.eraldy.module.organization.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlConnection;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.model.openapi.Organization;
import net.bytle.tower.eraldy.module.organization.inputs.OrganizationInputProps;
import net.bytle.tower.eraldy.module.organization.jackson.JacksonOrgaGuidDeserializer;
import net.bytle.tower.eraldy.module.organization.jackson.JacksonOrgaGuidSerializer;
import net.bytle.tower.eraldy.module.organization.model.OrgaGuid;
import net.bytle.tower.eraldy.module.organization.model.OrgaUserGuid;
import net.bytle.type.Handle;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.db.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

import static net.bytle.vertx.db.JdbcSchemaManager.COLUMN_PART_SEP;

public class OrganizationProvider {


  protected static final Logger LOGGER = LoggerFactory.getLogger(OrganizationProvider.class);
  public static final String TABLE_NAME = "organization";

  /**
   * Lower case is important
   */
  private static final String TABLE_PREFIX = "orga";

  public static final String ORGA_ID_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + "id";
  public static final String GUID_PREFIX = "org";
  private final EraldyApiApp apiApp;
  @SuppressWarnings("unused")
  private final Pool jdbcPool;
  private final JdbcTable orgaTable;


  public OrganizationProvider(EraldyApiApp apiApp, JdbcSchema jdbcSchema) {
    this.apiApp = apiApp;
    this.jdbcPool = apiApp.getHttpServer().getServer().getPostgresClient().getPool();
    this.orgaTable = JdbcTable.build(jdbcSchema, TABLE_NAME, OrganizationCols.values())
      .addPrimaryKeyColumn(OrganizationCols.ID)
      .build();
    this.apiApp.getJackson()
      .addSerializer(OrgaGuid.class, new JacksonOrgaGuidSerializer(apiApp))
      .addDeserializer(OrgaGuid.class, new JacksonOrgaGuidDeserializer(apiApp));
  }


  public Future<Organization> getByGuid(OrgaGuid orgaId) {
    return getByGuid(orgaId, Organization.class);
  }

  @SuppressWarnings("SameParameterValue")
  private <T extends Organization> Future<T> getByGuid(OrgaGuid orgaId, Class<T> clazz) {

    return jdbcPool.withConnection(sqlConnection -> getByGuid(orgaId, clazz, sqlConnection));
  }

  private <T extends Organization> Future<T> getByGuid(OrgaGuid orgaGuid, Class<T> clazz, SqlConnection sqlConnection) {

    return JdbcSelect.from(this.orgaTable)
      .addEqualityPredicate(OrganizationCols.ID, orgaGuid.getLocalId())
      .execute(sqlConnection)
      .onFailure(e -> LOGGER.error("Error: " + e.getMessage() + ", while retrieving the orga by id", e))
      .compose(orgRows -> {

        if (orgRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (orgRows.size() != 1) {
          return Future.failedFuture(new InternalException("the orga id (" + orgaGuid + ") returns more than one row"));
        }
        JdbcRow row = orgRows.iterator().next();
        return this.getOrganizationFromDatabaseRow(row, clazz);
      });
  }

  private <T extends Organization> Future<T> getOrganizationFromDatabaseRow(JdbcRow row, Class<T> clazz) {

    Organization organization = new Organization();
    organization.setCreationTime(row.getLocalDateTime(OrganizationCols.CREATION_TIME));
    organization.setModificationTime(row.getLocalDateTime(OrganizationCols.MODIFICATION_TIME));
    organization.setHandle(Handle.ofFailSafe(row.getString(OrganizationCols.HANDLE)));
    organization.setName(row.getString(OrganizationCols.NAME));
    organization.setGuid(new OrgaGuid(row.getLong(OrganizationCols.ID)));
    return Future.succeededFuture(clazz.cast(organization));

  }

  /**
   * Getsert: Get or insert the user
   */
  public Future<Organization> getsert(OrgaGuid organizationId, OrganizationInputProps organizationInputProps, SqlConnection sqlConnection) {

    return this.getByGuid(organizationId, sqlConnection)
      .recover(t -> Future.failedFuture(new InternalException("Error while selecting the organization", t)))
      .compose(storedOrganization -> {
        if (storedOrganization != null) {
          return Future.succeededFuture(storedOrganization);
        } else {
          return this.insert(organizationId, organizationInputProps, sqlConnection);
        }
      });

  }

  private Future<Organization> insert(OrgaGuid organizationGuid, OrganizationInputProps organizationInputProps, SqlConnection sqlConnection) {


    JdbcInsert jdbcInsert = JdbcInsert.into(this.orgaTable);
    Organization organization = new Organization();

    LocalDateTime nowInUtc = DateTimeService.getNowInUtc();
    organization.setCreationTime(nowInUtc);
    jdbcInsert.addColumn(OrganizationCols.CREATION_TIME, organization.getCreationTime());
    organization.setModificationTime(nowInUtc);
    jdbcInsert.addColumn(OrganizationCols.MODIFICATION_TIME, organization.getModificationTime());

    if (organizationGuid != null) {
      organization.setGuid(organizationGuid);
      jdbcInsert.addColumn(OrganizationCols.ID, organization.getGuid().getLocalId());
    }

    if (organizationInputProps.getHandle() != null) {
      organization.setHandle(organizationInputProps.getHandle());
      jdbcInsert.addColumn(OrganizationCols.HANDLE, organization.getHandle().getValue());
    }

    organization.setName(organizationInputProps.getName());
    jdbcInsert.addColumn(OrganizationCols.NAME, organization.getName());


    OrgaUserGuid ownerGuid = organizationInputProps.getOwnerGuid();
    OrgaUser ownerUser = this.apiApp.getOrganizationUserProvider().toOrgaUserFromGuid(ownerGuid, this.apiApp.getEraldyModel().getRealm());
    organization.setOwnerUser(ownerUser);
    jdbcInsert
      .addColumn(OrganizationCols.OWNER_ID, organization.getOwnerUser().getGuid().getLocalId())
      .addColumn(OrganizationCols.REALM_ID, organization.getOwnerUser().getGuid().getRealmId());

    return jdbcInsert
      .addReturningColumn(OrganizationCols.ID)
      .execute(sqlConnection)
      .compose(orgRows -> {
        long orgLocalId = orgRows.iterator().next().getLong(OrganizationCols.ID);

        if (organizationGuid != null && organizationGuid.getLocalId() != orgLocalId) {
          return Future.failedFuture(new InternalException("The asked organization id (" + organizationGuid + ") was not the one inserted (" + orgLocalId + ")"));
        }

        organization.setGuid(new OrgaGuid(orgLocalId));

        return Future.succeededFuture(organization);
      });
  }

  Future<Organization> getByGuid(OrgaGuid localId, SqlConnection sqlConnection) {
    return getByGuid(localId, Organization.class, sqlConnection);
  }


}
