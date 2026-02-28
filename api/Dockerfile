FROM amazoncorretto:25 AS build
WORKDIR /app
COPY gradle/ gradle/
COPY gradlew build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon
COPY src/ src/
RUN ./gradlew bootJar --no-daemon

FROM amazoncorretto:25-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
RUN mkdir -p /data /app/logs
ENV SQLITE_PATH=/data/pgallbattle.db
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
