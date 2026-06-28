# Feature Specification: Reserva de Cita en Línea 24/7

**Feature Branch**: `001-reserva-cita-online`

**Created**: 2026-06-27

**Status**: Draft

**Epic**: E-01 | **Story**: US-01 | **Estimación**: 8 pts

**Input**: Como paciente, quiero reservar una cita en línea en cualquier momento del día,
para no tener que llamar durante mi horario de almuerzo ni acumular intentos fallidos.

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Reserva exitosa fuera del horario telefónico (Priority: P1)

El paciente accede al sistema en cualquier momento (tarde de la noche, madrugada, fin de
semana) sin depender del horario de atención de la central telefónica. Selecciona el
médico de su preferencia, elige una fecha y franja horaria disponible, y confirma la
reserva. El sistema registra la cita y notifica al paciente vía WhatsApp.

**Why this priority**: Es el flujo principal que entrega valor inmediato al paciente y
justifica el desarrollo completo de la funcionalidad. Sin este flujo no existe producto.

**Independent Test**: Puede probarse de forma aislada ejecutando el flujo completo de
reserva con un médico y horario disponibles y verificando que la cita queda registrada
en el sistema y que se genera la notificación de confirmación.

**Acceptance Scenarios**:

1. **Dado que** el paciente accede al sistema fuera del horario de atención telefónica
   (por ejemplo, a las 11 PM de un domingo),
   **Cuando** selecciona un médico disponible, elige una fecha futura y una franja
   horaria libre, y confirma la reserva,
   **Entonces** la cita queda registrada en el sistema con estado "Confirmada", el
   paciente recibe un mensaje de confirmación por WhatsApp con los detalles de la cita
   (médico, fecha, hora, lugar de atención), y la franja horaria elegida deja de estar
   disponible para otros pacientes.

2. **Dado que** el paciente tiene sesión activa en el sistema,
   **Cuando** completa el flujo de reserva con datos válidos (médico, fecha, hora),
   **Entonces** el sistema asigna un código único de cita y lo incluye en la
   notificación de WhatsApp.

---

### User Story 2 — Intento de reserva en franja no disponible (Priority: P2)

El paciente intenta seleccionar una franja horaria que ya ha sido reservada por otro
paciente o que el médico ha bloqueado. El sistema debe informar claramente la no
disponibilidad y guiar al paciente hacia una alternativa válida, sin bloquear su
experiencia.

**Why this priority**: Sin este control, múltiples pacientes podrían reservar el mismo
horario, generando conflictos operativos graves en la consulta médica.

**Independent Test**: Puede probarse creando previamente una cita en una franja, y luego
intentando reservar la misma franja con un segundo paciente, verificando que el sistema
rechaza la segunda reserva y muestra un mensaje orientador.

**Acceptance Scenarios**:

1. **Dado que** una franja horaria específica de un médico ya está ocupada,
   **Cuando** el paciente intenta confirmar esa misma franja,
   **Entonces** el sistema muestra un mensaje de "franja no disponible", no registra la
   cita duplicada, y presenta opciones alternativas de horario disponibles para el mismo
   médico o sugiere otro médico de la misma especialidad.

2. **Dado que** el paciente visualiza el calendario de disponibilidad de un médico,
   **Cuando** accede a la vista de selección de horario,
   **Entonces** las franjas ya ocupadas o bloqueadas se muestran visualmente
   diferenciadas (por ejemplo, sombreadas o deshabilitadas) para que el paciente no
   pueda seleccionarlas antes de intentar confirmar.

---

### User Story 3 — Consulta y cancelación de citas propias (Priority: P3)

El paciente puede consultar las citas que tiene registradas y, de ser necesario,
cancelar una cita con antelación suficiente sin necesidad de llamar a la clínica.

**Why this priority**: Complementa el flujo de reserva y reduce la carga operativa del
personal de la clínica; sin embargo, el sistema ya entrega valor principal con P1 y P2.

**Independent Test**: Puede probarse listando las citas activas de un paciente y
cancelando una de ellas, verificando que el estado cambia y que la franja queda
disponible nuevamente.

**Acceptance Scenarios**:

1. **Dado que** el paciente tiene al menos una cita confirmada,
   **Cuando** accede a la sección "Mis citas",
   **Entonces** el sistema muestra el listado de citas activas con médico, fecha, hora
   y estado de cada una.

2. **Dado que** el paciente desea cancelar una cita que está a más de 24 horas de
   distancia,
   **Cuando** solicita la cancelación,
   **Entonces** la cita cambia a estado "Cancelada", la franja horaria queda disponible
   para otros pacientes, y el paciente recibe notificación de la cancelación por
   WhatsApp.

---

### Edge Cases

- ¿Qué ocurre si el paciente pierde la conexión justo después de confirmar pero antes
  de recibir la respuesta del sistema? El sistema debe garantizar idempotencia: si la
  cita fue registrada, no se crea duplicado; si no fue registrada, el paciente puede
  volver a intentarlo.
- ¿Qué ocurre si el servicio de WhatsApp no está disponible al momento de enviar la
  confirmación? La cita debe quedar registrada; la notificación debe reintentarse de
  forma asíncrona sin bloquear la respuesta al paciente.
