package com.tabulify.model;

import net.bytle.type.KeyInterface;

import java.sql.SQLType;

/**
 * An interface for type definition
 * that adds the {@link KeyInterface}
 * It may be used normally in an enum
 */
public interface SqlDataTypeKeyInterface extends KeyInterface, SQLType {

}
