package com.tabulify.jdbc;

import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.fs.FsDataPath;
import com.tabulify.model.SqlDataType;
import com.tabulify.spi.*;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferPropertiesSystem;
import com.tabulify.crypto.Digest;
import com.tabulify.exception.*;
import com.tabulify.fs.Fs;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.tabulify.jdbc.SqlRequestAttribute.SELECT_METADATA_DETECTIONS;

/**
 * SqlRequest extends {@link SqlDataPath}
 * so that the path method (getName, ...)
 * are taken over
 */
public class SqlRequest extends SqlDataPath {

  private final SqlScript sqlScript;


  /**
   * Determine if a sql request stop at the first error
   */
  private Boolean strictExecution;

  private List<SqlParameter> parameterList = new ArrayList<>();
  @SuppressWarnings("unchecked")
  private List<SqlQueryMetadataDetectionMethod> selectMetadataDetectionMethods = (List<SqlQueryMetadataDetectionMethod>) SELECT_METADATA_DETECTIONS.getDefaultValue();

  /**
   * Not null when already executed
   */
  private DataPath resultDataPath;


  public SqlRequest(SqlRequestBuilder sqlRequestBuilder) {

    super(
      sqlRequestBuilder.getExecutionConnection(),
      null,
      sqlRequestBuilder.sqlScript.getExecutableDataPath(),
      SqlMediaType.REQUEST
    );


    /**
     * The request sql statements
     */
    this.sqlScript = sqlRequestBuilder.sqlScript;


    /**
     * Add attributes
     */
    this.addVariablesFromEnumAttributeClass(SqlRequestAttribute.class);


    String logicalName = sqlRequestBuilder.getName();

    String logicalNameSql = this.getConnection().getDataSystem().toValidName(logicalName);
    this.setLogicalName(logicalNameSql);

    /**
     * Strict Execution Default
     * It gives the possibility to the user to overwrite it with the tabul cli
     */
    this.strictExecution = sqlRequestBuilder.getExecutionConnection().getTabular().isStrictExecution();

  }

  public static SqlRequestBuilder builder() {
    return new SqlRequestBuilder();
  }


  @Override
  public Long getSize() {
    return 0L;
  }

  /**
   * Get count
   * - create a request over a SELECT statement
   * - or count the number of record iterating over the result set
   */
  @Override
  public Long getCount() {

    long count = 0;
    if (!this.getExecutableSqlScript().isSingleSelectStatement()) {
      try (SelectStream selectStream = this.execute().getSelectStreamSafe()) {
        while (selectStream.next()) {
          count++;
        }
        return count;
      }
    }

    SqlRequest queryDataPath = this.getConnection().getRuntimeDataPath("select count(1) as count from " + this.getConnection().getDataSystem().createFromClause(this));
    if (this.isParametrizedStatement()) {
      queryDataPath.setParameters(this.getParameters());
    }
    try (
      SelectStream selectStream = queryDataPath.execute().getSelectStream()
    ) {
      boolean next = selectStream.next();
      if (next) {
        count = selectStream.getInteger(1);
      }
    } catch (SelectException e) {
      boolean strict = this.getIsStrictExecution();
      String message = "Error while trying to get the count of " + this;
      if (strict) {
        throw new StrictException(message, e);
      } else {
        SqlLog.LOGGER_DB_JDBC.warning(message + "\n" + e.getMessage());
      }
    }
    return count;
  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferPropertiesSystem transferProperties) {
    throw new UnsupportedOperationException("You can't insert in an executable");
  }

  @Override
  public SelectStream getSelectStream() throws SelectException {
    throw new InternalException("This is a sql request, you should execute it first");
  }

  @Override
  public SqlScript getExecutableSqlScript() {
    return this.sqlScript;
  }

  @Override
  public SchemaType getSchemaType() {
    return SchemaType.LOOSE;
  }

