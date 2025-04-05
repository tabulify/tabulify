package net.bytle.db.flow.step;

import net.bytle.type.AttributeValue;

/**
 * The report returned
 */
public enum CompareStepReportType implements AttributeValue {

  RESOURCE("A global report with all resources and their comparison result"),
  RECORD("A report by data resources and the comparison details by record"),
  ALL("Return the resource and record reports");


  private final String description;

  CompareStepReportType(String description) {

    this.description = description;
  }


  @Override
  public String getDescription() {
    return this.description;
  }


}
