package com.tabulify.template;

import com.tabulify.conf.Attribute;
import com.tabulify.spi.DataPath;
import com.tabulify.spi.Meta;
import com.tabulify.spi.MetaMap;
import com.tabulify.stream.SelectStream;
import net.bytle.exception.NoVariableException;
import net.bytle.type.KeyNormalizer;

import java.util.HashMap;
import java.util.Map;

import static com.tabulify.template.TemplatePrefix.*;

/**
 * A utility builder for a map of KeyNormalizer, Object value
 */
public class TemplateMetas {


  /**
   * Meta are organized by prefix
   */
  private final Map<KeyNormalizer, Meta> variablePrefixMetaMap = new HashMap<>();

  public static TemplateMetas builder() {
    return new TemplateMetas();
  }


  /**
   * Utility function to extract the stream column value
   *
   * @param selectStream - the stream
   */
  public TemplateMetas addSelectStream(SelectStream selectStream) {

    this.variablePrefixMetaMap.put(RECORD.toKeyNormalizer(), selectStream);
    return this;
  }

  /**
   * Utility function to extract the meta attributes
   *
   * @param meta   - the `meta` object
   * @param prefix - the prefix
   * @return a map of variables and their value
   */
  public TemplateMetas addMeta(Meta meta, KeyNormalizer prefix) {

    prefix = TemplatePrefix.normalization(prefix);
    this.variablePrefixMetaMap.put(prefix, meta);

    return this;

  }


  public TemplateMetas addInputDataPath(DataPath dataPath) {

    return addDataPath(dataPath, INPUT.toKeyNormalizer());
  }

  private TemplateMetas addDataPath(DataPath dataPath, KeyNormalizer keyNormalizer) {
    Meta meta = this.variablePrefixMetaMap.get(keyNormalizer);
    if (meta != null) {
      if (!(meta instanceof DataPath)) {
        throw new IllegalStateException("The " + keyNormalizer + " meta should be a DataPath, not a " + meta.getClass().getName());
      }
      if (meta.equals(dataPath)) {
        return this;
      }
      throw new IllegalStateException(keyNormalizer + " meta already set to " + meta + ". You can set it to " + dataPath);
    }
    return addMeta(dataPath, keyNormalizer);
  }

  public TemplateMetas addMeta(Meta meta, TemplatePrefix templatePrefix) {
    this.variablePrefixMetaMap.put(templatePrefix.toKeyNormalizer(), meta);
    return this;
  }

  public TemplateMetas addMetaMap(MetaMap metaMap, KeyNormalizer prefix) {

    return addMeta(metaMap, prefix);
  }


  public Map<KeyNormalizer, Meta> getVariablePrefixMetaMap() {
    return this.variablePrefixMetaMap;
  }

  public TemplateMetas addTargetDataPath(DataPath target) {
    return addDataPath(target, TARGET);
  }

  public Attribute getAttribute(TemplateVariable templateVariable) {
    try {
      KeyNormalizer prefix = templateVariable.getPrefix();
      if (prefix == null) {
        prefix = INPUT.toKeyNormalizer();
      }
      Meta meta = this.variablePrefixMetaMap
        .get(prefix);
      if (meta == null) {
        return null;
      }
      return meta
        .getAttribute(templateVariable.getVariableWithoutPrefix());
    } catch (NoVariableException e) {
      return null;
    }
  }

  public TemplateMetas addDataPath(DataPath dataPath, TemplatePrefix templatePrefix) {
    return addDataPath(dataPath, templatePrefix.toKeyNormalizer());
  }

}
