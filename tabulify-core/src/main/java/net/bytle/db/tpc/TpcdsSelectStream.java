package net.bytle.db.tpc;

import com.teradata.tpcds.Options;
import com.teradata.tpcds.Results;
import com.teradata.tpcds.Session;
import com.teradata.tpcds.Table;
import net.bytle.db.model.ColumnDef;
import net.bytle.db.model.RelationDef;
import net.bytle.db.spi.DataPath;
import net.bytle.db.stream.SelectStream;
import net.bytle.db.stream.SelectStreamAbs;
import net.bytle.db.stream.SelectStreamListener;
import net.bytle.exception.NoColumnException;
import net.bytle.exception.NoVariableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class TpcdsSelectStream extends SelectStreamAbs {

  // Tpcds data
  private Iterator<List<List<String>>> results;
  private List<String> values;
  private int row = 0;
  private SelectStreamListener selectStreamListener;
  private final Logger LOGGER = LoggerFactory.getLogger(TpcdsSelectStream.class);



  public TpcdsSelectStream(DataPath dataPath) {
    super(dataPath);
    init();
  }

  private void init() {

    if (!TpcdsModel.isStagingTable(this.getDataPath())) {

      // Teradata options
      Options options = new Options();
      Double scale;
      try {
        scale = (Double) this.getDataPath().getConnection().getVariable(TpcConnectionAttribute.SCALE).getValueOrDefaultOrNull();
      } catch (NoVariableException e) {
        scale = 0.001;
      }
      options.scale = scale;
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
  public RelationDef getRuntimeRelationDef() {
    return this.getDataPath().getOrCreateRelationDef();
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
