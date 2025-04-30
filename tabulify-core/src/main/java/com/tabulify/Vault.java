package com.tabulify;

import com.tabulify.conf.TabularEnvs;
import com.tabulify.connection.ConnectionAttribute;
import net.bytle.crypto.CryptoSymmetricCipher;
import net.bytle.crypto.Protector;
import net.bytle.exception.CastException;
import net.bytle.template.TextTemplate;
import net.bytle.template.TextTemplateEngine;
import net.bytle.type.Attribute;
import net.bytle.type.Casts;
import net.bytle.type.Origin;
import net.bytle.type.Variable;

import java.util.Arrays;
import java.util.Map;

/**
 * An object that instantiate {@link Variable}
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


  public Variable createVariableWithRawValue(String key, Object value, Origin origin) throws Exception {

    Variable variable = Variable.createWithClass(key, origin, value.getClass());
    this.setRawValue(variable, value);
    return variable;

  }


  private void setRawValue(Variable variable, Object value) throws CastException {

    variable.setOriginalValue(value);

    if (!(value instanceof String)) {
      variable.setClearValue(value);
      return;
    }

    String valueString = value.toString();
    if (valueString.startsWith(Vault.VAULT_PREFIX)) {

      /**
       * Decrypt
       */
      String valueToDecrypt = valueString.substring(Vault.VAULT_PREFIX.length());
      if (protector == null) {
        throw new CastException("No passphrase was given, we can't decrypt the vault value (" + valueToDecrypt + ")");
      }
      try {
        variable.setClearValue(protector.decrypt(valueToDecrypt));
      } catch (Exception exception) {
        String message = "We were unable to decrypt the value with the given passphrase. Value:" + valueToDecrypt;
        DbLoggers.LOGGER_DB_ENGINE.severe(message);
        throw new CastException(message);
      }
      // not a template
      return;
    }


    /*
     * Template processing if the value has an env variable
     */
    TextTemplate textTemplate = TextTemplateEngine
      .getOrCreate()
      .compile(valueString);
    if (!textTemplate.getVariableNames().isEmpty() && templatingEnvs == null) {
      // should happen only in test
      throw new RuntimeException("The templating envs are null but the value (" + valueString + ") has a variable");
    }
    String clearValue = textTemplate
      .applyVariables(templatingEnvs)
      .getResult();
    variable.setClearValue(clearValue);


  }


  public Variable createVariableWithRawValue(Attribute attribute, Object value, Origin origin) throws CastException {

    Variable variable = Variable.create(attribute, origin);
    this.setRawValue(variable, value);
    return variable;

  }


  public String encrypt(String s) {
    if (protector == null) {
      throw new RuntimeException("No passphrase was given, vault can't encrypt");
    }
    return VAULT_PREFIX + protector.encrypt(CryptoSymmetricCipher.AES_CBC_PKCS5PADDING, s);
  }

  public Variable createVariableSafe(ConnectionAttribute attribute, Object value, Origin origin) {
    try {
      return createVariableWithRawValue(attribute, value, origin);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public ConfVariable confVariable(TabularAttribute tabularAttributes) {
    return new ConfVariable(tabularAttributes);
  }

  public Variable createVariableWithClearValue(Attribute connectionAttribute, String secret, Origin origin) {
    Variable variable = Variable.create(connectionAttribute, origin);
    variable.setClearValue(secret);
    return variable;
  }

  /**
   * Helper to build a variable
   */
  public class ConfVariable {
    private final Attribute attribute;
    private Origin origin;

    public ConfVariable(Attribute attribute) {
      this.attribute = attribute;
    }

    public ConfVariable setOrigin(Origin origin) {
      this.origin = origin;
      return this;
    }

    /**
     * build from a conf (ie not an env)
     */
    public Variable buildFromRawValue(String value) {
      Variable variable = Variable.create(attribute, origin);
      if (Arrays.asList(Origin.COMMAND_LINE, Origin.OS, Origin.SYS).contains(origin)) {
        throw new RuntimeException("A value from a command line, env or sys is considered a clear value");
      }
      try {
        setRawValue(variable, value);
      } catch (CastException e) {
        throw new RuntimeException("Error while reading the variable value " + attribute + " from " + origin + ". Error: " + e.getMessage(), e);
      }
      return variable;
    }

    /**
     * Env/Command line argument are clear value
     * without any raw/original value
     * We don't want to see any clear value
     */
    public Variable buildFromClearValue(String value) {
      Variable variable = Variable.create(attribute, origin);
      variable.setClearValue(value);
      return variable;
    }
  }
}
