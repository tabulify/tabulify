package net.bytle.tower.eraldy.api.implementer.flow;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.FileUpload;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.fs.Fs;
import net.bytle.tower.eraldy.model.openapi.ListItem;
import net.bytle.type.time.Timestamp;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ListImportJob {
  private final ListImportFlow listImportFlow;
  private final ListItem list;
  private final Path uploadedCsvFile;
  private final Timestamp creationTime;
  private final String jobId;
  private final Path originalFileName;

  public ListImportJob(ListImportFlow listImportFlow, ListItem list, FileUpload fileUpload) {
    this.listImportFlow = listImportFlow;
    this.list = list;
    this.uploadedCsvFile = Paths.get(fileUpload.uploadedFileName());
    this.creationTime = Timestamp.createFromNow();
    this.originalFileName = Path.of(fileUpload.fileName());
    // time + the random uploaded file name
    this.jobId = this.creationTime.toFileSystemString() + "-" + this.uploadedCsvFile.getFileName().toString();
  }


  Path getFileNameWithExtension() {
    return this.originalFileName;
  }

  public void execute() {

    /**
     * Doc is at:
     * https://github.com/FasterXML/jackson-dataformats-text/tree/master/csv
     * below the howto
     */

    CsvMapper csvMapper = new CsvMapper();
    CsvSchema schema = CsvSchema.emptySchema();

    int counter = -1;
    Map<ListImportFlow.IMPORT_FIELD, Integer> headerMapping = new HashMap<>();
    try (MappingIterator<String[]> it = csvMapper
      .readerFor(String[].class)
      // This setting will transform the json as array
      .with(CsvParser.Feature.WRAP_AS_ARRAY)
      .with(schema)
      .readValues(uploadedCsvFile.toFile())) {

      List<Future<ListImportFlow.ListImportRow>> futureListImportRows = new ArrayList<>();
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
            throw new IllegalStructure("An email address header could not be found in the file (" + this.getFileNameWithExtension() + "). Headers found: " + Arrays.toString(row));

          }
          // second record
          continue;
        }
        Future<ListImportFlow.ListImportRow> futureListImportRow = this.listImportFlow.getEmailAddressValidator()
          .validate(row[headerMapping.get(ListImportFlow.IMPORT_FIELD.EMAIL_ADDRESS)], true)
          .compose(emailAddressValidityReport -> {
            ListImportFlow.ListImportRow listImportRow = new ListImportFlow.ListImportRow();
            listImportRow.emailAddress = emailAddressValidityReport.getEmailAddress();
            listImportRow.valid = emailAddressValidityReport.isValid();
            listImportRow.validityMessage = String.join(", ", emailAddressValidityReport.getErrors().values());
            listImportRow.familyName = row[headerMapping.get(ListImportFlow.IMPORT_FIELD.FAMILY_NAME)];
            listImportRow.givenName = row[headerMapping.get(ListImportFlow.IMPORT_FIELD.GIVEN_NAME)];
            return Future.succeededFuture(listImportRow);
          });
        futureListImportRows.add(futureListImportRow);
        if (counter >= this.listImportFlow.getMaxRowsProcessedByImport()) {
          break;
        }
      }
      Future.all(futureListImportRows)
        .onComplete(composite -> {
          Path resultFile = this.listImportFlow.getRowStatusFileJobByIdentifier(this.list.getGuid(), this.getIdentifier());
          String resultString = JsonArray.of(composite.result().list()).toString();
          Fs.write(resultFile, resultString);
        });
    } catch (IOException e) {
      throw new InternalException("List import couldn't read the csv file (" + this.getFileNameWithExtension() + "). Error: " + e.getMessage(), e);
    } catch (IllegalStructure e) {
      System.out.println(e.getMessage());
    }
  }

  public String getIdentifier() {
    return this.jobId;
  }
}
