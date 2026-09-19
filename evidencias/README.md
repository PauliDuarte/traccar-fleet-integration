# Checklist de evidencias manuales

Evidencias reales tomadas durante la validación final del proyecto:

- [x] `01-docker-compose.png`: siete servicios activos y healthchecks.
- [x] `02-gradle-tests.png`: suite Gradle completa en verde.
- [x] `03-post-pedido.png`: respuesta de `POST /pedidos`.
- [x] `04-db-recibido.png`: pedido persistido en estado `RECIBIDO`.
- [x] `05-traccar-position.png`: posición recibida por Traccar.
- [x] `06-en-camino.png`: transición a `EN_CAMINO`.
- [x] `07-cerca.png`: transición a `CERCA`.
- [x] `08-entregado.png`: transición a `ENTREGADO`.
- [x] `09-tracking-final.png`: respuesta final de `GET /pedidos/{id}/tracking`.
- [x] `10-pedido-eventos.png`: eventos, `publicado` y `notificado` en PostgreSQL.
- [x] `11-artemis.png`: direcciones y colas relevantes en Artemis.
- [x] `12-notificaciones.png`: notificaciones PUSH simuladas.
- [x] `13-idempotencia.png`: un registro por `(pedido_id, hito)`.

Estas evidencias corresponden al flujo end to end validado.
