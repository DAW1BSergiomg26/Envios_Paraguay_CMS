# HANDOFF — Envios_Paraguay_CMS

**Fecha:** 2026-08-04  
**Objetivo principal:** modernizar el runtime de Java a Java 25 y dejar el proyecto alineado con ese runtime en build, Docker, CI/CD y documentación.

---

## Resumen ejecutivo

Se ha realizado una actualización del stack de ejecución del proyecto para dejarlo compatible con Java 25. La aplicación sigue siendo un proyecto Spring Boot + Maven, pero ahora el runtime de compilación y ejecución está alineado con Java 25 y Spring Boot 3.5.x.

### Estado actual

- Build principal: compilación verificada con Maven bajo Java 25.
- Docker: imagen de build/ejecución actualizada a Temurin 25.
- CI/CD: workflows de GitHub Actions actualizados para usar Java 25.
- Tests: la compilación y el arranque de la aplicación van bien, pero la suite completa sigue necesitando el entorno de prueba de MySQL/Redis disponible para completar el bootstrap de Spring.

---

## Cambios realizados

### 1) Upgrade de Java y runtime

- Se cambió el target de compilación de Java 17 a Java 25 en [pom.xml](pom.xml).
- Se actualizó la configuración del proyecto para que Maven use un runtime compatible con Java 25.

### 2) Upgrade del framework base

- Se actualizó Spring Boot desde 3.3.5 a 3.5.16 en [pom.xml](pom.xml) para alinearlo con el runtime Java 25.
- Esto es clave para evitar incompatibilidades de compilación y runtime con la nueva versión de Java.

### 3) Ajustes de contenedores y despliegue

- Se actualizó [Dockerfile](Dockerfile) para usar imágenes Temurin 25 tanto en build como en runtime.
- Se ajustó la configuración de despliegue en [docker-compose.yml](docker-compose.yml) para que siga siendo consistente con el nuevo runtime.

### 4) CI/CD

- Se actualizaron los workflows de GitHub Actions para usar Java 25 en:
  - [.github/workflows/ci.yml](.github/workflows/ci.yml)
  - [.github/workflows/deploy-koyeb.yml](.github/workflows/deploy-koyeb.yml)
  - [.github/workflows/deploy-prod.yml](.github/workflows/deploy-prod.yml)

### 5) Documentación

- Se actualizaron referencias de Java en [README.md](README.md) y [AGENTS.md](AGENTS.md).
- Se creó el seguimiento del upgrade en [.github/modernize/java-upgrade/20260804155425/plan.md](.github/modernize/java-upgrade/20260804155425/plan.md), [.github/modernize/java-upgrade/20260804155425/progress.md](.github/modernize/java-upgrade/20260804155425/progress.md) y [.github/modernize/java-upgrade/20260804155425/summary.md](.github/modernize/java-upgrade/20260804155425/summary.md).

---

## Verificación realizada

### ✅ Confirmado

- Compilación del proyecto con Maven bajo Java 25:
  - Comando: `./mvnw.cmd clean test-compile -q`
- Build de contenedor con imagen Maven Temurin 25.

### ⚠️ Pendiente / bloqueante de entorno

- El arranque completo de la suite de tests alcanza la inicialización de Spring, pero sigue necesitando que el entorno de pruebas de MySQL y Redis esté correctamente levantado y accesible.
- El problema actual no es de compilación del código Java, sino de bootstrap del contexto de la aplicación en pruebas.

---

## Commit realizado

- Commit: `d63e1b3796705c026a50ea39b72ecb06f8622ca0`
- Rama: `appmod/java-upgrade-20260804155425`

---

## Qué debe saber Gemini o el siguiente agente

- El objetivo del cambio fue pasar el proyecto a Java 25, no añadir nuevas funcionalidades.
- La actualización fue principalmente de infraestructura, runtime, compilación, contenedores y CI/CD.
- El proyecto sigue funcionando en desarrollo local, pero la validación completa de tests necesita el entorno de base de datos y Redis correctamente preparado.
- Si el siguiente paso es completar la validación de tests, lo prioritario es revisar la configuración de datasource y Redis para el perfil de pruebas.

---

## Resumen corto para copiar/pegar

Se ha actualizado el proyecto Envios_Paraguay_CMS a Java 25. Se cambió el target de Maven en [pom.xml](pom.xml), se actualizó Spring Boot a 3.5.16, se pasó Docker y CI/CD a Temurin 25, y se actualizaron las referencias de documentación. La compilación bajo Java 25 está verificada. La suite completa de tests sigue necesitando el entorno de MySQL/Redis para completar el bootstrap de Spring.
