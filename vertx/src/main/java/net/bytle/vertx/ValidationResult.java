package net.bytle.vertx;

import java.util.Objects;

public class ValidationResult extends Exception {


  private final Builder builder;

  static public Builder builder(String name) {
    return new Builder(name);
  }

  public ValidationResult(Builder builder) {
    super(builder.message);
    this.builder = builder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ValidationResult that = (ValidationResult) o;
    return Objects.equals(builder.name, that.builder.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(builder.name);
  }

  public boolean fail() {
    return !this.builder.pass;
  }
  public boolean pass() {
    return this.builder.pass;
  }

  public String getName() {
    return this.builder.name;
  }

  static public class Builder {

    private final String name;
    private String message;
    private Boolean pass;

    public Builder(String message) {
      this.name = message;
    }

    public ValidationResult fail(String message) {
      this.pass = false;
      this.message = message;
      return new ValidationResult(this);
    }

    public ValidationResult succeed(String message) {
      this.pass = true;
      this.message = message;
      return new ValidationResult(this);
    }

    public ValidationResult fail(Throwable err) {
      return fail(err, null);
    }

    public ValidationResult fail(Throwable err, String message) {
      String failingMessage = err.getClass().getSimpleName() + ": " + err.getMessage();
      if(message!=null){
        failingMessage = message+". "+failingMessage;
      }
      return fail(failingMessage);
    }
  }

}
