package net.bytle.monitor;

public class MonitorReportResult {
  private MonitorReportResultStatus monitorReportResultStatus;
  private String message;

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

  public static MonitorReportResult warning(String message) {
    MonitorReportResult monitor = new MonitorReportResult();
    monitor.monitorReportResultStatus = MonitorReportResultStatus.WARNING;
    monitor.message = message;
    return monitor;
  }

  public MonitorReportResultStatus getStatus() {
    return this.monitorReportResultStatus;
  }

  public String getMessage() {
    return this.message;
  }

}
