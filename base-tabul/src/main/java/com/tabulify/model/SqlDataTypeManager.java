package com.tabulify.model;

import com.tabulify.conf.Attribute;
import com.tabulify.connection.Connection;
import com.tabulify.connection.ConnectionAttributeEnumBase;
import com.tabulify.spi.DataSystem;
import com.tabulify.spi.StrictException;
import com.tabulify.exception.CastException;
import com.tabulify.exception.InternalException;
import com.tabulify.exception.MissingSwitchBranch;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyInterface;
import com.tabulify.type.KeyNormalizer;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.sql.Types.*;

/**
 * Manager of Data Type
 * (Created to extract the code from the connection object)
 * It's called SQL because this is SQL based
 */
public class SqlDataTypeManager {


  /**
   * The maximum length of integer by size
   */
  public static final int INTEGER_SIGNED_MAX_LENGTH = 10;
  public static final int BIGINT_SIGNED_MAX_LENGTH = 19;
  public static final int SMALLINT_SIGNED_MAX_LENGTH = 5;
  public static final int TINYINT_SIGNED_MAX_LENGTH = 4;
  public static final int MEDIUMINT_SIGNED_MAX_LENGTH = 9;


  /**
   * Builder object
   * Used in the build setting (before the build)
   */
  /**
   * All Sql type by name
   * We have:
   * * at minima all our supported standard names {@link SqlDataTypeAnsi} with their standard type name or the database name implementation set at {@link SqlDataType#toKeyNormalizer()}
   * * and all aliases/synonym
   */
  private Map<SqlDataTypeKey, SqlDataType.SqlDataTypeBuilder<?>> typeNameTypeBuilderBuilderMap = new HashMap<>();
  /**
   * A map that add java class to type name
   * before build time
   */
  private Map<Class<?>, SqlDataTypeKey> javaClassTypeNameBuilderMap = new HashMap<>();
  /**
   * A map that add type code to type name
   * before build time
   */
  private Map<SqlDataTypeAnsi, SqlDataType.SqlDataTypeBuilder<?>> ansiTypeTypeNameBuilderMap = new HashMap<>();


  /**
   * We may receive a primitive type as value
   * This Map maps the primitive to the wrapper class
   * so that we can detect the default sql type code
   */
  private static final Map<Class<?>, Class<?>> primitiveToWrapperClassMap = new HashMap<>();

  /**
   * Build object
   * The final build object
   */
  /**
   * Which SQL type do we need to load the value of a java class
   * The first type code is the used one
   * <p>
   * Note:
   * By default, should be last
   * * LongNVarchar, nchar, nvarchar
   * * small int, tinyInt
   */
  private Map<Class<?>, SqlDataType<?>> javaClassBySqlTypeBuildMap;
  /**
   * ANSI Type code to Sql Type Mapping
   * Note that for integer, this is the signed type only
   * ie the type code of {@link SqlDataTypeAnsi}
   */
  private Map<SqlDataTypeAnsi, SqlDataType<?>> ansiTypeSqlTypeBuildMap;
  /**
   * The sql data type by key
   * It contains name and alias
   */
  private final Map<SqlDataTypeKey, SqlDataType<?>> sqlDataTypesByKeyBuildMap = new HashMap<>();


  /**
   * Default mapping from java class to type (ansi)
   */
  private final static Map<Class<?>, SqlDataTypeAnsi> javaClassAnsiMap = new HashMap<>();

  static {

    primitiveToWrapperClassMap.put(boolean.class, Boolean.class);
    primitiveToWrapperClassMap.put(byte.class, Byte.class);
    primitiveToWrapperClassMap.put(short.class, Short.class);
    primitiveToWrapperClassMap.put(int.class, Integer.class);
    primitiveToWrapperClassMap.put(long.class, Long.class);
    primitiveToWrapperClassMap.put(float.class, Float.class);
    primitiveToWrapperClassMap.put(double.class, Double.class);
    primitiveToWrapperClassMap.put(char.class, Character.class);


    javaClassAnsiMap.put(String.class, SqlDataTypeAnsi.CHARACTER_VARYING);
    javaClassAnsiMap.put(Integer.class, SqlDataTypeAnsi.INTEGER);
    javaClassAnsiMap.put(Long.class, SqlDataTypeAnsi.BIGINT);
    javaClassAnsiMap.put(java.math.BigDecimal.class, SqlDataTypeAnsi.DECIMAL);
    javaClassAnsiMap.put(Boolean.class, SqlDataTypeAnsi.BOOLEAN);
    javaClassAnsiMap.put(Double.class, SqlDataTypeAnsi.DOUBLE_PRECISION);
    javaClassAnsiMap.put(Float.class, SqlDataTypeAnsi.REAL);
    javaClassAnsiMap.put(java.sql.Date.class, SqlDataTypeAnsi.DATE);
    javaClassAnsiMap.put(java.sql.Timestamp.class, SqlDataTypeAnsi.TIMESTAMP);
    javaClassAnsiMap.put(java.sql.Time.class, SqlDataTypeAnsi.TIME);
    javaClassAnsiMap.put(OffsetDateTime.class, SqlDataTypeAnsi.TIMESTAMP_WITH_TIME_ZONE);
    javaClassAnsiMap.put(OffsetTime.class, SqlDataTypeAnsi.TIME_WITH_TIME_ZONE);

  }

