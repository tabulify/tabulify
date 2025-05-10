package com.tabulify.spi;

import com.tabulify.DbLoggers;
import com.tabulify.conf.AttributeEnum;
import com.tabulify.conf.Origin;
import com.tabulify.connection.Connection;
import com.tabulify.engine.StreamDependencies;
import com.tabulify.model.*;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferProperties;
import com.tabulify.uri.DataUri;
import net.bytle.dag.Dependency;
import net.bytle.exception.*;
import net.bytle.type.*;
import net.bytle.type.yaml.DefaultTimestampWithoutTimeZoneConstructor;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.scanner.ScannerException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;

import static com.tabulify.spi.DataPathAttribute.*;


public abstract class DataPathAbs implements Comparable<DataPath>, StreamDependencies, DataPath, Dependency {


  public static final String DATA_DEF_SUFFIX = "--datadef.yml";


  private final Connection connection;


  /**
   * The relative path from the connection locate the resource
   * This value can only be null if the script data path is not
   */
  private final String relativeConnectionPath;

  /**
   * The data path of a script
   * (If this value is null, the path should not)
   */
  protected final DataPath scriptDataPath; // Script in a data path

  protected MediaType mediaType;

  @Override
  public Connection getConnection() {
    return this.connection;
  }

  /**
   * Variable may be created dynamically
   * (ie backref of a regexp for instance)
   * So we need string as key identifier
   */
  private final Map<String, com.tabulify.conf.Attribute> variables = new MapKeyIndependent<>();


  @Override
  public String getRelativePath() {
    return this.relativeConnectionPath;
  }


  protected RelationDef relationDef;
  private String description;


  public DataPathAbs(Connection connection, DataPath scriptDataPath) {

    Objects.requireNonNull(connection, "The connection should not be null");
    Objects.requireNonNull(scriptDataPath, "The script data path should not be null");
    this.connection = connection;

    /**
     * A script resource
     */
    this.scriptDataPath = scriptDataPath;

    /**
     * A normal resource
     */
    this.relativeConnectionPath = null;

    /**
     * Media Type
     */
    this.mediaType = DataPathType.TABLI_SCRIPT;


    /**
     * Attach the function
     */
    this.buildAttachVariableFunctions();


  }

  @Override
  public String getName() {
    throw new InternalException("A name is mandatory. This function should be overridden and returns a value");
  }

  private void buildAttachVariableFunctions() {
    this.addVariablesFromEnumAttributeClass(DataPathAttribute.class);
    this.getOrCreateVariable(PATH).setValueProvider(this::getName);
    this.getOrCreateVariable(CONNECTION).setValueProvider(() -> this.getConnection().getName());
    this.getOrCreateVariable(DATA_URI).setValueProvider(this::toDataUri);
    this.getOrCreateVariable(PATH).setValueProvider(this::getRelativePath);
    this.getOrCreateVariable(ABSOLUTE_PATH).setValueProvider(this::getAbsolutePath);
    this.getOrCreateVariable(PARENT).setValueProvider(() -> {
      try {
        return this.getParent().getLogicalName();
      } catch (NoParentException e) {
        return null;
      }
    });
    this.getOrCreateVariable(TYPE).setValueProvider(() -> this.getMediaType().toString());
    this.getOrCreateVariable(SUBTYPE).setValueProvider(() -> this.getMediaType().getSubType());
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
  }


  public com.tabulify.conf.Attribute getOrCreateVariable(AttributeEnum attribute) {

    try {

      return this.getAttribute(attribute);

    } catch (NoVariableException e) {

      com.tabulify.conf.Attribute variable = com.tabulify.conf.Attribute.create(attribute, com.tabulify.conf.Origin.DEFAULT);
      this.addAttribute(variable);
      return variable;

    }
  }

  /**
   * @param connection   - the connection
   * @param relativePath - the relative path from the connection path
   * @param mediaType    - the media type
   */
  public DataPathAbs(Connection connection, String relativePath, MediaType mediaType) {

    Objects.requireNonNull(connection, "The connection should not be null");
    Objects.requireNonNull(relativePath, "The relative path should not be null");
    this.connection = connection;

    this.relativeConnectionPath = relativePath;
    this.scriptDataPath = null;
    this.mediaType = mediaType;

    /**
     * Init the arguments
     */
    this.buildAttachVariableFunctions();

  }


  @Override
  public String getId() {
    return toDataUri().toString();
  }

