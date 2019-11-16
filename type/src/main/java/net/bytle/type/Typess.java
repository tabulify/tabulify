package net.bytle.type;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Typess {

    @SuppressWarnings("unchecked")
    public static <T> T safeCast(Object object, Class<T> clazz) {
        if (object.getClass().equals(clazz)){
            return (T) object;
        } else {
            throw new RuntimeException("The class of the object is " + object.getClass() + " and not" + clazz);
        }
    }


}
