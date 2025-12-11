package com.tabulify.glob;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Glob {


  public static final char OPEN_BRACKET = '[';
  public static final char QUESTION_MARK = '?';
  public static final char STAR = '*';
  public static final char ESCAPE = '\\';
  public static final char CLOSE_BRACKET = ']';
  public static final String DOUBLE_STAR = "**";
  public final String glob;

  /**
   * The character that captures the groups
   */
  private static final List<Character> capturingElements = Arrays.asList(QUESTION_MARK, STAR, OPEN_BRACKET);

  public Glob(String glob) {
    Objects.requireNonNull(glob, "The glob passed was null");
    this.glob = glob;
  }

  public static Glob createOf(String s) {
    return new Glob(s);
  }

  private static final Pattern pattern = Pattern.compile("\\$[0-9]");

  public static boolean containsBackReferencesCharacters(String s) {
    return pattern.matcher(s).find();
  }

  public static boolean matchOneOfGlobs(String key, List<Glob> globs) {
    return matchOneOfGlobs(key, globs, 0);
  }

  /**
   * @param flags - flags of {@link Pattern#compile(String, int)} or 0
   */
  public static boolean matchOneOfGlobs(String key, List<Glob> globs, int flags) {
    boolean match = false;
    for (Glob glob : globs) {
      if (glob.matches(key, flags)) {
        match = true;
        break;
      }
    }
    return match;
  }

  public static Glob createOfSqlPattern(String s) {
    String regexPattern = s
      .replace("%", "*")
      .replace("_", "?")
      .replace("\\", "") // delete the escape
      ;
    return createOf(regexPattern);
  }

  /**
   * @return a regular expression pattern with groups
   */
  public String toRegexPatternWithGroup() {

    return toRegexPatternBase(true);

  }

  /**
   * Converts a standard POSIX Shell globbing pattern into a regular expression
   * pattern. The result can be used with the standard {@link java.util.regex} API to
   * recognize strings which match the glob pattern.
   * <p/>
   * <p>
   * See also, the POSIX Shell language:
   * http://pubs.opengroup.org/onlinepubs/009695399/utilities/xcu_chap02.html#tag_02_13_01
   * https://stackoverflow.com/questions/1247772/is-there-an-equivalent-of-java-util-regex-for-glob-type-patterns
   *
   * @return A regex pattern to recognize the given glob pattern.
   */
  private String toRegexPatternBase(Boolean withWildCardCapturingGroup) {
    StringBuilder sb = new StringBuilder(glob.length() + 10);

    /**
     * Should start with to avoid that
     * `foo_bar.txt`
     * is select if the glob is
     * `bar.txt`
     */
    sb.append("^");

    int inGroup = 0;
    /**
     * Are we in a class of characters (ie [abc]
     */
    int inClass = 0;
    /**
     * Do we apply group capturing
     */
    int firstIndexInClass = -1;
    char[] arr = glob.toCharArray();
    for (int i = 0; i < arr.length; i++) {
      char ch = arr[i];
      switch (ch) {
        case ESCAPE:
          if (++i >= arr.length) {
            sb.append('\\');
          } else {
            char next = arr[i];
            switch (next) {
              case ',':
                // escape not needed
                break;
              case 'Q':
              case 'E':
                // extra escape needed
                sb.append('\\');
              default:
                sb.append('\\');
            }
            sb.append(next);
          }
          break;
        case STAR:
          if (inClass == 0)
            if (withWildCardCapturingGroup) {
              sb.append("(.*)");
            } else {
              sb.append(".*");
            }
          else
            sb.append('*');
          break;
        case QUESTION_MARK:
          if (inClass == 0)
            if (withWildCardCapturingGroup) {
              sb.append("(.)");
            } else {
              sb.append('.');
            }
          else
            sb.append('?');
          break;
        case OPEN_BRACKET:
          inClass++;
          firstIndexInClass = i + 1;
          if (withWildCardCapturingGroup) {
            sb.append('(');
          }
          sb.append('[');

          break;
        case CLOSE_BRACKET:
          inClass--;
          sb.append(']');
          if (withWildCardCapturingGroup) {
            sb.append(')');
          }
          break;
        case '.':
        case '(':
        case ')':
        case '+':
        case '|':
        case '^':
        case '$':
        case '@':
        case '%':
          if (inClass == 0 || (firstIndexInClass == i && ch == '^'))
            sb.append('\\');
          sb.append(ch);
          break;
        case '!':
          if (firstIndexInClass == i)
            sb.append('^');
          else
            sb.append('!');
          break;
        case '{':
          inGroup++;
          sb.append('(');
          break;
        case '}':
          inGroup--;
          sb.append(')');
          break;
        case ',':
          if (inGroup > 0)
            sb.append('|');
          else
            sb.append(',');
          break;
        default:
          sb.append(ch);
      }
    }

    /**
     * Should end with to avoid that
     * `bar.jsonl`
     * is select if the glob is
     * `bar.json`
     */
    sb.append("$");
    return sb.toString();
  }

  public String toRegexPattern() {
    return toRegexPatternBase(false);
  }

  public Boolean matches(String s) {
    return matches(s, 0);
  }

  public Boolean matchesIgnoreCase(String s) {
    return matches(s, Pattern.CASE_INSENSITIVE);
  }

  /**
   * @param flags - {@link Pattern#compile(String, int)} flags
   */
  public Boolean matches(String s, int flags) {
    String regexpPattern = toRegexPattern();
    Pattern mypattern = Pattern.compile(regexpPattern, flags);
    return mypattern.matcher(s).matches();
  }

  @Override
  public String toString() {
    return glob;
  }

  /**
   * Get an SQL glob from Unix Glob
   *
   * @param escape - the escape character or null if not supported. The escape character in a JDBC driver can be retrieved with the {@link DatabaseMetaData#getSearchStringEscape()}
   * @return a sql pattern
   * where the sql character matchers are:
   * * "%" means match any substring of 0 or more characters (equivalent to the unix glob *)
   * * "_" means match any one character (equivalent to the unix glob ?)
   * <p>
   * An Runtime exception is thrown if the glob pattern has sql matchers but no escape character
   * @see <a href="https://en.wikipedia.org/wiki/Glob_(programming)#SQL">Wikipedia SQL Globbing</a>
   * @see <a href="https://en.wikipedia.org/wiki/Wildcard_character#Databases">Wildcard Databases</a>
   */
  public String toSqlPattern(String escape) {

    String sqlPattern = this.glob;
    if (escape != null) {
      sqlPattern = sqlPattern.replace("_", escape + "_")
        .replace("%", escape + "%");
    } else {
      if (containsSqlMatchers()) {
        throw new RuntimeException("No escape string has been defined and the glob pattern (" + sqlPattern + ") has the characters `%` or `_`");
      }
    }
    return sqlPattern
      .replace("*", "%")
      .replace("?", "_");
  }

  /**
   * @return true if the glob pattern contains sql matchers
   */
  public boolean containsSqlMatchers() {
    return glob.contains("%") || glob.contains("_");
  }

  /**
   * @param inputString - a glob expression
   * @return a matcher
   * so that you can do a {@link Matcher#find()}
   * and other logic
   */
  public Matcher toMatcher(String inputString) {
    assert inputString != null : "inputString should not be null";
    String regex = toRegexPatternWithGroup();
    Pattern pattern = Pattern.compile(regex);
    return pattern.matcher(inputString);
  }

  /**
   * Return the matched groups.
   * <p>
   * If there is no match, the groups will be only composed
   * of one group that is the input string at index 0
   *
   * @param inputString - the input string where the groups will be extracted
   * @return the groups
   */
  public List<String> getGroups(String inputString) {

    Matcher matcher = toMatcher(inputString);
    List<String> groups = new ArrayList<>();
    groups.add(inputString);
    if (matcher.find()) {
      for (int i = 1; i <= matcher.groupCount(); i++) {
        groups.add(matcher.group(i));
      }
    }
    return groups;

  }

  /**
   * @param haystack      - a string to match against the glob pattern that will provide the capturing group (one by glob wildcard)
   * @param backReference - a string template with back group referencing captured from the source (ie $0, $1 )
   * @return Example with:
   * <p>
   * * the glob pattern `*o`
   * * a haystack of `foo`
   * * a back reference of `$1i`
   * * the output string will be `foi`
   * <p>
   * In a transfer, the haystack is the file name selected, glob expression is the selector and back reference is the target
   */
  public String replace(String haystack, String backReference) {

    String regex = toRegexPatternWithGroup();
    return haystack.replaceFirst(regex, backReference);

  }

  /**
   * Does the glob contains matchers that returns groups
   *
   * @return if there is group found
   */
  public boolean containsGroupMatchers() {
    for (Character character : capturingElements) {
      if (this.glob.contains(String.valueOf(character))) {
        return true;
      }
    }
    return false;
  }

  /**
   * @return if the string expression has wild card
   * and is not just a string
   */
  public boolean containsGlobWildCard() {
    /**
     * Shortcut that should be good
     */
    return containsGroupMatchers();
  }

  public String getPattern() {
    return this.glob;
  }
}
