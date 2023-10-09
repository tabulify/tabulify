package net.bytle.dag;

public abstract class DependencyAbs implements Dependency {

  @Override
  public boolean equals(Object obj) {
    return this.getId().equals(((Dependency) obj).getId());
  }

  @Override
  public int hashCode() {
    return this.getId().hashCode();
  }

}
