CREATE TABLE IF NOT EXISTS pacientes (
    id                VARCHAR(36)  PRIMARY KEY,
    documento         VARCHAR(20)  NOT NULL,
    nombre            VARCHAR(120) NOT NULL,
    telefono_whatsapp VARCHAR(20)  NOT NULL,
    email             VARCHAR(255),
    CONSTRAINT uq_paciente_documento UNIQUE (documento)
);

CREATE TABLE IF NOT EXISTS medicos (
    id             VARCHAR(36)  PRIMARY KEY,
    nombre         VARCHAR(120) NOT NULL,
    especialidad   VARCHAR(100) NOT NULL,
    lugar_atencion VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS franjas_horarias (
    id          VARCHAR(36) PRIMARY KEY,
    medico_id   VARCHAR(36) NOT NULL,
    fecha       DATE        NOT NULL,
    hora_inicio TIME        NOT NULL,
    hora_fin    TIME        NOT NULL,
    estado      VARCHAR(10) NOT NULL DEFAULT 'LIBRE',
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT fk_franja_medico FOREIGN KEY (medico_id) REFERENCES medicos(id),
    CONSTRAINT uq_franja UNIQUE (medico_id, fecha, hora_inicio),
    CONSTRAINT chk_hora CHECK (hora_fin > hora_inicio)
);

CREATE TABLE IF NOT EXISTS citas (
    id                       VARCHAR(36)  PRIMARY KEY,
    codigo                   VARCHAR(36)  NOT NULL,
    paciente_id              VARCHAR(36)  NOT NULL,
    medico_id                VARCHAR(36)  NOT NULL,
    franja_horaria_id        VARCHAR(36)  NOT NULL,
    estado                   VARCHAR(15)  NOT NULL DEFAULT 'CONFIRMADA',
    idempotency_key          VARCHAR(36),
    creada_en                TIMESTAMP    NOT NULL,
    franja_fecha_hora_inicio TIMESTAMP    NOT NULL,
    CONSTRAINT uq_cita_codigo UNIQUE (codigo),
    CONSTRAINT uq_cita_idempotency UNIQUE (idempotency_key),
    CONSTRAINT fk_cita_paciente FOREIGN KEY (paciente_id) REFERENCES pacientes(id),
    CONSTRAINT fk_cita_medico   FOREIGN KEY (medico_id)   REFERENCES medicos(id),
    CONSTRAINT fk_cita_franja   FOREIGN KEY (franja_horaria_id) REFERENCES franjas_horarias(id)
);

CREATE TABLE IF NOT EXISTS notificaciones (
    id                VARCHAR(36) PRIMARY KEY,
    cita_id           VARCHAR(36) NOT NULL,
    tipo              VARCHAR(15) NOT NULL,
    canal             VARCHAR(15) NOT NULL DEFAULT 'WHATSAPP',
    estado_envio      VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE',
    intentos          INT         NOT NULL DEFAULT 0,
    creada_en         TIMESTAMP   NOT NULL,
    ultimo_intento_en TIMESTAMP,
    CONSTRAINT fk_notif_cita FOREIGN KEY (cita_id) REFERENCES citas(id)
);
