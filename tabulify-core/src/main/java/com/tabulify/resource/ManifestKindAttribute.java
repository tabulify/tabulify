package com.tabulify.resource;

import com.tabulify.conf.AttributeEnum;
import com.tabulify.uri.DataUriStringNode;

import java.util.HashMap;
import java.util.Map;

/**
 * Using kind as media type, the media type is no more needed
 */
public enum ManifestKindAttribute implements AttributeEnum {

  DATA_URI("The data uri of the resource", null, DataUriStringNode.class),
  DATA_DEF("The media type of the resource", new HashMap<>(), Map.class);

  private final String desc;
  private final Class<?> clazz;
  private final Object defaultValue;

  ManifestKindAttribute(String s, Object o, Class<?> valueClazz) {
    this.desc = s;
    this.defaultValue = o;
    this.clazz = valueClazz;
  }

  @Override
  public String getDescription() {
    return this.desc;
  }

  @Override
  public Object getDefaultValue() {
    return this.defaultValue;
  }

  @Override
  public Class<?> getValueClazz() {
    return this.clazz;
  }

}
