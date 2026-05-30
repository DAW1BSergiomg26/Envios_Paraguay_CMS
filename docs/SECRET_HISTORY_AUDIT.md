# SECRET_HISTORY_AUDIT

## Estado

```text
Proyecto: Envios_Paraguay_CMS
Rama: feature/hardening-secretos-perfil-prod
Fase: 1 — Hardening secretos y perfil prod
Tipo: auditoria documental de secretos
```

---

## Proposito

Este documento abre la Fase 1 de hardening.

El objetivo es verificar que el proyecto no haya subido secretos al repositorio y dejar una base segura antes de tocar configuracion o codigo.

---

## Contexto

La Fase 0 detecto que:

```text
.env esta ignorado por Git.
logs/ esta ignorado por Git.
uploads/ esta ignorado por Git.
target/ esta ignorado por Git.
node_modules y dist estan ignorados en frontend-react/.gitignore.
```

Tambien se detecto que existen archivos de ejemplo:

```text
.env.example
.env.production.example
src/main/resources/application.properties
src/main/resources/application-prod.properties
```

Estos archivos no deben contener secretos reales.

---

## Verificaciones recomendadas

Ejecutar en local:

```powershell
git status

git log --all -- .env

git log --all --name-only | Select-String "\.env"

git log --all --name-only | Select-String "password|secret|token|key|credential"
```

---

## Resultado esperado

```text
.env no debe aparecer como archivo versionado.
.env.example puede aparecer.
.env.production.example puede aparecer.
Los resultados con password/secret/token/key/credential deben ser revisados manualmente.
```

---

## Decision inicial

```text
No pegar secretos reales en chats, issues, commits ni documentacion.
Si aparece un secreto real en historial, rotar credenciales inmediatamente.
Si aparece .env en historial, investigar commit, alcance y exposicion.
```

---

## Riesgos a controlar

### P0 — .env en historial

```text
Si .env fue commiteado alguna vez, puede contener credenciales reales.
```

Accion:

```text
Rotar secretos y evaluar limpieza de historial.
```

---

### P1 — placeholders inseguros

```text
Valores como admin123, changeme o CHANGE_ME no son secretos reales, pero pueden inducir errores si llegan a produccion.
```

Accion:

```text
Mantenerlos solo como ejemplo y reforzar checklist de produccion.
```

---

### P1 — application.properties con defaults dev

```text
application.properties permite defaults comodos para desarrollo.
```

Accion:

```text
Produccion debe usar siempre SPRING_PROFILES_ACTIVE=prod.
```

---

## Checklist de cierre

```text
[ ] git status limpio.
[ ] git log --all -- .env revisado.
[ ] busqueda de .env revisada.
[ ] busqueda de password/secret/token/key/credential revisada.
[ ] .env.example revisado como plantilla, no como secreto real.
[ ] .env.production.example revisado como plantilla, no como secreto real.
[ ] application.properties clasificado como dev.
[ ] application-prod.properties clasificado como prod.
```

---

## Siguiente documento recomendado

```text
docs/PROD_PROFILE_HARDENING_PLAN.md
```

Objetivo:

```text
Definir como asegurar que produccion arranque siempre con perfil prod, variables obligatorias y smoke tests de seguridad.
```

---

## Frase guia

Un secreto no se protege cuando ya se filtro.

Se protege antes, con habitos, revisiones y ramas limpias.
