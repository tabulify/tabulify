package net.bytle.db.tpc;

import com.teradata.tpcds.Options;
import com.teradata.tpcds.Results;
import com.teradata.tpcds.Session;
import com.teradata.tpcds.Table;
import net.bytle.db.model.DataDefs;
import net.bytle.db.model.TableDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.stream.SelectStreamListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Clob;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class TpcdsSelectStream implements SelectStream {

  private final DataPath dataPath;
  // Tpcds data
  private Iterator<List<List<String>>> results;
  private List<String> values;
  private int row = 0;
  private SelectStreamListener selectStreamListener;
  private String name;
  /**
   * The table representation in the tpcds library
   */
  private Table table;
  private Logger LOGGER = LoggerFactory.getLogger(TpcdsSelectStream.class);;


  public TpcdsSelectStream(DataPath dataPath) {
    this.dataPath = dataPath;

    init();
  }

  private void init() {

    // Teradata options
    Options options = new Options();
    Double scale = dataPath.getDataSystem().getDataStore().getPropertyAsDouble(TpcDataSetSystem.SCALE);
    if (scale==null){
      scale = 0.01;
    }
    options.scale=scale;
    Session session = options.toSession();

    // Could be parallelize with
    // session.withChunkNumber(chunkNumber)
    options.table = dataPath.getName();

    if (!dataPath.getName().startsWith("s_")) {
      table = Table.getTable(dataPath.getName());
    } else {
      String msg = "The tpcds staging table (ie table that start with a s) are not yet supported. Data Path (" + dataPath + ") cannot be read.";
      LOGGER.error(msg);
      throw new RuntimeException(msg);
    }

    results = Results.constructResults(table, session).iterator();
  }

  public static TpcdsSelectStream of(DataPath dataPath) {
    return new TpcdsSelectStream(dataPath);
  }



  @Override
  public boolean next() {
    if (results.hasNext()) {
      row++;
      values = results.next().get(0);
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void close() {
    // Nothing to do
  }

  @Override
  public String getString(int columnIndex) {
    return values.get(columnIndex);
  }

  @Override
  public int getRow() {
    return row;
  }

  @Override
  public Object getObject(int columnIndex) {
    return values.get(columnIndex);
  }

  @Override
  public TableDef getSelectDataDef() {
    return dataPath.getDataDef();
  }

  @Override
  public double getDouble(int columnIndex) {
    return Double.parseDouble(values.get(columnIndex));
  }

  @Override
  public Clob getClob(int columnIndex) {
    throw new RuntimeException("Tpcds does not have a clob data type");
  }

  @Override
  public boolean next(Integer timeout, TimeUnit timeUnit) {
    return next();
  }

  @Override
  public Integer getInteger(int columnIndex) {
    return Integer.valueOf(values.get(columnIndex));
  }

  @Override
  public Object getObject(String columnName) {
    int i = DataDefs.getColumnIdFromName(dataPath.getDataDef(), columnName);
    return values.get(i);
  }

  @Override
  public SelectStreamListener getSelectStreamListener() {
    if (selectStreamListener == null) {
      selectStreamListener = SelectStreamListener.of(this);
    }
    return selectStreamListener;
  }

  @Override
  public List<Object> getObjects() {
    return Collections.singletonList(values);
  }

  @Override
  public SelectStream setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public void beforeFirst() {
    init();
  }

  @Override
  public void execute() {
    // nothing to do here
  }
}
