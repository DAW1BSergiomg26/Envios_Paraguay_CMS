# QA_BROWSER_CHECKLIST

## Estado

Proyecto: Envios_Paraguay_CMS  
Rama: feature/checklist-qa-navegador  
Fase: 4 - QA y estabilizacion funcional  
Tipo: checklist manual de navegador

---

## Proposito

Esta checklist sirve para probar la aplicacion como usuario real desde navegador.

No sustituye a los tests automatizados, pero ayuda a detectar errores visibles de flujo, permisos, sesiones, enlaces, archivos y experiencia de uso.

---

## Preparacion

Antes de empezar:

- [ ] Estar en la rama correcta.
- [ ] Tener la aplicacion levantada localmente o en entorno de prueba.
- [ ] Tener usuario admin valido.
- [ ] Tener usuario cliente valido.
- [ ] Tener al menos un envio asociado al cliente.
- [ ] Tener al menos una evidencia visible.
- [ ] Tener al menos una evidencia no visible.
- [ ] Tener un codigo de tracking publico valido.

Comando recomendado de validacion tecnica previa:

```powershell
mvn -DskipTests package
```

Resultado esperado:

```text
BUILD SUCCESS
```

---

## 1. Login admin

Ruta sugerida:

```text
/login
```

Pasos:

- [ ] Abrir navegador.
- [ ] Entrar en `/login`.
- [ ] Iniciar sesion con usuario admin.
- [ ] Confirmar redireccion al panel admin.

Resultado esperado:

```text
El admin entra correctamente al panel.
```

---

## 2. Panel admin

Ruta:

```text
/admin/dashboard
```

Pasos:

- [ ] Confirmar que carga el dashboard.
- [ ] Confirmar que no aparecen errores visibles.
- [ ] Revisar contadores y secciones principales.

Resultado esperado:

```text
Dashboard visible y estable.
```

---

## 3. Crear o editar envio

Ruta sugerida:

```text
/admin/tracking
```

Pasos:

- [ ] Entrar en listado de tracking.
- [ ] Crear nuevo envio o editar uno existente.
- [ ] Asociar cliente si aplica.
- [ ] Guardar cambios.
- [ ] Volver al listado.

Resultado esperado:

```text
El envio se guarda y aparece en el listado.
```

---

## 4. Subir evidencia

Ruta sugerida:

```text
/admin/tracking/editar/{id}
```

Pasos:

- [ ] Abrir un envio.
- [ ] Subir evidencia tipo FOTO con JPG/PNG/WEBP.
- [ ] Subir evidencia tipo DOCUMENTO con PDF.
- [ ] Intentar subir archivo no permitido.

Resultado esperado:

```text
Archivos permitidos se guardan.
Archivos no permitidos muestran error.
```

---

## 5. Mostrar y ocultar evidencia

Pasos:

- [ ] Localizar una evidencia del envio.
- [ ] Cambiar visibilidad.
- [ ] Confirmar mensaje de exito.
- [ ] Entrar como cliente y comprobar si aparece o desaparece.

Resultado esperado:

```text
visibleCliente controla lo que aparece en zona cliente.
```

---

## 6. Login cliente

Ruta sugerida:

```text
/cliente/login
```

Pasos:

- [ ] Cerrar sesion admin si hace falta.
- [ ] Entrar como cliente.
- [ ] Confirmar acceso al area cliente.

Resultado esperado:

```text
El cliente entra solo a su zona.
```

---

## 7. Cliente ve sus envios

Ruta/API:

```text
/api/v1/cliente/envios
```

Pasos:

- [ ] Desde sesion cliente, abrir la zona de envios.
- [ ] Confirmar que se listan sus envios.
- [ ] Confirmar que no se muestran envios de otro cliente.

Resultado esperado:

```text
Solo aparecen envios del cliente autenticado.
```

---

## 8. Cliente ve detalle de envio propio

Ruta/API:

```text
/api/v1/cliente/envios/{codigo}
```

Pasos:

- [ ] Abrir envio propio.
- [ ] Ver detalle.
- [ ] Revisar eventos.
- [ ] Revisar evidencias.

Resultado esperado:

```text
El detalle carga correctamente y muestra evidencias visibles.
```

---

## 9. Cliente descarga evidencia visible

Ruta/API esperada:

```text
/api/v1/cliente/evidencias/{id}/archivo
```

Pasos:

- [ ] Pulsar enlace de evidencia visible.
- [ ] Confirmar que abre o descarga el archivo.
- [ ] Confirmar que la URL ya no apunta directamente a /uploads/evidencias/.

Resultado esperado:

```text
La descarga pasa por el endpoint controlado y devuelve 200.
```

---

## 10. Cliente no accede a evidencia no visible

Pasos:

- [ ] Copiar URL controlada de una evidencia no visible si se conoce.
- [ ] Intentar abrirla con sesion cliente.

Resultado esperado:

```text
403 Forbidden.
```

---

## 11. Cliente no accede a evidencia ajena

Pasos:

- [ ] Intentar abrir URL de evidencia de otro cliente.

Resultado esperado:

```text
403 Forbidden.
```

---

## 12. Sin sesion no accede a evidencias

Pasos:

- [ ] Abrir ventana privada o cerrar sesion.
- [ ] Intentar abrir `/api/v1/cliente/evidencias/{id}/archivo`.

Resultado esperado:

```text
403 Forbidden.
```

---

## 13. Tracking publico

Ruta/API:

```text
/api/v1/tracking/{codigo}
```

Pasos:

- [ ] Abrir codigo publico valido.
- [ ] Confirmar datos visibles.
- [ ] Confirmar que no aparecen destinatario, peso ni contenido.

Resultado esperado:

```text
Tracking publico muestra informacion minima.
```

---

## 14. Tracking inexistente

Ruta/API:

```text
/api/v1/tracking/{codigo_inexistente}
```

Resultado esperado:

```text
404 Not Found.
```

---

## 15. Push test local o demo

Ruta/API:

```text
POST /api/v1/push/test
```

Contexto:

```text
Perfil no productivo.
```

Resultado esperado:

```text
200 OK con mensaje de simulacion.
```

---

## 16. Push test produccion

Ruta/API:

```text
POST /api/v1/push/test
```

Contexto:

```text
Perfil activo contiene prod.
```

Resultado esperado:

```text
403 Forbidden.
```

---

## 17. Revisar rutas directas antiguas

Ruta:

```text
/uploads/evidencias/<archivo>
```

Resultado actual esperado:

```text
Puede seguir funcionando si el archivo existe, porque /uploads/** aun esta publicado.
```

Decision:

```text
No cerrar todavia hasta separar imagenes publicas y evidencias privadas.
```

---

## Resultado final de QA manual

Marcar al terminar:

- [ ] Admin probado.
- [ ] Cliente probado.
- [ ] Tracking publico probado.
- [ ] Evidencias visibles probadas.
- [ ] Evidencias no visibles probadas.
- [ ] Evidencias ajenas probadas.
- [ ] Build Maven probado.
- [ ] Incidencias anotadas.

---

## Incidencias encontradas

Usar este espacio durante la prueba:

```text
1.
2.
3.
```

---

## Frase guia

El codigo puede compilar y aun asi fallar como producto.

Por eso se prueba con ojos de usuario real.
