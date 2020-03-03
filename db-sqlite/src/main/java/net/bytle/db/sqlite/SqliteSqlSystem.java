package net.bytle.db.sqlite;

import net.bytle.db.jdbc.SqlDataPath;
import net.bytle.db.jdbc.SqlDataSystem;
import net.bytle.db.jdbc.JdbcDataSystemSql;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.ForeignKeyDef;
import net.bytle.db.model.PrimaryKeyDef;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SqliteSqlSystem extends SqlDataSystem {

  private static final Logger LOGGER = LoggerFactory.getLogger(SqliteSqlSystem.class);
  private final SqliteDataStore sqliteDataStore;


  @Override
  public Boolean exists(DataPath dataPath) {
    SqliteDataPath sqliteDataPath = (SqliteDataPath) dataPath;
    if (sqliteDataPath.getName()==null){
      // Schema
      return true;
    } else {
      return super.exists(dataPath);
    }
  }

  public SqliteSqlSystem(SqliteDataStore sqliteDataStore) {
    super(sqliteDataStore);
    this.sqliteDataStore = sqliteDataStore;
  }


  @Override
  public void create(DataPath dataPath) {
    SqliteDataPath sqliteDataPath = (SqliteDataPath) dataPath;
    List<String> statements = createTableStatements(sqliteDataPath);
    super.execute(statements);
  }


  /**
   * Returns statement to create the table
   * SQLlite has limited support on the alter statement
   * The primary key shoud be in the create statement.
   * See https://sqlite.org/faq.html#q11
   *
   * @param dataPath
   * @return a create statement https://www.sqlite.org/lang_createtable.html
   */
  @Override
  protected List<String> createTableStatements(SqlDataPath dataPath) {

    List<String> statements = new ArrayList<>();
    StringBuilder statement = new StringBuilder();
    statement.append("CREATE TABLE " + JdbcDataSystemSql.getQuotedTableName(dataPath) + " (\n");
    RelationDef tableDef = dataPath.getOrCreateDataDef();
    if (tableDef == null) {
      throw new RuntimeException("The dataPath (" + dataPath.toString() + ") has no columns definitions. We can't create a table from then");
    }
    for (int i = 0; i < tableDef.getColumnsSize(); i++) {
      ColumnDef columnDef = tableDef.getColumnDef(i);
      statement.append(createColumnStatement(columnDef));
      if (i != tableDef.getColumnsSize() - 1) {
        statement.append(",\n");
      }
    }

    // Pk
    final PrimaryKeyDef primaryKey = tableDef.getPrimaryKey();
    if (tableDef.getPrimaryKey() != null) {
      statement.append(",\nPRIMARY KEY (");
      for (int i = 0; i < primaryKey.getColumns().size(); i++) {
        ColumnDef columnDef = primaryKey.getColumns().get(i);
        statement.append(columnDef.getColumnName());
        if (i < primaryKey.getColumns().size() - 1) {
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

      statement.append(",\nFOREIGN KEY (");

      // Child columns
      List<String> childColumns = foreignKeyDef.getChildColumns().stream()
        .map(s -> s.getColumnName())
        .collect(Collectors.toList());
      statement.append(String.join(",", childColumns));

      statement.append(") REFERENCES ");
      statement.append(foreignKeyDef.getForeignPrimaryKey().getDataDef().getDataPath().getName());
      statement.append("(");

      // Foreign / Parent  columns
      List<String> parentColumns = foreignKeyDef.getForeignPrimaryKey().getColumns()
        .stream()
        .map(s -> s.getColumnName())
        .collect(Collectors.toList());
      statement.append(String.join(",", parentColumns));
      statement.append(")");

    }


    // End statement
    statement.append("\n)");
    statements.add(statement.toString());

    return statements;

  }



  @Override
  public String truncateStatement(SqlDataPath dataPath) {
    StringBuilder truncateStatementBuilder = new StringBuilder().append("delete from ");
    truncateStatementBuilder.append(JdbcDataSystemSql.getFullyQualifiedSqlName(dataPath));
    return truncateStatementBuilder.toString();
  }


  @Override
  public List<DataPath> getChildrenDataPath(DataPath dataPath) {
    return super.getChildrenDataPath(dataPath);
  }
}
