package com.tabulify.gen;

import com.tabulify.spi.DataPath;

/**
 * A generator data path
 */
public interface GenDataPath extends DataPath {



  GenDataPath setMaxRecordCount(Long maxRecordCount);

  /**
   * The maximum number of record generated
   */
  Long getMaxRecordCount();


  /**
   * The number of record created in a stream
   * May be null
   */
  Long getStreamRecordCount();


  GenDataPath setStreamRecordCount(Long streamRecordCount);

  GenDataPathUtility getGenDataPathUtility();

  @Override
  GenRelationDef getOrCreateRelationDef();

  @Override
  GenRelationDef createEmptyRelationDef();

  @Override
  GenRelationDef createRelationDef();

  /**
   * @return the maximum size that this data path can return, not capped to max record count
   */
  Long getSizeNotCapped();


}
