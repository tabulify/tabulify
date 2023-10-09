package net.bytle.tower.eraldy.objectProvider;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.Tuple;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.model.openapi.Organization;
import net.bytle.tower.util.Guid;
import net.bytle.tower.util.JdbcPoolCs;
import net.bytle.tower.util.JdbcSchemaManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.bytle.tower.util.JdbcSchemaManager.COLUMN_PART_SEP;

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
  private static OrganizationProvider organizationProvider;
  private final Vertx vertx;
  public static final String ORGA_NAME_COLUMN = TABLE_PREFIX + COLUMN_PART_SEP + "name";

  public OrganizationProvider(Vertx vertx) {
    this.vertx = vertx;
  }

  public static OrganizationProvider createFrom(Vertx vertx) {

    if (organizationProvider != null) {
      return organizationProvider;
    }
    organizationProvider = new OrganizationProvider(vertx);

    return organizationProvider;
  }

  public Organization toPublicClone(Organization organization) {
    Organization organizationClone = JsonObject.mapFrom(organization).mapTo(Organization.class);
    organizationClone.setId(null);
    return organizationClone;
  }

  public Future<Organization> getById(Long orgaId) {
    return getById(orgaId,Organization.class);
  }
  private <T extends Organization> Future<T> getById(Long orgaId, Class<T> clazz) {

    PgPool jdbcPool = JdbcPoolCs.getJdbcPool(this.vertx);
    String sql = "SELECT * FROM\n" +
      QUALIFIED_TABLE_NAME + "\n" +
      "WHERE " + ORGA_ID_COLUMN + " = $1";
    return jdbcPool.preparedQuery(sql)
      .execute(Tuple.of(orgaId))
      .onFailure(e -> LOGGER.error("Error: " + e.getMessage() + ", while retrieving the realm by id with the sql\n" + sql, e))
      .compose(orgRows -> {

        if (orgRows.size() == 0) {
          return Future.succeededFuture();
        }

        if (orgRows.size() != 1) {
          return Future.failedFuture(new InternalException("the orga id (" + orgaId + ") returns  more than one row"));
        }
        Row row = orgRows.iterator().next();
        return this.getOrganizationFromDatabaseRow(row, clazz);
      });
  }

  private <T extends Organization> Future<T> getOrganizationFromDatabaseRow(Row row, Class<T> clazz) {
    String orgaName = row.getString(ORGA_NAME_COLUMN);
    Long orgaId = row.getLong(ORGA_ID_COLUMN);

    Organization organization = new Organization();
    organization.setId(orgaId);
    organization.setName(orgaName);
    organization.setGuid(this.computeGuid(organization));

    return Future.succeededFuture(clazz.cast(organization));

  }

  private <T extends Organization> String computeGuid(T organization) {
    return Guid.createGuidStringFromObjectId(GUID_PREFIX, organization.getLocalId(), vertx);
  }
}
