package com.tabulify.type;

public class Floats {

  private final Doubles doubles;

  public Floats(Doubles doubles) {
    this.doubles = doubles;
  }

  public static Floats createFromObject(Object o) {

    return new Floats(Doubles.createFromObject(o));

  }


}
