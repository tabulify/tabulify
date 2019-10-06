/**
 * Created by gerard on 07-06-2016.
 * Example on how to traverse a Sftp File System
 * With Groovy
 */

@Grapes([
        @Grab(group='net.bytle', module='bytle-sftp', version='1.0.0'),
])

import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

def traverse(path) {

    if (Files.isDirectory(path)) {

        println(path.toAbsolutePath().toString() + " is a directory")
        childPaths = Files.newDirectoryStream(path);
        for (Path childPath: childPaths) {
            traverse(childPath)
        }

    } else if (Files.isRegularFile(path)) {

        println(path.toAbsolutePath().toString() + " is a file")

    } else {

        println('Error: '+path.toAbsolutePath().toString()+' is not a file or a directory');
        System.exit(1)
    }

}

/**
 * The script
 */

if (args.length != 0) {

    url = args[0];


} else {

    def env = System.getenv();
    USER = env.get("BYTLE_NIOFS_SFTP_USER");
    PWD = env.get("BYTLE_NIOFS_SFTP_PWD");
    HOST = env.get("BYTLE_NIOFS_SFTP_HOST");
    PORT = env.get("BYTLE_NIOFS_SFTP_PORT");
    url = "sftp://" + USER + ":" + PWD + "@" + HOST + ":" + PORT;

}

fileSystem = FileSystems.newFileSystem(
        URI.create(url),
        null,
        Thread.currentThread().getContextClassLoader()
    );
path = fileSystem.getPath("");

try {

    traverse(path)

} finally {

    fileSystem.close();

}