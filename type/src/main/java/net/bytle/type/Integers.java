package net.bytle.type;

public class Integers {


  public static Double toDouble(Object o) {
    if (o instanceof Integer){
      o = ((Integer) o).doubleValue();
    }

    return (Double) o;
  }

  public static Integer toInteger(Object o) {
    if (o instanceof Double) {
      o = ((Double) o).intValue();
    }
    return (Integer) o;
  }
}
