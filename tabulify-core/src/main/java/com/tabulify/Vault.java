package com.tabulify;

import com.tabulify.conf.AttributeEnum;
import com.tabulify.conf.Origin;
import com.tabulify.conf.TabularEnvs;
import net.bytle.crypto.CryptoSymmetricCipher;
import net.bytle.crypto.Protector;
import net.bytle.exception.CastException;
import net.bytle.template.TextTemplate;
import net.bytle.template.TextTemplateEngine;
import net.bytle.type.Casts;

import java.util.Map;

/**
 * An object that instantiate {@link com.tabulify.conf.Attribute}
 * to:
 * - make them secure
 * - conserve the original value in order to write them on disk
 */
public class Vault {

  /**
   * This is a passphrase used to encrypt the sample database password
   * Don't change this value
   */
  public static final String VAULT_PREFIX = "vault";
  private final Map<String, Object> templatingEnvs;
  private Protector protector;


  public Vault(String passphrase, TabularEnvs tabularEnvs) {

    if (passphrase != null) {
      this.protector = Protector.create(passphrase);
    }

    if (tabularEnvs != null) {
      try {
        templatingEnvs = Casts.castToSameMap(tabularEnvs.getEnvs(), String.class, Object.class);
      } catch (CastException e) {
        throw new RuntimeException("Should not happen as we go to object value", e);
      }
    } else {
      templatingEnvs = null;
    }

  }

  /**
   * @param passphrase          - the passphrase
   * @param templatingVariables Free variable used in templating to create clear value from variable
   */
  public static Vault create(String passphrase, TabularEnvs templatingVariables) {
    return new Vault(passphrase, templatingVariables);
  }

  /**
   * @param passphrase - the passphrase
   */
  public static Vault create(String passphrase) {
    return new Vault(passphrase, null);
  }

  /**
   * Used mostly in test as a vault without passphrase
   * and env is not really real
   */
  public static Vault create() {
    return new Vault(null, null);
  }


  public com.tabulify.conf.Attribute createAttribute(String key, Object value, com.tabulify.conf.Origin origin) throws Exception {

    return createVariableBuilderFromName(key)
      .setOrigin(origin)
      .build(value);

  }

  private VariableBuilder createVariableBuilderFromName(String key) {

    return new VariableBuilder(key);

  }


  public String encrypt(String s) {
    if (protector == null) {
      throw new RuntimeException("No passphrase was given, vault can't encrypt");
    }
    return VAULT_PREFIX + protector.encrypt(CryptoSymmetricCipher.AES_CBC_PKCS5PADDING, s);
  }


  public VariableBuilder createVariableBuilderFromAttribute(AttributeEnum attribute) {
    return new VariableBuilder(attribute);
  }

  public com.tabulify.conf.Attribute createAttribute(AttributeEnum attribute, Object value, com.tabulify.conf.Origin origin) {
    return createVariableBuilderFromAttribute(attribute)
      .setOrigin(origin)
      .buildSafe(value);
  }

  /**
   * Helper to build a variable
   */
  public class VariableBuilder {

    private AttributeEnum attribute;
    private com.tabulify.conf.Origin origin;
    private String name;

    public VariableBuilder(String name) {
      this.name = name;
    }

    public VariableBuilder(AttributeEnum attribute) {
      this.attribute = attribute;
    }

    public VariableBuilder setOrigin(Origin origin) {
      this.origin = origin;
      return this;
    }

    /**
     * Env/Command line argument are clear value
     * without any raw/original value
     * We don't want to see any clear value
     */
    public com.tabulify.conf.Attribute build(Object value) throws CastException {

      if (this.attribute == null) {
        if (name == null) {
          throw new RuntimeException("Name and attribute cannot be null together.  A variable needs an identifiant");
        }
        this.attribute = new AttributeEnum() {

          @Override
          public String getDescription() {
            return name;
          }

          @Override
          public Class<?> getValueClazz() {
            return value.getClass();
          }

          @Override
          public Object getDefaultValue() {
            return null;
          }

          /**
           * @return the unique string identifier (mandatory)
           */
          @Override
          public String toString() {
            return name;
          }

        };
      }
      com.tabulify.conf.Attribute attribute = com.tabulify.conf.Attribute.create(this.attribute, origin);

      // Value may be null
      if (value == null) {
        return attribute;
      }


      if (!(value instanceof String)) {
        return attribute.setPlainValue(value);
      }

      String valueString = value.toString();
      attribute.setRawValue(valueString);

      if (valueString.startsWith(Vault.VAULT_PREFIX)) {

        /**
         * Decrypt
         */
        String valueToDecrypt = valueString.substring(Vault.VAULT_PREFIX.length());
        if (protector == null) {
          throw new CastException("No passphrase was given, we can't decrypt the vault value (" + valueToDecrypt + ")");
        }
        try {
          String decrypt = protector.decrypt(valueToDecrypt);
          return attribute.setPlainValue(decrypt);
        } catch (Exception exception) {
          String message = "We were unable to decrypt the value with the given passphrase. Value:" + valueToDecrypt;
          DbLoggers.LOGGER_DB_ENGINE.severe(message);
          throw new CastException(message);
        }

      }

      /*
       * Template processing if the value has an env variable
       */
      TextTemplate textTemplate = TextTemplateEngine
        .getOrCreate()
        .compile(valueString);
      if (textTemplate.getVariableNames().isEmpty()) {
        return attribute.setPlainValue(valueString);
      }
      if (!textTemplate.getVariableNames().isEmpty() && templatingEnvs == null) {
        // should happen only in test
        throw new RuntimeException("The templating envs are null but the value (" + valueString + ") has a variable");
      }
      String clearValue = textTemplate
        .applyVariables(templatingEnvs)
        .getResult();
      return attribute.setPlainValue(clearValue);
    }

    public VariableBuilder setName(String name) {
      this.name = name;
      return this;
    }

    public com.tabulify.conf.Attribute buildSafe(Object value) {
      try {
        return build(value);
      } catch (CastException e) {
        throw new RuntimeException(e.getMessage(), e);
      }
    }
  }
}
