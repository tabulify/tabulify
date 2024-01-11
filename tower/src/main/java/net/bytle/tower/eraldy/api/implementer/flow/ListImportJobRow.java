package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import net.bytle.dns.DnsException;
import net.bytle.dns.DnsIp;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;
import net.bytle.tower.eraldy.model.openapi.*;
import net.bytle.tower.eraldy.objectProvider.ListRegistrationProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.type.time.TimeException;
import net.bytle.type.time.Timestamp;
import net.bytle.vertx.resilience.ValidationStatus;
import net.bytle.vertx.resilience.ValidationTestResult;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * A unit of execution for a row
 * that can be re-executed if there is a fatal error (timeout, servfail DNS, ...)
 */
public class ListImportJobRow {
  private final int rowId;
  private final ListImportJob listImportJob;
  private String email;
  private int statusCode;
  private String statusMessage;
  private int executionCount = 0;
  private String optInOrigin;
  private String familyName;
  private String givenName;
  private String optInIp;
  private String optInTime;
  private String confirmIp;
  private String confirmTime;
  private String location;

  public ListImportJobRow(ListImportJob listImportJob, int rowId) {
    this.rowId = rowId;
    this.listImportJob = listImportJob;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Future<ListImportJobRow> getExecutableFuture() {
    this.executionCount++;
    return this.listImportJob.getListImportFlow().getEmailAddressValidator()
      .validate(email, this.listImportJob.getFailEarly())
      .compose(emailAddressValidityReport -> {
        ValidationStatus emailValidityStatus = emailAddressValidityReport.getStatus();
        if (emailValidityStatus != ValidationStatus.LEGIT) {
          String message = emailAddressValidityReport.getErrors().stream().map(ValidationTestResult::getMessage).collect(Collectors.joining(", "));
          return this.closeExecution(emailValidityStatus, message);
        }
        UserProvider userProvider = this.listImportJob.getListImportFlow().getApp().getUserProvider();
        BMailInternetAddress emailInternetAddress;
        try {
          emailInternetAddress = emailAddressValidityReport.getEmailInternetAddress();
        } catch (NullValueException e) {
          return Future.failedFuture(new InternalException("Email address was null but the email was validated. It should not happen."));
        }
        ListItem list = this.listImportJob.getList();
        return userProvider.getUserByEmail(emailInternetAddress, list.getRealm().getLocalId(), User.class, list.getRealm())
          .compose(userFromRegistry -> {
            if (userFromRegistry != null) {
              if (this.listImportJob.getUpdateExistingUser()) {
                User patchUser = new User();
                if(!this.givenName.isBlank()) {
                  patchUser.setGivenName(this.givenName);
                }
                if(!this.familyName.isBlank()){
                  patchUser.setFamilyName(this.familyName);
                }
                if(!this.location.isBlank()){
                  patchUser.setLocation(this.location);
                }
                return userProvider.patchUserIfPropertyValueIsNull(userFromRegistry, patchUser);
              }
              return Future.succeededFuture(userFromRegistry);
            } else {
              User newUser = new User();
              newUser.setEmail(emailInternetAddress.toNormalizedString());
              newUser.setGivenName(this.givenName);
              newUser.setFamilyName(this.familyName);
              newUser.setLocation(this.location);
              newUser.setRealm(list.getRealm());
              return userProvider.insertUserFromImport(newUser);
            }
          })
          .compose(user -> {
            ListRegistrationProvider listRegistrationProvider = this.listImportJob.getListImportFlow().getApp().getListRegistrationProvider();
            ListRegistration listRegistration = new ListRegistration();
            listRegistration.setSubscriber(user);
            listRegistration.setList(list);
            listRegistration.setFlow(RegistrationFlow.IMPORT);
            listRegistration.setStatus(1);
            if (this.optInOrigin == null) {
              listRegistration.setOptInOrigin(RegistrationFlow.IMPORT.toString());
            } else {
              listRegistration.setOptInOrigin(this.optInOrigin);
            }
            if (optInIp != null) {
              DnsIp optInIpAsDnsIp;
              try {
                optInIpAsDnsIp = DnsIp.createFromString(optInIp);
              } catch (DnsException e) {
                return this.closeExecution(ValidationStatus.DATA_INVALID, "The optInIp (" + optInIp + ") is not a valid ipv4 or ipv6.");
              }
              listRegistration.setOptInIp(optInIpAsDnsIp.getAddress());
            }
            if (optInTime != null) {
              LocalDateTime optInTimeAsObject;
              try {
                optInTimeAsObject = Timestamp.createFromString(optInTime).toLocalDateTime();
              } catch (TimeException e) {
                return this.closeExecution(ValidationStatus.DATA_INVALID, "The optInTime (" + optInTime + ") is not a known time string.");
              }
              listRegistration.setOptInTime(optInTimeAsObject);
            }
            if (confirmIp != null) {
              DnsIp confirmIpAsDnsIp;
              try {
                confirmIpAsDnsIp = DnsIp.createFromString(confirmIp);
              } catch (DnsException e) {
                return this.closeExecution(ValidationStatus.DATA_INVALID, "The confirmIp (" + confirmIp + ") is not a valid ipv4 or ipv6.");
              }
              listRegistration.setConfirmationIp(confirmIpAsDnsIp.getAddress());
            }
            if (confirmTime != null) {
              LocalDateTime confirmTimeAsObject;
              try {
                confirmTimeAsObject = Timestamp.createFromString(confirmTime).toLocalDateTime();
              } catch (TimeException e) {
                return this.closeExecution(ValidationStatus.DATA_INVALID, "The confirmTime (" + confirmTime + ") is not a known time string.");
              }
              listRegistration.setConfirmationTime(confirmTimeAsObject);
            }
            return listRegistrationProvider.upsertRegistration(listRegistration)
              .compose(listRegistration1 -> {
                return Future.succeededFuture(this);
              });
          });

      });
  }

  private Future<ListImportJobRow> closeExecution(ValidationStatus status, String message) {
    this.setStatusMessage(message);
    this.setStatusCode(status.getStatusCode());
    return Future.succeededFuture(this);
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

  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public void setOptInOrigin(String optInOrigin) {
    this.optInOrigin = optInOrigin;
  }

  public void setOptInIp(String optInIp) {
    this.optInIp = optInIp;
  }

  public void setOptInTime(String optInTime) {
    this.optInTime = optInTime;
  }

  public void setConfirmIp(String confirmIp) {
    this.confirmIp = confirmIp;
  }

  public void setConfirmTime(String confirmTime) {
    this.confirmTime = confirmTime;
  }

  public void setLocation(String location) {
    this.location = location;
  }
}
