package net.bytle.db.yaml;

import net.bytle.type.Attribute;

enum YamDataPathAttribute implements Attribute {

  STRUCTURE("The structure of the returned tabular", YamlStructure.class, YamlStructure.DOCUMENT),
  OUTPUT_STYLE("In which Yaml format a document is returned", YamlStyle.class, YamlStyle.JSON);


  private final String description;
  private final Class<?> clazz;
  private final Object defaultValue;

  YamDataPathAttribute(String description, Class<?> clazz, Object defaultValue) {
    this.description = description;
    this.clazz = clazz;
    this.defaultValue = defaultValue;
  }

  @Override
  public String getDescription() {
    return this.description;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }


}
