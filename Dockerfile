# =============================================
# MONTEASTUR ENVIOS — Dockerfile (multi-stage)
# =============================================
# Build:        docker build -t monteastur-app .
# Run:          docker run -p 8080:8080 monteastur-app
# Compose:      docker compose up -d
# =============================================

# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -q

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=prod"]