  private final Connection connection;
  /**
   * A build element to avoid/detect recursion
   */
  private Set<SqlDataType.SqlDataTypeBuilder<?>> buildTypes = new HashSet<>();
  /**
   * A pointer to avoid recursion on data type building
   * (Example: trying to get a result set that tries to get a data type)
   */
  private boolean isBuilding = false;


  public SqlDataTypeManager(Connection connection) {

    this.connection = connection;


  }

  /**
   * @return a type with the next precision on integer
   */
  public SqlDataType<?> getUpperUnsignedIntegerType(SqlDataTypeAnsi sqlDataType) throws CastException {
    SqlDataTypeAnsi higherTypedName;
    switch (sqlDataType) {
      case TINYINT:
        higherTypedName = SqlDataTypeAnsi.SMALLINT;
        break;
      case SMALLINT:
        higherTypedName = SqlDataTypeAnsi.INTEGER;
        break;
      case INTEGER:
      case BIGINT:
        higherTypedName = SqlDataTypeAnsi.BIGINT;
        break;
      default:
        throw new MissingSwitchBranch("typed name", sqlDataType);
    }
    SqlDataType<?> higherTypeObject = this.getSqlDataType(higherTypedName);
    if (higherTypeObject != null) {
      return higherTypeObject;
    }
    if (sqlDataType == SqlDataTypeAnsi.BIGINT) {
      // Almost all database support bigint, should not happen
      throw new CastException("The connection (" + this.connection + ") seems to have no bigint data type");
    }
    return getUpperUnsignedIntegerType(higherTypedName);
  }

  /**
   * @return the SqlDataType or null if not found so that you can search elsewhere
   */
  public <T> SqlDataType<T> getSqlDataType(Class<T> clazz) {
    this.buildIfNeeded();
    SqlDataType<?> sqlDataType = javaClassBySqlTypeBuildMap.get(clazz);
    if (sqlDataType != null) {
      /**
       * Not sure if it's the good way to handle that
       * For instance, asking for a float, in sqlite would return a real that is `double.class`
       * We don't return a float, so it should not have any impact
       */
      if (Casts.isNarrowingConversion(clazz, sqlDataType.getValueClass())) {
        throw new RuntimeException("Narrowing conversion, possible data loss: Bad class returned by the data type " + sqlDataType + ". Asked class was " + clazz + ", returned class is " + sqlDataType.getValueClass());
      }
      //noinspection unchecked
      return (SqlDataType<T>) sqlDataType;
    }
    sqlDataType = javaClassBySqlTypeBuildMap.get(primitiveToWrapperClassMap.get(clazz));
    if (sqlDataType != null) {
      if (Casts.isNarrowingConversion(clazz, sqlDataType.getValueClass())) {
        throw new RuntimeException("Narrowing conversion, possible data loss: Bad class returned by the data type " + sqlDataType + ". Asked class was " + clazz + ", returned class is " + sqlDataType.getValueClass());
      }
      //noinspection unchecked
      return (SqlDataType<T>) sqlDataType;
    }
    return null;
  }

