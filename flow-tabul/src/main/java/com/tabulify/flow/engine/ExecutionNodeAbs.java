package com.tabulify.flow.engine;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.spi.DataPath;
import com.tabulify.type.KeyNormalizer;

import java.util.List;
import java.util.Objects;
import java.util.Set;

public abstract class ExecutionNodeAbs implements ExecutionNode {


  final ExecutionNodeBuilder executionNodeBuilder;

  public ExecutionNodeAbs(ExecutionNodeBuilder executionNodeBuilder) {
    this.executionNodeBuilder = executionNodeBuilder;
  }

  @Override
  public KeyNormalizer getNodeName() {

    return this.executionNodeBuilder.getNodeName();

  }

  public String getNodeType() {
    return this.getClass().getSimpleName();
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof ExecutionNode)) return false;
    ExecutionNode that = (ExecutionNode) o;
    return Objects.equals(getNodeId(), that.getNodeId());
  }

  @Override
  public List<Attribute> getArguments() {
    return executionNodeBuilder.getArguments();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getNodeId());
  }

  @Override
  public Tabular getTabular() {
    return executionNodeBuilder.getTabular();
  }


  @Override
  public Attribute getAttribute(KeyNormalizer name) {
    return executionNodeBuilder.getAttribute(name);
  }

  @Override
  public Set<Attribute> getAttributes() {
    return this.executionNodeBuilder.getAttributes();
  }

  @Override
  public DataPath toAttributesDataPath() {
    DataPath dataPath = this.getTabular().getMemoryConnection()
      .getDataPath(this.getNodeName() + "_attributes")
      .setComment("Attributes of " + getNodeType() + " (" + this + ")");
    return toAttributesDataPath(dataPath);
  }

}
