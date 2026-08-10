# Plan de Implementación — Arquitectura de Interfaces Thymeleaf + React (P2.2)

**Fecha:** 2026-08-10
**Repositorio:** `DAW1BSergiomg26/Envios_Paraguay_CMS`
**Rama base:** `main`
**Spec de referencia:** `docs/superpowers/specs/2026-08-10-arquitectura-interfaces-thymeleaf-react-design.md` (commit `3744d20`)

---

## 1. Resumen

Ejecuta el hito **P2.2** del backlog de hardening (arquitectura híbrida Thymeleaf + SPA React,
Enfoque A aprobado): documentar la matriz oficial/legacy/complementaria, redirigir los flujos
de login admin y dashboard al panel React, y señalar visualmente el CMS como interfaz
heredada mediante un banner autocontenido.

**Decisiones de alcance aprobadas en sesión (confirmadas el 2026-08-10):**

1. **Solo banner autocontenido.** El banner `.legacy-banner` lleva su propio CSS (padding,
   fondo, bordes, tipografía) **sin depender** de `.sidebar`, `.nav-links`, `.main-content`,
   `.btn-logout` ni `.logout-form`.
2. **Regresión del sidebar documentada, no reparada.** Los estilos del sidebar del CMS fueron
   eliminados en el commit `e72def6` (0 matches en `design-system.css`, 3995 líneas). Esta
   regresión previa se documenta como hallazgo en este plan y en el backlog, **fuera del
   alcance de P2.2**.
3. La SPA React (`/dashboard`) es el panel admin oficial; el CMS Thymeleaf (`/admin/**`) es
   legacy en migración (se mantiene accesible).

---

## 2. Hallazgos de la pre-flight scan (investigación previa al plan)

| # | Hallazgo | Impacto |
|---|---|---|
| H1 | `SecurityConfig` línea 47: `defaultSuccessUrl("/admin/dashboard")` | Debe ser `/react-dashboard/`. |
| H2 | `LoginController.login()` (18 líneas) no inyecta `Authentication` | Debe comprobar `ROLE_ADMIN` y redirigir. |
| H3 | `AdminController.dashboard()` (líneas 86–95) usa `Model` y devuelve `cms/dashboard` | Debe devolver `redirect:/react-dashboard/`; se elimina el bloque `model.addAttribute`. |
| H4 | No existe `LoginControllerTest` | Se crea nuevo con T1.1–T1.4. |
| H5 | `AdminControllerTest` y `SecurityConfigTest` usan `@WebMvcTest` + `@Import(SecurityConfig.class)` + mocks `DataSource`, `RBACAccessLogger`, `CustomAccessDeniedHandler` + `@TestPropertySource` | Patrón a replicar. |
| H6 | `RBACAccessLogger` es clase plana con constructor `JdbcTemplate` (no `@Component`); `CustomAccessDeniedHandler` es `@Component` | Ambos se mockean en `@WebMvcTest`. |
| H7 | `admin-sidebar.html` es el fragment heredado por todas las páginas `cms/*.html` | Punto único de inserción del banner. |
| H8 | **Regresión:** `design-system.css` (3995 líneas) no contiene `.sidebar`, `.nav-links`, `.main-content`, `.btn-logout`, `.logout-form` (borrados en `e72def6`) | El banner debe ser autocontenido; el sidebar roto es hallazgo documentado, no se repara. |
| H9 | CSS referenciado vía `th:href="@{/css/design-system.css(v=${@environment.getProperty('app.web.css-version')})}"` en `fragments/header.html` (`cms-head`) | `.legacy-banner` se añade a `design-system.css`. |
| H10 | `SpaForwardControllerTest` (3/3 verdes) verifica `/login-react`, `/dashboard`, `/dashboard/envio/MT-1` → `forward:/react-dashboard/index.html` | No debe romperse. |

---

## 3. Estrategia de ejecución (TDD estricto)

Secuencia **Red → Green → Refactor**, con un commit por unidad lógica. Los tests que no
dependen de la implementación se escriben primero y se verifica que **fallen** por la razón
correcta antes de tocar producción.

| Fase | Contenido | Verificación |
|---|---|---|
| **F1 (Red)** | Tests nuevos/actualizados de routing (Login, Admin, Security) | `mvn test -Dtest=LoginControllerTest,AdminControllerTest,SecurityConfigTest,SpaForwardControllerTest` → **fallan** T1.2, T2.1, T3.1 |
| **F2 (Green)** | `SecurityConfig`, `LoginController`, `AdminController.dashboard` | Misma suite → **verde** |
| **F3 (Green)** | Banner `admin-sidebar.html` + `.legacy-banner` en CSS | Verificación de contenido y regresión |
| **F4 (Docs)** | `docs/ARQUITECTURA_INTERFACES.md` + backlog actualizado | Revisión de contenido |
| **F5 (Regresión)** | `mvn clean test` completo | **BUILD SUCCESS** (300 tests; 37 errores ambientales de `*IntegrationTest` requieren Docker y se validan aparte) |

