INSERT INTO repartidores (device_id, nombre, msisdn) VALUES
    ('repartidor-01', 'Repartidor Demo 01', '+595981000001'),
    ('repartidor-02', 'Repartidor Demo 02', '+595981000002')
ON CONFLICT (device_id) DO NOTHING;
