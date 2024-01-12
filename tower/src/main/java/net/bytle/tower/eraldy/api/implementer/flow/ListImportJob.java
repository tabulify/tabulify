package net.bytle.tower.eraldy.api.implementer.flow;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import net.bytle.fs.Fs;
import net.bytle.tower.eraldy.model.openapi.ListImportJobRowStatus;
import net.bytle.tower.eraldy.model.openapi.ListImportJobStatus;
import net.bytle.tower.eraldy.model.openapi.ListItem;
import net.bytle.type.time.Timestamp;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static net.bytle.vertx.resilience.EmailAddressValidationStatus.FATAL_ERROR;

public class ListImportJob {

  static final Logger LOGGER = LogManager.getLogger(ListImportJob.class);
  private static final int RUNNING_STATUS_CODE = -1;
  private static final int FAILURE_STATUS_CODE = 1;
  private static final int SUCCESS_STATUS_CODE = 0;
  private static final int ABOVE_IMPORT_QUOTA = 2;
  private static final int BAD_FILE_STATUS = 3;
  private static final int TO_PROCESS_STATUS_CODE = -2;


  private final ListImportFlow listImportFlow;
  private final ListItem list;
  private final Path uploadedCsvFile;
  private final String jobId;
  private final Path originalFileName;
  private final ListImportJobStatus listImportJobStatus;

  private final Integer maxRowCountToProcess;
  private final ListImportJobAction action;
  /**
   * The execution status code
   * We use a runtime variable because
   * the final status code must be set at the end of the processing.
   * (ie the client can query on the status to see if the job is really completed)
   */
  private Integer executionStatusCode = TO_PROCESS_STATUS_CODE;
  private final boolean updateExistingUser;

  public ListImportJob(Builder builder) {
    this.listImportFlow = builder.listImportFlow;
    this.list = builder.list;
    this.uploadedCsvFile = Paths.get(builder.fileUpload.uploadedFileName());
    Timestamp creationTime = Timestamp.createFromNow();
    this.originalFileName = Path.of(builder.fileUpload.fileName());
    // time + list make the job unique
    this.jobId = creationTime.toFileSystemString();
    // Init all field
    // to avoid dealing with empty value when returning the data object
    this.listImportJobStatus = new ListImportJobStatus();
    listImportJobStatus.setJobId(this.getIdentifier());
    listImportJobStatus.setStatusCode(TO_PROCESS_STATUS_CODE);
    listImportJobStatus.setUploadedFileName(builder.fileUpload.fileName());
    listImportJobStatus.setCountTotal(0);
    listImportJobStatus.setCountComplete(0);
    listImportJobStatus.setCountSuccess(0);
    listImportJobStatus.setCreationTime(creationTime.toLocalDateTime());
    this.maxRowCountToProcess = Objects.requireNonNullElse(builder.maxRowCountToProcess, 5000);
    this.action = Objects.requireNonNull(builder.action);
    this.updateExistingUser = Objects.requireNonNullElse(builder.updateExistingUser, false);
  }

  public static ListImportJob.Builder builder(ListImportFlow listImportFlow, ListItem list, FileUpload filoeUpload, ListImportJobAction action) {
    return new ListImportJob.Builder(listImportFlow, list, filoeUpload, action);
  }

  public boolean getUpdateExistingUser() {
    return this.updateExistingUser;
  }

  public ListImportJobAction getAction() {
    return this.action;
  }

  public static class Builder {
    private final FileUpload fileUpload;
    private final ListImportFlow listImportFlow;
    private final ListItem list;
    private Integer maxRowCountToProcess;
    private final ListImportJobAction action;
    private Boolean updateExistingUser;

    Builder(ListImportFlow listImportFlow, ListItem list, FileUpload fileUpload, ListImportJobAction action) {
      this.listImportFlow = listImportFlow;
      this.list = list;
      this.fileUpload = fileUpload;
      this.action = action;
    }

    public Builder setMaxRowCountToProcess(Integer maxRowCountToProcess) {
      this.maxRowCountToProcess = maxRowCountToProcess;
      return this;
    }

    public ListImportJob build() {
      return new ListImportJob(this);
    }

