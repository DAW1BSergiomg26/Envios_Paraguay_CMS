# Handoff 2 — Envios_Paraguay_CMS

Fecha: 2026-08-04

Este documento resume el estado real del proyecto, lo que ya se ha cambiado, por qué se cambió, qué quedó verificado y qué debe saber cualquier agente que continúe desde aquí (incluido OpenCode).

---

## 1. Objetivo del trabajo realizado

El objetivo principal fue modernizar el runtime del proyecto para dejarlo operativo con Java 25 y asegurar que la aplicación y su suite de tests puedan arrancar correctamente bajo ese runtime.

No se trató de agregar nuevas funciones de negocio. Se trató de:

- dejar el proyecto compatible con Java 25,
- actualizar el stack base de Spring Boot y runtime,
- alinear Docker, CI/CD y documentación con ese nuevo runtime,
- y corregir el problema de bootstrap de tests que impedía arrancar el contexto de Spring con MySQL/Redis.

---

## 2. Estado actual del proyecto

### Stack actual

- Java: 25
- Spring Boot: 3.5.16
- Build tool: Maven Wrapper
- Base de datos: MySQL 8
- Cache/sesiones: Redis 7
- Frontend: Thymeleaf + Bootstrap 5
- Arquitectura: Spring Boot + Spring Data JPA + Flyway + Security + Redis

### Estado verificado

Se verificó que el proyecto:

- compila correctamente con Java 25,
- arranca el contexto de Spring para tests,
- y pasa la suite completa de pruebas.

### Verificación ejecutada y resultado

Comandos usados:

```powershell
./mvnw.cmd -q -DskipTests compile
./mvnw.cmd test
```

Resultado confirmado:

- Build: SUCCESS
- Tests: 235 ejecutados
- Failures: 0
- Errors: 0
- Skipped: 0

---

## 3. Cambios realizados y por qué

### 3.1 Upgrade a Java 25

Cambios hechos:

- Se ajustó el target de Java en pom.xml a 25.

Por qué:

- El objetivo del trabajo era modernizar el runtime a la última LTS relevante y dejar el proyecto preparado para ese entorno.

Archivos implicados:

- pom.xml

---

### 3.2 Upgrade de Spring Boot

Cambios hechos:

- Se actualizó Spring Boot de 3.3.5 a 3.5.16 en pom.xml.

Por qué:

- Mantener compatibilidad con Java 25 y evitar incompatibilidades de runtime y compilación.

Archivos implicados:

- pom.xml

---

### 3.3 Ajuste del runtime de contenedores

Cambios hechos:

- Se actualizó Dockerfile para usar imágenes Temurin 25 tanto en build como en runtime.

Por qué:

- El proyecto debía construir y ejecutarse con el mismo runtime que el de desarrollo/CI.

Archivos implicados:

- Dockerfile

---

### 3.4 Ajuste de Docker Compose para pruebas y desarrollo local

Cambios hechos:

- Se añadió la exposición del puerto 3306 para MySQL para que el host pueda llegar a la base de datos desde los tests.
- Se configuró MYSQL_ROOT_HOST="%" para permitir conexiones remotas desde la máquina host.
- Se mantuvieron los servicios MySQL y Redis operativos para que el arranque de pruebas funcione correctamente.

Por qué:

- El problema inicial no era de Java sino de conectividad de la base de datos durante el bootstrap de Spring en tests.
- Los tests necesitan que MySQL sea accesible desde el proceso de Maven/Java que ejecuta las pruebas.

Archivos implicados:

- docker-compose.yml

---

### 3.5 Ajuste de la configuración de tests

Cambios hechos:

- Se actualizó src/test/resources/application-test.properties para usar variables configurables de host/puerto/base de datos.
- La configuración actual usa placeholders como:
  - DB_HOST
  - DB_PORT
  - DB_NAME
  - DB_USERNAME
  - DB_PASSWORD

Por qué:

- Esto permite que los tests funcionen tanto en Docker como en un entorno local sin depender de una única forma fija de URL.

Archivos implicados:

- src/test/resources/application-test.properties

---

### 3.6 Corrección del bootstrap de Spring en tests

Problema detectado:

