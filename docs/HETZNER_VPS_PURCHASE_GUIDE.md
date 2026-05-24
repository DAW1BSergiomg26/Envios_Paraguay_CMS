# Guía de Compra — VPS Hetzner para Producción

Guía paso a paso para contratar el VPS de MonteAstur en Hetzner Cloud.

---

## A) Crear cuenta en Hetzner

### 1. Registro

- Ir a [hetzner.com](https://www.hetzner.com/)
- Click en **Cloud Console** → **Register**
- Rellenar: nombre, email, contraseña
- Aceptar términos

### 2. Verificación de identidad

Hetzner exige verificación obligatoria. Prepara:

| Documento | Formato | Nota |
|-----------|---------|------|
| DNI/NIE/pasaporte | Escaneado o foto | Vigente |
| Comprobante de domicilio | Factura reciente (<3 meses) | Banco, luz, agua, internet |

**Times normal:** 2-24 horas hábiles.

Si no llega en 24h:
- Revisar spam
- Contactar soporte via ticket
- Subir documentación con mejor resolución

### 3. Añadir método de pago

| Método | Nota |
|--------|------|
| PayPal | Más fácil, protección comprador |
| Tarjeta crédito/débito | Directo |
| Transferencia bancaria | Lento (no recomendado para empezar) |

**Recomendado:** PayPal.

---

## B) Crear servidor

### 1. Entrar a Cloud Console

- [console.hetzner.cloud](https://console.hetzner.cloud/)
- Crear o seleccionar un **Project**
- Click **Add Server**

### 2. Configuración exacta

| Campo | Valor recomendado | Alternativa |
|-------|-------------------|-------------|
| Location | **Nuremberg (NBG)** | Helsinki (HEL) |
| Image | **Ubuntu 24.04 LTS** | Ubuntu 22.04 LTS |
| Type | **CX22** (2 vCPU, 4 GB, 40 GB SSD) | CX32 (si más RAM) |
| IPv4 | **Sí** | — |
| IPv6 | Sí (opcional) | — |
| SSH Key | **Subir clave pública** (ver abajo) | Contraseña temporal |
| Backups | **Sí** (+20% precio) | No |
| Firewall | **Recomendado** (ver reglas abajo) | Configurar después |

### 3. Subir SSH Key (recomendado)

Antes de crear el servidor, en la sección **SSH Keys**:

```bash
# En tu máquina LOCAL (Windows/PowerShell)
ssh-keygen -t ed25519 -f "$env:USERPROFILE\.ssh\hetzner_root_ed25519" -N ""

# Ver clave pública
cat "$env:USERPROFILE\.ssh\hetzner_root_ed25519.pub"
```

Pegar el contenido en el formulario de Hetzner.

Si no subes SSH Key, usarás contraseña (menos seguro).

### 4. Reglas de Firewall Cloud (recomendado)

| Dirección | Protocolo | Puerto | Origen | Nota |
|-----------|-----------|--------|--------|------|
| In | TCP | 22 | 0.0.0.0/0 | SSH |
| In | TCP | 80 | 0.0.0.0/0 | HTTP |
| In | TCP | 443 | 0.0.0.0/0 | HTTPS |
| In | ICMP | — | 0.0.0.0/0 | Ping |

**No abrir puertos de monitoring** (9090, 3000, 3001) públicamente.

### 5. Crear servidor

- Click **Create & Buy Now**
- Esperar 1-3 minutos
- Anotar **IP pública** (ej. `203.0.113.10`)
- Guardar contraseña temporal si no usaste SSH Key

---

## C) Qué NO elegir

Evitar estos errores al pedir el servidor:

| ❌ Opción | Por qué |
|-----------|---------|
| Windows Server | No compatible con Docker/Spring Boot |
| Kubernetes (K8s) | Excesivo para 1 app monolítica |
| Load Balancer | No necesario para un solo VPS |
| Volúmenes extra | 40 GB SSD es suficiente |
| CX11 (2 GB RAM) | MySQL + app + monitoring no caben |
| Dedicated Server | Carísimo (~40€/mes), no necesario |
| Floating IP | Solo si tienes multi-VPS |
| Ubuntu 20.04 | Desactualizado, no recomendado |

---

## D) Primer login

```bash
# Si usaste SSH Key
ssh root@<IP_DEL_VPS>

# Si usaste contraseña (te la pide Hetzner)
ssh root@<IP_DEL_VPS>
# Password: <la que te dio Hetzner>
# Te pedirá cambiarla al entrar
```

### Una vez dentro:

```bash
# 1. Verificar sistema
cat /etc/os-release

# 2. Actualizar TODO
apt update && apt upgrade -y

# 3. Instalar herramientas útiles
apt install -y htop curl wget git ufw

# 4. Crear usuario deploy
adduser deploy
usermod -aG sudo deploy

# 5. Configurar SSH para deploy (desde local)
# En tu máquina local:
ssh-copy-id deploy@<IP_DEL_VPS>

# 6. Hardening básico
sed -i 's/^PermitRootLogin yes/PermitRootLogin no/' /etc/ssh/sshd_config
sed -i 's/^#PasswordAuthentication yes/PasswordAuthentication no/' /etc/ssh/sshd_config
echo "AllowUsers deploy" >> /etc/ssh/sshd_config
systemctl restart sshd

# 7. PROBAR en OTRA terminal antes de cerrar
# ssh deploy@<IP_DEL_VPS>
```

---

## E) Checklist VPS listo

- [ ] Cuenta Hetzner verificada
- [ ] Método de pago añadido
- [ ] VPS CX22 creado con Ubuntu 24.04
- [ ] IP pública anotada
- [ ] SSH Key subida o contraseña guardada
- [ ] Firewall Cloud activo (22, 80, 443)
- [ ] Backups activados
- [ ] Primer login exitoso (root)
- [ ] Sistema actualizado
- [ ] Usuario `deploy` creado
- [ ] SSH funciona para `deploy` sin contraseña
- [ ] `PermitRootLogin no` configurado
- [ ] `PasswordAuthentication no` configurado
- [ ] Conexión `deploy` probada antes de cerrar root

---

## Costes

| Concepto | Precio |
|----------|--------|
| CX22 (2 vCPU, 4 GB, 40 GB) | ~€4.50/mes |
| Backup (+20%) | ~€0.90/mes |
| IPv4 incluida | €0 |
| **Total VPS** | **~€5.40/mes** |
| **Primer mes** (setup + prorrateo) | ~€5.40 |

Sin backup: ~€4.50/mes.
