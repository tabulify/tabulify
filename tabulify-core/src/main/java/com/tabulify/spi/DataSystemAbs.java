package com.tabulify.spi;

import com.tabulify.connection.Connection;
import com.tabulify.model.ColumnDef;
import com.tabulify.model.ForeignKeyDef;

import java.util.Collections;
import java.util.List;

public abstract class  DataSystemAbs implements DataSystem {


  private final Connection connection;

  public DataSystemAbs(Connection connection) {
    this.connection = connection;
  }

  @Override
  public Connection getConnection() {
    return this.connection;
  }

  @Override
  public void truncate(DataPath dataPath) {
    truncate(Collections.singletonList(dataPath));
  }

  @Override
  public void dropNotNullConstraint(DataPath dataPath) {
    for(ColumnDef columnDef: dataPath.getOrCreateRelationDef().getColumnDefs()){
      columnDef.setNullable(false);
    }
  }

  @Override
  public void dropForce(DataPath dataPath) {
    List<ForeignKeyDef> foreignKeyDefs = Tabulars.getReferences(dataPath);
    for (ForeignKeyDef foreignKeyDef : foreignKeyDefs) {
      Tabulars.dropConstraint(foreignKeyDef);
    }
    drop(dataPath);
  }

  @Override
  public void execute(DataPath dataPath) {
    throw new UnsupportedOperationException("The execute command is not yet supported in the system of the connection ("+this.getConnection().getName()+")");
  }


}
