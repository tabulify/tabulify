package com.tabulify.jdbc;

import com.tabulify.conf.AttributeValue;
import com.tabulify.exception.CastException;
import com.tabulify.exception.NotSupportedException;
import com.tabulify.type.Casts;
import com.tabulify.type.Enums;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MediaType;

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
public enum SqlMediaType implements AttributeValue, MediaType {

  // In case there is no schema functionality such as with sqlite,
  // the schema is the empty string
  SCHEMA("A schema", true, false, false), // container
  CATALOG("A catalog", true, false, false), // Container
  TABLE("A table", false, false, false),
  VIEW("A view", false, false, false),
  SYSTEM_VIEW("A system view", false, false, false),
  SYSTEM_TABLE("A system table", false, false, false),
  ALIAS("An alias", false, false, false),
  SYNONYM("A synonym", false, false, false),
  // Special internal type (runtime table such as query or statement)
  // It's a sql script with a connection, expected data
  REQUEST("A SQL request", false, true, true),
  /**
   * The result of a request execution if the result is a result set
   */
  RESULT_SET("A SQL result set", false, false, true),
  // Before we read the metadata in the database, we don't know the type of object
  // We miss a builder pattern here
  // This is a start state to not default to null
  OBJECT("Object (Non qualified object)", false, false, false);


  public static final String SQL_TYPE = "sql";
  private final boolean isContainer;
  private final String description;
  private final String subType;
  private final boolean isRuntime;
  private final KeyNormalizer kind;
  private final boolean anonymous;

  /**
   * @param description the description of the type
   * @param isContainer true if the object is a container of object, false if not
   * @param isRuntime   if the media is a script
   * @param anonymous   if the media has no name in the path (note that at the end they always have a derived name so that the name variable in a template is available)
   */
  SqlMediaType(String description, boolean isContainer, boolean isRuntime, boolean anonymous) {
    this.description = description;
    this.isContainer = isContainer;
    this.subType = this.name().toLowerCase();
    this.isRuntime = isRuntime;
    this.kind = KeyNormalizer.createSafe(SQL_TYPE + "-" + this.name().toLowerCase());
    this.anonymous = anonymous;
  }

  /**
   * This function is used as condition to instantiate or not
   * a resource as table type
   * <p>
   * We don't manage `index` for instance and index is not in the list
   *
   * @param subType the subtype (the type being {@link #SQL_TYPE})
   */
  public static SqlMediaType castsToSqlType(String subType) throws NotSupportedException {

    try {
      return Casts.cast(subType, SqlMediaType.class);
    } catch (CastException e) {
      throw new NotSupportedException("The subtype value " + subType + " is not a supported sql subtype. " + e.getMessage());
    }

  }


  public static SqlMediaType castsToSqlType(MediaType mediaType) {

    if (mediaType instanceof SqlMediaType) {
      return (SqlMediaType) mediaType;
    }
    String subType = mediaType.getSubType();
    // The value may be entered by a human without sql
    String type = mediaType.getType();
    if (type != null && !type.isEmpty() && !type.equalsIgnoreCase(SqlMediaType.SQL_TYPE)) {
      throw new IllegalArgumentException("The media type " + mediaType + " is not a sql type. It has the type " + type + ". We were expecting as subtype one of " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(SqlMediaType.class) + ", not " + subType);
    }

    try {
      return SqlMediaType.castsToSqlType(subType);
    } catch (NotSupportedException e) {
      throw new IllegalArgumentException("The media type " + mediaType + " is not a sql type. We were expecting as subtype one of " + Enums.toConstantAsStringOfUriAttributeCommaSeparated(SqlMediaType.class) + ", not " + subType, e);
    }
  }


  @Override
  public String toString() {

    return SQL_TYPE + "/" + subType;

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
  public KeyNormalizer getKind() {
    return this.kind;
  }


  @Override
  public String getSubType() {
    return this.subType;
  }

  @Override
  public String getType() {
    return SQL_TYPE;
  }

  @Override
  public boolean isRuntime() {
    return this.isRuntime;
  }

  public boolean isAnonymous() {
    return this.anonymous;
  }
}