  @Override
  public DataPath addAttribute(KeyNormalizer key, Object value) {
    SqlRequestAttribute sqlRequestAttribute;
    try {
      sqlRequestAttribute = Casts.cast(key, SqlRequestAttribute.class);
    } catch (CastException e) {
      // not a sql request attribute may be a common one (logical name)
      return super.addAttribute(key, value);
    }
    Attribute attribute;
    try {
      attribute = this.getConnection().getTabular().getVault()
        .createVariableBuilderFromAttribute(sqlRequestAttribute)
        .setOrigin(Origin.MANIFEST)
        .build(value);
      this.addAttribute(attribute);
    } catch (CastException e) {
      throw new IllegalArgumentException("The " + sqlRequestAttribute + " value (" + value + ") is not conform . Error: " + e.getMessage(), e);
    }
    switch (sqlRequestAttribute) {
      case STRICT_EXECUTION:
        this.setStrictExecution((Boolean) attribute.getValueOrDefault());
        break;
      case PARAMETERS:
        List<Object> parametersObjectList;
        try {
          parametersObjectList = Casts.castToNewList(attribute.getValueOrDefault(), Object.class);
        } catch (CastException e) {
          throw new IllegalArgumentException("The " + sqlRequestAttribute + " value is not a valid list. Error: " + e.getMessage(), e);
        }
        List<SqlParameter> parametersList = new ArrayList<>();
        for (Object parameterObject : parametersObjectList) {
          int parameterId = parametersList.size() + 1;
          Map<SqlParameterAttribute, Object> sqlParameterAttributeObjectMap;
          try {
            sqlParameterAttributeObjectMap = Casts.castToNewMap(parameterObject, SqlParameterAttribute.class, Object.class);
          } catch (CastException e) {
            throw new IllegalArgumentException("The parameter " + parameterId + " is not a map. Error: " + e.getMessage(), e);
          }
          SqlParameter.SqlParameterBuilder sqlParameter = SqlParameter.builder()
            .setConnection(this.getConnection())
            .setIndex(parameterId);
          for (Map.Entry<SqlParameterAttribute, Object> entry : sqlParameterAttributeObjectMap.entrySet()) {
            SqlParameterAttribute parameterAttribute = entry.getKey();
            switch (parameterAttribute) {
              case VALUE:
                sqlParameter.setValue(entry.getValue());
                break;
              case DIRECTION:
                SqlParameterDirection sqlParameterDirection;
                try {
                  sqlParameterDirection = Casts.cast(entry.getValue(), SqlParameterDirection.class);
                } catch (CastException e) {
                  throw new IllegalArgumentException("The direction value (" + entry.getValue() + ") of the parameter (" + parameterId + ") is not a valid direction. Error: " + e.getMessage(), e);
                }
                sqlParameter.setDirection(sqlParameterDirection);
                break;
              case NAME:
                try {
                  sqlParameter.setName(KeyNormalizer.create(entry.getValue()));
                } catch (CastException e) {
                  throw new IllegalArgumentException("The name value (" + entry.getValue() + ") of the parameter (" + parameterId + ") is not valid. Error: " + e.getMessage(), e);
                }
                break;
              case TYPE:
                KeyNormalizer type;
                try {
                  type = KeyNormalizer.create(entry.getValue());
                } catch (CastException e) {
                  throw new IllegalArgumentException("The type value (" + entry.getValue() + ") of the parameter (" + parameterId + ") is not valid. Error: " + e.getMessage(), e);
                }
                SqlDataType<?> sqlDataType = this.getConnection().getSqlDataType(type);
                if (sqlDataType == null) {
                  throw new IllegalArgumentException("The type (" + type + ") of of the parameter (" + parameterId + ") is unknown for the connection (" + this.getConnection() + ")");
                }
                sqlParameter.setType(sqlDataType);
                break;
              default:
                throw new InternalException("The sql parameter attribute (" + parameterAttribute + ") of of the parameter (" + parameterId + ") was not processed in the switch case.");
            }
          }
          parametersList.add(sqlParameter.build());
        }

        this.setParameters(parametersList);
        break;
      case SELECT_METADATA_DETECTIONS:
        List<String> metadataDetectionMethods;
        try {
          metadataDetectionMethods = Casts.castToNewList(attribute.getValueOrDefault(), String.class);
        } catch (CastException e) {
          throw new IllegalArgumentException("The " + sqlRequestAttribute + " value (" + value + ") is not a list. Error: " + e.getMessage(), e);
        }
        List<SqlQueryMetadataDetectionMethod> metadataDetectionMethodsObject = new ArrayList<>();
        for (String metadataDetectionMethod : metadataDetectionMethods) {
          try {
            metadataDetectionMethodsObject.add(Casts.cast(metadataDetectionMethod, SqlQueryMetadataDetectionMethod.class));
          } catch (CastException e) {
            throw IllegalArgumentExceptions.createFromMessageWithPossibleValues("A value of the attribute " + SELECT_METADATA_DETECTIONS.getKeyNormalized().toCliLongOptionName() + " is not valid. Value: " + metadataDetectionMethod + "\n.", SqlQueryMetadataDetectionMethod.class, e);
          }
        }
        this.setSelectMetadataDetection(metadataDetectionMethodsObject);
        break;
      default:
        throw new MissingSwitchBranch("sqlRequestAttribute", sqlRequestAttribute);
    }
    return this;
  }

