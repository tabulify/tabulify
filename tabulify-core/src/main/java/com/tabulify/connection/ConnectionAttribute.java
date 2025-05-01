package com.tabulify.connection;

import net.bytle.type.Attribute;

/**
 * An interface to no mix common {@link Attribute}
 * with Connection attribute. They should be enum and not created dynamically
 * but with {@link net.bytle.type.Casts#cast(Object, Class)} from the enum class
 */
public interface ConnectionAttribute extends Attribute {
}
