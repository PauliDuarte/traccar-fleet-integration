# Prueba E2E real de seguimiento de delivery

Este procedimiento recorre la entrada real del sistema:

`OsmAnd -> Traccar -> HTTP forwarding -> broker Camel -> Artemis -> delivery-tracking -> PostgreSQL -> Artemis -> PUSH simulado`.

Los puertos publicados por defecto evitan colisiones frecuentes con instalaciones locales: PostgreSQL `5433`, Artemis Core `61617`, consola Artemis `8162` y delivery API `8083`. Dentro de la red Compose se conservan los puertos estándar.

## 1. Preparar un entorno limpio

> `down -v` elimina la base PostgreSQL de este proyecto. Úselo solo para una ejecución E2E descartable.

```bash
docker compose down -v --remove-orphans
docker compose build
docker compose up -d
docker compose ps
```

Espere hasta que `artemis` y `postgres` estén `healthy` y los siete servicios estén activos.

## 2. Registrar usuario y dispositivo en Traccar

La primera cuenta creada en una instancia vacía de Traccar queda como administradora. No se debe enviar el atributo `administrator` durante el registro público.

```bash
curl -i -X POST http://localhost:8082/api/users \
  -H 'Content-Type: application/json' \
  --data '{"name":"Admin E2E","email":"admin-e2e@example.test","password":"traccar-e2e"}'

curl -i -u 'admin-e2e@example.test:traccar-e2e' \
  -X POST http://localhost:8082/api/devices \
  -H 'Content-Type: application/json' \
  --data '{"name":"Repartidor E2E","uniqueId":"repartidor-01"}'
```

Ambas operaciones deben responder `200`. El `uniqueId` debe coincidir con `repartidor_device_id`; el identificador numérico interno de Traccar no se usa para correlacionar pedidos.

## 3. Crear el pedido

```bash
curl -i -X POST http://localhost:8083/pedidos \
  -H 'Content-Type: application/json' \
  --data '{
    "id":"PED-E2E-001",
    "cliente_nombre":"Cliente E2E",
    "cliente_msisdn":"+595981000001",
    "cliente_fcm_id":"fcm-e2e",
    "direccion_texto":"Destino E2E",
    "lat_destino":-25.2967,
    "lon_destino":-57.6359,
    "radio_llegada_m":150,
    "repartidor_device_id":"repartidor-01"
  }'
```

Debe responder `202` y el pedido queda en `RECIBIDO`.

## 4. Enviar posiciones por Traccar/OsmAnd

Use timestamps Unix crecientes y cercanos a la hora actual. Este ejemplo usa `T0`; sustitúyalo por el resultado de `date +%s` y sume 10 y 20 para los mensajes siguientes.

```bash
T0=$(date +%s)

curl -i "http://localhost:5055/?id=repartidor-01&lat=-25.3100&lon=-57.6500&timestamp=${T0}&speed=10"
curl -i "http://localhost:5055/?id=repartidor-01&lat=-25.2971&lon=-57.6362&timestamp=$((T0+10))&speed=10"
curl -i "http://localhost:5055/?id=repartidor-01&lat=-25.2968&lon=-57.6360&timestamp=$((T0+20))&speed=1"
```

Cada envío debe responder `200`. Las transiciones esperadas son:

1. posición válida lejana: `RECIBIDO -> EN_CAMINO`;
2. distancia menor o igual a 150 m: `EN_CAMINO -> CERCA`;
3. nueva posición dentro del radio y velocidad menor o igual a 3 km/h: `CERCA -> ENTREGADO`.

El parámetro OsmAnd `speed` está expresado en nudos; el modelo canónico lo convierte a km/h.

## 5. Verificar API, base, Artemis y PUSH

```bash
curl -sS http://localhost:8083/pedidos/PED-E2E-001/tracking

docker compose exec -T postgres psql -U delivery -d delivery -c \
  "SELECT id, estado FROM pedidos WHERE id='PED-E2E-001';"

docker compose exec -T postgres psql -U delivery -d delivery -c \
  "SELECT pedido_id, device_id, lat, lon, velocidad_kmh, distancia_destino_m, timestamp
     FROM pedido_ultima_posicion WHERE pedido_id='PED-E2E-001';"

docker compose exec -T postgres psql -U delivery -d delivery -c \
  "SELECT hito, event_type, estado_anterior, estado_nuevo, publicado, notificado
     FROM pedido_eventos WHERE pedido_id='PED-E2E-001' ORDER BY id;"

docker compose exec -T artemis /var/lib/artemis-instance/bin/artemis queue stat \
  --user admin --password admin123 --url tcp://localhost:61616

docker compose logs delivery-tracking | grep 'PUSH SIMULADO'
```

Resultado observado el 18 de septiembre de 2026:

- API: `estado=ENTREGADO`, posición final a `14.99 m` y `1.852 km/h`;
- PostgreSQL: un solo registro para cada hito `RECIBIDO`, `EN_CAMINO`, `CERCA`, `ENTREGADO`;
- los tres eventos de transición quedaron `publicado=true` y `notificado=true`;
- Artemis mostró mensajes confirmados en `vehicle.positions`, `notification.commands` e `ingest`;
- logs: exactamente un PUSH simulado para cada hito notificable.

## 6. Verificar idempotencia y orden temporal

Repita la última URL sin cambiar su timestamp. El pedido debe permanecer `ENTREGADO`, no debe aparecer otro evento ni otro PUSH. También puede verificar la restricción de unicidad:

```bash
docker compose exec -T postgres psql -U delivery -d delivery -c \
  "SELECT pedido_id, hito, count(*) FROM pedido_eventos
     WHERE pedido_id='PED-E2E-001' GROUP BY pedido_id, hito ORDER BY hito;"
```

Cada combinación `(pedido_id, hito)` debe tener `count=1`.
