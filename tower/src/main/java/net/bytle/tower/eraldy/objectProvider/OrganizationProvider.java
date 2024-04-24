package net.bytle.tower.eraldy.objectProvider;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.SqlConnection;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.OrgaUser;
import net.bytle.tower.eraldy.model.openapi.Organization;
import net.bytle.tower.eraldy.module.organization.db.OrganizationCols;
import net.bytle.tower.eraldy.module.organization.inputs.OrganizationInputProps;
import net.bytle.tower.eraldy.module.organization.jackson.JacksonOrgaGuidDeserializer;
import net.bytle.tower.eraldy.module.organization.jackson.JacksonOrgaGuidSerializer;
import net.bytle.tower.eraldy.module.organization.model.OrgaGuid;
import net.bytle.tower.util.Guid;
import net.bytle.type.Handle;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.db.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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


  public Future<Organization> getById(Long orgaId) {
    return getById(orgaId, Organization.class);
  }

  @SuppressWarnings("SameParameterValue")
  private <T extends Organization> Future<T> getById(Long orgaId, Class<T> clazz) {

    return jdbcPool.withConnection(sqlConnection -> getById(orgaId, clazz, sqlConnection));
  }

  private <T extends Organization> Future<T> getById(Long orgaId, Class<T> clazz, SqlConnection sqlConnection) {

    return JdbcSelect.from(this.orgaTable)
      .addEqualityPredicate(OrganizationCols.ID, orgaId)
      .execute(sqlConnection)
      .onFailure(e -> LOGGER.error("Error: " + e.getMessage() + ", while retrieving the orga by id", e))
      .compose(orgRows -> {

        if (orgRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (orgRows.size() != 1) {
          return Future.failedFuture(new InternalException("the orga id (" + orgaId + ") returns more than one row"));
        }
        JdbcRow row = orgRows.iterator().next();
        return this.getOrganizationFromDatabaseRow(row, clazz);
      });
  }

  private <T extends Organization> Future<T> getOrganizationFromDatabaseRow(JdbcRow row, Class<T> clazz) {

    Organization organization = new Organization();
    organization.setCreationTime(row.getLocalDateTime(OrganizationCols.CREATION_TIME));
    organization.setHandle(Handle.ofFailSafe(row.getString(OrganizationCols.HANDLE)));
    organization.setLocalId(row.getLong(OrganizationCols.ID));
    organization.setModificationTime(row.getLocalDateTime(OrganizationCols.MODIFICATION_IME));

    this.updateGuid(organization);

    return Future.succeededFuture(clazz.cast(organization));

  }

  private void updateGuid(Organization organization) {
    OrgaGuid orgaGuid = new OrgaGuid();
    orgaGuid.setLocalId(organization.getLocalId());
    organization.setGuid(orgaGuid);
  }


  public Guid createGuidFromHash(String guid) throws CastException {
    return apiApp.createGuidFromHashWithOneId(GUID_PREFIX, guid);
  }

  /**
   * Getsert: Get or insert the user
   */
  public Future<Organization> getsert(Long organizationId, OrganizationInputProps organizationInputProps, SqlConnection sqlConnection) {

    return this.getById(organizationId, sqlConnection)
      .recover(t -> Future.failedFuture(new InternalException("Error while selecting the organization", t)))
      .compose(storedOrganization -> {
        if (storedOrganization != null) {
          return Future.succeededFuture(storedOrganization);
        } else {
          return this.insert(organizationId, organizationInputProps, sqlConnection);
        }
      });

  }

  private Future<Organization> insert(Long organizationId, OrganizationInputProps organizationInputProps, SqlConnection sqlConnection) {


    JdbcInsert jdbcInsert = JdbcInsert.into(this.orgaTable);
    Organization organization = new Organization();

    organization.setCreationTime(DateTimeService.getNowInUtc());
    jdbcInsert.addColumn(OrganizationCols.CREATION_TIME, organization.getCreationTime());

    if (organizationId != null) {
      organization.setLocalId(organizationId);
      jdbcInsert.addColumn(OrganizationCols.ID, organizationId);
    }

    if (organizationInputProps.getHandle() != null) {
      organization.setHandle(organizationInputProps.getHandle());
      jdbcInsert.addColumn(OrganizationCols.HANDLE, organization.getHandle().getValue());
    }

    organization.setName(organizationInputProps.getName());
    jdbcInsert.addColumn(OrganizationCols.NAME, organization.getName());

    OrgaUser orgaUser = new OrgaUser();
    orgaUser.setLocalId(organizationInputProps.getOwnerGuid().getLocalId());
    orgaUser.setRealm(this.apiApp.getEraldyModel().getRealm());
    jdbcInsert.addColumn(OrganizationCols.OWNER_ID, orgaUser.getLocalId())
      .addColumn(OrganizationCols.REALM_ID, orgaUser.getRealm().getLocalId());

    return jdbcInsert
      .addReturningColumn(OrganizationCols.ID)
      .execute(sqlConnection)
      .compose(orgRows -> {
        Long orgLocalId = orgRows.iterator().next().getLong(OrganizationCols.ID);

        if (organizationId != null && !organizationId.equals(orgLocalId)) {
          return Future.failedFuture(new InternalException("The asked organization id (" + organizationId + ") was not the one inserted (" + orgLocalId + ")"));
        }
        organization.setLocalId(orgLocalId);
        this.updateGuid(organization);

        return Future.succeededFuture(organization);
      });
  }

  Future<Organization> getById(Long localId, SqlConnection sqlConnection) {
    return getById(localId, Organization.class, sqlConnection);
  }


}
