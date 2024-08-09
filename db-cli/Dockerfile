#Gradle version
ARG GRADLE_IMAGE=gradle:7.5.1-jdk11
ARG JAVA_IMAGE=openjdk:11-jre-slim
ARG PROJECT_DIR=db-cli
ARG JAR_NAME=tabli-all.jar

# Build
FROM ${GRADLE_IMAGE} AS builder
ARG PROJECT_DIR
WORKDIR /app
COPY . /app
RUN --mount=type=cache,target=/root/.gradle \
    cd ${PROJECT_DIR} && \
    gradle shadowJar tabliShadowStartScripts --no-daemon


# Run
FROM ${JAVA_IMAGE}
ARG JAR_NAME
ARG PROJECT_DIR

# Install Git to be able to do a git pull
RUN apt-get update && apt-get install -y git

# The env name `APP_HOME` is used in the executable script
ENV APP_HOME=/tabli

# Copy the JAR file from the builder stage to the current stage
COPY --from=builder /app/${PROJECT_DIR}/build/libs/${JAR_NAME} ${APP_HOME}/lib/${JAR_NAME}

# Executable Script
COPY --from=builder /app/${PROJECT_DIR}/build/scripts/shadow/tabli ${APP_HOME}/bin/tabli
RUN chmod +x /${APP_HOME}/bin/tabli
ENV PATH="${APP_HOME}/bin:${PATH}"

# Specify the entrypoint to run the application
# We can't use a variable in the entrypoint
ENTRYPOINT ["tabli"]
