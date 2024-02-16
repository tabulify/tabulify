package net.bytle.vertx.resilience;

import io.vertx.core.json.JsonObject;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;
import net.bytle.type.EmailAddress;

import java.util.*;
import java.util.stream.Collectors;

public class EmailAddressValidatorReport {


  private final Builder builder;

  public EmailAddressValidatorReport(Builder builder) {
    this.builder = builder;
  }

  public static Builder builder(String email) {
    return new Builder(email);
  }


  public EmailAddressValidationStatus getStatus() {
    return this.builder.status;
  }

  public JsonObject toJsonObject() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("status", this.builder.status);
    jsonObject.put("email", this.builder.inputEmailAddress);
    JsonObject jsonResultsObjectMessage = new JsonObject();
    jsonObject.put("results", jsonResultsObjectMessage);
    Map<String, String> failed = this.builder
      .validationTestResults
      .stream()
      .filter(ValidationTestResult::hasFailed)
      .collect(Collectors.toMap(
        v -> v.getValidation().getName(),
        ValidationTestResult::getMessage
      ));
    if (!failed.isEmpty()) {
      jsonResultsObjectMessage.put("failed", failed);
    }
    Map<String, String> failures = this.builder
      .validationTestResults
      .stream()
      .filter(ValidationTestResult::hasFatalError)
      .collect(Collectors.toMap(
        v -> v.getValidation().getName(),
        ValidationTestResult::getMessage
      ));
    if (!failures.isEmpty()) {
      jsonResultsObjectMessage.put("failure", failures);
    }
    Map<String, String> passed = this.builder
      .validationTestResults
      .stream()
      .filter(ValidationTestResult::hasPassed)
      .collect(Collectors.toMap(
        v -> v.getValidation().getName(),
        ValidationTestResult::getMessage
      ));
    if (!passed.isEmpty()) {
      jsonResultsObjectMessage.put("passed", passed);
    }
    Map<String, String> skipped = this.builder
      .validationTestResults
      .stream()
      .filter(ValidationTestResult::hasSkipped)
      .collect(Collectors.toMap(
        v -> v.getValidation().getName(),
        ValidationTestResult::getMessage
      ));
    if (!skipped.isEmpty()) {
      jsonResultsObjectMessage.put("skipped", skipped);
    }
    return jsonObject;
  }

  public EmailAddress getEmailAddress() throws NullValueException {
    EmailAddress emailInternetAddress = this.builder.emailInternetAddress;
    if (emailInternetAddress == null) {
      throw new NullValueException("This method returns a value only if the address is valid.");
    }
    return emailInternetAddress;
  }

  /**
   * @return the results that have failed or have a fatal error
   * (not the skipped and passed results)
   */
  public List<ValidationTestResult> getErrors() {
    return this.builder.validationTestResults
      .stream()
      // fatal error and failed
      .filter(res -> res.hasFatalError() || res.hasFailed())
      .collect(Collectors.toList());
  }


  public static class Builder {

    private EmailAddress emailInternetAddress;
    private final String inputEmailAddress;
    public EmailAddressValidationStatus status;
    private final Set<ValidationTestResult> validationTestResults = new HashSet<>();

    public Builder(String inputEmailAddress) {
      this.inputEmailAddress = inputEmailAddress;
    }

    public Builder addResult(ValidationTestResult validationTestResult) {
      this.validationTestResults.add(validationTestResult);
      return this;
    }

    public EmailAddressValidatorReport build() {
      if (this.validationTestResults.isEmpty()) {
        throw new InternalException("A validation result should be added");
      }
      status = EmailAddressValidationStatus.LEGIT;
      for (ValidationTestResult validationTestResult : validationTestResults) {
        if (validationTestResult.hasFatalError()) {
          status = EmailAddressValidationStatus.FATAL_ERROR;
          break;
        }
        if (validationTestResult.hasFailed()) {
          EmailAddressValidationStatus emailAddressValidationStatus = validationTestResult.getValidation().getValidationType();
          if (emailAddressValidationStatus.getOrderOfPrecedence() > status.getOrderOfPrecedence()) {
            status = emailAddressValidationStatus;
          }
        }
      }
      return new EmailAddressValidatorReport(this);
    }

    public Builder addResults(Collection<ValidationTestResult> res) {
      validationTestResults.addAll(res);
      return this;
    }

    public void setEmailInternetAddress(EmailAddress mailInternetAddress) {
      this.emailInternetAddress = mailInternetAddress;
    }
  }
}
