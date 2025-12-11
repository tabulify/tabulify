package com.tabulify.flow.engine;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.spi.Meta;
import com.tabulify.type.KeyNormalizer;

import java.util.List;

/**
 * A node in the graph
 * They share all these properties
 * ie {@link Pipeline} and {@link PipelineStep}
 */
public interface ExecutionNode extends Meta {

  /**
   * @return The node id (unique)
   */
  Integer getNodeId();

  /**
   * @return The name of the node (unique)
   */
  KeyNormalizer getNodeName();


  /**
   * @return the final build arguments in an attribute format
   */
  List<Attribute> getArguments();

  /**
   * @return tabular
   */
  Tabular getTabular();

  /**
   * Called at the start
   */
  default void onStart() {
  }

  /**
   * Called at the end
   */
  default void onComplete() {
  }

  /**
   * @return A string description of the type of step for reporting
   */
  String getNodeType();


}
