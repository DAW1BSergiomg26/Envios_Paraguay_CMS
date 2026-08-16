# AGENTS.md — Envios_Paraguay_CMS

Guía de trabajo para agentes de IA y desarrolladores en este repositorio. Establece las reglas, arquitectura y estándares del proyecto.

## 🌐 Contexto del Proyecto

- **Stack tecnológico:** Spring Boot 3.3.5 + Thymeleaf + Spring Data JPA + Hibernate + MySQL 8 + Bootstrap 5.
- **Versión de Java:** **Java 25** (verificado en `pom.xml`; se compila con `maven:3.9-eclipse-temurin-25`). Inyección de dependencias por constructor, cero `@Autowired` en campos.
- **Color corporativo / identidad:** `#d4762a`.
- **Arquitectura de eventos y concurrencia:**
  - Despacho de eventos de dominio transaccionales (`@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`).
  - Procesamiento asíncrono no bloqueante mediante pools de hilos dedicados (`@Async`) bajo transacciones independientes (`Propagation.REQUIRES_NEW`).
- **Seguridad e integración:**
  - Firma digital criptográfica obligatoria (`HMAC-SHA256` usando `HexFormat` de Java 17).
  - Clientes HTTP robustos con control estricto de timeouts (`RestClient`).

## 🚨 Reglas Globales Inquebrantables (Backend & Enterprise)

### 1. Restricciones de Dominio y Modelado

- **PROHIBIDO LOMBOK:** Todas las entidades JPA, modelos de dominio, repositorios y DTOs se escriben estrictamente en **Java puro**: atributos privados, constructor vacío (obligatorio para JPA), constructores con parámetros y todos sus getters/setters manuales.
- **Inyección por constructor:** campos de servicios, repositorios y componentes declarados como `private final` e inicializados en el constructor.

### 2. Gestión de Migraciones y Datos

- Migraciones de esquema exclusivamente con **Flyway** (`V{N}__descripcion.sql`), integridad referencial estricta, motor InnoDB y codificación UTF-8 (`utf8mb4`).
- Las relaciones entre entidades respetan las restricciones de borrado en cascada y claves foráneas definidas a nivel de base de datos.

### 3. Calidad, Testing y Resiliencia

- **TDD:** toda nueva funcionalidad o servicio crítico lleva su test de unidad e integración (`SpringBootTest`, `DataJpaTest`, `@WebMvcTest`), con AssertJ y Awaitility para flujos asíncronos.
- **Cero excepciones silenciadas:** los listeners asíncronos o integraciones (p. ej. webhooks) capturan y auditan fallos en tablas dedicadas (`*_logs`), sin romper el flujo transaccional principal del cliente.
- **Seguridad en DTOs:** los campos sensibles (`secret_token`, contraseñas, credenciales de API) **nunca** se exponen en respuestas REST ni DTOs administrativos.

## 🔄 Protocolo de Ejecución Técnica

1. **Pre-flight scan:** auditar paquetes y beans existentes antes de añadir código nuevo.
2. **Plan arquitectónico:** exponer el diseño técnico si la tarea cruza varias capas (Migración → Modelo → Repositorio → Servicio → Listener/Controller).
3. **Implementación limpia:** código robusto, documentado en español, con nomenclatura en inglés para clases, métodos y variables. Sin comentarios redundantes.
4. **Validación y verificación:** compilar y ejecutar pruebas con Maven (`mvn clean test`) garantizando `BUILD SUCCESS` antes de dar por terminada una tarea.

## 🧪 Comandos de Verificación

- Compilar y testear localmente (requiere JDK 17 + Maven): `C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd clean test`
- Suite completa en contenedor (con `db` y `redis` levantados en `envios_paraguay_cms_backend`):

  ```powershell
  docker run --rm -v "${PWD}:/app" -w /app --network envios_paraguay_cms_backend `
    -e SPRING_DATASOURCE_URL="jdbc:mysql://db:3306/envios_paraguay_cms_test?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true" `
    -e DB_USERNAME=root -e DB_PASSWORD=root -e SPRING_DATA_REDIS_HOST=redis `
    -e SPRING_MAIL_HOST=mailpit `
    -v "${HOME}\.m2:/root/.m2" maven:3.9-eclipse-temurin-25 mvn clean test
  ```

## 📌 Estado y Flujo de Trabajo

- Rama estable: `main`. No hacer push ni merge sin confirmación explícita del usuario.
- Cambios pequeños y revisables; no mezclar mejoras no relacionadas en un solo commit.
- Estado de avance detallado en `docs/handoff.md`.
