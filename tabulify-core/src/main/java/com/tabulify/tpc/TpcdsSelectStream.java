package com.tabulify.tpc;

import com.tabulify.model.ColumnDef;
import com.tabulify.spi.DataPath;
import com.tabulify.stream.SelectStream;
import com.tabulify.stream.SelectStreamAbs;
import com.tabulify.stream.SelectStreamListener;
import com.teradata.tpcds.Options;
import com.teradata.tpcds.Results;
import com.teradata.tpcds.Session;
import com.teradata.tpcds.Table;
import net.bytle.exception.NoColumnException;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class TpcdsSelectStream extends SelectStreamAbs {

  // Tpcds data
  private Iterator<List<List<String>>> results;
  private List<String> values;
  private int row = 0;
  private SelectStreamListener selectStreamListener;



  public TpcdsSelectStream(DataPath dataPath) {
    super(dataPath);
    init();
  }

  private void init() {

    if (!TpcdsModel.isStagingTable(this.getDataPath())) {

      // Teradata options
      Options options = new Options();
      options.scale = (Double) this.getDataPath().getConnection().getAttribute(TpcConnectionAttributeEnum.SCALE).getValueOrDefaultOrNull();
      options.noSexism = true;
      Session session = options.toSession();

      // Could be parallelized with
      // session.withChunkNumber(chunkNumber)
      options.table = this.getDataPath().getName();

      /**
       * The table representation in the tpcds library
       */
      Table table = Table.getTable(this.getDataPath().getName());

      results = Results.constructResults(table, session).iterator();

    }


  }



  public static TpcdsSelectStream create(DataPath dataPath) {
    return new TpcdsSelectStream(dataPath);
  }


  @Override
  public boolean next() {

    if (!TpcdsModel.isStagingTable(this.getDataPath())) {
      if (results.hasNext()) {
        row++;
        values = results.next().get(0);
        return true;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  public void close() {
    // Nothing to do
  }


  @Override
  public long getRow() {
    return row;
  }

  @Override
  public Object getObject(int columnIndex) {
    return values.get(columnIndex-1);
  }


  @Override
  public boolean next(Integer timeout, TimeUnit timeUnit) {
    return next();
  }


  @Override
  public Object getObject(String columnName) {
    ColumnDef columnDef;
    try {
      columnDef = this.getDataPath().getOrCreateRelationDef().getColumnDef(columnName);
    } catch (NoColumnException e) {
      throw new RuntimeException("Column name (" + columnName + ") not found in data document (" + this.getDataPath() + ")");
    }
    return values.get(columnDef.getColumnPosition()-1);
  }

  @Override
  public SelectStreamListener getSelectStreamListener() {
    if (selectStreamListener == null) {
      selectStreamListener = SelectStreamListener.of(this);
    }
    return selectStreamListener;
  }

  @Override
  public List<?> getObjects() {
    return values;
  }

  @Override
  public SelectStream setName(String name) {
    return this;
  }

  @Override
  public void beforeFirst() {
    init();
  }



}
