package com.tabulify.fs.sql;

import com.tabulify.exception.CastException;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;

import java.util.Arrays;
import java.util.List;

import static com.tabulify.fs.sql.SqlSubset.PLSQL;

/**
 * The First Sql Words of a statement
 * Note we may get them via <a href="https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getSQLKeywords()">...</a>
 */
public enum SqlStatementKind {


  ANALYZE(SqlSubset.STATISTICS, SqlTokenCategory.SQL),
  ALTER(SqlSubset.DDL, SqlTokenCategory.SQL),
  AUDIT(SqlSubset.AUDIT, SqlTokenCategory.SQL),
  CALL(SqlSubset.PROCEDURE, SqlTokenCategory.PSQL),
  COMMIT(SqlSubset.TRANSACTION, SqlTokenCategory.SQL),
  COMMENT(SqlSubset.COMMENT, SqlTokenCategory.SQL),
  CREATE(SqlSubset.DDL, SqlTokenCategory.SQL),
  /**
   * <a href="https://www.postgresql.org/docs/current/ecpg-sql-describe.html">...</a>
   */
  DESCRIBE(SqlSubset.DML, SqlTokenCategory.SQL),
  DELETE(SqlSubset.DML, SqlTokenCategory.SQL),
  // DO Postgres
  // There is no DO statement in the SQL standard.
  // https://www.postgresql.org/docs/current/sql-do.html
  DO(PLSQL, SqlTokenCategory.PSQL),
  // MySQL
  // https://dev.mysql.com/doc/refman/8.4/en/stored-programs-defining.html
  DELIMITER(SqlSubset.CLI, SqlTokenCategory.COMMAND),
  DISASSOCIATE(SqlSubset.STATISTICS, SqlTokenCategory.SQL),
  DROP(SqlSubset.DML, SqlTokenCategory.SQL),
  EXPLAIN(SqlSubset.EXPLAIN, SqlTokenCategory.SQL),
  FLASHBACK(SqlSubset.LOG, SqlTokenCategory.SQL),
  GRANT(SqlSubset.DCL, SqlTokenCategory.SQL),
  INSERT(SqlSubset.DML, SqlTokenCategory.SQL),
  LOCK(SqlSubset.TRANSACTION, SqlTokenCategory.SQL),
  MERGE(SqlSubset.DML, SqlTokenCategory.SQL),
  NO_AUDIT(SqlSubset.AUDIT, SqlTokenCategory.SQL),
  // Pragma: compiler, hint, ...
  // https://www.sqlite.org/pragma.html
  // Can return data but can also set property
  PRAGMA(SqlSubset.COMMAND, SqlTokenCategory.SQL),
  PURGE(SqlSubset.DML, SqlTokenCategory.SQL),
  RENAME(SqlSubset.DDL, SqlTokenCategory.SQL),
  REVOKE(SqlSubset.DCL, SqlTokenCategory.SQL),
  ROLLBACK(SqlSubset.TRANSACTION, SqlTokenCategory.SQL),
  SAVEPOINT(SqlSubset.LOG, SqlTokenCategory.SQL),
  SELECT(SqlSubset.DML, SqlTokenCategory.SQL),
  /**
   * MySQL,<a href="https://dev.mysql.com/doc/refman/8.4/en/set-statement.html">...</a>
   */
  SET(SqlSubset.CLI, SqlTokenCategory.COMMAND),
  TRUNCATE(SqlSubset.DML, SqlTokenCategory.SQL),
  UPDATE(SqlSubset.DML, SqlTokenCategory.SQL),
  // https://dev.mysql.com/doc/refman/8.4/en/use.html
  USE(SqlSubset.COMMAND, SqlTokenCategory.SQL),
  WITH(SqlSubset.DML, SqlTokenCategory.SQL),
  DECLARE(SqlSubset.PLSQL, SqlTokenCategory.PSQL),
  /**
   * SQL Server anonymous block starts with begin
   * <a href="https://learn.microsoft.com/en-us/sql/t-sql/language-elements/begin-end-transact-sql">...</a>
   */
  BEGIN(SqlSubset.PLSQL, SqlTokenCategory.PSQL),
  SCRIPT_COMMENT(SqlSubset.SCRIPT_COMMENT, SqlTokenCategory.COMMENT),
  UNKNOWN(SqlSubset.UNKNOWN, SqlTokenCategory.UNKNOWN),
  /**
   * The following stored procedure sql statement will make Sql plus
   * to enter in PLSQL Mode
   */
  CREATE_FUNCTION(PLSQL, SqlTokenCategory.PSQL),
  CREATE_LIBRARY(PLSQL, SqlTokenCategory.PSQL),
  CREATE_PACKAGE(PLSQL, SqlTokenCategory.PSQL),
  CREATE_PROCEDURE(PLSQL, SqlTokenCategory.PSQL),
  CREATE_TRIGGER(PLSQL, SqlTokenCategory.PSQL),
  CREATE_TYPE(PLSQL, SqlTokenCategory.PSQL),
  /**
   * 4 words PSQL mode
   */
  CREATE_OR_REPLACE_FUNCTION(PLSQL, SqlTokenCategory.PSQL),
  CREATE_OR_REPLACE_LIBRARY(PLSQL, SqlTokenCategory.PSQL),
  CREATE_OR_REPLACE_PACKAGE(PLSQL, SqlTokenCategory.PSQL),
  CREATE_OR_REPLACE_PROCEDURE(PLSQL, SqlTokenCategory.PSQL),
  CREATE_OR_REPLACE_TRIGGER(PLSQL, SqlTokenCategory.PSQL),
  CREATE_OR_REPLACE_TYPE(PLSQL, SqlTokenCategory.PSQL);

  static final List<Integer> KIND_LENGTH_IN_DESC_ORDER = Arrays.asList(4, 2, 1);

  private final SqlTokenCategory sqlTokenCategory;

  public static SqlStatementKind cast(List<String> words) {

    for (int kindLength : KIND_LENGTH_IN_DESC_ORDER) {
      if (words.size() < kindLength) {
        continue;
      }
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < kindLength; i++) {
        builder.append(words.get(i)).append("_");
      }
      KeyNormalizer key;
      try {
        key = KeyNormalizer.create(builder.toString());
      } catch (CastException e) {
        // Case of `?_=_` when length is 2 in the statement `? = call upper( ? )`
        continue;
      }
      try {
        return Casts.cast(key, SqlStatementKind.class);
      } catch (CastException e) {
        // i words statement not detected
      }

    }
    return UNKNOWN;

  }

  public SqlSubset getSqlSubset() {
    return sqlSubset;
  }


  public SqlTokenCategory getSqlTokenCategory() {
    return sqlTokenCategory;
  }

  private final SqlSubset sqlSubset;

  SqlStatementKind(SqlSubset sqlSubset, SqlTokenCategory sqlTokenCategory) {
    this.sqlSubset = sqlSubset;
    this.sqlTokenCategory = sqlTokenCategory;
  }

  /**
   * @return if the statement is a select statement usable in a view or as sub-block in a select
   */
  public boolean isSelect() {
    /**
     * query can be other thing than a select
     * Example: `PRAGMA table_info(f_sales)`
     */
    return this == WITH || this == SELECT || this == PRAGMA;
  }

}