---

## 4. Fase 1 — Red (tests)

### T1. `LoginControllerTest` (nuevo)

**Archivo:** `src/test/java/com/monteastur/envios/controller/LoginControllerTest.java`

**Patrón:** `@WebMvcTest(LoginController.class)` + `@Import(SecurityConfig.class)` +
`@MockBean DataSource`, `@MockBean RBACAccessLogger`, `@MockBean CustomAccessDeniedHandler` +
`@TestPropertySource(properties = {"app.admin.username=admin","app.admin.password=test","app.upload.dir=src/test/resources/uploads"})`.

| # | Test | Request | Esperado |
|---|---|---|---|
| T1.1 | `loginSinSesion_muestraTemplate` | `GET /login` anónimo | `200`, `viewName("login")` |
| T1.2 | `loginConAdmin_redirigeAlPanelReact` | `GET /login` con `@WithMockUser(roles="ADMIN")` | `302`, `redirectedUrl("/react-dashboard/")` |
| T1.3 | `loginConCliente_noDesvia_usaTemplate` | `GET /login` con `@WithMockUser(roles="CLIENTE")` | `200`, `viewName("login")` |
| T1.4 | `adminLogin_redirigeALogin` | `GET /admin/login` anónimo | `302`, `redirectedUrl("/login")` |

### T2. `AdminControllerTest` (actualización)

**Archivo:** `src/test/java/com/monteastur/envios/controller/AdminControllerTest.java`

Se añaden 3 tests a la clase existente (mocks ya presentes). Patrón de autenticación:
`.with(user("admin").roles("ADMIN"))`.

| # | Test | Request | Esperado |
|---|---|---|---|
| T2.1 | `dashboard_conSesion_redirigeAlPanelReact` | `GET /admin/dashboard` con `user("admin").roles("ADMIN")` | `302`, `redirectedUrl("/react-dashboard/")` |
| T2.2 | `dashboard_sinSesion_redirigeALogin` | `GET /admin/dashboard` anónimo (Accept TEXT_HTML) | `302`, `redirectedUrl("/login")` |
| T2.3 | `reservas_conSesion_sigueSirviendoCms` | `GET /admin/reservas` con `user("admin").roles("ADMIN")` | `200`, `viewName("cms/reservas")` |

### T3. `SecurityConfigTest` (actualización)

**Archivo:** `src/test/java/com/monteastur/envios/config/SecurityConfigTest.java`

Se añade el test T3.1 con `SecurityMockMvcRequestBuilders.formLogin` y un mock de
`JdbcUserDetailsManager` (el bean real apunta a un `DataSource` mockeado que no devuelve
usuarios; es necesario stubbear `loadUserByUsername` para que la autenticación tenga éxito).

| # | Test | Request | Esperado |
|---|---|---|---|
| T3.1 | `loginCorrecto_redirigeAlPanelReact` | `POST /login` (formLogin con credenciales válidas) | `302`, `redirectedUrl("/react-dashboard/")` |

**Detalles técnicos de T3.1:**
- `@MockBean JdbcUserDetailsManager userDetailsManager;`
- Stub: `when(userDetailsManager.loadUserByUsername("admin")).thenReturn(admin)` donde `admin`
  es `User.withUsername("admin").password(encodedPassword).roles("ADMIN").build()` con
  `encodedPassword = new BCryptPasswordEncoder().encode("test")`.
- `mockMvc.perform(formLogin("/login").user("admin").password("test"))`.

> **Nota:** se debe validar en la ejecución de la F1 que T3.1 falla en rojo (el
> `defaultSuccessUrl` actual es `/admin/dashboard`). Si la autenticación por `formLogin` no
> fuese viable en el slice, alternativa: verificar el `defaultSuccessUrl` leyendo la propiedad
> del `SecurityFilterChain` configurado, o usar `@WithMockUser` + request POST manual al
> loginProcessingUrl comprobando el redirect del éxito (se ajusta en ejecución sin alterar el
> criterio de aceptación).

### Comando de verificación F1

```powershell
& "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" test -Dtest=LoginControllerTest,AdminControllerTest,SecurityConfigTest,SpaForwardControllerTest
```

