# Impact.X Mobile Prototype

Prototipo de interfaz móvil para Impact.X hecho con **HTML, CSS y JavaScript puro**. No usa frameworks ni librerías externas. Está pensado únicamente para maquetado y simulación funcional.

## Cómo abrirlo

1. Descomprime la carpeta.
2. Abre `index.html` en cualquier navegador moderno.
3. Navega usando los botones de la interfaz móvil.

## Qué incluye

- Maqueta visual tipo smartphone.
- Navegación SPA con rutas `#/...`.
- Persistencia simulada con `localStorage`.
- Splash, bienvenida, login, registro y recuperación simulada.
- Selección de planes: Trial, Básico y Premium.
- Onboarding completo:
  - permisos móviles,
  - perfil del conductor,
  - datos médicos,
  - datos del vehículo,
  - vinculación de smartwatch,
  - permisos Wear OS,
  - calibración,
  - contactos iniciales,
  - red de monitoreo,
  - resumen de activación.
- Dashboard móvil de protección activa.
- Estado del smartwatch y sensores.
- Contactos de emergencia con límite por plan.
- Simulación de cache local para contactos, rutas y mensajes internos.
- Red de monitores con invitaciones internas por usuario/ID único, tokens, activar, revocar y restaurar acceso.
- Suscripción, cambio de plan, pago simulado y vencimiento simulado.
- Historial de incidentes, detalle, línea de tiempo y mapa simulado.
- Notificaciones internas.
- Perfil, configuración, seguridad, privacidad, sincronización local y ayuda.
- Flujo SOS completo:
  - SOS manual,
  - detección automática,
  - pantalla “¿Estás bien?”,
  - temporizador,
  - falsa alarma,
  - envío por chat interno,
  - modo interno offline con cache local,
  - alerta activa,
  - cierre de incidente.
- Modo monitor/contacto invitado:
  - aceptar invitación,
  - inicio de monitor,
  - alerta recibida,
  - llamada, ruta y “voy en camino” simulados.

## Flujos recomendados para probar

### Flujo titular normal

1. `Continuar` desde Splash.
2. Crear cuenta o iniciar sesión.
3. Si es cuenta nueva, elegir plan inicial; si solo inicias sesión, se carga el plan existente.
4. Completar permisos, perfil, datos médicos, vehículo y wearable.
5. Agregar contacto o usar los contactos demo.
6. Llegar a Inicio / Protección activa.

### Flujo SOS con internet

1. Ir a Inicio.
2. Tocar `SOS MANUAL` o `Posible choque`.
3. Confirmar envío.
4. Completar envío.
5. Ver alerta activa.
6. Cerrar incidente.

### Flujo SOS offline

1. Ir a Inicio o Sincronización.
2. Tocar `Quitar internet`.
3. Activar SOS.
4. La app cambia a modo `cache local + chat interno pendiente`.
5. Finalizar alerta y guardar incidente.

### Flujo de plan

1. Ir a Perfil.
2. Entrar a Suscripción.
3. Cambiar entre Trial, Básico y Premium.
4. Revisar cómo cambian sensores, límites y bypass crítico.

## Archivos

- `index.html`: estructura base.
- `styles.css`: diseño visual móvil.
- `app.js`: lógica de navegación, estado y simulación.
- `README.md`: guía de uso.

## Nota

Este prototipo es exclusivamente de maquetado/simulación. No envía SMS reales, no usa GPS real, no procesa pagos, no conecta con smartwatch real y no consume APIs reales. Sin embargo, la lógica está organizada para que después pueda migrarse a Kotlin/Android o integrarse con backend .NET.

## Actualización v2: modo conducción Android

Se agregó el flujo de conducción Android. En versiones posteriores se ajustó para que el viaje se inicie desde el wearable y el móvil solo lo visualice.

Incluye:

- El botón principal `Iniciar viaje` ya no aparece en Inicio; se conserva la simulación solo como compatibilidad interna y demo desde wearable.
- Pantalla de preparación del viaje.
- Checklist realista antes de conducir:
  - sesión activa,
  - plan vigente,
  - wearable conectado,
  - GPS,
  - Bluetooth,
  - servicio en segundo plano,
  - contactos activos,
  - SQLite sincronizado,
  - chat interno habilitado,
  - batería suficiente del reloj.
