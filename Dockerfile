FROM ubuntu:20.04 as builder
RUN apt update && DEBIAN_FRONTEND=noninteractive apt install -y openjdk-11-jdk
COPY . /workspace
RUN cd /workspace && echo "build project" && ./gradlew clean distTar

FROM eclipse-temurin:17-jdk-jammy
ENTRYPOINT ["java"]
CMD ["-cp","/app/lib/*","-XX:+UnlockExperimentalVMOptions","-Djava.net.preferIPv4Stack=true","-XshowSettings:vm","-Djava.security.egd=file:/dev/./urandom", "net.rossonet.agent.MainAgent"]
RUN mkdir -p /app
COPY --from=builder /workspace/build/distributions/*.tar /tmp/
RUN tar -xf /tmp/*.tar -C ./app --strip-components=1 && rm -rf /tmp/*.tar
