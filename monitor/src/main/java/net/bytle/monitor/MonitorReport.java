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


  public MonitorReport(String title) {
    this.title = title;
  }


  public Future<String> print() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(this.title).append(":" + CRLF);
    for (MonitorReportResult result : this.monitorResults) {
      this.appendResultPrint(stringBuilder, result);
    }
    return Future.join(futureResults)
      .compose(r -> {
        for (int i = 0; i < futureResults.size(); i++) {
          this.appendResultPrint(stringBuilder, r.resultAt(i));
        }
        return Future.join(futureListResults)
          .compose(rs -> {
            for (int i = 0; i < futureListResults.size(); i++) {
              List<MonitorReportResult> monitorReportResults = rs.resultAt(i);
              for (MonitorReportResult monitorReportResult : monitorReportResults) {
                this.appendResultPrint(stringBuilder, monitorReportResult);
              }
            }
            return Future.succeededFuture(stringBuilder.toString());
          });
      });
  }

  private void appendResultPrint(StringBuilder stringBuilder, MonitorReportResult result) {
    String TAB1 = "  ";
    stringBuilder.append(TAB1).append(LIST_CHAR).append(" ").append(result.print()).append(CRLF);
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
}
