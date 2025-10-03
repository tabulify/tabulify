package com.tabulify.spi;

import com.tabulify.DbLoggers;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.AttributeEnum;
import com.tabulify.conf.Origin;
import com.tabulify.connection.Connection;
import com.tabulify.engine.StreamDependencies;
import com.tabulify.model.*;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferOperation;
import com.tabulify.transfer.TransferPropertiesSystem;
import com.tabulify.uri.DataUriNode;
import net.bytle.dag.Dependency;
import net.bytle.exception.*;
import net.bytle.type.*;

import java.nio.file.NoSuchFileException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.util.*;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Collectors;

import static com.tabulify.spi.DataPathAttribute.*;


public abstract class DataPathAbs implements Comparable<DataPath>, StreamDependencies, DataPath, Dependency {


  private final Connection connection;


  /**
   * A compact path identifier known as the {@link DataPathAttribute#PATH}
   * * for a file system, this is the relative path from the connection base directory
   * * for a SQL system, this is the sql path qualified if not in the current schema
   * <p>
   * This value can only be null if the {@link #executableDataPath} is not
   * We call it the compact path to make a difference with the {@link #getAbsolutePath()}
   */
  private final String compactConnectionPath;

  /**
   * The data path of an executable
   * (ie code or binary)
   * (If this value is null, the {@link #compactConnectionPath} should not)
   */
  protected final DataPath executableDataPath;

  protected MediaType mediaType;

  /**
   * We build data uri and id has final because
   * it's in the {@link #hashCode()}
   * and we got error on the Set#contains
   */
  private final DataUriNode dataUri;
  private final String id;
  private TabularType tabularType;

  @Override
  public Connection getConnection() {
    return this.connection;
  }

  /**
   * Variable may be created dynamically
   * (ie backref of a regexp for instance)
   * So we need string as key identifier
   */
  private final Map<KeyNormalizer, Attribute> attributes = new HashMap<>();


  @Override
  public String getCompactPath() {
    return this.compactConnectionPath;
  }


  protected RelationDef relationDef;
  private String description;


  @Override
  public String getName() {
    throw new InternalException("A name is mandatory. This function should be overridden and returns a value");
  }

  private void buildAttachVariableFunctions() {
    this.addVariablesFromEnumAttributeClass(DataPathAttribute.class);
    this.getOrCreateVariable(PATH).setValueProvider(this::getName);
    this.getOrCreateVariable(CONNECTION).setValueProvider(() -> this.getConnection().getName());
    this.getOrCreateVariable(DATA_URI).setValueProvider(this::toDataUri);
    this.getOrCreateVariable(PATH).setValueProvider(this::getCompactPath);
    this.getOrCreateVariable(ABSOLUTE_PATH).setValueProvider(this::getAbsolutePath);
    this.getOrCreateVariable(PARENT).setValueProvider(() -> {
      try {
        return this.getParent().getLogicalName();
      } catch (NoParentException e) {
        return null;
      }
    });
    this.getOrCreateVariable(MEDIA_TYPE).setValueProvider(() -> this.getMediaType().toString());
    this.getOrCreateVariable(MEDIA_SUBTYPE).setValueProvider(() -> this.getMediaType().getSubType());
    this.getOrCreateVariable(KIND).setValueProvider(() -> this.getMediaType().getKind());
    this.getOrCreateVariable(NAME).setValueProvider(this::getName);
    this.getOrCreateVariable(COUNT).setValueProvider(this::getCount);
    this.getOrCreateVariable(SIZE).setValueProvider(this::getSize);
    this.getOrCreateVariable(MD5).setValueProvider(() -> {
      try {
        return Bytes.printHexBinary(this.getByteDigest("MD5")).toLowerCase();
      } catch (NoSuchFileException e) {
        return "";
      }
    });
    this.getOrCreateVariable(SHA384).setValueProvider(() -> {
      try {
        return Bytes.printHexBinary(this.getByteDigest("SHA-384")).toLowerCase();
      } catch (NoSuchFileException e) {
        return "";
      }
    });

    this.getOrCreateVariable(SHA384_INTEGRITY).setValueProvider(() -> {
      try {
        return "sha384-" + Base64.getEncoder().encodeToString((this.getByteDigest("SHA-384")));
      } catch (NoSuchFileException e) {
        return "";
      }
    });
    this.getOrCreateVariable(LOGICAL_NAME).setValueProvider(this::getDefaultLogicalName);
    this.getOrCreateVariable(ACCESS_TIME).setValueProvider(this::getAccessTime);
    this.getOrCreateVariable(UPDATE_TIME).setValueProvider(this::getUpdateTime);
    this.getOrCreateVariable(CREATION_TIME).setValueProvider(this::getCreationTime);
    this.getOrCreateVariable(TABULAR_TYPE).setValueProvider(this::getTabularType);
  }


