package net.bytle.vertx.resilience;

import java.util.Collection;
import java.util.Set;

public class DomainValidatorResult {


  private final Set<ValidationTestResult> validationTestResult;

  public DomainValidatorResult(Set<ValidationTestResult> validationTestResults) {
    this.validationTestResult = validationTestResults;
  }

  public Collection<ValidationTestResult> getResults() {
    return this.validationTestResult;
  }

}
