# Docker build stage: copy builder output and configure entry point
FROM adoptopenjdk:11-jre-hotspot
ENV APP_DIR /application
ENV APP_NAME smtp-server
# uber/fat jar
ENV APP_FILE  bytle-${APP_NAME}-all.jar

EXPOSE 25

RUN echo "==> Update"  && \
    apt-get update && \
    echo "==> Install Dig"  && \
    # added because it's used in private network dns query
    apt-get install -y bind9-dnsutils

COPY ./build/libs/$APP_FILE $APP_DIR/$APP_FILE
COPY ./deploy/inbox.eraldy.com/.smtp-server-inbox.yml $APP_DIR/.$APP_NAME.yml

WORKDIR $APP_DIR
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -jar $APP_FILE"]
