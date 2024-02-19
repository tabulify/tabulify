package net.bytle.tower.eraldy.api.implementer.flow;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.bytle.exception.CastException;
import net.bytle.fs.Fs;
import net.bytle.tower.eraldy.model.openapi.ListImportJobRowStatus;
import net.bytle.tower.eraldy.model.openapi.ListImportJobStatus;
import net.bytle.tower.eraldy.model.openapi.ListObject;
import net.bytle.vertx.DateTimeUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static net.bytle.tower.eraldy.api.implementer.flow.ListImportJobRowStatus.FATAL_ERROR;

public class ListImportJob {

  static final Logger LOGGER = LogManager.getLogger(ListImportJob.class);
  private static final int RUNNING_STATUS_CODE = -1;
  private static final int FAILURE_STATUS_CODE = 1;
  private static final int SUCCESS_STATUS_CODE = 0;
  private static final int ABOVE_IMPORT_QUOTA = 2;
  private static final int BAD_FILE_STATUS = 3;
  private static final int TO_PROCESS_STATUS_CODE = -2;


  private final ListImportFlow listImportFlow;

  private final Path sourceCsvFile;
  private final String jobId;

  private final ListImportJobStatus listImportJobStatus;

  private final Integer maxRowCountToProcess;
  private final ListImportListUserAction listUserAction;
  /**
   * The execution status code
   * We use a runtime variable because
   * the final status code must be set at the end of the processing.
   * (ie the client can query on the status to see if the job is really completed)
   */
  private Integer executionStatusCode = TO_PROCESS_STATUS_CODE;
  private final ListImportUserAction userAction;
  private final List<ListImportJobRow> resultImportJobRows = new ArrayList<>();

  /**
   * A counter of the number of errors on row
   * execution. We log only the first 5 to not be overwhelmed with error.
   */
  private int rowFatalErrorExecutionCounter = 0;
  private ListObject list;

  /**
   * The id in the store
   * (It's also the guid)
   */
  private String storeId;

  public ListImportJob(ListImportFlow listImportFlow, ListImportJobStatus listImportJobStatus, Path sourceCsvFile) {
    this.listImportFlow = listImportFlow;

    this.sourceCsvFile = sourceCsvFile;

    /**
     * All new job should be processed
     * (if they are created via the API or via the database at mount time)
     * Even if they were processing while the server was restarted
     * We consider them as new for now.
     */
    listImportJobStatus.setStatusCode(ListImportJob.TO_PROCESS_STATUS_CODE);
    listImportJobStatus.setCountTotal(0);
    listImportJobStatus.setCountSuccess(0);
    listImportJobStatus.setCountComplete(0);

    // Init all field
    // to avoid dealing with empty value when returning the data object
    this.listImportJobStatus = listImportJobStatus;
    this.jobId = listImportJobStatus.getJobId();
    this.storeId = this.listImportJobStatus.getListGuid() + "/lij-" + this.jobId;
    this.maxRowCountToProcess = listImportJobStatus.getMaxRowCountToProcess();

    // list user action
    Integer listUserActionCode = this.listImportJobStatus.getListUserActionCode();
    try {
      this.listUserAction = ListImportListUserAction.fromCode(listUserActionCode);
    } catch (CastException e) {
      throw new IllegalArgumentException("The list user action code is not valid (" + listUserActionCode + ")", e);
    }
    // user action
    Integer userActionCode = this.listImportJobStatus.getUserActionCode();
    try {
      this.userAction = ListImportUserAction.fromCode(userActionCode);
    } catch (CastException e) {
      throw new IllegalArgumentException("The user action code is not valid (" + userActionCode + ")", e);
    }

  }


  public ListImportUserAction getUserAction() {
    return this.userAction;
  }

  public ListImportListUserAction getListUserAction() {
    return this.listUserAction;
  }

  public int incrementRowFatalErrorCounter() {
    this.rowFatalErrorExecutionCounter++;
    return this.rowFatalErrorExecutionCounter;
  }

  public String getStoreId() {
    return this.storeId;
  }


