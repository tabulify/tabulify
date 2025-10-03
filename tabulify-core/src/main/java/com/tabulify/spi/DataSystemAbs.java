package com.tabulify.spi;

import com.tabulify.connection.Connection;
import com.tabulify.model.*;
import net.bytle.type.KeyInterface;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MediaType;

import java.util.Set;

public abstract class DataSystemAbs implements DataSystem {


  private final Connection connection;

  public DataSystemAbs(Connection connection) {
    this.connection = connection;
  }

  @Override
  public Connection getConnection() {
    return this.connection;
  }

  @Override
  public void dataTypeBuildingMain(SqlDataTypeManager sqlDataTypeManager) {

    /**
     * Default used to build the memory column
     */
    for (SqlDataTypeAnsi sqlDataTypeAnsi : SqlDataTypeAnsi.values()) {

      if (sqlDataTypeAnsi.getValueClass() == null) {
        continue;
      }

      SqlDataType.SqlDataTypeBuilder<?> parent = sqlDataTypeManager.createTypeBuilder(sqlDataTypeAnsi)
        .setPriority(sqlDataTypeAnsi.getPriority());
      for (KeyInterface shortName : sqlDataTypeAnsi.getAliases()) {
        parent.addChildAliasName(shortName);
      }


    }

  }

  @Override
  public void dropNotNullConstraint(DataPath dataPath) {
    for (ColumnDef<?> columnDef : dataPath.getOrCreateRelationDef().getColumnDefs()) {
      columnDef.setNullable(false);
    }
  }


  @Override
  public DataPath getTargetFromSource(DataPath sourceDataPath, MediaType targetMediaType, DataPath targetParentDataPath) {
    return this.getConnection().getDataPath(sourceDataPath.getName(), targetMediaType);
  }

  @Override
  public String toValidName(String name) {
    return name;
  }


  @Override
  public boolean isContainer(DataPath dataPath) {
    return dataPath.getMediaType().equals(this.getContainerMediaType());
  }

  @Override
  public Long getSize(DataPath dataPath) {
    return -1L;
  }

  @Override
  public SqlDataTypeVendor getSqlDataTypeVendor(KeyNormalizer typeName, int typeCode) {
    return SqlDataTypeAnsi.cast(typeName, typeCode);
  }

  @Override
  public Set<SqlDataTypeVendor> getSqlDataTypeVendors() {
    return Set.of(SqlDataTypeAnsi.values());
  }

  @Override
  public SqlTypeKeyUniqueIdentifier getSqlTypeKeyUniqueIdentifier() {
    return SqlTypeKeyUniqueIdentifier.NAME_AND_CODE;
  }
}
