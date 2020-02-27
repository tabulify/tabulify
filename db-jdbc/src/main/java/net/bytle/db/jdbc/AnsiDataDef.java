package net.bytle.db.jdbc;

import net.bytle.db.model.RelationDef;
import net.bytle.db.model.TableDef;
import net.bytle.db.stream.SelectStream;

import static net.bytle.db.jdbc.AnsiDataPath.QUERY_TYPE;

public  class AnsiDataDef extends TableDef implements RelationDef {

  private final AnsiDataPath jdbcDataPath;
  private SqlSelectStream selectStream;


  /**
   * The construction happens before
   * @param dataPath
   */
  public AnsiDataDef(AnsiDataPath dataPath) {
    super(dataPath);
    this.jdbcDataPath = dataPath;

    // Do we have already a structure in the database
    if (dataPath.getDataStore().getDataSystem().exists(dataPath)){
      Ansis.buildAnsiDataDef(this);
    }

    // Does this data path a query is
    if (dataPath.getType().equals(QUERY_TYPE)) {
      // The select stream build the data def
      selectStream = SqlSelectStream.of(dataPath);
      selectStream.buildDataDef(this);
      // sqlite for instance
      if (this.jdbcDataPath.getDataStore().getMaxWriterConnection() == 1) {
        selectStream.close();
        selectStream = null;
      }
    }
  }

  /**
   * @return a select stream
   * <p>
   * The constructor {@link #AnsiDataDef(AnsiDataPath)} may have initialized this select stream
   * when the data path is a query
   */
  public SelectStream getSelectStream() {

    if (selectStream == null) {
      selectStream = SqlSelectStream.of(this.getDataPath());
    }
    return selectStream;

  }

  @Override
  public AnsiDataPath getDataPath(){
    return jdbcDataPath;
  }



}
