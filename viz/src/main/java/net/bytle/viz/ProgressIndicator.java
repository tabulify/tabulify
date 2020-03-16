package net.bytle.viz;

import java.util.stream.IntStream;

/**
 * Progress indicator on the console
 */
public class ProgressIndicator {

  /**
   * Print a progress by refreshing a ratio
   */
  public static void printProgressNumber() {

    System.out.println("Loading...");
    IntStream.range(0, 100).forEach(i -> {
        sleep(100);
        System.out.print(CursorMove.CURSOR_MOVE_BACK + " " + i + "%");
      }
    );

  }

  /**
   * Print a progress bar
   */
  static void printProgressBar() {
    System.out.println("Loading...");
    int total = 100;
    int characterEvery = 4; // every advancement of 4 we will get a character
    int totalCharacters = total/characterEvery; // 25 characters
    IntStream.range(0, total).forEach(i -> {
      int actualCharacters = (i + 1) / characterEvery;
      String bar = "[" + (new String(new char[actualCharacters]).replace("\0", "#")) + (new String(new char[totalCharacters - actualCharacters]).replace("\0", " ")) + "]";
        System.out.println(CursorMove.CURSOR_MOVE_BACK + bar);
      }
    );
  }

  /**
   * Just a wrapper
   * @param ms
   */
  private static void sleep(int ms) {
    try {
      Thread.sleep(ms);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

}
