package net.bytle.vertx.resilience;

import io.vertx.core.json.JsonObject;
import net.bytle.exception.InternalException;

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


  public ValidationStatus getStatus() {
    return this.builder.status;
  }

  public JsonObject toJsonObject() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("pass", this.builder.status);
    jsonObject.put("email", this.builder.emailAddress);
    JsonObject jsonObjectMessage = new JsonObject();
    jsonObject.put("results", jsonObjectMessage);
    jsonObjectMessage.put("errors", this.getErrors()
      .stream()
      .collect(Collectors.toMap(
        ValidationTestResult::getValidation,
        ValidationTestResult::getMessage
      )));
    jsonObjectMessage.put("success", this.builder
      .validationTestResults
      .stream()
      .filter(ValidationTestResult::pass)
      .collect(Collectors.toMap(
        ValidationTestResult::getValidation,
        ValidationTestResult::getMessage
      ))
    );
    return jsonObject;
  }

  public String getEmailAddress() {
    return this.builder.emailAddress;
  }

  public List<ValidationTestResult> getErrors() {
    return this.builder.validationTestResults.stream().filter(ValidationTestResult::fail).collect(Collectors.toList());
  }

  public List<ValidationTestResult> getReports() {
    return new ArrayList<>(this.builder.validationTestResults);
  }


  public static class Builder {

    private final String emailAddress;
    public ValidationStatus status;
    private final Set<ValidationTestResult> validationTestResults = new HashSet<>();

    public Builder(String emailAddress) {
      this.emailAddress = emailAddress;
    }

    public Builder addResult(ValidationTestResult validationTestResult) {
      this.validationTestResults.add(validationTestResult);
      return this;
    }

    public EmailAddressValidatorReport build() {
      if (this.validationTestResults.isEmpty()) {
        throw new InternalException("A validation result should be added");
      }
      status = ValidationStatus.LEGIT;
      for (ValidationTestResult validationTestResult : validationTestResults) {
        if (validationTestResult.fail()) {
          if(validationTestResult.hasFatalError()){
            status = ValidationStatus.FATAL_ERROR;
            break;
          }
          ValidationStatus validationStatus = validationTestResult.getValidation().getValidationType();
          if (validationStatus.getOrderOfPrecedence() > status.getOrderOfPrecedence()) {
            status = validationStatus;
          }
        }
      }
      return new EmailAddressValidatorReport(this);
    }

    public Builder addResults(Collection<ValidationTestResult> res) {
      validationTestResults.addAll(res);
      return this;
    }
  }
}
