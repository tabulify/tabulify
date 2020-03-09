package net.bytle.db.stream;

import net.bytle.db.spi.DataPath;

import java.sql.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public abstract class SelectStreamAbs implements Stream, SelectStream {


  private final DataPath dataPath;
  protected SelectStreamListener selectStreamListener = SelectStreamListener.of(this);
  private String name;


  public SelectStreamAbs(DataPath dataPath) {

    this.dataPath = dataPath;

  }

  /**
   * Retrieves and removes the head of this data path, or returns null if this queue is empty.
   *
   * @param timeout
   * @param timeUnit
   * @return
   */
  public abstract boolean next(Integer timeout, TimeUnit timeUnit);

  @Override
  public SelectStreamListener getSelectStreamListener() {
    return this.selectStreamListener;
  }

  @Override
  public SelectStream setName(String name) {
    this.name = name;
    return this;
  }


  @Override
  public abstract List<Object> getObjects();

  @Override
  public DataPath getDataPath() {
    return this.dataPath;
  }

  @Override
  public Date getDate(int columnIndex) {
    return (Date) getObject(columnIndex);
  }

}
