# Monteastur Envios — Estrategia de Testing

> **Versión:** 1.0 | **Fecha:** 2026-05-25
> **Propósito:** Definir qué tipo de test usar en cada situación y cómo ejecutarlos correctamente.

---

## 1. Tipos de test

| Tipo | Herramienta | Ubicación | Archivos |
|------|------------|-----------|----------|
| **Unit / Component** | **Vitest** + Testing Library | `frontend-react/src/` | `*.test.jsx`, `*.spec.jsx` |
| **Backend (unit)** | **JUnit 5** + Mockito | `src/test/java/` | `*Test.java` |
| **E2E** | **Playwright** | `frontend-react/e2e/` | `*.spec.js` |

---

## 2. Unit / Component tests (Vitest)

### Qué prueban

- Componentes React de forma aislada
- Lógica de hooks y helpers
- Renderizado condicional
- Eventos e interacciones del usuario (simuladas con `userEvent`)
- No requieren servidor, base de datos ni navegador real

### Cuándo escribirlos

- Cada nuevo componente o página del frontend
- Cada bug corregido en el frontend (test de regresión)
- Cuando un componente tiene lógica condicional o estados múltiples

### Comandos

```bash
# Ejecutar unit tests (entorno jsdom)
cd frontend-react && npm run test:unit

# Modo watch (desarrollo)
cd frontend-react && npm run test:watch

# Con cobertura
cd frontend-react && npm run test:coverage
```

### Archivos detectados

Solo busca en `src/`:
- `src/**/*.test.jsx`
- `src/**/*.spec.jsx`
- `src/**/*.test.js`
- `src/**/*.spec.js`

Excluye: `e2e/`, `node_modules/`.

---

## 3. Backend tests (JUnit 5)

### Qué prueban

- Controladores REST (MockMvc)
- Servicios con lógica de negocio
- Configuración de seguridad (rutas públicas/protegidas)
- No requieren base de datos real (mocking con Mockito)

### Cuándo escribirlos

- Cada nuevo endpoint REST
- Cada nuevo servicio con lógica transaccional
- Cambios en configuración de seguridad

### Comandos

```bash
# Todos los tests backend
mvn test

# Test específico
mvn test -Dtest=TrackingApiControllerTest

# Build completo (test + package)
mvn clean package
```

---

## 4. E2E tests (Playwright)

### Qué prueban

- Flujos completos en navegador real (Chromium)
- Login, dashboard, tracking, upload
- Interacciones reales (click, navegación, formularios)
- Requieren: app Spring Boot corriendo + MySQL + frontend build

### Cuándo escribirlos

- Flujos críticos de usuario (login, tracking, dashboard)
- Antes de un release para validar integración real
- Cuando un bug involucra interacción entre frontend y backend

### Comandos

```bash
# Ejecutar E2E (headless, requiere app corriendo)
cd frontend-react && npm run test:e2e

# Con UI interactiva
cd frontend-react && npm run e2e:ui

# Con navegador visible
cd frontend-react && npm run e2e:headed
```

### Requisitos para ejecutar E2E local

1. App corriendo: `docker compose up -d --build` (o `mvn spring-boot:run`)
2. URL configurable vía `E2E_BASE_URL` (default: `http://localhost:8090`)

### En CI

El job `e2e-tests` está deshabilitado por defecto (`if: false`) porque necesita:
- MySQL service
- Spring Boot corriendo
- Playwright + Chromium instalado

Para habilitarlo: cambiar `if: false` a `if: true` en `.github/workflows/ci.yml`.

---

## 5. Todos los tests

```bash
# Unit + E2E (requiere app corriendo para E2E)
cd frontend-react && npm run test:all

# Backend + frontend unit
mvn test && cd frontend-react && npm run test:unit

# Full suite (CI)
mvn clean package && cd frontend-react && npm run test:all
```

---

## 6. Cuándo usar cada uno

| Situación | Test recomendado | Por qué |
|-----------|-----------------|---------|
| Nuevo botón en componente | Unit (Vitest) | Rápido, aísla el componente |
| Nueva API endpoint | Unit (JUnit) | MockMvc, no requiere BD |
| Nuevo flujo de login | E2E (Playwright) | Valida HTML, JS, redirect, cookies reales |
| Bug visual en dashboard | Unit (Vitest) + visual check | Renderizado condicional |
| Bug de sesión/seguridad | Unit (JUnit) + E2E | MockMvc para lógica, Playwright para cookie real |
| Refactor de componente | Unit (Vitest) | Los tests existentes deben seguir pasando |
| Antes de release | E2E (Playwright) | Validar flujos completos |
| CI/CD pre-deploy | Unit (Vitest + JUnit) | Rápidos, no requieren infraestructura |

---

## 7. Pipeline CI/CD

```yaml
jobs:
  backend-build:
    - mvn clean package             # Compila + ejecuta tests JUnit

  frontend-build:
    - npm run test:unit             # Solo unit/component tests
    - npm run build                 # Build de producción

  e2e-tests:
    if: false                       # Deshabilitado por defecto
    - mvn spring-boot:run &         # Inicia backend
    - npm run test:e2e              # E2E en navegador real
```

---

> **Documentos relacionados:**
> - [`frontend-react/vitest.config.js`](../frontend-react/vitest.config.js) — Configuración Vitest (excluye e2e/)
> - [`frontend-react/playwright.config.js`](../frontend-react/playwright.config.js) — Configuración Playwright (solo e2e/)
> - [`frontend-react/package.json`](../frontend-react/package.json) — Scripts: test:unit, test:e2e, test:all
> - [`.github/workflows/ci.yml`](../.github/workflows/ci.yml) — Workflow CI/CD
>
> **Mantenido por:** Equipo Monteastur Envios
> **Próxima revisión:** 2026-06-25
