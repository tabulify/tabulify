package net.bytle.db.engine;

import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.SelectStream;

import java.util.List;

public interface Relational {

  /**
   *
   * @return the foreign (ie parent data path)
   */
  List<DataPath> getForeignKeyDependencies();

  /**
   *
   * @return the data path that:
   *   * should be loaded (selected) at the same time
   *   * and where a call to the {@link SelectStream#next()} should happen before the select of this data path
   */
  List<DataPath> getSelectStreamDependencies();

}
