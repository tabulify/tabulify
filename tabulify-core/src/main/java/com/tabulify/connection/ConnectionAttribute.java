package com.tabulify.connection;

import com.tabulify.conf.AttributeEnum;
import com.tabulify.conf.AttributeParameter;

/**
 * An interface to no mix common {@link AttributeEnum}
 * with Connection attribute. They should be enum and not created dynamically
 * but with {@link net.bytle.type.Casts#cast(Object, Class)} from the enum class
 */
public interface ConnectionAttribute extends AttributeParameter {
}
