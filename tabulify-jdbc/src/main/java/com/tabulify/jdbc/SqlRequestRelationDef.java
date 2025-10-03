package com.tabulify.jdbc;

/**
 * Executable got normally their relation def
 * at request runtime, but we can use some trick to get them before
 */
public class SqlRequestRelationDef extends SqlDataPathRelationDef {

  public SqlRequestRelationDef(SqlRequest DataPath, boolean buildFromMeta) {
    super(DataPath, false);
    if (!buildFromMeta) {
      return;
    }

    SqlQueryMetadataDetection
      .builder()
      .setDetectionMethods(getDataPath().getSelectMetadataMethods())
      .build()
      .detect(this);

  }


  @Override
  public SqlRequest getDataPath() {
    return (SqlRequest) super.getDataPath();
  }


}
