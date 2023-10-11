package net.bytle.monitor;

import java.util.ArrayList;
import java.util.List;

public class MonitorReport {

  List<MonitorReportResult> monitorResults = new ArrayList<>();

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
    for(MonitorReportResult result: this.monitorResults){
      stringBuilder
        .append(result.getStatus())
        .append(": ")
        .append(result.getMessage())
        .append("\r\n");
    }
    return stringBuilder.toString();
  }
}
