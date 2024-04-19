package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import net.bytle.dns.DnsException;
import net.bytle.dns.DnsIp;
import net.bytle.exception.CastException;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;
import net.bytle.tower.eraldy.graphql.pojo.input.ListUserInputProps;
import net.bytle.tower.eraldy.model.openapi.ListUserSource;
import net.bytle.tower.eraldy.model.openapi.ListUserStatus;
import net.bytle.tower.eraldy.module.list.db.ListUserProvider;
import net.bytle.tower.eraldy.module.user.db.UserProvider;
import net.bytle.tower.eraldy.module.user.inputs.UserInputProps;
import net.bytle.type.EmailAddress;
import net.bytle.type.time.TimeZoneCast;
import net.bytle.type.time.TimeZoneUtil;
import net.bytle.type.time.Timestamp;
import net.bytle.vertx.flow.FlowType;
import net.bytle.vertx.resilience.EmailAddressValidationStatus;
import net.bytle.vertx.resilience.ValidationTestResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.LocalDateTime;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * A unit of execution for a row
 * that can be re-executed if there is a fatal error (timeout, servfail DNS, ...)
 */
public class ListImportJobRow implements Handler<Promise<ListImportJobRow>> {

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
  private String timeZoneString;
  private String userGuid;
  private ListImportListUserStatus listUserStatus = ListImportListUserStatus.NOTHING;
  private ListImportUserStatus userStatus = ListImportUserStatus.NOTHING;
  private String listUserGuid;

  public ListImportJobRow(ListImportJob listImportJob, int rowId) {
    this.rowId = rowId;
    this.listImportJob = listImportJob;
  }

