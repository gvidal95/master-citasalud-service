-- Idempotent seed: clear + re-insert so multiple test contexts don't conflict
SET REFERENTIAL_INTEGRITY FALSE;
DELETE FROM notificaciones WHERE TRUE;
DELETE FROM citas WHERE TRUE;
DELETE FROM franjas_horarias WHERE TRUE;
DELETE FROM pacientes WHERE TRUE;
DELETE FROM medicos WHERE TRUE;
SET REFERENTIAL_INTEGRITY TRUE;

-- Médicos
INSERT INTO medicos (id, nombre, especialidad, lugar_atencion) VALUES
  ('00000000-0000-4000-a000-000000000001', 'Dra. Ana García',  'Cardiología',      'Consultorios Norte, Piso 2'),
  ('00000000-0000-4000-a000-000000000002', 'Dr. Luis Herrera', 'Medicina General', 'Consultorios Sur, Piso 1'),
  ('00000000-0000-4000-a000-000000000003', 'Dra. Sofía Ramos', 'Pediatría',        'Torre B, Piso 3');

-- Pacientes
INSERT INTO pacientes (id, documento, nombre, telefono_whatsapp, email) VALUES
  ('00000000-0000-4000-b000-000000000001', '1020304050', 'Carlos Mendoza', '+573001234567', 'carlos@example.com'),
  ('00000000-0000-4000-b000-000000000002', '9080706050', 'María López',    '+573009876543', 'maria@example.com');

-- Franjas horarias (>24h en el futuro para que todas sean cancelables en tests)
INSERT INTO franjas_horarias (id, medico_id, fecha, hora_inicio, hora_fin, estado, version) VALUES
  ('00000000-0000-4000-c000-000000000001', '00000000-0000-4000-a000-000000000001', DATEADD('DAY', 2, CURRENT_DATE), '09:00', '09:30', 'LIBRE', 0),
  ('00000000-0000-4000-c000-000000000002', '00000000-0000-4000-a000-000000000001', DATEADD('DAY', 2, CURRENT_DATE), '09:30', '10:00', 'LIBRE', 0),
  ('00000000-0000-4000-c000-000000000003', '00000000-0000-4000-a000-000000000001', DATEADD('DAY', 3, CURRENT_DATE), '10:00', '10:30', 'LIBRE', 0),
  ('00000000-0000-4000-c000-000000000004', '00000000-0000-4000-a000-000000000002', DATEADD('DAY', 2, CURRENT_DATE), '08:00', '08:20', 'LIBRE', 0),
  ('00000000-0000-4000-c000-000000000005', '00000000-0000-4000-a000-000000000002', DATEADD('DAY', 2, CURRENT_DATE), '08:20', '08:40', 'LIBRE', 0),
  ('00000000-0000-4000-c000-000000000006', '00000000-0000-4000-a000-000000000003', DATEADD('DAY', 4, CURRENT_DATE), '14:00', '14:30', 'LIBRE', 0);
