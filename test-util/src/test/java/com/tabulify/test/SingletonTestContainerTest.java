package com.tabulify.test;


import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class SingletonTestContainerTest {

    @Test
    public void databaseCommandTest() {
        if (CiDetector.isRunningInCI()) {
            // 100Mb
            return;
        }
        SingletonTestContainer sqlServer = new SingletonTestContainer("sqlserver", "postgres:17-alpine")
                .withEnv("ACCEPT_EULA", "Y")
                .withEnv("SA_PASSWORD", "TheSecret1!")
                .withPort(1433);
        System.out.println(sqlServer.createDockerCommand());
    }

    @Test
    public void httpPrivilegedPortTest() throws IOException {
        Path path = Paths.get("target", "httpBinWorkdir");
        Files.createDirectories(path);
        SingletonTestContainer httpBin = new SingletonTestContainer("httpbin", "kennethreitz/httpbin:latest")
                .withBindMount(path, "/workdir")
                .withPort(80);
        System.out.println(httpBin.createDockerCommand());
        httpBin
                .startContainer()
                .stop();

        httpBin = new SingletonTestContainer("httpbin", "kennethreitz/httpbin:latest")
                .withBindMount(path, "/workdir")
                .withPort(8081, 80);
        System.out.println(httpBin.createDockerCommand());
        httpBin.startContainer().stop();

    }
}
