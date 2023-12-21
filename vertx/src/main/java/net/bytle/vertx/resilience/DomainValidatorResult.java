package net.bytle.vertx.resilience;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class DomainValidatorResult {


  private final Set<ValidationTestResult> validationTestResult = new HashSet<>();

  public DomainValidatorResult() {}

  public Collection<ValidationTestResult> getResults() {
    return this.validationTestResult;
  }

  public DomainValidatorResult addTest(ValidationTestResult validationTestResult) {
    this.validationTestResult.add(validationTestResult);
    return this;
  }

  public DomainValidatorResult addTests(Collection<ValidationTestResult> validationTestResults) {
    this.validationTestResult.addAll(validationTestResults);
    return this;
  }

}
