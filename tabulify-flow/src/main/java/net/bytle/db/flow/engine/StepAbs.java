package net.bytle.db.flow.engine;

import net.bytle.db.Tabular;
import net.bytle.exception.NotFoundException;
import net.bytle.type.*;

import java.util.*;

public abstract class StepAbs extends StepProvider implements OperationStep {


  protected Tabular tabular;
  protected String name;
  private String comment;

  private final MapKeyIndependent<Variable> arguments = new MapKeyIndependent<>();


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
    return Key.toNormalizedKey(commandName).equals(Key.toNormalizedKey(operationName));
  }

  @Override
  public OperationStep createStep() {
    try {
      return this.getClass().newInstance();
    } catch (InstantiationException | IllegalAccessException e) {
      throw new RuntimeException();
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
  public Set<Variable> getArguments() {
    return new HashSet<>(this.arguments.values());
  }

  public StepAbs addArgumentsFromEnumAttributeClass(Class<? extends Attribute> enumClass) {
    Arrays.asList(enumClass.getEnumConstants()).forEach(c -> this.addArgument(Variable.create(c,Origin.INTERNAL)));
    return this;
  }

  private StepAbs addArgument(Variable variable) {
    this.arguments.put(variable.getUniqueName(), variable);
    return this;
  }

  protected Variable getOrCreateArgument(Attribute attribute) {
    try {
      return this.getArgument(attribute);
    } catch (NotFoundException e) {
      Variable argument = Variable.create(attribute, Origin.INTERNAL);
      this.addArgument(argument);
      return argument;
    }

  }

  private Variable getArgument(Attribute attribute) throws NotFoundException {
    Variable variable = this.arguments.get(attribute.toString());
    if (variable == null) {
      throw new NotFoundException("The argument (" + attribute + ") was not found");
    }
    return variable;
  }
}
