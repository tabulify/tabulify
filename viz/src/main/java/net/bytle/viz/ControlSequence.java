package net.bytle.viz;

/**
 * CSI (Control Sequence Indicator)
 * The base characters of the control sequence
 *
 * <a href=https://en.wikipedia.org/wiki/ANSI_escape_code#CSI_sequences></a>
 *
 * This sequence are used :
 *   * {@link AnsiColors}
 */
public class ControlSequence {

  // Windows
  // https://superuser.com/questions/413073/windows-console-with-ansi-colors-handling/1105718#1105718
  // Enable https://github.com/rg3/youtube-dl/issues/15758
  static final String ESC = "\033"; // octal \033 or unicode \u001B
  static final String ESCAPE_SEQUENCE_RESET_TO_INITIAL_STATE = ESC + 'c'; // Resets the device to its original state

  static final String ESCAPE_SEQUENCE_CONTROL_SEQUENCE_INTRODUCER = ESC + '['; // Most useful,

}
