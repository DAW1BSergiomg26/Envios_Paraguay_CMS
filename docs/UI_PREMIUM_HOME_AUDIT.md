# UI_PREMIUM_HOME_AUDIT

## Estado

Proyecto: Envios_Paraguay_CMS  
Rama: feature/auditoria-ui-premium-home  
Fase: 5 - Auditoria UI/UX Premium  
Tipo: auditoria visual y experiencia de usuario

---

## Proposito

Este documento inicia la Fase 5 del proyecto.

El objetivo es revisar la experiencia visual de la web publica, especialmente la home, para llevar el proyecto desde una base funcional correcta hacia una presencia mas profesional, clara y comercial.

---

## Contexto actual

La aplicacion ya funciona en entorno local con Docker Compose.

Se confirmo en QA real:

- home publica activa;
- version espanola e inglesa;
- panel admin operativo;
- panel cliente operativo;
- tracking publico minimo funcionando;
- endpoint publico sin datos sensibles innecesarios.

La Fase 5 no busca tocar seguridad ni backend de momento.

Busca mejorar percepcion visual, claridad, jerarquia, confianza y experiencia comercial.

---

## Diagnostico general

La web tiene una base funcional solida, pero visualmente todavia transmite una mezcla entre proyecto academico, demo tecnica y sitio comercial.

La oportunidad principal es convertirla en una web mas premium, mas limpia y mas convincente.

---

## Fortalezas actuales

- La web carga correctamente.
- Existe version multiidioma ES/EN.
- La marca Monteastur ya aparece con identidad reconocible.
- El hero tiene fuerza visual por el uso de imagenes, bandera y carga emocional.
- Existen CTAs claros como cotizar envio, rastrear envio y entrar al area cliente.
- La aplicacion ya tiene tracking funcional real.
- El panel cliente aporta valor diferencial.

---

## Problemas visuales detectados en home

### 1. Hero demasiado cargado

El hero mezcla varios elementos fuertes:

- fondo oscuro;
- imagen del camion;
- bandera paraguaya;
- tarjetas flotantes;
- multiples botones;
- textos grandes;
- decoraciones visuales.

Riesgo:

```text
El usuario no sabe donde mirar primero.
```

Recomendacion:

```text
Simplificar la composicion y reforzar un unico mensaje principal.
```

---

### 2. Jerarquia visual mejorable

El titulo principal tiene presencia, pero los subtitulos, badges, tarjetas y CTAs compiten demasiado entre si.

Recomendacion:

```text
Definir una jerarquia clara:
1. Promesa principal.
2. Beneficio principal.
3. CTA principal.
4. CTA secundario.
5. Prueba de confianza.
```

---

### 3. CTAs con distinto peso visual

Los botones principales existen, pero podrian ser mas intencionales:

- Cotizar envio;
- Rastrear envio;
- Area cliente.

Recomendacion:

```text
Dar maximo protagonismo a Rastrear envio y Cotizar envio.
Area cliente debe quedar como acceso secundario pero visible.
```

---

### 4. Sensacion visual todavia no premium

La web funciona, pero necesita mas refinamiento:

- mas espacio controlado;
- mejor escala tipografica;
- tarjetas mas limpias;
- sombras mas suaves;
- colores mas consistentes;
- menos ruido visual.

---

## Problemas UX detectados

### 1. Tracking publico podria estar mas presente

El tracking es una de las funciones principales del producto.

Recomendacion:

```text
Incluir una caja de tracking mas clara y elegante en la home.
```

Ejemplo conceptual:

```text
Introduce tu codigo de envio
[ MT-2026-0001        ] [Rastrear]
```

---

### 2. Propuesta comercial mejorable

La home debe explicar rapido:

```text
Que hacemos.
Para quien.
Por que confiar.
Que hago ahora.
```

Recomendacion:

```text
Crear bloques breves de confianza:
- Espana a Paraguay.
- Seguimiento online.
- Gestion documental.
- Atencion personalizada.
```

---

### 3. Falta prueba social o confianza visible

Recomendacion:

```text
Anadir microseccion de confianza:
- envios gestionados;
- clientes atendidos;
- seguimiento 24/7;
- rutas Espana-Paraguay.
```

Si los datos son demo, deben presentarse con cuidado para no parecer falsos.

---

## Responsive

Pendiente de revisar en profundidad.

Hipotesis inicial:

```text
El hero puede volverse pesado en movil por la cantidad de capas visuales.
```

Recomendacion:

```text
En movil, priorizar:
1. titulo;
2. CTA rastrear;
3. CTA cotizar;
4. imagen secundaria o reducida.
```

---

## Linea visual recomendada

### Estilo

```text
Logistica premium, humana y confiable.
```

No debe parecer una app fria ni una web generica.

Debe unir:

- tecnologia;
- confianza;
- cercania;
- identidad Espana-Paraguay;
- profesionalidad logistica.

---

## Inspiraciones utiles

- DHL para claridad logistica.
- FedEx para tracking simple.
- Stripe para limpieza visual.
- Linear para orden y tarjetas modernas.
- Apple para respiracion y jerarquia.

No copiar estilos, solo aprender de su claridad.

---

## Primeros cambios recomendados

### Fase 5.1 - Hero premium

Rama sugerida:

```text
feature/home-hero-premium
```

Objetivo:

```text
Redisenar hero sin romper rutas ni backend.
```

Cambios:

- simplificar headline;
- mejorar subtitulo;
- ordenar CTAs;
- crear bloque de tracking mas visible;
- reducir ruido visual;
- mejorar responsive.

---

### Fase 5.2 - Tracking publico visual

Rama sugerida:

```text
feature/tracking-publico-premium-ui
```

Objetivo:

```text
Convertir la respuesta de tracking en una experiencia visual mas profesional.
```

---

### Fase 5.3 - Panel cliente premium

Rama sugerida:

```text
feature/cliente-panel-premium-ui
```

Objetivo:

```text
Mejorar legibilidad, cards, timeline, evidencias y sensacion de producto real.
```

---

## Cambios no recomendados todavia

```text
No tocar backend.
No tocar seguridad.
No mezclar home, cliente y admin en una sola rama.
No redisenar todo de golpe.
No cambiar textos legales o rutas sensibles en esta fase.
No hacer cambios gigantes en una unica feature.
```

---

## Primer cambio tecnico recomendado

Crear una rama pequena:

```text
feature/home-hero-premium
```

Y trabajar solo:

```text
home publica
hero
CTA principal
bloque tracking
responsive inicial
```

---

## Criterios de aceptacion

- La home carga correctamente.
- No se rompe el cambio de idioma.
- Los CTAs siguen funcionando.
- El tracking sigue accesible.
- La home se ve mas limpia.
- El hero tiene menos ruido visual.
- Mobile no queda saturado.

---

## Decision actual

Estado: auditoria UI premium de home creada.  
Siguiente paso: integrar esta auditoria en develop y abrir rama `feature/home-hero-premium`.

---

## Frase guia

Una web funcional demuestra que el sistema vive.

Una web premium demuestra que el negocio inspira confianza.
