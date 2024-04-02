package net.bytle.java;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Static function around the JVM
 */
public class Javas {

  /**
   * @return return the class file path or the jar if located in a jar
   */
  public static Path getFilePathFromUrl(java.net.URL sourceCodeUrl) {
    try {

      switch (sourceCodeUrl.getProtocol()) {
        case "file":
          return Paths.get(sourceCodeUrl.toURI());

        case "jar":

          String fileUri = sourceCodeUrl.getFile();

          /**
           * Path in a jar have at the end a suffix separated by an exclamation such as
           * dir/myjar!package.myclass
           * We delete it if we are in a jar
           */
          fileUri = fileUri.substring(0, fileUri.indexOf("!"));

          /**
           * Send back the path
           */
          URI pathUri = URI.create(fileUri);
          return Paths.get(pathUri);
        default:
          throw new RuntimeException("The protocol (" + sourceCodeUrl.getProtocol() + ") is not implemented and we can therefore return the class file path back");
      }


    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static Path getSourceCodePath(Class<?> clazz) {
    return getFilePathFromUrl(Objects.requireNonNull(clazz.getResource(clazz.getSimpleName() + ".class")));
  }

  public static Boolean getSourceCodePathInJar(Class<?> clazz) {
    java.net.URL sourceCodeUrl = Objects.requireNonNull(clazz.getResource(clazz.getSimpleName() + ".class"));

    switch (sourceCodeUrl.getProtocol()) {
      case "file":
        return false;
      case "jar":
        return true;
      default:
        throw new RuntimeException("The protocol (" + sourceCodeUrl.getProtocol() + ") is not implemented and we can therefore return the class file path back");
    }

  }

  public static Path getModulePath(Class<?> clazz) throws NotDirectoryException {


    Path buildPath = getBuildDirectory(clazz);
    return buildPath.getParent();


  }

  /**
   * Check if there is a build directory
   * (We could also check if there is a build file (ie gradle.kts)
   * in the working directory to not throw an error when testing the software locally
   * See Tabli.hasBuildFileInRunningDirectory
   */
  public static Path getBuildDirectory(Class<?> clazz) throws NotDirectoryException {

    Path sourceCodePath = Javas.getSourceCodePath(clazz);
    List<String> buildPathNames = Arrays.asList(
      "build", // gradle
      "out" // idea
    );

    for (String buildPathName : buildPathNames) {
      Path buildPath = JavaEnvs.getPathUntilName(sourceCodePath.getParent(), buildPathName);
      if (buildPath != null) {
        return buildPath;
      }
    }

    throw new NotDirectoryException("No build path was found");

  }

  /**
   * Just a snippet that shows a way to locate the code source
   * We use know {@link #getSourceCodePath(Class)}
   */
  @SuppressWarnings("unused")
  public void getCodeSourcePathFromCodeSource() {

    URL location = Javas.class.getProtectionDomain().getCodeSource().getLocation();
    System.out.println(location.getFile());

  }
}
