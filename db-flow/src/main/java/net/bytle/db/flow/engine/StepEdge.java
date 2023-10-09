package net.bytle.db.flow.engine;

public class StepEdge {

  private boolean b = false;

  public void setFailure(boolean b) {
    this.b = b;
  }


  public boolean getFailure() {
    return this.b;
  }
}
