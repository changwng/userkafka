# 빌드 스테이지
FROM gradle:8.3-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle build -x test

# 런타임 스테이지
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"] 