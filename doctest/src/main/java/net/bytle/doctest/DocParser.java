package net.bytle.doctest;


import net.bytle.type.Strings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static methods to parse a file into DocTestUnit
 */
public class DocParser {

  /**
   * @param path - where the doc is located
   * @return list of DocTestUnit parsed from the file defined by the path
   */
  static public List<DocUnit> getDocTests(Path path) {

    List<DocUnit> docUnits = new ArrayList<>();


    final String unitTestNode = "unit";
    final String codeNodeName = "code";
    final String fileNodeName = "file";
    final String consoleNodeName = "console";


    Pattern unitTestNodePattern = Pattern.compile("<" + unitTestNode + "(.*?)>(.*?)</" + unitTestNode + ">", Pattern.DOTALL);
    Pattern codeTestNodePattern = Pattern.compile("<" + codeNodeName + "(.*?)>(.*?)</" + codeNodeName + ">", Pattern.DOTALL);
    Pattern consolePattern = Pattern.compile("<" + consoleNodeName + "(.*?)>(.*?)</" + consoleNodeName + ">", Pattern.DOTALL);
    Pattern filePattern = Pattern.compile("<" + fileNodeName + "(.*?)>(.*?)</" + fileNodeName + ">", Pattern.DOTALL);
    String s = Strings.createFromPath(path).toString();

    Matcher unitTestMatcher = unitTestNodePattern.matcher(s);
    while (unitTestMatcher.find()) {

      DocUnit docUnit = new DocUnit();
      docUnit.setPath(path);
      docUnits.add(docUnit);
      String unitTestProperties = unitTestMatcher.group(1);
      docUnit.setProperty(unitTestProperties);
      String unitTestDefinitie = unitTestMatcher.group(2);
      final int unitTestStartLocation = unitTestMatcher.start() + 1 + unitTestNode.length() + unitTestProperties.length() + 1;

      // Try to find the code
      Matcher codeTestMatcher = codeTestNodePattern.matcher(unitTestDefinitie);
      boolean unitFound = codeTestMatcher.find();
      if (unitFound) {

        // Properties of the node is now only the language
        String codeProperties = codeTestMatcher.group(1).trim();
        final int i = codeProperties.indexOf(" ");
        if (i == -1) {
          docUnit.setLanguage(codeProperties);
        } else {
          docUnit.setLanguage(codeProperties.substring(0, i));
        }

        // The code
        String codeDefinitie = codeTestMatcher.group(2);
        docUnit.setCode(codeDefinitie);

        Integer[] codeLocation = new Integer[2];
        codeLocation[0] = codeTestMatcher.start() + unitTestStartLocation + 1 + codeNodeName.length() + 1;
        codeLocation[1] = codeTestMatcher.end() + unitTestStartLocation - 2 - codeNodeName.length() - 1;
        docUnit.setCodeLocation(codeLocation);

      }

      // Try to find the console
      Matcher consoleMatcher = consolePattern.matcher(unitTestDefinitie);
      boolean codeFound = consoleMatcher.find();
      if (codeFound) {
        String consoleContent = consoleMatcher.group(2);
        Integer[] consoleLocation = new Integer[2];
        consoleLocation[0] = consoleMatcher.start() + unitTestStartLocation + 1 + consoleNodeName.length() + 1;
        consoleLocation[1] = consoleMatcher.end() + unitTestStartLocation - 2 - consoleNodeName.length() - 1;
        docUnit.setConsoleLocation(consoleLocation);
        docUnit.setConsoleContent(consoleContent);
      }

      // Try to find the file
      Matcher fileMatcher = filePattern.matcher(unitTestDefinitie);
      boolean fileFound = fileMatcher.find();
      if (fileFound) {

        DocFileBlock docFileBlock = DocFileBlock.get(docUnit);
        docUnit.addFileBlock(docFileBlock);
        String fileProperties = fileMatcher.group(1);
        String[] properties = fileProperties.trim().split(" ");
        if (properties.length >= 1) {
          docFileBlock.setLanguage(properties[0]);
        }
        if (properties.length >= 2) {
          docFileBlock.setPath(properties[1]);
        }

        String fileContent = fileMatcher.group(2).trim();
        docFileBlock.setContent(fileContent);

        int startLocation = fileMatcher.start() + unitTestStartLocation + 1 + fileNodeName.length() + fileProperties.length() + 1;
        int locationEnd = fileMatcher.end() + unitTestStartLocation - 2 - fileNodeName.length() - 1;
        docFileBlock.setLocationStart(startLocation);
        docFileBlock.setLocationEnd(locationEnd);

      }

      // Test that the file node is before the console node
      for (DocFileBlock docFileBlock : docUnit.getFileBlocks()) {

        // Console is not mandatory
        final Integer[] consoleLocation = docUnit.getConsoleLocation();
        if (consoleLocation != null) {
          if (docFileBlock.getLocationStart() > consoleLocation[0]) {
            throw new RuntimeException("Order is not good, the console node must be after the file node");
          }
        }

        // Code is then we have a location
        if (docUnit.getCodeLocation()!=null) {
          if (docFileBlock.getLocationStart() > docUnit.getCodeLocation()[0]) {
            throw new RuntimeException("Order is not good, the file node must be before the code node in the doc test file " + docUnit.getPath());
          }
        }

      }

    }


    // Test that all nodes are closed
    final Integer numberOfUnitTestNode = Strings.createFromString(s).numberOfOccurrences("<" + unitTestNode);
    if (docUnits.size() != numberOfUnitTestNode) {
      throw new RuntimeException("A " + unitTestNode + " node seems not to be closed in the file (" + path + "). There is " + numberOfUnitTestNode + " unit test node with the name (" + unitTestNode + ") but we returns only " + docUnits.size() + " doc unit test code.");
    }


    return docUnits;

  }

  /**
   * Alias method that call {@link #getDocTests(Path)}
   *
   * @param path
   * @return a list of doc test from a file
   */
  public static List<DocUnit> getDocTests(String path) {
    return getDocTests(Paths.get(path));
  }

}
