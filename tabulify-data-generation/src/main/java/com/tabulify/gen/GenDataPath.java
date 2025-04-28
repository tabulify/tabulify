package com.tabulify.gen;

import com.tabulify.spi.DataPath;

/**
 * A generator data path
 */
public interface GenDataPath extends DataPath {



  GenDataPath setMaxRecordCount(Long maxRecordCount);

  Long getMaxRecordCount();


  GenDataPathUtility getGenDataPathUtility();

  @Override
  GenRelationDef getOrCreateRelationDef();



  @Override
  GenRelationDef createRelationDef();

  /**
   * @return the maximum size that this data path can return, not capped to max record count
   */
  Long getSizeNotCapped();


}
