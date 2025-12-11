package com.tabulify.jdbc;

import com.tabulify.spi.DataPath;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;
import com.tabulify.transfer.TransferPropertiesSystem;
import com.tabulify.exception.InternalException;

import static com.tabulify.jdbc.SqlMediaType.RESULT_SET;

/**
 * A data path that encapsulates a {@link SqlResultSetStream Sql Result}
 * to pass it back to the {@link SqlRequest#execute()}
 */
public class SqlResultSetDataPath extends SqlDataPath {


  private final SqlResultSetStream selectStream;
  private final SqlRequest sqlRequest;

  public SqlResultSetDataPath(SqlRequest sqlRequest, SqlResultSetStream selectStream) {
    super(sqlRequest.getConnection(), sqlRequest.getName(), null, RESULT_SET);
    this.selectStream = selectStream;
    this.sqlRequest = sqlRequest;
  }

  @Override
  public String getName() {
    /**
     * Why we return a name?
     * Because SqlResultSetDataPath is becoming the input data path
     * and in a template we should be able to resolve ${input_logical_name}
     */
    return sqlRequest.getName();
  }

  @Override
  public SqlDataPathRelationDef getRelationDef() {
    return selectStream.getRuntimeRelationDef();
  }

  @Override
  public SqlDataPathRelationDef createEmptyRelationDef() {
    throw new InternalException("This is a sql result set, you can't create empty relation def");
  }

  @Override
  public SelectStream getSelectStream() {
    return this.selectStream;
  }

  @Override
  public InsertStream getInsertStream(DataPath source, TransferPropertiesSystem transferProperties) {
    throw new InternalException("This is a sql result set, you can't insert in it");
  }
}
