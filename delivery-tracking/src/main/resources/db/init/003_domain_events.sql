ALTER TABLE pedido_eventos ADD COLUMN IF NOT EXISTS message_id VARCHAR(64);
ALTER TABLE pedido_eventos ADD COLUMN IF NOT EXISTS event_type VARCHAR(50);
ALTER TABLE pedido_eventos ADD COLUMN IF NOT EXISTS device_id VARCHAR(64);
ALTER TABLE pedido_eventos ADD COLUMN IF NOT EXISTS estado_anterior VARCHAR(20);
ALTER TABLE pedido_eventos ADD COLUMN IF NOT EXISTS estado_nuevo VARCHAR(20);
ALTER TABLE pedido_eventos ADD COLUMN IF NOT EXISTS distancia_destino_m DOUBLE PRECISION;
ALTER TABLE pedido_eventos ADD COLUMN IF NOT EXISTS publicado BOOLEAN NOT NULL DEFAULT FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uq_pedido_eventos_message_id
    ON pedido_eventos (message_id) WHERE message_id IS NOT NULL;
