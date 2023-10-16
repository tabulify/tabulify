package net.bytle.monitor;

import io.vertx.core.Future;

import java.util.ArrayList;
import java.util.List;

public class MonitorReport {

  public static final String CRLF = "\r\n";
  private static final char LIST_CHAR = '*';
  public static String TAB2 = "    ";
  private final String title;
  List<MonitorReportResult> monitorResults = new ArrayList<>();
  private final List<Future<MonitorReportResult>> futureResults = new ArrayList<>();
  private final List<Future<List<MonitorReportResult>>> futureListResults = new ArrayList<>();
  private boolean joined = false;


  public MonitorReport(String title) {
    this.title = title;
  }

  public Future<MonitorReport> resolve() {
    if (this.joined) {
      return Future.succeededFuture(null);
    }
    this.joined = true;
    return Future.join(futureResults)
      .compose(r -> {

        this.monitorResults.addAll(r.list());
        return Future.join(futureListResults)
          .compose(rs -> {
            for (int i = 0; i < futureListResults.size(); i++) {
              List<MonitorReportResult> futureMonitorReportResults = rs.resultAt(i);
              this.monitorResults.addAll(futureMonitorReportResults);
            }
            return Future.succeededFuture(this);
          });
      });
  }

  public String print() {
    if (!this.joined) {
      throw new RuntimeException("The report should be first resolved");
    }
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(this.title).append(":" + CRLF);
    String TAB1 = "  ";
    for (MonitorReportResult result : this.monitorResults) {
      stringBuilder.append(TAB1).append(LIST_CHAR).append(" ").append(result.print()).append(CRLF);
    }
    return stringBuilder.toString();
  }


  @Override
  public String toString() {
    return this.title;
  }

  public void addResult(MonitorReportResult monitorReportResult) {
    this.monitorResults.add(monitorReportResult);
  }

  public MonitorReport addFutureResult(Future<MonitorReportResult> monitorReportResult) {
    this.futureResults.add(monitorReportResult);
    return this;
  }


  public MonitorReport addFutureResults(Future<List<MonitorReportResult>> listFuture) {
    this.futureListResults.add(listFuture);
    return this;
  }

  public int getFailures() {
    if (!this.joined) {
      throw new RuntimeException("The report should be first resolved");
    }
    return Math.toIntExact(
      this.monitorResults.stream().filter(r -> r.getStatus() == MonitorReportResultStatus.FAILURE)
        .count()
    );
  }
}
