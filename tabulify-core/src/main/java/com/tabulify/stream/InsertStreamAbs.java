package com.tabulify.stream;

import com.tabulify.spi.DataPath;
import com.tabulify.spi.Tabulars;
import com.tabulify.transfer.TransferMethod;
import net.bytle.exception.InternalException;
import net.bytle.type.Casts;

import java.util.Arrays;
import java.util.List;

public abstract class InsertStreamAbs implements Stream, InsertStream, AutoCloseable {

  protected final DataPath dataPath;
  protected InsertStreamListener insertStreamListener = InsertStreamListener.create(this);

  protected String name = Thread.currentThread().getName();
  protected Integer feedbackFrequency = 10000;
  protected Integer batchSize = 1000;
  protected Integer commitFrequency = 100;
  protected int currentRowInLogicalBatch = 0;

  public InsertStreamAbs(DataPath dataPath) {

    this.dataPath = dataPath;
  }

  @Override
  public InsertStream setName(String name) {
    this.name = name;
    return this;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public InsertStream insert(Object... values) {

    int columnsSize = this.getDataPath()
      .getOrCreateRelationDef().getColumnsSize();

    List<Object> valuesToInsert;

    // Case when the first object is a list
    if (values.length == 1 && values[0] instanceof List) {
      valuesToInsert = Casts.castToNewListSafe(values[0], Object.class);
    } else {
      valuesToInsert = Arrays.asList(values);
    }

    if (valuesToInsert.size() != columnsSize && !Tabulars.isFreeSchemaForm(this.getDataPath())) {
      throw new InternalException("The number of values (" + valuesToInsert.size() + ") to insert does not match the number of columns (" + columnsSize + ") of the data path (" + this.getDataPath() + ")");
    }

    return this.insert(valuesToInsert);
  }


  @Override
  public InsertStreamListener getInsertStreamListener() {
    return this.insertStreamListener;
  }


  @Override
  public DataPath getDataPath() {
    return dataPath;
  }


  /**
   * Does the next insert will send data (ie a batch)
   * to the remote server
   */
  @Override
  public boolean flushAtNextInsert() {
    return (currentRowInLogicalBatch + 1 == batchSize);
  }


  @Override
  public TransferMethod getMethod() {
    return TransferMethod.INSERT;
  }

}
