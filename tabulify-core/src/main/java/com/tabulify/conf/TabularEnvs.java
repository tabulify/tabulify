package com.tabulify.conf;

import com.tabulify.Tabular;
import net.bytle.type.Attribute;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MapKeyIndependent;

import java.util.Map;

/**
 * A class to hold all external envs
 */
public class TabularEnvs {

  private final MapKeyIndependent<String> sysEnv;

  private final MapKeyIndependent<String> allIn;

  /**
   * @param initEnvs extra envs (mostly for test)
   */
  public TabularEnvs(Map<String, String> initEnvs) {

    // Not yet used
    // this.sysProperties = MapKeyIndependent.createFrom(System.getProperties(), String.class);
    this.sysEnv = MapKeyIndependent.createFrom(System.getenv(), String.class);
    this.sysEnv.putAll(initEnvs);

    // Cpu economy ...
    this.allIn = new MapKeyIndependent<>();
    this.allIn.putAll(sysEnv);

  }

  public String getOsEnvValue(KeyNormalizer keyNormalize) {
    return this.sysEnv.get(keyNormalize);
  }

  public KeyNormalizer getOsTabliEnvName(Attribute attribute) {
    return KeyNormalizer.create(Tabular.TABLI_NAME + "_" + attribute);
  }

  public Map<String, String> getEnvs() {
    return this.allIn;
  }


}
