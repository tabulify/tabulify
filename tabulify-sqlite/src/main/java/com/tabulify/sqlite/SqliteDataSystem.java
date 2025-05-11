package com.tabulify.sqlite;

import com.tabulify.jdbc.SqlDataPath;
import com.tabulify.jdbc.SqlDataSystem;
import com.tabulify.jdbc.SqlMediaType;
import com.tabulify.jdbc.SqlMetaDataType;
import com.tabulify.model.*;
import com.tabulify.spi.DataPath;
import com.tabulify.transfer.TransferSourceTarget;
import net.bytle.exception.InternalException;
import net.bytle.type.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SqliteDataSystem extends SqlDataSystem {

  private static final Logger LOGGER = LoggerFactory.getLogger(SqliteDataSystem.class);

  /*
   * The driver returns the system tables
   * This is not the behavior of sqlite3, we make it for now default
   */
  private final boolean filterSystemTables = true;


  public SqliteDataSystem(SqliteConnection sqliteDataStore) {
    super(sqliteDataStore);
  }


  @Override
  public Boolean exists(DataPath dataPath) {
    if (!(dataPath instanceof SqliteDataPath)) {
      throw new InternalException("This data path is not a sqlite data path. " + dataPath);
    }
    SqliteDataPath sqliteDataPath = (SqliteDataPath) dataPath;
    if (sqliteDataPath.getMediaType() == SqlMediaType.SCHEMA) {
      return true;
    }
    return super.exists(dataPath);
  }


  /**
   * Returns statement to create the table
   * SQLlite has limited support on the alter statement
   * The primary key shoud be in the create statement.
   * See <a href="https://sqlite.org/faq.html#q11">...</a>
   *
   * @param dataPath the data path source
   * @return a create statement <a href="https://www.sqlite.org/lang_createtable.html">lang_createtable</a>
   */
  @Override
  public String createTableStatement(SqlDataPath dataPath) {

    StringBuilder statement = new StringBuilder();
    statement.append("create table ").append(dataPath.toSqlStringPath()).append(" (\n");
    RelationDef tableDef = dataPath.getOrCreateRelationDef();
    if (tableDef == null) {
      throw new RuntimeException("The dataPath (" + dataPath + ") has no columns definitions. We can't create a table from then");
    }
    for (int i = 1; i <= tableDef.getColumnsSize(); i++) {
      ColumnDef columnDef = tableDef.getColumnDef(i);
      statement.append(createColumnStatement(columnDef));
      if (i != tableDef.getColumnsSize()) {
        statement.append(",\n");
      }
    }

    // Pk
    final PrimaryKeyDef primaryKey = tableDef.getPrimaryKey();
    if (tableDef.getPrimaryKey() != null) {
      statement.append(",\nprimary key (");
      for (int i = 1; i <= primaryKey.getColumns().size(); i++) {
        ColumnDef columnDef = primaryKey.getColumns().get(i - 1);
        statement.append(createQuotedName(columnDef.getColumnName()));
        if (i < primaryKey.getColumns().size()) {
          statement.append(", ");
        }
      }
      statement.append(")");
    }

    // Fk
    // http://www.hwaci.com/sw/sqlite/foreignkeys.html
    // The parent table is the table that a foreign key constraint refers to.
    // The child table is the table that a foreign key constraint is applied to and the table that contains the REFERENCES clause.
    //        CREATE TABLE song(
    //                songid     INTEGER,
    //                songartist TEXT,
    //                songalbum TEXT,
    //                songname   TEXT,
    //                FOREIGN KEY(songartist, songalbum) REFERENCES album(albumartist, albumname)
    //        );
    final List<ForeignKeyDef> foreignKeyDefs = tableDef.getForeignKeys();
    Collections.sort(foreignKeyDefs);
    for (ForeignKeyDef foreignKeyDef : foreignKeyDefs) {

      statement.append(",\nforeign key (");

      // Child columns
      List<String> childColumns = foreignKeyDef.getChildColumns().stream()
        .map(s -> createQuotedName(s.getColumnName()))
        .collect(Collectors.toList());
      statement.append(String.join(",", childColumns));

      statement.append(") references ");
      statement.append(foreignKeyDef.getForeignPrimaryKey().getRelationDef().getDataPath().getName());
      statement.append("(");

      // Foreign / Parent  columns
      List<String> parentColumns = foreignKeyDef.getForeignPrimaryKey().getColumns()
        .stream()
        .map(s -> createQuotedName(s.getColumnName()))
        .collect(Collectors.toList());
      statement.append(String.join(",", parentColumns));
      statement.append(")");

    }

    // Uk
    // Syntax in the table constraint clause
    // https://sqlite.org/lang_createtable.html
    tableDef.getUniqueKeys().forEach(uk -> {
      assert !uk.getColumns().isEmpty() : "The unique key (" + uk + ") of the table (" + tableDef.getDataPath() + ") has no columns";
      statement.append(",\nunique (");
      for (int i = 1; i <= uk.getColumns().size(); i++) {
        ColumnDef columnDef = uk.getColumns().get(i - 1);
        statement.append(createQuotedName(columnDef.getColumnName()));
        if (i < uk.getColumns().size()) {
          statement.append(", ");
        }
      }
      statement.append(")");

    });

    // End statement
    statement.append("\n)");

    return statement.toString();

  }

  /**
   * Already include in the {@link #createTableStatement(SqlDataPath)}
   * SQLite does not support in its alter statement the creation of a primary key. See <a href="https://sqlite.org/lang_altertable.html">alter</a>
   */
  @Override
  protected String createPrimaryKeyStatement(SqlDataPath jdbcDataPath) {
    return null;
  }

  /**
   * Already include in the {@link #createTableStatement(SqlDataPath)}
   * SQLite does not support in its alter statement the creation of a primary key. See <a href="https://sqlite.org/lang_altertable.html">...</a>
   */
  @Override
  protected String createUniqueKeyStatement(UniqueKeyDef uniqueKeyDef) {
    return null;
  }

  /**
   * Already include in the {@link #createTableStatement(SqlDataPath)}
   * SQLite does not support in its alter statement the creation of a primary key. See <a href="https://sqlite.org/lang_altertable.html">...</a>
   */
  @Override
  protected String createForeignKeyStatement(ForeignKeyDef foreignKeyDef) {
    return null;
  }

  /**
   * <a href="https://sqlite.org/lang_delete.html#the_truncate_optimization">...</a>
   */
  @Override
  public List<String> createTruncateStatement(List<SqlDataPath> dataPaths) {

    List<String> statements = new ArrayList<>();
    dataPaths.forEach(dp -> {
      String truncateStatementBuilder = "delete from " +
        dp.toSqlStringPath();
      statements.add(truncateStatementBuilder);
    });

    return statements;

  }


  @Override
  public List<SqlDataPath> getChildrenDataPath(DataPath dataPath) {
    /**
     * The sqlite driver returns also
     * the sys tables such as:
     * * sqlite_schema
     * * sqlite_autoindex_xxx_1@sqlite
     * We filter them out
     */
    return super.getChildrenDataPath(dataPath)
      .stream()
      .filter(d -> {
        if (this.filterSystemTables) {
          return !d.getLogicalName().startsWith("sqlite");
        }
        return true;
      })
      .collect(Collectors.toList());
  }

  @Override
  public void dropConstraint(Constraint constraint) {

    StringBuilder msg = new StringBuilder();
    if (constraint instanceof ForeignKeyDef) {
      msg.append("The dropping of a foreign key was passed. ");

    } else if (constraint instanceof PrimaryKeyDef) {
      msg.append("The dropping of a primary key was passed. ");

    } else if (constraint instanceof UniqueKeyDef) {
      msg.append("The dropping of a unique key was passed. ");
    }
    LOGGER.warn(
      msg
        .append("Sqlite does not support the drop of constraints in its ALTER statement. ")
        .append("See https://sqlite.org/lang_altertable.html ")
        .append("You need to create another data resource structure and copy the data. ")
        .toString()
    );
  }

  @Override
  public Map<Integer, SqlMetaDataType> getMetaDataTypes() {

    Map<Integer, SqlMetaDataType> sqlMetaDataType = super.getMetaDataTypes();

    /**
     * Text data type
     * From https://sqlite.org/datatype3.html
     * If the declared type of the column contains any of the strings "CHAR", "CLOB", or "TEXT"
     * then that column has TEXT affinity.
     * Notice that the type VARCHAR contains the string "CHAR" and is thus assigned TEXT affinity.
     */
    sqlMetaDataType.computeIfAbsent(Types.CHAR, SqlMetaDataType::new)
      .setSqlName("text")
      .setMaxPrecision(SqliteType.MAX_LENGTH);

    sqlMetaDataType.computeIfAbsent(Types.NCHAR, SqlMetaDataType::new)
      .setSqlName("text")
      .setMaxPrecision(SqliteType.MAX_LENGTH);

    sqlMetaDataType.computeIfAbsent(Types.VARCHAR, SqlMetaDataType::new)
      .setSqlName("text")
      .setMaxPrecision(SqliteType.MAX_LENGTH);

    sqlMetaDataType.computeIfAbsent(Types.NVARCHAR, SqlMetaDataType::new)
      .setSqlName("text")
      .setMaxPrecision(SqliteType.MAX_LENGTH);

    sqlMetaDataType.computeIfAbsent(Types.CLOB, SqlMetaDataType::new)
      .setSqlName("text")
      .setMaxPrecision(SqliteType.MAX_LENGTH);

    /**
     * https://sqlite.org/datatype3.html
     * If the declared type contains the string "INT" then it is assigned INTEGER affinity.
     */
    sqlMetaDataType.computeIfAbsent(Types.INTEGER, SqlMetaDataType::new)
      .setSqlName("integer");

    /**
     * https://sqlite.org/datatype3.html
     * If the declared type contains the string "INT" then it is assigned INTEGER affinity.
     */
    sqlMetaDataType.computeIfAbsent(Types.BIGINT, SqlMetaDataType::new)
      .setSqlName("bigint");

    /**
     *
     * SQLite 3 database has only the NUMERIC data type for fixed point
     * DECIMAL(10,5) is NUMERIC
     * See https://sqlite.org/datatype3.html
     */
    sqlMetaDataType.computeIfAbsent(Types.DECIMAL, SqlMetaDataType::new)
      .setSqlName("numeric")
      .setMaxPrecision(SqliteType.MAX_NUMERIC_PRECISION)
      .setMinimumScale(0)
      .setMaximumScale(SqliteType.MAX_NUMERIC_PRECISION);
    sqlMetaDataType.computeIfAbsent(Types.NUMERIC, SqlMetaDataType::new)
      .setSqlName("numeric")
      .setMaxPrecision(SqliteType.MAX_NUMERIC_PRECISION)
      .setMinimumScale(0)
      .setMaximumScale(SqliteType.MAX_NUMERIC_PRECISION);

    /**
     * https://sqlite.org/datatype3.html
     * SQlite knows only real for floating point
     */
    sqlMetaDataType.computeIfAbsent(Types.DOUBLE, SqlMetaDataType::new)
      .setSqlName("real");

    sqlMetaDataType.computeIfAbsent(Types.FLOAT, SqlMetaDataType::new)
      .setSqlName("real");

    sqlMetaDataType.computeIfAbsent(Types.REAL, SqlMetaDataType::new)
      .setSqlName("real");

    /**
     * SQLXML
     */
    sqlMetaDataType.computeIfAbsent(Types.SQLXML, SqlMetaDataType::new)
      .setSqlName("sqlxml")
      .setSqlJavaClazz(String.class);

    return sqlMetaDataType;

  }

  @Override
  public void drop(DataPath dataPath) {
    SqlDataPath sqlDataPath = (SqlDataPath) dataPath;

    if (sqlDataPath.getMediaType() == SqlMediaType.TABLE) {
      /**
       * Sqlite will still drop the table even if there is a foreign key that references it
       */
      List<ForeignKeyDef> foreignKeysThatReference = super.getForeignKeysThatReference(sqlDataPath);
      if (!foreignKeysThatReference.isEmpty()) {
        throw new RuntimeException("The table (" + dataPath + ") is referenced by the following tables " + foreignKeysThatReference.stream().map(fk -> fk.getRelationDef().getDataPath().getName()).collect(Collectors.joining(", ")) + " and can't therefore be dropped");
      }

    }
    super.drop(dataPath);

  }

  /**
   * Sqlite will still drop the table even if there is a foreign key that references it
   * There is no integrity
   */
  @Override
  public void dropForce(DataPath dataPath) {
    super.drop(dataPath);
  }

  /**
   * Insert On conflict cause
   */
  @Override
  public String createUpsertStatementWithSelect(TransferSourceTarget transferSourceTarget) {

    StringBuilder statement = new StringBuilder();
    String insertStatementWithSelect = createInsertStatementWithSelect(transferSourceTarget);
    statement.append(insertStatementWithSelect);

    // https://sqlite.org/lang_upsert.html See `2.1 Parsing Ambiguity`
    String insertStatementWithSelectLowercase = insertStatementWithSelect.toLowerCase();
    if (!insertStatementWithSelectLowercase.contains("where")
      && !insertStatementWithSelectLowercase.contains("order")
      && !insertStatementWithSelectLowercase.contains("group")
      && !insertStatementWithSelectLowercase.contains("having")
    ) {
      statement.append(" where true");
    }

    statement.append(" ")
      .append(createUpsertStatementUtilityOnConflict(transferSourceTarget));

    return statement.toString();


  }

  @Override
  public List<SqlDataPath> select(DataPath dataPath, String globNameOrPath, MediaType mediaType) {
    return super.select(dataPath, globNameOrPath, mediaType)
      .stream()
      .filter(d -> {
          if (this.filterSystemTables) {
            return !d.getLogicalName().startsWith("sqlite");
          }
          return true;
        }
      )
      .collect(Collectors.toList());
  }
}
