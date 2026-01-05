package com.tabulify;

import com.tabulify.conf.*;
import com.tabulify.crypto.CryptoSymmetricCipher;
import com.tabulify.crypto.Protector;
import com.tabulify.exception.CastException;
import com.tabulify.template.TextTemplate;
import com.tabulify.template.TextTemplateEngine;
import com.tabulify.type.Casts;
import com.tabulify.type.KeyNormalizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

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
     * We use tvault and not vault to not confound with hashicorp vault
     */
    public static final String VAULT_PREFIX = "tvault";
    private final Map<String, Object> templatingEnvs;
    private final Protector protector;


    public Vault(Protector protector, TabularEnvs tabularEnvs) {

        this.protector = protector;

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
     * @param protector           - the protector
     * @param templatingVariables Free variable used in templating to create clear value from variable
     */
    public static Vault create(Protector protector, TabularEnvs templatingVariables) {
        return new Vault(protector, templatingVariables);
    }

    /**
     * @param protector - the protector
     */
    public static Vault create(Protector protector) {
        return new Vault(protector, null);
    }

    /**
     * Used mostly in test as a vault without passphrase
     * and env is not really real
     */
    public static Vault create() {
        return new Vault(null, null);
    }


    /**
     * @param key -  a string, not a key normalizer because we may get external attributes
     */
    public com.tabulify.conf.Attribute createAttribute(String key, Object value, com.tabulify.conf.Origin origin) throws Exception {

        return createVariableBuilderFromName(key)
                .setOrigin(origin)
                .build(value);

    }

    /**
     * @param key -  a string, not a key normalizer because we may get external attributes
     */
    public VariableBuilder createVariableBuilderFromName(String key) {

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
     * @param value - the value to encrypt if possible (passphrase provided)
     * @return the encrypted value or non ecnrytped if no passphrase
     */
    public String encryptIfPossible(String value) {
        if (protector != null && !value.startsWith(Vault.VAULT_PREFIX)) {
            return encrypt(value);
        }
        return value;
    }

    public Attribute createAttribute(KeyNormalizer key, Object value, Origin origin) throws Exception {
        return createAttribute(key.toKebabCase(), value, origin);
    }

    /**
     * Helper to build a variable
     */
    public class VariableBuilder {

        private AttributeEnum attributeEnum;
        private com.tabulify.conf.Origin origin;
        private String name;
        private Function<Object, ?> castLambda;
        private RawValueType rawValueType = RawValueType.OTHER;


        /**
         * @param name - a string, not a key normalizer because we may get external keys (such as Jdbc Properties)
         */
        public VariableBuilder(String name) {
            this.name = name;
        }

        public VariableBuilder(AttributeEnum attributeEnum) {
            this.attributeEnum = attributeEnum;
        }

        public VariableBuilder setOrigin(Origin origin) {
            this.origin = origin;
            return this;
        }

        public com.tabulify.conf.Attribute build(Supplier<?> valueProvider, Class<?> valueClazz) {
            com.tabulify.conf.Attribute attribute = builtAttribute(valueClazz);
            attribute.setValueProvider(valueProvider);
            return attribute;
        }

        /**
         * Env/Command line argument are clear value
         * without any raw/original value
         * We don't want to see any clear value
         */
        public com.tabulify.conf.Attribute build(Object value) throws CastException {

            return build(value, null);

        }

        private Object toPlain(Object value) throws CastException {

            if ((value instanceof String)) {
                return this.rawToPlain((String) value);
            }

            if ((value instanceof List<?> valueList)) {
                if (valueList.isEmpty()) {
                    return value;
                }
                List<String> valueListString = new ArrayList<>();
                for (Object object : valueList) {
                    if (object.getClass() != String.class) {
                        return value;
                    }
                    valueListString.add(this.rawToPlain((String) object));
                }
                return valueListString;
            }

            if ((value instanceof Map<?, ?> valueMap)) {
                if (valueMap.isEmpty()) {
                    return value;
                }
                Map<Object, String> valueMapString = new HashMap<>();
                for (Map.Entry<?, ?> object : valueMap.entrySet()) {
                    if (object.getValue().getClass() != String.class) {
                        return value;
                    }
                    valueMapString.put(object.getKey(), rawToPlain((String) object.getValue()));
                }
                return valueMapString;
            }

            return value;

        }

        private String rawToPlain(String valueString) throws CastException {

            if (valueString == null) {
                return null;
            }

            if (valueString.startsWith(Vault.VAULT_PREFIX)) {

                this.rawValueType = RawValueType.VAULT;

                /*
                 * Decrypt
                 */
                String valueToDecrypt = valueString.substring(Vault.VAULT_PREFIX.length());
                if (protector == null) {
                    throw new CastException("No passphrase was given, we can't decrypt the vault value (" + valueToDecrypt + ")");
                }
                try {
                    return protector.decrypt(valueToDecrypt);
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
                return valueString;
            }
            if (!textTemplate.getVariableNames().isEmpty() && templatingEnvs == null) {
                // should happen only in test
                throw new RuntimeException("The templating envs are null but the value (" + valueString + ") has a variable");
            }
            this.rawValueType = RawValueType.VARIABLE;
            return textTemplate
                    .applyVariables(templatingEnvs)
                    .getResult();
        }

        private com.tabulify.conf.Attribute builtAttribute(Class<?> valueClazz) {

            if (this.attributeEnum != null) {
                return Attribute.create(attributeEnum, origin);
            }
            if (name == null) {
                throw new RuntimeException("Name or Enum cannot be null together. A variable needs an identifier");
            }

            return Attribute.createWithClassAndDefault(KeyNormalizer.createSafe(name), origin, valueClazz, null);

        }

        public com.tabulify.conf.Attribute buildSafe(Object value) {
            try {
                return build(value);
            } catch (CastException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }


        public Attribute build(Object value, Function<Object, ?> castFunction) throws CastException {

            Class<?> valueClazz = String.class;
            if (value != null) {
                valueClazz = value.getClass();
            }
            com.tabulify.conf.Attribute attribute = builtAttribute(valueClazz);

            // Value may be null
            if (value == null) {
                return attribute;
            }

            // The raw (String, integer, ...)
            attribute.setRawValue(value);

            // Decode all string
            Object valueDecoded = toPlain(value);
            // setRawValueType should happen after the toPlain function
            // as this is a recursive function that set the raw value type
            attribute.setRawValueType(this.rawValueType);

            attribute.setPlainValue(valueDecoded, castFunction);

            return attribute;
        }
    }


}
