package net.bytle.monitor;

public class MonitorReportResult {
  private MonitorReportResultStatus monitorReportResultStatus;
  private String message;
  private String checkName = "";

  public static MonitorReportResult failed(String message) {
    MonitorReportResult monitor = new MonitorReportResult();
    monitor.monitorReportResultStatus = MonitorReportResultStatus.FAILURE;
    monitor.message = message;
    return monitor;
  }

  public static MonitorReportResult success(String message) {
    MonitorReportResult monitor = new MonitorReportResult();
    monitor.monitorReportResultStatus = MonitorReportResultStatus.SUCCESS;
    monitor.message = message;
    return monitor;
  }


  public static MonitorReportResult create(MonitorReportResultStatus status, String message) {
    MonitorReportResult monitor = new MonitorReportResult();
    monitor.monitorReportResultStatus = status;
    monitor.message = message;
    return monitor;
  }

  public MonitorReportResultStatus getStatus() {
    return this.monitorReportResultStatus;
  }

  public String getMessage() {
    if (!checkName.equals("")) {
      return this.checkName + ": " + this.message;
    }
    return this.message;
  }

  public MonitorReportResult setCheckName(String checkName) {
    this.checkName = checkName;
    return this;
  }

}
