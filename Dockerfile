# =============================================
# MONTEASTUR ENVIOS — Dockerfile (multi-stage)
# =============================================
# Build:        docker build -t monteastur-app .
# Run:          docker run -p 8080:8080 monteastur-app
# Compose:      docker compose up -d
# =============================================

# ---- Stage 1: Build frontend ----
FROM node:20-alpine AS frontend
WORKDIR /frontend
COPY frontend-react/package.json frontend-react/package-lock.json ./
RUN npm install
COPY frontend-react/ .
ENV VITE_START_URL=/login-react
ENV VITE_BASE=/react-dashboard/
RUN npm run build

# ---- Stage 2: Build backend ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline -B
COPY src ./src
COPY --from=frontend /frontend/dist ./src/main/resources/static/react-dashboard/
RUN --mount=type=cache,target=/root/.m2 mvn package -DskipTests -q

# ---- Stage 3: Runtime ----
FROM eclipse-temurin:17-jre

# OCI labels
LABEL org.opencontainers.image.title="Monteastur Envios"
LABEL org.opencontainers.image.description="Plataforma log\u00EDstica Espa\u00F1a \u2194 Paraguay"
LABEL org.opencontainers.image.version="3.2"
LABEL org.opencontainers.image.authors="Grupo B2"
LABEL org.opencontainers.image.vendor="Monteastur"
LABEL org.opencontainers.image.created=""

# Install curl for the container healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create non-root user and writable directories
RUN useradd -m appuser && \
    mkdir -p /app/uploads /app/logs && \
    chown -R appuser:appuser /app

USER appuser
WORKDIR /app

COPY --from=build /build/target/*.jar app.jar
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD ["sh", "-c", "curl -fsS \"http://localhost:${PORT:-8080}/actuator/health\" || exit 1"]
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]
