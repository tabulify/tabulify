package net.bytle.db.textline;

import net.bytle.db.model.RelationDef;
import net.bytle.db.stream.SelectStreamAbs;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.Clob;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LineSelectStream extends SelectStreamAbs {


  private final LineDataPath lineDataPath;
  private BufferedReader br;

  /**
   * The line number in the file
   */
  private long lineNumberInTextFile = 0;
  private String line;

  LineSelectStream(LineDataPath lineDataPath) {

    super(lineDataPath);
    this.lineDataPath = lineDataPath;
    buildDataDef(lineDataPath.getDataDef());
    beforeFirst();
  }

  public static LineSelectStream of(LineDataPath lineDataPath) {

    return new LineSelectStream(lineDataPath);

  }


  @Override
  public boolean next() {
    try {
      line = br.readLine();
      if (line == null){
        return false;
      } else {
        lineNumberInTextFile++;
        return true;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


  @Override
  public void close() {
    try {
      br.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String getString(int columnIndex) {

    return safeGet(columnIndex);

  }

  private String safeGet(int columnIndex) {
    if (columnIndex==0){
      return line;
    } else {
      throw new RuntimeException("A line file structure has only one column. Therefore, you can ask the column ("+columnIndex+") ");
    }
  }


  @Override
  public void beforeFirst() {
    try {
      lineNumberInTextFile = 0;
      br = Files.newBufferedReader(lineDataPath.getNioPath(), lineDataPath.getDataDef().getCharset());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void execute() {
    // no external request, nothing to do
  }


  @Override
  public long getRow() {
    return lineNumberInTextFile;
  }


  @Override
  public Object getObject(int columnIndex) {
    return safeGet(columnIndex);
  }

  @Override
  public void buildDataDef(RelationDef relationDef) {
    if (relationDef.getColumnsSize()==0) {
      // One column only
      relationDef.addColumn("Lines");
    }
  }


  @Override
  public Double getDouble(int columnIndex) {

    String s = safeGet(columnIndex);
    if (s == null) {
      return null;
    } else {
      return Double.parseDouble(s);
    }

  }

  @Override
  public Clob getClob(int columnIndex) {
    throw new RuntimeException("Not Yet implemented");
  }


  /**
   * Retrieves and removes the head of this data path, or returns false if this queue is empty.
   *
   * @param timeout  - the time out before returning null
   * @param timeUnit - the time unit of the time out
   * @return true if there is a new element, otherwise false
   */
  @Override
  public boolean next(Integer timeout, TimeUnit timeUnit) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override
  public List<Object> getObjects() {
    return Arrays.asList(line);
  }

  @Override
  public Integer getInteger(int columnIndex) {
    String s = safeGet(columnIndex);
    if (s == null) {
      return null;
    } else {
      return Integer.parseInt(s);
    }
  }

  @Override
  public Object getObject(String columnName) {
    return getObject(this.lineDataPath.getDataDef().getColumnDef(columnName).getColumnPosition());
  }


}
