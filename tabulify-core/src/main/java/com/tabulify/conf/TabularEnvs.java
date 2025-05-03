package com.tabulify.conf;

import com.tabulify.DbLoggers;
import com.tabulify.Tabular;
import com.tabulify.Vault;
import net.bytle.crypto.Protector;
import net.bytle.exception.CastException;
import net.bytle.type.KeyNormalizer;
import net.bytle.type.MapKeyIndependent;

import java.util.Map;

/**
 * A class to hold all external envs
 */
public class TabularEnvs {

  private final MapKeyIndependent<String> sysEnv;

  /**
   * @param initEnvs extra envs (mostly for test)
   */
  public TabularEnvs(Map<String, String> initEnvs, Protector protector) {

    // Os
    this.sysEnv = new MapKeyIndependent<>();
    this.sysEnv.putAll(initEnvs);
    for (Map.Entry<String, String> osEnv : System.getenv().entrySet()) {
      String key = osEnv.getKey();
      String originalValue = osEnv.getValue();
      String decryptedValue = originalValue;
      if (originalValue.startsWith(Vault.VAULT_PREFIX)) {
        if (protector == null) {
          throw new RuntimeException("No passphrase was given, we can't decrypt the vault value (" + originalValue + ") of the os environment variable (" + key + ")");
        }
        try {
          decryptedValue = protector.decrypt(originalValue);
        } catch (Exception exception) {
          String message = "We were unable to decrypt the value of the os environment variable (" + key + ") with the given passphrase. Value:" + originalValue;
          throw new RuntimeException(message);
        }
      }
      this.sysEnv.put(key, decryptedValue);
    }


  }

  public String getOsEnvValue(KeyNormalizer keyNormalize) {
    return this.sysEnv.get(keyNormalize);
  }

  public KeyNormalizer getOsTabliEnvName(AttributeEnum attribute) {
    return KeyNormalizer.create(Tabular.TABLI_NAME + "_" + attribute);
  }

  public Map<String, String> getEnvs() {
    return this.sysEnv;
  }


}
