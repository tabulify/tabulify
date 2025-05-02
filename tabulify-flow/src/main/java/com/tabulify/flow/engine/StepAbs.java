package com.tabulify.flow.engine;

import com.tabulify.Tabular;
import com.tabulify.conf.AttributeEnum;
import com.tabulify.conf.Origin;
import net.bytle.exception.NotFoundException;
import net.bytle.type.*;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class StepAbs extends StepProvider implements OperationStep {


  protected Tabular tabular;
  protected String name;
  private String comment;

  private final MapKeyIndependent<com.tabulify.conf.Attribute> arguments = new MapKeyIndependent<>();


  @Override
  public StepAbs setTabular(Tabular tabular) {
    this.tabular = tabular;
    return this;
  }

  @Override
  public Tabular getTabular() {
    return this.tabular;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public StepAbs setName(String name) {
    if (name.contains(" ")) {
      throw new IllegalStateException("The step name (`" + name + "`) is not compliant because it has a space. Delete the spaces.");
    }
    this.name = name;
    return this;
  }

  @Override
  public StepAbs setDescription(String comment) {

    this.comment = comment;
    return this;
  }

  @Override
  public Boolean accept(String commandName) {
    String operationName = getOperationName();
    if (operationName == null) {
      throw new IllegalStateException("The `getOperationName` function of the step (" + this.getClass().getSimpleName() + ") should return a command name and not a null value.");
    }
    return KeyNormalizer.create(commandName).equals(KeyNormalizer.create(operationName));
  }

  @Override
  public OperationStep createStep() {
    try {
      return this.getClass().getDeclaredConstructor().newInstance();
    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Set<String> getAcceptedCommandNames() {
    return Collections.singleton(getOperationName());
  }


  @Override
  public OperationStep setArguments(MapKeyIndependent<Object> arguments) {
    throw new UnsupportedOperationException("The operation `" + getOperationName() + "` (" + this.getClass().getSimpleName() + ") from the step (" + name + ") does not yet accept arguments from a map.");
  }

  @Override
  public String toString() {
    return name + " (" + getOperationName() + ")";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    StepAbs stepAbs = (StepAbs) o;
    return name.equals(stepAbs.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  public String getComment() {
    return comment;
  }

  @Override
  public Set<com.tabulify.conf.Attribute> getArguments() {
    return new HashSet<>(this.arguments.values());
  }

  @SuppressWarnings("unused")
  public StepAbs addArgumentsFromEnumAttributeClass(Class<? extends AttributeEnum> enumClass) {
    Arrays.asList(enumClass.getEnumConstants()).forEach(c -> this.addArgument(com.tabulify.conf.Attribute.create(c, com.tabulify.conf.Origin.RUNTIME)));
    return this;
  }

  private StepAbs addArgument(com.tabulify.conf.Attribute attribute) {
    this.arguments.put(attribute.getAttributeMetadata().toString(), attribute);
    return this;
  }

  protected com.tabulify.conf.Attribute getOrCreateArgument(AttributeEnum attribute) {
    try {
      return this.getArgument(attribute);
    } catch (NotFoundException e) {
      com.tabulify.conf.Attribute argument = com.tabulify.conf.Attribute.create(attribute, Origin.RUNTIME);
      this.addArgument(argument);
      return argument;
    }

  }

  private com.tabulify.conf.Attribute getArgument(AttributeEnum attribute) throws NotFoundException {
    com.tabulify.conf.Attribute variable = this.arguments.get(attribute.toString());
    if (variable == null) {
      throw new NotFoundException("The argument (" + attribute + ") was not found");
    }
    return variable;
  }
}
