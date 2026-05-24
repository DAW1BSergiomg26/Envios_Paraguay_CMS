# Monteastur Envios — Configuración GitHub Secrets + SSH

> **Versión:** 1.0 | **Última actualización:** 2026-05-24
> **Objetivo:** Guía completa para configurar SSH entre GitHub Actions y el VPS de producción.

---

## Índice

- [A) Generar SSH keys](#a-generar-ssh-keys)
- [B) Copiar clave pública al VPS](#b-copiar-clave-pública-al-vps)
- [C) Configurar GitHub Secrets](#c-configurar-github-secrets)
- [D) Verificar conexión SSH](#d-verificar-conexión-ssh)
- [E) Troubleshooting](#e-troubleshooting)
- [F) Buenas prácticas](#f-buenas-prácticas)

---

## A) Generar SSH keys

### Linux / macOS

```bash
# Generar clave ed25519 para GitHub Actions
ssh-keygen -t ed25519 -C "github-actions@monteastur" -f ~/.ssh/github-actions-monteastur

# Salida esperada:
# Your identification has been saved in ~/.ssh/github-actions-monteastur
# Your public key has been saved in ~/.ssh/github-actions-monteastur.pub

# Verificar archivos generados
ls -la ~/.ssh/github-actions-monteastur*
# → -rw-------   1 user  ~/.ssh/github-actions-monteastur       (privada)
# → -rw-r--r--   1 user  ~/.ssh/github-actions-monteastur.pub   (pública)
```

### Windows (PowerShell)

```powershell
# Generar clave ed25519
ssh-keygen -t ed25519 -C "github-actions@monteastur" -f "$env:USERPROFILE\.ssh\github-actions-monteastur"

# Salida esperada:
# Your identification has been saved in C:\Users\tuuser\.ssh\github-actions-monteastur
# Your public key has been saved in C:\Users\tuuser\.ssh\github-actions-monteastur.pub

# Verificar archivos generados
Get-ChildItem "$env:USERPROFILE\.ssh\github-actions-monteastur*"
```

### Parámetros explicados

| Parámetro | Valor | Motivo |
|-----------|-------|--------|
| `-t ed25519` | Tipo de clave | Más segura y rápida que RSA. Soportada por GitHub y Ubuntu 22.04+ |
| `-C` | Comentario | Identifica la clave en `authorized_keys` |
| `-f` | Ruta archivo | Nombre descriptivo para el proyecto |
| `-N ""` | Passphrase (opcional) | Vacío para deploy automático sin intervención |

> **¿Passphrase?** Para CI/CD automático debe estar vacía (`-N ""`). Si pones passphrase, el workflow fallará al conectar. La seguridad está en que la clave privada solo se almacena cifrada en GitHub Secrets.

### Archivos generados

| Archivo | Contenido | ¿Subir a Git? |
|---------|-----------|---------------|
| `~/.ssh/github-actions-monteastur` | **Clave PRIVADA** | ❌ **NUNCA** |
| `~/.ssh/github-actions-monteastur.pub` | Clave pública | Sí, se copia al VPS |

---

## B) Copiar clave pública al VPS

### Opción 1: ssh-copy-id (recomendado)

```bash
# Linux / macOS (si tienes acceso por SSH al VPS como deploy)
ssh-copy-id -i ~/.ssh/github-actions-monteastur.pub deploy@<VPS_IP>

# Verificar que la clave se añadió
ssh deploy@<VPS_IP> "cat ~/.ssh/authorized_keys | grep github-actions"
```

### Opción 2: Manual

```bash
# 1. Conectar al VPS
ssh deploy@<VPS_IP>

# 2. Crear directorio .ssh si no existe
mkdir -p ~/.ssh
chmod 700 ~/.ssh

# 3. Añadir clave pública
echo "<contenido_de_github-actions-monteastur.pub>" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# 4. Verificar
cat ~/.ssh/authorized_keys
# → debe mostrar la línea con "github-actions@monteastur"
```

### Opción 3: Desde local

```bash
# Leer clave pública (desde tu máquina local)
cat ~/.ssh/github-actions-monteastur.pub

# Conectar al VPS y añadirla manualmente
ssh deploy@<VPS_IP>
echo "<pegar_clave_aqui>" >> ~/.ssh/authorized_keys
```

### Permisos correctos

```bash
# En el VPS, verificar permisos
ls -la ~/.ssh/
# → drwx------  2 deploy deploy   <fecha> .ssh/          (700)
# → -rw-------  1 deploy deploy   <fecha> authorized_keys  (600)

# Corregir si es necesario
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
```

---

## C) Configurar GitHub Secrets

### Secrets requeridos

Ir a: **GitHub → Repositorio → Settings → Secrets and variables → Actions**

| Secret | Valor | Ejemplo |
|--------|-------|---------|
| `VPS_HOST` | IP o dominio del VPS | `203.0.113.10` o `monteastur.com` |
| `VPS_USER` | Usuario SSH en el VPS | `deploy` |
| `VPS_SSH_KEY` | Clave privada completa (multilinea) | `-----BEGIN OPENSSH PRIVATE KEY-----\n...` |
| `VPS_PORT` | Puerto SSH (opcional) | `22` |

### Cómo copiar la clave privada correctamente

```bash
# En tu máquina local (donde generaste la clave)
cat ~/.ssh/github-actions-monteastur

# La salida completa será algo como:
# -----BEGIN OPENSSH PRIVATE KEY-----
# b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2g...
# ...
# -----END OPENSSH PRIVATE KEY-----

# Copiar TODO el contenido (incluyendo BEGIN y END) al portapapeles
# En Linux: cat ~/.ssh/github-actions-monteastur | xclip -selection clipboard
# En macOS: cat ~/.ssh/github-actions-monteastur | pbcopy
# En Windows: Get-Content ~\.ssh\github-actions-monteastur | Set-Clipboard
```

### Añadir secreto paso a paso

1. Ir a **Settings → Secrets and variables → Actions**
2. Click **New repository secret**
3. Nombre: `VPS_HOST`
   Valor: `203.0.113.10` (la IP real del VPS)
4. Click **Add secret**
5. Repetir para `VPS_USER` → `deploy`
6. Repetir para `VPS_SSH_KEY` → pegar la clave privada completa
7. Repetir para `VPS_PORT` → `22` (opcional)

> ⚠️ **Importante:** La clave privada se pega **exactamente** como se muestra en el terminal. GitHub Secrets maneja automáticamente los saltos de línea. No modificar el formato.

### Verificar secrets configurados

```bash
# Los secrets no se pueden leer una vez guardados.
# Solo se pueden:
#   - Ver los nombres (Settings → Secrets)
#   - Reemplazar (Delete + New)
#   - Usar en workflows con ${{ secrets.VPS_SSH_KEY }}
```

---

## D) Verificar conexión SSH

### Prueba local

```bash
# Probar conexión con la clave generada
ssh -i ~/.ssh/github-actions-monteastur deploy@<VPS_IP>

# Si funciona, deberías ver el prompt del VPS:
# deploy@monteastur-vps:~$

# Probar con verbose (para debug)
ssh -v -i ~/.ssh/github-actions-monteastur deploy@<VPS_IP> exit

# Buscar en la salida:
# Authenticated to <VPS_IP> ([<VPS_IP>]:22)
```

### Prueba con script

```bash
# Usar el script de verificación
./scripts/check-ssh-connection.sh

# Ejemplo de salida exitosa:
# ============================================
#   Monteastur Envios — SSH Connection Check
# ============================================
# Host: 203.0.113.10
# User: deploy
# Port: 22
# Key:  ~/.ssh/github-actions-monteastur
# --------------------------------------------
# [OK] Conexión SSH exitosa
# [OK] Usuario deploy identificado
# [OK] Docker accesible
# --------------------------------------------
```

### Prueba de workflow (simulación)

Para verificar que GitHub Actions puede conectar:

1. Ir a **GitHub → Actions → Deploy Production → Run workflow**
2. Seleccionar branch `develop`
3. Escribir `deploy` en confirmación
4. El workflow ejecutará `pre-deploy-check` y luego `deploy-production`
5. Si SSH falla, el job `deploy-production` mostrará el error

---

## E) Troubleshooting

### "Permission denied (publickey)"

```bash
# Causa: La clave pública no está en authorized_keys del VPS
# Solución:
ssh deploy@<VPS_IP> "cat ~/.ssh/authorized_keys"  # Verificar claves
ssh-copy-id -i ~/.ssh/github-actions-monteastur.pub deploy@<VPS_IP>  # Re-añadir
```

### "bad permissions: ignore key"

```bash
# Causa: Permisos incorrectos en ~/.ssh o authorized_keys
# Solución en el VPS:
chmod 700 ~/.ssh
chmod 600 ~/.ssh/authorized_keys
chmod 600 ~/.ssh/github-actions-monteastur  # (si existe la clave privada en el VPS)
```

### "Host key verification failed"

```bash
# Causa: La clave del host del VPS cambió (reinstalación, IP reasignada)
# Solución local:
ssh-keygen -R "<VPS_IP>"
ssh-keygen -R "<VPS_HOSTNAME>"

# O manualmente:
rm ~/.ssh/known_hosts  # Ojo: borra TODOS los hosts conocidos
```

### "Connection refused"

```bash
# Causa: Puerto 22 no está abierto o SSH no corre
# Solución en el VPS:
sudo systemctl status sshd      # Verificar que SSH está corriendo
sudo ufw status                 # Verificar puerto 22 permitido
sudo lsof -i :22                # Verificar qué proceso escucha
```

### "Connection timed out"

```bash
# Causa: Firewall de red o VPS apagado
# Solución:
ping <VPS_IP>                   # Verificar reachability
# Si no responde, revisar panel de Hetzner/Cloud Provider
```

### fail2ban bloqueó la IP

```bash
# Verificar en el VPS:
sudo fail2ban-client status sshd
# → Status for the jail: sshd
# → |- Total banned: 3
# → `- Banned IP list:  203.0.113.50

# Desbloquear IP:
sudo fail2ban-client set sshd unbanip <TU_IP>

# Añadir IP a la whitelist (opcional):
sudo nano /etc/fail2ban/jail.local
# [sshd]
# ignoreip = <TU_IP_LOCAL> <IP_GITHUB_ACTIONS>
```

### Líneas finales incorrectas (Windows)

```bash
# Si copiaste la clave privada desde Windows PowerShell y usas el
# contenido en GitHub Secrets, asegurar que no hay saltos de línea extra.

# Formato correcto: la clave debe empezar con
# -----BEGIN OPENSSH PRIVATE KEY-----
# y terminar con
# -----END OPENSSH PRIVATE KEY-----
# sin líneas en blanco al inicio o final.
```

### Probar la clave desde GitHub Actions (simulación local)

```bash
# Puedes simular lo que hará GitHub Actions:
ssh -i ~/.ssh/github-actions-monteastur \
  -o StrictHostKeyChecking=no \
  -o UserKnownHostsFile=/dev/null \
  deploy@<VPS_IP> \
  "echo CONNECTION_OK && uname -a && docker ps --format '{{.Names}} {{.Status}}'"
```

---

## F) Buenas prácticas

### Seguridad

| Práctica | Estado |
|----------|--------|
| Clave dedicada para CI/CD (no usar clave personal) | ✅ Recomendado |
| Passphrase vacía para CI/CD automático | ✅ Necesario |
| Clave privada solo en GitHub Secrets | ✅ Obligatorio |
| Clave privada nunca en el repositorio | ✅ Obligatorio |
| `PermitRootLogin no` en VPS | ✅ Recomendado |
| `PasswordAuthentication no` en VPS | ✅ Recomendado |
| `AllowUsers deploy` en VPS | ✅ Recomendado |
| Rotación periódica de claves | ⬜ Recomendado (cada 6 meses) |

### Rotación de claves (cada 6 meses)

```bash
# 1. Generar nueva clave
ssh-keygen -t ed25519 -C "github-actions@monteastur-v2" -f ~/.ssh/github-actions-monteastur-v2

# 2. Copiar pública al VPS
ssh-copy-id -i ~/.ssh/github-actions-monteastur-v2.pub deploy@<VPS_IP>

# 3. Actualizar GitHub Secret VPS_SSH_KEY con la nueva clave privada

# 4. Probar que funciona
ssh -i ~/.ssh/github-actions-monteastur-v2 deploy@<VPS_IP> "echo OK"

# 5. Eliminar clave anterior del VPS
ssh deploy@<VPS_IP> "nano ~/.ssh/authorized_keys"  # Borrar línea antigua
```

### Backup seguro de la clave

> La clave privada de GitHub Actions **no se puede recuperar** una vez guardada en GitHub Secrets.
> Si la pierdes, tendrás que generar una nueva.

```bash
# Opción recomendada: gestor de contraseñas (Bitwarden, 1Password, KeePass)
# Guardar la clave privada como nota segura en el gestor.

# Opción NO recomendada: archivo local sin cifrar
# Si la guardas local, usar cifrado:
gpg -c ~/.ssh/github-actions-monteastur
# → ~/.ssh/github-actions-monteastur.gpg
```

### Resumen de comandos

```bash
# GENERAR
ssh-keygen -t ed25519 -C "github-actions@monteastur" -f ~/.ssh/github-actions-monteastur

# COPIAR PÚBLICA AL VPS
ssh-copy-id -i ~/.ssh/github-actions-monteastur.pub deploy@<VPS_IP>

# VERIFICAR CONEXIÓN
ssh -i ~/.ssh/github-actions-monteastur deploy@<VPS_IP> "docker ps && echo OK"

# MOSTRAR CLAVE PRIVADA (para GitHub Secrets)
cat ~/.ssh/github-actions-monteastur

# PROBAR COMO GITHUB ACTIONS
ssh -i ~/.ssh/github-actions-monteastur \
  -o StrictHostKeyChecking=no \
  -o UserKnownHostsFile=/dev/null \
  deploy@<VPS_IP> "echo SSH_OK"
```

---

> **Documentos relacionados:**
> - [`scripts/check-ssh-connection.sh`](../scripts/check-ssh-connection.sh) — Script para verificar conexión SSH
> - [`docs/LIVE_DEPLOY_PLAN.md`](LIVE_DEPLOY_PLAN.md) — Plan de deploy que usa estos secrets
> - [`docs/PRODUCTION_VPS_RUNBOOK.md`](PRODUCTION_VPS_RUNBOOK.md) — Runbook de operaciones
>
> **Mantenido por:** Equipo Monteastur Envios
> **Próxima revisión:** 2026-06-24
