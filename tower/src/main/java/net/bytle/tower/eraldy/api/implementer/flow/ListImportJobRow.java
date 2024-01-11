package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import net.bytle.dns.DnsException;
import net.bytle.dns.DnsIp;
import net.bytle.email.BMailInternetAddress;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;
import net.bytle.tower.eraldy.model.openapi.ListItem;
import net.bytle.tower.eraldy.model.openapi.ListRegistration;
import net.bytle.tower.eraldy.model.openapi.RegistrationFlow;
import net.bytle.tower.eraldy.model.openapi.User;
import net.bytle.tower.eraldy.objectProvider.ListRegistrationProvider;
import net.bytle.tower.eraldy.objectProvider.UserProvider;
import net.bytle.type.time.Timestamp;
import net.bytle.vertx.resilience.EmailAddressValidationStatus;
import net.bytle.vertx.resilience.ValidationTestResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

/**
 * A unit of execution for a row
 * that can be re-executed if there is a fatal error (timeout, servfail DNS, ...)
 */
public class ListImportJobRow {

  static final Logger LOGGER = LogManager.getLogger(ListImportJobRow.class);
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
  private String userGuid;
  private boolean userAdded = false;
  private boolean userCreated = false;
  private boolean userUpdated = false;

  public ListImportJobRow(ListImportJob listImportJob, int rowId) {
    this.rowId = rowId;
    this.listImportJob = listImportJob;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Future<ListImportJobRow> getExecutableFuture() {
    return this.getExecutableFutureWithoutErrorHandling()
      .compose(
        listImportJobRow -> Future.succeededFuture(this),
        err -> {
          // database timeout for instance
          this.closeExecution(ListImportJobRowStatus.FATAL_ERROR, err.getMessage() + " (" + err.getClass().getSimpleName() + ")");
          LOGGER.error("A fatal error has occurred on the row (" + this.rowId + ", " + this.email + ") with the list import job (" + listImportJob.getList().getGuid() + "," + listImportJob.getStatus().getJobId() + ")", err);
          return Future.succeededFuture(this);
        }
      );
  }

  public Future<ListImportJobRow> getExecutableFutureWithoutErrorHandling() {
    this.executionCount++;
    return this.listImportJob.getListImportFlow().getEmailAddressValidator()
      .validate(email, this.listImportJob.getFailEarly())
      .compose(emailAddressValidityReport -> {
        EmailAddressValidationStatus emailValidityStatus = emailAddressValidityReport.getStatus();
        if (emailValidityStatus != EmailAddressValidationStatus.LEGIT) {
          String message = emailAddressValidityReport.getErrors().stream().map(ValidationTestResult::getMessage).collect(Collectors.joining(", "));
          ListImportJobRowStatus listImportJobStatus;
          switch (emailValidityStatus) {
            case FATAL_ERROR:
              listImportJobStatus = ListImportJobRowStatus.FATAL_ERROR;
              break;
            case HARD_BAN:
              listImportJobStatus = ListImportJobRowStatus.HARD_BAN;
              break;
            case SOFT_BAN:
              listImportJobStatus = ListImportJobRowStatus.SOFT_BAN;
              break;
            case EMAIL_ADDRESS_INVALID:
              listImportJobStatus = ListImportJobRowStatus.EMAIL_ADDRESS_INVALID;
              break;
            case DOMAIN_BLOCKED:
              listImportJobStatus = ListImportJobRowStatus.DOMAIN_BLOCKED;
              break;
            case DOMAIN_SUSPICIOUS:
              listImportJobStatus = ListImportJobRowStatus.DOMAIN_SUSPICIOUS;
              break;
            default:
              listImportJobStatus = ListImportJobRowStatus.EMAIL_ADDRESS_INVALID;
              message = "Email validity mapping is missing. The real status is (" + emailValidityStatus + "). " + message;
              break;
          }
          return this.closeExecution(listImportJobStatus, message);
        }
        UserProvider userProvider = this.listImportJob.getListImportFlow().getApp().getUserProvider();
        BMailInternetAddress emailInternetAddress;
        try {
          emailInternetAddress = emailAddressValidityReport.getEmailInternetAddress();
        } catch (NullValueException e) {
          return Future.failedFuture(new InternalException("Email address was null but the email was validated. It should not happen."));
        }
        ListItem list = this.listImportJob.getList();
        return userProvider
          .getUserByEmail(emailInternetAddress, list.getRealm().getLocalId(), User.class, list.getRealm())
          .compose(userFromRegistry -> {
            if (userFromRegistry != null) {
              if (this.listImportJob.getUpdateExistingUser()) {
                User patchUser = new User();
                boolean updateUser = false;
                if (!this.givenName.isBlank()) {
                  patchUser.setGivenName(this.givenName);
                  updateUser = true;
                }
                if (!this.familyName.isBlank()) {
                  patchUser.setFamilyName(this.familyName);
                  updateUser = true;
                }
                if (!this.location.isBlank()) {
                  patchUser.setLocation(this.location);
                  updateUser = true;
                }
                if (updateUser) {
                  this.userUpdated = true;
                  return userProvider.patchUserIfPropertyValueIsNull(userFromRegistry, patchUser);
                }
              }
              return Future.succeededFuture(userFromRegistry);
            } else {
              User newUser = new User();
              newUser.setEmail(emailInternetAddress.toNormalizedString());
              newUser.setGivenName(this.givenName);
              newUser.setFamilyName(this.familyName);
              newUser.setLocation(this.location);
              newUser.setRealm(list.getRealm());
              this.userCreated = true;
              return userProvider.insertUserFromImport(newUser);
            }
          })
          .compose(user -> {
            this.userGuid = user.getGuid();
            ListRegistrationProvider listRegistrationProvider = this.listImportJob.getListImportFlow().getApp().getListRegistrationProvider();
            return listRegistrationProvider.
              getRegistrationByListAndUser(list, user)
              .compose(listRegistration -> {
                if (listRegistration == null) {
                  this.userAdded = false;
                  return this.closeExecution(ListImportJobRowStatus.COMPLETED, null);
                }
                ListRegistration listRegistrationToInsert = new ListRegistration();
                listRegistrationToInsert.setSubscriber(user);
                listRegistrationToInsert.setList(list);
                listRegistrationToInsert.setFlow(RegistrationFlow.IMPORT);
                listRegistrationToInsert.setStatus(1);
                if (this.optInOrigin == null) {
                  listRegistrationToInsert.setOptInOrigin(RegistrationFlow.IMPORT.toString());
                } else {
                  listRegistrationToInsert.setOptInOrigin(this.optInOrigin);
                }
                if (optInIp != null) {
                  DnsIp optInIpAsDnsIp;
                  try {
                    optInIpAsDnsIp = DnsIp.createFromString(optInIp);
                  } catch (DnsException e) {
                    return this.closeExecution(ListImportJobRowStatus.DATA_INVALID, "The optInIp (" + optInIp + ") is not a valid ipv4 or ipv6.");
                  }
                  listRegistrationToInsert.setOptInIp(optInIpAsDnsIp.getAddress());
                }
                if (optInTime != null) {
                  LocalDateTime optInTimeAsObject;
                  try {
                    optInTimeAsObject = Timestamp.createFromString(optInTime).toLocalDateTime();
                  } catch (CastException e) {
                    return this.closeExecution(ListImportJobRowStatus.DATA_INVALID, "The optInTime (" + optInTime + ") is not a known time string.");
                  }
                  listRegistrationToInsert.setOptInTime(optInTimeAsObject);
                }
                if (confirmIp != null) {
                  DnsIp confirmIpAsDnsIp;
                  try {
                    confirmIpAsDnsIp = DnsIp.createFromString(confirmIp);
                  } catch (DnsException e) {
                    return this.closeExecution(ListImportJobRowStatus.DATA_INVALID, "The confirmIp (" + confirmIp + ") is not a valid ipv4 or ipv6.");
                  }
                  listRegistrationToInsert.setConfirmationIp(confirmIpAsDnsIp.getAddress());
                }
                if (confirmTime != null) {
                  LocalDateTime confirmTimeAsObject;
                  try {
                    confirmTimeAsObject = Timestamp.createFromString(confirmTime).toLocalDateTime();
                  } catch (CastException e) {
                    return this.closeExecution(ListImportJobRowStatus.DATA_INVALID, "The confirmTime (" + confirmTime + ") is not a known time string.");
                  }
                  listRegistrationToInsert.setConfirmationTime(confirmTimeAsObject);
                }
                return listRegistrationProvider.insertRegistration(listRegistrationToInsert)
                  .compose(listRegistrationInserted -> this.closeExecution(ListImportJobRowStatus.COMPLETED, null));
              });
          });

      });
  }

  private Future<ListImportJobRow> closeExecution(ListImportJobRowStatus status, String message) {
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

  public net.bytle.tower.eraldy.model.openapi.ListImportJobRowStatus toListJobRowStatus() {
    net.bytle.tower.eraldy.model.openapi.ListImportJobRowStatus jobRowStatus = new net.bytle.tower.eraldy.model.openapi.ListImportJobRowStatus();
    jobRowStatus.setEmailAddress(this.email);
    jobRowStatus.setUserGuid(this.userGuid);
    jobRowStatus.setStatusCode(this.statusCode);
    jobRowStatus.setRowId(this.rowId);
    jobRowStatus.setUserAdded(this.userAdded);
    jobRowStatus.setUserCreated(this.userCreated);
    jobRowStatus.setUserUpdated(this.userUpdated);
    String statusMessage = this.statusMessage;
    if (statusCode == EmailAddressValidationStatus.FATAL_ERROR.getStatusCode()) {
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
