package net.bytle.tower.eraldy.module.organization.db;

import io.vertx.core.Future;
import io.vertx.sqlclient.SqlConnection;
import net.bytle.vertx.DateTimeService;
import net.bytle.vertx.db.JdbcInsert;
import net.bytle.vertx.db.JdbcOnConflictAction;
import net.bytle.vertx.db.JdbcSchema;
import net.bytle.vertx.db.JdbcTable;

import java.time.LocalDateTime;

public class OrganizationRoleProvider {
  public static final int OWNER_ROLE_ID = 1;
  private final JdbcTable orgRoleTable;

  public OrganizationRoleProvider(JdbcSchema jdbcSchema) {

    this.orgRoleTable = JdbcTable.build(jdbcSchema, "organization_role", OrganizationRoleCols.values())
      .addPrimaryKeyColumn(OrganizationRoleCols.ID)
      .build();
  }

  public Future<Void> upsertAll(SqlConnection sqlConnection) {
    /**
     * For now, there is only one role
     */
    LocalDateTime nowInUtc = DateTimeService.getNowInUtc();
    return JdbcInsert.into(orgRoleTable)
      .addColumn(OrganizationRoleCols.ID, OWNER_ROLE_ID)
      .addColumn(OrganizationRoleCols.NAME, "Owner")
      .addColumn(OrganizationRoleCols.CREATION_TIME, nowInUtc)
      .addColumn(OrganizationRoleCols.MODIFICATION_TIME, nowInUtc)
      .onConflictPrimaryKey(JdbcOnConflictAction.DO_NOTHING)
      .execute(sqlConnection)
      .compose(selectRows -> Future.succeededFuture());
  }

}
