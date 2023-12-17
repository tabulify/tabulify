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

public class ListImportJob {

  static final Logger LOGGER = LogManager.getLogger(ListImportJob.class);
  private static final Integer RUNNING_STATUS_CODE = -1;
  private static final Integer FAILURE_STATUS_CODE = 1;
  private static final Integer ABOVE_IMPORT_QUOTA = 2;
  private static final Integer TO_PROCESS_STATUS_CODE = -2;

  private final ListImportFlow listImportFlow;
  private final ListItem list;
  private final Path uploadedCsvFile;
  private final String jobId;
  private final Path originalFileName;
  private final ListImportJobStatus listImportJobStatus;
  private final List<Future<ListImportJobRowStatus>> futureListImportRows = new ArrayList<>();

  public ListImportJob(ListImportFlow listImportFlow, ListItem list, FileUpload fileUpload) {
    this.listImportFlow = listImportFlow;
    this.list = list;
    this.uploadedCsvFile = Paths.get(fileUpload.uploadedFileName());
    Timestamp creationTime = Timestamp.createFromNow();
    this.originalFileName = Path.of(fileUpload.fileName());
    // time + the random uploaded file name
    this.jobId = creationTime.toFileSystemString() + "-" + this.uploadedCsvFile.getFileName().toString();

    this.listImportJobStatus = new ListImportJobStatus();
    listImportJobStatus.setJobId(this.getIdentifier());
    listImportJobStatus.setStatusCode(TO_PROCESS_STATUS_CODE);
    listImportJobStatus.setUploadedFileName(fileUpload.fileName());
    listImportJobStatus.setCountTotal(0);
    listImportJobStatus.setCountFailure(0);
    listImportJobStatus.setCreationTime(creationTime.toLocalDateTime());

  }


  Path getFileNameWithExtension() {
    return this.originalFileName;
  }

  public void execute() {

    int statusCode = listImportJobStatus.getStatusCode();
    if (statusCode != TO_PROCESS_STATUS_CODE) {
      LOGGER.warn("The job (" + this + ") was already processed. It has the status code (" + statusCode + ")");
      return;
    }


    listImportJobStatus.setStatusCode(RUNNING_STATUS_CODE);
    listImportJobStatus.setStartTime(LocalDateTime.now());

    /**
     * Csv import Doc for Jackson is at:
     * https://github.com/FasterXML/jackson-dataformats-text/tree/master/csv
     * below the howto
     */
    CsvMapper csvMapper = new CsvMapper();
    CsvSchema schema = CsvSchema.emptySchema(); // we detect the columns

    /**
     * Mappng hea
     */
    Map<ListImportFlow.IMPORT_FIELD, Integer> headerMapping = new HashMap<>();

    try (MappingIterator<String[]> it = csvMapper
      .readerFor(String[].class)
      // This setting will transform the json as array
      .with(CsvParser.Feature.WRAP_AS_ARRAY)
      .with(schema)
      .readValues(uploadedCsvFile.toFile())) {


      int counter = -1;
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
            this.closeJobWithFailure("An email address header could not be found in the file (" + this.getFileNameWithExtension() + "). Headers found: " + Arrays.toString(row));
            return;
          }
          // second record
          continue;
        }

        // header is at 0, 1 is the first row
        listImportJobStatus.setCountTotal(counter);
        Future<ListImportJobRowStatus> futureListImportRow = this.listImportFlow.getEmailAddressValidator()
          .validate(row[headerMapping.get(ListImportFlow.IMPORT_FIELD.EMAIL_ADDRESS)], true)
          .compose(emailAddressValidityReport -> {
            ListImportJobRowStatus listImportRow = new ListImportJobRowStatus();
            listImportRow.setEmailAddress(emailAddressValidityReport.getEmailAddress());
            listImportRow.setStatusCode(emailAddressValidityReport.isValid()?0:1);
            listImportRow.setStatusMessage(String.join(", ", emailAddressValidityReport.getErrors().values()));
            // row[headerMapping.get(ListImportFlow.IMPORT_FIELD.FAMILY_NAME)];
            // row[headerMapping.get(ListImportFlow.IMPORT_FIELD.GIVEN_NAME)];
            return Future.succeededFuture(listImportRow);
          });
        futureListImportRows.add(futureListImportRow);
        int maxRowsProcessedByImport = this.listImportFlow.getMaxRowsProcessedByImport();
        if (counter >= maxRowsProcessedByImport) {
          this.listImportJobStatus.setStatusCode(ABOVE_IMPORT_QUOTA);
          this.listImportJobStatus.setStatusMessage("The import quota is set to ("+maxRowsProcessedByImport+") and was reached");
          break;
        }
      }
      this.closeJob();
    } catch (IOException e) {
      this.closeJobWithFailure("List import couldn't read the csv file (" + this.getFileNameWithExtension() + "). Error: " + e.getMessage());
    }
  }

  private void closeJobWithFailure(String message) {
    listImportJobStatus.setStatusCode(FAILURE_STATUS_CODE);
    listImportJobStatus.setStatusMessage(message);
    this.closeJob();
  }

  private void closeJob() {

    Future.all(futureListImportRows)
      .onComplete(composite -> {

        /**
         * Write the row status
         */
        listImportJobStatus.setEndTime(LocalDateTime.now());
        Path resultFile = this.listImportFlow.getRowStatusFileJobByIdentifier(this.list.getGuid(), this.getIdentifier());
        String resultString = JsonArray.of(composite.result().list()).toString();
        Fs.write(resultFile, resultString);

        /**
         * Write the job status
         */
        Path statusPath = this.listImportFlow.getStatusFileJobByIdentifier(this.list.getGuid(), this.getIdentifier());
        Fs.write(statusPath, JsonObject.mapFrom(listImportJobStatus).toString());

        /**
         * Move the csv file
         */
        Path csvFile = this.listImportFlow.getListDirectory(this.list.getGuid()).resolve(this.getIdentifier() + ".csv");
        Fs.move(this.uploadedCsvFile, csvFile);

      });

  }

  public String getIdentifier() {
    return this.jobId;
  }

}
