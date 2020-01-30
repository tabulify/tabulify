package net.bytle.db.stream;

import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;

import java.sql.Clob;
import java.util.List;
import java.util.concurrent.TimeUnit;

public interface SelectStream extends AutoCloseable {

  /**
   * If this method is returning data paths,
   * this data path are children that should be loaded synchronously
   * ie a call to
   *    SelectStream.getRow
   * should be immediately followed by a
   *    ReferenceSelectStream.getRow
   * because:
   *   * the data is generated in tandem (as TPCDS does for instance)
   *   * of we are loading an tree like file (xml, ..) that contains several data path in one file.
   *
   * @return the data path that should be loaded synchronously
   */
  List<DataPath> getReference();

  boolean next();

  void close();

  String getString(int columnIndex);

  int getRow();


  Object getObject(int columnIndex);


  TableDef getSelectDataDef();

  double getDouble(int columnIndex);

  Clob getClob(int columnIndex);

  /**
   * @param timeout  - timeout to retrieve an object from a queue implementation
   * @param timeUnit - timeUnit of the timeout
   */
  boolean next(Integer timeout, TimeUnit timeUnit);

  Integer getInteger(int columnIndex);

  Object getObject(String columnName);

  SelectStreamListener getSelectStreamListener();

  List<Object> getObjects();

  SelectStream setName(String name);

  /**
   * This function is used in two pass functions.
   * <p>
   * Example:
   * {@link Streams#print(SelectStream)} will first scan the rows to calculate the layout then print
   */
  void beforeFirst();


}
