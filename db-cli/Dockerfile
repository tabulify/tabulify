#Gradle version
ARG GRADLE_IMAGE=gradle:7.5.1-jdk11
ARG JAVA_IMAGE=openjdk:11-jre-slim
ARG PROJECT_DIR=db-cli
ARG PROJECT_NAME=tabli

# Build
FROM ${GRADLE_IMAGE} AS builder
ARG PROJECT_DIR
WORKDIR /app
COPY . /app
RUN --mount=type=cache,target=/root/.gradle \
    cd ${PROJECT_DIR} && \
    gradle shadowJar --no-daemon


# Run
FROM ${JAVA_IMAGE}
ARG PROJECT_NAME
ARG PROJECT_DIR
# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the builder stage to the current stage
# app.jar to keep it general. We can't use a variable in the entrypoint
COPY --from=builder /app/${PROJECT_DIR}/build/libs/${PROJECT_NAME}-all.jar app.jar

# Specify the entrypoint to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
