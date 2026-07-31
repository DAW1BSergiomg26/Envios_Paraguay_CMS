-- Seed mínimo de envíos para pruebas de carga k6 (Bloque 9)
INSERT IGNORE INTO envios_tracking
  (codigo_unico, estado, destinatario, origen, destino, peso, contenido, fecha_creacion, ultima_actualizacion)
VALUES
  ('MT-2026-0001', 'EN_TRANSITO',      'Juan Pérez',      'Asturias',   'Asunción',        '12 kg', 'Documentos', NOW(), NOW()),
  ('MT-2026-0002', 'EN_ADUANA_DESTINO','María López',     'Madrid',     'Ciudad del Este', '5 kg',  'Ropa',       NOW(), NOW()),
  ('MT-2026-0003', 'ENTREGADO',        'Carlos Gómez',    'Barcelona',  'Asunción',        '20 kg', 'Mercancía',  NOW(), NOW()),
  ('MT-2026-0004', 'EN_REPARTO',       'Ana Martínez',    'Gijón',      'Encarnación',     '3 kg',  'Regalos',    NOW(), NOW()),
  ('MT-2026-0005', 'RECIBIDO',         'Pedro Fernández', 'Oviedo',     'Asunción',        '8 kg',  'Equipos',    NOW(), NOW());