    public Builder setUpdateExistingUser(Boolean updateExistingUser) {
      this.updateExistingUser = updateExistingUser;
      return this;
    }
  }


  Path getFileNameWithExtension() {
    return this.originalFileName;
  }

  /**
   * @return execute incrementally the job
   */
  public Future<ListImportJob> executeSequentially() {

    synchronized (this) {
      if (this.isComplete()) {
        return Future.succeededFuture(this);
      }
      executionStatusCode = RUNNING_STATUS_CODE;
    }
    listImportJobStatus.setStartTime(LocalDateTime.now());
    listImportJobStatus.setStatusCode(executionStatusCode);

    return this.buildFutureListToExecute()
      .compose(futuresToExecute -> this.executeSequentiallyWithRetry(futuresToExecute, new ArrayList<>(futuresToExecute.size()), 1));


  }

  private Future<ListImportJob> executeSequentiallyWithRetry(List<Future<ListImportJobRow>> listFutureToExecute, List<ListImportJobRow> resultImportJobRows, int iterationCount) {


    return this.listImportFlow.getApp().getApexDomain().getHttpServer().getServer().getFutureSchedulers()
      .createSequentialScheduler(ListImportJobRow.class)
      .all(listFutureToExecute, this.listImportJobStatus)
      .compose(composite -> {
        List<ListImportJobRow> listImportJobRows = composite.getResults();
        if (composite.hasFailed()) {
          Throwable failure = composite.getFailure();
          return this.closeJobWithFatalError(failure, "A fatal error has happened on the row " + (composite.getFailureIndex() + 1), listImportJobRows);
        }
        List<Future<ListImportJobRow>> toRetryFutures = new ArrayList<>();
        for (ListImportJobRow listImportJobRow : listImportJobRows) {
          if (
            iterationCount < this.listImportFlow.getRowValidationRetryCount()
              && listImportJobRow.getStatus().equals(FATAL_ERROR.getStatusCode())
          ) {
            toRetryFutures.add(listImportJobRow.getExecutableFuture());
          }
          /**
           * We build the list first
           * One retry, the elements will be replaced
           */
          int rowId = listImportJobRow.getRowId();
          if (rowId >= 0 && rowId < resultImportJobRows.size()) {
            resultImportJobRows.set(rowId, listImportJobRow);
          } else {
            resultImportJobRows.add(rowId, listImportJobRow);
          }
        }
        if (!toRetryFutures.isEmpty()) {
          int nextIterationCount = iterationCount + 1;
          return executeSequentiallyWithRetry(toRetryFutures, resultImportJobRows, nextIterationCount);
        }
        return this.closeJob(resultImportJobRows);
      });
  }


