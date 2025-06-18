# STAGE 1: Build a JAR file with JDK 21
FROM gradle:jdk21 AS builder
WORKDIR /build
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle bootJar

# STAGE 2: Create a final image with JRE 21
FROM amazoncorretto:21-alpine-jdk
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar ./app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]