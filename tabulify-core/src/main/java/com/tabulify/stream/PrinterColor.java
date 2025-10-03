package com.tabulify.stream;

import net.bytle.exception.CastException;

import java.util.Arrays;
import java.util.stream.Collectors;

public enum PrinterColor {

  RED("r", "\u001B[31m"),
  GREEN("g", "\u001B[32m"),
  BLUE("b", "\u001B[34m");

  public static final String COLOR_SEPARATOR = ",";
  private final String letterColor;

  public static PrinterColor cast(String colorPart) throws CastException {
    for (PrinterColor printerColor : PrinterColor.values()) {
      if (printerColor.getLetterColor().equals(colorPart.toLowerCase())) {
        return printerColor;
      }
    }
    throw new CastException("The color letter " + colorPart + " is unknown. We were expecting " +
      Arrays.stream(PrinterColor.values())
        .map(PrinterColor::getLetterColor)
        .collect(Collectors.joining(", ")));
  }

  public static String addColorIfNotNull(String string, PrinterColor printerColor) {

    if (printerColor == null) {
      return string;
    }
    return printerColor.getAnsiColor() + string + "\u001B[0m";

  }

  public String getAnsiColor() {
    return ansiColor;
  }

  private final String ansiColor;

  PrinterColor(String letterColor, String ansiColor) {
    this.letterColor = letterColor;
    this.ansiColor = ansiColor;
  }

  public String getLetterColor() {
    return letterColor;
  }
}
