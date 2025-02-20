FROM --platform=$BUILDPLATFORM amazoncorretto:17 AS builder

WORKDIR /build

COPY . .

ENV _BUILD_CONTEXT="docker"

RUN ./gradlew  # keep this as a separate code for caching
RUN ./gradlew build bootJar


FROM --platform=$TARGETPLATFORM amazoncorretto:17-alpine

WORKDIR /run
COPY --from=builder /build/build/libs/boot.jar /run/boot.jar

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c"]
CMD ["java ${JAVA_OPTS} -jar /run/boot.jar"]
