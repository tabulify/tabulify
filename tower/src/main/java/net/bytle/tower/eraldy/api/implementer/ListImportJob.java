package net.bytle.tower.eraldy.api.implementer;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.vertx.core.Future;
import io.vertx.ext.web.FileUpload;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.model.openapi.ListItem;
import net.bytle.type.time.Timestamp;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ListImportJob {
  private final ListImportManager listImportManager;
  private final ListItem list;
  private final FileUpload fileUpload;
  private final Timestamp creationTime;

  public ListImportJob(ListImportManager listImportManager, ListItem list, FileUpload fileUpload) {
    this.listImportManager = listImportManager;
    this.list = list;
    this.fileUpload = fileUpload;
    this.creationTime = Timestamp.createFromNow();
  }

  public String getIdentifier() {
    /**
     * One job by list
     */
    return list.getGuid();
  }

  String getFileNameWitExtension() {
    return this.fileUpload.fileName();
  }

  public void execute() {

    /**
     * Doc is at:
     * https://github.com/FasterXML/jackson-dataformats-text/tree/master/csv
     * below the howto
     */
    Path csv = Paths.get(fileUpload.uploadedFileName());
    CsvMapper csvMapper = new CsvMapper();
    CsvSchema schema = CsvSchema.emptySchema();

    int counter = -1;
    Map<ListImportManager.IMPORT_FIELD, Integer> headerMapping = new HashMap<>();
    try (MappingIterator<String[]> it = csvMapper
      .readerFor(String[].class)
      // This setting will transform the json as array
      .with(CsvParser.Feature.WRAP_AS_ARRAY)
      .with(schema)
      .readValues(csv.toFile())) {

      List<Future<ListImportManager.ListImportRow>> futureListImportRows = new ArrayList<>();
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
                headerMapping.put(ListImportManager.IMPORT_FIELD.EMAIL_ADDRESS, i);
                continue;
              case "firstname":
              case "givenname":
                headerMapping.put(ListImportManager.IMPORT_FIELD.GIVEN_NAME, i);
                continue;
              case "lastname":
              case "familyname":
                headerMapping.put(ListImportManager.IMPORT_FIELD.FAMILY_NAME, i);
                break;
              default:
                System.out.println(normalizedHeader);
            }
          }
          if (headerMapping.get(ListImportManager.IMPORT_FIELD.EMAIL_ADDRESS) == null) {
            throw new IllegalStructure("An email address header could not be found in the file (" + this.getFileNameWitExtension() + "). Headers found: " + Arrays.toString(row));

          }
          // second record
          continue;
        }
        Future<ListImportManager.ListImportRow> futureListImportRow = this.listImportManager.getEmailAddressValidator()
          .validate(row[headerMapping.get(ListImportManager.IMPORT_FIELD.EMAIL_ADDRESS)], true)
          .compose(emailAddressValidityReport -> {
            ListImportManager.ListImportRow listImportRow = new ListImportManager.ListImportRow();
            listImportRow.emailAddress = emailAddressValidityReport.getEmailAddress();
            listImportRow.valid = emailAddressValidityReport.isValid();
            listImportRow.validityMessage = String.join(", ", emailAddressValidityReport.getErrors().values());
            listImportRow.familyName = row[headerMapping.get(ListImportManager.IMPORT_FIELD.FAMILY_NAME)];
            listImportRow.givenName = row[headerMapping.get(ListImportManager.IMPORT_FIELD.GIVEN_NAME)];
            return Future.succeededFuture(listImportRow);
          });
        futureListImportRows.add(futureListImportRow);
        if (counter >= 10) {
          break;
        }
      }
      Future.all(futureListImportRows)
        .onComplete(composite -> {
          System.out.println("Save the output into " + this.list.getGuid() + "/" + this.creationTime.toIsoString() + "-" + csv.getFileName().toString());
          System.out.println(composite.result().list());
        });
    } catch (IOException e) {
      throw new InternalException("List import couldn't read the csv file (" + this.getFileNameWitExtension() + "). Error: " + e.getMessage(), e);
    } catch (IllegalStructure e) {
      System.out.println(e.getMessage());
    }
  }
}
