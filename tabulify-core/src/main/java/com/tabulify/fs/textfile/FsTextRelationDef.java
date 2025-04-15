package com.tabulify.fs.textfile;

import com.tabulify.model.RelationDefDefault;
import com.tabulify.spi.DataPath;

public class FsTextRelationDef extends RelationDefDefault {

  public <T extends DataPath> FsTextRelationDef(T DataPath) {
    super(DataPath);
  }

  @Override
  public int getColumnsSize() {
    return 1;
  }

}
