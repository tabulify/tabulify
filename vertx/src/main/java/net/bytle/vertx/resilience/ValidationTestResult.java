package net.bytle.vertx.resilience;

import java.util.Objects;

public class ValidationTestResult {


  private final Builder builder;


  static public Builder builder(ValidationTest validationTest) {
    return new Builder(validationTest);
  }

  public ValidationTestResult(Builder builder) {
    this.builder = builder;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ValidationTestResult that = (ValidationTestResult) o;
    return Objects.equals(builder.validationTest, that.builder.validationTest);
  }

  @Override
  public int hashCode() {
    return Objects.hash(builder.validationTest);
  }

  public boolean fail() {
    return !this.builder.pass;
  }
  public boolean pass() {
    return this.builder.pass;
  }

  public ValidationTest getValidation() {
    return this.builder.validationTest;
  }

  public String getMessage() {
    return this.builder.message;
  }

  /**
   * The builder is also an exception, therefore we
   * can pass it when a Future fail.
   */
  static public class Builder extends Exception {

    private final ValidationTest validationTest;
    private String message;
    private Boolean pass;

    public Builder(ValidationTest validationTest) {
      this.validationTest = validationTest;
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
    public ValidationTestResult succeed() {
      this.pass = true;
      return new ValidationTestResult(this);
    }


    /**
     * Fail and succeed are the final methods
     * because the status should be consistent with the
     * status of Future.
     * The status of the validation is then set
     * after the execution of the Future.
     */
    public ValidationTestResult fail() {
      this.pass = false;
      return new ValidationTestResult(this);
    }



  }

}
