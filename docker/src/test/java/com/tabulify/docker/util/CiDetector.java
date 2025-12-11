package com.tabulify.docker.util;

public class CiDetector {
  public static boolean isRunningInCI() {
    // Check for common CI environment variables
    String[] ciEnvVars = {
      "CI",                  // GitHub Actions, CircleCI, Travis CI, etc.
      "JENKINS_URL",         // Jenkins
      "GITLAB_CI",           // GitLab CI
      "TEAMCITY_VERSION",    // TeamCity
      "TRAVIS",              // Travis CI
      "CIRCLECI",            // CircleCI
      "GITHUB_ACTIONS",      // GitHub Actions
      "BUILDKITE",           // Buildkite
      "DRONE",               // Drone CI
      "APPVEYOR"             // AppVeyor
    };

    for (String var : ciEnvVars) {
      if (System.getenv(var) != null) {
        return true;
      }
    }

    return false;
  }

  public static String detectCIProvider() {
    if (System.getenv("GITHUB_ACTIONS") != null) return "GitHub Actions";
    if (System.getenv("JENKINS_URL") != null) return "Jenkins";
    if (System.getenv("GITLAB_CI") != null) return "GitLab CI";
    if (System.getenv("TRAVIS") != null) return "Travis CI";
    if (System.getenv("CIRCLECI") != null) return "CircleCI";
    if (System.getenv("TEAMCITY_VERSION") != null) return "TeamCity";
    if (System.getenv("BUILDKITE") != null) return "Buildkite";
    if (System.getenv("DRONE") != null) return "Drone CI";
    if (System.getenv("APPVEYOR") != null) return "AppVeyor";
    if (System.getenv("CI") != null) return "Unknown CI";

    return "Not in CI";
  }
}
