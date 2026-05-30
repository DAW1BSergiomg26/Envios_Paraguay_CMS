# Cloudflare Tunnel Demo Guide — MonteAstur Envios

Guía para exponer tu Docker local temporalmente usando Cloudflare Tunnel,
ideal para demostraciones en vivo con clientes sin coste y sin VPS.

---

## Requisitos

- Docker Desktop funcionando con el proyecto levantado
- Node.js 20+ (para cloudflared en algunos métodos)
- Conexión a internet (al menos 5 Mbps de subida)
- Sin necesidad de cuenta Cloudflare (opcional pero recomendado)

---

## Paso 1: Verificar Docker local

```bash
# Asegúrate de que el proyecto está funcionando
cd /ruta/a/Envios_Paraguay_CMS
docker compose ps

# Healthcheck debe responder UP
curl http://localhost:8080/actuator/health
# → {"status":"UP"}

# Nginx debe responder
curl -I http://localhost:8090
# → 200 OK
```

---

## Paso 2: Instalar cloudflared

### Windows (PowerShell)

```powershell
winget install cloudflare.cloudflared
# O manual:
# 1. Descargar: https://github.com/cloudflare/cloudflared/releases/latest
# 2. Buscar cloudflared-windows-amd64.exe
# 3. Renombrar a cloudflared.exe y mover a C:\Windows\System32\ o añadir al PATH
```

### Linux (Ubuntu/Debian)

```bash
sudo apt install cloudflared
# O manual:
curl -L https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64 -o cloudflared
chmod +x cloudflared
sudo mv cloudflared /usr/local/bin/
```

### macOS

```bash
brew install cloudflared
```

### Verificar instalación

```bash
cloudflared --version
```

---

## Paso 3: Iniciar túnel temporal

```bash
# Desde cualquier terminal
cloudflared tunnel --url http://localhost:8090
```

### Salida esperada

```
2026/05/25 14:00:00 INF +--------------------------------------------------------------------------------------------+
2026/05/25 14:00:00 INF |  Your quick Tunnel has been created! Visit it at the following URL:                        |
2026/05/25 14:00:00 INF |  https://random-words-123.trycloudflare.com                                                |
2026/05/25 14:00:00 INF +--------------------------------------------------------------------------------------------+
```

**NO cierres esta terminal.** El túnel permanece abierto mientras el proceso esté corriendo.

---

## Paso 4: Compartir la URL

Copia la URL `https://random-words-123.trycloudflare.com` y compártela con el cliente.

### Qué probar

| Ruta | Descripción |
|------|-------------|
| `/` | Página de inicio |
| `/tracking` | Tracking público (probar con MT-2026-0001) |
| `/login` | Login admin (admin / admin123) |
| `/admin/dashboard` | Panel admin |
| `/admin/tracking` | Gestión de envíos |
| `/admin/imagenes` | Galería demo |
| `/admin/mensajesrecibidos` | Mensajes de contacto |
| `/cliente/login` | Login cliente (email: `cliente@monteastur.com`, pass: `demo2026`) |
| `/cliente/panel` | Panel cliente |
| `/login-react` | Dashboard React SPA |
| `/actuator/health` | Healthcheck |

### Si no cargan las rutas /admin/*

Si las rutas admin dan 404, probablemente es porque la app no tiene datos demo o las rutas difieren de las documentadas. En ese caso:
1. Verificar que `APP_DEMO_DATA=true` en `.env`
2. Verificar `docker compose logs app` para errores
3. Ajustar la ruta según la configuración real

---

## Paso 5: Durante la demo

### Buenas prácticas

1. **Cierra apps pesadas**: libera RAM/CPU para la demo
2. **Conecta por cable**: WiFi puede ser inestable
3. **Prepara las pestañas**: ten abiertas las URLs clave antes de compartir
4. **Prueba antes**: verifica que todo funciona antes de la reunión
5. **Silencia notificaciones**: evita distracciones

