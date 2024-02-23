package net.bytle.tower.eraldy.api.implementer.flow;

import io.vertx.core.Future;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.healthchecks.Status;
import io.vertx.ext.web.FileUpload;
import net.bytle.exception.InternalException;
import net.bytle.exception.NullValueException;
import net.bytle.fs.Fs;
import net.bytle.tower.eraldy.api.EraldyApiApp;
import net.bytle.tower.eraldy.model.openapi.ListImportJobStatus;
import net.bytle.type.Strings;
import net.bytle.type.time.Timestamp;
import net.bytle.vertx.*;
import net.bytle.vertx.collections.WriteThroughQueue;
import net.bytle.vertx.flow.FlowType;
import net.bytle.vertx.flow.WebFlowAbs;
import net.bytle.vertx.resilience.EmailAddressValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A class that handles
 * the list import flow
 */
public class ListImportFlow extends WebFlowAbs {

  static Logger LOGGER = LogManager.getLogger(ListImportFlow.class);
  private static final String FILE_SUFFIX_JOB_STATUS = "-status.json";

  private final EraldyApiApp apiApp;
  private final Path runtimeDataDirectory;
  /**
   * If a validation fail with a fatal error
   * (DNS timeout, DNS servfail error, ...)
   * we retry up to this number (default is 2 for a total of 3 attempts)
   */
  private final int rowValidationFailureRetryCount;

  /**
   * The maximum of jobs to keep by list
   */
  private final int maxJobHistoryByList;
  /**
   * The retention days
   * Passed these number of days, we delete the import jobs
   */
  private final int maxJobHistoryRetentionInDays;
  /**
   * The delay/period to set on the timer to execute
   * the jobs.
   */
  private final Integer executionPeriodInMs;
  /**
   * The period of purge
   */
  private final int purgeJobPeriodMs;

  /**
   * Utility variable to get quickly the server
   */
  private final Server server;
  /**
   * The last execution time to check that the job execution is still healthy
   */
  private LocalDateTime executionLastTime;
  /**
   * The last purge time to check that the job purge is still healthy
   */
  private LocalDateTime purgeLastTime;


  public EmailAddressValidator getEmailAddressValidator() {
    return this.apiApp.getEmailAddressValidator();
  }

  @Override
  public EraldyApiApp getApp() {
    return apiApp;
  }

  @Override
  public FlowType getFlowType() {
    return FlowType.LIST_IMPORT;
  }

  public Path getRuntimeDataDirectory() {
    return this.runtimeDataDirectory;
  }


  public Path getRowStatusFileJobByIdentifier(String listIdentifier, String jobIdentifier) {

    return this.getListDirectory(listIdentifier)
      .resolve(jobIdentifier + "-status-rows.json");
  }

  public Path getStatusFileJobByIdentifier(String listIdentifier, String jobIdentifier) {

    return this.getListDirectory(listIdentifier)
      .resolve(jobIdentifier + FILE_SUFFIX_JOB_STATUS);
  }

  Path getListDirectory(String listGuid) {
    Path listDirectory = this
      .getRuntimeDataDirectory()
      .resolve(listGuid);
    Fs.createDirectoryIfNotExists(listDirectory);
    return listDirectory;
  }

