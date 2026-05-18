# MONTEASTUR ENVIOS

> Plataforma web premium de logística internacional para transporte y gestión de aduanas España ↔ Paraguay.  
> Desarrollada con Spring Boot, Thymeleaf y MySQL.

---

![Java](https://img.shields.io/badge/Java-24-orange?style=for-the-badge)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green?style=for-the-badge)
![Thymeleaf](https://img.shields.io/badge/Thymeleaf-MVC-darkgreen?style=for-the-badge)
![MySQL](https://img.shields.io/badge/MySQL-Database-blue?style=for-the-badge)
![Responsive](https://img.shields.io/badge/Responsive-Yes-success?style=for-the-badge)
![Git Flow](https://img.shields.io/badge/Git_Flow-Active-blueviolet?style=for-the-badge)

---

## Descripción

**MONTEASTUR ENVIOS** es una plataforma web profesional para la gestión de envíos internacionales entre España y Paraguay. El proyecto combina una experiencia visual cinematográfica con una arquitectura MVC sólida, ofreciendo tanto una web pública premium como un centro de operaciones logístico para administración.

### Capacidades reales del proyecto

- Web pública bilingüe (ES/EN) con diseño glassmorphism
- Hero cinematográfico con imágenes reales de operaciones
- Sección de tracking visual premium con timeline logístico
- CMS administrativo con dashboard de operaciones
- Gestión de envíos, consultas y galería multimedia
- Páginas de servicios, operaciones reales y contacto
- Imágenes reales integradas (carga, entrega, branding)
- Sistema de autenticación y panel de control

---

## Funcionalidades

### Web pública
- Hero con imagen real de operaciones y overlay cinematográfico
- Timeline visual "Seguimiento de tu envío" con 6 estados logísticos
- Sección "Cómo funciona" con 4 pasos
- Página de servicios con 4 cards premium
- Página de operaciones con 6 cards logísticos y galería de envíos reales
- Sección "Nuestros envíos reales" con grid cinematográfico de operaciones
- Brand banner con imagen de marca
- Formulario de envío y contacto
- Lightbox para galería multimedia
- Partículas ambientales y scroll reveal animations

### CMS administrativo
- Login seguro con Spring Security
- Dashboard transformado en "Centro de Operaciones"
- Panel de inicio con estadísticas de envíos, consultas e imágenes
- Card de "Operaciones activas" con 6 indicadores logísticos y badges de estado
- Gestión de envíos (aprobar, cancelar, eliminar)
- Gestión de consultas de clientes
- Gestión de imágenes con previsualización
- Editor de textos legales
- Bitácora de actividad logística

### Tracking visual
- 6 estados logísticos: Preparando carga → Recogida realizada → Gestión de aduanas → En tránsito → Llegada a destino → Entregado
- Cards glassmorphism con glow naranja/verde
- Línea de progreso con gradiente
- Badges de estado (En curso, Pendiente, Completado)
- Responsive: 3 columnas → 2 → 1

### Operaciones reales
- 6 categorías de operaciones: Carga y embalaje, Mudanzas internacionales, Vehículos y maquinaria, Gestión de aduanas, Entrega puerta a puerta, Seguimiento personalizado
- Cards con imágenes reales y placeholders premium para las pendientes
- Gradientes cinematográficos como fondo de placeholder
- Overlay oscuro con badges naranja

---

## Tecnologías

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 24 | Backend |
| Spring Boot | 3.3 | Framework principal |
| Spring Security | — | Autenticación y seguridad |
| Spring Data JPA | — | Persistencia |
| Thymeleaf | 3 | Motor de plantillas MVC |
| MySQL | 8 | Base de datos |
| Maven | — | Gestión de dependencias |
| HTML5 | — | Estructura semántica |
| CSS3 | — | Glassmorphism, animaciones, responsive |
| JavaScript | vanilla | Interactividad, lightbox, partículas |
| Git + GitHub | — | Control de versiones y Git Flow |

---

## Estructura del proyecto

```text
src/main/
├── java/com/grupb2/casarural/
│   ├── controller/       → Controladores MVC
│   ├── model/            → Entidades JPA
│   ├── repository/       → Repositorios
│   ├── service/          → Lógica de negocio
│   └── security/         → Configuración Spring Security
│
├── resources/
│   ├── static/
│   │   ├── css/
│   │   │   ├── style.css     → Estilos web pública (2400+ líneas)
│   │   │   └── admin.css     → Estilos CMS administrativo (1500+ líneas)
│   │   ├── js/app.js         → Scripts de interacción
│   │   └── img/
│   │       └── monteastur/
│   │           ├── hero/            → hero-monteastur.jpg, quienes_somos.mp4
│   │           ├── operaciones/     → operaciones-carga.jpg, operaciones-entrega.jpg
│   │           └── branding/        → banner-monteastur.jpg
│   │
│   └── templates/
│       ├── home.html               → Página principal ES
│       ├── lacasa.html             → Servicios ES
│       ├── reservas.html           → Envíos ES
│       ├── operaciones.html        → Operaciones ES
│       ├── contacto.html           → Contacto ES
│       ├── login.html              → Inicio de sesión
│       ├── en/                     → Versiones EN
│       ├── cms/                    → Panel administrativo
│       │   ├── dashboard.html      → Centro de Operaciones
│       │   ├── reservas.html       → Gestión de envíos
│       │   ├── mensajesrecibidos   → Consultas
│       │   ├── imagenes.html       → Galería
│       │   └── textos.html         → Textos legales
│       └── fragments/
│           ├── header.html         → Head + nav ES
│           ├── header-en.html      → Head + nav EN
│           ├── footer.html         → Footer ES
│           └── footer-en.html      → Footer EN
└── resources/application.properties → Configuración
```

---

## Diseño premium

### Glassmorphism
Fondo blanco semitransparente con `backdrop-filter: blur()` en cards, dashboard, formularios y navegación. Efecto de vidrio esmerilado con bordes sutiles.

### UX cinematográfica
- Hero con gradiente oscuro + imagen real + overlays de luz
- Scroll reveal animations progresivas
- Partículas ambientales flotantes
- Transiciones suaves en todos los elementos interactivos
- Sombras profundas y glow effects
- Timeline visual conectar con línea gradiente

### Paleta de colores
- Verde institucional: `#3f6338` (marca MONTEASTUR)
- Naranja acento: `#d4762a` (CTAs y badges)
- Fondos oscuros: degradados verde oscuro `#1a2218` → `#0d1a0d`
- Glass: blanco 88-94% opacidad con backdrop blur

---

## Responsive

| Dispositivo | Breakpoint | Adaptaciones |
|---|---|---|
| Desktop | > 900 px | Layout completo, hero 70vh, grid 3 columnas |
| Tablet | ≤ 900 px | Grids a 2 columnas, hero 50vh, sidebar colapsada |
| Móvil | ≤ 480 px | Grids a 1 columna, hero 40vh, nav compacto, cards en fila |

---

## SEO

- Meta description por página (ES/EN)
- Open Graph tags (og:title, og:description, og:image)
- Favicon desde branding real
- Títulos descriptivos por sección
- URLs amigables y estructura semántica HTML5
- Etiquetas `alt` en imágenes

---

## Seguridad

- Spring Security con autenticación por formulario
- Sesión protegida con logout
- CSRF implícito en formularios Thymeleaf
- Panel admin solo accesible tras autenticación
- Contraseñas hasheadas en base de datos
- Datos de contacto protegidos (sin emails personales en web pública)

---

## Assets reales

| Archivo | Ruta | Uso |
|---|---|---|
| `hero-monteastur.jpg` | `img/monteastur/hero/` | Fondo hero principal |
| `quienes_somos.mp4` | `img/monteastur/hero/` | Video corporativo |
| `operaciones-carga.jpg` | `img/monteastur/operaciones/` | Card carga real |
| `operaciones-entrega.jpg` | `img/monteastur/operaciones/` | Card entrega real |
| `banner-monteastur.jpg` | `img/monteastur/branding/` | Banner marca + favicon + OG |

---

## Instalación

### Requisitos
- Java 24+
- Maven 3.9+
- MySQL 8+

### Pasos

```bash
# 1. Clonar el repositorio
git clone https://github.com/DAW1BSergiomg26/Envios_Paraguay_CMS.git
cd Envios_Paraguay_CMS

# 2. Configurar base de datos MySQL
# Crear base de datos 'casarural' y configurar application.properties

# 3. Compilar y empaquetar
mvn clean package

# 4. Ejecutar
java -jar target/casarural-0.0.1-SNAPSHOT.jar

# 5. Abrir en navegador
# http://localhost:8089
```

---

## Rutas principales

| Ruta | Descripción |
|---|---|
| `/` | Inicio ES |
| `/en` | Home EN |
| `/casa` | Servicios ES |
| `/en/casa` | Services EN |
| `/reservas` | Envíos ES |
| `/en/reservas` | Shipments EN |
| `/operaciones` | Operaciones ES |
| `/en/operaciones` | Operations EN |
| `/contacto` | Contacto ES |
| `/en/contacto` | Contact EN |
| `/login` | Inicio de sesión admin |
| `/admin/dashboard` | Centro de Operaciones |
| `/admin/reservas` | Gestión de envíos |
| `/admin/mensajesrecibidos` | Consultas clientes |
| `/admin/imagenes` | Galería de fotos |
| `/admin/textos` | Textos legales |

---

## Git Flow

```text
main
└── develop
    ├── feature/nivel-dios-ui
    ├── feature/transformacion-envios-paraguay
    ├── feature/operaciones-reales
    ├── feature/assets-reales-monteastur
    └── feature/pulido-final-monteastur
```

- `main`: Rama de producción
- `develop`: Rama de integración continua
- `feature/*`: Ramas de funcionalidad (merge con `--no-ff`)
- Tags: `v1.0-monteastur-demo`

---

## Versión actual

**v1.0-monteastur-demo** — Demo funcional para presentación al cliente.

### Implementado
- Transformación completa de Casa Rural a MONTEASTUR ENVIOS
- Rebranding visual y textual (headers, footers, páginas)
- Hero cinematográfico con imagen real
- Sección "Cómo funciona" con timeline 4 pasos
- Tracking visual premium con 6 estados
- Página de operaciones con 6 cards logísticos
- Dashboard transformado a Centro de Operaciones
- Imágenes reales integradas (hero, carga, entrega, branding)
- SEO, Open Graph y favicon
- Build funcional (JAR 61 MB)

### Pendiente
- Imágenes reales para mudanzas, vehículos, carga pesada y aduanas
- Integración de video corporativo
- Logo oficial en PNG con fondo transparente
- Despliegue en producción
- Funcionalidades backend avanzadas (tracking real, notificaciones)

---

## Futuras mejoras

- Tracking en tiempo real con número de seguimiento
- Notificaciones automáticas al cliente por email/WhatsApp
- Calculadora de precios online
- Mapa interactivo de rutas España ↔ Paraguay
- Panel de estadísticas avanzadas con gráficos
- API REST para integración con operadores logísticos
- Subida masiva de imágenes de operaciones
- Vídeo de fondo en hero
- Página "Quiénes somos" con video corporativo

---

## Contacto MONTEASTUR

**MONTEASTUR ENVIOS**  
Pola de Siero, Asturias — Asunción, Paraguay

- Teléfono: [+34 642 687 292](tel:+34642687292)
- Email: [monteastur@hotmail.es](mailto:monteastur@hotmail.es)
- WhatsApp: [wa.me/34642687292](https://wa.me/34642687292)

---

> © 2026 MONTEASTUR ENVIOS. Todos los derechos reservados.  
> Proyecto desarrollado como parte del Curso DAW1B — IES Monte Naranco.
