#Gradle version
ARG GRADLE_IMAGE=gradle:7.5.1-jdk11
ARG JAVA_IMAGE=openjdk:11-jre-slim
ARG PROJECT_DIR=db-cli
ARG JAR_NAME=tabul-all.jar

# Build
FROM ${GRADLE_IMAGE} AS builder
ARG PROJECT_DIR
WORKDIR /app
COPY . /app
RUN --mount=type=cache,target=/root/.gradle \
    cd ${PROJECT_DIR} && \
    gradle shadowJar tabulShadowStartScripts --no-daemon


# Last
FROM ${JAVA_IMAGE}
ARG JAR_NAME
ARG PROJECT_DIR


RUN apt-get update \
    # Install Git to be able to do a git pull
    && apt-get install -y  git \
    # Install jq and curl for scripting
    && apt-get install -y jq curl

# The env name `APP_HOME` is used in the executable script
ENV APP_HOME=/tabul

# Copy the JAR file from the builder stage to the current stage
COPY --from=builder /app/${PROJECT_DIR}/build/libs/${JAR_NAME} ${APP_HOME}/lib/${JAR_NAME}

# Executable Script
COPY --from=builder /app/${PROJECT_DIR}/build/scripts/shadow/tabul ${APP_HOME}/bin/tabul
RUN chmod +x /${APP_HOME}/bin/tabul
ENV PATH="${APP_HOME}/bin:${PATH}"

# Create a host user `1000` called `me` for convenience when using WSL
# ie the UID 1000 is assigned to first non-root user
# Why? The user id created on Linux systems starts from 1000
# It permits to mount ssh keys and other asset in the home directory
RUN addgroup --gid 1000 megroup && \
    adduser --uid 1000 --gid 1000 --shell /bin/bash me

# Specify the default command to run
CMD ["tabul"]
