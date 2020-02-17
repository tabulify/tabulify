package net.bytle.db.gen;

import net.bytle.db.spi.DataPath;

/**
 * A generator data path
 */
public interface GenDataPath extends DataPath {

  GenDataDef getDataDef();

}
