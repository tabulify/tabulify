package com.tabulify.flow.engine;

import com.tabulify.Tabular;
import net.bytle.type.MapKeyIndependent;
import com.tabulify.conf.Attribute;

import java.util.Set;

public interface OperationStep {


  /**
   * The name of the operation
   * must be unique in the set of operation
   * @return the operation name
   */
  String getOperationName();

  /**
   *
   * @return The name of the step
   */
  String getName();


  StepAbs setTabular(Tabular tabular);

  Tabular getTabular();


  StepAbs setDescription(String comment);

  /**
   * The arguments in a map format
   * (taken from a file)
   * @param arguments the arguments
   * @return the arguments
   */
  OperationStep setArguments(MapKeyIndependent<Object> arguments);

  /**
   *
   * @param stepName The name of the step
   * @return the object for chaining
   */
  OperationStep setName(String stepName);

  /**
   *
   * @return the arguments used
   */
  Set<Attribute> getArguments();

}
