package com.tabulify.conf;

import com.tabulify.Tabular;
import com.tabulify.Vault;
import com.tabulify.crypto.Protector;
import com.tabulify.type.KeyNormalizer;
import com.tabulify.type.MapKeyIndependent;

import java.util.Map;

import static com.tabulify.DbLoggers.LOGGER_DB_ENGINE;

/**
 * A class to hold all external envs
 */
public class TabularEnvs {

  private final MapKeyIndependent<String> sysEnv;
  private final MapKeyIndependent<String> javaProps;

  /**
   * @param initEnvs extra envs (mostly for test)
   */
  public TabularEnvs(Map<String, String> initEnvs, Protector protector) {

    // Sys
    // (used by developer to pass env at the command line)
    this.javaProps = new MapKeyIndependent<>();
    for (Map.Entry<Object, Object> sysProperty : System.getProperties().entrySet()) {
      String key = (String) sysProperty.getKey();
      String originalValue = (String) sysProperty.getValue();
      String decryptedValue = originalValue;
      if (originalValue.startsWith(Vault.VAULT_PREFIX)) {
        if (protector == null) {
          throw new RuntimeException("No passphrase was given, we can't decrypt the vault value (" + originalValue + ") of the java sys property (" + key + ")");
        }
        try {
          decryptedValue = protector.decrypt(originalValue);
        } catch (Exception exception) {
          String message = "We were unable to decrypt the value of the java sys property (" + key + ") with the given passphrase. Value:" + originalValue;
          throw new RuntimeException(message);
        }
      }
      try {
        this.javaProps.put(key, decryptedValue);
      } catch (Exception e) {
        // Happens when the key does not have any letter or digit
        LOGGER_DB_ENGINE.fine("The java system property " + key + " is not compliant as key. Error: " + e.getMessage());
      }
    }

    // Os
    this.sysEnv = new MapKeyIndependent<>();
    this.sysEnv.putAll(initEnvs);
    for (Map.Entry<String, String> osEnv : System.getenv().entrySet()) {
      String key = osEnv.getKey();
      if (key.equals("_")) {
        // this is the command being executed (ie /usr/bin/env for shell or /home/admin/.sdkman/candidates/java/11.0.26-tem/bin/java ofr java)
        // it results in no key so we skip it
        continue;
      }
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
      try {
        this.sysEnv.put(key, decryptedValue);
      } catch (Exception e) {
        // Happens when the key does not have any letter or digit
        LOGGER_DB_ENGINE.fine("The java system property " + key + " is not compliant as key. Error: " + e.getMessage());
      }
    }


  }

  public String getOsEnvValue(KeyNormalizer keyNormalize) {
    return this.sysEnv.get(keyNormalize);
  }


  /**
   * We don't look up without the tabli prefix because it can cause clashes
   * for instance, name in os is the name of the computer
   */
  public KeyNormalizer getNormalizedKey(AttributeEnum attribute) {
    return KeyNormalizer.createSafe(Tabular.TABUL_NAME + "_" + attribute);
  }

  public Map<String, String> getEnvs() {
    return this.sysEnv;
  }


  public String getJavaSysValue(KeyNormalizer keyNormalized) {
    return this.javaProps.get(keyNormalized);
  }

}
