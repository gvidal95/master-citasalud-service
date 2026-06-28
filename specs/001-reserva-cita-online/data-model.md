# Data Model: Reserva de Cita en Línea 24/7

**Feature**: US-01 | **Date**: 2026-06-27

---

## Entities (Domain Layer)

Las siguientes son entidades del **dominio puro** — sin anotaciones JPA ni Spring.

### Paciente

| Campo | Tipo | Restricciones |
|-------|------|--------------|
| `id` | UUID | Obligatorio, inmutable |
| `documento` | String | Obligatorio, único en el sistema |
| `nombre` | String | Obligatorio, 2–120 caracteres |
| `telefonoWhatsApp` | NumeroWhatsApp (VO) | Obligatorio; formato E.164 |
| `email` | String | Opcional; formato email válido |

**Invariantes de dominio**:
- `documento` no puede ser nulo ni vacío.
- `telefonoWhatsApp` DEBE tener formato E.164 (e.g., `+573001234567`); validado en el
  value object `NumeroWhatsApp`.

---

### Medico

| Campo | Tipo | Restricciones |
|-------|------|--------------|
| `id` | UUID | Obligatorio, inmutable |
| `nombre` | String | Obligatorio, 2–120 caracteres |
| `especialidad` | String | Obligatorio |
| `lugarAtencion` | String | Obligatorio |

**Invariantes de dominio**:
- `especialidad` y `lugarAtencion` no pueden ser vacíos.

---

### FranjaHoraria

| Campo | Tipo | Restricciones |
|-------|------|--------------|
| `id` | UUID | Obligatorio, inmutable |
| `medicoId` | UUID | Obligatorio, referencia a Médico |
| `fecha` | LocalDate | Obligatorio; no puede ser pasada |
| `horaInicio` | LocalTime | Obligatorio |
| `horaFin` | LocalTime | Obligatorio; `horaFin > horaInicio` |
| `estado` | EstadoFranja | Obligatorio; enum: LIBRE, OCUPADA, BLOQUEADA |
| `version` | Long | Control de concurrencia optimista |

**Invariantes de dominio**:
- `horaFin` DEBE ser posterior a `horaInicio`.
- Una franja con estado `OCUPADA` o `BLOQUEADA` NO puede transicionar a reserva.
- Solo puede cambiar a `OCUPADA` desde `LIBRE`.
- Solo puede volver a `LIBRE` desde `OCUPADA` (al cancelar la cita asociada).

**State machine**:
```
LIBRE ──reservar──► OCUPADA ──cancelar──► LIBRE
LIBRE ──bloquear──► BLOQUEADA
```

---

### Cita

| Campo | Tipo | Restricciones |
|-------|------|--------------|
| `id` | UUID | Obligatorio, inmutable (identidad técnica) |
| `codigo` | CodigoCita (VO) | Obligatorio, único globalmente; UUID v4 |
| `pacienteId` | UUID | Obligatorio |
| `medicoId` | UUID | Obligatorio |
| `franjaHorariaId` | UUID | Obligatorio |
| `estado` | EstadoCita | Obligatorio; enum: CONFIRMADA, CANCELADA, ATENDIDA |
| `idempotencyKey` | String | Opcional; UUID del cliente para idempotencia |
| `creadaEn` | Instant | Obligatorio, inmutable; generado al crear |

**Invariantes de dominio**:
- Solo se puede cancelar una `Cita` en estado `CONFIRMADA`.
- La cancelación requiere que la franja asociada esté a más de 24 horas del `Instant` actual.
- Un paciente no puede tener dos citas `CONFIRMADA` con el mismo médico en la misma fecha.

**State machine**:
```
CONFIRMADA ──cancelar (>24h antes)──► CANCELADA
CONFIRMADA ──atender──► ATENDIDA
```

---

### Notificacion

| Campo | Tipo | Restricciones |
|-------|------|--------------|
| `id` | UUID | Obligatorio, inmutable |
| `citaId` | UUID | Obligatorio |
| `tipo` | TipoNotificacion | Obligatorio; enum: CONFIRMACION, CANCELACION |
| `canal` | CanalNotificacion | Obligatorio; enum: WHATSAPP |
| `estadoEnvio` | EstadoEnvio | Obligatorio; enum: PENDIENTE, ENVIADA, FALLIDA |
| `intentos` | int | Obligatorio; 0 en creación; máximo 3 intentos |
| `creadaEn` | Instant | Obligatorio, inmutable |
| `ultimoIntentoEn` | Instant | Nullable; actualizado en cada intento |

**Invariantes de dominio**:
- `intentos` no puede superar 3. Si se alcanza el límite, el estado pasa a `FALLIDA`.
- Solo se puede reintentar si el estado es `PENDIENTE` o `FALLIDA` con menos de 3 intentos.

---

## Value Objects (Domain Layer)

### CodigoCita
- Envuelve un `String` con formato UUID v4.
- Inmutable. Generado por el dominio en la creación de `Cita`.
- Validación: matches `^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$`.

### NumeroWhatsApp
- Envuelve un `String` en formato E.164.
- Validación: matches `^\+[1-9]\d{7,14}$`.

### EstadoCita
```java
enum EstadoCita { CONFIRMADA, CANCELADA, ATENDIDA }
```

### EstadoFranja
```java
enum EstadoFranja { LIBRE, OCUPADA, BLOQUEADA }
```

