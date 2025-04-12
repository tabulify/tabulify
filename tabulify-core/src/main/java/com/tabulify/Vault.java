package com.tabulify;

import net.bytle.crypto.CryptoSymmetricCipher;
import net.bytle.crypto.Protector;
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
  private final Protector protector;
  private final Map<String, Object> tabularEnvVariables;

  public Vault(Tabular tabular, String passphrase) {
    /**
     * If passphrase is null, the vault protect with a default passphrase
     */
    this.protector = Protector.create(passphrase);


    this.tabularEnvVariables = tabular.getEnvVariables().getVariablesAsKeyIndependentMap();

  }

  public static Vault create(Tabular tabular, String passphrase) {
    return new Vault(tabular, passphrase);
  }


  public Variable createVariable(String key, Object value) throws Exception {

    Variable variable = Variable.createWithClass(key, Origin.INTERNAL,  value.getClass());
    this.setValue(variable, value);
    return variable;

  }



  private void setValue(Variable variable, Object value) throws Exception {

    variable.setOriginalValue(value);

    if (value instanceof String) {

      String valueString = value.toString();
      if (valueString.startsWith(Vault.VAULT_PREFIX)) {

        /**
         * Decrypt
         */
        String valueToDecrypt = valueString.substring(Vault.VAULT_PREFIX.length());
        try {
          variable.setClearValue(protector.decrypt(valueToDecrypt));
        } catch (Exception exception) {
          try {
            /**
             * The passphrase has been set for the first time,
             * the password  may be encrypted with the default passphrase
             */
            variable.setClearValue(protector.decryptWithDefault(valueToDecrypt));
          } catch (Exception exceptionHowTo) {
            String message = "We were unable to decrypt the value with the given passphrase. Value:" + valueToDecrypt;
            DbLoggers.LOGGER_DB_ENGINE.severe(message);
            throw new Exception(message);
          }
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
      variable.setProcessedValue(clearValue);

    }

  }


  public Variable createVariable(Attribute attribute, Object value) throws Exception {

    Variable variable = Variable.create(attribute, Origin.INTERNAL);
    this.setValue(variable, value);
    return variable;

  }

  public String encrypt(String s) {
    return  VAULT_PREFIX + protector.encrypt(CryptoSymmetricCipher.AES_CBC_PKCS5PADDING, s);
  }

}
