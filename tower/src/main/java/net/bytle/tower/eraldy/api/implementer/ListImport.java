package net.bytle.tower.eraldy.api.implementer;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.vertx.core.Future;
import io.vertx.ext.web.FileUpload;
import net.bytle.exception.IllegalStructure;
import net.bytle.exception.InternalException;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.ListItem;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class ListImport {
  private final FileUpload fileUpload;
  private final ListItem list;
  private final EraldyApiApp apiApp;

  enum IMPORT_FIELD {
    GIVEN_NAME, FAMILY_NAME, EMAIL_ADDRESS
  }

  public static class ListImportRow {
    boolean valid;
    String validityMessage;
    String emailAddress;
    String givenName;
    String familyName;
  }

  public ListImport(EraldyApiApp apiApp, ListItem list, FileUpload fileUpload) {
    this.apiApp = apiApp;
    this.list = list;
    this.fileUpload = fileUpload;
  }

  public static ListImport create(EraldyApiApp apiApp, ListItem list, FileUpload fileBinary) {
    return new ListImport(apiApp, list, fileBinary);
  }

  String getFileNameWitExtension(){
    return this.fileUpload.fileName();

  }

  public Future<List<ListImportRow>> execute() {
    /**
     * Doc is at:
     * https://github.com/FasterXML/jackson-dataformats-text/tree/master/csv
     * below the howto
     */
    Path csv = Paths.get(fileUpload.uploadedFileName());
    // file name is a random string
    String uploadId = csv.getFileName().toString();
    CsvMapper csvMapper = new CsvMapper();
    CsvSchema schema = CsvSchema.emptySchema();

    int counter = -1;
    Map<IMPORT_FIELD, Integer> headerMapping = new HashMap<>();
    try (MappingIterator<String[]> it = csvMapper
      .readerFor(String[].class)
      // This setting will transform the json as array
      .with(CsvParser.Feature.WRAP_AS_ARRAY)
      .with(schema)
      .readValues(csv.toFile())) {

      List<Future<ListImportRow>> futureListImportRows = new ArrayList<>();
      while (it.hasNextValue()) {
        counter++;
        String[] row = it.nextValue();
        if (counter == 0) {
          // Header
          // Mailchimp doc
          // https://mailchimp.com/help/view-export-contacts/
          for (int i=0;i<row.length;i++) {
            String normalizedHeader = row[i].trim().replace(" ","") .toLowerCase();
            switch (normalizedHeader){
              case "email":
              case "emailaddress":
                headerMapping.put(IMPORT_FIELD.EMAIL_ADDRESS,i);
                continue;
              case "firstname":
              case "givenname":
                headerMapping.put(IMPORT_FIELD.GIVEN_NAME,i);
                continue;
              case "lastname":
              case "familyname":
                headerMapping.put(IMPORT_FIELD.FAMILY_NAME,i);
                break;
              default:
                System.out.println(normalizedHeader);
            }
          }
          if(headerMapping.get(IMPORT_FIELD.EMAIL_ADDRESS)==null){
            return Future.failedFuture(new IllegalStructure("An email address header could not be found in the file ("+this.getFileNameWitExtension()+"). Headers found: "+ Arrays.toString(row)));
          }
        }
        Future<ListImportRow> futureListImportRow = this.apiApp.getEmailAddressValidator()
          .validate(row[headerMapping.get(IMPORT_FIELD.EMAIL_ADDRESS)], true)
          .compose(emailAddressValidityReport -> {
            ListImportRow listImportRow = new ListImportRow();
            listImportRow.emailAddress = emailAddressValidityReport.getEmailAddress();
            listImportRow.valid = emailAddressValidityReport.isValid();
            listImportRow.validityMessage = String.join(", ", emailAddressValidityReport.getErrors().values());
            listImportRow.familyName = row[headerMapping.get(IMPORT_FIELD.FAMILY_NAME)];
            listImportRow.givenName = row[headerMapping.get(IMPORT_FIELD.GIVEN_NAME)];
            return Future.succeededFuture(listImportRow);
          });
        futureListImportRows.add(futureListImportRow);
      }
      return Future.all(futureListImportRows)
        .compose(composite->Future.succeededFuture(composite.list()));
    } catch (IOException e) {
      return Future.failedFuture(new InternalException("List import couldn't read the csv file (" + this.getFileNameWitExtension() + "). Error: " + e.getMessage(), e));
    }

  }
}
