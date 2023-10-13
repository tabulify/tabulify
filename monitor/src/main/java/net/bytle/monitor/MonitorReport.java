package net.bytle.monitor;

import java.util.ArrayList;
import java.util.List;

public class MonitorReport {

  private final String title;
  List<MonitorReportResult> monitorResults = new ArrayList<>();

  public MonitorReport(String title) {
    this.title = title;
  }

  public MonitorReportResult addFailure(String s) {
    MonitorReportResult failed = MonitorReportResult.failed(s);
    monitorResults.add(failed);
    return failed;
  }

  public MonitorReportResult addSuccess(String message) {
    MonitorReportResult success = MonitorReportResult.success(message);
    monitorResults.add(success);
    return success;
  }

  public String print() {
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(this.title)
      .append(":\r\n");
    for(MonitorReportResult result: this.monitorResults){
      stringBuilder
        .append(result.getStatus())
        .append(": ")
        .append(result.getMessage())
        .append("\r\n");
    }
    return stringBuilder.toString();
  }


  @Override
  public String toString() {
    return print();
  }

}
