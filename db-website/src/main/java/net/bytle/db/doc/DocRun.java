package net.bytle.db.doc;

import net.bytle.cli.CliCommand;
import net.bytle.cli.CliLog;
import net.bytle.cli.CliParser;
import net.bytle.cli.CliUsage;
import net.bytle.db.Tabular;
import net.bytle.db.TabularAttributes;
import net.bytle.db.fs.FsDataSystem;
import net.bytle.db.tabli.Tabli;
import net.bytle.db.tabli.TabliWords;
import net.bytle.doctest.CommandDel;
import net.bytle.doctest.DocCache;
import net.bytle.doctest.DocExecutor;
import net.bytle.doctest.DocLog;
import net.bytle.java.JavaEnvs;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

public class DocRun {

  private static boolean purgeCache = false;

  /**
   * @param args - first parameter the page path (example 'howto:glob')
   *             - second parameter optional - if present it will purge the cache cache (Every value is correct)
   */
  public static void main(String[] args) {

    CliLog.LOGGER.setLevel(Level.INFO);

    CliCommand rootCli = CliCommand.createRootWithEmptyInput("docrun")
      .setDescription("Run code in documentation page with unit elements")
      .addExample(
        "To run a page with caching disabled",
        CliUsage.CODE_BLOCK,
        "docrun page:namespace:name no-cache",
        CliUsage.CODE_BLOCK
      )
      .addExample(
        "To run all page with caching disabled",
        CliUsage.CODE_BLOCK,
        "docrun * no-cache",
        CliUsage.CODE_BLOCK
      );

    String pagePatternArg = "pagePattern";
    rootCli.addArg(pagePatternArg)
      .setDescription("The page id or a glob pattern");

    String cachingOffArg = "cachingOff";
    rootCli.addArg(cachingOffArg)
      .setDescription("If this argument is present, the caching is off");

    String logging = "--log-level";
    rootCli.addProperty(logging)
      .setShortName("-l")
      .setDescription("The log run logging")
      .setDefaultValue("info");

    String help = "--help";
    rootCli.addFlag(help)
      .setShortName("-h")
      .setDescription("This help");
    rootCli.setHelpWord(help);

    /**
     * Variable Name
     */
    String projectName = "db-website";
    String fileSubDirectoryName = "howto";
    String pagesDirectoryName = "pages";

    /**
     * Doc Home directory detection
     */
    Path docHomePath = JavaEnvs.HOME_PATH;
    if (Files.exists(docHomePath.resolve(projectName))) {
      docHomePath = docHomePath
        .resolve(projectName)
        .resolve("src")
        .resolve("doc");
    }

    /**
     * DokuWiki to Fs Path
     */
    Path absolutePagesPath = docHomePath.resolve(pagesDirectoryName);

    CliParser cliParser = rootCli.parse(args);
    /**
     * First argument is a dokuwiki path
     */
    String pagePattern = cliParser.getString(pagePatternArg);
    pagePattern = pagePattern.replace(":", "/");
    String docFileExtension = ".txt";
    if (!pagePattern.endsWith(docFileExtension)) {
      pagePattern = pagePattern + docFileExtension;
    }
    List<Path> paths = FsDataSystem.getFilesByGlob(absolutePagesPath, pagePattern);


    if (cliParser.getString(cachingOffArg) != null) {
      // pc for purge cache
      purgeCache = true;
    }

    String logLevel = cliParser.getString(logging);
    Level level = Level.parse(logLevel.toUpperCase());
    DocLog.LOGGER.setLevel(level);


    /**
     * We use the cache
     */
    DocCache docCache = DocCache.get(TabularAttributes.APP_NAME.toString());
    if (purgeCache) {
      docCache.purge(absolutePagesPath);
    }

    /**
     * One clean environment by run
     */
    Tabular.tabularWithCleanEnvironment().close();


    /**
     * Definition of the command and Run
     */
    DocExecutor.create(TabularAttributes.APP_NAME.toString())
      .addCommand(TabliWords.CLI_NAME, Tabli.class)
      .addCommand("del", CommandDel.class)
      .setBaseFileDirectory(docHomePath.resolve(fileSubDirectoryName))
      .setOverwrite(true)
      .setCache(docCache)
      .setSystemProperty("tabli.log-level", "warning")
      .setStopRunAtFirstError(true)
      .run(paths.toArray(new Path[0]));

  }
}