- Consentimientos simulados:
  - ubicación continua,
  - ejecución en segundo plano,
  - compartir ubicación con monitores solo durante SOS.
- Pantalla `Viaje activo` con:
  - velocímetro,
  - ruta,
  - tiempo,
  - distancia,
  - precisión GPS,
  - Fuerza G,
  - ruido,
  - ritmo cardíaco,
  - riesgo calculado,
  - estado nube/chat interno.
- Acciones durante viaje:
  - pausar/reanudar,
  - simular pérdida de internet,
  - simular bache fuerte,
  - simular choque detectado,
  - SOS manual durante viaje,
  - finalizar viaje.
- Pantalla de resumen del viaje.
- Historial local de viajes con `localStorage`.
- Ajuste visual a formato Android: pantalla 412×915, cámara tipo punch-hole, bordes menos redondeados y barra gestual inferior.

Todo sigue siendo maquetado/simulación; no usa GPS, mensajería real, Bluetooth ni Wear OS reales.

## Actualización v3: sincronización por código con wearable

Se agregó una pantalla concreta para vincular el teléfono Android con el wearable mediante código de sincronización.

Incluye:

- Ruta nueva `#/wearable-code`.
- Pantalla previa de detección Bluetooth/Wear OS con código visible en el reloj.
- Código demo de 6 dígitos mostrado como si apareciera en el Galaxy Watch.
- Campo de entrada tipo PIN.
- Teclado numérico simulado.
- Validación de código correcto/incorrecto.
- Botón `Autocompletar demo` para probar el flujo rápido.
- Generación de nuevo código temporal.
- Timeline realista de vinculación:
  - código temporal,
  - handshake seguro,
  - permisos Wear OS,
  - sincronización final.
- Token de confianza simulado.
- Sesión de emparejamiento simulada.
- Notificación interna al completar la vinculación.
- Acceso también desde `Dispositivo > Sincronizar por código`.

Flujo recomendado:

1. Ir a `Vehículo`.
2. Tocar `Guardar y vincular reloj`.
3. Entrar a `Sincronizar con código`.
4. Usar el código mostrado en pantalla o tocar `Autocompletar demo`.
5. Tocar `Validar y vincular`.
6. Continuar a permisos del reloj.

Sigue siendo solo simulación visual/funcional; no utiliza Bluetooth real, Wear OS real ni claves criptográficas reales.

## Actualización v4: vehículos de 4 ruedas y ficha médica concreta

Se mantuvo todo lo anterior y se hicieron ajustes puntuales al flujo móvil:

- La pantalla `Vehículo` ahora está enfocada únicamente en vehículos de 4 ruedas.
- Se removieron opciones fuera del caso de uso y se dejaron opciones como:
  - Auto,
  - SUV,
  - Camioneta,
  - Pickup,
  - Van / Minivan.
- Se agregó normalización interna para que, si el navegador conservaba datos antiguos en `localStorage`, el tipo de vehículo se corrija automáticamente a una opción válida.
- La pantalla `Datos médicos` ahora usa preguntas más claras:
  - `¿Tienes algún padecimiento o condición médica?`
  - `¿Qué alergias tienes?`
  - `¿Qué medicamento tomas actualmente?`
- La vista de alerta para monitor ahora muestra padecimiento, alergias, medicamento y nota de emergencia de forma separada.
- El perfil móvil también refleja padecimiento y medicamento para que el titular pueda validar rápido su ficha médica.

Todo sigue siendo simulación/maquetado; no se eliminó ningún flujo previo.

## Actualización v5: chat interno, rutas frecuentes y viaje iniciado desde wearable

Se mantuvo lo anterior y se modificó solo lo necesario para alinear el móvil con el nuevo flujo del proyecto:

- Se agregó **chat interno** como único canal de comunicación dentro de Impact.X.
- Las alertas, avisos de ruta, invitaciones y mensajes precargados ya no se plantean por SMS, WhatsApp ni correo.
- Las invitaciones ahora funcionan como una **solicitud interna**, similar a agregar un amigo o unirse a una familia compartida.
- Se agregó pantalla `#/chat` con:
  - red familiar,
  - conversación interna,
  - mensajes del sistema,
  - mensajes de ruta,
  - plantillas rápidas.