  public void setEmail(String email) {
    this.email = email;
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
            case GREY_BAN:
              listImportJobStatus = ListImportJobRowStatus.GREY_BAN;
              break;
            case EMAIL_ADDRESS_INVALID:
            case NOT_AN_APEX_DOMAIN:
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
        EmailAddress emailInternetAddress;
        try {
          emailInternetAddress = emailAddressValidityReport.getEmailAddress();
        } catch (NullValueException e) {
          return Future.failedFuture(new InternalException("Email address was null but the email was validated. It should not happen."));
        }
        return this.listImportJob.getList()
          .compose(list -> userProvider
            .getUserByEmail(emailInternetAddress, list.getRealm())
            .compose(userFromRegistry -> {
              if (userFromRegistry != null) {
                if (this.listImportJob.getUserAction() == ListImportUserAction.UPDATE) {
                  UserInputProps userInputProps = new UserInputProps();
                  boolean userShouldUpdate = false;
                  if (!this.givenName.isBlank() && !userFromRegistry.getGivenName().equals(this.givenName)) {
                    userInputProps.setGivenName(this.givenName);
                    userShouldUpdate = true;
                  }
                  if (!this.familyName.isBlank() && !userFromRegistry.getFamilyName().equals(this.familyName)) {
                    userInputProps.setFamilyName(this.familyName);
                    userShouldUpdate = true;
                  }
                  if (userShouldUpdate) {
                    this.userStatus = ListImportUserStatus.UPDATED;
                    return userProvider.updateUser(userFromRegistry,userInputProps);
                  }
                }
                return Future.succeededFuture(userFromRegistry);
              } else {
                UserInputProps userInputProps = new UserInputProps();
                userInputProps.setEmailAddress(emailInternetAddress);
                userInputProps.setGivenName(this.givenName);
                userInputProps.setFamilyName(this.familyName);
                if (!timeZoneString.isEmpty()) {
                  try {
                    TimeZone timeZone = TimeZoneUtil.getTimeZoneWithValidation(timeZoneString);
                    userInputProps.setTimeZone(timeZone);
                  } catch (TimeZoneCast e) {
                    return Future.failedFuture(new CastException("The timezone (" + timeZoneString + ") is not a valid time zone. Skipped.", e));
                  }
                }
                this.userStatus = ListImportUserStatus.CREATED;
                return userProvider.insertUserAndTrackEvent(list.getRealm(), userInputProps, FlowType.LIST_IMPORT);
              }
            })
            .compose(
              user -> {
                this.userGuid = user.getGuid();
                ListUserProvider listUserProvider = this.listImportJob.getListImportFlow().getApp().getListUserProvider();
                return listUserProvider.
                  getListUserByListAndUser(list, user)
                  .compose(listUser -> {
                    if (listUser != null) {
                      this.listUserStatus = ListImportListUserStatus.NOTHING;
                      this.listUserGuid = listUser.getGuid();
                      return this.closeExecution(ListImportJobRowStatus.COMPLETED, null);
                    }
                    /**
                     * Action OUT not yet implemented
                     */
                    if (this.listImportJob.getListUserAction() != ListImportListUserAction.IN) {
                      return this.closeExecution(ListImportJobRowStatus.COMPLETED, null);
                    }
                    /**
                     * Insert
                     */
                    ListUserInputProps listUserInsertionProps = new ListUserInputProps();
                    listUserInsertionProps.setInListUserSource(ListUserSource.IMPORT);
                    listUserInsertionProps.setStatus(ListUserStatus.OK);
                    if (this.optInOrigin == null) {
                      listUserInsertionProps.setInOptInOrigin(ListUserSource.IMPORT.toString());
                    } else {
                      listUserInsertionProps.setInOptInOrigin(this.optInOrigin);
                    }
                    if (optInIp != null) {
                      DnsIp optInIpAsDnsIp;
                      try {
                        optInIpAsDnsIp = DnsIp.createFromString(optInIp);
                      } catch (DnsException e) {
                        return this.closeExecution(ListImportJobRowStatus.DATA_INVALID, "The optInIp (" + optInIp + ") is not a valid ipv4 or ipv6.");
                      }
                      listUserInsertionProps.setInOptInIp(optInIpAsDnsIp.getAddress());
                    }
                    if (optInTime != null) {
                      LocalDateTime optInTimeAsObject;
                      try {
                        optInTimeAsObject = Timestamp.createFromString(optInTime).toLocalDateTime();
                      } catch (CastException e) {
                        return this.closeExecution(ListImportJobRowStatus.DATA_INVALID, "The optInTime (" + optInTime + ") is not a known time string.");
                      }
                      listUserInsertionProps.setInOptInTime(optInTimeAsObject);
                    }
                    if (confirmIp != null) {
                      DnsIp confirmIpAsDnsIp;
                      try {
                        confirmIpAsDnsIp = DnsIp.createFromString(confirmIp);
                      } catch (DnsException e) {
                        return this.closeExecution(ListImportJobRowStatus.DATA_INVALID, "The confirmIp (" + confirmIp + ") is not a valid ipv4 or ipv6.");
                      }
                      listUserInsertionProps.setInOptInConfirmationIp(confirmIpAsDnsIp.getAddress());
                    }
                    if (confirmTime != null) {
                      LocalDateTime confirmTimeAsObject;
                      try {
                        confirmTimeAsObject = Timestamp.createFromString(confirmTime).toLocalDateTime();
                      } catch (CastException e) {
                        return this.closeExecution(ListImportJobRowStatus.DATA_INVALID, "The confirmTime (" + confirmTime + ") is not a known time string.");
                      }
                      listUserInsertionProps.setInOptInConfirmationTime(confirmTimeAsObject);
                    }
                    return listUserProvider.insertListUser(user, list, listUserInsertionProps)
                      .compose(listRegistrationInserted -> {
                        this.listUserStatus = ListImportListUserStatus.ADDED;
                        this.listUserGuid = listRegistrationInserted.getGuid();
                        return this.closeExecution(ListImportJobRowStatus.COMPLETED, null);
                      });
                  });
              },
              err -> {
                if (err instanceof CastException) {
                  return this.closeExecution(ListImportJobRowStatus.DATA_INVALID, err.getMessage());
                }
                return Future.failedFuture(err);
              }
            )
          );
      });
  }

  private Future<ListImportJobRow> closeExecution(ListImportJobRowStatus status, String message) {
    this.buildStatusMessage(message);
    this.setStatusCode(status.getStatusCode());
    return Future.succeededFuture(this);
  }

  /**
   * @param message - the message to add to the status message
   */
  private void buildStatusMessage(String message) {
    if (this.statusMessage != null) {
      this.statusMessage += message;
      return;
    }
    this.statusMessage = message;
  }

  private void setStatusCode(int statusCode) {
    this.statusCode = statusCode;
  }

  public net.bytle.tower.eraldy.model.openapi.ListImportJobRowStatus toListJobRowStatus() {
    net.bytle.tower.eraldy.model.openapi.ListImportJobRowStatus jobRowStatus = new net.bytle.tower.eraldy.model.openapi.ListImportJobRowStatus();
    jobRowStatus.setEmailAddress(this.email);
    jobRowStatus.setUserGuid(this.userGuid);
    jobRowStatus.setListUserGuid(this.listUserGuid);
    jobRowStatus.setStatusCode(this.statusCode);
    jobRowStatus.setRowId(this.rowId);
    jobRowStatus.setUserStatus(this.userStatus.getCode());
    jobRowStatus.setListUserStatus(this.listUserStatus.getCode());

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

  public void setTimeZone(String timeZone) {
    this.timeZoneString = timeZone;
  }

  @Override
  public void handle(Promise<ListImportJobRow> event) {

    this.getExecutableFutureWithoutErrorHandling()
      .onFailure(err -> {
        // database timeout for instance
        this.setFatalError(err);
        int executionFatalErrorCounter = this.listImportJob.incrementRowFatalErrorCounter();
        if (executionFatalErrorCounter <= 3) {
          /**
           * We log only 3 because we don't want 10 thousand error
           * if the same error repeat on each row of a 10 thousand file
           */
          LOGGER.error("A fatal error has occurred on the row (" + this.rowId + ", " + this.email + ") with the list import job (" + listImportJob.getStatus().getListGuid() + "," + listImportJob.getStatus().getJobId() + ")", err);
        }
        event.complete(this);
      })
      .onSuccess(listImportJobRow -> event.complete(this));

  }

  /**
   * Set a fatal error caught during the execution or caught by the executor
   * @param err - the fatal error
   */
  public void setFatalError(Throwable err) {
    this.closeExecution(ListImportJobRowStatus.FATAL_ERROR, err.getMessage() + " (" + err.getClass().getSimpleName() + ")");
  }

}
