import subprocess
import datetime
import os

def run_cmd(command):
    try:
        result = subprocess.run(command, capture_output=True, text=True, shell=True, check=True)
        return result.stdout.strip()
    except Exception as e:
        return f"Error ejecutando comando: {e}"

def generate_handoff():
    date_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    branch = run_cmd("git branch --show-current")
    last_commit = run_cmd("git log -1 --pretty=format:\"%h - %an: %s (%ar)\"")
    status = run_cmd("git status -s")
    docker_status = run_cmd("docker compose ps")
    commit_count = run_cmd("git rev-list --count HEAD")
    file_list = run_cmd("git ls-files --cached")
    file_count = len(file_list.splitlines()) if file_list and not file_list.startswith("Error") else "N/A"

    content = f"""# HANDOFF - Envios_Paraguay_CMS
**Ultima actualizacion:** {date_str}

## Estado Actual del Proyecto
* **Rama Git Activa:** `{branch}`
* **Ultimo Commit:** `{last_commit}`
* **Total de Commits:** {commit_count}
* **Archivos rastreados:** {file_count}
* **Cambios pendientes (Git Status):**
```text
{status if status else "Directorio limpio (sin cambios sin commitear)"}
```

## Servicios Docker
```text
{docker_status if docker_status else "Docker Compose no esta corriendo o no hay contenedores activos."}
```

## Arquitectura del Sistema
* **Backend:** Spring Boot 3.3.5 / Java 17 / Maven
* **Frontend:** React 19 + Vite 8 (SPA en `frontend-react/`)
* **Base de datos:** MySQL 8.0 (Docker)
* **Proxy reverso:** Nginx (Alpine)
* **Monitoreo:** Prometheus + Grafana + Uptime Kuma
* **Puertos:**
    * App: 8080
    * Frontend (dev): 5173
    * Nginx: 8090
    * Prometheus: 9090
    * Grafana: 3001
    * Uptime Kuma: 3002

## Endpoints API Clave
| Metodo | Ruta | Auth | Descripcion |
|--------|------|------|-------------|
| POST | /api/v1/reservas | Publico | Crear reserva |
| GET | /api/v1/reservas/disponibilidad | Publico | Verificar disponibilidad |
| GET | /api/v1/admin/reservas | Admin | Listar reservas |
| GET | /api/v1/admin/reservas/{{id}} | Admin | Detalle reserva |
| PUT | /api/v1/admin/reservas/{{id}} | Admin | Editar reserva |
| PATCH | /api/v1/admin/reservas/{{id}}/estado | Admin | Cambiar estado |
| DELETE | /api/v1/admin/reservas/{{id}} | Admin | Eliminar reserva |
| GET | /actuator/health | Publico | Health check |
| GET | /actuator/prometheus | Publico | Metricas Prometheus |

## Credenciales por Defecto (dev)
| Servicio | Usuario | Password |
|----------|---------|----------|
| MySQL | app_user | changeme_app |
| MySQL (root) | root | root |
| Admin Panel | admin | admin123 |
| Grafana | admin | admin123 |

## Comandos Utiles
```powershell
# Levantar todo
docker compose down; docker compose up -d

# Local sin Docker
mvn spring-boot:run

# Frontend
cd frontend-react; npm run dev

# Verificar salud
curl http://localhost:8080/actuator/health

# Verificar metricas
curl http://localhost:8080/actuator/prometheus
```

---
*Generado automaticamente por generate_handoff.py*
"""

    output_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "HANDOFF.md")
    with open(output_path, "w", encoding="utf-8") as f:
        f.write(content)
    
    print(f"HANDOFF generado: {output_path}")

if __name__ == "__main__":
    generate_handoff()
