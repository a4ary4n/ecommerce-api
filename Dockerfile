# ---- build stage ----
FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# gradle files + wrapper copied first so dependency resolution is cached in its
# own layer, separate from source - only re-runs when build.gradle.kts changes
COPY gradlew build.gradle.kts settings.gradle.kts ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies > /dev/null || true

COPY src src
# bootJar does NOT depend on `test` in the Spring Boot Gradle plugin, so tests
# don't run during the image build - correct, since the one existing test
# (@SpringBootTest context-load) needs a live MySQL/ES connection that isn't
# reachable during `docker build` anyway.
RUN ./gradlew --no-daemon bootJar

# ---- runtime stage ----
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
