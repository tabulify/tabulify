package net.bytle.type;

import java.util.regex.Pattern;

public class Color {


  static Pattern pattern;

  static {
    // Regular expression to match common color formats
    String colorRegex = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$|^((rgb|hsl)a?\\(((\\s*\\d+%?\\s*,){2}\\s*\\d+%?\\s*,?\\s*\\d*(\\.\\d+)?\\s*\\)))$|^\\b(black|silver|gray|white|maroon|red|purple|fuchsia|green|lime|olive|yellow|navy|blue|teal|aqua)\\b$";
    pattern = Pattern.compile(colorRegex);
  }

  private final String color;

  private Color(String color) throws ColorCastException {

    // Match the colorString against the pattern
    if (!pattern.matcher(color).matches()) {
      throw new ColorCastException("The color value (" + color + ") is not a valid color");
    }

    this.color = color;
  }

  public static Color of(String appPrimaryColor) throws ColorCastException {
    return new Color(appPrimaryColor);
  }

  public String getValue() {
    return color;
  }

  @Override
  public String toString() {
    return color;
  }

}
