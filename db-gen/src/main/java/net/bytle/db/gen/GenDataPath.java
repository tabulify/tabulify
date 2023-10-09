package net.bytle.db.gen;

import net.bytle.db.spi.DataPath;

/**
 * A generator data path
 */
public interface GenDataPath extends DataPath {


  /**
   * The {@link DataPath#getVariable(String)} key giving the maximum number of rows generated
   * <p>
   * For now, if you want to move the maximum size higher,
   * you need to truncate the table, you can't just set it higher and rerun a data generation
   * <p>
   * TODO: To be able to move the max size up without truncating the table before,
   * this is possible to create a sequence generator for a
   * primary key but for a unique key on multiple columns
   * this is more difficult
   */
  String MAX_RECORD_COUNT_PROPERTY_KEY = "MaxRecordCount".toLowerCase();


  GenDataPath setMaxRecordCount(Long maxRecordCount);

  Long getMaxRecordCount();



  GenDataPathUtility getGenDataPathUtility();

  @Override
  GenRelationDef getOrCreateRelationDef();



  @Override
  GenRelationDef createRelationDef();



}
