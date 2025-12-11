package com.tabulify.cli;


import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Static class
 */
public class CliWords2Yaml {

  /**
   * Create the config file with the default values in yaml format
   *
   * @param cliWords
   */
  public void createYamlConfigFile(List<CliWord> cliWords, Path path) {


    BufferedWriter outputStream;

    try {

      /**
       * Snake YML does not permit to add comments
       * We are then writing the yaml text file ourselves
       */
      outputStream = Files.newBufferedWriter(path);

      final String commentWord = "#";
      for (CliWord cliWord : cliWords) {
        if (cliWord.getConfigName() != null) {

          // Comment
          final String description = cliWord.getDescription();
          outputStream.write(commentWord + " " + cliWord.getName());
          if (description != null) {
            outputStream.write(" : " + description);
          }
          outputStream.newLine();

          // Value
          List<String> defaultValues = cliWord.getDefaultValues();
          switch (defaultValues.size()) {
            case 0:
              break;
            case 1:
              outputStream.write(cliWord.getName() + ": " + defaultValues.get(0));
              outputStream.newLine();
              break;
            default:
              outputStream.write(cliWord.getName() + ":");
              outputStream.newLine();
              for (String value : defaultValues) {
                outputStream.write("  - " + value);
                outputStream.newLine();
              }
              break;

          }

          // Hr
          outputStream.newLine();
        }
      }
      outputStream.flush();
      outputStream.close();

    } catch (IOException e) {

      throw new RuntimeException(e);

    }
  }

}
