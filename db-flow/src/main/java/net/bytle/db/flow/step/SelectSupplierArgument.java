package net.bytle.db.flow.step;

import net.bytle.type.Attribute;

public enum SelectSupplierArgument implements Attribute {


  DATA_SELECTOR("A selector", null, String.class),
  WITH_DEPENDENCIES("Add the dependencies to the selection", false, Boolean.class),
  STRICT("Fail if no data resources are selected when strict", false, Boolean.class),
  ATTRIBUTES("The data definition attributes of the data path", null, String.class),
  TYPE("The media type of the selected resources", null, String.class),
  LOGICAL_NAME("The logical name in a pattern format of the selected resources", null, String.class),
  DATA_DEFINITION("A extra inline data definition over the selection", null, null)
  ;


  private final Object defaultValue;
  private final String description;
  private final Class<?> clazz;

  SelectSupplierArgument(String description, Object defaultValue, Class<?> clazz) {

    this.description = description;
    this.defaultValue = defaultValue;
    this.clazz = clazz;

  }


  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
  }

  public Object getDefaultValue() {
    return this.defaultValue;
  }

}
