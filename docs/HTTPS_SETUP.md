# Monteastur Envios — Configuraci\u00f3n HTTPS con Let's Encrypt

## Arquitectura

```
Cliente --- HTTPS (443) ---> Nginx --- HTTP (8080) ---> Spring Boot
                                |
                          Let's Encrypt
```

## Requisitos previos

- Dominio configurado (ej: `monteastur.com`) apuntando a la IP del VPS
- Puerto 80 accesible desde internet (para validaci\u00f3n Let's Encrypt)
- Nginx funcionando en el VPS

## Paso 1: Verificar dominio

```bash
# Verificar que el dominio resuelve a la IP del VPS
dig +short monteastur.com
curl -I http://monteastur.com
```

Debe responder con el header `Server: nginx/1.31.1` o similar.

## Paso 2: Obtener certificado SSL

Usando Certbot con el perfil de Docker Compose:

```bash
cd /opt/monteastur

docker compose --profile certbot run --rm certbot certonly \
  --webroot -w /var/www/certbot \
  -d monteastur.com \
  -d www.monteastur.com \
  --email admin@monteastur.com \
  --agree-tos \
  --non-interactive
```

Esto genera los certificados en el volumen `certbot_www` y los guarda en `./nginx/ssl/live/`.

## Paso 3: Copiar certificados

```bash
# Copiar certificados al directorio esperado por Nginx
cp ./nginx/ssl/live/monteastur.com/fullchain.pem ./nginx/ssl/
cp ./nginx/ssl/live/monteastur.com/privkey.pem  ./nginx/ssl/
chmod 644 ./nginx/ssl/fullchain.pem
chmod 600 ./nginx/ssl/privkey.pem
```

## Paso 4: Configurar HTTPS en Nginx

Editar `nginx/conf.d/monteastur.conf` y descomentar el bloque `server` de HTTPS (puerto 443):

```nginx
server {
    listen 443 ssl http2;
    server_name monteastur.com;

    ssl_certificate /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:...;
    ssl_prefer_server_ciphers on;
    ...
}
```

## Paso 5: Redirigir HTTP a HTTPS

En el bloque HTTP (puerto 80), a\u00f1adir redirect:

```nginx
server {
    listen 80;
    server_name monteastur.com www.monteastur.com;

    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    return 301 https://$host$request_uri;
}
```

## Paso 6: Recargar Nginx

```bash
docker compose restart nginx
```

Verificar HTTPS:
```bash
curl -I https://monteastur.com
```

Debe responder `HTTP/2 200` con `Server: nginx/1.31.1`.

## Renovaci\u00f3n autom\u00e1tica

A\u00f1adir al crontab (`crontab -e`):

```cron
# Renovar certificados cada 2 meses (valen 90 d\u00edas)
0 3 1 */2 * docker compose --profile certbot run --rm certbot renew --webroot -w /var/www/certbot --quiet && docker compose restart nginx
```

## Verificar seguridad SSL

```bash
# Usar curl
curl -sI https://monteastur.com | grep -i "strict-transport"

# O escanear con test externo
# https://www.ssllabs.com/ssltest/analyze.html?d=monteastur.com
```

## Checklist HTTPS final

- [ ] Certificado v\u00e1lido (no expirado)
- [ ] HTTP → HTTPS redirect 301
- [ ] HSTS header presente (`max-age=31536000`)
- [ ] TLS 1.2 y 1.3 habilitados
- [ ] TLS 1.0 y 1.1 deshabilitados
- [ ] SSL Labs calificaci\u00f3n A o superior
