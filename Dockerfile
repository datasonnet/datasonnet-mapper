FROM openjdk:8-jre-alpine
VOLUME /tmp

ENV _JAVA_OPTIONS "-Djava.awt.headless=true"
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/opt/datasonnet/app.jar"]


RUN addgroup datasonnet && \
    adduser -D -S -h /var/cache/datasonnet -s /sbin/nologin -G datasonnet datasonnet
USER datasonnet
WORKDIR /var/cache/datasonnet

ARG JAR_FILE
COPY ${JAR_FILE} /opt/datasonnet/app.jar