**Resultado esperado en rojo:** `T1.2`, `T2.1` y `T3.1` **fallan**; T1.1, T1.3, T1.4, T2.2,
T2.3 y los 3 tests de `SpaForwardControllerTest` **pasan** (regresión intacta). Compilación
de tests correcta.

---

## 5. Fase 2 — Green (routing)

### T4. `SecurityConfig`

**Archivo:** `src/main/java/com/monteastur/envios/config/SecurityConfig.java` (línea 47)

```java
.defaultSuccessUrl("/react-dashboard/")
```

### T5. `LoginController`

**Archivo:** `src/main/java/com/monteastur/envios/controller/LoginController.java`

```java
package com.monteastur.envios.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/react-dashboard/";
        }
        return "login";
    }

    @GetMapping("/admin/login")
    public String adminLogin() {
        return "redirect:/login";
    }
}
```

### T6. `AdminController.dashboard()`

**Archivo:** `src/main/java/com/monteastur/envios/controller/AdminController.java` (líneas 86–95)

```java
@GetMapping("/dashboard")
public String dashboard() {
    return "redirect:/react-dashboard/";
}
```

**Nota:** se elimina el parámetro `Model` y el bloque `model.addAttribute(...)` del dashboard
legacy. El resto de rutas `/admin/**` no se tocan. El `@PreAuthorize` y el
`.requestMatchers("/admin/**").authenticated()` garantizan que solo se alcanza con sesión.

### Comando de verificación F2

```powershell
& "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" test -Dtest=LoginControllerTest,AdminControllerTest,SecurityConfigTest,SpaForwardControllerTest
```

**Resultado esperado en verde:** los 11 tests del routing pasan.

### Commit

```powershell
git add docs/superpowers/plans/2026-08-10-arquitectura-interfaces-thymeleaf-react-plan.md
git add src/test/java/com/monteastur/envios/controller/LoginControllerTest.java
git add src/test/java/com/monteastur/envios/controller/AdminControllerTest.java
git add src/test/java/com/monteastur/envios/config/SecurityConfigTest.java
git add src/main/java/com/monteastur/envios/config/SecurityConfig.java
git add src/main/java/com/monteastur/envios/controller/LoginController.java
git add src/main/java/com/monteastur/envios/controller/AdminController.java
git commit -m "feat(admin): redirige login y dashboard admin al panel React (#P2.2)"
```

---

## 6. Fase 3 — Green (banner legacy)

### T7. `admin-sidebar.html` (banner)

**Archivo:** `src/main/resources/templates/fragments/admin-sidebar.html`

Insertar **tras `sidebar-header`** (antes de `<nav class="nav-links">`):

```html
<div class="legacy-banner">
    <strong>Interfaz heredada</strong>
    <span>La gestión de envíos se realiza en el <a href="/react-dashboard/">Nuevo Panel</a>.</span>
</div>
```

### T8. CSS `.legacy-banner` (autocontenido)

**Archivo:** `src/main/resources/static/css/design-system.css`

CSS autocontenido (no depende de `.sidebar` ni de las clases del sidebar legacy, ausentes por
la regresión H8), coherente con la paleta corporativa `#d4762a`:

```css
.legacy-banner {
    display: flex;
    flex-direction: column;
    gap: 4px;
    margin: 12px;
    padding: 12px 14px;
    border: 1px solid #d4762a;
    border-radius: 8px;
    background-color: #d4762a14;
    color: inherit;
    font-size: 0.85rem;
    line-height: 1.4;
}
.legacy-banner strong {
    color: #d4762a;
    font-size: 0.8rem;
    text-transform: uppercase;
    letter-spacing: 0.05em;
}
.legacy-banner a {
    color: #d4762a;
    font-weight: 600;
    text-decoration: underline;
}
```

> **Restricción de alcance:** NO se restauran `.sidebar`, `.nav-links`, `.main-content`,
> `.btn-logout` ni `.logout-form` (regresión H8 documentada en backlog, fuera de P2.2).

### Verificación de contenido (sin infraestructura)

```powershell
# 1) El fragment contiene el banner
Select-String -Path "src/main/resources/templates/fragments/admin-sidebar.html" -Pattern "legacy-banner","/react-dashboard/"
# 2) El CSS define .legacy-banner y NO redefine el sidebar (autocontenido)
Select-String -Path "src/main/resources/static/css/design-system.css" -Pattern "legacy-banner"
```

### Commit

```powershell
git add src/main/resources/templates/fragments/admin-sidebar.html
git add src/main/resources/static/css/design-system.css
git commit -m "feat(cms): banner de interfaz heredada en el sidebar del CMS (#P2.2)"
```

