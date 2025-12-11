package com.tabulify.flow.operation;

public enum SelectPipelineStepArgumentOrder {

  /**
   * Natural order
   */
  NATURAL,
  /**
   * Create order
   * Independent data resource first
   */
  CREATE,
  /**
   * Create order
   * Dependent data resource first
   */
  DROP

}
