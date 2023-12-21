package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import net.bytle.fs.Fs;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.ListImportJobStatus;
import net.bytle.tower.eraldy.model.openapi.ListItem;
import net.bytle.type.Strings;
import net.bytle.vertx.TowerApp;
import net.bytle.vertx.TowerFailureException;
import net.bytle.vertx.TowerFailureTypeEnum;
import net.bytle.vertx.flow.WebFlow;
import net.bytle.vertx.resilience.EmailAddressValidator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class tha handles
 * the list import flow
 */
public class ListImportFlow implements WebFlow {

  private static final String FILE_SUFFIX_JOB_STATUS = "-status.json";
  private final EraldyApiApp apiApp;
  private final Path runtimeDataDirectory;
  private boolean running = false;
  private final int maxRowsProcessedByImport;

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

  public int getMaxRowsProcessedByImport() {
    return this.maxRowsProcessedByImport;
  }

  public Path getRowStatusFileJobByIdentifier(String listIdentifier, String jobIdentifier) {

    return this.getListDirectory(listIdentifier)
      .resolve(jobIdentifier + "-status-rows.json");
  }

  public Path getStatusFileJobByIdentifier(String listIdentifier, String jobIdentifier) {

    return this.getListDirectory(listIdentifier)
      .resolve(jobIdentifier + FILE_SUFFIX_JOB_STATUS);
  }

  Path getListDirectory(String listIdentifier) {
    Path listDirectory = this
      .getRuntimeDataDirectory()
      .resolve(listIdentifier);
    Fs.createDirectoryIfNotExists(listDirectory);
    return listDirectory;
  }

  public List<ListImportJobStatus> getJobsStatuses(String listIdentifier) {
    List<Path> files = Fs.getChildrenFiles(this.getListDirectory(listIdentifier));
    List<ListImportJobStatus> listImportJobStatuses = new ArrayList<>();
    for (Path file : files) {
      if (Files.isDirectory(file)) {
        continue;
      }
      // The toString is important otherwise it compares with the object id
      if (!file.toString().endsWith(FILE_SUFFIX_JOB_STATUS)) {
        continue;
      }
      String string = Strings.createFromPath(file).toString();
      JsonObject jsonObject = new JsonObject(string);
      ListImportJobStatus listImportStatus;
      try {
        listImportStatus = jsonObject.mapTo(ListImportJobStatus.class);
      } catch (Exception e) {
        // Migration error
        continue;
      }
      listImportJobStatuses.add(listImportStatus);
    }
    return listImportJobStatuses;
  }

  public boolean isRunning(String jobIdentifier) {
    return this.importJobs.containsKey(jobIdentifier);
  }

  public ListImportJob getQueuedJob(String jobIdentifier) {
    return this.importJobs.get(jobIdentifier);
  }


  /**
   * The fields in the import file
   */
  enum IMPORT_FIELD {
    GIVEN_NAME, FAMILY_NAME, EMAIL_ADDRESS
  }


  Map<String, ListImportJob> importJobs = new HashMap<>();

  public ListImportFlow(EraldyApiApp apiApp) {
    this.apiApp = apiApp;
    int sec10 = 10000;
    Vertx vertx = apiApp.getApexDomain().getHttpServer().getServer().getVertx();
    this.runtimeDataDirectory = this.apiApp.getRuntimeDataDirectory().resolve("list-import");
    Fs.createDirectoryIfNotExists(this.runtimeDataDirectory);
    this.maxRowsProcessedByImport = apiApp.getApexDomain().getHttpServer().getServer().getConfigAccessor().getInteger("list.import.max.rows", 3000);
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
        ListImportJob listImportJob = listImportJobEntry.getValue();
        listImportJob.execute();
        this.importJobs.remove(listImportJobEntry.getKey());
      }
    } finally {
      this.running = false;
    }
  }
}
