package com.tabulify.conf;

/**
 * Type of the raw value
 */
public enum RawValueType implements AttributeValue {

    VARIABLE("An os environment variable"),
    VAULT("An encrypted variable"),
    OTHER("Not a secret");

    private final String description;

    RawValueType(String description) {

        this.description = description;

    }

    @Override
    public String getDescription() {
        return this.description;
    }

}
