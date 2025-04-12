package com.tabulify.engine;

import com.tabulify.spi.DataPath;
import com.tabulify.stream.SelectStream;
import net.bytle.exception.NotFoundException;

/**
 * Just an interface that extract then relationship function
 * There is no other benefits than that
 */
public interface StreamDependencies {


  /**
   *
   * A select stream dependency is used in the transfer of data.
   *
   * @return the data path that:
   *   * should be loaded (selected) at the same time
   *   * and where a call to the {@link SelectStream#next()} should happen before the select of this data path
   *
   * Every stream dependencies that you get with this function should also have a {@link net.bytle.dag.Dependency#getDependencies()}
   *
   * This was introduced because of the data generation of tpcds that happens for some table in tandem:ie
   * The following tables must be generated together.
   *   * store_returns and store_sales
   *   * catalog_returns and catalog_sales
   *   * web_returns and web_sales
   * The data for sales must be generated (with sales_stream.next) then immediately the data for returns (with returns_stream.next)
   *
   * A data generation relationship is needed when transferring (generating) data
   *
   * If this method is returning data paths,
   * this data path are children that should be loaded synchronously
   * ie a call to
   *    SelectStream.getRow
   * should be executed after a
   *    ParentSelectStream.getRow
   * because:
   *    * the data is generated in tandem (as TPCDS does for instance, it generate the returns at the same time that the sales)
   *    * of we are loading an tree like file (xml, ..) that contains several data path in one file.
   */
  DataPath getSelectStreamDependency() throws NotFoundException;

}
