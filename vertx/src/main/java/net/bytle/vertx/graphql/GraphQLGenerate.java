package net.bytle.vertx.graphql;

import graphql.language.Document;
import graphql.parser.Parser;
import net.bytle.fs.Fs;

import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

/**
 * A utility class to generate
 * code from the GraphQL file
 * <p>
 * May be moved as Gradle Build (see buildSrc)
 * <a href="https://docs.gradle.org/current/userguide/organizing_gradle_projects.html#sec:build_sources">...</a>
 */
public class GraphQLGenerate {


  public static void main(String[] args) throws NoSuchFileException {

    String file = Fs.getFileContent(Path.of("..","tower","src","main","resources","graphql","eraldy.graphqls"));
    Parser parser = new Parser();
    Document document = parser.parseDocument(file);
    System.out.println(document.getDefinitions().size());

  }

}
