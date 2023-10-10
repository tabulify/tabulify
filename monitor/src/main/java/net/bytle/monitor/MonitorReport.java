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

  public void print() {
    for(MonitorReportResult result: this.monitorResults){
      System.out.println(result.getStatus()+": "+ result.getMessage());
    }
  }
}
