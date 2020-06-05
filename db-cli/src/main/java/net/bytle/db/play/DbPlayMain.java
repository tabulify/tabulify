package net.bytle.db.play;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DbPlayMain {



  /**
   * The play file and the play home
   *
   * @param args
   * @throws IOException
   */
  public static void main(String[] args) throws IOException {

    Path homeDirectory = null;
    Path playFile = null;

    switch (args.length) {
      case 0:
        System.err.println("A play should be given as first parameters");
        System.exit(1);
        break;
      case 1:
        playFile = Paths.get(args[0]);
        homeDirectory = playFile.getParent();
        break;
      case 2:
        homeDirectory = Paths.get(args[1]);
        playFile = homeDirectory.resolve(args[0]);
        break;
      default:
        System.err.println("Too much arguments");
        System.exit(1);
        break;
    }

    if (!Files.exists(playFile)) {
      System.err.println("The play file (" + playFile.toAbsolutePath().toString() + ") does not exist");
      System.exit(1);
    }


    DbPlay.of()
      .setPlayFile(playFile)
      .setHomDirectory(homeDirectory)
      .run();

  }
}