- Se agregó pantalla `#/templates` para mensajes precargados:
  - inicio de ruta,
  - cambio de ruta,
  - llegué bien,
  - retraso,
  - alerta SOS.
- Se agregó pantalla `#/routes` para **rutas frecuentes por etiquetas**:
  - nombre de ruta,
  - etiqueta visible,
  - origen,
  - destino,
  - nota,
  - seleccionar ruta del día,
  - usar y avisar por chat interno.
- El botón anterior de iniciar viaje desde el móvil dejó de mostrarse en Inicio.
- Ahora el móvil solo muestra el viaje cuando recibe el evento desde el wearable.
- En `Dispositivo` se dejó una acción de demo llamada `Recibir inicio de viaje desde wearable` para simular que el reloj inició el viaje.
- El viaje activo ahora se presenta como **inicio desde Wear OS**, con la app móvil únicamente visualizando telemetría, ruta y comunicación interna.
- La sincronización local ahora habla de cache de contactos, rutas y mensajes internos.

Nota: el flujo viejo se conservó internamente para compatibilidad de la maqueta, pero la interfaz principal ya no ofrece iniciar el viaje desde el teléfono.


## Actualización v6: chat amigable, usuario único e invitaciones internas

Se mantuvo todo lo anterior y se modificó lo necesario para que el chat interno y las invitaciones sean más claras para el usuario final.

Incluye:

- El chat interno ahora es más visible y amigable, con una tarjeta principal que explica que es el único canal oficial de Impact.X.
- Se agregaron acciones rápidas desde Inicio:
  - preparar ruta de hoy,
  - abrir chat interno,
  - invitar por usuario/ID.
- El chat ahora simula respuestas recibidas de los contactos o monitores, por ejemplo: “Recibido, estaré pendiente desde Impact.X”.
- Se agregó envío de mensajes personalizados, no solo plantillas precargadas.
- La pantalla de mensajes precargados conserva las plantillas, pero también permite escribir mensajes libres.
- Las rutas frecuentes ya no dependen de entrar al chat; ahora se pueden crear, elegir y avisar desde `#/routes`.
- Se agregó pantalla `#/invite-user` para invitar/agregar personas mediante:
  - nombre de usuario único, por ejemplo `maria_tejeda`,
  - ID único Impact.X, por ejemplo `IX-MAR-7731`.
- El registro ahora pide `Nombre de usuario único`.
- El login ahora acepta correo o nombre de usuario.
- Al iniciar sesión ya no se manda al usuario a elegir plan; se carga el plan existente y continúa al flujo de permisos/onboarding o al Inicio.
- El perfil muestra nombre de usuario e ID Impact.X del titular.
- Contactos y monitores muestran usuario/ID para reforzar que la red funciona como una familia interna tipo Facebook/Spotify.

Todo sigue siendo únicamente maqueta funcional; no hay usuarios reales, backend real ni mensajería externa.


## Versión v7 — Paleta Impact.X

Cambios visuales aplicados sin eliminar pantallas ni funciones previas:

- Fondo móvil claro `#F4F7FB` con cards blancas `#FFFFFF`.
- Header Android en azul noche `#081A2E`.
- Paneles secundarios y zonas de monitoreo con azul petróleo `#0F2A44` cuando aplica.
- Botón principal en teal seguridad `#00A6A6` y hover `#008B8B`.
- Acciones de rutas, chat, sincronización e IDs con azul confianza `#2563EB` y cyan tecnológico `#06B6D4`.
- Estados conectados/activos en verde `#22C55E`.
- Pendientes, avisos y límites en ámbar vial `#F59E0B`.
- Rojo `#EF4444` reservado para SOS, accidente, riesgo crítico, eliminación o revocación.
- Chat interno diferenciado: mensaje propio teal, recibido gris azulado, sistema ámbar e información/rutas azul.
- Rutas frecuentes con puntos de inicio teal, destino ámbar y mapas simulados azul claro.
- Perfil médico con tono verde suave y lectura más seria.
