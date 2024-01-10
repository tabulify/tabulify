package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;
import net.bytle.tower.eraldy.model.openapi.ListImportJobRowStatus;
import net.bytle.tower.eraldy.model.openapi.ListItem;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.vertx.resilience.ValidationStatus;
import net.bytle.vertx.resilience.ValidationTestResult;

import java.util.stream.Collectors;

/**
 * A unit of execution for a row
 * that can be re-executed if there is a fatal error (timeout, servfail DNS, ...)
 */
public class ListImportJobRow {
  private final int rowId;
  private final boolean failEarly;
  private final ListImportJob listImportJob;
  private String email;
  private int statusCode;
  private String statusMessage;
  private int executionCount = 0;

  public ListImportJobRow(ListImportJob listImportJob, int rowId, boolean failEarly) {
    this.rowId = rowId;
    this.failEarly = failEarly;
    this.listImportJob = listImportJob;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Future<ListImportJobRow> getExecutableFuture() {
    this.executionCount++;
    return this.listImportJob.getListImportFlow().getEmailAddressValidator()
      .validate(email, failEarly)
      .compose(emailAddressValidityReport -> {
        int emailValidityStatus = emailAddressValidityReport.getStatus().getStatusCode();
        if (emailValidityStatus != ValidationStatus.LEGIT.getStatusCode()) {
          this.setStatusCode(emailValidityStatus);
          this.setStatusMessage(emailAddressValidityReport.getErrors().stream().map(ValidationTestResult::getMessage).collect(Collectors.joining(", ")));
          return Future.succeededFuture(this);
        }
        UserProvider userProvider = this.listImportJob.getListImportFlow().getApp().getUserProvider();
        BMailInternetAddress emailInternetAddress;
        try {
          emailInternetAddress = emailAddressValidityReport.getEmailInternetAddress();
        } catch (NullValueException e) {
          return Future.failedFuture(new InternalException("Email address was null but the email was validated. It should not happen."));
        }
        ListItem list = this.listImportJob.getList();
        Future<User> user = userProvider.getUserByEmail(emailInternetAddress, list.getRealm().getLocalId(), User.class, list.getRealm());
        if(user==null){

        }
        return Future.succeededFuture(this);
      });
  }

  private void setStatusMessage(String message) {
    this.statusMessage = message;
  }

  private void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public ListImportJobRowStatus toListJobRowStatus() {
    ListImportJobRowStatus jobRowStatus = new ListImportJobRowStatus();
    jobRowStatus.setEmailAddress(this.email);
    jobRowStatus.setStatusCode(this.statusCode);
    String statusMessage = this.statusMessage;
    if (statusCode == ValidationStatus.FATAL_ERROR.getStatusCode()) {
      statusMessage += " . After " + this.executionCount + " attempts";
    }
    jobRowStatus.setStatusMessage(statusMessage);
    return jobRowStatus;
  }

  public Integer getStatus() {
    return this.statusCode;
  }

  public int getRowId() {
    return rowId;
  }

}
