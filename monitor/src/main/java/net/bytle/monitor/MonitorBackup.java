package net.bytle.monitor;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import net.bytle.ovh.OvhApiClient;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MonitorBackup {
  public static MonitorBackup create() {
    return new MonitorBackup();
  }

  public MonitorReport check(OvhApiClient ovhApi, String ovhVpsServiceName) {

    MonitorReport monitorReport = new MonitorReport("Ovh Backup for service (" + ovhVpsServiceName + ")");

    /**
     * https://api.ovh.com/console/#/vps/%7BserviceName%7D/backupftp~GET
     */
    Future<List<MonitorReportResult>> monitorResults = ovhApi
      .getRequest("/vps/" + ovhVpsServiceName + "/automatedBackup/restorePoints")
      .addQueryParam("state","available")
      .send()
      .compose(res -> {

        List<MonitorReportResult> monitorReportResults = new ArrayList<>();
        if (res.statusCode() != 200) {
          MonitorReportResult badRequest = MonitorReportResult.failed("Bad request " + res.body());
          monitorReportResults.add(badRequest);
          return Future.succeededFuture(monitorReportResults);
        }

        JsonArray jsonArray = res.bodyAsJsonArray();
        if (jsonArray == null) {
          MonitorReportResult badRequest = MonitorReportResult.failed("Bad request. The Json array is null");
          monitorReportResults.add(badRequest);
          return Future.succeededFuture(monitorReportResults);
        }

        /**
         * Example:
         * [
         *     "2023-11-13T04:10:17+00:00"
         *     "2023-11-12T04:10:16+00:00"
         *     "2023-11-11T04:10:17+00:00"
         *     "2023-11-10T04:10:25+00:00"
         *     "2023-11-09T04:10:10+00:00"
         *     "2023-11-08T04:10:38+00:00"
         *     "2023-11-07T04:10:23+00:00"
         * ]
         */
        Instant lastBackupDate = null;
        for (int i = 0; i < jsonArray.size(); i++) {
          String timeString = jsonArray.getString(i);
          Instant backupDate = Instant.from(DateTimeFormatter.ISO_DATE_TIME.parse(timeString));
          if (lastBackupDate == null) {
            lastBackupDate = backupDate;
          } else {
            if (lastBackupDate.isBefore(backupDate)) {
              lastBackupDate = backupDate;
            }
          }
        }


        /**
         * Minimal one backup
         */
        MonitorReportResult backupAvailable;
        if (lastBackupDate == null) {
          backupAvailable = MonitorReportResult.failed("No Backup available");
          monitorReportResults.add(backupAvailable);
          return Future.succeededFuture(monitorReportResults);
        }
        backupAvailable = MonitorReportResult.success(jsonArray.size() + " backups are available");
        monitorReportResults.add(backupAvailable);

        /**
         * Backup is not too late
         */
        long duration = Duration.between(Instant.now(), lastBackupDate).toDays();
        MonitorReportResult backupIsFresh;
        if (duration > 1) {
          // error
          backupIsFresh = MonitorReportResult.failed("The last Backup is too old (occurred " + duration + " days ago, ie " + lastBackupDate + ")");
          monitorReportResults.add(backupIsFresh);
          return Future.succeededFuture(monitorReportResults);
        }

        backupIsFresh = MonitorReportResult.success("The last Backup is not too old (occurred " + duration + " days ago, ie " + lastBackupDate + ")");
        monitorReportResults.add(backupIsFresh);
        return Future.succeededFuture(monitorReportResults);


      });

    return monitorReport.addFutureResults(monitorResults);

  }
}