---

## 7. Fase 4 — Documentación y backlog

### T9. `docs/ARQUITECTURA_INTERFACES.md` (nuevo)

Contenido según spec Sección 4 (matriz oficial/legacy/complementaria, autenticación
compartida, hoja F1–F6, reglas de convivencia y referencia al backlog).

### T10. Backlog

**Archivo:** `docs/HARDENING_BACKLOG_ENVIOS_CMS.md`

- Marcar P2.2 como cerrado (estado del ítem).
- Actualizar la sección "Decision actual" para reflejar el estado real (SPA oficial, CMS
  legacy, P2.2 cerrado, P3.1/P3.2 pendientes).
- Añadir el hallazgo H8 (regresión del sidebar en `design-system.css` desde `e72def6`) como
  ítem pendiente/observación para un futuro bloque de pulido visual.

### Commit

```powershell
git add docs/ARQUITECTURA_INTERFACES.md
git add docs/HARDENING_BACKLOG_ENVIOS_CMS.md
git commit -m "docs: documenta arquitectura de interfaces y cierra P2.2 (#P2.2)"
```

---

## 8. Fase 5 — Regresión completa

```powershell
& "C:\Users\astur\Desktop\maven\apache-maven-3.9.9\bin\mvn.cmd" clean test
```

**Criterio de éxito:** `BUILD SUCCESS` con la suite local en verde (300 tests). Los 37 errores
ambientales de las 9 clases `*IntegrationTest` (MySQL/Redis vía Docker apagado) se reportan
como esperados y se validan en el contenedor si se requiere.

---

## 9. Archivos Afectados

**Nuevos:**
- `docs/superpowers/plans/2026-08-10-arquitectura-interfaces-thymeleaf-react-plan.md` (este plan)
- `docs/ARQUITECTURA_INTERFACES.md`
- `src/test/java/com/monteastur/envios/controller/LoginControllerTest.java`

**Modificados:**
- `docs/HARDENING_BACKLOG_ENVIOS_CMS.md`
- `src/main/java/com/monteastur/envios/config/SecurityConfig.java`
- `src/main/java/com/monteastur/envios/controller/LoginController.java`
- `src/main/java/com/monteastur/envios/controller/AdminController.java`
- `src/main/resources/templates/fragments/admin-sidebar.html`
- `src/main/resources/static/css/design-system.css` (`.legacy-banner`)
- `src/test/java/com/monteastur/envios/controller/AdminControllerTest.java`
- `src/test/java/com/monteastur/envios/config/SecurityConfigTest.java`

**Sin cambios:** `ReactConfig`, `SpaForwardController`, `SpaForwardControllerTest`,
`App.jsx`, `LoginPage.jsx`, `AuthContext.jsx`, `ProtectedRoute.jsx`, `api.js` (verificados
como destino del redirect).

---

## 10. Criterios de Aceptación (derivados del spec)

1. `docs/ARQUITECTURA_INTERFACES.md` documenta matriz y hoja F1–F6.
2. `GET /login` con admin → `302 /react-dashboard/`; con cliente → login; anónimo → login.
3. `POST /login` correcto → `302 /react-dashboard/`.
4. `GET /admin/dashboard` con sesión → `302 /react-dashboard/`; anónimo → login.
5. Resto de `/admin/**` sigue sirviendo `cms/*.html` con banner legacy.
6. `SpaForwardControllerTest` y suite sin infraestructura en verde (`BUILD SUCCESS`).
7. Backlog: P2.2 cerrado, "Decision actual" corregida.

---

## 11. Riesgos y Mitigaciones

| Riesgo | Mitigación |
|---|---|
| T3.1 (formLogin) inviable en el slice `@WebMvcTest` | Alternativa documentada en T3: verificar `defaultSuccessUrl` del `SecurityFilterChain`; criterio de aceptación intacto. |
| T2.1 depende de que `@PreAuthorize` permita al `user("admin")` | `user("admin").roles("ADMIN")` aporta `ROLE_ADMIN`; patrón ya usado en `imports_returnsView...`. |
| Banner roto visualmente por la regresión del sidebar | CSS autocontenido (decisión aprobada); regresión H8 documentada, no reparada. |
| `@WithMockUser(roles="CLIENTE")` (T1.3) interceptado por rutas `/cliente/**` | El test usa `GET /login` (no `/cliente/**`); no aplica el matcher `hasRole("CLIENTE")`. |

---

*Plan generado el 2026-08-10 a partir del spec aprobado `3744d20` y la investigación del estado
actual del repositorio.*
