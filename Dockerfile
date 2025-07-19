# 빌드
FROM gradle:jdk21 AS builder
WORKDIR /build
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle build # 테스트 포함

# 최종 이미지 생성
FROM amazoncorretto:21-alpine-jdk
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar ./app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]