  public Attribute getOrCreateVariable(AttributeEnum attribute) {

    try {

      return this.getAttribute(attribute);

    } catch (NoVariableException e) {

      Attribute variable = Attribute.create(attribute, Origin.DEFAULT);
      this.addAttribute(variable);
      return variable;

    }
  }

  /**
   * A data path may represent:
   * * static data (stored on disk), located via the relative path
   * * runtime data (created by execution), created with the code data path
   * <p>
   * Why do we have only one data path object and not 2 that represents respectively
   * a static and a runtime data resource ?
   * Because an executable data path may extend a static data path so that
   * it does not need to implement the path functions and use an anonymous name.
   * <p>
   * If you want to pass some dynamic code, create a memory data path and store the code in it
   * Why? We get:
   * * a runtime identifier (name@memory)@connection
   * * a name for the code
   *
   * @param connection         - the connection
   * @param compactPath        - the relative path of a static resource from the connection path  (null if executableDataPath is NOT null)
   * @param executableDataPath - the executable data path of a runtime resource that contains the execution code or is a binary (null if relative path is not null)
   * @param mediaType          - the media type
   */
  public DataPathAbs(Connection connection, String compactPath, DataPath executableDataPath, MediaType mediaType) {

    Objects.requireNonNull(connection, "The connection should not be null");
    Objects.requireNonNull(mediaType, "The media type should not be null");
    this.connection = connection;
    this.mediaType = mediaType;

    if (compactPath == null && executableDataPath == null) {
      throw new InternalException("The compact path and executable data path should not be null together, we need at least an identifier");
    }

    if (compactPath != null && executableDataPath != null) {
      throw new InternalException("The compact path and executable data path should not be NON null together. Compact path: " + compactPath + ", Executable Path: " + executableDataPath);
    }

    if (executableDataPath != null) {
      this.compactConnectionPath = null;
      this.executableDataPath = executableDataPath;
      this.dataUri = DataUriNode.builder()
        .setConnection(this.connection)
        .setPathDataUri(this.executableDataPath.toDataUri())
        .build();
    } else {
      this.compactConnectionPath = compactPath;
      this.executableDataPath = null;
      this.dataUri = DataUriNode.builder()
        .setConnection(this.connection)
        .setPath(this.compactConnectionPath)
        .build();
    }


    this.id = this.dataUri.toString();

    /**
     * Init the arguments
     */
    this.buildAttachVariableFunctions();

  }


  @Override
  public String getId() {
    return this.dataUri.toString();
  }

  public int compareTo(DataPath o) {
    return (Sorts.naturalSortComparator(this.getConnection().getName() + this.getCompactPath(), o.getConnection().getName() + o.getCompactPath()));
  }

  @Override
  public String toString() {
    return getId();
  }


  @Override
  public RelationDef getOrCreateRelationDef() {
    if (this.relationDef == null) {
      this.relationDef = createRelationDef();
    }
    return this.relationDef;
  }


  @Override
  public RelationDef createRelationDef() {
    this.relationDef = new RelationDefDefault(this);
    return relationDef;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataPathAbs dataPath = (DataPathAbs) o;
    return getId().equals(dataPath.getId());
  }