### Qué hacer si el túnel se cae

```bash
# 1. Reiniciar túnel
cloudflared tunnel --url http://localhost:8090

# 2. Compartir la NUEVA URL (cambia cada vez)
# 3. Disculparse y continuar
```

---

## Paso 6: Cerrar túnel

Cuando termines la demo:

```bash
# En la terminal del túnel: Ctrl+C
# O cerrar la terminal
```

La URL deja de funcionar inmediatamente.

---

## Personalizar URL (opcional, con cuenta Cloudflare)

Si tienes una cuenta Cloudflare y un dominio, puedes crear URLs personalizadas:

```bash
# 1. Autenticar
cloudflared tunnel login

# 2. Crear túnel nombrado
cloudflared tunnel create monteastur-demo

# 3. Configurar DNS
cloudflared tunnel route dns monteastur-demo demo.monteastur.com

# 4. Iniciar con configuración
cloudflared tunnel run monteastur-demo
```

Esto da una URL como `https://demo.monteastur.com` en lugar de la aleatoria de trycloudflare.

---

## Riesgos y advertencias

### ⚠️ Seguridad

- **No uses datos reales**: El tráfico pasa por Cloudflare. Usa solo datos demo (`APP_DEMO_DATA=true`).
- **No expongas credenciales reales**: Las credenciales locales `admin/admin123` y `cliente@monteastur.com`/`demo2026` son solo para demo.
- **No dejes el túnel abierto**: Cuando no lo necesites, ciérralo. Cualquiera con la URL puede acceder.
- **No compartas la URL en público**: Comparte solo con el cliente en la reunión.

### ⚠️ Técnico

- **Ancho de banda**: El tráfico va desde tu PC → Cloudflare → Cliente. Si tu subida es lenta (<5 Mbps), la app irá lenta.
- **Latencia**: Añade ~50-100ms comparado con acceso local.
- **Cortes**: Si tu internet falla, el túnel se cae.
- **URL cambia**: Cada vez que inicias el túnel, la URL es diferente (a menos que configures cuenta Cloudflare).

### ⚠️ Legal

- **Términos de Cloudflare**: trycloudflare.com es para pruebas. No usar para producción o tráfico comercial continuado.
- **GDPR/Protección de datos**: Si enseñas a un cliente europeo, asegúrate de no exponer datos personales reales.

---

## Troubleshooting

| Problema | Solución |
|----------|----------|
| `cloudflared` no encontrado | Verificar instalación, PATH, o usar ruta completa `./cloudflared` |
| Puerto 8090 no accesible | Verificar Docker: `docker compose ps`, nginx debe estar UP |
| Túnel no conecta | Firewall local bloqueando. Probar con `cloudflared tunnel --url http://localhost:8080` (directo a app) |
| App lenta | Verificar uso de CPU/RAM local. Cerrar otras apps. |
| Login falla | CSRF puede fallar si la URL cambia. Refrescar página de login. |
| React SPA no carga | El SPA espera ciertas rutas. Probar `/login-react` primero. |
| 502 Bad Gateway | Nginx no responde. Verificar `docker compose logs nginx`. |

---

## Post-demo: Próximos pasos

1. **Recoger feedback del cliente**
2. **Decidir si hay presupuesto para VPS** (~€5/mes)
3. **Si sí**: Seguir `docs/VPS_DEPLOY_EXECUTION_PLAN.md`
4. **Si no**: Evaluar Render/Neon.tech como alternativa 24/7
5. **Documentar**: Guardar feedback para mejorar el producto

---

## Alternativas si cloudflared no funciona

```bash
# Usar ngrok (similar, también gratis)
ngrok http 8090

# Usar bore (similar, open source)
bore local 8090 --to bore.pub

# Usar localhost.run
ssh -R 80:localhost:8090 nokey@localhost.run
```

Todas estas opciones hacen lo mismo: exponen tu localhost a internet temporalmente.