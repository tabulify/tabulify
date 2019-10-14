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

    /**
     * @param value - the input double value
     * @param scale - the scale (the number of decimal after the comma)
     * @return a double - half up rounded to the scale
     * TODO: The test utility of Sqlite use a printFormat for double
     */
    public static double round(double value, int scale) {
        if (scale < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(scale, RoundingMode.HALF_UP);
        return bd.doubleValue();

    }

}
