package net.bytle.db.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ThreadListenerAbs implements ThreadListener {


  private List<Exception> exceptions = new ArrayList<>();

  @Override
  public List<Exception> getExceptions() {
    return this.exceptions;
  }

  @Override
  public void addException(Exception e) {
    RuntimeException run;
    if (e.getClass().equals(RuntimeException.class)) {
      run = (RuntimeException) e;
    } else {
      run = new RuntimeException(e);
    }
    this.exceptions.add(run);
  }

  @Override
  public int getExitStatus() {
    return exceptions.size();
  }

  @Override
  public List<String> getErrorMessages() {
    return getExceptions()
      .stream()
      .map(Throwable::getMessage)
      .collect(Collectors.toList());
  }

}
