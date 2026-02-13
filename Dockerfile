FROM  --platform=$BUILDPLATFORM eclipse-temurin:21-jdk AS builder
COPY . /workspace
RUN cd /workspace && echo "build project" && ./gradlew clean waldot-app:generateGitProperties waldot-app:distTar -Dorg.gradle.daemon=false

FROM eclipse-temurin:21-jre-alpine AS initial
RUN apk update
RUN apk upgrade
RUN mkdir -p /app
RUN mkdir -p /app/help
COPY ./docker/HELP.txt /app/help/index.txt
RUN echo "Andrea Ambrosini - Rossonet s.c.a r.l." > /app/help/author.txt
COPY ./LICENSE /app/help/license.txt
COPY --from=builder /workspace/waldot-app/build/distributions/*.tar /tmp/
RUN tar -xf /tmp/*.tar -C ./app --strip-components=1 && rm -rf /tmp/*.tar

FROM eclipse-temurin:21-jre-alpine
ENTRYPOINT ["java"]
CMD ["-cp","/app/lib/*","-XX:+UnlockExperimentalVMOptions","-Djava.net.preferIPv4Stack=true","-XshowSettings:vm","-Djava.security.egd=file:/dev/./urandom", "net.rossonet.agent.MainAgent"]
EXPOSE 12686
COPY --from=initial / /
