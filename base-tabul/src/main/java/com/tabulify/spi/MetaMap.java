package com.tabulify.spi;

import com.tabulify.Tabular;
import com.tabulify.conf.Attribute;
import com.tabulify.conf.Origin;
import com.tabulify.exception.InternalException;
import com.tabulify.exception.NoVariableException;
import com.tabulify.type.KeyNormalizer;

import java.util.*;

/**
 * A meta implementation around a map
 */
public class MetaMap implements Meta, Map<KeyNormalizer, Object> {
  private final Tabular tabular;
  private final String name;
  Map<KeyNormalizer, Object> variableNameValueMap = new HashMap<>();
  private final Map<KeyNormalizer, Attribute> attributes = new HashMap<>();

  public MetaMap(Tabular tabular) {
    this(tabular, "anonymous");
  }


  public MetaMap(Tabular tabular, String anonymous) {
    this.tabular = tabular;
    this.name = anonymous;
  }


  @Override
  public Attribute getAttribute(KeyNormalizer name) throws NoVariableException {
    return attributes.get(name);
  }

  @Override
  public Set<Attribute> getAttributes() {
    return new HashSet<>(attributes.values());
  }

  @Override
  public DataPath toAttributesDataPath() {
    DataPath dataPath = this.tabular.getMemoryConnection()
      .getDataPath(this.name + "-meta-map");
    return toAttributesDataPath(dataPath);
  }

  @Override
  public int size() {
    return variableNameValueMap.size();
  }

  @Override
  public boolean isEmpty() {
    return variableNameValueMap.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return variableNameValueMap.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return variableNameValueMap.containsValue(value);
  }

  @Override
  public Object get(Object key) {
    return variableNameValueMap.get(key);
  }

  @Override
  public Object put(KeyNormalizer key, Object value) {
    try {
      com.tabulify.conf.Attribute attribute = this.tabular.getVault().createAttribute(key, value, Origin.DEFAULT);
      this.attributes.put(key, attribute);
    } catch (Exception e) {
      throw new InternalException("Error while creating a variable from an attribute (" + key + ") for the resource (" + this + "). Error: " + e.getMessage(), e);
    }
    return variableNameValueMap.put(key, value);
  }

  @Override
  public Object remove(Object key) {
    this.attributes.remove(key);
    return variableNameValueMap.remove(key);
  }

  @Override
  public void putAll(Map<? extends KeyNormalizer, ?> m) {
    for (Map.Entry<? extends KeyNormalizer, ?> entry : m.entrySet()) {
      put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    attributes.clear();
    variableNameValueMap.clear();
  }

  @Override
  public Set<KeyNormalizer> keySet() {
    return variableNameValueMap.keySet();
  }

  @Override
  public Collection<Object> values() {
    return variableNameValueMap.values();
  }

  @Override
  public Set<Entry<KeyNormalizer, Object>> entrySet() {
    return variableNameValueMap.entrySet();
  }
}
