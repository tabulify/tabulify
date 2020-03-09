package net.bytle.db.engine;

import java.util.List;

/**
 * The thread result on a process level
 */
public interface ThreadListener {


  List<RuntimeException> getExceptions();

  void addException(Exception e);

  int getExitStatus();

  String getErrorMessage();

}
