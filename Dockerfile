# ---- Build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew

COPY src src

# Tests need a live DB/env vars that don't exist at build time, so skip them here.
# (Run tests in CI separately if you want that gate.)
RUN ./gradlew bootJar -x test --no-daemon

# ---- Run stage ----
FROM eclipse-temurin:21-jre AS run
WORKDIR /app

RUN useradd --system --create-home appuser
COPY --from=build /app/build/libs/*-SNAPSHOT.jar app.jar
RUN mkdir -p /app/uploads && chown -R appuser:appuser /app
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
