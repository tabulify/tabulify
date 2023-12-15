package net.bytle.tower.eraldy.api.implementer;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.vertx.core.Vertx;
import io.vertx.ext.web.FileUpload;
import net.bytle.fs.Fs;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.ListItem;
import net.bytle.tower.util.EmailAddressValidator;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.flow.WebFlow;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * A class tha handles
 * the list import flow
 */
public class ListImportFlow implements WebFlow {

  private final EraldyApiApp apiApp;
  private final Path runtimeDataDirectory;
  private boolean running = false;

  public EmailAddressValidator getEmailAddressValidator() {
    return this.apiApp.getEmailAddressValidator();
  }

  @Override
  public TowerApp getApp() {
    return apiApp;
  }

  public Path getRuntimeDataDirectory() {
    return this.runtimeDataDirectory;
  }

  enum IMPORT_FIELD {
    GIVEN_NAME, FAMILY_NAME, EMAIL_ADDRESS
  }

  public static class ListImportRow {
    @JsonProperty("valid")
    public boolean valid;
    @JsonProperty("validityMessage")
    public String validityMessage;
    @JsonProperty("emailAddress")
    public String emailAddress;
    @JsonProperty("givenName")
    public String givenName;
    @JsonProperty("familyName")
    public String familyName;
  }

  Map<String, ListImportJob> importJobs = new HashMap<>();

  public ListImportFlow(EraldyApiApp apiApp) {
    this.apiApp = apiApp;
    int sec10 = 10000;
    Vertx vertx = apiApp.getApexDomain().getHttpServer().getServer().getVertx();
    this.runtimeDataDirectory = this.apiApp.getRuntimeDataDirectory().resolve("list-import");
    Fs.createDirectoryIfNotExists(this.runtimeDataDirectory);
    vertx.setPeriodic(sec10, sec10, jobId -> processJob());
  }

  public String step1CreateAndGetJobId(ListItem list, FileUpload upload) throws TowerFailureException {

    if (importJobs.containsKey(list.getGuid())) {
      throw TowerFailureException.builder()
        .setType(TowerFailureTypeEnum.ALREADY_EXIST_409)
        .setMessage("The list (" + list + ") has already a job running")
        .build();
    }
    ListImportJob importJob = new ListImportJob(this, list, upload);
    importJobs.put(list.getGuid(), importJob);
    return importJob.getIdentifier();
  }


  public void processJob() {

    if (this.running || this.importJobs.isEmpty()) {
      return;
    }
    this.running = true;
    try {
      for (Map.Entry<String, ListImportJob> listImportJobEntry : this.importJobs.entrySet()) {
        listImportJobEntry.getValue().execute();
        this.importJobs.remove(listImportJobEntry.getKey());
      }
    } finally {
      this.running = false;
    }
  }
}
