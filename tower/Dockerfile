# https://how-to.vertx.io/executable-jar-docker-howto/
ARG APP_NAME=eraldy-api
ARG PROJECT_DIR=tower
ARG PROJECT_NAME=tower

# 1st Docker build stage: build the project with Gradle
FROM gradle:7.5.1-jdk11 as builder
ARG PROJECT_DIR
WORKDIR /project
COPY . /project
RUN --mount=type=cache,target=/root/.gradle \
    cd ${PROJECT_DIR} && \
    gradle shadowJar --no-daemon


# Use a minimal base image for the final stage
FROM openjdk:11-jre-slim
ARG PROJECT_DIR
ARG PROJECT_NAME

# Set the working directory
WORKDIR /eraldy-api

ENV PROJECT_ARCHIVE=${PROJECT_NAME}-all.jar

# Copy the built application from the builder stage
COPY --from=builder /project/${PROJECT_DIR}/build/libs/${PROJECT_ARCHIVE} ./${PROJECT_ARCHIVE}

EXPOSE 8083

# Set the command
CMD java -jar ${PROJECT_ARCHIVE}
