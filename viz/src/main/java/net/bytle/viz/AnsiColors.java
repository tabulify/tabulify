package net.bytle.viz;

/**
 * ANSI RGB color
 *
 * @see <a href="https://en.wikipedia.org/wiki/ANSI_escape_code">Ansi Escape code</a>
 * <p>
 * A sequences of bytes (mostly starting with Esc and '[') are embedded into the text,
 * The terminal will interpret them as commands, not as character.
 * <p>
 * All sequences start with ESC (27 / hex 0x1B / oct 033), followed by a second byte in the range 0x40–0x5F (ASCII @A–Z[\]^_)
 * Standard ECMA-48: Control Functions for Coded Character Sets
 * http://www.ecma-international.org/publications/standards/Ecma-048.htm
 *
 * Good article:
 * http://www.lihaoyi.com/post/BuildyourownCommandLinewithANSIescapecodes.html
 */
public class AnsiColors {


  // CSI SGR
  // https://en.wikipedia.org/wiki/ANSI_escape_code#SGR_parameters
  // CSI code m
  public static final char M = 'm';
  static final String ANSI_RESET = ControlSequence.ESCAPE_SEQUENCE_CONTROL_SEQUENCE_INTRODUCER + 0 + M; // Reset (Each display attribute remains in effect until a following occurrence of SGR resets it)

  // CSI Colors
  // https://en.wikipedia.org/wiki/ANSI_escape_code#Colors

  // Colors 3/4 bit
  // https://en.wikipedia.org/wiki/ANSI_escape_code#3/4_bit
  public static final int COLORS_RED_FOREGROUND_CODE = 31;
  public static final int COLORS_RED_BRIGHT_FOREGROUND = 91;
  public static final int COLORS_BLACK_FOREGROUND_CODE = 30;
  public static final int COLORS_WHITE_BACKGROUND_CODE = 47;
  static final String COLORS_RED = ControlSequence.ESCAPE_SEQUENCE_CONTROL_SEQUENCE_INTRODUCER + COLORS_RED_FOREGROUND_CODE + M; // Red
  static final String COLORS_RED_BRIGHT = ControlSequence.ESCAPE_SEQUENCE_CONTROL_SEQUENCE_INTRODUCER + COLORS_RED_BRIGHT_FOREGROUND + M; // Bright Red
  static final String COLORS_BLACK = ControlSequence.ESCAPE_SEQUENCE_CONTROL_SEQUENCE_INTRODUCER + COLORS_BLACK_FOREGROUND_CODE + M; // Black
  static final String COLORS_BLACK_ON_WHITE = ControlSequence.ESCAPE_SEQUENCE_CONTROL_SEQUENCE_INTRODUCER + COLORS_BLACK_FOREGROUND_CODE + ';' + COLORS_WHITE_BACKGROUND_CODE + M;
  static final String COLORS_RESET = ControlSequence.ESCAPE_SEQUENCE_CONTROL_SEQUENCE_INTRODUCER + 39 + ';' + 49 + M; // Reset colors attributes


  /**
   * Print the colors on 8 bit as defined here
   * https://en.wikipedia.org/wiki/ANSI_escape_code#8-bit
   */
  public static void print8BitColorPalette() {
    // Ansi 8 bit
    for (int i = 0; i < 256; i += 1) {
      if (i % 10 == 0) {
        System.out.println();
      }
      System.out.print(String.format(ControlSequence.ESCAPE_SEQUENCE_CONTROL_SEQUENCE_INTRODUCER + "38;5;%s" + AnsiColors.M + " %d " + AnsiColors.ANSI_RESET, i, i));
    }
  }

  /**
   *
   * @param string
   * @param color_sequence
   * @return an ascii control sequence that colors the string with the provided color code
   *
   * Example:
   *  colorize("original text","38;5;33")
   */
  String colorize(String string, String color_sequence) {
    return ControlSequence.ESCAPE_SEQUENCE_CONTROL_SEQUENCE_INTRODUCER+color_sequence +M+string+ANSI_RESET;
  }


}