  private void setStrictExecution(Boolean strictExecution) {
    this.strictExecution = strictExecution;
  }

  private SqlDataPath setSelectMetadataDetection(List<SqlQueryMetadataDetectionMethod> metadataDetectionMethods) {
    this.selectMetadataDetectionMethods = metadataDetectionMethods;
    return this;
  }

  public void setParameters(List<SqlParameter> values) {
    this.parameterList = values;
  }

  public List<SqlParameter> getParameters() {
    return this.parameterList;
  }

  public List<SqlQueryMetadataDetectionMethod> getSelectMetadataMethods() {
    return this.selectMetadataDetectionMethods;
  }

  public boolean getIsStrictExecution() {
    return this.strictExecution;
  }

  public SqlRequest addParameter(SqlParameter.SqlParameterBuilder parameter) {
    this.parameterList.add(parameter
      .setIndex(this.parameterList.size() + 1)
      .setConnection(this.getConnection())
      .build()
    );
    return this;
  }


  public boolean getHasOutParameters() {
    /**
     * If there is an out, this is a callable
     */
    return this.getParameters()
      .stream()
      .anyMatch(p -> !p.getDirection().equals(SqlParameterDirection.IN));
  }

  public boolean isParametrizedStatement() {
    return !this.parameterList.isEmpty();
  }

  /**
   * A request can be built from:
   * * {@link SqlDataPath sql object} such as table or view to get the data with a select
   * * directly from a sql file or java code
   */
  public static class SqlRequestBuilder {
    /**
     * The sql statements and their origin
     */
    private SqlScript sqlScript;
    /**
     * The connections
     */
    private SqlConnection executionConnection;
    private String name = "Anonymous";

    private SqlConnectionResourcePath sqlConnectionResourcePath;

    /**
     * A request from a data path with sql statements as content
     */
    public SqlRequestBuilder setExecutableDataPath(SqlConnection sqlConnection, DataPath executableDataPath) {

      this.executionConnection = sqlConnection;
      this.sqlScript = SqlScript.builder()
        .setExecutableDataPath(executableDataPath)
        .build();
      /**
       * Logical Name is the name of the file without the extension
       * If you load `query_1.sql`, you will create a `query_1` table
       */
      this.name = executableDataPath.getLogicalName();
      if (executableDataPath instanceof FsDataPath) {
        name = Fs.getFileNameWithoutExtension(((FsDataPath) executableDataPath).getNioPath());
      }
      /**
       * We don't read the content of the script and make a hash id
       * as the resource may not exist
       */
      String object = this.executionConnection.getDataSystem().toValidName(executableDataPath.getCompactPath() + "_" + executableDataPath.getConnection() + "_" + executionConnection);
      this.sqlConnectionResourcePath = getExecutionResourcePathFromName(executionConnection, object);
      return this;
    }


