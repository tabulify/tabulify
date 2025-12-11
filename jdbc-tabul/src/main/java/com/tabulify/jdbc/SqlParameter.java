package com.tabulify.jdbc;

import com.tabulify.model.SqlDataType;
import com.tabulify.model.SqlDataTypeAnsi;
import com.tabulify.exception.InternalException;
import com.tabulify.type.KeyNormalizer;

public class SqlParameter {

  private final SqlParameterBuilder builder;

  public SqlParameter(SqlParameterBuilder sqlParameterBuilder) {
    this.builder = sqlParameterBuilder;
  }

  public static SqlParameterBuilder builder() {
    return new SqlParameterBuilder();
  }

  public Object getValue() {
    return this.builder.value;
  }

  public SqlDataType<?> getType() {
    return this.builder.type;
  }

  public SqlParameterDirection getDirection() {
    return this.builder.direction;
  }

  public KeyNormalizer getName() {
    return this.builder.name;
  }

  public int getIndex() {
    return this.builder.index;
  }

  public static class SqlParameterBuilder {

    private SqlConnection sqlConnection;
    private Object value = null;
    private SqlParameterDirection direction = SqlParameterDirection.IN;
    private SqlDataType<?> type;
    private Integer typeCode;
    private KeyNormalizer name;
    private Integer index;


    public SqlParameterBuilder setValue(Object value) {
      this.value = value;
      return this;
    }

    public SqlParameterBuilder setConnection(SqlConnection sqlConnection) {
      this.sqlConnection = sqlConnection;
      return this;
    }

    public SqlParameterBuilder setDirection(SqlParameterDirection direction) {
      this.direction = direction;
      return this;
    }

    public SqlParameter build() {
      if (this.type == null) {
        if (sqlConnection == null) {
          // Internal error has it's our responsibility to inject it
          throw new InternalException("The sql connection is null. The type is null and cannot be derived from the connection.");
        }
        if (this.typeCode != null) {
          this.type = sqlConnection.getSqlDataType(SqlDataTypeAnsi.cast(null, this.typeCode));
        } else {
          if (value == null) {
            throw new IllegalArgumentException("For a sql parameter, when the value is null, the type cannot be null");
          }
          this.type = sqlConnection.getSqlDataType(value.getClass());
        }
      }
      if (this.index == null) {
        throw new InternalException("For a sql parameter, the index is mandatory");
      }
      if (this.name == null) {
        this.name = KeyNormalizer.createSafe("para_" + this.index);
      }
      return new SqlParameter(this);
    }

    public SqlParameterBuilder setType(SqlDataType sqlDataType) {
      this.type = sqlDataType;
      return this;
    }

    public SqlParameterBuilder setType(Integer typeCode) {
      this.typeCode = typeCode;
      return this;
    }

    public SqlParameterBuilder setName(KeyNormalizer name) {
      this.name = name;
      return this;
    }

    /**
     * @param index - the index starting at 1 (same as column position)
     */
    public SqlParameterBuilder setIndex(int index) {
      this.index = index;
      return this;
    }
  }
}
