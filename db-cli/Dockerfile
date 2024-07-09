#Gradle version
ARG GRADLE_IMAGE
ARG JDK_IMAGE
ARG PROJECT_DIR

# Use the specified Gradle version to build the application
FROM ${GRADLE_IMAGE} AS builder
ARG PROJECT_DIR
WORKDIR /app
COPY . /app
RUN --mount=type=cache,target=/root/.gradle \
    cd ${PROJECT_DIR} && \
    gradle shadowJar --no-daemon


# Use a minimal base image for running the application
FROM ${GRADLE_IMAGE}
ARG PROJECT_DIR
ARG PROJECT_NAME

# Set the working directory inside the container
WORKDIR /app

ENV PROJECT_ARCHIVE=${PROJECT_NAME}-all.jar

# Copy the JAR file from the builder stage to the current stage
COPY --from=builder /app/build/libs/*.jar app.jar

# Specify the command to run the application
CMD ["java", "-jar", "app.jar"]
