package com.tabulify.uri;

import java.util.ArrayDeque;
import java.util.NoSuchElementException;

import static com.tabulify.uri.DataUriStringNode.BLOCK_CLOSE;
import static com.tabulify.uri.DataUriStringNode.BLOCK_OPEN;

/**
 * The parser of string to {@link DataUriStringNode}
 */
public class DataUriStringParser {

  /**
   * @param dataUriString - a data uri string
   * @return a {@link DataUriStringNode}
   */
  public static DataUriStringNode parse(String dataUriString) {

    DataUriStringNode.DataUriNodeBuilder root = DataUriStringNode.builder();
    /**
     * The actual Data Uri Path build
     */
    ArrayDeque<DataUriStringNode.DataUriNodeBuilder> actualNodeBuild = new ArrayDeque<>();
    actualNodeBuild.add(root);

    boolean connectionPart = false;
    for (int i = 0; i < dataUriString.length(); i++) {
      char c = dataUriString.charAt(i);
      switch (c) {
        case BLOCK_OPEN:
          DataUriStringNode.DataUriNodeBuilder child = DataUriStringNode.builder();
          actualNodeBuild.getLast().setPathNodeBuilder(child);
          actualNodeBuild.add(child);
          break;
        case BLOCK_CLOSE:
          actualNodeBuild.pollLast();
          connectionPart = false;
          break;
        case DataUriStringNode.AT_CHAR:
          connectionPart = true;
          break;
        default:
          DataUriStringNode.DataUriNodeBuilder actualDataUriStringNode = null;
          try {
            actualDataUriStringNode = actualNodeBuild.getLast();
          } catch (NoSuchElementException e) {
            throw new IllegalArgumentException("The data uri (" + dataUriString + ") has parenthesis that are not closed or opened", e);
          }
          if (connectionPart) {
            actualDataUriStringNode.appendConnectionCharacter(c);
          } else {
            actualDataUriStringNode.appendPathCharacter(c);
          }
      }
    }
    return root.build();
  }
}
