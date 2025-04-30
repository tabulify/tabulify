package com.tabulify.conf;

import com.tabulify.Tabular;
import com.tabulify.TabularAttribute;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MapKeyIndependent;

import java.util.Map;

/**
 * A class to hold all external envs
 */
public class TabularEnvs {

  private final MapKeyIndependent<String> sysEnv;
  private final MapKeyIndependent<String> sysProperties;
  private final MapKeyIndependent<String> allIn;

  /**
   * @param initEnvs extra envs (mostly for test)
   */
  public TabularEnvs(Map<String, String> initEnvs) {

    this.sysProperties = MapKeyIndependent.createFrom(System.getProperties(), String.class);
    this.sysEnv = MapKeyIndependent.createFrom(System.getenv(), String.class);
    this.sysEnv.putAll(initEnvs);

    // Cpu economy ...
    this.allIn = new MapKeyIndependent<>();
    this.allIn.putAll(sysEnv);
    this.allIn.putAll(sysProperties);

  }

  public String getOsEnvValue(KeyNormalizer keyNormalize) {
    return this.sysEnv.get(keyNormalize);
  }

  public KeyNormalizer getOsTabliEnvName(TabularAttribute tabularAttributes) {
    return KeyNormalizer.create(Tabular.TABLI_NAME + "_" + tabularAttributes);
  }

  public Map<String, String> getEnvs() {
    return this.allIn;
  }


}