- ¿Qué sucede si el paciente ya tiene una cita activa con el mismo médico en la misma
  fecha? El sistema debe advertirlo e impedir el doble agendamiento con el mismo médico
  en el mismo día.
- ¿Puede un menor de edad reservar una cita? Se asume que el sistema opera con
  pacientes adultos; la gestión de menores queda fuera del alcance de esta historia.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema DEBE estar disponible para recibir reservas las 24 horas del
  día, los 7 días de la semana, incluyendo días festivos.
- **FR-002**: El sistema DEBE mostrar al paciente la disponibilidad actualizada de
  franjas horarias por médico, diferenciando visualmente las disponibles de las
  ocupadas o bloqueadas.
- **FR-003**: El paciente DEBE poder filtrar médicos por especialidad y/o nombre antes
  de ver la disponibilidad.
- **FR-004**: El sistema DEBE registrar la cita solo si la franja horaria sigue
  disponible en el momento exacto de la confirmación (control de concurrencia).
- **FR-005**: El sistema DEBE enviar una notificación de confirmación al paciente vía
  WhatsApp inmediatamente después de registrar la cita exitosamente. La notificación
  DEBE incluir: nombre del médico, especialidad, fecha, hora y lugar de atención.
- **FR-006**: El sistema DEBE asignar un código único e irrepetible a cada cita
  registrada e incluirlo en la notificación de confirmación.
- **FR-007**: Si una franja ya está ocupada al momento de confirmar, el sistema DEBE
  informar al paciente y DEBE presentar alternativas de horario disponibles para el
  mismo médico.
- **FR-008**: El sistema DEBE impedir que un mismo paciente tenga dos citas activas con
  el mismo médico en la misma fecha.
- **FR-009**: El paciente DEBE poder consultar el listado de sus citas activas.
- **FR-010**: El paciente DEBE poder cancelar una cita con al menos 24 horas de
  antelación. Al cancelar, la franja debe quedar disponible y el paciente debe recibir
  notificación por WhatsApp.
- **FR-011**: Si la notificación de WhatsApp falla, la cita DEBE quedar registrada y
  el sistema DEBE reintentar el envío de la notificación de forma asíncrona.

### Key Entities *(include if feature involves data)*

- **Paciente**: persona que utiliza el sistema para agendar citas; identificado por
  documento de identidad o número de historia clínica; tiene nombre, teléfono celular
  (para WhatsApp) y correo electrónico opcional.
- **Médico**: profesional de la salud que ofrece citas; tiene nombre, especialidad,
  y una agenda de disponibilidad.
- **Disponibilidad (Agenda)**: conjunto de franjas horarias que un médico ha habilitado
  para atención; cada franja tiene fecha, hora de inicio, hora de fin y estado
  (libre / ocupada / bloqueada).
- **Cita**: reserva confirmada que vincula un paciente con un médico en una franja
  horaria específica; tiene código único, estado (Confirmada / Cancelada / Atendida)
  y lugar de atención.
- **Notificación**: mensaje enviado al paciente (vía WhatsApp) con el resultado de una
  acción (confirmación o cancelación de cita); registra el intento, el resultado del
  envío y la marca de tiempo.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de los intentos de reserva sobre franjas disponibles resulta en
  una cita registrada sin errores del sistema.
- **SC-002**: El paciente recibe la notificación de confirmación por WhatsApp en menos
  de 30 segundos después de confirmar la reserva en condiciones normales de operación.
- **SC-003**: Cero citas duplicadas (mismo médico, misma franja) registradas en el
  sistema, incluso bajo intentos concurrentes.
- **SC-004**: El tiempo total para completar una reserva (desde la selección del médico
  hasta recibir la confirmación) no supera los 3 minutos en el flujo estándar.
- **SC-005**: El 95 % de las notificaciones de WhatsApp que fallaron en el primer
  intento son entregadas exitosamente dentro de los 5 minutos siguientes mediante
  reintento automático.
- **SC-006**: La disponibilidad del servicio de reservas en línea es igual o superior
  al 99,5 % medida mensualmente.

---

## Assumptions

- El sistema de autenticación de pacientes ya existe y está disponible como servicio;
  esta historia no incluye el registro ni el login de nuevos pacientes.
- Los médicos gestionan su disponibilidad a través de un módulo administrativo separado
  (fuera del alcance de esta historia); la agenda ya está precargada al momento de que
  el paciente realiza la búsqueda.
- Las notificaciones se envían exclusivamente al número de WhatsApp registrado en el
  perfil del paciente; no se gestionan otros canales de notificación en esta historia.
- El lugar de atención está definido a nivel de médico o agenda y no requiere que el
  paciente lo seleccione.
- Las franjas horarias tienen una duración fija definida por el médico o la
  especialidad; el paciente no elige la duración de la cita.
- El horizonte máximo de reserva anticipada es de 60 días calendario; el horizonte
  mínimo es de 2 horas antes del inicio de la franja.
- La cancelación por parte del paciente está permitida únicamente con un mínimo de
  24 horas de antelación; cancelaciones con menor anticipación requieren contacto
  telefónico (fuera del alcance de esta historia).
- Los menores de edad y la gestión de representantes/tutores quedan fuera del alcance
  de esta historia.
- El sistema opera en zona horaria única (la local del servidor); no se requiere soporte
  multi-zona horaria en esta versión.
