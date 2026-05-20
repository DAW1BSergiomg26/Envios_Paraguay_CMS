# MONTEASTUR ENVIOS — Plataforma logística España-Paraguay

## Tecnologías

- Java 17
- Spring Boot 3.3.5
- Thymeleaf
- MySQL 8
- Docker / Docker Compose
- CSS propio (Bootstrap no se usa, CSS custom)
- Actuator
- Logback

## Arranque local sin Docker

Para ejecutar la aplicación en entorno de desarrollo sin Docker:

1. Arrancar MySQL en Docker (puerto 3307 para no конфликт con posibles MySQL locales):
   ```powershell
   docker run -d --name mysql-dev -p 3307:3306 -e MYSQL_ROOT_PASSWORD=root -e MYSQL_DATABASE=casarural mysql:8.0
   ```

2. Configurar variables de entorno necesarias en PowerShell:
   ```powershell
   $env:PORT="8895"
   $env:SPRING_DATASOURCE_URL="jdbc:mysql://localhost:3307/casarural?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
   $env:SPRING_DATASOURCE_USERNAME="root"
   $env:SPRING_DATASOURCE_PASSWORD="root"
   ```

3. Ejecutar la aplicación:
   ```powershell
   mvn spring-boot:run
   ```

4. Abrir en navegador: http://localhost:8895

## Arranque con Docker Compose

Para despliegue completo con todos los servicios:

1. Copiar el archivo de ejemplo de variables de entorno:
   ```powershell
   cp .env.example .env
   ```

2. Editar `.env` con las credenciales y configuración adecuadas (especialmente cambiar contraseñas en producción)

3. Construir y levantar los contenedores:
   ```powershell
   docker compose up -d --build
   ```

4. La aplicación estará disponible en http://localhost:8080 (o el puerto definido en PORT)

## Variables importantes

| Variable | Descripción | Valor por defecto (dev) | Requerido en prod |
|----------|-------------|-------------------------|-------------------|
| PORT | Puerto del servidor | 8081 | No |
| SPRING_PROFILES_ACTIVE | Perfil activo (dev/prod) | (vacío) | prod |
| SPRING_DATASOURCE_URL | URL de conexión a MySQL | jdbc:mysql://localhost:3306/casarural?... | Sí |
| DB_USERNAME | Usuario de base de datos | root | Sí |
| DB_PASSWORD | Contraseña de base de datos | (vacía) | Sí |
| UPLOAD_DIR | Directorio para subida de archivos | ./uploads | No |
| ADMIN_USERNAME | Usuario del panel de admin | admin | No |
| ADMIN_PASSWORD | Contraseña del panel de admin | admin123 | Sí (cambiar en prod) |
| LOG_DIR | Directorio para logs | ./logs | No |

## Rutas importantes

| Ruta | Descripción |
|------|-------------|
| `/` | Página principal |
| `/login` | Inicio de sesión (admin y cliente) |
| `/admin/dashboard` | Panel de administración principal |
| `/admin/tracking` | Gestión de envíos y tracking |
| `/admin/imagenes` | Gestión de imágenes del CMS |
| `/cliente/login` | Acceso específico para clientes |
| `/cliente/panel` | Panel de cliente tras login |
| `/actuator/health` | Endpoint de salud de la aplicación |
| `/actuator/info` | Información detallada de la aplicación |

## Uploads

- **Entorno local**: Los archivos se suben a la carpeta `uploads/` relativa al directorio de ejecución
- **Producción (Docker)**: Se utiliza un volumen Docker montado en `/app/uploads` dentro del contenedor
- **Importante**: Nunca subir la carpeta `uploads/` a Git (está en `.gitignore`)
- **Backup**: Realizar copias de seguridad periódicas de esta carpeta ya que contiene las imágenes del sistema

## Logs

El sistema de logging utiliza Logback con la siguiente configuración:

- `logs/monteastur.log`: Log general de la aplicación (nivel INFO y superior)
- `logs/monteastur-error.log`: Solo errores y advertencias (nivel WARN y superior)
- Rotación: Diaria (un archivo nuevo cada día)
- Retención: 30 días (los archivos más antiguos se eliminan automáticamente)
- El directorio de logs se puede configurar con la variable de entorno `LOG_DIR` (por defecto `./logs`)

## Seguridad

### Checklist de seguridad para producción:

- [ ] Cambiar `ADMIN_PASSWORD` en producción (nunca usar el valor por defecto)
- [ ] Nunca subir el archivo `.env` a repositorios Git (está en `.gitignore`)
- [ ] Las contraseñas de clientes se almacenan usando BCrypt (nunca en texto plano)
- [ ] En producción, usar `spring.jpa.hibernate.ddl-auto=validate` (no `update`)
- [ ] Activar el perfil de producción con `SPRING_PROFILES_ACTIVE=prod`
- [ ] Proteger las copias de seguridad de base de datos y uploads
- [ ] En servidores reales, utilizar HTTPS con certificado válido
- [ ] Mantener actualizadas las dependencias (especialmente Spring Boot y MySQL driver)

## Healthcheck

La aplicación incluye endpoints de Actuator para monitoreo:

- **GET /actuator/health**
  - Devuelve `{"status":"UP"}` cuando la aplicación está funcionando correctamente
  - Incluye detalles de salud cuando se accede con credenciales de administrador
  - Utilizado por Docker Compose y orchestadores para verificar disponibilidad

- **GET /actuator/info**
  - Muestra información de la aplicación: nombre, versión y descripción
  - Útil para verificar qué versión está desplegada

## Backup básico

### Base de datos:
```powershell
docker exec monteastur-mysql mysqldump -u root -p casarrural > backup-casarural.sql
```
*(Nota: usar la contraseña definida en MYSQL_ROOT_PASSWORD del archivo .env)*

### Uploads:
```powershell
tar -czf backup-uploads.tar.gz uploads/
```
*(En producción, respaldar el volumen Docker correspondiente a `/app/uploads`)*

## Checklist antes de producción

Antes de desplegar en un entorno de producción, verificar:

- [ ] **Build OK**: `mvn clean package -DskipTests` finaliza sin errores
- [ ] **Docker compose OK**: `docker compose up -d` levanta todos los servicios correctamente
- [ ] **Health UP**: `http://<dominio>/actuator/health` devuelve `{"status":"UP"}`
- [ ] **Login admin OK**: Acceso con las credenciales configuradas en producción
- [ ] **Login cliente OK**: Los clientes pueden autenticarse correctamente
- [ ] **Uploads OK**: Las imágenes se suben y se muestran correctamente
- [ ] **Logs OK**: Se generan archivos en `logs/` sin errores de permisos
- [ ] **Backup probado**: Se puede restaurar tanto la base de datos como los uploads
- [ ] **Variables .env configuradas**: Todas las variables necesarias están definidas y son apropiadas para entorno

---

> **Nota**: Este documento está pensado como guía de arranque y operaciones básicas. Para consultas técnicas avanzadas, referirse al código fuente y comentarios en el mismo.