package com.tabulify.oracle;

import com.tabulify.jdbc.SqlConnectionMetadata;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.SelectException;
import com.tabulify.stream.SelectStream;


public class OracleConnectionMetadata extends SqlConnectionMetadata {
  public OracleConnectionMetadata(OracleConnection oracleConnection) {
    super(oracleConnection);
  }


  /**
   * The unicode / national character set (used to determine the number of bytes for a char)
   * @return
   */
  public String getUnicodeCharacterSet() {

    String statement = "select value from nls_database_parameters where parameter ='NLS_NCHAR_CHARACTERSET'";
    DataPath queryDataPath = this.getConnection().createScriptDataPath(statement);
    String value = null;
    try (
      SelectStream selectStream = queryDataPath.getSelectStream()
    ) {
      boolean next = selectStream.next();
      if (next) {
        value = selectStream.getString(1);
      }
    } catch (SelectException e) {
      throw new RuntimeException(e);
    }
    return value;
  }

  public String getCharacterSet() {

    String statement = "select value from nls_database_parameters where parameter ='NLS_CHARACTERSET'";
    DataPath queryDataPath = this.getConnection().createScriptDataPath(statement);
    String value = null;
    try (
      SelectStream selectStream = queryDataPath.getSelectStream()
    ) {
      boolean next = selectStream.next();
      if (next) {
        value = selectStream.getString(1);
      }
    } catch (SelectException e) {
      throw new RuntimeException(e);
    }
    return value;
  }
}