  @Override
  public int hashCode() {
    return Objects.hash(getId());
  }


  /**
   * @return the dependency
   * (Example:
   * * for foreign key dependencies, the primary key table referenced by the foreign key relationship
   * ie a fact table is dependent of its dimensions
   * * for a query, the tables that the query references
   * )
   * <p>
   * This function returns only the foreign key dependencies
   * Every datastore should overwrite this function if it wants to add other dependencies
   */
  @Override
  public Set<DataPath> getDependencies() {

    List<ForeignKeyDef> foreignKeys = new ArrayList<>();
    if (this.getOrCreateRelationDef() != null) {
      foreignKeys = this.getOrCreateRelationDef().getForeignKeys();
    }
    Set<DataPath> parentDataPaths = new HashSet<>();
    if (!foreignKeys.isEmpty()) {

      for (ForeignKeyDef foreignKeyDef : foreignKeys) {
        parentDataPaths.add(foreignKeyDef.getForeignPrimaryKey().getRelationDef().getDataPath());
      }

    }
    return parentDataPaths;

  }


  /**
   * @param description - set a description (to be able to label queries)
   * @return the object for chaining
   */
  @Override
  public DataPath setComment(String description) {
    this.description = description;
    return this;
  }


  /**
   * @return the description
   */
  @Override
  public String getComment() {
    return this.description;
  }


  @Override
  public String getLogicalName() {

    try {
      return (String) this.getAttribute(LOGICAL_NAME).getValueOrDefault();
    } catch (NoVariableException e) {
      return this.getDefaultLogicalName();
    }

  }

  private String getDefaultLogicalName() {
    if (this.isRuntime()) {
      return this.getExecutableDataPath().getLogicalName();
    }
    return getName();
  }

  @Override
  public DataPath getExecutableDataPath() {
    return this.executableDataPath;
  }


  @Override
  public Attribute getAttribute(KeyNormalizer name) throws NoVariableException {
    Attribute attribute = this.attributes.get(name);
    if (attribute == null) {
      throw new NoVariableException();
    }
    return attribute;
  }

  @Override
  public SelectStream getSelectStreamSafe() {

    if (this.isRuntime()) {
      DataPath execute;
      try {
        execute = execute();
      } catch (Exception e) {
        throw new RuntimeException("Error during execution of the data path " + this + ". Error: " + e.getMessage(), e);
      }
      try {
        return execute.getSelectStream();
      } catch (SelectException e) {
        throw new RuntimeException("We were unable to open the result data path " + execute + " of the runtime " + this + ". Error: " + e.getMessage(), e);
      }
    }
    try {
      return getSelectStream();
    } catch (SelectException e) {
      throw new RuntimeException("We were unable to open the data path " + this + ". Error: " + e.getMessage(), e);
    }

  }

  @Override
  public DataPath getSelectStreamDependency() throws NotFoundException {
    throw new NotFoundException("No select stream dependency found");
  }

  @Override
  public RelationDef getRelationDef() {
    return this.relationDef;
  }

  @Override
  public DataUriNode toDataUri() {
    return this.dataUri;
  }


