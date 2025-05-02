package com.tabulify.jdbc;

import com.tabulify.conf.AttributeValue;
import net.bytle.exception.CastException;
import net.bytle.exception.NotSupportedException;
import net.bytle.type.*;

/**
 * In SQL specification, this type is called a `TABLE_TYPE`
 * This is for history reason.
 * <p>
 * We extend it with container type for catalog and schema
 * and with a `unknown` object type to retrieve it later from the database
 *
 * @see <a href="https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getTableTypes()">table type</a>
 * @see <a href=https://calcite.apache.org/docs/model.html#view>The calcite definition</a>
 */
public enum SqlDataPathType implements AttributeValue, MediaType {

  // In case there is no schema functionality such as with sqlite,
  // the schema is the empty string
  SCHEMA("A schema", true), // container
  CATALOG("A catalog", true), // Container
  TABLE("A table", false),
  VIEW("A view", false),
  SYSTEM_VIEW("A system view", false),
  SYSTEM_TABLE("A system table", false),
  ALIAS("An alias", false),
  SYNONYM("A synonym", false),
  SCRIPT("A script", false), // Special internal type (runtime table such as query or statement)
  UNKNOWN("Object (Non qualified object, this is start state to not default to null)", false);


  private final boolean isContainer;
  private final String description;
  private final String subType;

  /**
   * @param description the description of the type
   * @param isContainer true if the object is a container of object, false if not
   */
  SqlDataPathType(String description, boolean isContainer) {
    this.description = description;
    this.isContainer = isContainer;
    this.subType = Key.toUriName(this.name());
  }

  /**
   * This function is used as condition to instantiate or not
   * a resource as table type
   * <p>
   * We don't manage `index` for instance and index is not in the list
   *
   * @param typeName the type name
   * @return true if this is a table type data path
   */
  public static SqlDataPathType getSqlType(String typeName) throws NotSupportedException {

    try {
      return Casts.cast(typeName, SqlDataPathType.class);
    } catch (CastException e) {
      throw new NotSupportedException("Not a supported table type. "+e.getMessage());
    }

  }


  @Override
  public String toString() {

    // Name should match the Sql Jdbc type
    return super.toString();

  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public boolean isContainer() {
    return isContainer;
  }

  @Override
  public String getExtension() {
    return this.subType;
  }

  @Override
  public String getSubType() {
    return this.subType;
  }

  @Override
  public String getType() {
    return "sql";
  }


}