  /**
   * @return execute incrementally the job
   */
  public Future<ListImportJob> executeSequentially(WorkerExecutor executor, int poolSize) {

    synchronized (this) {
      if (this.isComplete()) {
        return Future.succeededFuture(this);
      }
      executionStatusCode = RUNNING_STATUS_CODE;
    }
    listImportJobStatus.setStartTime(DateTimeUtil.getNowInUtc());
    listImportJobStatus.setStatusCode(executionStatusCode);

    List<ListImportJobRow> listImportJobRows;
    try {
      listImportJobRows = this.getListImportJobRows();
    } catch (CastException | IOException e) {
      return this.closeJobWithFatalError(e, "");
    }
    return this.executeSequentiallyWithRetry(listImportJobRows, 0, executor, poolSize);

  }

  /**
   *
   * @param importRows - the import rows to execute
   * @param iterationIndex - the number of times that this job has run (0 = first time, 1 = second time - first retry, ...)
   * @param workerExecutor - the executor
   * @param batchSize - the number of handlers to execute a one time
   */
  private Future<ListImportJob> executeSequentiallyWithRetry(List<ListImportJobRow> importRows,
                                                             int iterationIndex,
                                                             WorkerExecutor workerExecutor,
                                                             int batchSize) {

    List<Handler<Promise<ListImportJobRow>>> handlers = importRows.stream()
      .map(e -> (Handler<Promise<ListImportJobRow>>) e)
      .collect(Collectors.toList());

    return this.listImportFlow.getApp().getHttpServer().getServer().getFutureSchedulers()
      .createSequentialScheduler(ListImportJobRow.class)
      .setListener(this.listImportJobStatus)
      .setBatchSize(batchSize)
      .setExecutorContext(workerExecutor)
      .all(handlers)
      .compose(composite -> {

        if (composite.hasFailed()) {
          Throwable failure = composite.getFailureCause();
          return this.closeJobWithFatalError(failure, "A fatal error has happened on the import");
        }
        List<ListImportJobRow> toRetryJobRow = new ArrayList<>();
        for (int i = 0; i < composite.size(); i++) {
          ListImportJobRow resultListImportJobRow;
          if (composite.failed(i)) {
            /**
             * Fatal error not caught in the handler
             */
            resultListImportJobRow = importRows.get(i);
            Throwable throwable = composite.cause(i);
            resultListImportJobRow.setFatalError(throwable);

          } else {
            /**
             * Normal process where all error where caught
             * during the execution
             */
            resultListImportJobRow = composite.resultAt(i);
            if (
              iterationIndex < this.listImportFlow.getRowValidationFailureRetryCount()
                && resultListImportJobRow.getStatus().equals(FATAL_ERROR.getStatusCode())
            ) {
              toRetryJobRow.add(resultListImportJobRow);
            }
          }
          /**
           * We build the list first
           * On retry, the elements will be replaced
           */
          int rowId = resultListImportJobRow.getRowId();
          if (rowId >= 0 && rowId < resultImportJobRows.size()) {
            resultImportJobRows.set(rowId, resultListImportJobRow);
          } else {
            resultImportJobRows.add(rowId, resultListImportJobRow);
          }
        }
        if (!toRetryJobRow.isEmpty()) {
          int nextIterationIndex = iterationIndex + 1;
          return executeSequentiallyWithRetry(toRetryJobRow, nextIterationIndex, workerExecutor, batchSize);
        }
        return this.closeJob();
      });
  }


