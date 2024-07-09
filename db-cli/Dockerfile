#Gradle version
ARG GRADLE_VERSION

# Use the specified Gradle version to build the application
FROM gradle:${GRADLE_VERSION}-jdk11 AS builder
ARG PROJECT_DIR
WORKDIR /project
COPY . /project
RUN --mount=type=cache,target=/root/.gradle \
    cd ${PROJECT_DIR} && \
    gradle shadowJar --no-daemon


# Use a minimal base image for running the application
FROM openjdk:11-jre-slim

# Set the working directory inside the container
WORKDIR /app

# Copy the JAR file from the builder stage to the current stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Specify the command to run the application
CMD ["java", "-jar", "app.jar"]
