# QA_REAL_EXECUTION_LOG

## Estado

Proyecto: Envios_Paraguay_CMS  
Rama: feature/ejecucion-qa-real  
Fase: 4 - QA y estabilizacion funcional  
Tipo: registro de prueba real en navegador

---

## Fecha de ejecucion

2026-06-01

---

## Entorno probado

Entorno local con Docker Compose.

URLs revisadas:

```text
http://localhost:8090
http://localhost:8090/en
http://localhost:8090/admin/dashboard
http://localhost:8090/cliente/panel
http://localhost:8090/api/v1/tracking/MT-2026-0001
```

---

## Resultado general

La web carga correctamente en entorno local.

Se confirmo funcionamiento de:

- web publica;
- cambio de idioma ES/EN;
- panel admin;
- panel cliente;
- tracking publico;
- Docker Compose como forma recomendada de levantar el proyecto.

---

## QA 1 - Web publica

URL:

```text
http://localhost:8090
```

Resultado:

```text
OK
```

Observaciones:

```text
La home publica carga correctamente.
El hero principal se visualiza.
La navegacion superior aparece en espanol.
```

---

## QA 2 - Cambio de idioma

URL:

```text
http://localhost:8090/en
```

Resultado:

```text
OK
```

Observaciones:

```text
La version inglesa carga correctamente.
La navegacion cambia a Home, The House, Tracking, Bookings y Contact.
El contenido principal cambia a ingles.
```

---

## QA 3 - Panel admin

URL:

```text
http://localhost:8090/admin/dashboard
```

Resultado:

```text
OK
```

Observaciones:

```text
El dashboard admin carga correctamente.
Se visualizan tarjetas de reservas, mensajes, imagenes y estado del sistema.
El panel muestra estado online y base de datos activa.
```

---

## QA 4 - Panel cliente

URL:

```text
http://localhost:8090/cliente/panel
```

Resultado:

```text
OK
```

Observaciones:

```text
El panel cliente carga correctamente.
Se visualizan envios asociados al cliente demo.
Se muestran datos de envio, historial y estado.
```

---

## QA 5 - Tracking publico minimo

URL:

```text
http://localhost:8090/api/v1/tracking/MT-2026-0001
```

Resultado:

```text
OK
```

Respuesta confirmada:

```json
{
  "codigoUnico": "MT-2026-0001",
  "estado": "EN_TRANSITO",
  "origen": "Pola de Siero, Asturias",
  "destino": "Asuncion, Paraguay",
  "ultimaActualizacion": "2026-05-15T14:30:00"
}
```

Validacion:

```text
El endpoint publico ya no expone destinatario.
El endpoint publico ya no expone peso.
El endpoint publico ya no expone contenido.
```

---

## QA 6 - Ruta recomendada para levantar el proyecto

Decision tomada:

```text
Usar Docker Compose como metodo principal para levantar la web.
No usar XAMPP para este proyecto.
No usar Maven como metodo principal para navegar la web completa.
```

Comando recomendado:

```powershell
cd C:\Users\astur\Desktop\Envios_Paraguay_CMS
docker compose up -d --build
```

URL principal recomendada:

```text
http://localhost:8090
```

URL directa de la app:

```text
http://localhost:8080
```

---

## Incidencias detectadas

### Incidencia 1 - Confusion inicial entre XAMPP, Docker y Maven

Estado:

```text
Resuelta por decision operativa.
```

Decision:

```text
Para este proyecto se usara Docker Compose como camino unico de ejecucion local.
XAMPP queda fuera del flujo normal de trabajo.
Maven queda para compilar y validar codigo.
```

---

### Incidencia 2 - Imagen Docker antigua mostraba tracking completo

Estado:

```text
Resuelta con reconstruccion Docker.
```

Resultado:

```text
Despues de reconstruir, el tracking publico muestra solo el DTO minimo.
```

---

## Pendientes posteriores

- Probar subida real de evidencia desde admin.
- Probar descarga real de evidencia visible desde cliente.
- Probar evidencia no visible.
- Probar evidencia ajena.
- Revisar responsive movil de home, admin y cliente.
- Iniciar fase de mejora visual premium.

---

## Conclusion

La ejecucion QA real confirma que los cambios principales de la Fase 3 estan activos en navegador.

El proyecto esta listo para avanzar hacia una fase visual y de experiencia de usuario mas ambiciosa, manteniendo Docker Compose como forma principal de ejecucion local.
