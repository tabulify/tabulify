package com.tabulify.flow.operation;

import com.tabulify.conf.AttributeValue;

/**
 * The report returned
 */
public enum DiffPipelineStepReportType implements AttributeValue {

  CELL("A report with cells"),
  UNIFIED("A unified report with the source/target record"),
  SUMMARY("The result of the diff only (diff stats)");


  private final String description;

  DiffPipelineStepReportType(String description) {

    this.description = description;
  }


  @Override
  public String getDescription() {
    return this.description;
  }


}
