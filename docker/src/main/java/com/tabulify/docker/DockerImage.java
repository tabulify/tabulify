package com.tabulify.docker;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;


import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A Docker image class that handles OCI-compliant image references
 * Format: [<registry>/][<project>/]<image>[:<tag>|@<digest>]
 */
public class DockerImage {

    private static final Logger logger = Logger.getLogger(DockerImage.class.getName());

    private final String imageReference;
    private final String registry;
    private final String project;
    private final String imageName;
    private final String tag;
    private final String digest;
    private final DockerClient dockerClient;

    // Regex pattern for OCI image reference parsing
    // [<registry>/][<project>/]<image>[:<tag>|@<digest>]
    private static final Pattern IMAGE_PATTERN = Pattern.compile(
            "^(?:([^/]+(?:\\.[^/]+)*(?::[0-9]+)?)/)?(?:([^/]+)/)?([^/:@]+)(?::([^@]+)|@(.+))?$"
    );

    /**
     * Constructor that parses an OCI-compliant image reference
     *
     * @param imageReference the image reference in OCI format
     */
    public DockerImage(String imageReference) {
        this.imageReference = imageReference;

        // Parse the image reference
        Matcher matcher = IMAGE_PATTERN.matcher(imageReference);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid image reference format: " + imageReference);
        }

        // Apply defaults for optional components
        this.registry = matcher.group(1) != null && matcher.group(2) != null ? matcher.group(1) : null;
        this.project = matcher.group(2) != null ? matcher.group(2) : matcher.group(1);
        this.imageName = matcher.group(3);
        this.tag = matcher.group(4) != null ? matcher.group(4) : (matcher.group(5) == null ? "latest" : null);
        this.digest = matcher.group(5);

        // Initialize Docker client
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();
        this.dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    /**
     * Static factory method to create a DockerImage instance
     *
     * @param imageReference the image reference in OCI format
     * @return a new DockerImage instance
     */
    public static DockerImage create(String imageReference) {
        return new DockerImage(imageReference);
    }

    /**
     * Check if the image exists locally
     *
     * @return true if the image exists locally, false otherwise
     */
    public boolean exists() {
        try {
            return dockerClient.listImagesCmd()
                    .exec()
                    .stream()
                    .anyMatch(image -> {
                        if (image.getRepoTags() != null) {
                            return Arrays.stream(image.getRepoTags())
                                    .anyMatch(repoTag -> repoTag.equals(imageReference) ||
                                            repoTag.equals(getCanonicalName()));
                        }
                        if (image.getRepoDigests() != null && digest != null) {
                            return Arrays.stream(image.getRepoDigests())
                                    .anyMatch(repoDigest -> repoDigest.equals(imageReference));
                        }
                        return false;
                    });
        } catch (Exception e) {
            throw new RuntimeException("Failed to check if image exists: " + e.getMessage(), e);
        }
    }

    /**
     * Delete the image from local Docker
     *
     * @throws RuntimeException if the image cannot be deleted
     */
    public void delete() {
        try {
            dockerClient.removeImageCmd(imageReference).withForce(true).exec();
            logger.info(() -> "Deleted image: " + imageReference);
        } catch (NotFoundException e) {
            logger.warning("Image " + imageReference + " not found");
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete image " + imageReference + ": " + e.getMessage(), e);
        }
    }

    /**
     * Delete the image from local Docker only if it exists
     * This method will not throw exceptions if the image doesn't exist
     *
     * @return true if the image was deleted, false if it didn't exist
     */
    public boolean deleteIfExists() {
        if (!exists()) {
            logger.info(() -> "Image " + imageReference + " does not exist locally");
            return false;
        }

        try {
            dockerClient.removeImageCmd(imageReference).withForce(true).exec();
            logger.info(() -> "Deleted image: " + imageReference);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete image " + imageReference + ": " + e.getMessage(), e);
        }
    }

    /**
     * Pull the image from the registry
     *
     * @throws RuntimeException if the image cannot be pulled
     */
    public void pull() {
        // We don't use the image string given in the constructor but the canonical one because the tag default is
        // platform dependent
        // For instance, busybox will become docker.io/library/busybox:1-ubuntu
        // and, we get the following error because the image is not in a good format
        //
        // Failed to pull image (busybox): Could not pull image: [DEPRECATION NOTICE]
        // Docker Image Format v1 and Docker Image manifest version 2, schema 1 support is disabled by default
        // and will be removed in an upcoming release.
        // Suggest the author of docker.io/library/busybox:1-ubuntu
        // to upgrade the image to the OCI Format or Docker Image manifest v2, schema 2.
        // More information at https://docs.docker.com/go/deprecated-image-specs/
        String canonicalName = this.getCanonicalName();
        try {
            logger.info(() -> "Pulling image: " + canonicalName);
            dockerClient.pullImageCmd(canonicalName).start().awaitCompletion();
        } catch (Exception e) {
            throw new RuntimeException("Failed to pull image (" + canonicalName + "): " + e.getMessage(), e);
        }
    }

