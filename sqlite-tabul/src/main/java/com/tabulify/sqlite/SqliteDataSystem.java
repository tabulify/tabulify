package com.tabulify.sqlite;

import com.tabulify.jdbc.*;
import com.tabulify.model.*;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.DropTruncateAttribute;
import com.tabulify.spi.Tabulars;
import com.tabulify.transfer.TransferSourceTargetOrder;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.type.Casts;
import com.tabulify.type.MediaType;

import java.math.BigDecimal;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SqliteDataSystem extends SqlDataSystem {

  private static final Logger LOGGER = Logger.getLogger("tabulify.sqlite");
  // In sqlite, precision and scale are just hint, they are optional
  // but for our type system, we need them
  // as we check that are not 0
  public static final int MAX_PRECISION_OR_SCALE = 100;

  /*
   * The driver returns the system tables
   * This is not the behavior of sqlite3, we make it for now default
   */
  private final boolean filterSystemTables = true;


  public SqliteDataSystem(SqliteConnection sqliteDataStore) {
    super(sqliteDataStore);
  }

  @Override
  protected String dropConstraintStatement(Constraint constraint) {
    throw new UnsupportedOperationException("SQLite does not support to drop any constraints in its alter statement. https://www.sqlite.org/lang_altertable.html");
  }

  @Override
  public Boolean exists(DataPath dataPath) {
    if (dataPath instanceof SqlRequest) {
      return Tabulars.exists(dataPath.getExecutableDataPath());
    }
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
   * SQLite has limited support on the alter statement
   * The primary key should be in the `create` statement.
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
      ColumnDef<?> columnDef = tableDef.getColumnDef(i);
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

    throw new UnsupportedOperationException("SQLite does not support the creation of primary key via the alter statement. https://sqlite.org/lang_altertable.html");

  }

  /**
   * Already include in the {@link #createTableStatement(SqlDataPath)}
   * SQLite does not support in its alter statement the creation of a primary key. See <a href="https://sqlite.org/lang_altertable.html">...</a>
   */
  @Override
  protected String createUniqueKeyStatement(UniqueKeyDef uniqueKeyDef) {

    throw new UnsupportedOperationException("SQLite does not support the creation of unique key via the alter statement. https://sqlite.org/lang_altertable.html");

  }

  /**
   * Already include in the {@link #createTableStatement(SqlDataPath)}
   * SQLite does not support in its alter statement the creation of a primary key. See <a href="https://sqlite.org/lang_altertable.html">...</a>
   */
  @Override
  protected String createForeignKeyStatement(ForeignKeyDef foreignKeyDef) {
    throw new UnsupportedOperationException("SQLite does not support the creation of foreign key via the alter statement. https://sqlite.org/lang_altertable.html");
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
    LOGGER.warning(
      msg
        .append("Sqlite does not support the drop of constraints in its ALTER statement. ")
        .append("See https://sqlite.org/lang_altertable.html ")
        .append("You need to create another data resource structure and copy the data. ")
        .toString()
    );
  }

  public SqliteConnection getConnection() {
    return (SqliteConnection) sqlConnection;
  }

  @Override
  public void dataTypeBuildingMain(SqlDataTypeManager sqlDataTypeManager) {


    super.dataTypeBuildingMain(sqlDataTypeManager);

    List<SqlDataTypeVendor> sqlDataTypeVendors = new ArrayList<>();
    // The numeric affinity is not in the driver
    sqlDataTypeVendors.add(SqliteTypeAffinity.NUMERIC);

    if (this.getConnection().getAffinityConversion()) {

      /**
       * Text data type.
       * They finally get an affinity text
       * All syntax can be used (VARCHAR, ...) but they become text
       * See https://www.sqlite.org/datatype3.html
       * If the declared type of the column contains any of the strings "CHAR", "CLOB", or "TEXT"
       * then that column has TEXT affinity.
       * Notice that the type VARCHAR contains the string "CHAR" and is thus assigned TEXT affinity.
       * <p>
       * SQLXML and JSON Not supported natively
       * but because of the affinity system we can take them as TEXT
       */
      sqlDataTypeManager.createTypeBuilder(SqlDataTypeAnsi.CHARACTER_VARYING)
        .setMaxPrecision(SqliteTypeParser.MAX_LENGTH)
        .addChildAliasTypedName(SqlDataTypeAnsi.CHARACTER, sqlDataTypeManager)
        .addChildAliases(SqlDataTypeAnsi.CHARACTER.getAliases())
        .addChildAliasTypedName(SqlDataTypeAnsi.NATIONAL_CHARACTER, sqlDataTypeManager)
        .addChildAliases(SqlDataTypeAnsi.NATIONAL_CHARACTER.getAliases())
        .addChildAliasTypedName(SqlDataTypeAnsi.NATIONAL_CHARACTER_VARYING, sqlDataTypeManager)
        .addChildAliases(SqlDataTypeAnsi.NATIONAL_CHARACTER_VARYING.getAliases())
        .addChildAliasCommonNames();

      /**
       * Integer is the top type
       * https://sqlite.org/datatype3.html
       * If the declared type contains the string "INT" then it is assigned INTEGER affinity.
       */
      sqlDataTypeManager.createTypeBuilder(SqlDataTypeAnsi.INTEGER)
        .setMaxPrecision(SqlDataTypeManager.INTEGER_SIGNED_MAX_LENGTH)
        .addChildAliasCommonNames()
        .addChildAliasName(SqlDataTypeCommon.MEDIUMINT)
        .addChildAliasName(SqlDataTypeCommon.INT3)
        .addChildAliasTypedName(SqlDataTypeAnsi.BIGINT, sqlDataTypeManager)
        .addChildAliases(SqlDataTypeAnsi.BIGINT.getAliases())
        .addChildAliasTypedName(SqlDataTypeAnsi.SMALLINT, sqlDataTypeManager)
        .addChildAliases(SqlDataTypeAnsi.SMALLINT.getAliases())
        .addChildAliasTypedName(SqlDataTypeAnsi.TINYINT, sqlDataTypeManager)
        .addChildAliases(SqlDataTypeAnsi.TINYINT.getAliases());

      /**
       * For number, sqlite knows only real
       * Sqlite Real is not Sql real. It's a Sql double
       * https://sqlite.org/datatype3.html
       * Thanks to affinity, we can still keep the sql name original
       * Real is a double
       */
      sqlDataTypeManager.getTypeBuilder(SqliteTypeAffinity.REAL)
        .addChildAliasCommonNames()
        .addChildAliasTypedName(SqlDataTypeAnsi.FLOAT, sqlDataTypeManager);
      sqlDataTypeManager.getTypeBuilder(SqliteTypeAffinity.NUMERIC)
        .addChildAliasTypedName(SqlDataTypeAnsi.DECIMAL, sqlDataTypeManager);

    } else {

      sqlDataTypeVendors.addAll(Arrays.asList(SqliteTypeAnsi.values()));

    }

    for (SqlDataTypeVendor sqlTypeAnsi : sqlDataTypeVendors) {
      sqlDataTypeManager.createTypeBuilder(sqlTypeAnsi.toKeyNormalizer(), sqlTypeAnsi.getVendorTypeNumber(), sqlTypeAnsi.getValueClass())
        .setMaxPrecision(sqlTypeAnsi.getMaxPrecision())
        .setMaximumScale(sqlTypeAnsi.getMaximumScale())
        .setDescription(sqlTypeAnsi.getDescription())
        .addChildAliases(sqlTypeAnsi.getAliases());
    }

    /**
     * Real is a float4 normally, but in Sqlite, it's a double (float8)
     */
    sqlDataTypeManager
      .addJavaClassToTypeRelation(Float.class, SqliteTypeAffinity.REAL)
      .addJavaClassToTypeRelation(BigDecimal.class, SqliteTypeAffinity.NUMERIC)
      .addJavaClassToTypeRelation(Double.class, SqliteTypeAffinity.REAL)
      // a double is a real in sqlite
      .addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi.DOUBLE_PRECISION, SqliteTypeAffinity.REAL)
      .addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi.REAL, SqliteTypeAffinity.REAL)
      .addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi.FLOAT, SqliteTypeAnsi.FLOAT)
      .addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi.CLOB, SqliteTypeAffinity.TEXT)
    ;


  }


  /**
   * Insert On conflict cause
   */
  @Override
  public String createUpsertStatementWithSelect(TransferSourceTargetOrder transferSourceTarget) {

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

  @Override
  protected List<String> createDropStatement(List<SqlDataPath> sqlDataPaths, Set<DropTruncateAttribute> dropAttributes) {
    SqlMediaType enumObjectType = sqlDataPaths.get(0).getMediaType();
    return SqlDropStatement.builder()
      .setType(enumObjectType)
      .setIsCascadeSupported(false)
      .setIfExistsSupported(true)
      .setMultipleSqlObjectSupported(false)
      .build()
      .getStatements(sqlDataPaths, dropAttributes);
  }

  @Override
  public SqlTypeKeyUniqueIdentifier getSqlTypeKeyUniqueIdentifier() {
    return SqlTypeKeyUniqueIdentifier.NAME_ONLY;
  }

  @Override
  public Set<SqlDataTypeVendor> getSqlDataTypeVendors() {
    return Set.of(SqliteTypeAffinity.values());
  }

  @Override
  public Long getSize(DataPath dataPath) {

    SqlDataPath sqlDataPath = (SqlDataPath) dataPath;
    /**
     * Table only for now
     */
    if (!
      (
        sqlDataPath.getMediaType() == SqlMediaType.TABLE ||
          sqlDataPath.getMediaType() == SqlMediaType.SYSTEM_TABLE
      )) {
      return -1L;
    }
    // https://stackoverflow.com/questions/27572387/query-that-returns-the-size-of-a-table-in-a-sqlite-database
    SqlRequest sqlRequest = SqlRequest.builder()
      .setSql(this.getConnection(), "SELECT SUM(\"pgsize\") FROM \"dbstat\" WHERE name='" + sqlDataPath.toSqlStringPath() + "'")
      .build();
    List<List<?>> records = sqlRequest.execute().getRecords();
    if (records.isEmpty()) {
      // may not exist
      return -1L;
    }
    /**
     * {@link SqlDataTypeAnsi#BIGINT} ie Long
     */
    Object sizeAsObject = records.get(0).get(0);
    try {
      return Casts.cast(sizeAsObject, Long.class);
    } catch (CastException e) {
      throw new InternalException("The returned size of the resource (" + dataPath + ") could not be cast to a long. Error:" + e.getMessage(), e);
    }
  }

  @Override
  public String createUpsertMergeStatementWithParameters(TransferSourceTargetOrder transferSourceTarget) {
    return createUpsertStatementUtilityValuesPartBefore(transferSourceTarget) +
      createInsertStatementUtilityValuesClauseGenerator(transferSourceTarget, true, false) +
      createUpsertStatementUtilityValuesPartAfter(transferSourceTarget);
  }

  /**
   * Create upsert from values statement
   */
  @Override
  public String createUpsertMergeStatementWithPrintfExpressions(TransferSourceTargetOrder transferSourceTarget) {
    return createUpsertStatementUtilityValuesPartBefore(transferSourceTarget) +
      createInsertStatementUtilityValuesClauseGenerator(transferSourceTarget, false, false) +
      createUpsertStatementUtilityValuesPartAfter(transferSourceTarget);
  }

}
