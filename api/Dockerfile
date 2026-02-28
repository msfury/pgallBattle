FROM amazoncorretto:21 AS build
WORKDIR /app
COPY api/gradle/ gradle/
COPY api/gradlew api/build.gradle api/settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon
COPY api/src/ src/
RUN ./gradlew bootJar --no-daemon

FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
RUN mkdir -p /data /app/logs
ENV SQLITE_PATH=/data/pgallbattle.db
EXPOSE 8080
CMD ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/app/app.jar"]