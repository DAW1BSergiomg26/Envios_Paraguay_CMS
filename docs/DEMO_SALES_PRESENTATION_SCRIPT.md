# Demo Sales Presentation Script — MonteAstur Envios

Guión para presentar el proyecto a un cliente potencial.

---

## Antes de empezar

- [ ] Docker compose funcionando en local
- [ ] Cloudflare Tunnel iniciado (ver `CLOUDFLARE_TUNNEL_DEMO_GUIDE.md`)
- [ ] URL del túnel copiada y compartida con el cliente
- [ ] Navegador abierto con pestañas preparadas
- [ ] Silencio en notificaciones del sistema

---

## 1. Introducción (2 min)

**"Te voy a enseñar MonteAstur Envios, una plataforma logística premium para gestionar envíos entre España y Paraguay."**

**"Esto no es un prototipo ni un diseño. Es la aplicación funcionando en tiempo real, con base de datos, panel de administración, panel de cliente y seguimiento público de envíos."**

> 📌 Mostrar: página de inicio `https://<tunel>.trycloudflare.com/`

---

## 2. Tracking público (3 min)

**"Cualquier cliente puede seguir su envío sin necesidad de registrarse. Solo con el código único."**

```text
1. Ir a /tracking
2. Escribir código: MT-2026-0001
3. Mostrar timeline visual
4. Mostrar estado: "En tránsito", ubicaciones, fechas
```

**"Cada envío tiene un timeline visual con eventos. El cliente ve exactamente dónde está su paquete en cada momento."**

> 📌 Mostrar: `/tracking` → búsqueda → resultado con timeline

**"Además, se pueden adjuntar evidencias: fotos del paquete, documentos de aduana, comprobantes de entrega."**

> 📌 Mostrar: evidencias asociadas al envío (si hay datos demo)

---

## 3. Panel de administración (5 min)

**"El panel de administración permite gestionar todos los envíos, clientes y contenidos desde un solo lugar."**

### Login

```text
1. Ir a /login
2. Usuario: admin
3. Contraseña: admin123
```

> 📌 Mostrar: login exitoso → redirect a dashboard

### Dashboard

**"El dashboard muestra estadísticas en tiempo real: envíos activos, entregados, en aduana, y métricas de rendimiento."**

> 📌 Mostrar: `/admin/dashboard` — KPIs, gráficos

### Gestión de envíos

**"Aquí puedes ver todos los envíos, filtrarlos por estado, buscar por código, y actualizar su estado."**

```text
1. Mostrar lista de envíos
2. Filtrar por estado "En tránsito"
3. Abrir detalle de un envío
4. Mostrar timeline completo + evidencias
```

> 📌 Mostrar: `/admin/tracking` → detalle de envío

### Galería CMS

**"El CMS incluye una galería de imágenes para las operaciones. Los administradores pueden subir y gestionar imágenes."**

> 📌 Mostrar: `/admin/imagenes` — galería con imágenes demo

### Mensajes de contacto

**"Los mensajes que los clientes envían desde el formulario de contacto se gestionan aquí."**

> 📌 Mostrar: `/admin/mensajesrecibidos`

---

## 4. Panel de cliente (3 min)

**"Cada cliente tiene su propio panel donde puede ver sus envíos, su historial y su información personal."**

```text
1. Ir a /cliente/login
2. Email: cliente@monteastur.com
3. Contraseña: demo2026
```

> 📌 Mostrar: `/cliente/panel` — lista de envíos del cliente

**"El cliente ve SOLO sus envíos. No puede ver los de otros clientes. La seguridad está garantizada por sesión."**

> 📌 Mostrar: detalle de envío desde el panel cliente

---

## 5. Dashboard React SPA (3 min)

**"Además del panel Thymeleaf tradicional, tenemos un dashboard moderno en React, con gráficos interactivos y una experiencia de usuario más fluida."**

```text
1. Ir a /login-react
2. Login con admin / admin123
```

> 📌 Mostrar: SPA React — dashboard con gráficos, navegación

**"El SPA es una Progressive Web App. Se puede instalar en el móvil o escritorio, y funciona offline."**

---

## 6. Seguridad (2 min)

**"La seguridad es una prioridad:**
- **Contraseñas hasheadas con BCrypt** (nunca en texto plano)
- **Sesiones con cookie HttpOnly** (no accesible desde JavaScript)
- **CSRF activo en formularios** (protección contra ataques)
- **Panel admin protegido** (solo usuarios autorizados)
- **Cada cliente ve solo sus propios envíos**
- **Nginx con security headers** (HSTS, CSP, XFO)"

---

## 7. Tecnología (2 min)

**"La aplicación usa tecnologías modernas y robustas:"**

| Componente | Tecnología |
|------------|-----------|
| Backend | Java 17 + Spring Boot 3.3 |
| Frontend clásico | Thymeleaf + CSS modular |
| Frontend moderno | React 19 + Vite 8 |
| Base de datos | MySQL 8 |
| Proxy | Nginx |
| Containerización | Docker |
| Monitoring | Prometheus + Grafana |
| PWA | Service Worker + Push Notifications |

---

## 8. Propuesta de valor (2 min)

**"¿Qué diferencia a MonteAstur Envios de otras soluciones?"**

1. **Hecho a medida**: No es un SaaS genérico. Está diseñado específicamente para envíos España ↔ Paraguay.
2. **Completo**: Tracking público, panel admin, panel cliente, CMS, monitoring, PWA, offline.
3. **Seguro**: Arquitectura híbrida con buenas prácticas de seguridad.
4. **Escalable**: Docker + VPS por ~€5/mes. Se puede escalar cuando crezca el volumen.
5. **Código propio**: Sin dependencias de terceros bloqueantes. Total control del producto.

---

## 9. Precio orientativo (1 min)

| Plan | Coste | Incluye |
|------|-------|---------|
| **Demo / Prueba** | €0 | Túnel Cloudflare temporal desde tu PC |
| **Básico** | ~€5/mes | VPS Hetzner + dominio, producción real |
| **Premium** | ~€15/mes | VPS mejorado + mantenimiento + soporte |
| **Enterprise** | A consultar | Infraestructura dedicada + custom features |

---

## 10. Cierre (2 min)

**"¿Te gustaría probarlo tú mismo? Puedo dejarte una demo para que juegues durante unos días."**

**"Si te interesa, el siguiente paso sería pasar a producción real con un VPS por ~€5/mes, dominio propio, SSL y monitorización 24/7."**

**"¿Qué opinas? ¿Hay alguna funcionalidad que te gustaría añadir o modificar?"**

---

## Post-demo

- [ ] Anotar feedback del cliente
- [ ] Preguntar: ¿Hay presupuesto para VPS (~€5/mes)?
- [ ] Preguntar: ¿Cuándo querrían tenerlo operativo?
- [ ] Enviar email de seguimiento con enlaces a la documentación
- [ ] Si hay interés: enviar `docs/VPS_DEPLOY_EXECUTION_PLAN.md`

---

## Notas para el presentador

- **No te apresures**: Deja que el cliente explore cada sección
- **Pregunta**: "¿Esto te sería útil en tu operativa diaria?"
- **Escucha**: El feedback del cliente es más importante que mostrar todas las features
- **Adapta**: Si el cliente solo le interesa el tracking, céntrate en eso
- **Sé honesto**: Si algo no está listo o tiene limitaciones, dilo. Genera confianza.
- **Demo data**: Recuerda que los datos son demo. Si el cliente pregunta por datos reales, explica que se cargarán en producción.