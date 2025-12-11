package com.tabulify.flow.engine;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.AttributeEnum;
import com.tabulify.conf.Origin;
import com.tabulify.exception.CastException;
import com.tabulify.type.KeyNormalizer;

import java.util.*;

public class ExecutionNodeBuilder {

  private final Map<KeyNormalizer, com.tabulify.conf.Attribute> argumentMap = new HashMap<>();
  private final Map<KeyNormalizer, com.tabulify.conf.Attribute> derivedAttributeMap = new HashMap<>();
  @SuppressWarnings("FieldCanBeLocal")
  private String comment;
  /**
   * Node name is a logical name
   */
  private KeyNormalizer nodeName;
  private Tabular tabular;


  public List<Class<? extends ArgumentEnum>> getArgumentEnums() {
    return List.of();
  }

  /**
   * @param nodeName The name of the node
   * @return the object for chaining
   */
  public ExecutionNodeBuilder setNodeName(String nodeName) {
    try {
      this.nodeName = KeyNormalizer.create(nodeName);
    } catch (CastException e) {
      throw new IllegalArgumentException("The step name (" + nodeName + ") is not valid. Error: " + e.getMessage(), e);
    }
    return this;
  }


  public KeyNormalizer getNodeName() {
    return this.nodeName;
  }

  /**
   * @param nodeComment - the comment/description of the node
   */
  public ExecutionNodeBuilder setComment(String nodeComment) {
    this.comment = nodeComment;
    return this;
  }

  @Override
  public String toString() {
    return (this.getNodeName() != null ? this.getNodeName().toCliLongOptionName() : super.getClass().getSimpleName());
  }

  /**
   * Utility function used in the constructor for the implementer
   * that initialize the arguments list object
   */
  protected void buildArgumentAttributesFromClass() {
    for (Class<? extends ArgumentEnum> argumentEnums : getArgumentEnums()) {
      for (ArgumentEnum argumentEnum : argumentEnums.getEnumConstants()) {
        Attribute attribute = Attribute.create(argumentEnum, Origin.DEFAULT);
        this.argumentMap.put(KeyNormalizer.createSafe(argumentEnum), attribute);
      }
    }
  }

  protected void setArgument(Attribute attribute) {
    ArgumentEnum attributeMetadata = (ArgumentEnum) attribute.getAttributeMetadata();
    this.argumentMap.put(KeyNormalizer.createSafe(attributeMetadata), attribute);
  }

  /**
   * @return the arguments in an attribute format
   */
  public List<Attribute> getArguments() {
    return new ArrayList<>(argumentMap.values());
  }

  /**
   * @return an argument
   * To make it possible to change the default after initialization in the constructor
   */
  public Attribute getArgument(ArgumentEnum argumentEnum) {

    return getArgument(KeyNormalizer.createSafe(argumentEnum.name()));

  }

  /**
   * Set a raw external argument (from a file or cli)
   */
  public ExecutionNodeBuilder setArgument(KeyNormalizer key, Object value) {
    throw new IllegalArgumentException("The argument (" + key + ") is unknown for the " + this.getClass().getSimpleName() + "(" + this.getNodeName() + ")");
  }

  /**
   * The arguments in a map format
   * (taken from a Yaml file)
   * <p>
   * You should implement{@link #setArgument(KeyNormalizer, Object)}
   *
   * @param arguments the arguments
   */
  public ExecutionNodeBuilder setArguments(Map<KeyNormalizer, Object> arguments) {
    for (Map.Entry<KeyNormalizer, Object> argument : arguments.entrySet()) {
      setArgument(argument.getKey(), argument.getValue());
    }
    return this;
  }

  protected Tabular getTabular() {
    return this.tabular;
  }


  public ExecutionNodeBuilder setTabular(Tabular tabular) {
    this.tabular = tabular;
    return this;
  }

  public Attribute getArgument(KeyNormalizer keyNormalizer) {
    return argumentMap.get(keyNormalizer);
  }

  protected ExecutionNodeBuilder setDerivedAttribute(Attribute attribute) {
    AttributeEnum attributeMetadata = attribute.getAttributeMetadata();
    this.derivedAttributeMap.put(KeyNormalizer.createSafe(attributeMetadata), attribute);
    return this;
  }

  public Attribute getDerivedAttribute(KeyNormalizer keyNormalizer) {
    return this.derivedAttributeMap.get(keyNormalizer);
  }

  public Attribute getAttribute(KeyNormalizer keyNormalizer) {
    Attribute attribute = this.derivedAttributeMap.get(keyNormalizer);
    if (attribute != null) {
      return attribute;
    }
    return this.argumentMap.get(keyNormalizer);
  }

  public Set<Attribute> getAttributes() {
    Set<Attribute> attributes = new HashSet<>();
    attributes.addAll(this.argumentMap.values());
    attributes.addAll(this.derivedAttributeMap.values());
    return attributes;
  }

  public Set<KeyNormalizer> getAttributeNames() {
    Set<KeyNormalizer> attributes = new HashSet<>();
    attributes.addAll(this.argumentMap.keySet());
    attributes.addAll(this.derivedAttributeMap.keySet());
    return attributes;
  }


}