- Los tests fallaban al arrancar el contexto de Spring porque el proceso de pruebas no podía conectar correctamente a MySQL durante Flyway/JPA initialization.

Solución aplicada:

- Se implementó un normalizador de propiedades para corregir URLs JDBC que llegaban con separadores de propiedades incompatibles.
- Se añadió un EnvironmentPostProcessor que aplica el ajuste lo antes posible en el arranque.

Archivos implicados:

- src/main/java/com/monteastur/envios/config/BootstrapPropertyNormalizer.java
- src/main/java/com/monteastur/envios/config/BootstrapPropertyEnvironmentPostProcessor.java
- src/main/resources/META-INF/spring.factories
- src/test/java/com/monteastur/envios/config/BootstrapPropertyNormalizerTest.java

Por qué era importante:

- Esto resolvió el bloqueo real que impedía que la suite de integración arrancara correctamente después del upgrade.

---

### 3.7 CI/CD alineado con Java 25

Cambios hechos:

- Se actualizó la pipeline de CI para usar Java 25.
- Se mantuvo la ejecución de tests con MySQL y Redis como servicios del workflow.

Archivos implicados:

- .github/workflows/ci.yml
- .github/workflows/deploy-koyeb.yml
- .github/workflows/deploy-prod.yml

---

## 4. Contexto técnico importante para el siguiente agente

### 4.1 Regla de oro del proyecto

- No introducir Lombok.
- Mantener inyección por constructor.
- Mantener el enfoque en Java puro para entidades, DTOs, repositorios y servicios.
- Respetar el estilo y arquitectura del proyecto existente.

### 4.2 Reglas de arquitectura ya asumidas

- Spring Boot + Spring Data JPA + Flyway.
- MySQL como base de datos principal.
- Redis para cache/sesiones.
- Tests de integración usando Spring Boot Test.
- El arranque de tests depende de que los servicios de MySQL y Redis estén accesibles.

### 4.3 Qué NO debe romperse

- La compatibilidad con Java 25.
- El ajuste de bootstrap de datasource/Redis.
- La configuración de Docker Compose para que MySQL y Redis estén accesibles.
- La compatibilidad con el perfil de tests.

---

## 5. Configuración actual recomendada para ejecutar tests localmente

### Variables de entorno recomendadas

```powershell
$env:JAVA_HOME='C:\Users\astur\.jdks\openjdk-25.0.2'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
$env:SPRING_PROFILES_ACTIVE='test'
$env:SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='root'
```

### Servicios requeridos

- MySQL en localhost:3306
- Redis en localhost:6379

Si se usan los servicios de Docker Compose, deben estar levantados antes de correr tests.

---

## 6. Qué está funcionando hoy

### Funciona correctamente

- Compilación bajo Java 25
- Arranque de la aplicación en el perfil test
- Suite completa de tests
- Integración con MySQL y Redis desde el contexto de pruebas

### Lo que ya quedó resuelto

- Problema de bootstrap del contexto de Spring
- Problema de conectividad de MySQL para tests
- Alineación de runtime y build con Java 25

---

## 7. Recomendación para el siguiente agente

El proyecto ya está en un estado estable y verificado. El siguiente agente debe asumir que:

- el upgrade a Java 25 ya quedó aplicado,
- los tests pasan,
- y la prioridad ahora es continuar con nuevas tareas de negocio o refactorización sin romper esta base ya validada.

Si vas a trabajar en nuevas funcionalidades, hazlo con cuidado y siempre verifica:

1. que sigan pasando los tests,
2. que no se rompa la compatibilidad con Java 25,
3. que la configuración de datasource/Redis siga funcionando.

---

## 8. Resumen corto para pegar en OpenCode

Proyecto Spring Boot + Maven ya modernizado a Java 25 y Spring Boot 3.5.16. El runtime, Docker, CI/CD y documentación quedaron alineados. Se corrigió además el bootstrap de tests para que la aplicación pueda arrancar correctamente con MySQL/Redis en el perfil test. La verificación actual confirma que la suite completa pasa: 235 tests, 0 failures, 0 errors, 0 skipped.

Si continúas con nuevas tareas, parte de este estado ya validado y no vuelvas a reintroducir cambios de runtime sin verificar tests.