  public int compareTo(DataPath o) {
    return (this.getConnection().getName() + this.getRelativePath()).compareTo(o.getConnection().getName() + o.getRelativePath());
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
   * @return the query definition (select) or null if it's not a query
   * See also SqlDataSystem#createOrGetQuery
   */
  @Override
  public String getQuery() {
    return this.getScript();
  }

  @Override
  public String getScript() {

    /**
     * In the case of a script/runtime data path, the script field is not null
     * <p>
     * The script value can be:
     * * a query that returns data
     * * a command that does not return data
     * <p>
     * Historically, the query is here because even if it defines a little bit the structure
     * (data def), for now, we get it after its execution if you put the query on the data def you got a recursion
     */
    if (scriptDataPath == null) {
      throw new InternalException("This is not a script resource, you can't ask the script content");
    }
    return Tabulars.getString(scriptDataPath);

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
  public DataPath setDescription(String description) {
    this.description = description;
    return this;
  }


  /**
   * @return the description
   */
  @Override
  public String getDescription() {
    return this.description;
  }


  @Override
  public String getLogicalName() {

    try {
      return (String) this.getAttribute(LOGICAL_NAME).getValueOrDefault();
    } catch (NoVariableException | NoValueException e) {
      return this.getDefaultLogicalName();
    }

  }

  private String getDefaultLogicalName() {
    if (this.isScript()) {
      return this.getScriptDataPath().getLogicalName();
    }
    return getName();
  }

  public DataPath getScriptDataPath() {
    return this.scriptDataPath;
  }

  public com.tabulify.conf.Attribute getAttribute(AttributeEnum attribute) throws NoVariableException {
    return getAttribute(attribute.toString());
  }

  @Override
  public com.tabulify.conf.Attribute getAttribute(String name) throws NoVariableException {
    com.tabulify.conf.Attribute attribute = this.variables.get(name);
    if (attribute == null) {
      throw new NoVariableException();
    }
    return attribute;
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
  public DataUri toDataUri() {
    if (this.scriptDataPath != null) {
      return DataUri.createFromConnectionAndScriptUri(this.connection, this.scriptDataPath.toDataUri());
    }
    return DataUri.createFromConnectionAndPath(this.connection, this.relativeConnectionPath);
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
        boolean isStrict = this.getConnection().getTabular().isStrict();
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
  public boolean isScript() {
    return scriptDataPath != null;
  }

  /**
   * Add a property for this table def
   *
   * @param key   - the key
   * @param value - the attribute value
   * @return the tableDef for initialization chaining
   */
  @Override
  public DataPath addAttribute(String key, Object value) {

    try {
      com.tabulify.conf.Attribute attribute = getConnection().getTabular().createAttribute(key, value);
      this.addAttribute(attribute);
      return this;
    } catch (Exception e) {
      throw new RuntimeException("Error while creating a variable from an attribute (" + key + ") for the resource (" + this + ")", e);
    }

  }

  @Override
  public DataPath addAttribute(AttributeEnum key, Object value) {

    try {
      com.tabulify.conf.Attribute attribute = getConnection().getTabular().createAttribute(key, value);
      this.addAttribute(attribute);
      return this;
    } catch (Exception e) {
      throw new RuntimeException("Error while creating a variable from an attribute (" + key + ") for the resource (" + this + ")", e);
    }

  }


  /**
   * Property value are generally given via a {@link DataDef data definition file}
   *
   * @return the properties value of this table def
   */
  @Override
  public Set<com.tabulify.conf.Attribute> getAttributes() {
    return new HashSet<>(variables.values());
  }


  @Override
  public DataPath mergeDataPathAttributesFrom(DataPath source) {
    source.getAttributes().forEach(this::addAttribute);
    return this;
  }


  /**
   * @param dataDefPath - the path of a data def file
   * @return a data path from a data def path
   * It will create or merge the data path from the data def file
   * <p>
   * If the data document already exist in the data store, it will merge, otherwise it will create it.
   */
  @Override
  public DataPath mergeDataDefinitionFromYamlFile(Path dataDefPath) {
    assert Files.exists(dataDefPath) : "The data definition file path (" + dataDefPath.toAbsolutePath() + " does not exist";
    assert Files.isRegularFile(dataDefPath) : "The data definition file path (" + dataDefPath.toAbsolutePath() + " does not exist";
    String fileName = dataDefPath.getFileName().toString();
    assert fileName.matches("(.*)--(.*).yml") : "The file (" + fileName + ") has not the data def extension (" + DATA_DEF_SUFFIX + ")";

    InputStream input;
    try {
      input = Files.newInputStream(dataDefPath);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    // Transform the file in properties
    Yaml yaml = new Yaml(new DefaultTimestampWithoutTimeZoneConstructor(new LoaderOptions()));

    // Every document is one dataDef
    List<Map<String, Object>> documents = new ArrayList<>();
    try {
      for (Object data : yaml.loadAll(input)) {
        Map<String, Object> document;
        try {
          document = Casts.castToSameMap(data, String.class, Object.class);
        } catch (CastException e) {
          String message = "A data Def must be in a map format. ";
          //noinspection ConstantValue
          if (data.getClass().equals(java.util.ArrayList.class)) {
            message += "They are in a list format. You should suppress the minus if they are present.";
          }
          message += "The Bad Data Def Values are: " + data;
          throw new RuntimeException(message, e);
        }
        documents.add(document);
      }
    } catch (ScannerException e) {
      throw new RuntimeException("Error while parsing the yaml file " + dataDefPath + ". Error: \n" + e.getMessage());
    }

    switch (documents.size()) {
      case 0:
        throw new RuntimeException("No data definition was found in the file (" + relativeConnectionPath + "). The file seems to be empty.");
      case 1:

        Map<String, Object> document = documents.get(0);
        mergeDataDefinitionFromYamlMap(document);

        break;
      default:
        throw new RuntimeException("Too much metadata documents (" + documents.size() + ") found in the file (" + dataDefPath + ") for the dataPath (" + this + ")");
    }
    return this;
  }

  @Override
  public DataPath mergeDataDefinitionFromYamlMap(Map<String, ?> document) {

    List<String> primaryColumns = null;

    // Loop through all others properties
    for (Map.Entry<String, ?> entry : document.entrySet()) {

      DataPathAttribute dataPathAttribute;
      try {
        dataPathAttribute = Casts.cast(entry.getKey(), DataPathAttribute.class);
      } catch (Exception e) {
        /**
         * This is an attribute, not a built-in attribute
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
        case PRIMARY_COLUMNS:
          Object primaryColumnsValue = entry.getValue();
          if (!(primaryColumnsValue instanceof List)) {
            throw new RuntimeException("The primary columns are not a list but a " + primaryColumnsValue.getClass());
          }
          primaryColumns = Casts.castToListSafe(primaryColumnsValue, String.class);
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
            columns = Casts.castToListSafe(columnValues, Object.class);
          } catch (ClassCastException e) {
            String message = "The columns must be in a list format. ";
            message += "They are in the following format:" + (entry.getValue() != null ? entry.getValue().getClass().getSimpleName() : null);
            message += "Bad Columns Values are: " + entry.getValue();
            throw new RuntimeException(message, e);
          }
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
               * List of map
               */
              Map<String, Object> columnProperties;
              try {
                columnProperties = Casts.castToSameMap(column, String.class, Object.class);
              } catch (CastException e) {
                throw new InternalException("String and Object should not throw a cast exception", e);
              }

              Object columnNameObject = Maps.getPropertyCaseIndependent(columnProperties, ColumnAttribute.NAME.toString());
              if (columnNameObject == null) {
                throw new RuntimeException("The name property for a column is mandatory and was not found for the column (" + columnCount + ")");
              }
              String columnName;
              try {
                columnName = (String) columnNameObject;
              } catch (ClassCastException e) {
                throw new RuntimeException("The name property of the column (" + columnCount + ") is not a string but a " + columnNameObject.getClass().getSimpleName());
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
              String type = "varchar";
              Object oType = Maps.getPropertyCaseIndependent(columnProperties, "type");
              if (oType != null) {
                type = (String) oType;
              }
              SqlDataType sqlDataType = this.getConnection().getSqlDataType(type);
              if (sqlDataType == null) {
                throw new IllegalStateException("The type (" + type + ") of the column (" + columnName + ") is unknown for the connection (" + this.getConnection() + ")");
              }


              /**
               * Column building
               */
              ColumnDef columnDef;
              try {
                columnDef = relationDef.getColumnDef(columnName);
              } catch (NoColumnException e) {
                /**
                 * In the case of the text file, all data type are varchar
                 * We are just creating a new column for now
                 */
                columnDef = relationDef.createColumn(columnName, sqlDataType, sqlDataType.getSqlClass());
              }

              for (Map.Entry<String, Object> columnProperty : columnProperties.entrySet()) {
                try {
                  ColumnAttribute columnAttribute = Casts.cast(columnProperty.getKey(), ColumnAttribute.class);
                  switch (columnAttribute) {
                    case NAME:
                    case TYPE:
                      // already done during the creation
                      break;
                    case PRECISION:
                      if (columnDef.getPrecision() == null) {
                        columnDef.precision((Integer) columnProperty.getValue());
                      }
                      break;
                    case SCALE:
                      if (columnDef.getScale() == null) {
                        columnDef.scale((Integer) columnProperty.getValue());
                      }
                      break;
                    case COMMENT:
                      if (columnDef.getComment() == null) {
                        columnDef.setComment((String) columnProperty.getValue());
                      }
                      break;
                    case NULLABLE:
                      if (columnDef.isNullable() == null) {
                        columnDef.setNullable(Boolean.valueOf((String) columnProperty.getValue()));
                      }
                      break;
                    case POSITION:
                      columnDef.setColumnPosition(Casts.cast(columnProperty.getValue(), Integer.class));
                      break;
                    default:
                      DbLoggers.LOGGER_DB_ENGINE.warning("The column attribute (" + columnAttribute + ") is not an attribute that can be set on the data path (" + this + ")");
                      break;
                  }
                } catch (CastException arg) {
                  /**
                   * This is not a built-in attribute
                   */
                  columnDef.setVariable(columnProperty.getKey(), columnProperty.getValue());
                }
              }
            } catch (ClassCastException e) {
              String message = Strings.createMultiLineFromStrings(
                "The properties of the column (" + columnCount + ") from the data def (" + this + ") must be given in a map format. ",
                "They are in the following format: " + column.getClass().getSimpleName(),
                "Bad Columns Properties Values are: " + column).toString();
              throw new RuntimeException(message, e);
            }
          }
          break;
        default:
          DbLoggers.LOGGER_DB_ENGINE.warning("The data path attribute (" + dataPathAttribute + ") is not an attribute that can be modified on the data path (" + this + ")");
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
  public DataPath setDataAttributes(Map<String, ?> dataAttributes) {
    dataAttributes.forEach(this::addAttribute);
    return this;
  }

  @Override
  public DataPath mergeDataDefinitionFrom(DataPath mergeFrom) {
    this.mergeDataPathAttributesFrom(mergeFrom);
    this.getOrCreateRelationDef().mergeDataDef(mergeFrom);
    return this;
  }

  @Override
  public DataPath setLogicalName(String logicalName) {
    try {
      com.tabulify.conf.Attribute attribute = getConnection().getTabular().createAttribute(LOGICAL_NAME, logicalName);
      this.addAttribute(attribute);
      return this;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

  }

  @Override
  public DataPath toAttributesDataPath() {

    RelationDef variablesDataPath = this.getConnection().getTabular().getMemoryDataStore()
      .getDataPath(this.getName() + "_variable")
      .setDescription("Information about the data resource (" + this + ")")
      .getOrCreateRelationDef()
      .addColumn(KeyNormalizer.createSafe(AttributeProperties.ATTRIBUTE).toSqlCaseSafe())
      .addColumn(KeyNormalizer.createSafe(AttributeProperties.VALUE).toSqlCaseSafe())
      .addColumn(KeyNormalizer.createSafe(AttributeProperties.DESCRIPTION).toSqlCaseSafe());

    try (InsertStream insertStream = variablesDataPath.getDataPath().getInsertStream()) {
      for (com.tabulify.conf.Attribute attribute : this.getAttributes()) {
        List<Object> row = new ArrayList<>();
        row.add(KeyNormalizer.createSafe(attribute.getAttributeMetadata().toString()).toCamelCase());
        row.add(attribute.getValueOrDefaultOrNull());
        row.add(attribute.getAttributeMetadata().getDescription());
        insertStream.insert(row);
      }
    }
    return variablesDataPath.getDataPath();
  }

  @Override
  public InsertStream getInsertStream() {
    /**
     * If the source is not defined, we expect the same structure as the target
     */
    return getInsertStream(this, TransferProperties.create());
  }

  @Override
  public InsertStream getInsertStream(TransferProperties transferProperties) {
    /**
     * If the source is not defined, we expect the same structure than the target
     */
    return getInsertStream(this, transferProperties);
  }

  @Override
  public DataPath addAttribute(com.tabulify.conf.Attribute attribute) {
    this.variables.put(attribute.getAttributeMetadata().toString(), attribute);
    /**
     * This conditional is for perf/debug reason, as the {@link #toString()}
     * is pretty expensive (ie the `this` in the string)
     * The problem with this code is that the code in the string gets executed even if the level is not finest
     */
    if (DbLoggers.LOGGER_DB_ENGINE.getLevel().intValue() <= Level.FINEST.intValue()) {
      DbLoggers.LOGGER_DB_ENGINE.finest("The variable (" + attribute + ") for the resource (" + this + ") was set to the value (" + Strings.createFromObjectNullSafe(attribute.getValueOrDefaultOrNull()) + ")");
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
    Arrays.asList(enumClass.getEnumConstants()).forEach(c -> this.addAttribute(com.tabulify.conf.Attribute.create(c, Origin.DEFAULT)));
    return this;
  }

  @Override
  public MediaType getMediaType() {
    return this.mediaType;
  }

  /**
   * Same as {@link #getAttribute(AttributeEnum)} but without compile exception
   */
  @Override
  public com.tabulify.conf.Attribute getAttributeSafe(AttributeEnum sqlDataPathAttribute) {
    try {
      return getAttribute(sqlDataPathAttribute);
    } catch (NoVariableException e) {
      throw new RuntimeException(e);
    }
  }
}
