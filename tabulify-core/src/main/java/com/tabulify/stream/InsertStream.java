package com.tabulify.stream;

import com.tabulify.transfer.TransferListener;
import com.tabulify.transfer.TransferMethod;

import java.util.List;

public interface InsertStream extends Stream, AutoCloseable {


  /**
   * @return the name of this stream
   * The table name if not set
   */
  String getName();

  /**
   * @param name - The name of this stream to be able to identify it in threads
   * @return the {@link InsertStream} for constructor chaining
   */
  InsertStream setName(String name);

  /**
   * @param values - The values to insert
   * @return the {@link InsertStream} for insert chaining
   */
  InsertStream insert(Object... values);

  /**
   * @param values - The values to insert in the same positional order than the source
   * @return the {@link InsertStream} for insert chaining
   */
  InsertStream insert(List<Object> values);


  /**
   * To send feedback and retrieve the exception if the input stream
   * is wrapped in a thread
   *
   * @return insertStreamListener or null if not implemented
   */
  InsertStreamListener getInsertStreamListener();

  /**
   * Close the stream
   * ie
   * * commit the last rows
   * * close the connection
   * * ...
   */
  void close();



  /**
   * Does the next insert will send a batch (ie flush to disk or to database)
   *
   * @return
   */
  boolean flushAtNextInsert();

  /**
   * In case of parent child hierarchy
   * we can check if we need to send the data with the function nextInsertSendBatch()
   * and send it with this function
   */
  void flush();


  /**
   *
   * @return the transfer method for any {@link TransferListener}
   */
  TransferMethod getMethod();

}