  /**
   * Build the list of future to process
   */
  private Future<List<Future<ListImportJobRow>>> buildFutureListToExecute() {
    /**
     * Jackson Csv Processing
     * Csv import Doc for Jackson is at:
     * https://github.com/FasterXML/jackson-dataformats-text/tree/master/csv
     * below the howto
     */
    CsvMapper csvMapper = new CsvMapper(); // the mapper is the entry point
    CsvSchema schema = CsvSchema.emptySchema(); // the jackson schema (we detect the columns)

    /**
     * Mapping header
     */
    Map<ListImportFlow.IMPORT_FIELD, Integer> headerMapping = new HashMap<>();

    /**
     * The Jackson Iterator
     */
    try (MappingIterator<String[]> it = csvMapper
      .readerFor(String[].class)
      // This setting will transform the json as array to get a String[]
      .with(CsvParser.Feature.WRAP_AS_ARRAY)
      .with(schema)
      .readValues(uploadedCsvFile.toFile())) {

      int counter = -1;
      List<Future<ListImportJobRow>> listFutureJobRowStatus = new ArrayList<>();
      while (it.hasNextValue()) {
        counter++;
        String[] row = it.nextValue();
        if (counter == 0) {
          // Header
          // Mailchimp doc
          // https://mailchimp.com/help/view-export-contacts/
          for (int i = 0; i < row.length; i++) {
            String normalizedHeader = row[i]
              .trim()
              .replace(" ", "")
              .replace("_", "")
              .replace("-", "")
              .replace(".", "")
              .toLowerCase();
            switch (normalizedHeader) {
              case "email":
              case "emailaddress":
                headerMapping.put(ListImportFlow.IMPORT_FIELD.EMAIL_ADDRESS, i);
                continue;
              case "firstname":
              case "givenname":
                headerMapping.put(ListImportFlow.IMPORT_FIELD.GIVEN_NAME, i);
                continue;
              case "lastname":
              case "familyname":
                headerMapping.put(ListImportFlow.IMPORT_FIELD.FAMILY_NAME, i);
                break;
              case "optinuri":
                headerMapping.put(ListImportFlow.IMPORT_FIELD.OPT_IN_ORIGIN, i);
                break;
              case "optintime":
                headerMapping.put(ListImportFlow.IMPORT_FIELD.OPT_IN_TIME, i);
                break;
              case "optinip":
                headerMapping.put(ListImportFlow.IMPORT_FIELD.OPT_IN_IP, i);
                break;
              case "confirmtime":
              case "confirmationtime":
                headerMapping.put(ListImportFlow.IMPORT_FIELD.CONFIRM_TIME, i);
                break;
              case "confirmip":
              case "confirmationip":
                headerMapping.put(ListImportFlow.IMPORT_FIELD.CONFIRM_IP, i);
                break;
              case "location":
              case "region":
                headerMapping.put(ListImportFlow.IMPORT_FIELD.LOCATION, i);
                break;
              default:
                break;
            }
          }
          // end for loop parsing header columns
          Integer emailIndex = headerMapping.get(ListImportFlow.IMPORT_FIELD.EMAIL_ADDRESS);
          if (emailIndex == null) {

            String statusMessage = "An email address header could not be found in the file (" + this.getFileNameWithExtension() + "). Headers found: " + Arrays.toString(row);
            listImportJobStatus.setStatusMessage(statusMessage);
            this.executionStatusCode = BAD_FILE_STATUS;
            return this.closeJob(new ArrayList<>())
              .compose(v -> Future.failedFuture(statusMessage));

          }
          // continue the loop and process the second record
          continue;
        }

        // header is at 0, 1 is the first row
        listImportJobStatus.setCountTotal(counter);
        int rowId = counter - 1;
        ListImportJobRow listImportJobRow = new ListImportJobRow(this, rowId);
        Integer emailIndex = headerMapping.get(ListImportFlow.IMPORT_FIELD.EMAIL_ADDRESS);
        String email = row[emailIndex];
        listImportJobRow.setEmail(email);
        Integer familyNameIndex = headerMapping.get(ListImportFlow.IMPORT_FIELD.FAMILY_NAME);
        if (familyNameIndex != null) {
          String familyName = row[familyNameIndex];
          listImportJobRow.setFamilyName(familyName);
        }
        Integer givenNameIndex = headerMapping.get(ListImportFlow.IMPORT_FIELD.GIVEN_NAME);
        if (givenNameIndex != null) {
          String givenName = row[givenNameIndex];
          listImportJobRow.setGivenName(givenName);
        }
        Integer optInOriginIndex = headerMapping.get(ListImportFlow.IMPORT_FIELD.OPT_IN_ORIGIN);
        if (optInOriginIndex != null) {
          String optInOrigin = row[optInOriginIndex];
          listImportJobRow.setOptInOrigin(optInOrigin);
        }
        Integer optInIpIndex = headerMapping.get(ListImportFlow.IMPORT_FIELD.OPT_IN_IP);
        if (optInIpIndex != null) {
          String optInIp = row[optInIpIndex];
          listImportJobRow.setOptInIp(optInIp);
        }
        Integer optInTimeIndex = headerMapping.get(ListImportFlow.IMPORT_FIELD.OPT_IN_TIME);
        if (optInTimeIndex != null) {
          String optInTime = row[optInTimeIndex];
          listImportJobRow.setOptInTime(optInTime);
        }
        Integer confirmIpIndex = headerMapping.get(ListImportFlow.IMPORT_FIELD.CONFIRM_IP);
        if (confirmIpIndex != null) {
          String confirmIp = row[confirmIpIndex];
          listImportJobRow.setConfirmIp(confirmIp);
        }
        Integer confirmTimeIndex = headerMapping.get(ListImportFlow.IMPORT_FIELD.CONFIRM_TIME);
        if (confirmTimeIndex != null) {
          String confirmTime = row[confirmTimeIndex];
          listImportJobRow.setConfirmTime(confirmTime);
        }
        Integer locationIndex = headerMapping.get(ListImportFlow.IMPORT_FIELD.LOCATION);
        if (locationIndex != null) {
          String location = row[locationIndex];
          listImportJobRow.setLocation(location);
        }
        Future<ListImportJobRow> futureListImportRow = listImportJobRow.getExecutableFuture();
        listFutureJobRowStatus.add(futureListImportRow);
        int maxRowsProcessedByImport = this.getMaxRowCountToProcess();
        if (counter >= maxRowsProcessedByImport) {
          this.executionStatusCode = ABOVE_IMPORT_QUOTA;
          this.listImportJobStatus.setStatusMessage("The import quota is set to (" + maxRowsProcessedByImport + ") and was reached");
          break;
        }
      }
      return Future.succeededFuture(listFutureJobRowStatus);
    } catch (IOException e) {
      String message = "List import couldn't read the csv file (" + this.getFileNameWithExtension() + ").";
      return this.closeJobWithFatalError(e, message, new ArrayList<>())
        .compose(v -> Future.failedFuture(message));
    }
  }


