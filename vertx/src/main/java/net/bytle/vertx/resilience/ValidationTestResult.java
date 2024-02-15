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

  public boolean hasFailed() {
    return this.builder.status == ValidationTestStatus.FAILED;
  }

  public boolean hasPassed() {
    return this.builder.status == ValidationTestStatus.PASS;
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
    return this.builder.status == ValidationTestStatus.FATAL_ERROR;
  }

  public boolean hasSkipped() {
    return this.builder.status == ValidationTestStatus.SKIPPED;
  }

  /**
   * The builder is also an exception, therefore we
   * can pass it when a Future fail.
   */
  static public class Builder extends Exception {

    private final ValidationTest validationTest;
    private String message;
    private ValidationTestStatus status;

    public Builder(ValidationTest validationTest) {
      this.validationTest = validationTest;
    }

    public Builder setMessage(String message) {
      this.message = message;
      return this;
    }

    /**
     * Fatal error could be retried
     * because they do depend on request condition such as network, disk
     * Example: timeout, host not found
     */
    public Builder setFatalError(Throwable fatalError) {
      return setFatalError(fatalError, null);
    }

    public Builder setFatalError(Throwable fatalError, String s) {
      this.setNonFatalError(fatalError, s);
      this.status = ValidationTestStatus.FATAL_ERROR;
      return this;
    }


    /**
     * Fail and succeed are the final methods
     * because the status should be consistent with the
     * status of the Future.
     * The status of the validation is then set
     * after the execution of the Future.
     */
    public ValidationTestResult succeed() {
      // edge case when the test has been skipped
      if (this.status == ValidationTestStatus.SKIPPED) {
        return new ValidationTestResult(this);
      }
      this.status = ValidationTestStatus.PASS;
      return new ValidationTestResult(this);
    }


    /**
     * Fail and succeed are the final methods
     * because the status should be consistent with the
     * status of the Future.
     * The status of the validation is then set
     * after the execution of the Future.
     */
    public ValidationTestResult fail() {
      // edge case when the test has been skipped or has a fatal error
      if (this.status != null) {
        return new ValidationTestResult(this);
      }
      this.status = ValidationTestStatus.FAILED;
      return new ValidationTestResult(this);
    }

    /**
     * Non-fatal error will not be retried
     * because they do not depend on request condition such as network, disk
     */
    public Builder setNonFatalError(Throwable error, String nonFatalMessage) {
      this.message = error.getMessage()+" ("+error.getClass().getSimpleName()+")";
      if (nonFatalMessage != null) {
        this.message = nonFatalMessage + " " + this.message;
      }
      return this;
    }

    public Builder skipped() {
      this.status = ValidationTestStatus.SKIPPED;
      return this;
    }

  }

}
