package net.bytle.tower.eraldy.api;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import net.bytle.vertx.DateTimeUtil;

public class OrganizationRoleProvider {
  public static final int OWNER_ROLE_ID = 1;
  @SuppressWarnings({"FieldCanBeLocal", "unused"})
  private final EraldyApiApp apiApp;

  public OrganizationRoleProvider(EraldyApiApp eraldyApiApp) {
    this.apiApp = eraldyApiApp;
  }

  public Future<Void> upsertAll(SqlConnection sqlConnection) {
    /**
     * For now, there is only one role
     */
    String selectSql = "select * from cs_realms.organization_role where orga_role_id = $1";
    Tuple selectTuple = Tuple.of(OWNER_ROLE_ID);
    return sqlConnection
      .preparedQuery(selectSql)
      .execute(selectTuple)
      .compose(selectRows -> {
        if (selectRows.size() == 1) {
          return Future.succeededFuture();
        }
        String insertSql = "insert into cs_realms.organization_role(orga_role_id, orga_role_name, orga_role_creation_time) values ($1, $2, $3)";
        Tuple insertTuple = Tuple.of(OWNER_ROLE_ID, "Owner", DateTimeUtil.getNowInUtc());
        return sqlConnection
          .preparedQuery(insertSql)
          .execute(insertTuple)
          .compose(v -> Future.succeededFuture());
      });
  }

}
