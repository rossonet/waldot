FROM ubuntu:24.04 AS builder
RUN apt update && DEBIAN_FRONTEND=noninteractive apt install -y openjdk-21-jdk
COPY . /workspace
RUN cd /workspace && echo "build project" && ./gradlew clean :waldot-app:distTar

FROM eclipse-temurin:21-jre
ENTRYPOINT ["java"]
CMD ["-cp","/app/lib/*","-XX:+UnlockExperimentalVMOptions","-Djava.net.preferIPv4Stack=true","-XshowSettings:vm","-Djava.security.egd=file:/dev/./urandom", "net.rossonet.agent.MainAgent"]
RUN mkdir -p /app
COPY --from=builder /workspace/waldot-app/build/distributions/*.tar /tmp/
RUN tar -xf /tmp/*.tar -C ./app --strip-components=1 && rm -rf /tmp/*.tar
