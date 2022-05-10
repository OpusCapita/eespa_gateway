## using multistage docker build for speed
## temp container to build
FROM openjdk:8 AS TEMP_BUILD_IMAGE

ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME

COPY . $APP_HOME


RUN chmod +x ./gradlew
RUN ./gradlew -q build || return 0

## actual container
FROM openjdk:8
LABEL author="Dusty Murray <Dusty.Murray@opuscapita.com>"

# https://github.com/prometheus/jmx_exporter
ADD https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_javaagent/0.14.0/jmx_prometheus_javaagent-0.14.0.jar /opt/jmx_prometheus_javaagent.jar
COPY jmx_exporter_config.yml /opt/
RUN chmod 777 /opt/jmx_prometheus_javaagent.jar

## setting heap size automatically to the container memory limits
ENV JAVA_OPTS="\
 -XX:+UseContainerSupport\
 -XX:MaxRAMPercentage=75.0\
 -XshowSettings:vm\
 -javaagent:/opt/jmx_prometheus_javaagent.jar=3077:/opt/jmx_exporter_config.yml"

ENV APP_HOME=/usr/app/
WORKDIR $APP_HOME


COPY --from=TEMP_BUILD_IMAGE $APP_HOME/build/libs/eespa-gateway.jar .

HEALTHCHECK --interval=15s --timeout=30s --start-period=40s --retries=15 \
  CMD curl --silent --fail http://localhost:3076/api/health/check || exit 1

EXPOSE 3076
EXPOSE 3077
ENTRYPOINT exec java $JAVA_OPTS -jar eespa-gateway.jar