    /**
     * Delete all images with the same name as this image, regardless of tag
     * This will remove all versions/tags of the image
     */
    public void deleteAllImagesWithoutTag() {
        String imageName = getImageNameWithoutTag();

        try {
            // List all images and filter by name (without tag)
            dockerClient.listImagesCmd()
                    .exec()
                    .stream()
                    .filter(image -> image.getRepoTags() != null)
                    .filter(image -> Arrays.stream(image.getRepoTags())
                            .anyMatch(tag -> tag.startsWith(imageName + ":")))
                    .forEach(image -> {
                        try {
                            dockerClient.removeImageCmd(image.getId()).withForce(true).exec();
                            logger.info(() -> "Deleted image: " + Arrays.toString(image.getRepoTags()));
                        } catch (Exception e) {
                            logger.severe(() -> "Failed to delete image " + Arrays.toString(image.getRepoTags()) + ": " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete images for " + imageName + ": " + e.getMessage(), e);
        }
    }

    /**
     * List all available local images without tags
     * Returns a list of unique image names (without tags) that are available locally
     *
     * @return a list of image names without tags
     * @throws RuntimeException if unable to list local images
     */
    public static List<String> listLocal() {
        DockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();
        DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

        try {
            Set<String> imageNamesWithoutTag = new HashSet<>();

            dockerClient.listImagesCmd()
                    .exec()
                    .stream()
                    .filter(image -> image.getRepoTags() != null)
                    .forEach(image -> {
                        Arrays.stream(image.getRepoTags())
                                .filter(repoTag -> !repoTag.equals("<none>:<none>"))
                                .forEach(repoTag -> {
                                    // Extract image name without tag
                                    String nameWithoutTag = repoTag.contains(":") ?
                                            repoTag.substring(0, repoTag.lastIndexOf(":")) : repoTag;
                                    imageNamesWithoutTag.add(nameWithoutTag);
                                });
                    });

            return imageNamesWithoutTag.stream()
                    .sorted()
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("Failed to list local images: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to extract image name without tag from this image
     *
     * @return the image name without tag
     */
    private String getImageNameWithoutTag() {
        StringBuilder nameWithoutTag = new StringBuilder();

        if (registry != null) {
            nameWithoutTag.append(registry).append("/");
        }
        if (project != null) {
            nameWithoutTag.append(project).append("/");
        }
        nameWithoutTag.append(imageName);

        return nameWithoutTag.toString();
    }

    /**
     * Get the canonical name of the image (with default registry and tag if not specified)
     *
     * @return the canonical image name
     */
    public String getCanonicalName() {
        StringBuilder canonical = new StringBuilder();

        // Add registry (default to docker.io if not specified)
        canonical.append(Objects.requireNonNullElse(registry, "docker.io"));
        canonical.append("/");

        // Add project (default to library if not specified and registry is docker.io)
        if (project != null) {
            canonical.append(project);
        } else if (registry == null || registry.equals("docker.io")) {
            canonical.append("library");
        }
        canonical.append("/");

        // Add image name
        canonical.append(imageName);

        // Add tag or digest
        if (digest != null) {
            canonical.append("@").append(digest);
        } else if (tag != null) {
            canonical.append(":").append(tag);
        } else {
            canonical.append(":latest");
        }

        return canonical.toString();
    }

    /**
     * Get the full image reference as provided in constructor
     *
     * @return the full image reference
     */
    public String getImageReference() {
        return imageReference;
    }

    /**
     * Get the registry part of the image reference
     *
     * @return the registry, or null if not specified
     */
    public String getRegistry() {
        return registry;
    }

    /**
     * Get the project part of the image reference
     *
     * @return the project, or null if not specified
     */
    public String getProject() {
        return project;
    }

    /**
     * Get the image name part of the image reference
     *
     * @return the image name
     */
    public String getImageName() {
        return imageName;
    }

    /**
     * Get the tag part of the image reference
     *
     * @return the tag, or null if not specified or if digest is used
     */
    public String getTag() {
        return tag;
    }

    /**
     * Get the digest part of the image reference
     *
     * @return the digest, or null if not specified
     */
    public String getDigest() {
        return digest;
    }

    @Override
    public String toString() {
        return "DockerImage{" +
                "fullImageReference='" + imageReference + '\'' +
                ", registry='" + registry + '\'' +
                ", project='" + project + '\'' +
                ", imageName='" + imageName + '\'' +
                ", tag='" + tag + '\'' +
                ", digest='" + digest + '\'' +
                '}';
    }
}
