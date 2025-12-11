package com.tabulify.connection;

import com.tabulify.conf.AttributeEnum;
import com.tabulify.conf.AttributeEnumParameter;

/**
 * An interface to no mix common {@link AttributeEnum}
 * with Connection attribute. They should be enum and not created dynamically
 * but with {@link com.tabulify.type.Casts#cast(Object, Class)} from the enum class
 */
public interface ConnectionAttributeEnum extends AttributeEnumParameter {
}