  /**
   * Build the list of future to process
   */
  private List<ListImportJobRow> getListImportJobRows() throws CastException, IOException {
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
    try (MappingIterator<String[]> iterator = csvMapper
      .readerFor(String[].class)
      // This setting will transform the json as array to get a String[]
      .with(CsvParser.Feature.WRAP_AS_ARRAY)
      .with(schema)
      .readValues(sourceCsvFile.toFile())) {

      int counter = -1;
      List<ListImportJobRow> listImportJobRows = new ArrayList<>();
      while (iterator.hasNextValue()) {
        counter++;
        String[] row = iterator.nextValue();
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
              case "timezone":
                headerMapping.put(ListImportFlow.IMPORT_FIELD.TIMEZONE, i);
                break;
              default:
                break;
            }
          }
          // end for loop parsing header columns
          Integer emailIndex = headerMapping.get(ListImportFlow.IMPORT_FIELD.EMAIL_ADDRESS);
          if (emailIndex == null) {

            String statusMessage = "An email address header could not be found in the file (" + this.listImportJobStatus.getUploadedFileName() + "). Headers found: " + Arrays.toString(row);
            listImportJobStatus.setStatusMessage(statusMessage);
            this.executionStatusCode = BAD_FILE_STATUS;
            throw new CastException(statusMessage);

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
        Integer timeZoneIndex = headerMapping.get(ListImportFlow.IMPORT_FIELD.TIMEZONE);
        if (timeZoneIndex != null) {
          String timeZoneString = row[timeZoneIndex];
          listImportJobRow.setTimeZone(timeZoneString);
        }
        listImportJobRows.add(listImportJobRow);
        int maxRowsProcessedByImport = this.getMaxRowCountToProcess();
        if (counter >= maxRowsProcessedByImport) {
          this.executionStatusCode = ABOVE_IMPORT_QUOTA;
          this.listImportJobStatus.setStatusMessage("The import quota is set to (" + maxRowsProcessedByImport + ") and was reached");
          break;
        }
      }
      return listImportJobRows;
    } catch (IOException e) {
      String message = "List import couldn't read the csv file (" + this.listImportJobStatus.getUploadedFileName() + ").";
      throw new IOException(message, e);
    }
  }


  private int getMaxRowCountToProcess() {
    return this.maxRowCountToProcess;
  }

  public Future<ListImportJob> closeJobWithFatalError(Throwable e, String message) {
    String exceptionSuffix = "Error:" + e.getMessage() + " (" + e.getClass().getSimpleName() + ")";
    if (message == null || message.isEmpty()) {
      message = exceptionSuffix;
    } else {
      message = message + " " + exceptionSuffix;
    }
    listImportJobStatus.setStatusMessage(message);
    this.executionStatusCode = FAILURE_STATUS_CODE;
    LOGGER.error("A fatal error has occurred with the list import job (" + this.listImportJobStatus.getListGuid() + "," + listImportJobStatus.getJobId() + ")", e);
    return this.closeJob();
  }


  private Future<ListImportJob> closeJob() {

    /**
     * Write the row status
     */
    Path resultFile = this.listImportFlow.getRowStatusFileJobByIdentifier(this.listImportJobStatus.getListGuid(), this.getJobId());
    List<ListImportJobRowStatus> listJobRowStatus = this.resultImportJobRows
      .stream()
      .map(ListImportJobRow::toListJobRowStatus)
      .collect(Collectors.toList());
    String resultString = new JsonArray(listJobRowStatus).toString();
    Fs.write(resultFile, resultString);

    /**
     * Write the job report
     */
    listImportJobStatus.setEndTime(DateTimeUtil.getNowInUtc());

    /**
     * If the job is in a running state (<0), the status is success
     * The status code is written at the end because the client may query it to see if the job has terminated
     */
    if (this.executionStatusCode <= 0) {
      this.executionStatusCode = SUCCESS_STATUS_CODE;
    }
    listImportJobStatus.setStatusCode(this.executionStatusCode);
    Path statusPath = this.listImportFlow.getStatusFileJobByIdentifier(this.listImportJobStatus.getListGuid(), this.getJobId());
    Fs.write(statusPath, JsonObject.mapFrom(listImportJobStatus).toString());

    /**
     * The execution pointer
     */
    return Future.succeededFuture();

  }

  public String getJobId() {
    return this.jobId;
  }

  public ListImportJobStatus getStatus() {
    return this.listImportJobStatus;
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

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ListImportJob that = (ListImportJob) o;
    return Objects.equals(this.listImportJobStatus.getListGuid(), that.listImportJobStatus.getListGuid()) && Objects.equals(jobId, that.jobId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.listImportJobStatus.getListGuid(), jobId);
  }

  @Override
  public String toString() {
    return this.getStoreId();
  }

  public Future<ListObject> getList() {
    if (this.list != null) {
      return Future.succeededFuture(this.list);
    }
    return this.listImportFlow.getApp()
      .getListProvider()
      .getListByGuidHashIdentifier(this.listImportJobStatus.getListGuid())
      .onFailure(err -> LOGGER.error("Error while getting the list with the guid (" + this.listImportJobStatus.getListGuid() + ") for the job (" + this + ")", err))
      .compose(list -> {
        this.list = list;
        return Future.succeededFuture(list);
      });
  }

  public void setStoreId(String storeId) {
    this.storeId = storeId;
  }
}
