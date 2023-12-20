package net.bytle.vertx;

import java.util.Objects;

public class ValidationResult  {


  private final Builder builder;


  static public Builder builder(String name) {
    return new Builder(name);
  }

  public ValidationResult(Builder builder) {
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

  public String getMessage() {
    return this.builder.message;
  }

  /**
   * The builder is also an exception, therefore we
   * can pass it when a Future fail.
   */
  static public class Builder extends Exception {

    private final String name;
    private String message;
    private Boolean pass;

    public Builder(String name) {
      this.name = name;
    }

    public Builder setMessage(String message) {
      this.message = message;
      return this;
    }

    public Builder setError(Throwable err) {
      return setError(err, null);
    }
    public Builder setError(Throwable err, String s) {
      this.message = err.getClass().getSimpleName() + ": " + err.getMessage();
      if(s!=null){
        this.message = s+". "+this.message;
      }
      return this;
    }



    /**
     * Fail and succeed are the final methods
     * because the status should be consistent with the
     * status of Future.
     * The status of the validation is then set
     * after the execution of the Future.
     */
    public ValidationResult succeed() {
      this.pass = true;
      return new ValidationResult(this);
    }


    /**
     * Fail and succeed are the final methods
     * because the status should be consistent with the
     * status of Future.
     * The status of the validation is then set
     * after the execution of the Future.
     */
    public ValidationResult fail() {
      this.pass = false;
      return new ValidationResult(this);
    }



  }

}
