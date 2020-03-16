package net.bytle.viz;

/**
 * The {@link ControlSequence} for the cursor
 * at the command line
 */
public class CursorMove {

  // CSI CUB (Cursor Back) 1000 back - move cursor left by 1000 characters
  // Cursor Back = CSI + n + D
  // https://en.wikipedia.org/wiki/ANSI_escape_code#Terminal_output_sequences
  public static final String CURSOR_MOVE_BACK = ControlSequence.ESCAPE_SEQUENCE_CONTROL_SEQUENCE_INTRODUCER + "1000" + 'D';
  public static final String CURSOR_MOVE_UP = ControlSequence.ESCAPE_SEQUENCE_CONTROL_SEQUENCE_INTRODUCER + "1" + 'A'; // Move up one

}
