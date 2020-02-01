package net.bytle.db.engine;

import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.SelectStream;

import java.util.List;

/**
 * Just an interface that extract then relationship function
 * There is no other benefits than that
 */
public interface Relational {

  /**
   *
   * @return the foreign (ie parent data path)
   */
  List<DataPath> getForeignKeyDependencies();

  /**
   *
   * A select stream dependency is used in the transfer of data.
   *
   * @return the data path that:
   *   * should be loaded (selected) at the same time
   *   * and where a call to the {@link SelectStream#next()} should happen before the select of this data path
   *
   * Every stream dependencies that you get with this function should also have a {@link #getForeignKeyDependencies()}
   *
   * This was introduced because of the data generation of tpcds that happens for some table in tandem:ie
   * The following tables must be generated together.
   *   * store_returns and store_sales
   *   * catalog_returns and catalog_sales
   *   * web_returns and web_sales
   * The data for sales must be generated (with sales_stream.next) then immediately the data for returns (with returns_stream.next)
   *
   */
  List<DataPath> getSelectStreamDependencies();

}
