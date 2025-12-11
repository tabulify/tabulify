package com.tabulify.docker;

import com.tabulify.docker.util.CiDetector;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import com.tabulify.os.Oss;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class DockerContainerTest {


    private DockerClient dockerClient;

    private static final String CONTAINER_NAME = "test-container";

    @BeforeEach
    void setUp() {
        // Create Docker client for verification
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();
        dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    @AfterEach
    void tearDown() {
        // Clean up - try to remove the container
        try {
            dockerClient.stopContainerCmd(CONTAINER_NAME).exec();
            dockerClient.removeContainerCmd(CONTAINER_NAME).exec();
        } catch (Exception e) {
            // Ignore exceptions during cleanup
        }
    }


    @Test
    void testHttpdContainer() throws InterruptedException {

        // Create DockerContainer with busybox image
        DockerImage image = new DockerImage("busybox");
        image.deleteIfExists();

        /*
         * Container
         */
        DockerContainer.Conf conf = DockerContainer.createConf(image)
                .setContainerName(CONTAINER_NAME);

        Integer startedPort = Oss.getRandomAvailablePort();
        Path baseBusyBoxHostPath = Paths.get("src/test/resources/busybox");
        Path wwwHostPath = baseBusyBoxHostPath.resolve("var/www");
        if (!Files.exists(wwwHostPath)) {
            throw new RuntimeException("The www host path does not exists");
        }
        String verboseOutput = "-v";
        String runInForeground = "-f";
        final String ENV_NAME = "ONE";
        final String ENV_VALUE = "two";
        conf
                .setPortBinding(startedPort, 80)
                .setEnv(ENV_NAME, ENV_VALUE)
                .setVolumes(wwwHostPath, Paths.get("/var/www/"))
                .setCommand("httpd", runInForeground, verboseOutput, "-h", "/var/www/");
        DockerContainer dockerContainer = conf.build();

        if (dockerContainer.exists()) {
            dockerContainer.rm();
        }

        // Create Docker client for verification
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();
        dockerClient = DockerClientImpl.getInstance(config, httpClient);

        if (dockerContainer.isRunning()) {
            dockerContainer.stop();
            dockerContainer.rm();
        }

        // Run the container
        dockerContainer.run();

        // Verify container is running
        // Wait for the container to fully initialize with polling
        int timeoutToStartStop = 3000;
        assertTimeoutPreemptively(Duration.ofMillis(timeoutToStartStop), () -> {
            while (!dockerContainer.isRunning()) {
                //noinspection BusyWait
                Thread.sleep(100);
            }
        });

        // Run it again, should not throw any errors
        assertDoesNotThrow(dockerContainer::run, "Running an already running container should not throw errors");

        // Make HTTP request to verify environment variable
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + startedPort + "/cgi-bin/print-env.cgi?name=" + ENV_NAME))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), "HTTP response status code should be 200");
            assertEquals(ENV_VALUE, response.body().trim(), "Environment variable " + ENV_NAME + " should have value " + ENV_VALUE);
        } catch (IOException e) {
            fail("HTTP request failed: " + e.getMessage());
        }

        // Stop the container
        dockerContainer.stop();

        // Wait for the container to stop with polling
        // Wait for the container to fully initialize with polling
        assertTimeoutPreemptively(Duration.ofMillis(timeoutToStartStop), () -> {
            while (dockerContainer.isRunning()) {
                //noinspection BusyWait
                Thread.sleep(100);
            }
        });

        // Verify container is stopped
        assertFalse(dockerContainer.isRunning(), "Container should be stopped after stop() call");

        // Remove the container
        dockerContainer.rm();

        // Verify container is removed
        assertThrows(NotFoundException.class, () -> dockerClient.inspectContainerCmd(CONTAINER_NAME).exec(),
                "Container should be removed after rm() call");
    }

    @EnabledIfSystemProperty(named = "run-slow-tests", matches = "true")
    @Tag("slow")
    @Test
    void testSqlServerContainer() throws SQLException {

        final String IMAGE_NAME = "mcr.microsoft.com/mssql/server:2022-CU19-ubuntu-22.04";

        Integer startedPort = Oss.getRandomAvailablePort();
        Map<String, String> envs = new HashMap<>();
        envs.put("ACCEPT_EULA", "Y");
        String password = "TheSecret1!";
        envs.put("MSSQL_SA_PASSWORD", password);
        DockerContainer dockerContainer = DockerContainer.createConf(IMAGE_NAME)
                .setContainerName(CONTAINER_NAME)
                .setPortBinding(startedPort, 1433)
                .setEnvs(envs)
                .build();

        if (dockerContainer.isRunning()) {
            dockerContainer.stop();
        }
        if (dockerContainer.exists()) {
            dockerContainer.rm();
        }


        // Run the container
        dockerContainer.run();

        // Verify container is running
        // Wait for the container to fully initialize with polling
        int timeoutToStartStop = 10000; // startup time is around 5
        assertTimeoutPreemptively(Duration.ofMillis(timeoutToStartStop), () -> {
            while (!dockerContainer.isRunning()) {
                //noinspection BusyWait
                Thread.sleep(100);
            }
        });


        String connectionUrl = "jdbc:sqlserver://localhost:" + startedPort + ";encrypt=true;trustServerCertificate=true";

        AtomicReference<Connection> atomicConnection = new AtomicReference<>(null);

        assertTimeoutPreemptively(Duration.ofMillis(timeoutToStartStop), () -> {
            while (atomicConnection.get() == null) {
                try {
                    atomicConnection.set(DriverManager.getConnection(connectionUrl, "sa", password));
                } catch (SQLException e) {
                    //noinspection BusyWait
                    Thread.sleep(1000);
                }
            }
        });
        // Create the connection
        Connection connection = atomicConnection.get();
        assertTrue(connection.isValid(timeoutToStartStop));
        connection.close();


        // Stop the container
        dockerContainer.stop();

        // Wait for the container to stop with polling
        // Wait for the container to fully initialize with polling
        assertTimeoutPreemptively(Duration.ofMillis(timeoutToStartStop), () -> {
            while (dockerContainer.isRunning()) {
                //noinspection BusyWait
                Thread.sleep(100);
            }
        });

        // Verify container is stopped
        assertFalse(dockerContainer.isRunning(), "Container should be stopped after stop() call");

        // Remove the container
        dockerContainer.rm();

        // Verify container is removed
        assertThrows(NotFoundException.class, () -> dockerClient.inspectContainerCmd(CONTAINER_NAME).exec(),
                "Container should be removed after rm() call");
    }
}
