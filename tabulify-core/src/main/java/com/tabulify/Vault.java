package com.tabulify;

import com.tabulify.connection.ConnectionAttribute;
import net.bytle.crypto.CryptoSymmetricCipher;
import net.bytle.crypto.Protector;
import net.bytle.template.TextTemplate;
import net.bytle.template.TextTemplateEngine;
import net.bytle.type.Attribute;
import net.bytle.type.Origin;
import net.bytle.type.Variable;

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
  private Protector protector;
  private final Map<String, Object> tabularEnvVariables;

  public Vault(Tabular tabular, String passphrase) {

    if (passphrase != null) {
      this.protector = Protector.create(passphrase);
    }


    this.tabularEnvVariables = tabular.getEnvVariables().getVariablesAsKeyIndependentMap();

  }

  public static Vault create(Tabular tabular, String passphrase) {
    return new Vault(tabular, passphrase);
  }


  public Variable createVariable(String key, Object value, Origin origin) throws Exception {

    Variable variable = Variable.createWithClass(key, origin, value.getClass());
    this.setValue(variable, value);
    return variable;

  }


  private void setValue(Variable variable, Object value) throws Exception {

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
        throw new Exception("No passphrase was given, we can't decrypt the vault value (" + valueToDecrypt + ")");
      }
      try {
        variable.setClearValue(protector.decrypt(valueToDecrypt));
      } catch (Exception exception) {
        String message = "We were unable to decrypt the value with the given passphrase. Value:" + valueToDecrypt;
        DbLoggers.LOGGER_DB_ENGINE.severe(message);
        throw new Exception(message);
      }
      // not a template
      return;
    }


    /*
     * Template processing if the value has a variable
     */
    String clearValue = TextTemplateEngine
      .getOrCreate()
      .compile(valueString)
      .applyVariables(tabularEnvVariables)
      .getResult();
    variable.setClearValue(clearValue);


  }


  public Variable createVariable(Attribute attribute, Object value, Origin origin) throws Exception {

    Variable variable = Variable.create(attribute, origin);
    this.setValue(variable, value);
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
      return createVariable(attribute,value,origin);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