    /**
     * A request for an object such as a table or view
     */
    public SqlRequestBuilder setSqlObjectDataPath(SqlDataPath sqlDataPath) {
      this.sqlScript = sqlDataPath.getExecutableSqlScript();
      this.executionConnection = sqlDataPath.getConnection();
      this.sqlConnectionResourcePath = sqlDataPath.getSqlConnectionResourcePath();
      return this;
    }

    public SqlRequest build() {

      if (this.name == null) {
        throw new InternalException("Name should not be null");
      }
      if (this.sqlConnectionResourcePath == null) {
        throw new InternalException("SqlConnectionResourcePath should not be null");
      }
      return new SqlRequest(this);
    }

    public SqlConnection getExecutionConnection() {
      return this.executionConnection;
    }

    public String getName() {
      return this.name;
    }

    public SqlRequestBuilder setSql(SqlConnection executionConnection, String sql) {
      this.name = "anonymous_" + Digest.createFromString(Digest.Algorithm.MD5, sql).getHashHex();
      DataPath executableDataPath = executionConnection.getTabular()
        .getMemoryConnection()
        .getDataPath(this.name)
        .setContent(sql);
      this.sqlScript = SqlScript.builder()
        .setSqlString(executableDataPath, sql)
        .build();
      this.executionConnection = executionConnection;
      this.sqlConnectionResourcePath = getExecutionResourcePathFromName(executionConnection, this.name);
      return this;
    }

    private SqlConnectionResourcePath getExecutionResourcePathFromName(SqlConnection executionConnection, String name) {
      /**
       * Path
       */
      String currentCatalog;
      try {
        currentCatalog = executionConnection.getCurrentCatalog();
      } catch (NoCatalogException e) {
        currentCatalog = null;
      }
      String schema = executionConnection.getCurrentSchema();
      return SqlConnectionResourcePath
        .createOfCatalogSchemaAndObjectName(this.executionConnection, currentCatalog, schema, name);
    }
  }

  @Override
  public SqlRequestRelationDef getRelationDef() {

    return (SqlRequestRelationDef) super.getRelationDef();
  }

  @Override
  public SqlRequestRelationDef createRelationDef() {
    this.relationDef = new SqlRequestRelationDef(this, true);
    return (SqlRequestRelationDef) this.relationDef;
  }

  @Override
  public SqlRequestRelationDef createEmptyRelationDef() {
    this.relationDef = new SqlRequestRelationDef(this, false);
    return (SqlRequestRelationDef) this.relationDef;
  }

  @Override
  public SqlRequestRelationDef getOrCreateRelationDef() {
    return (SqlRequestRelationDef) super.getOrCreateRelationDef();
  }

  @Override
  public DataPath execute() {

    SelectStream selectStream;
    try {
      selectStream = SqlRequestExecution.executeAndGetSelectStream(this);
    } catch (SelectException e) {
      throw new RuntimeException("Error while executing (" + this + ". Error:" + e.getMessage(), e);
    }

    if (selectStream instanceof SqlResultSetStream) {
      resultDataPath = new SqlResultSetDataPath(this, (SqlResultSetStream) selectStream);
    } else {
      resultDataPath = selectStream.getDataPath();
    }
    return this.resultDataPath;

  }

  @Override
  public TabularType getTabularType() {
    if (sqlScript.isSingleSelectStatement()) {
      return TabularType.DATA;
    }
    return TabularType.COMMAND;
  }

}
