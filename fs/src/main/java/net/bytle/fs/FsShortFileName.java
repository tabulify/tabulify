package net.bytle.fs;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Windows Short file name implementation
 * https://en.wikipedia.org/wiki/8.3_filename
 */
public class FsShortFileName {

  private final static Pattern pattern = Pattern.compile("(.*)(~[0-9]*)(.*)");
  private final String s;
  private final boolean found;
  private final String shortName;

  public FsShortFileName(String s) {
    this.s = s;
    Matcher matcher = pattern.matcher(s);
    found = matcher.find();
    if (found) {
      shortName = matcher.group(1);
    } else {
      shortName = null;
    }
  }

  public static FsShortFileName of(String s) {
    return new FsShortFileName(s);
  }

  public boolean isShortFileName() {
    return found;
  }

  String getShortName() {
    return shortName;
  }
}
