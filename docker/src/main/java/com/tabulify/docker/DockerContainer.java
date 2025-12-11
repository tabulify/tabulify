package com.tabulify.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;

import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A Docker container class to start, stop, and run Docker containers
 */
public class DockerContainer {

    private static final Logger logger = Logger.getLogger(DockerContainer.class.getName());

    private final Conf conf;
    private final DockerClient dockerClient;

    public DockerContainer(Conf conf) {
        this.conf = conf;
        // Configure and build Docker client for version 3.5.0
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();
        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    /**
     * Check if a container exists
     *
     * @return true if the container exists, false otherwise
     */
    public boolean exists() {
        String containerName = getName();
        try {
            dockerClient.inspectContainerCmd(containerName).exec();
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }

    /**
     * Check if a container is running
     *
     * @return true if the container is running, false otherwise
     */
    public boolean isRunning() {
        String containerName = getName();
        try {
            InspectContainerResponse containerInfo = dockerClient.inspectContainerCmd(containerName).exec();
            return Boolean.TRUE.equals(containerInfo.getState().getRunning());
        } catch (NotFoundException e) {
            return false;
        }
    }

    /**
     * Run the container
     * - If container exists, start it
     * - If container doesn't exist, create and run it
     */
    public void run() {
        String containerName = getName();

        if (isRunning()) {
            return;
        }

        if (exists()) {
            // Container exists, start it if not running
            try {
                dockerClient.startContainerCmd(containerName).exec();
                logger.info(() -> "Container " + containerName + " started");
            } catch (Exception e) {
                throw new RuntimeException("Failed to start container: " + e.getMessage());
            }
            return;
        }

        // Container doesn't exist, create and run it
        createAndRunContainer();

    }

    /**
     * Create and run a new container based on DockerContainer configuration
     */
    private void createAndRunContainer() {

        DockerImage image = getImage();
        if (!image.exists()) {
            image.pull();
        }
        String containerName = getName();
        Map<Integer, Integer> portMap = getPortBinding();
        Map<Path, Path> volumeMap = getVolumeBinding();

        try {
            // Prepare port bindings
            List<PortBinding> portBindings = new ArrayList<>();
            List<ExposedPort> exposedPorts = new ArrayList<>();

            for (Map.Entry<Integer, Integer> entry : portMap.entrySet()) {
                Integer hostPort = entry.getKey();
                Integer containerPort = entry.getValue();

                ExposedPort exposedPort = ExposedPort.tcp(containerPort);
                exposedPorts.add(exposedPort);
                portBindings.add(new PortBinding(Ports.Binding.bindPort(hostPort), exposedPort));
            }

            // Prepare volume bindings
            List<Bind> binds = new ArrayList<>();
            for (Map.Entry<Path, Path> entry : volumeMap.entrySet()) {
                Path hostPath = entry.getKey();
                Path containerPath = entry.getValue();

                Volume volume = new Volume(containerPath.toString());
                binds.add(new Bind(hostPath.toString(), volume));
            }

            // Create host config with port and volume bindings
            HostConfig hostConfig = new HostConfig()
                    .withPortBindings(portBindings)
                    .withBinds(binds);

            // Create container
            CreateContainerCmd createContainerCmd = dockerClient.createContainerCmd(image.getImageReference())
                    .withName(containerName)
                    .withHostConfig(hostConfig)
                    .withEnv(getEnvs().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.toList()))
                    .withExposedPorts(exposedPorts);

            // Add command if specified
            if (!conf.command.isEmpty()) {
                createContainerCmd.withCmd(conf.command);
            }

            CreateContainerResponse container = createContainerCmd.exec();

            // Start container
            dockerClient.startContainerCmd(container.getId()).exec();
            logger.info(() -> "Container " + containerName + " created and started");

        } catch (Exception e) {
            throw new RuntimeException("Failed to create and run container: " + this + ". Error: " + e.getMessage(), e);
        }
    }

    /**
     * Remove the container if it exists
     */
    public void rm() {
        String containerName = getName();

        if (!exists()) {
            logger.info(() -> "Container " + containerName + " does not exist");
            return;
        }

        if (isRunning()) {
            stop();
        }

        // Container exists, remove it
        try {
            dockerClient.removeContainerCmd(containerName).exec();
            logger.info(() -> "Container " + containerName + " removed");
        } catch (Exception e) {
            throw new RuntimeException("Failed to remove container: " + e.getMessage());
        }

    }


    /**
     * Stop the container
     * No error will be thrown if the container exists or does not run
     *
     * @throws RuntimeException if the container cannot be stopped
     */
    public void stop() {
        String containerName = getName();

        if (!exists()) {
            return;
        }

        if (!isRunning()) {
            return;
        }

        try {
            dockerClient.stopContainerCmd(containerName).exec();
            logger.info(() -> "Container " + containerName + " stopped");
        } catch (Exception e) {
            throw new RuntimeException("Failed to stop container: " + this + "," + e.getMessage(), e);
        }

    }


    /**
     * @return the shell command to run in dos and bash format
     */
    public String createDockerCommand() {
        String windowsLineSeparator = "^" + System.lineSeparator();
        String bashLineSeparator = "\\" + System.lineSeparator();
        List<String> separators = Arrays.asList(windowsLineSeparator, bashLineSeparator);
        String spaces = "    ";

        StringBuilder stringBuilder = new StringBuilder();
        for (String separator : separators) {
            stringBuilder.append(System.lineSeparator());
            if (separator.equals(windowsLineSeparator)) {
                stringBuilder.append("Cmd:").append(System.lineSeparator());
            } else {
                stringBuilder.append("Bash:").append(System.lineSeparator());
            }
            stringBuilder.append("docker run ").append(separator);
            for (Map.Entry<String, String> env : conf.envs.entrySet()) {
                stringBuilder.append(spaces).append("-e ")
                        .append(env.getKey())
                        .append("=")
                        .append(env.getValue())
                        .append(" ")
                        .append(separator);
            }
            for (Map.Entry<Path, Path> volumeMapEntry : conf.volumeMap.entrySet()) {
                // https://docs.docker.com/engine/storage/bind-mounts/#syntax
                String hostPath = volumeMapEntry.getKey().toAbsolutePath().toString();
                String containerPath = volumeMapEntry.getValue().toAbsolutePath().toString();
                stringBuilder.append(spaces).append("--volume ").append(hostPath).append(":").append(containerPath).append(" ").append(separator);
            }
            for (Map.Entry<Integer, Integer> portMapEntry : conf.portMap.entrySet()) {
                stringBuilder.append(spaces).append("-p ").append(portMapEntry.getKey()).append(":").append(portMapEntry.getValue()).append(" ").append(separator);
            }
            stringBuilder
                    .append(spaces).append("-d ").append(separator)
                    .append(spaces).append("--name ").append(conf.containerName).append(" ").append(separator)
                    .append(spaces).append(conf.image.getImageReference()).append(System.lineSeparator());
        }

        return stringBuilder.toString();

    }

    /**
     * @return a port binding map where the first integer is the host port and the second is the container port
     * when the container is started, the container port is the public port and the host port is the private port
     * In a Docker command, it will become `-p hostPort:containerPort`
     */
    public Map<Integer, Integer> getPortBinding() {
        return conf.portMap;
    }

    /**
     * @return a volume binding map where the first path is the host path and the second is the container path
     * In a Docker command, it will become `--volume hostPath:containerPath`
     */
    public Map<Path, Path> getVolumeBinding() {
        return conf.volumeMap;
    }

    /**
     * @return the os envs
     */
    public Map<String, String> getEnvs() {
        return conf.envs;
    }

    /**
     * @return the DockerImage object
     * It's the image parameter in a Docker command.
     * For instance, for a run, it's the `IMAGE` in `docker run [OPTIONS] IMAGE [COMMAND] [ARG...]`
     */
    public DockerImage getImage() {
        return conf.image;
    }

    /**
     * @return the container name
     * It's the name parameter in a Docker command.
     * For instance, for a run, it's the `NAME` in `docker run --name NAME`
     */
    public String getName() {
        return conf.containerName;
    }

    public static Conf createConf(String image) {
        return new Conf(new DockerImage(image));
    }

    public static Conf createConf(DockerImage image) {
        return new Conf(image);
    }


    @SuppressWarnings("unused")
    public static class Conf {

        private String containerName = "test-container";
        private final DockerImage image;
        private Map<Integer, Integer> portMap = new HashMap<>();
        private Map<String, String> envs = new HashMap<>();
        private Map<Path, Path> volumeMap = new HashMap<>();
        private List<String> command = new ArrayList<>();

        public Conf(DockerImage image) {
            this.image = image;
        }

        public Conf setContainerName(String containerName) {
            this.containerName = containerName;
            return this;
        }

        public Conf setPortBinding(Integer hostPort, Integer containerPort) {
            this.portMap.put(hostPort, containerPort);
            return this;
        }

        public Conf setPortBindings(Map<Integer, Integer> hostToContainersPortMap) {
            this.portMap.putAll(hostToContainersPortMap);
            return this;
        }

        public Conf setVolumes(Path hostPath, Path containerPath) {
            if (!containerPath.isAbsolute()) {
                throw new RuntimeException("The container path " + containerPath + " is not an absolute path");
            }
            this.volumeMap.put(hostPath.toAbsolutePath(), containerPath);
            return this;
        }

        public Conf setVolumeBindings(Map<Path, Path> volumeBindings) {
            this.volumeMap.putAll(volumeBindings);
            return this;
        }

        public Conf setCommand(List<String> command) {
            this.command = command;
            return this;
        }

        public Conf setCommand(String... command) {
            setCommand(Arrays.asList(command));
            return this;
        }

        public Conf setEnv(String key, String value) {
            this.envs.put(key, value);
            return this;
        }

        public Conf setEnvs(Map<String, String> envs) {
            this.envs.putAll(envs);
            return this;
        }

        public DockerContainer build() {
            return new DockerContainer(this);
        }


    }
}
