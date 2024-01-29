package net.bytle.vertx.resilience;

import io.vertx.core.json.JsonObject;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    jsonObject.put("pass", this.builder.status);
    jsonObject.put("email", this.builder.inputEmailAddress);
    JsonObject jsonObjectMessage = new JsonObject();
    jsonObject.put("results", jsonObjectMessage);
    jsonObjectMessage.put("errors", this.getErrors()
      .stream()
      .collect(Collectors.toMap(
        v -> v.getValidation().getName(),
        ValidationTestResult::getMessage
      )));
    jsonObjectMessage.put("success", this.builder
      .validationTestResults
      .stream()
      .filter(ValidationTestResult::hasPassed)
      .collect(Collectors.toMap(
        v -> v.getValidation().getName(),
        ValidationTestResult::getMessage
      ))
    );
    return jsonObject;
  }

  public BMailInternetAddress getEmailInternetAddress() throws NullValueException {
    BMailInternetAddress emailInternetAddress = this.builder.emailInternetAddress;
    if (emailInternetAddress == null) {
      throw new NullValueException("This method returns a value only if the address is valid.");
    }
    return emailInternetAddress;
  }

  public List<ValidationTestResult> getErrors() {
    return this.builder.validationTestResults.stream().filter(ValidationTestResult::hasFailed).collect(Collectors.toList());
  }


  public static class Builder {

    private BMailInternetAddress emailInternetAddress;
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
        if (validationTestResult.hasFailed()) {
          if (validationTestResult.hasFatalError()) {
            status = EmailAddressValidationStatus.FATAL_ERROR;
            break;
          }
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

    public void setEmailInternetAddress(BMailInternetAddress mailInternetAddress) {
      this.emailInternetAddress = mailInternetAddress;
    }
  }
}
