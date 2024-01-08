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
import net.bytle.vertx.TowerCompositeFuture;
import net.bytle.vertx.resilience.ValidationTestResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
  /**
   * The execution status code
   * We use a runtime variable because
   * the final status code must be set at the end of the processing.
   * (ie the client can query on the status to see if the job is really completed)
   */
  private Integer executionStatusCode = TO_PROCESS_STATUS_CODE;

  public ListImportJob(ListImportFlow listImportFlow, ListItem list, FileUpload fileUpload, Integer maxRowCountToProcess) {
    this.listImportFlow = listImportFlow;
    this.list = list;
    this.uploadedCsvFile = Paths.get(fileUpload.uploadedFileName());
    Timestamp creationTime = Timestamp.createFromNow();
    this.originalFileName = Path.of(fileUpload.fileName());
    // time + the random uploaded file name
    this.jobId = creationTime.toFileSystemString() + "-" + this.uploadedCsvFile.getFileName().toString();

    // Init all field
    // to avoid dealing with empty value when returning the data object
    this.listImportJobStatus = new ListImportJobStatus();
    listImportJobStatus.setJobId(this.getIdentifier());
    listImportJobStatus.setStatusCode(TO_PROCESS_STATUS_CODE);
    listImportJobStatus.setUploadedFileName(fileUpload.fileName());
    listImportJobStatus.setCountTotal(0);
    listImportJobStatus.setCountComplete(0);
    listImportJobStatus.setCountSuccess(0);
    listImportJobStatus.setCreationTime(creationTime.toLocalDateTime());
    this.maxRowCountToProcess = Objects.requireNonNullElse(maxRowCountToProcess, 10000);

  }


  Path getFileNameWithExtension() {
    return this.originalFileName;
  }

  /**
   * @return execute incrementally the job
   */
  public Future<ListImportJob> executeSequentially() {

    synchronized (this) {
      if(this.isComplete()){
        return Future.succeededFuture(this);
      }
      executionStatusCode = RUNNING_STATUS_CODE;
    }
    listImportJobStatus.setStartTime(LocalDateTime.now());
    listImportJobStatus.setStatusCode(executionStatusCode);

    return this.buildFutureListToExecute()
      .compose(v -> buildFutureListToExecute()
        .compose(listFuture -> TowerCompositeFuture.allSequentially(listFuture, this.listImportJobStatus)
          .compose(composite -> {
            if (composite.hasFailed()) {
              Throwable failure = composite.getFailure();
              return this.closeJobWithFatalError(failure, "A fatal error has happened on the row " + composite.getFailureIndex() + " (header row index is -1)", composite.getResults());
            }
            return this.closeJob(composite.getResults());
          })));


  }


  /**
   * Build the list of future to process
   */
  private Future<List<Future<ListImportJobRowStatus>>> buildFutureListToExecute() {
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
      List<Future<ListImportJobRowStatus>> listFutureJobRowStatus = new ArrayList<>();
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
              default:
                System.out.println(normalizedHeader);
            }
          }
          if (headerMapping.get(ListImportFlow.IMPORT_FIELD.EMAIL_ADDRESS) == null) {

            String statusMessage = "An email address header could not be found in the file (" + this.getFileNameWithExtension() + "). Headers found: " + Arrays.toString(row);
            listImportJobStatus.setStatusMessage(statusMessage);
            this.executionStatusCode = BAD_FILE_STATUS;
            return this.closeJob(new ArrayList<>())
              .compose(v -> Future.failedFuture(statusMessage));

          }
          // second record
          continue;
        }

        // header is at 0, 1 is the first row
        listImportJobStatus.setCountTotal(counter);
        // fail early make the report returns to the first error
        boolean failEarly = true;
        Future<ListImportJobRowStatus> futureListImportRow = getImportFuture(row, headerMapping, failEarly);
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
      String message = "List import couldn't read the csv file (" + this.getFileNameWithExtension() + "). Error: " + e.getMessage();
      return this.closeJobWithFatalError(e, message, new ArrayList<>())
        .compose(v -> Future.failedFuture(message));
    }
  }

  private Future<ListImportJobRowStatus> getImportFuture(String[] row, Map<ListImportFlow.IMPORT_FIELD, Integer> headerMapping, boolean failEarly) {
    String email = row[headerMapping.get(ListImportFlow.IMPORT_FIELD.EMAIL_ADDRESS)];
    return this.listImportFlow.getEmailAddressValidator()
      .validate(email, failEarly)
      .compose(emailAddressValidityReport -> {
        ListImportJobRowStatus listImportRow = new ListImportJobRowStatus();
        listImportRow.setEmailAddress(emailAddressValidityReport.getEmailAddress());
        listImportRow.setStatusCode(emailAddressValidityReport.getStatus().getStatusCode());
        listImportRow.setStatusMessage(emailAddressValidityReport.getErrors().stream().map(ValidationTestResult::getMessage).collect(Collectors.joining(", ")));
        // row[headerMapping.get(ListImportFlow.IMPORT_FIELD.FAMILY_NAME)];
        // row[headerMapping.get(ListImportFlow.IMPORT_FIELD.GIVEN_NAME)];
        return Future.succeededFuture(listImportRow);
      });
  }

  private int getMaxRowCountToProcess() {
    return this.maxRowCountToProcess;
  }

  private Future<ListImportJob> closeJobWithFatalError(Throwable e, String message, List<ListImportJobRowStatus> results) {
    if (message == null) {
      message = e.getMessage();
    }
    listImportJobStatus.setStatusMessage(message);
    this.executionStatusCode = FAILURE_STATUS_CODE;
    LOGGER.error("A fatal error has occurred with the list import job (" + list.getGuid() + "," + listImportJobStatus.getJobId() + ")", e);
    return this.closeJob(results);
  }


  private Future<ListImportJob> closeJob(List<ListImportJobRowStatus> listJobRowStatus) {


    /**
     * Write the row status
     */
    Path resultFile = this.listImportFlow.getRowStatusFileJobByIdentifier(this.list.getGuid(), this.getIdentifier());
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
}
