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
    /**
     * Map and other structure requires a non-null value
     * NPE fight continues
     */
    return Objects.requireNonNullElse(this.builder.message, "");
  }

  public boolean hasFatalError() {
    return this.builder.fatalError != null;
  }

  /**
   * The builder is also an exception, therefore we
   * can pass it when a Future fail.
   */
  static public class Builder extends Exception {

    private final ValidationTest validationTest;
    private String message;
    private Boolean pass;
    private Throwable fatalError;

    public Builder(ValidationTest validationTest) {
      this.validationTest = validationTest;
    }

    public Builder setMessage(String message) {
      this.message = message;
      return this;
    }

    public Builder setFatalError(Throwable fatalError) {
      return setFatalError(fatalError, null);
    }

    public Builder setFatalError(Throwable fatalError, String s) {
      this.fatalError = fatalError;
      this.setNonFatalError(fatalError,s);
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


    public Builder setNonFatalError(Throwable error, String s) {
      this.message = error.getClass().getSimpleName() + ": " + error.getMessage();
      if (s != null) {
        this.message = s + ". " + this.message;
      }
      return this;
    }
  }

}
