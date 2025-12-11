package com.tabulify.memory;

import com.tabulify.spi.DataPath;
import com.tabulify.spi.SelectException;
import com.tabulify.stream.InsertStream;
import com.tabulify.stream.SelectStream;

/**
 * A memory data path does not have any special property
 * <p>
 * The memory data path are managed by the java garbage collector
 * All store operations (such as truncate, size, ...) are then on the memory data path variable
 * <p>
 * TODO: To delete an integrate into the {@link MemoryDataPathAbs}
 */
public interface MemoryDataPath extends DataPath {



  String PATH_SEPARATOR = "/";

  MemoryConnection getConnection();


  MemoryDataPath resolve(String stringPath);


  void truncate();


  /**
   * Create (ie initialize the variable)
   */
  void create();

  InsertStream getInsertStream();

  SelectStream getSelectStream() throws SelectException;

  /**
   * Utility function to set quickly the value to be a text
   */
  MemoryDataPath setContent(String text);

  MemoryDataPath setLogicalName(String logicalName);

}
