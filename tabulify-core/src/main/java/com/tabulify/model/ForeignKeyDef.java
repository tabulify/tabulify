package com.tabulify.model;

import com.tabulify.DbLoggers;
import net.bytle.exception.NoColumnException;
import net.bytle.type.Arrayss;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A foreign key is a constraint definition
 * but it's basically the definition of relation between two tables
 * * from a column(s)
 * * and the columns of the (foreign) primary key
 * This object should then not be created with the pair of columns that creates this relations ie
 * * the intern columns
 * * the (foreign) primary key
 * <p>
 * A foreign key cannot be considered as a data resource because in SQL (JDBC)
 * * its {@link #getName() name} can be null
 * * dropping a foreign key is altering a table
 * * it's considered as <a href="https://www.w3.org/TR/2015/REC-tabular-metadata-20151217/#foreign-key-reference-between-tables">metadata</a>
 *
 * @see <a href="https://www.w3.org/TR/2015/REC-tabular-metadata-20151217/#dfn-foreign-key-definition">Web tabular Metadata Foreign Key</a>
 * @see <a href="https://www.w3.org/TR/2015/REC-tabular-metadata-20151217/#schema-examples">Web tabular Metadata Foreign Key - Examples</a>
 */
public class ForeignKeyDef implements Comparable<ForeignKeyDef>, Constraint {

  // The list of column
  // Order is important
  private final List<ColumnDef> columnDefs;
  // The foreign primary key
  private final PrimaryKeyDef foreignPrimaryKey;
  // The data def
  private final RelationDef relationDef;

  // May be null via JBDC
  // See description
  // https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getImportedKeys(java.lang.String,%20java.lang.String,%20java.lang.String)
  private String name;

  private ForeignKeyDef(RelationDef relationDef, PrimaryKeyDef primaryKeyDef, String... columnNames) {

    // null check
    if (primaryKeyDef == null) {
      final String msg = "The foreign primary key can not be null when creating a foreign key";
      DbLoggers.LOGGER_DB_ENGINE.severe(msg);
      throw new RuntimeException(msg);
    }
    if (columnNames == null) {
      throw new RuntimeException("columnDefs should not be null for the primary key (" + primaryKeyDef + ")");
    }
    if (columnNames.length == 0) {
      throw new RuntimeException("The columnDefs arguments should not have a size of zero to define a foreign key to " + primaryKeyDef.getRelationDef().getDataPath());
    }
    if (Arrays.stream(columnNames).noneMatch(Objects::nonNull)) {
      throw new RuntimeException("The columnDefs argument has only null value. We can't therefore add a foreign key to " + primaryKeyDef.getRelationDef().getDataPath());
    }


    // Size check
    if (primaryKeyDef.getColumns().size() != columnNames.length) {
      final String msg = "The foreign primary key (" + primaryKeyDef + ") has (" + primaryKeyDef.getColumns().size() + ") columns and we got only (" + columnNames.length + ") columns from the argument (" + Arrayss.toJoinedStringWithComma(columnNames) + ").";
      DbLoggers.LOGGER_DB_ENGINE.severe(msg);
      throw new RuntimeException(msg);
    }

    // Finally
    this.relationDef = relationDef;
    this.foreignPrimaryKey = primaryKeyDef;
    this.columnDefs = Arrays.stream(columnNames)
      .filter(columnName -> {

        if (!relationDef.hasColumn(columnName)) {
          String msg = "The column (" + columnName + ") is not known for the data resource (" + relationDef.getDataPath() + ") and then cannot be used to create a foreign key.";
          if (this.getRelationDef().getDataPath().getConnection().getTabular().isIdeEnv()) {
            throw new IllegalStateException(msg);
          }
          DbLoggers.LOGGER_DB_ENGINE.severe(msg);
          return false;
        }
        return true;

      })
      .map(columnName -> {

        try {
          return relationDef.getColumnDef(columnName);
        } catch (NoColumnException e) {
          throw new RuntimeException("The column (" + columnName + ") is not known for the data resource (" + relationDef.getDataPath() + ") and then cannot be used to create a foreign key.");
        }

      })
      .collect(Collectors.toList());


  }

  public static ForeignKeyDef createOf(RelationDef relationDef, PrimaryKeyDef primaryKeyDef, String... columnNames) {
    return new ForeignKeyDef(relationDef, primaryKeyDef, columnNames);
  }

  public static ForeignKeyDef createOf(RelationDef relationDef, PrimaryKeyDef primaryKeyDef, ColumnDef... columnDefs) {
    return createOf(relationDef, primaryKeyDef, Arrays.stream(columnDefs).map(ColumnDef::getColumnName).toArray(String[]::new));
  }

  public static ForeignKeyDef createOf(RelationDef relationDef, PrimaryKeyDef primaryKey, List<String> columnNames) {
    return new ForeignKeyDef(relationDef, primaryKey, columnNames.toArray(new String[0]));
  }

  /**
   * The name may be null
   * See
   * https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getImportedKeys(java.lang.String,%20java.lang.String,%20java.lang.String)
   *
   * @return the name of the fk
   */
  public String getName() {
    return name;
  }


  public List<ColumnDef> getChildColumns() {
    return columnDefs;
  }

  public PrimaryKeyDef getForeignPrimaryKey() {

    return foreignPrimaryKey;
  }

  public ForeignKeyDef setName(String name) {
    this.name = name;
    return this;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ForeignKeyDef that = (ForeignKeyDef) o;
    return columnDefs.equals(that.columnDefs) &&
      foreignPrimaryKey.equals(that.foreignPrimaryKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(columnDefs, foreignPrimaryKey);
  }

  @Override
  public String toString() {
    final PrimaryKeyDef foreignPrimaryKey = this.foreignPrimaryKey;
    final List<String> childColumns = getChildColumns().stream().map(ColumnDef::getColumnName).collect(Collectors.toList());
    return "Fk from " + getRelationDef().getDataPath() + childColumns + " to " + foreignPrimaryKey.getRelationDef().getDataPath();
  }

  @Override
  public RelationDef getRelationDef() {
    return this.relationDef;
  }

  @Override
  public int compareTo(ForeignKeyDef o) {

    return (this.getRelationDef().getDataPath().getName() + this.getForeignPrimaryKey().getRelationDef().getDataPath().getName())
      .compareTo(o.getRelationDef().getDataPath().getName() + o.getForeignPrimaryKey().getRelationDef().getDataPath().getName());

  }
}