  /**
   * Lazy building
   * We update the type from the driver lazily
   * Why?
   * * When listing connection, we try to not connect to it for performance reason
   * * The default may be changed (ie {@link ConnectionAttributeEnumBase#VARCHAR_DEFAULT_PRECISION}
   * * The credentials are needed for database, and they are not given at the constructor level (no connection builder pattern yet)
   */
  private void buildIfNeeded() {

    // A breaker to not update the data type each time
    if (!sqlDataTypesByKeyBuildMap.isEmpty()) {
      return;
    }

    if (isBuilding) {
      throw new InternalException("Recursion detected: Data type building is occurring, you can't ask a data type");
    }
    isBuilding = true;


    /**
     * Extension point for connection to change the types
     */
    this.connection.getDataSystem().dataTypeBuildingMain(this);

    /**
     * Build
     */
    for (SqlDataType.SqlDataTypeBuilder<?> sqlDataTypeBuilder : typeNameTypeBuilderBuilderMap.values()) {
      if (buildTypes.contains(sqlDataTypeBuilder)) {
        continue;
      }
      buildType(sqlDataTypeBuilder, null);
    }
    // null to be sure that we will not use it anymore and to release the memory
    typeNameTypeBuilderBuilderMap = null;
    buildTypes = null;


    /**
     * Utility class to get the default by type code
     * We get them by looking up for known name,
     * and we return the parent if any
     * The ansi code for now are only for signed value
     */
    ansiTypeSqlTypeBuildMap = sqlDataTypesByKeyBuildMap.values()
      .stream()
      /**
       * Only signed type
       */
      .filter(d -> {
        if (!d.isNumber()) {
          return true;
        }
        /**
         * No unsigned attribute
         */
        return !d.getUnsignedAttribute();
      })
      /**
       * Other is equivalent to unknown/null
       */
      .filter(d -> d.getAnsiType() != SqlDataTypeAnsi.OTHER)
      .collect(Collectors.groupingBy(SqlDataType::getAnsiType))
      .entrySet()
      .stream()
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        entry -> {
          List<SqlDataType<?>> sqlDataTypes = entry.getValue();
          if (sqlDataTypes.size() == 1) {
            return sqlDataTypes.get(0);
          }
          SqlDataTypePriority highestPriority = sqlDataTypes.stream()
            .map(SqlDataType::getPriority)
            .max(Comparator.comparing(SqlDataTypePriority::getValue))
            .orElse(SqlDataTypePriority.DEFAULT);
          // return the type that has the same name
          // ie Example from sqlserver
          // between nvarchar and sysname for the nvarchar type, we take nvarchar
          for (SqlDataType<?> sqlDataType : sqlDataTypes) {
            SqlDataTypeAnsi typedName = sqlDataType.getAnsiType();
            /**
             * If this is a known ansi name, this is it
             */
            if (isKnownAnsiName(sqlDataType)) {
              return sqlDataType;
            }
            // national character varying has the nvarchar alias
            if (typedName.getNormalizedAliases().contains(sqlDataType.toKeyNormalizer())) {
              /**
               * In SQL Server, datetime2 should be chosen and not datetime
               */
              if (sqlDataType.getPriority() == highestPriority) {
                return sqlDataType;
              }
            }
          }
          return sqlDataTypes.stream()
            .max(Comparator.comparing(SqlDataType::getPriority))
            .orElseThrow();
        }
      ))
      .entrySet()
      .stream()
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        e -> e.getValue().getParentOrSelf()
      ));

    // Extra Types, type name
    for (Map.Entry<SqlDataTypeAnsi, SqlDataType.SqlDataTypeBuilder<?>> typeCodeTypeNameEntry : ansiTypeTypeNameBuilderMap.entrySet()) {
      SqlDataTypeAnsi typeCode = typeCodeTypeNameEntry.getKey();
      SqlDataType<?> value = sqlDataTypesByKeyBuildMap.get(typeCodeTypeNameEntry.getValue().getTypeKey());
      ansiTypeSqlTypeBuildMap.put(typeCode, value);
    }
    ansiTypeTypeNameBuilderMap = null;

    // Check that we have all types
    Boolean isIdeEnv = connection.getTabular().isIdeEnv();
    if (isIdeEnv) {
      for (SqlDataTypeVendor sqlVendor : connection.getDataSystem().getSqlDataTypeVendors()) {
        Integer vendorTypeNumber = sqlVendor.getVendorTypeNumber();
        // The vendor type SqlDataTypeAnsi are for memory, file system
        // We don't test them
        if (sqlVendor instanceof SqlDataTypeAnsi) {
          continue;
        }
        SqlDataType<?> typeBuilder = getSqlDataType(sqlVendor.toKeyNormalizer(), vendorTypeNumber);
        if (typeBuilder == null) {
          throw new InternalException("Internal Check: the defined vendor type (" + sqlVendor + "/" + sqlVendor.getVendorTypeNumber() + ") was not found in the types");
        }
      }
    }

    /**
     * Java class to type
     */
    javaClassBySqlTypeBuildMap = sqlDataTypesByKeyBuildMap
      .values()
      .stream()
      .filter(e -> e.getValueClass() != null)
      .collect(Collectors.groupingBy(SqlDataType::getValueClass))
      .entrySet()
      .stream()
      .collect(Collectors.toMap(
        Map.Entry::getKey,
        entry -> {

          /**
           * If the default ansi type is in the list, we return it
           */
          SqlDataTypeAnsi ansiForClass = javaClassAnsiMap.get(entry.getKey());
          SqlDataType<?> ansiType = ansiTypeSqlTypeBuildMap.get(ansiForClass);
          if (entry.getValue().contains(ansiType)) {
            return ansiType;
          }

          /**
           * By name
           */
          SqlDataType<?> finalValue = entry.getValue()
            .stream()
            .filter(this::isKnownAnsiName)
            .max(Comparator.comparing(e -> e.getPriority().getValue()))
            .orElse(null);
          if (finalValue != null) {
            return finalValue;
          }

          /**
           * Return the first one
           */
          return entry.getValue().get(0);
        }
      ));

    /**
     * Adding the missing one if any
     */
    for (Map.Entry<Class<?>, SqlDataTypeAnsi> entry : javaClassAnsiMap.entrySet()) {
      SqlDataType<?> type = javaClassBySqlTypeBuildMap.get(entry.getKey());
      if (type != null) {
        continue;
      }
      type = ansiTypeSqlTypeBuildMap.get(entry.getValue());
      if (type != null) {
        javaClassBySqlTypeBuildMap.put(entry.getKey(), type);
      }
    }

    /**
     * Value Override by the system
     */
    for (Map.Entry<Class<?>, SqlDataTypeKey> classNameEntry : javaClassTypeNameBuilderMap.entrySet()) {
      SqlDataType<?> value = sqlDataTypesByKeyBuildMap.get(classNameEntry.getValue());
      javaClassBySqlTypeBuildMap.put(classNameEntry.getKey(), value);
    }
    javaClassTypeNameBuilderMap = null;

    /**
     * We don't build anymore
     */
    isBuilding = false;

  }

  /**
   * @return true if the type name has an ansi name
   * Java class or Ansi type are coupled to known ansi type not
   * to custom type
   * for instance,
   * sql server has the type
   * * `sql identifier` coupled to `String.class`
   * * `int identity` coupled to {@link SqlDataTypeAnsi#INTEGER}
   * We map java class to known ansi name only
   */
  private boolean isKnownAnsiName(SqlDataType<?> e) {

    SqlDataTypeAnsi ansiType = e.getAnsiType();
    if (e.toKeyNormalizer().equals(ansiType.toKeyNormalizer())) {
      return true;
    }
    return ansiType.getNormalizedAliases().contains(e.toKeyNormalizer());
  }


  /**
   * Recursive function (ue that can call itself)
   * to handle the fact that type may be aliased/have a synonym
   *
   * @param typeBuilder - the type
   * @param parent      - the parent or null if no parent
   */
  private <T> SqlDataType<T> buildType(SqlDataType.SqlDataTypeBuilder<T> typeBuilder, SqlDataType.SqlDataTypeBuilder<T> parent) {

    /**
     * Cycle detections
     */
    if (this.buildTypes.contains(typeBuilder)) {
      throw new InternalException("Cycle detected: The child/synonym/alias type builder (" + typeBuilder + ") provided by the parent (" + parent + ") has already been seen");
    }
    this.buildTypes.add(typeBuilder);

    /**
     * The work
     */

    /**
     * Already Built?
     */
    SqlDataType<?> buildType = sqlDataTypesByKeyBuildMap.get(typeBuilder.getTypeKey());
    if (buildType != null) {
      // recursion (may have been already built)
      //noinspection unchecked
      return (SqlDataType<T>) buildType;
    }

    /**
     * Get the parent builder
     */
    SqlDataType.SqlDataTypeBuilder<T> parentBuilder = this.getParentBuilder(typeBuilder);
    SqlDataType<T> buildParent = null;
    if (parentBuilder != null) {
      SqlDataType<?> buildParentInBuildMap = sqlDataTypesByKeyBuildMap.get(parentBuilder.getTypeKey());
      if (buildParentInBuildMap == null) {
        buildParentInBuildMap = buildType(parentBuilder, typeBuilder);
      }
      if (buildParentInBuildMap.getValueClass() != typeBuilder.getJavaClass()) {
        throw new RuntimeException("Type should be the same between the child (" + typeBuilder.getJavaClass() + ") and the parent (" + buildParentInBuildMap + ")");
      }
      //noinspection unchecked
      buildParent = (SqlDataType<T>) buildParentInBuildMap;
      typeBuilder.setParent(buildParent);
    }


    /**
     * Build and put
     */
    SqlDataType<T> sqlDataType = typeBuilder.build();
    if (buildParent != null) {
      buildParent.addChild(sqlDataType);
    }

    sqlDataTypesByKeyBuildMap.put(sqlDataType.getKey(), sqlDataType);
    return sqlDataType;
  }

  private <T> SqlDataType.SqlDataTypeBuilder<T> getParentBuilder(SqlDataType.SqlDataTypeBuilder<T> sqlDataTypeBuilder) {
    return sqlDataTypeBuilder.getParentBuilder();
  }


  /**
   * @param typeCode - an ansi type code
   * @deprecated use {@link #getSqlDataType(SqlDataTypeKeyInterface)}  with an ANSI constant
   */
  @Deprecated
  public SqlDataType<?> getSqlDataType(Integer typeCode) {
    this.buildIfNeeded();
    /**
     * Other is the equivalent of unknown or null
     */
    if (typeCode == OTHER) {
      return null;
    }
    SqlDataTypeAnsi ansi = SqlDataTypeAnsi.cast(null, typeCode);
    return ansiTypeSqlTypeBuildMap.get(ansi);

  }


  /**
   * @param typeName - the name
   * @param typeCode - the type code
   * @return the corresponding type
   * may return null if not found so that we can test another name
   */
  public SqlDataType<?> getSqlDataType(KeyNormalizer typeName, int typeCode) {

    this.buildIfNeeded();

    /**
     * The name (the unique key)
     * This should hit almost 100% of the time
     * <p>
     * We don't try on ANSI Standard Sql Name as it's not a translation, it's a get
     * See {@link Connection#getSqlDataTypeFromSourceColumn(ColumnDef)} for a type translation
     * <p>
     * If we tried to get a type by {@link SqlDataType#getAnsiType() ANSI Standard Sql Name} the name would be not unique. We could get an integer, but we could get an `int identity` (SQL Server type with auto-increment to true)
     * <p>
     * To get the type for an ANSI, use {@link #getSqlDataType(SqlDataTypeKeyInterface)}
     */
    SqlDataTypeKey sqlDataTypeKey = new SqlDataTypeKey(connection, typeName, typeCode);
    return sqlDataTypesByKeyBuildMap.get(sqlDataTypeKey);


  }

  public Set<SqlDataType<?>> getSqlDataTypes() {
    this.buildIfNeeded();
    return new HashSet<>(sqlDataTypesByKeyBuildMap.values());
  }


  public <T> SqlDataType.SqlDataTypeBuilder<T> createTypeBuilder(KeyNormalizer typeName, int typeCode, Class<T> aClass) {
    SqlDataTypeKey sqlDataTypeKey = new SqlDataTypeKey(connection, typeName, typeCode);
    return createTypeBuilder(sqlDataTypeKey, aClass);
  }

  /**
   * Create a type
   */
  public <T> SqlDataType.SqlDataTypeBuilder<T> createTypeBuilder(SqlDataTypeKey sqlDataTypeKey, Class<T> aClass) {

    SqlDataType.SqlDataTypeBuilder<?> typeInList = this.typeNameTypeBuilderBuilderMap.get(sqlDataTypeKey);
    if (typeInList != null) {

      /**
       * Due to the fact that we have a child/parent relationship in type
       * via the {@link SqlDataTypeVendor#getAliases()}, a type may have been created by its parent
       * <p>
       * Normally, we don't create type. The driver is the driver.
       * If we gave to the driver back a bad type code, it will for sure throw an exception
       * If you want to set the class at build time, you need to create a vendor type {@link DataSystem#getSqlDataTypeVendors()}
       * The only system where we have created type is SQLite as its type system is completely loose.
       */
      if (typeInList.getJavaClass() != aClass) {
        throw new InternalException("The type (" + sqlDataTypeKey + ") already exists and has not the same java class " + typeInList.getJavaClass() + " as the one created " + aClass + " ");
      }
      //noinspection unchecked
      return (SqlDataType.SqlDataTypeBuilder<T>) typeInList;

    }
    SqlDataType.SqlDataTypeBuilder<T> type = SqlDataType.builder(connection, sqlDataTypeKey, aClass)
      .setManager(this);
    typeNameTypeBuilderBuilderMap.put(sqlDataTypeKey, type);

    /**
     * Default known values
     */
    Integer typeCode = sqlDataTypeKey.getVendorTypeNumber();
    switch (typeCode) {
      /**
       * Data Types without max precision and scale
       * Implicitly defined by the name
       */
      case DOUBLE:
      case REAL:
      case Types.BIGINT:
      case INTEGER:
      case SMALLINT:
      case TINYINT:
      case BOOLEAN:
      case DATE:
      case SQLXML:
      case OTHER:
        type
          .setMinimumScale(0) // no scale parameter
          .setMaximumScale(0); // no scale parameter
        /**
         * Other default
         */
        switch (typeCode) {
          case INTEGER:
          case BIGINT:
          case SMALLINT: // too small
          case TINYINT:
          case DOUBLE: // approximate numeric
          case REAL: // approximate numeric
          default:
            type
              .setAutoIncrement(false)
              // signed by default
              .setUnsignedAttribute(false);
        }
        // Precision = Length of the maximum value (ie the number of digits)
        switch (typeCode) {
          case INTEGER:
            // The maximum value (2,147,483,647) has 10 digits
            type
              .setMaxPrecision(INTEGER_SIGNED_MAX_LENGTH)
              // length is for a signed integer
              .setUnsignedAttribute(false);
            break;
          case Types.BIGINT:
            // Signed BIGINT (20): -9223372036854775808 to 9,223,372,036,854,775,807
            // Unsigned BIGINT: 0 to 18,446,744,073,709,551,615
            type
              .setMaxPrecision(BIGINT_SIGNED_MAX_LENGTH)
              // length is for a signed integer
              .setUnsignedAttribute(false);
            break;
          case SMALLINT:
            // 6 (range -32,768 to 32,767)
            type
              .setMaxPrecision(SMALLINT_SIGNED_MAX_LENGTH)
              // length is for a signed integer
              .setUnsignedAttribute(false);
            break;
          case TINYINT:
            // 4 (range -128 to 127, or 0 to 255 unsigned)
            type
              .setMaxPrecision(TINYINT_SIGNED_MAX_LENGTH)
              // length is for a signed integer
              .setUnsignedAttribute(false);
            break;
        }
        break;
      /**
       * Data Types
       * Expecting a max precision but no scale
       */
      // char
      case VARCHAR:
      case CHAR:
      case NCHAR:
      case NVARCHAR:
      case LONGVARCHAR:
        // binary
      case BIT:
      case VARBINARY:
      case LONGVARBINARY:
        // time
      case TIMESTAMP:
      case TIME:
      case TIME_WITH_TIMEZONE:
      case TIMESTAMP_WITH_TIMEZONE:
      case FLOAT:
        // no scale parameter
        type
          .setMinimumScale(0)
          .setMaximumScale(0);
        // Default precision value
        switch (typeCode) {
          case TIME:
          case TIMESTAMP:
          case TIMESTAMP_WITH_TIMEZONE:
          case TIME_WITH_TIMEZONE:
            type
              // implicit zero
              // https://datacadamia.com/data/type/relation/sql/time
              .setDefaultPrecision(0)
              // 6 max is postgres default https://www.postgresql.org/docs/9.1/datatype-datetime.html
              .setMaxPrecision(6)
              .setMandatorySpecifier(false);
            break;
          case CHAR:
            type
              .setDefaultPrecision(this.getDefaultCharLength())
              .setMandatorySpecifier(false);
            break;
          case NCHAR:
            type
              .setDefaultPrecision(this.getDefaultNcharLength())
              .setMandatorySpecifier(false);
            break;
          case VARCHAR:
            type
              .setDefaultPrecision(this.getDefaultVarcharLength())
              // not always SQL server default to 1
              .setMandatorySpecifier(true);
            break;
          case NVARCHAR:
            type
              .setDefaultPrecision(this.getDefaultNVarcharLength())
              // not always SQL server default to 1
              .setMandatorySpecifier(true);
            break;
          case FLOAT:
            // not precise numeric
            type.setAutoIncrement(false);
            break;
        }
        break;
      /**
       * Data Types
       * Expecting a maximum precision and scale
       */
      case NUMERIC:
      case DECIMAL:
        break;
    }

    return type;
  }

  public SqlDataType.SqlDataTypeBuilder<?> createTypeBuilder(KeyNormalizer typeName, SqlDataTypeAnsi sqlDataTypeAnsi) {
    SqlDataTypeKey sqlTypeKey = new SqlDataTypeKey(connection, typeName, sqlDataTypeAnsi.getVendorTypeNumber());
    return createTypeBuilder(sqlTypeKey, sqlDataTypeAnsi.getValueClass())
      .setAnsiType(sqlDataTypeAnsi);
  }

  public SqlDataType<?> getSqlDataType(SqlDataTypeKeyInterface type) {

    this.buildIfNeeded();

    if (type instanceof SqlDataTypeAnsi) {
      return this.ansiTypeSqlTypeBuildMap.get(type);
    }

    SqlDataType<?> sqlDataType = getSqlDataType(type.toKeyNormalizer(), type.getVendorTypeNumber());
    if (sqlDataType != null) {
      return sqlDataType;
    }

    SqlDataTypeAnsi ansi = SqlDataTypeAnsi.cast(type.toKeyNormalizer(), type.getVendorTypeNumber());
    return ansiTypeSqlTypeBuildMap.get(ansi);

  }

  /**
   * Add the name and short name of the {@link SqlDataTypeAnsi}
   */
  public SqlDataType.SqlDataTypeBuilder<?> createTypeBuilder(SqlDataTypeAnsi sqlDataTypeAnsi) {
    SqlDataTypeKey sqlTypeKey = new SqlDataTypeKey(connection, sqlDataTypeAnsi.toKeyNormalizer(), sqlDataTypeAnsi.getVendorTypeNumber());
    return createTypeBuilder(sqlTypeKey, sqlDataTypeAnsi.getValueClass())
      .setAnsiType(sqlDataTypeAnsi)
      .setPriority(sqlDataTypeAnsi.getPriority());
  }

  public SqlDataType.SqlDataTypeBuilder<?> createTypeBuilder(SqlDataTypeVendor vendorType) {
    SqlDataTypeKey sqlTypeKey = new SqlDataTypeKey(connection, vendorType.toKeyNormalizer(), vendorType.getVendorTypeNumber());
    return createTypeBuilder(sqlTypeKey, vendorType.getValueClass())
      .setAnsiType(vendorType.getAnsiType())
      .setMaxPrecision(vendorType.getMaxPrecision())
      .setMaximumScale(vendorType.getMaximumScale())
      .setDefaultPrecision(vendorType.getDefaultPrecision())
      .addChildAliases(vendorType.getAliases());
  }

  private SqlDataType.SqlDataTypeBuilder<?> getTypeBuilder(SqlDataTypeKey sqlDataTypeKey) {
    return typeNameTypeBuilderBuilderMap.get(sqlDataTypeKey);
  }


  public SqlDataTypeManager addJavaClassToTypeRelation(Class<?> javaClass, SqlDataTypeKey typeName) {
    this.javaClassTypeNameBuilderMap.put(javaClass, typeName);
    return this;
  }

  public SqlDataTypeManager addJavaClassToTypeRelation(Class<?> javaClass, KeyInterface typeName) {
    SqlDataType.SqlDataTypeBuilder<?> builder = getTypeBuilder(typeName.toKeyNormalizer());
    if (builder == null) {
      throw new InternalException("No type found with the name (" + typeName + ") for the connection (" + this.connection + ")");
    }
    return addJavaClassToTypeRelation(javaClass, builder.getTypeKey());
  }


  public SqlDataType.SqlDataTypeBuilder<?> getTypeBuilder(KeyNormalizer keyNormalizer, int typeCode) {
    if (typeCode == 0) {
      return getTypeBuilder(keyNormalizer);
    }
    SqlDataTypeKey sqlDataTypeKey = new SqlDataTypeKey(connection, keyNormalizer, typeCode);
    return getTypeBuilder(sqlDataTypeKey);
  }

  public SqlDataTypeManager addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi typeCode, SqlDataTypeKey sqlTypeKey) {

    return addTypeCodeTypeNameMapEntry(typeCode, (SqlDataTypeKeyInterface) sqlTypeKey);
  }

  public SqlDataTypeManager addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi typeCode, SqlDataTypeKeyInterface sqlTypeKey) {
    SqlDataType.SqlDataTypeBuilder<?> builder = getTypeBuilder(sqlTypeKey);
    if (builder == null) {
      throw new InternalException("Bad type code, type mapping. The type (" + sqlTypeKey + ") is unknown / not defined for the connection (" + this.connection + "), we can't add a mapping for the type code (" + typeCode + "). Verify the type building definition.");
    }
    this.ansiTypeTypeNameBuilderMap.put(typeCode, builder);
    return this;
  }

  public SqlDataTypeManager addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi typeCode, KeyNormalizer keyNormalizer) {
    SqlDataType.SqlDataTypeBuilder<?> typeBuilder = getTypeBuilder(keyNormalizer);
    if (typeBuilder == null) {
      throw new InternalException("No type found with the name " + keyNormalizer);
    }
    this.ansiTypeTypeNameBuilderMap.put(typeCode, typeBuilder);
    return this;
  }

  public SqlDataType.SqlDataTypeBuilder<?> getTypeBuilder(KeyNormalizer keyNormalizer) {
    List<SqlDataType.SqlDataTypeBuilder<?>> typeKeys = typeNameTypeBuilderBuilderMap.values()
      .stream()
      .filter(d -> d.getName().equals(keyNormalizer))
      .collect(Collectors.toList());
    if (typeKeys.isEmpty()) {
      return null;
    }
    if (typeKeys.size() > 1) {
      throw new IllegalStateException("More than one type key found for " + keyNormalizer + " (" + typeKeys + ")");
    }
    return typeKeys.get(0);
  }

  public SqlDataType.SqlDataTypeBuilder<?> createTypeBuilder(String name, SqlDataTypeAnsi sqlDataTypeAnsi) {
    KeyNormalizer nameNormalized = KeyNormalizer.createSafe(name);
    return createTypeBuilder(nameNormalized, sqlDataTypeAnsi);
  }


  public SqlDataType.SqlDataTypeBuilder<?> createTypeBuilder(KeyInterface typeName, SqlDataTypeAnsi sqlDataTypeAnsi) {
    return createTypeBuilder(typeName.toKeyNormalizer(), sqlDataTypeAnsi);
  }

  public Integer getDefaultVarcharLength() {

    Attribute attribute = this.connection.getAttribute(ConnectionAttributeEnumBase.VARCHAR_DEFAULT_PRECISION);
    Object valueOrDefault1 = attribute.getValueOrDefault();
    try {
      return Casts.cast(valueOrDefault1, Integer.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The value of the connection attribute (" + ConnectionAttributeEnumBase.VARCHAR_DEFAULT_PRECISION + ") for the connection (" + this + ") is not an integer. Value: " + valueOrDefault1 + ". Error: " + e.getMessage(), e);
    }

  }

  public Integer getDefaultNVarcharLength() {

    Attribute attribute = this.connection.getAttribute(ConnectionAttributeEnumBase.NVARCHAR_DEFAULT_PRECISION);
    Object valueOrDefault1 = attribute.getValueOrDefault();
    try {
      return Casts.cast(valueOrDefault1, Integer.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The value of the connection attribute (" + ConnectionAttributeEnumBase.NVARCHAR_DEFAULT_PRECISION + ") for the connection (" + this + ") is not an integer. Value: " + valueOrDefault1 + ". Error: " + e.getMessage(), e);
    }

  }

  public Integer getDefaultNcharLength() {

    Attribute attribute = this.connection.getAttribute(ConnectionAttributeEnumBase.NCHAR_DEFAULT_PRECISION);
    Object valueOrDefault1 = attribute.getValueOrDefault();
    try {
      return Casts.cast(valueOrDefault1, Integer.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The value of the connection attribute (" + ConnectionAttributeEnumBase.NCHAR_DEFAULT_PRECISION + ") for the connection (" + this + ") is not an integer. Value: " + valueOrDefault1 + ". Error: " + e.getMessage(), e);
    }

  }

  public Integer getDefaultCharLength() {

    Attribute attribute = this.connection.getAttribute(ConnectionAttributeEnumBase.CHAR_DEFAULT_PRECISION);
    Object valueOrDefault1 = attribute.getValueOrDefault();
    try {
      return Casts.cast(valueOrDefault1, Integer.class);
    } catch (CastException e) {
      throw new IllegalArgumentException("The value of the connection attribute (" + ConnectionAttributeEnumBase.CHAR_DEFAULT_PRECISION + ") for the connection (" + this + ") is not an integer. Value: " + valueOrDefault1 + ". Error: " + e.getMessage(), e);
    }

  }

  public SqlDataTypeManager addTypeCodeTypeNameMapEntry(SqlDataTypeAnsi typeCode, SqlDataTypeAnsi sqlDataTypeAnsi) {
    SqlDataTypeKey sqlDataTypeKey = new SqlDataTypeKey(connection, sqlDataTypeAnsi.toKeyNormalizer(), sqlDataTypeAnsi.getVendorTypeNumber());
    addTypeCodeTypeNameMapEntry(typeCode, sqlDataTypeKey);
    return this;
  }


  /**
   * Why only by name and not by {@link SqlDataTypeKey} (ie with name + type code)
   * Because in
   * * a manifest, the name is the identifier
   * * for some database, the name is the identifier
   */
  public SqlDataType<?> getSqlDataType(KeyNormalizer typeName) {

    this.buildIfNeeded();

    List<SqlDataType<?>> types = this.sqlDataTypesByKeyBuildMap.values()
      .stream()
      .filter(d -> d.toKeyNormalizer().equals(typeName))
      .collect(Collectors.toList());
    if (types.isEmpty()) {
      /**
       * Alias
       */
      types = this.sqlDataTypesByKeyBuildMap.values()
        .stream()
        .filter(d -> d.getChildrenAlias().stream().map(SqlDataType::toKeyNormalizer).collect(Collectors.toSet()).contains(typeName))
        .collect(Collectors.toList());
      if (types.isEmpty()) {
        /**
         * Standard Type
         */
        types = this.sqlDataTypesByKeyBuildMap.values()
          .stream()
          .filter(d -> d.getAnsiType().toKeyNormalizer().equals(typeName))
          .collect(Collectors.toList());
        if (types.isEmpty()) {
          /**
           * Standard Type common name
           */
          types = this.sqlDataTypesByKeyBuildMap.values()
            .stream()
            .filter(d -> d.getAnsiType().getNormalizedAliases().contains(typeName))
            .collect(Collectors.toList());
          if (types.isEmpty()) {
            return null;
          }
        }
      }
    }
    if (types.size() > 1) {
      if (connection.getTabular().isStrictExecution()) {
        throw new StrictException("The type name (" + typeName + ") returns 2 types. In non-strict mode, we will return the first one. The types: " + (types.stream().map(SqlDataType::toString).collect(Collectors.joining(", "))));
      }
    }
    return types.get(0);
  }

  public SqlDataType.SqlDataTypeBuilder<?> getTypeBuilder(SqlDataTypeKeyInterface sqlDataType) {
    return getTypeBuilder(sqlDataType.toKeyNormalizer(), sqlDataType.getVendorTypeNumber());
  }


  public SqlDataType.SqlDataTypeBuilder<?> getTypeBuilder(String typeName) {
    return getTypeBuilder(KeyNormalizer.createSafe(typeName));
  }


  public <T> SqlDataType.SqlDataTypeBuilder<T> getTypeBuilder(KeyNormalizer childName, Class<T> javaClazz) {
    SqlDataType.SqlDataTypeBuilder<?> typeBuilder = getTypeBuilder(childName);
    if (typeBuilder == null) {
      return null;
    }
    if (typeBuilder.getJavaClass() != javaClazz) {
      throw new InternalException("The retrieved sql type " + childName + " has not the same java type (" + typeBuilder.getJavaClass().getName() + ") as the asked java type (" + javaClazz.getName() + ")");
    }
    //noinspection unchecked
    return ((SqlDataType.SqlDataTypeBuilder<T>) typeBuilder);
  }

}
