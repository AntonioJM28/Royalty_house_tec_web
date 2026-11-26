-- Cuentas de demostración: contraseñas de prueba
-- admin@royaltyhouse.local / Admin123!
-- recepcion@royaltyhouse.local / Recepcion123!
-- cliente@royaltyhouse.local / Cliente123!
-- password_hash usa el formato sal_hex:sha256_hex(sal_hex + ":" + contraseña).
INSERT INTO usuario (id, nombre, correo, telefono, password_hash, rol, activo, acepto_datos)
VALUES
    (1, 'Administrador de prueba', 'admin@royaltyhouse.local', '555-0101',
     '00112233445566778899aabbccddeeff:5249da1a52503ef297a04b9efc36169dbb50d0a56ea2335f3ae3f041bc2cd362',
     'ADMINISTRADOR', TRUE, TRUE),
    (2, 'Recepcionista de prueba', 'recepcion@royaltyhouse.local', '555-0102',
     '102132435465768798a9bacbdcedfe0f:9e76e5a1fbd12312a7739e84aac2599fa8fc8aed2b96404772e6ece77f1b263c',
     'RECEPCIONISTA', TRUE, TRUE),
    (3, 'Cliente de prueba', 'cliente@royaltyhouse.local', '555-0103',
     'ffeeddccbbaa99887766554433221100:2221aa2e3ddf749e538cd6b28af41b45ed850913de7a041dc01056b9df158775',
     'CLIENTE', TRUE, TRUE)
ON CONFLICT (correo) DO NOTHING;

INSERT INTO recurso (id, nombre, tipo, capacidad, activo)
VALUES
    (1, 'Mesa normal 1', 'MESA_NORMAL', 4, TRUE),
    (2, 'Mesa normal 2', 'MESA_NORMAL', 4, TRUE),
    (3, 'Mesa normal 3', 'MESA_NORMAL', 4, TRUE),
    (4, 'Mesa VIP 1', 'MESA_VIP', 6, TRUE),
    (5, 'Mesa VIP 2', 'MESA_VIP', 6, TRUE),
    (6, 'Sala minicine 1', 'SALA_MINICINE', 2, TRUE),
    (7, 'Sala minicine 2', 'SALA_MINICINE', 2, TRUE)
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO tarifa (id, tipo_recurso, precio_hora)
VALUES
    (1, 'MESA_NORMAL', 10.00),
    (2, 'MESA_VIP', 18.00),
    (3, 'SALA_MINICINE', 25.00)
ON CONFLICT (tipo_recurso) DO NOTHING;

INSERT INTO horario_atencion (id, hora_apertura, hora_cierre)
VALUES (1, TIME '10:00', TIME '23:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO pelicula (id, titulo, duracion_min, en_cartelera)
VALUES
    (1, 'La aventura comienza', 95, TRUE),
    (2, 'Misterio en la ciudad', 110, TRUE),
    (3, 'Viaje estelar', 125, TRUE),
    (4, 'Risas de medianoche', 90, TRUE),
    (5, 'El ultimo desafio', 150, TRUE)
ON CONFLICT (titulo) DO NOTHING;

INSERT INTO producto (id, nombre, precio, existencias, activo)
VALUES
    (1, 'Palomitas pequenas', 4.50, 40, TRUE),
    (2, 'Palomitas grandes', 7.00, 30, TRUE),
    (3, 'Nachos con queso', 6.50, 25, TRUE),
    (4, 'Caramelos surtidos', 2.50, 50, TRUE),
    (5, 'Chocolate', 3.50, 35, TRUE),
    (6, 'Gaseosa pequena', 2.00, 60, TRUE),
    (7, 'Gaseosa grande', 3.50, 40, TRUE),
    (8, 'Agua embotellada', 1.50, 45, TRUE)
ON CONFLICT (nombre) DO NOTHING;

-- Avanza las secuencias tras insertar identificadores de demostración explícitos.
SELECT setval(pg_get_serial_sequence('usuario', 'id'),
              GREATEST(COALESCE((SELECT MAX(id) FROM usuario), 1), 1), TRUE);
SELECT setval(pg_get_serial_sequence('recurso', 'id'),
              GREATEST(COALESCE((SELECT MAX(id) FROM recurso), 1), 1), TRUE);
SELECT setval(pg_get_serial_sequence('tarifa', 'id'),
              GREATEST(COALESCE((SELECT MAX(id) FROM tarifa), 1), 1), TRUE);
SELECT setval(pg_get_serial_sequence('horario_atencion', 'id'),
              GREATEST(COALESCE((SELECT MAX(id) FROM horario_atencion), 1), 1), TRUE);
SELECT setval(pg_get_serial_sequence('pelicula', 'id'),
              GREATEST(COALESCE((SELECT MAX(id) FROM pelicula), 1), 1), TRUE);
SELECT setval(pg_get_serial_sequence('producto', 'id'),
              GREATEST(COALESCE((SELECT MAX(id) FROM producto), 1), 1), TRUE);
