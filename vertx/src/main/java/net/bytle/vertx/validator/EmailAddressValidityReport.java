package net.bytle.vertx.validator;

import io.vertx.core.json.JsonObject;
import net.bytle.exception.InternalException;
import net.bytle.vertx.ValidationResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class EmailAddressValidityReport {


  private final Builder builder;

  public EmailAddressValidityReport(Builder builder) {
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
        ValidationResult::getName,
        ValidationResult::getMessage
      )));
    jsonObjectMessage.put("success", this.builder
      .validationResults
      .stream()
      .filter(ValidationResult::pass)
      .collect(Collectors.toMap(
        ValidationResult::getName,
        ValidationResult::getMessage
      ))
    );
    return jsonObject;
  }

  public String getEmailAddress() {
    return this.builder.emailAddress;
  }

  public List<ValidationResult> getErrors() {
    return this.builder.validationResults.stream().filter(ValidationResult::fail).collect(Collectors.toList());
  }

  public List<ValidationResult> getReports() {
    return this.builder.validationResults;
  }


  public static class Builder {

    private final String emailAddress;
    public boolean pass;
    private final List<ValidationResult> validationResults = new ArrayList<>();

    public Builder(String emailAddress) {
      this.emailAddress = emailAddress;
    }

    public Builder addResult(ValidationResult validationResult) {
      this.validationResults.add(validationResult);
      return this;
    }

    public EmailAddressValidityReport build() {
      if (this.validationResults.isEmpty()) {
        throw new InternalException("A validation result should be added");
      }
      this.pass = true;
      for (ValidationResult validationResult : validationResults) {
        if (validationResult.fail()) {
          this.pass = false;
          break;
        }
      }
      return new EmailAddressValidityReport(this);
    }

    public Builder addResults(Collection<ValidationResult> res) {
      validationResults.addAll(res);
      return this;
    }
  }
}
