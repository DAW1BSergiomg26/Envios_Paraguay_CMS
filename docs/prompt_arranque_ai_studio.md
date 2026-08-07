# Prompt de Arranque para Google AI Studio

> Pegar como primer mensaje en AI Studio **después** de subir `docs/CONTEXTO_COMPLETO_PROYECTO.md`.

```
Trabajá sobre el proyecto "Envios_Paraguay_CMS" (MONTEASTUR ENVIOS), cuya documentación
completa acabas de recibir como archivo adjunto (CONTEXTO_COMPLETO_PROYECTO.md).

CONFIRMA antes de responder: leíste el documento completo y tenés claro el stack,
la arquitectura y las reglas del proyecto. Si tenés dudas sobre algo, preguntámelas
antes de proponer cambios.

REGLA ORO: la fuente de verdad es el código + pom.xml (Spring Boot 3.5.16, Java 17),
NO el README. Nunca asumas versiones ni dependencias sin verificarlas en el repo.

RESTRICCIONES INQUIETABLES:
1. Java puro: sin Lombok, sin @Autowired en campos, inyección por constructor (private final).
2. Migraciones solo con Flyway (V{N}__descripcion.sql, InnoDB, utf8mb4). Mantener
   data/schema.sql sincronizado con las migraciones.
3. El SPA React NO usa JWT: usa la cookie de sesión de Spring Security. No cambiar este modelo.
4. Nunca regenerar design-system.css desde cero (es un ensamblado; ver docs/handoff.md §14).
5. Nunca exponer secret_token, contraseñas ni credenciales de API en DTOs/respuestas.
6. Los listeners asíncronos (notificaciones, webhooks, batch) capturan y auditan errores
   sin romper el flujo transaccional principal.
7. Seguir TDD: toda funcionalidad nueva lleva tests (SpringBootTest/@WebMvcTest/DataJpaTest)
   con AssertJ y Awaitility para flujos asíncronos.
8. Nomenclatura en inglés para clases/métodos/variables; documentación y comentarios en español;
   sin comentarios redundantes.

CUANDO TE PIDA ALGO:
- Explicá primero tu plan técnico si la tarea cruza varias capas (Migración → Modelo →
  Repositorio → Servicio → Controller).
- Implementá en pasos pequeños y revisables.
- No hagas push ni merge de nada sin mi confirmación explícita.
- Antes de dar una tarea por terminada, indicá qué verificación corres: mvn clean test
  (BUILD SUCCESS) y, si toca frontend, npm test / npm run build en frontend-react/.

¿Entendido? Confirmá que estás listo y preguntá lo que necesites antes de empezar.
```
