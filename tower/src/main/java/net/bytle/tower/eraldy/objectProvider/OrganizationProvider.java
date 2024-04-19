package net.bytle.tower.eraldy.objectProvider;

import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.Organization;
import net.bytle.tower.eraldy.module.organization.db.OrganizationCols;
import net.bytle.tower.eraldy.module.organization.inputs.OrganizationInputProps;
import net.bytle.tower.util.Guid;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.db.JdbcInsert;
import net.bytle.vertx.db.JdbcSchema;
import net.bytle.vertx.db.JdbcSchemaManager;
import net.bytle.vertx.db.JdbcTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

import static net.bytle.vertx.db.JdbcSchemaManager.COLUMN_PART_SEP;

public class OrganizationProvider {


  protected static final Logger LOGGER = LoggerFactory.getLogger(OrganizationProvider.class);
  public static final String TABLE_NAME = "organization";

  public static final String QUALIFIED_TABLE_NAME = JdbcSchemaManager.CS_REALM_SCHEMA + "." + TABLE_NAME;

  /**
   * Lower case is important
   */
  private static final String TABLE_PREFIX = "orga";

  public static final String ORGA_ID_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + "id";
  private static final String GUID_PREFIX = "org";
  private final EraldyApiApp apiApp;
  public static final String ORGA_HANDLE_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + "handle";
  @SuppressWarnings("unused")
  private static final String ORGA_MODIFICATION_TIME_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + JdbcSchemaManager.MODIFICATION_TIME_COLUMN_SUFFIX;
  private final Pool jdbcPool;
  private final JdbcTable orgaTable;


  public OrganizationProvider(EraldyApiApp apiApp, JdbcSchema jdbcSchema) {
    this.apiApp = apiApp;
    this.jdbcPool = apiApp.getHttpServer().getServer().getPostgresClient().getPool();
    this.orgaTable = JdbcTable.build(jdbcSchema, TABLE_NAME)
      .addPrimaryKeyColumn(OrganizationCols.ID)
      .build();
  }


  public Future<Organization> getById(Long orgaId) {
    return getById(orgaId, Organization.class);
  }

  @SuppressWarnings("SameParameterValue")
  private <T extends Organization> Future<T> getById(Long orgaId, Class<T> clazz) {

    return jdbcPool.withConnection(sqlConnection -> getById(orgaId, clazz, sqlConnection));
  }

  private <T extends Organization> Future<T> getById(Long orgaId, Class<T> clazz, SqlConnection sqlConnection) {

    String sql = "SELECT * FROM\n" +
      QUALIFIED_TABLE_NAME + "\n" +
      "WHERE " + ORGA_ID_COLUMN + " = $1";
    return sqlConnection.preparedQuery(sql)
      .execute(Tuple.of(orgaId))
      .onFailure(e -> LOGGER.error("Error: " + e.getMessage() + ", while retrieving the realm by id with the sql\n" + sql, e))
      .compose(orgRows -> {

        if (orgRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (orgRows.size() != 1) {
          return Future.failedFuture(new InternalException("the orga id (" + orgaId + ") returns more than one row"));
        }
        Row row = orgRows.iterator().next();
        return this.getOrganizationFromDatabaseRow(row, clazz);
      });
  }

  private <T extends Organization> Future<T> getOrganizationFromDatabaseRow(Row row, Class<T> clazz) {
    String orgaHandle = row.getString(ORGA_HANDLE_COLUMN);
    Long orgaId = row.getLong(ORGA_ID_COLUMN);

    Organization organization = new Organization();
    organization.setLocalId(orgaId);
    organization.setHandle(orgaHandle);
    organization.setGuid(this.computeGuid(organization).toString());

    return Future.succeededFuture(clazz.cast(organization));

  }

  private <T extends Organization> Guid computeGuid(T organization) {
    return apiApp.createGuidFromObjectId(GUID_PREFIX, organization.getLocalId());
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


    LocalDateTime nowInUtc = DateTimeService.getNowInUtc();
    return JdbcInsert.into(this.orgaTable)
      .addColumn(OrganizationCols.CREATION_TIME, nowInUtc)
      .addColumn(OrganizationCols.ID, organizationId)
      .addColumn(OrganizationCols.HANDLE, organizationInputProps.getHandle())
      .addColumn(OrganizationCols.NAME, organizationInputProps.getName())
      .addReturningColumn(OrganizationCols.ID)
      .execute(sqlConnection)
      .compose(orgRows -> {
        Long orgLocalId = orgRows.iterator().next().getLong(OrganizationCols.ID);
        Organization organization = new Organization();
        organization.setLocalId(orgLocalId);
        this.computeGuid(organization);
        organization.setName(organizationInputProps.getName());
        organization.setHandle(organizationInputProps.getHandle());
        organization.setCreationTime(nowInUtc);
        return Future.succeededFuture(organization);
      });
  }

  Future<Organization> getById(Long localId, SqlConnection sqlConnection) {
    return getById(localId, Organization.class, sqlConnection);
  }


}
