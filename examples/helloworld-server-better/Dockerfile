FROM saturnism/kotlin-native:0.7 AS build
RUN apt-get update && apt-get install -y \
    libevent-dev \
    libssl-dev \
    libevhtp-dev \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir -p /src && mkdir -p /target

WORKDIR /src

ADD c_interop /src/c_interop
RUN cinterop -def c_interop/evhtp.def -o evhtp
RUN cinterop -def c_interop/event.def -o event

ADD main /src/main
RUN kotlinc main/ -l evhtp -l event -opt -o /app

FROM gcr.io/distroless/cc
COPY --from=build /usr/lib/x86_64-linux-gnu/libevhtp.so* /usr/lib/
COPY --from=build /usr/lib/x86_64-linux-gnu/libevent*.so* /usr/lib/
COPY --from=build /usr/lib/x86_64-linux-gnu/libssl.so* /usr/lib/
COPY --from=build /usr/lib/x86_64-linux-gnu/libcrypto.so* /usr/lib/
COPY --from=build /usr/lib/x86_64-linux-gnu/libonig.so* /usr/lib/
COPY --from=build /app.kexe /app
ENTRYPOINT ["/app"]
