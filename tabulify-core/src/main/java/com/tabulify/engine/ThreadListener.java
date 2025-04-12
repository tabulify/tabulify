package com.tabulify.engine;

import java.util.List;

/**
 * The thread result on a process level
 */
public interface ThreadListener {


  List<Exception> getExceptions();

  void addException(Exception e);

  int getExitStatus();

  List<String> getErrorMessages();

}