  public List<ListImportJobStatus> getJobsStatuses(String listGuid) {

    /**
     * Actual Jobs
     */
    List<ListImportJobStatus> listImportJobStatuses = this.importJobQueue
      .stream()
      .map(ListImportJob::getStatus)
      .filter(status -> status.getListGuid().equals(listGuid))
      .collect(Collectors.toList());

    /**
     * Add the Past jobs
     */
    List<Path> files = Fs.getChildrenFiles(this.getListDirectory(listGuid));
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


  public ListImportJob getQueuedJob(String jobIdentifier) throws NullValueException {

    /**
     * In the queue
     */
    for (ListImportJob listImportJob : this.importJobQueue) {
      if (listImportJob.getJobId().equals(jobIdentifier)) {
        return listImportJob;
      }
    }
    throw new NullValueException();

  }

  /**
   * @return the maximum number of retries in case of failure
   */
  public int getRowValidationFailureRetryCount() {
    return this.rowValidationFailureRetryCount;
  }

  @Override
  public Future<Void> start() {

    /**
     * Load job (from database, ie not processed or were processing)
     */
    importJobQueue.loadElementsFromSink();

    /**
     * Job
     */
    LOGGER.info("Starting the job cron");
    this.executionLastTime = LocalDateTime.now();
    this.purgeLastTime = LocalDateTime.now();
    this.scheduleNextJob();

    /**
     * Purge Job
     */
    LOGGER.info("Starting the purge history job cron");
    server.getVertx().setPeriodic(6000, this.purgeJobPeriodMs, jobId -> purgeJobHistory());

    /**
     * Health Checks
     */
    this.server
      .getServerHealthCheck()
      .register(ListImportFlow.class, promise -> {

        /**
         * Test
         * Boolean should be true
         */
        Duration agoLastExecution = Duration.between(this.executionLastTime, LocalDateTime.now());
        boolean executionTest = agoLastExecution.toMillis() <= this.executionPeriodInMs + 1000;
        Duration agoLastPurge = Duration.between(this.purgeLastTime, LocalDateTime.now());
        boolean purgeTest = agoLastPurge.toMillis() <= this.purgeJobPeriodMs + 1000;

        /**
         * Data
         * Bug? even if we have added the LocalDateTime Jackson Time module, we get an error
         * {@link JacksonMapperManager}
         * We do it manually then
         */
        String purgeLastTimeString = DateTimeUtil.LocalDateTimetoString(this.purgeLastTime);
        String executionsLastTimeString = DateTimeUtil.LocalDateTimetoString(this.executionLastTime);
        JsonObject data = new JsonObject();
        data.put("purge-last-time", purgeLastTimeString);
        data.put("purge-last-ago-sec", agoLastPurge.toSeconds());
        data.put("execution-last-time", executionsLastTimeString);
        data.put("execution-last-ago-sec", agoLastExecution.toSeconds());

        /**
         * Checks and Status Report
         */
        // ok
        if (
          executionTest
            && purgeTest) {
          promise.complete(Status.OK(data));
          return;
        }
        // not ok
        Status status = Status.KO();
        List<String> messages = new ArrayList<>();
        if (!executionTest) {
          messages.add("The last time execution date is too old.");
        }
        if (!purgeTest) {
          messages.add("The last time purge date is too old.");
        }
        data.put("message", String.join(" ", messages));
        status.setData(data);
        promise.complete(status);
      });

    return Future.succeededFuture();

  }

  @Override
  public void close() {
    InternalException e = new InternalException("The server has restarted while this job was running. We are Sorry. You need to re-create a new import job.");
    for (ListImportJob listImportJob : this.importJobQueue) {
      listImportJob.closeJobWithFatalError(e, null);
    }
  }

  public ListImportJob createJobFromApi(ListImportJobStatus listImportJobStatus, FileUpload fileUpload) {

    Timestamp nowInUtc = Timestamp.createFromNowUtc();
    listImportJobStatus.setCreationTime(nowInUtc.toLocalDateTime());
    String jobId = nowInUtc.toFileSystemString();
    listImportJobStatus.setJobId(jobId);
    if (listImportJobStatus.getMaxRowCountToProcess() == null) {
      listImportJobStatus.setMaxRowCountToProcess(5000);
    }
    Objects.requireNonNull(listImportJobStatus.getListUserActionCode());
    if (listImportJobStatus.getUserActionCode() == null) {
      listImportJobStatus.setUserActionCode(ListImportUserAction.UPDATE.getActionCode());
    }
    Objects.requireNonNull(listImportJobStatus.getUploadedFileName());
    /**
     * Move the csv file
     */
    Path csvFile = this.getSourceFilePathFromJobStatus(listImportJobStatus);
    Path source = Paths.get(fileUpload.uploadedFileName());
    Fs.move(source, csvFile);
    return new ListImportJob(this, listImportJobStatus, csvFile);
  }

  /**
   * @param listImportJobStatus - the metadata
   * @return the path of the source csv file used for the job
   */
  private Path getSourceFilePathFromJobStatus(ListImportJobStatus listImportJobStatus) {
    return this.getListDirectory(listImportJobStatus.getListGuid()).resolve(listImportJobStatus.getJobId() + ".csv");
  }

  /**
   * Create a job from the database store
   *
   * @param listImportStatus - the import status
   * @return the job
   */
  public ListImportJob createJobFromDatabase(ListImportJobStatus listImportStatus) {
    Path csvFile = this.getSourceFilePathFromJobStatus(listImportStatus);
    return new ListImportJob(this, listImportStatus, csvFile);
  }


  /**
   * The fields in the import file
   */
  enum IMPORT_FIELD {
    GIVEN_NAME, FAMILY_NAME, OPT_IN_TIME, OPT_IN_IP, CONFIRM_TIME, TIMEZONE, CONFIRM_IP, OPT_IN_ORIGIN, EMAIL_ADDRESS
  }

  /**
   * Import Job by JobId String
   */
  final WriteThroughQueue<ListImportJob> importJobQueue;

  public ListImportFlow(EraldyApiApp apiApp) {
    super(apiApp);
    this.apiApp = apiApp;
    this.server = apiApp.getHttpServer().getServer();

    importJobQueue = this.server.getWriteThroughCollection()
      .createQueue(
        ListImportJob.class,
        "list-import",
        new ListImportJobSerializer(this)
      );

    /**
     * Config
     */
    this.runtimeDataDirectory = this.apiApp.getRuntimeDataDirectory().resolve("list-import");
    Fs.createDirectoryIfNotExists(this.runtimeDataDirectory);
    ConfigAccessor configAccessor = server.getConfigAccessor();
    this.rowValidationFailureRetryCount = configAccessor.getInteger("list.import.execution.row.validation.retry.count", 2);


    /**
     * Timer Queue Execution
     */
    this.executionPeriodInMs = configAccessor.getInteger("list.import.execution.delay.ms", 5000);

    /**
     * Purge
     */
    int purgeDelayMsDefault = 24 * 60 * 60000;
    this.purgeJobPeriodMs = configAccessor.getInteger("list.import.purge.history.delay", purgeDelayMsDefault);


    /**
     * The number of files created by job import
     * - The original file
     * - the status report for the job
     * - the status report for the rows
     */
    int numberOfFilesToPurgeByJob = 3;
    this.maxJobHistoryByList = 10 * numberOfFilesToPurgeByJob;
    this.maxJobHistoryRetentionInDays = 30;

  }

  private void scheduleNextJob() {
    this.server.getVertx().setTimer(executionPeriodInMs, jobId -> step2ExecuteNextJob());
  }

  private void purgeJobHistory() {

    this.purgeLastTime = LocalDateTime.now();
    List<Path> firstLevelChildren = Fs.getChildrenFiles(this.runtimeDataDirectory);
    Instant maxRetentionTime = Instant.now().minus(Duration.ofDays(this.maxJobHistoryRetentionInDays));

    for (Path child : firstLevelChildren) {

      if (Fs.isFile(child)) {
        continue;
      }

      List<Path> secondLevelChildren = Fs.getChildrenFiles(child);
      // sort in descending order to keep the recent first
      secondLevelChildren.sort(Comparator.comparing(Fs::getCreationTime).reversed());
      int fileCounter = 0;
      for (Path listImportFile : secondLevelChildren) {

        fileCounter++;

        if (fileCounter > this.maxJobHistoryByList) {
          Fs.deleteIfExists(listImportFile);
          continue;
        }
        boolean isTooOld = Fs.getCreationTime(listImportFile).isBefore(maxRetentionTime);
        if (isTooOld) {
          Fs.deleteIfExists(listImportFile);
        }
      }

    }
  }

  public String step1AddJobToQueue(ListImportJob importJob) throws TowerFailureException {

    String listGuid = importJob.getStatus().getListGuid();
    for (ListImportJob listImportJob : importJobQueue) {
      if (listImportJob.getStatus().getListGuid().equals(listGuid)) {
        throw TowerFailureException.builder()
          .setType(TowerFailureTypeEnum.ALREADY_EXIST_409)
          .setMessage("The list (" + listGuid + ") has already a job in the queue")
          .build();
      }
    }

    String identifier = importJob.getJobId();
    importJobQueue.add(importJob);
    return identifier;
  }


  public void step2ExecuteNextJob() {

    this.executionLastTime = LocalDateTime.now();
    ListImportJob listImportJob;
    synchronized (this) {
      listImportJob = this.importJobQueue.peek();
      if (listImportJob == null) {
        this.scheduleNextJob();
        return;
      }
      /**
       * The peek job is deleted at the end of
       * the execution
       */
      if (listImportJob.isRunning()) {
        this.scheduleNextJob();
        return;
      }
    }

    int poolSize = 2;
    WorkerExecutor executor = this.getExecutor(poolSize);
    try {
      listImportJob.executeSequentially(executor, poolSize)
        .onComplete(blockingAsyncResult -> {
          /**
           * Executor Fatal Error
           * (Timeout)
           */
          if (blockingAsyncResult.failed()) {
            Throwable cause = blockingAsyncResult.cause();
            listImportJob.closeJobWithFatalError(cause, null);
            this.closeExecutionAndExecuteNextJob(listImportJob, executor);
            return;
          }
          this.closeExecutionAndExecuteNextJob(listImportJob, executor);
        });
    } catch (Exception e) {
      // Trying to catch JVM error such as Stack overflow error
      listImportJob.closeJobWithFatalError(e, null);
      this.closeExecutionAndExecuteNextJob(listImportJob, executor);
    }

  }

  private WorkerExecutor getExecutor(int poolSize) {
    long maxExecutionTimeSecond = 60 * 5; // 5 minutes
    TimeUnit maxExecuteTimeUnit = TimeUnit.SECONDS;
    String workerExecutor = "list-import-flow";
    return this.server.getVertx().createSharedWorkerExecutor(workerExecutor, poolSize, maxExecutionTimeSecond, maxExecuteTimeUnit);
  }

  private void closeExecutionAndExecuteNextJob(ListImportJob executingJob, WorkerExecutor executor) {
    /**
     * Remove from the queue
     */
    this.importJobQueue.remove(executingJob);
    /**
     * Destroy executor
     */
    executor.close();
    /**
     *
     * Execute the next one
     */
    this.step2ExecuteNextJob();
  }


}
