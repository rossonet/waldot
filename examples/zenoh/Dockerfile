FROM rust:latest AS builder
RUN apt-get update
RUN apt-get upgrade -y
RUN apt-get install -y git cmake clang build-essential libc6-dev pkg-config
WORKDIR /src
RUN git clone --depth 1 https://github.com/rossonet/zenoh
RUN git clone --depth 1 https://github.com/rossonet/zenoh-backend-filesystem
RUN git clone --depth 1 https://github.com/rossonet/zenoh-backend-rocksdb
RUN git clone --depth 1 https://github.com/rossonet/zenoh-backend-influxdb
RUN git clone --depth 1 https://github.com/rossonet/zenoh-plugin-mqtt
RUN for file in $(find "/src" -type f -print0 | xargs -0 grep -Il "https://github.com/eclipse-zenoh"); do \
    sed -i "s#https://github.com/eclipse-zenoh#https://github.com/rossonet#g" "$file"; \
    echo "aggiornato $file"; done
RUN cd zenoh && \
    cargo build --release
RUN cd zenoh-backend-filesystem && \
    cargo build --release
RUN cd zenoh-backend-rocksdb && \
    cargo build --release
RUN cd zenoh-backend-influxdb && \
    cargo build --release
RUN cd zenoh-plugin-mqtt && \
    cargo build --release

FROM debian:trixie-slim AS base
RUN apt update
RUN DEBIAN_FRONTEND=noninteractive apt upgrade -y
RUN DEBIAN_FRONTEND=noninteractive apt install libc6 -y
RUN DEBIAN_FRONTEND=noninteractive apt-get clean && rm -rf /var/cache/apt/archives /var/lib/apt/lists/*
COPY --from=builder /src/zenoh/target/release/zenohd /usr/local/bin/
COPY --from=builder /src/zenoh/target/release/deps/*.so /usr/local/lib/
RUN chmod +x /usr/local/bin/zenohd
RUN ls -lh /usr/local/bin
RUN mkdir -p /root/.zenoh/lib
COPY --from=builder /src/zenoh-backend-filesystem/target/release/*.so /usr/local/lib/
COPY --from=builder /src/zenoh-backend-filesystem/target/release/deps/*.so /usr/local/lib/
COPY --from=builder /src/zenoh-backend-rocksdb/target/release/*.so /usr/local/lib/
COPY --from=builder /src/zenoh-backend-rocksdb/target/release/deps/*.so /usr/local/lib/
COPY --from=builder /src/zenoh-backend-influxdb/target/release/*.so /usr/local/lib/
COPY --from=builder /src/zenoh-backend-influxdb/target/release/deps/*.so /usr/local/lib/
COPY --from=builder /src/zenoh-plugin-mqtt/target/release/*.so /usr/local/lib/
COPY --from=builder /src/zenoh-plugin-mqtt/target/release/deps/*.so /usr/local/lib/
RUN mkdir -p /config
COPY zenoh.json /config/zenoh.json

FROM debian:trixie-slim
COPY --from=base / /
ENV ZENOH_BACKEND_FS_ROOT=/store
ENV RUST_LOG=info
ENV RUST_BACKTRACE=full
EXPOSE 7447 8000
ENTRYPOINT ["/bin/sh","-c","/usr/local/bin/zenohd --config=/config/zenoh.json --rest-http-port=8000"]
