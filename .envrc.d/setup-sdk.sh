# SDKMAN
# loading, as it's as the sdk command is a bash function
export SDKMAN_DIR=${SDKMAN_DIR:-"$HOME/.sdkman"}
SDKMAN_INIT_FILE="${SDKMAN_DIR}/bin/sdkman-init.sh"
if [[ ! -s "$SDKMAN_INIT_FILE" ]]; then
  echo "sdkman init file was not found and is mandatory (Path: $SDKMAN_INIT_FILE)"
  echo "You can"
  echo "  * install sdkman"
  echo "  * or overwrite the <.sdkman> dir by setting the SDKMAN_DIR env"
  return 1
fi
# We disable `set -o nounset` to avoid unbound variable errors
set +u
# shellcheck disable=SC1090
source "${SDKMAN_INIT_FILE}"

# Note sdkman vs nix (May be devbox?)
# We don't use nix because the jdk11 package distro is difficult to know and the version to pinpoint
# FYI:
# Nix: OpenJDK Runtime Environment (build 11.0.25+0-adhoc..source)
# Temurin: OpenJDK Runtime Environment Temurin-11.0.26+4 (build 11.0.26+4)

# Java Temurin
# We also download it with maven, may be we could download it once
export JDK_VERSION
JDK_VERSION=$(yq --exit-status '.project.properties."jdk.version"' pom.xml)
export JRELEASER_PROJECT_LANGUAGES_JAVA_VERSION=${JDK_VERSION}
export JDK_DISTRIBUTION
JDK_DISTRIBUTION=$(yq --exit-status '.project.properties."jdk.distribution"' pom.xml | cut -c 1-3)
SDKMAN_JDK_VERSION="${JDK_VERSION}-${JDK_DISTRIBUTION}"
if ! sdk home java "${SDKMAN_JDK_VERSION}" >/dev/null; then
  sdk install java "${SDKMAN_JDK_VERSION}"
fi
sdk use java "${SDKMAN_JDK_VERSION}"

###################
# Mavens
###################
SDKMAN_MAVEN_VERSION=3.9.9
if ! sdk home maven "${SDKMAN_MAVEN_VERSION}" >/dev/null; then
  sdk install maven "${SDKMAN_MAVEN_VERSION}"
fi
sdk use maven "${SDKMAN_MAVEN_VERSION}"

###################
# JReleaser
# JReleaser is integrated and called from maven
# ie: mvnw jreleaser:config
###################

# Cli tabul install (Used by install-cli and doc-exec)
# Cli because TABUL_HOME depends on the context
# In ide development, TABUL_HOME is the ide project root
# In cli,             TABUL_HOME is the cli installation root
export CLI_TABUL_HOME=/opt/tabulify
# Path Should be last in path because tabul comes with a JRE java
# so that we use the jdk of sdkman
export PATH=$PATH:$CLI_TABUL_HOME/bin


# Utility Script
export PATH=$PWD/contrib/script:$PATH

# Java Envs
# Date from java util takes into account the default timezone
# https://learn.microsoft.com/en-us/java/openjdk/timezones#setting-the-tz-environment-variable
export TZ=UTC
# By default, we run in the terminal as local installation
# So no env


# We change the user home so that the documentation has a consistent value across the dev environment (laptops, ci/cd)
export TABUL_OS_USER_HOME=/home/tabulify
if [ ! -d $TABUL_OS_USER_HOME ]; then
  echo "Os User home does not exist. Creating $TABUL_OS_USER_HOME with Sudo"
  sudo mkdir -p $TABUL_OS_USER_HOME
  sudo chown "$USER" "$TABUL_OS_USER_HOME"
fi
export TABUL_USER_HOME="$TABUL_OS_USER_HOME"/.tabul
if [ ! -d $TABUL_USER_HOME ]; then
  echo "Tabul User home does not exist. Creating $TABUL_USER_HOME"
  mkdir -p $TABUL_USER_HOME
fi


###################
# Git
###################

# Used by git-cliff
# https://git-cliff.org/docs/integration/github
# Should move in a wrapper
export GIT_REPO=tabulify/tabulify
export GITHUB_REPO=${GIT_REPO}