  /**
   * Return the digest
   *
   * @param algorithm the cipher
   * @return the byte
   */
  @Override
  public byte[] getByteDigest(String algorithm) throws NoSuchFileException {
    try {
      MessageDigest digest = MessageDigest.getInstance(algorithm);
      SelectStream selectStream;
      try {
        selectStream = this.getSelectStream();
      } catch (SelectException e) {
        boolean isStrict = this.getConnection().getTabular().isStrictExecution();
        String message = "Error while trying to get the byte digest";
        if (isStrict) {
          throw new RuntimeException(message, e);
        } else {
          DbLoggers.LOGGER_DB_ENGINE.warning(message + "\n" + e.getMessage());
          return new byte[]{};
        }
      }
      while (selectStream.next()) {
        for (Object object : selectStream.getObjects()) {
          if (object == null) {
            object = "null";
          }
          digest.update(object.toString().getBytes());
        }
      }
      return digest.digest();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public boolean isRuntime() {
    if (this.executableDataPath != null) {
      return true;
    }
    return this.mediaType.isRuntime();
  }

  /**
   * Add a property for this table def
   *
   * @param key   - the key
   * @param value - the attribute value
   * @return the tableDef for initialization chaining
   */
  public DataPath addAttribute(KeyNormalizer key, Object value) {

    Attribute attribute = null;

    /**
     * Special case of regexp backreference
     * ie $1, $2, ...
     */
    try {
      Integer keyInteger = Casts.cast(key.toString(), Integer.class);
      attribute = Attribute.create(keyInteger.toString(), Origin.DEFAULT)
        .setPlainValue(value);
    } catch (CastException e) {
      //
    }

    /**
     * Normal attribute
     */
    if (attribute == null) {
      DataPathAttribute dataPathAttribute;
      try {
        dataPathAttribute = Casts.cast(key, DataPathAttribute.class);
      } catch (CastException e) {
        String expectedKeyAsString = getAttributes().stream()
          .map(Attribute::getAttributeMetadata)
          .filter(AttributeEnum::getIsUpdatable)
          .map(AttributeEnum::getKeyNormalized)
          .sorted()
          .map(KeyNormalizer::toCliLongOptionName)
          .collect(Collectors.joining(", "));
        throw new IllegalArgumentException("The data path attribute (" + key.toCliLongOptionName() + ") is unknown for the resource (" + this + ", " + getMediaType() + ")" + ". We were expecting one of:" + System.lineSeparator() + expectedKeyAsString);
      }

      try {
        attribute = this.getConnection().getTabular().getVault()
          .createVariableBuilderFromAttribute(dataPathAttribute)
          .setOrigin(Origin.MANIFEST)
          .build(value);

      } catch (CastException e) {
        throw new IllegalArgumentException("The value (" + value + ") is not conform for the common resource attribute (" + dataPathAttribute + "). Error: " + e.getMessage(), e);
      }
    }
    this.addAttribute(attribute);
    return this;
  }


  @Override
  public DataPath addAttribute(AttributeEnum key, Object value) {
    /**
     * We redirect so that the attribute is checked
     */
    return addAttribute(key.getKeyNormalized(), value);
  }

  /**
   * Property value are generally given via a {@link DataDefManifest data definition file}
   *
   * @return the properties value of this table def
   */
  @Override
  public Set<Attribute> getAttributes() {
    return new HashSet<>(attributes.values());
  }


  @Override
  public DataPath mergeDataPathAttributesFrom(DataPath source) {
    Set<Attribute> attributes = source.getAttributes();
    for (Attribute attribute : attributes) {
      if (attribute.isValueProviderValue()) {
        /**
         * We don't copy the value provider otherwise
         * We get the function against the source and not against the target
         * For example, {@link DataPathAbs} initialize the attribute with the {@link DataPath#getName}
         */
        continue;
      }
      this.addAttribute(attribute);
    }
    return this;
  }


  @Override
  public DataPath mergeDataDefinitionFromYamlMap(Map<KeyNormalizer, ?> document) {

    List<String> primaryColumns = null;

    // Loop through all others properties
    for (Map.Entry<KeyNormalizer, ?> entry : document.entrySet()) {

      DataPathAttribute dataPathAttribute;
      try {
        dataPathAttribute = Casts.cast(entry.getKey(), DataPathAttribute.class);
      } catch (Exception e) {
        /**
         * This is an attribute, not a common attribute
         */
        this.addAttribute(entry.getKey(), entry.getValue());
        continue;
      }
      switch (dataPathAttribute) {
        case LOGICAL_NAME:
          Object logicalName = entry.getValue();
          if (logicalName == null) {
            DbLoggers.LOGGER_DB_ENGINE.warning("The yaml key `" + LOGICAL_NAME + "` does not have any value for the data resource (" + this + ")");
            continue;
          }
          this.setLogicalName(logicalName.toString());
          continue;
        case TABULAR_TYPE:
          Object tabularType = entry.getValue();
          if (tabularType == null) {
            DbLoggers.LOGGER_DB_ENGINE.warning("The yaml key `" + TABULAR_TYPE + "` does not have any value for the data resource (" + this + ")");
            continue;
          }
          try {
            this.setTabularType(Casts.cast(tabularType, TabularType.class));
          } catch (CastException e) {
            throw IllegalArgumentExceptions.createFromMessageWithPossibleValues(
              "The tabular type value (" + tabularType + ") is not conform",
              TabularType.class,
              e
            );
          }
          continue;
        case MEDIA_TYPE:
          // To avoid the warning in the default branch below
          // type should be given at construction time but is part
          // of the attributes, we know that. This is no error or warning.
          continue;
        case PRIMARY_COLUMNS:
          Object primaryColumnsValue = entry.getValue();
          if (!(primaryColumnsValue instanceof List)) {
            throw new RuntimeException("The primary columns are not a list but a " + primaryColumnsValue.getClass());
          }
          primaryColumns = Casts.castToNewListSafe(primaryColumnsValue, String.class);
          continue;
        case COLUMNS:
          List<Object> columns;
          // To verify that there is only one column definition by column name
          Set<String> columnsFound = new HashSet<>();
          try {
            Object columnValues = entry.getValue();
            if (columnValues == null) {
              DbLoggers.LOGGER_DB_ENGINE.warning("The yaml key `" + COLUMNS + "` does not have any columns definitions for the data resource (" + this + ")");
              continue;
            }
            columns = Casts.castToNewList(columnValues, Object.class);
          } catch (CastException e) {
            String message = "The columns must be in a list format. ";
            message += "They are in the following format:" + (entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : null);
            message += "Bad Columns Values are: " + entry.getValue();
            throw new RuntimeException(message, e);
          }
          /**
           * {@link DataPath#getOrCreateRelationDef() Get or create}
           * and not {@link DataPath#createEmptyRelationDef()}
           * because it is almost always an additive merge
           * Example: ie
           * * enrich will add columns
           * * csv definition may be not complete
           */
          RelationDef relationDef = this.getOrCreateRelationDef();
          int columnCount = 0;
          for (Object column : columns) {
            columnCount++;
            try {

              /**
               * List of string (ie column name)
               */
              if (column instanceof String) {

                /**
                 * Only one definition by column name
                 * If this is not the case, through because this is never what you want
                 */
                String columnName = (String) column;
                if (columnsFound.contains(columnName)) {
                  throw new IllegalStateException("The column (" + columnName + ") has already be defined. Delete one of the column definition or merge them.");
                } else {
                  columnsFound.add(columnName);
                }


                if (!relationDef.hasColumn(columnName)) {
                  relationDef.getOrCreateColumn(columnName, String.class);
                }
                continue;
              }

              /**
               * A map of column properties
               */
              Map<String, Object> columnProperties;
              try {
                columnProperties = Casts.castToSameMap(column, String.class, Object.class);
              } catch (CastException e) {
                throw new IllegalArgumentException("The expected column properties format should be a map of string/object. It's a " + column.getClass().getSimpleName() + ". Error: " + e.getMessage(), e);
              }

              Object columnNameObject = Maps.getPropertyCaseIndependent(columnProperties, ColumnAttribute.NAME.toString());
              if (columnNameObject == null) {
                throw new IllegalArgumentException("The name property for a column is mandatory and was not found for the column (" + columnCount + ")");
              }
              String columnName;
              try {
                columnName = (String) columnNameObject;
              } catch (ClassCastException e) {
                throw new IllegalArgumentException("The name property of the column (" + columnCount + ") is not a string but a " + columnNameObject.getClass().getSimpleName());
              }
              /**
               * Only one definition by column name
               * If this is not the case, through because this is never what you want
               */
              if (columnsFound.contains(columnName)) {
                throw new IllegalStateException("The column (" + columnName + ") has already be defined. Delete one of the column definition or merge them.");
              } else {
                columnsFound.add(columnName);
              }

              /**
               * Determination of the type of the column
               */
              KeyNormalizer type = SqlDataTypeAnsi.CHARACTER_VARYING.toKeyNormalizer();
              Object manifestType = Maps.getPropertyCaseIndependent(columnProperties, ColumnAttribute.TYPE.toString());
              if (manifestType != null) {
                try {
                  type = KeyNormalizer.create(manifestType);
                } catch (CastException e) {
                  throw new IllegalArgumentException("The type (" + manifestType + ") of the column (" + columnName + ") is not a valid identifier. Error: " + e.getMessage(), e);
                }
              }
              Object manifestAnsiType = Maps.getPropertyCaseIndependent(columnProperties, ColumnAttribute.ANSI_TYPE.toString());
              SqlDataTypeAnsi sqlDataTypeAnsi = null;
              if (manifestAnsiType != null) {
                KeyNormalizer typeName;
                try {
                  typeName = KeyNormalizer.create(manifestAnsiType.toString());
                } catch (CastException e) {
                  throw new IllegalArgumentException("The type (" + manifestAnsiType + ") of the column (" + columnName + ") is not a valid identifier. Error: " + e.getMessage(), e);
                }
                SqlDataTypeAnsi.cast(typeName, null);
              }
              SqlDataType<?> sqlDataType = this.getConnection().getSqlDataType(type, sqlDataTypeAnsi);
              if (sqlDataType == null) {
                String sqlNames = this.getConnection().getSqlDataTypes().stream().map(SqlDataType::toKeyNormalizer)
                  .map(Object::toString)
                  .sorted()
                  .collect(Collectors.joining(", "));
                throw new IllegalStateException("The type (" + type + ") of the column (" + columnName + ") is unknown for the connection (" + this.getConnection() + "). Possible values: " + sqlNames);
              }


              /**
               * Column building
               */
              ColumnDef<?> columnDef;
              try {
                columnDef = relationDef.getColumnDef(columnName);
                // if the type is defined in the manifest, we need to override the actual
                // we are missing a building system here but yeah
                if (manifestType != null) {
                  columnDef = relationDef.createColumn(columnName, sqlDataType)
                    .setColumnPosition(columnDef.getColumnPosition())
                    .setNullable(columnDef.isNullable())
                    .setComment(columnDef.getComment())
                    .setPrecision(columnDef.getPrecision())
                    .setScale(columnDef.getScale())
                    .setIsAutoincrement(columnDef.isAutoincrement())
                    .setIsGeneratedColumn(columnDef.isGeneratedColumn());
                }
              } catch (NoColumnException e) {
                /**
                 * In the case of the text file, all data type are varchar
                 * We are just creating a new column for now
                 */
                columnDef = relationDef.createColumn(columnName, sqlDataType);
              }


              for (Map.Entry<String, Object> columnProperty : columnProperties.entrySet()) {
                ColumnAttribute columnAttribute;
                Object value = columnProperty.getValue();
                try {
                  columnAttribute = Casts.cast(columnProperty.getKey(), ColumnAttribute.class);
                } catch (CastException arg) {
                  /**
                   * This is not a built-in attribute
                   */
                  columnDef.setVariable(columnProperty.getKey(), value);
                  continue;
                }
                switch (columnAttribute) {
                  case NAME:
                  case TYPE:
                  case ANSI_TYPE:
                    // already done during the creation
                    break;
                  case PRECISION:
                    if (columnDef.getPrecision() == 0) {
                      try {
                        columnDef.setPrecision(Casts.cast(value, Integer.class));
                      } catch (CastException e) {
                        throw new IllegalArgumentException("The precision value (" + value + ") is not a integer. Error: " + e.getMessage(), e);
                      }
                    }
                    break;
                  case SCALE:
                    if (columnDef.getScale() == 0) {
                      try {
                        columnDef.setScale(Casts.cast(value, Integer.class));
                      } catch (CastException e) {
                        throw new IllegalArgumentException("The scale value (" + value + ") is not a integer. Error: " + e.getMessage(), e);
                      }
                    }
                    break;
                  case COMMENT:
                    if (columnDef.getComment() == null) {
                      columnDef.setComment(String.valueOf(value));
                    }
                    break;
                  case NULLABLE:
                    if (columnDef.isNullable() == null) {
                      try {
                        columnDef.setNullable(Casts.cast(value, Boolean.class));
                      } catch (CastException e) {
                        throw new IllegalArgumentException("Nullable value (" + value + ") is not a boolean. Error: " + e.getMessage(), e);
                      }
                    }
                    break;
                  case POSITION:
                    try {
                      columnDef.setColumnPosition(Casts.cast(value, Integer.class));
                    } catch (CastException e) {
                      throw new IllegalArgumentException("Column position value (" + value + ") is not a integer. Error: " + e.getMessage(), e);
                    }
                    break;
                  default:
                    throw new MissingSwitchBranch("columnAttribute", columnAttribute);
                }

              }
            } catch (Exception e) {
              throw ExceptionWrapper.builder(e,
                  "Error on a property of the column (" + columnCount + ") from the data " +
                    "resource (" + this + ")")
                .setPosition(ExceptionWrapper.ContextPosition.FIRST)
                .buildAsRuntimeException();
            }
          }
          break;
        case COMMENT:
          this.setComment((String) entry.getValue());
          break;
        default:
          if (this.getConnection().getTabular().isStrictExecution()) {
            throw new StrictException("The data path attribute (" + dataPathAttribute + ") is not an attribute that can be modified on the data path (" + this + ")");
          }
          break;
      }

    }

    /**
     * Primary columns at the end
     */
    if (primaryColumns != null) {
      RelationDef relationDef = this.getOrCreateRelationDef();
      for (String columnName : primaryColumns) {

        try {
          relationDef.getColumnDef(columnName);
        } catch (NoColumnException e) {
          // If the columns do not exist
          relationDef.getOrCreateColumn(columnName, String.class);
        }
      }
      relationDef.setPrimaryKey(primaryColumns);
    }
    return this;
  }


  @Override
  public DataPath mergeDataDefinitionFrom(DataPath sourceDataPath) {
    return mergeDataDefinitionFrom(sourceDataPath, null);
  }

  @Override
  public DataPath mergeDataDefinitionFrom(DataPath sourceDataPath, Map<DataPath, DataPath> sourceTargetMap) {
    this.mergeDataPathAttributesFrom(sourceDataPath);
    this.getOrCreateRelationDef().mergeDataDef(sourceDataPath, sourceTargetMap);
    return this;
  }

  @Override
  public DataPath setLogicalName(String logicalName) {
    try {
      Attribute attribute = getConnection().getTabular().getVault().createAttribute(LOGICAL_NAME, logicalName, Origin.DEFAULT);
      this.addAttribute(attribute);
      return this;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public DataPath setTabularType(TabularType tabularType) {
    this.tabularType = tabularType;
    return this;
  }


  @Override
  public InsertStream getInsertStream() {
    /**
     * If the source is not defined, we expect the same structure as the target
     */
    return getInsertStream(this, TransferPropertiesSystem.builder().setOperation(TransferOperation.INSERT).build());
  }

  @Override
  public InsertStream getInsertStream(TransferPropertiesSystem transferProperties) {
    /**
     * If the source is not defined, we expect the same structure as the target
     */
    return getInsertStream(this, transferProperties);
  }

  @Override
  public DataPath toAttributesDataPath() {

    DataPath dataPath = this.getConnection().getTabular().getMemoryConnection()
      .getDataPath(this.getName() + "_variable")
      .setComment("Information about the data resource (" + this + ")");

    return toAttributesDataPath(dataPath);

  }

  /**
   * Add a variable.
   * It makes it possible to create a variable with a {@link Attribute#setValueProvider(Supplier)}
   */
  public DataPath addAttribute(Attribute attribute) {
    AttributeEnum attributeEnum = attribute.getAttributeMetadata();
    this.attributes.put(KeyNormalizer.createSafe(attributeEnum), attribute);
    /**
     * This conditional is for perf/debug reason, as the {@link #toString()}
     * is pretty expensive (ie the `this` in the string)
     * The problem with this code is that the code in the string gets executed even if the level is not finest
     */
    if (DbLoggers.LOGGER_DB_ENGINE.getLevel().intValue() <= Level.FINEST.intValue()) {
      DbLoggers.LOGGER_DB_ENGINE.finest("The variable (" + attribute + ") for the resource (" + this + ") was set to the value (" + Strings.createFromObjectNullSafe(attribute.getValueOrDefault()) + ")");
    }
    return this;
  }

  /**
   * A utility class to add the default variables when a path is build
   *
   * @param enumClass - the class that holds all enum attribute
   * @return the path for chaining
   */
  public DataPath addVariablesFromEnumAttributeClass(Class<? extends AttributeEnum> enumClass) {
    Arrays.asList(enumClass.getEnumConstants()).forEach(c -> this.addAttribute(Attribute.create(c, Origin.DEFAULT)));
    return this;
  }

  @Override
  public MediaType getMediaType() {
    return this.mediaType;
  }


  @Override
  public DataPath resolve(String name) {
    return resolve(name, null);
  }

  @Override
  public RelationDef createEmptyRelationDef() {

    this.relationDef = new RelationDefDefault(this);
    return this.relationDef;

  }

  @Override
  public Timestamp getAccessTime() {
    return getAttributeValueIfNotProvider(ACCESS_TIME);
  }

  @Override
  public Timestamp getCreationTime() {
    return getAttributeValueIfNotProvider(CREATION_TIME);
  }

  /**
   * Utility function to return an attribute value
   * if it's not a function to not recurse
   * System such as memory, just set the value as attribute
   */
  private Timestamp getAttributeValueIfNotProvider(DataPathAttribute dataPathAttribute) {
    // unknown by default
    try {
      Attribute attribute = this.getAttribute(dataPathAttribute);
      if (attribute.isValueProviderValue()) {
        return null;
      }
      return (Timestamp) attribute.getValueOrNull();
    } catch (NoVariableException e) {
      return null;
    }
  }


  @Override
  public Timestamp getUpdateTime() {
    return getAttributeValueIfNotProvider(UPDATE_TIME);
  }

  @Override
  public List<List<?>> getRecords() {
    List<List<?>> records = new ArrayList<>();
    try (SelectStream selectStream = this.getSelectStreamSafe()) {
      while (selectStream.next()) {
        records.add(selectStream.getObjects());
      }
    }
    return records;
  }

  @Override
  public DataPath execute() {
    if (this.isRuntime()) {
      throw new InternalException("The data path (" + this + ", Media Type: " + this.getMediaType() + ") is not yet implemented as self-executable runtime");
    }
    throw new IllegalArgumentException("The data path (" + this + ", Media Type: " + this.getMediaType() + ") is not a runtime data path and cannot be executed");
  }

  @Override
  public TabularType getTabularType() {

    if (this.tabularType != null) {
      return this.tabularType;
    }

    if (isRuntime()) {
      return TabularType.COMMAND;
    }
    return TabularType.DATA;

  }


  @Override
  public SelectStream getSelectStream() throws SelectException {

    if (this.isRuntime()) {
      /**
       * Runtime resource should be executed before selecting them
       * (ie the result of {@link #execute()})
       * Why? We need the headers and structure parsing
       * We let the choice of execution to the action/step
       * Why?
       * * It's lazy execution
       * * the info step may choose to show:
       *   * the runtime info
       *   * or the result
       */
      throw new InternalException("The resource (" + this + ") is a runtime resource. It is virtual, you should execute it and open a select stream on the static result resource, not the runtime resource.");
    }
    throw new UnsupportedOperationException("Record iteration over the resource (" + this + "/" + this.getMediaType() + ") is not implemented or not possible.");

  }


}
