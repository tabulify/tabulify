package com.tabulify.oracle;

import com.tabulify.jdbc.SqlConnection;
import com.tabulify.jdbc.SqlConnectionMetadata;
import com.tabulify.jdbc.SqlRequest;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.SelectStream;
import net.bytle.type.Casts;


public class OracleConnectionMetadata extends SqlConnectionMetadata {
  public OracleConnectionMetadata(OracleConnection oracleConnection) {
    super(oracleConnection);
  }


  /**
   * The Unicode / national character set (used to determine the number of bytes for a char)
   */
  public String getUnicodeCharacterSet() {


    SqlRequest queryDataPath = SqlRequest.builder()
      .setSql((SqlConnection) this.getConnection(), "select value from nls_database_parameters where parameter ='NLS_NCHAR_CHARACTERSET'")
      .build();
    /**
     * We can't use {@link DataPath#getRecords()}
     * because it would start a recursion.
     * Why? because getLengthSemantic is a part of data type building
     */

    try (
      SelectStream selectStream = queryDataPath.execute().getSelectStreamSafe()
    ) {
      selectStream.next();
      return selectStream.getString(1);
    }
  }

  public String getCharacterSet() {

    SqlRequest queryDataPath = SqlRequest
      .builder()
      .setSql((SqlConnection) this.getConnection(), "select value from nls_database_parameters where parameter ='NLS_CHARACTERSET'")
      .build();
    /**
     * We can't use {@link DataPath#getRecords()}
     * because it would start a recursion.
     * Why? because getLengthSemantic is a part of data type building
     */
    try (
      SelectStream selectStream = queryDataPath.execute().getSelectStreamSafe()
    ) {
      selectStream.next();
      return selectStream.getString(1);
    }
  }

  public OracleCharLengthSemantic getLengthSemantic() {
    SqlRequest queryDataPath = SqlRequest
      .builder()
      .setSql((SqlConnection) this.getConnection(), "select value from nls_database_parameters where parameter ='NLS_LENGTH_SEMANTICS'")
      .build();
    /**
     * We can't use {@link DataPath#getRecords()}
     * because it would start a recursion.
     * Why? because getLengthSemantic is a part of data type building
     */
    try (
      SelectStream selectStream = queryDataPath.execute().getSelectStreamSafe()
    ) {
      selectStream.next();
      return Casts.castSafe(selectStream.getString(1), OracleCharLengthSemantic.class);
    }
  }
}
