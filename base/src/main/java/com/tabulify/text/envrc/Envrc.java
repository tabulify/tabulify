package com.tabulify.text.envrc;

import com.tabulify.exception.BadExitStatusException;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Execute an envrc file and returns the environment
 * <p>
 * Example:
 * <pre>{@code
 * Map<String, String> env = Envrc.exec(
 * Paths.get(".envrc"),
 * Map.of("PASSWORD_STORE_DIR", "/home/user/store/dir")
 * );
 * for (Map.Entry<String, String> entry : env.entrySet()) {
 * System.out.println(entry.getKey() + ":" + entry.getValue());
 * }
 * }</pre>
 */
public class Envrc {

    public static Map<String, String> exec(Path envrcPath, Map<String, String> env) throws BadExitStatusException {

        Map<String, String> environmentVariables = new HashMap<>();


        if (!Files.exists(envrcPath)) {
            throw new BadExitStatusException(envrcPath + " was not found", 1);
        }

        // Create the command to source the .envrc file and print all environment variables
        // script is there to set a tty so that we can enter a password
        String[] command = {
                "/bin/bash",
                "-c",
                "source " + envrcPath.toAbsolutePath() + " && env"
        };

        String output;
        try {
            output = new ProcessExecutor()
                    .command(command)
                    .environment(env)
                    .readOutput(true)
                    .exitValues(0)
                    .execute()
                    .outputUTF8();
        } catch (InvalidExitValueException e) {
            throw new BadExitStatusException(e.getResult().outputUTF8(), e.getExitValue());
        } catch (TimeoutException | InterruptedException | IOException e) {
            throw new BadExitStatusException(e);
        }

        Map<String, String> sysEnvs = System.getenv();

        // Read the output
        try (BufferedReader reader = new BufferedReader(new StringReader(output))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Parse each line as NAME=VALUE
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String name = line.substring(0, equalsIndex);
                    if (sysEnvs.containsKey(name) || env.containsKey(name)) {
                        continue;
                    }
                    String value = line.substring(equalsIndex + 1);
                    environmentVariables.put(name, value);
                }
            }
        } catch (IOException e) {
            throw new BadExitStatusException(e);
        }

        return environmentVariables;
    }

}
