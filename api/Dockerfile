FROM amazoncorretto:25 AS build
WORKDIR /app
COPY api/gradle/ gradle/
COPY api/gradlew api/build.gradle api/settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon
COPY api/src/ src/
RUN ./gradlew bootJar --no-daemon

FROM amazoncorretto:25-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
COPY api/entrypoint.sh /app/entrypoint.sh
COPY pgallbattle.db /app/pgallbattle.db.default
RUN mkdir -p /data /app/logs && chmod +x /app/entrypoint.sh
ENV SQLITE_PATH=/data/pgallbattle.db
EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]