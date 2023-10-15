package net.bytle.monitor;

import io.vertx.core.Future;

import java.util.ArrayList;
import java.util.List;

public class MonitorReport {

  public static final String CRLF = "\r\n";
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
      stringBuilder.append(result.print()).append(CRLF);
    }
    return Future.join(futureResults)
      .compose(r -> {
        for (int i = 0; i < futureResults.size(); i++) {
          String print = ((MonitorReportResult) r.resultAt(i)).print();
          stringBuilder.append(print).append(CRLF);
        }
        return Future.join(futureListResults)
          .compose(rs -> {
            for (int i = 0; i < futureListResults.size(); i++) {
              List<MonitorReportResult> monitorReportResults = rs.resultAt(i);
              for (MonitorReportResult monitorReportResult : monitorReportResults) {
                stringBuilder.append(monitorReportResult.print()).append(CRLF);
              }
            }
            return Future.succeededFuture(stringBuilder.toString());
          });
      });
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