### TipoNotificacion
```java
enum TipoNotificacion { CONFIRMACION, CANCELACION }
```

### EstadoEnvio
```java
enum EstadoEnvio { PENDIENTE, ENVIADA, FALLIDA }
```

---

## Persistence Schema (Adapter / Infrastructure Layer)

Las siguientes tablas son propiedad de la capa de persistencia JPA. Los `Entity` classes
residen en `adapter/out/persistence/entity/`. **No son parte del dominio.**

### tabla: `pacientes`
```sql
CREATE TABLE pacientes (
    id              VARCHAR(36)  PRIMARY KEY,
    documento       VARCHAR(20)  NOT NULL UNIQUE,
    nombre          VARCHAR(120) NOT NULL,
    telefono_whatsapp VARCHAR(20) NOT NULL,
    email           VARCHAR(255)
);
```

### tabla: `medicos`
```sql
CREATE TABLE medicos (
    id              VARCHAR(36)  PRIMARY KEY,
    nombre          VARCHAR(120) NOT NULL,
    especialidad    VARCHAR(100) NOT NULL,
    lugar_atencion  VARCHAR(255) NOT NULL
);
```

### tabla: `franjas_horarias`
```sql
CREATE TABLE franjas_horarias (
    id          VARCHAR(36) PRIMARY KEY,
    medico_id   VARCHAR(36) NOT NULL REFERENCES medicos(id),
    fecha       DATE        NOT NULL,
    hora_inicio TIME        NOT NULL,
    hora_fin    TIME        NOT NULL,
    estado      VARCHAR(10) NOT NULL DEFAULT 'LIBRE',
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_franja UNIQUE (medico_id, fecha, hora_inicio)
);
```

### tabla: `citas`
```sql
CREATE TABLE citas (
    id                VARCHAR(36)  PRIMARY KEY,
    codigo            VARCHAR(36)  NOT NULL UNIQUE,
    paciente_id       VARCHAR(36)  NOT NULL REFERENCES pacientes(id),
    medico_id         VARCHAR(36)  NOT NULL REFERENCES medicos(id),
    franja_horaria_id VARCHAR(36)  NOT NULL REFERENCES franjas_horarias(id),
    estado            VARCHAR(15)  NOT NULL DEFAULT 'CONFIRMADA',
    idempotency_key   VARCHAR(36),
    creada_en         TIMESTAMP    NOT NULL,
    CONSTRAINT uq_idempotency UNIQUE (idempotency_key),
    CONSTRAINT uq_paciente_medico_fecha UNIQUE (paciente_id, medico_id, franja_horaria_id)
);
```

### tabla: `notificaciones`
```sql
CREATE TABLE notificaciones (
    id               VARCHAR(36) PRIMARY KEY,
    cita_id          VARCHAR(36) NOT NULL REFERENCES citas(id),
    tipo             VARCHAR(15) NOT NULL,
    canal            VARCHAR(15) NOT NULL DEFAULT 'WHATSAPP',
    estado_envio     VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE',
    intentos         INT         NOT NULL DEFAULT 0,
    creada_en        TIMESTAMP   NOT NULL,
    ultimo_intento_en TIMESTAMP
);
```

---

## Relationships

```
Medico  1 ──────────── * FranjaHoraria
Paciente 1 ──────────── * Cita
Medico   1 ──────────── * Cita
FranjaHoraria 1 ──────── 1 Cita  (una franja activa por cita)
Cita     1 ──────────── * Notificacion
```

---

## Ports (Application Layer — Interfaces)

### Input Ports (`application/port/in/`)

```java
interface ReservarCitaUseCase {
    Cita execute(ReservarCitaCommand command);
}

interface ConsultarDisponibilidadUseCase {
    List<FranjaHoraria> execute(ConsultarDisponibilidadQuery query);
}

interface CancelarCitaUseCase {
    void execute(CancelarCitaCommand command);
}

interface ConsultarCitasPacienteUseCase {
    List<Cita> execute(ConsultarCitasPacienteQuery query);
}

interface BuscarMedicosUseCase {
    List<Medico> execute(BuscarMedicosQuery query);
}
```

### Output Ports (`application/port/out/`)

```java
interface CitaRepositoryPort {
    Cita save(Cita cita);
    Optional<Cita> findByCodigo(CodigoCita codigo);
    Optional<Cita> findByIdempotencyKey(String key);
    List<Cita> findByPacienteId(UUID pacienteId);
    boolean existsConfirmadaByPacienteAndMedicoAndFecha(UUID pacienteId, UUID medicoId, LocalDate fecha);
}

interface FranjaHorariaRepositoryPort {
    Optional<FranjaHoraria> findById(UUID id);
    List<FranjaHoraria> findDisponiblesByMedicoAndRangoFecha(UUID medicoId, LocalDate desde, LocalDate hasta);
    FranjaHoraria save(FranjaHoraria franja);
}

interface MedicoRepositoryPort {
    List<Medico> findByEspecialidadAndNombre(String especialidad, String nombre);
    Optional<Medico> findById(UUID id);
}

interface PacienteRepositoryPort {
    Optional<Paciente> findById(UUID id);
}

interface NotificacionWhatsAppPort {
    void enviar(Notificacion notificacion, Paciente paciente, Cita cita, Medico medico);
}

interface NotificacionRepositoryPort {
    Notificacion save(Notificacion notificacion);
    List<Notificacion> findPendientesConIntentosInsuficientes(int maxIntentos);
}
```
