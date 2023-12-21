package net.bytle.vertx.resilience;

import io.vertx.core.json.JsonObject;
import net.bytle.exception.InternalException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class EmailAddressValidatorReport {


  private final Builder builder;

  public EmailAddressValidatorReport(Builder builder) {
    this.builder = builder;
  }

  public static Builder builder(String email) {
    return new Builder(email);
  }


  public boolean pass() {
    return this.builder.pass;
  }

  public JsonObject toJsonObject() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.put("pass", this.builder.pass);
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
    return this.builder.validationTestResults;
  }


  public static class Builder {

    private final String emailAddress;
    public boolean pass;
    private final List<ValidationTestResult> validationTestResults = new ArrayList<>();

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
      this.pass = true;
      for (ValidationTestResult validationTestResult : validationTestResults) {
        if (validationTestResult.fail()) {
          this.pass = false;
          break;
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
