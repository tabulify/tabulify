package com.tabulify.stream;

public enum PrinterPrintFormat {

  /**
   * Output is sent to a pipe in a shell
   * No header, no printed character
   * Delete the headers and don't print the control characters
   */
  PIPE,
  /**
   * Print the headers only the first time
   * a data path is printed
   */
  STREAM,
  DEFAULT

}
