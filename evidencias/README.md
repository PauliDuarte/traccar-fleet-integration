# Checklist de evidencias manuales

Esta carpeta no contiene capturas generadas o simuladas. Antes de la entrega, tomar las evidencias reales y guardarlas con estos nombres:

- [ ] `01-docker-compose.png`: siete servicios activos y healthchecks.
- [ ] `02-gradle-tests.png`: suite Gradle completa en verde.
- [ ] `03-post-pedido.png`: respuesta de `POST /pedidos`.
- [ ] `04-db-recibido.png`: pedido persistido en estado `RECIBIDO`.
- [ ] `05-traccar-position.png`: posición recibida por Traccar/forwarding.
- [ ] `06-en-camino.png`: transición a `EN_CAMINO`.
- [ ] `07-cerca.png`: transición a `CERCA`.
- [ ] `08-entregado.png`: transición a `ENTREGADO`.
- [ ] `09-tracking-final.png`: respuesta final de `GET /pedidos/{id}/tracking`.
- [ ] `10-pedido-eventos.png`: eventos, `publicado` y `notificado` en PostgreSQL.
- [ ] `11-artemis.png`: direcciones/colas relevantes en Artemis.
- [ ] `12-notificaciones.png`: tres líneas `PUSH SIMULADO`.
- [ ] `13-idempotencia.png`: un registro por `(pedido_id, hito)` tras reprocesar.
- [ ] `14-github-commits.png`: historial de commits de las ramas del proyecto.
- [ ] `15-github-prs.png`: pull requests reales del trabajo colaborativo.

Solo enlazar una imagen desde el README principal después de agregar el archivo real.