  private int getMaxRowCountToProcess() {
    return this.maxRowCountToProcess;
  }

  public Future<ListImportJob> closeJobWithFatalError(Throwable e, String message, List<ListImportJobRow> results) {
    String exceptionSuffix = "Error:" + e.getMessage() + " (" + e.getClass().getSimpleName() + ")";
    if (message == null) {
      message = exceptionSuffix;
    } else {
      message = message + " " + exceptionSuffix;
    }
    listImportJobStatus.setStatusMessage(message);
    this.executionStatusCode = FAILURE_STATUS_CODE;
    LOGGER.error("A fatal error has occurred with the list import job (" + list.getGuid() + "," + listImportJobStatus.getJobId() + ")", e);
    return this.closeJob(results);
  }


  private Future<ListImportJob> closeJob(List<ListImportJobRow> listJobRow) {


    /**
     * Write the row status
     */
    Path resultFile = this.listImportFlow.getRowStatusFileJobByIdentifier(this.list.getGuid(), this.getIdentifier());
    List<ListImportJobRowStatus> listJobRowStatus = listJobRow.stream()
      .map(ListImportJobRow::toListJobRowStatus)
      .collect(Collectors.toList());
    String resultString = new JsonArray(listJobRowStatus).toString();
    Fs.write(resultFile, resultString);


    /**
     * Move the csv file
     */
    Path csvFile = this.listImportFlow.getListDirectory(this.list.getGuid()).resolve(this.getIdentifier() + ".csv");
    Fs.move(this.uploadedCsvFile, csvFile);

    /**
     * Write the job report
     */
    listImportJobStatus.setEndTime(LocalDateTime.now());
    /**
     * If the job is in a running state (<0), the status is success
     * The status code is written at the end because the client may query it to see if the job has terminated
     */
    if (this.executionStatusCode <= 0) {
      this.executionStatusCode = SUCCESS_STATUS_CODE;
    }
    listImportJobStatus.setStatusCode(this.executionStatusCode);
    Path statusPath = this.listImportFlow.getStatusFileJobByIdentifier(this.list.getGuid(), this.getIdentifier());
    Fs.write(statusPath, JsonObject.mapFrom(listImportJobStatus).toString());

    /**
     * The execution pointer
     */
    return Future.succeededFuture();

  }

  public String getIdentifier() {
    return this.jobId;
  }

  public ListImportJobStatus getStatus() {
    return this.listImportJobStatus;
  }

  public ListItem getList() {
    return this.list;
  }


  public boolean isComplete() {
    return this.getStatus().getStatusCode() >= SUCCESS_STATUS_CODE;
  }

  public boolean isRunning() {
    return this.getStatus().getStatusCode() == RUNNING_STATUS_CODE;
  }

  public ListImportFlow getListImportFlow() {
    return this.listImportFlow;
  }

  public boolean getFailEarly() {
    return false;
  }
}
