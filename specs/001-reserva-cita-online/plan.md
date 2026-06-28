# Implementation Plan: Reserva de Cita en Línea 24/7

**Branch**: `001-reserva-cita-online` | **Date**: 2026-06-27 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-reserva-cita-online/spec.md`

---

## Summary

El sistema debe permitir a los pacientes reservar citas médicas en línea en cualquier
momento del día (24/7), sin depender del horario de atención telefónica. El enfoque
técnico adopta **Clean Architecture** sobre **Spring Boot 4.1 / Java 25**, con un
contrato **OpenAPI 3.1** generado vía `openapi-generator-gradle-plugin` como fuente de
verdad. La concurrencia se controla con **bloqueo optimista JPA** + restricción UNIQUE
en BD. Las notificaciones WhatsApp se envían de forma **asíncrona con reintento**. La
suite de tests BDD cubre tres niveles: unit, integration y functional.

---

## Technical Context

**Language/Version**: Java 25

**Primary Dependencies**:
- Spring Boot 4.1.0 (ya configurado en `build.gradle`)
- Spring Data JPA + H2
- openapi-generator-gradle-plugin 7.x (a añadir)
- Mockito 5.x (incluido vía Spring Boot Test)
- ArchUnit 1.x (a añadir en `testImplementation`)
- JaCoCo (plugin Gradle estándar, a añadir)
- Spring Retry (`spring-retry` + `aspectjweaver`, para `@Retryable` en notificaciones)

**Storage**: H2 in-memory (desarrollo y tests); esquema gestionado por scripts SQL propios en
`src/main/resources/db/` (`schema.sql` + `data.sql`); Hibernate `ddl-auto=none`; ANSI SQL para portabilidad a PostgreSQL

**Testing**: JUnit 5 (junit-platform-launcher ya en build.gradle) + Mockito + MockMvc + ArchUnit + `@SpringBootTest`

**Target Platform**: Servidor Linux (JVM); API REST consumida por frontend web o móvil

**Project Type**: web-service (REST API)

**Performance Goals**:
- Reserva completada y respondida en < 2 s en condiciones normales (SC-004)
- Notificación WhatsApp despachada en < 30 s (SC-002); asíncrona, no bloquea la respuesta HTTP

**Constraints**:
- Control de concurrencia: bloqueo optimista (`@Version`) + restricción UNIQUE en DB
- Idempotencia en `POST /citas` mediante header `Idempotency-Key`
- Disponibilidad ≥ 99,5 % mensual (SC-006)

**Scale/Scope**: Servicio académico de citas médicas — usuarios concurrentes del orden de decenas

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Principio | Cumplimiento | Observaciones |
|---|-----------|-------------|--------------|
| I | Clean Architecture | ✅ PASS | 4 capas bien delimitadas; domain sin deps; ports/adapters definidos en data-model.md |
| II | BDD Testing | ✅ PASS | 3 tiers (unit/integration/functional); Given-When-Then; tests antes del código |
| III | SOLID/DRY/YAGNI | ✅ PASS | Un use case = un `execute`; interfaces de repositorio estrechas; sin abstracciones especulativas |
| IV | API First (OpenAPI) | ✅ PASS | Contrato `contracts/citamedicos-api.yaml` creado antes de cualquier stub |
| V | JaCoCo ≥ 80% | ✅ PASS | Plugin configurado en plan; generados excluidos; `check` task enforced |

**Post-design re-check**: ✅ Todas las decisiones en research.md son compatibles con los 5 principios.
No hay violaciones justificadas que listar en Complexity Tracking.

---

## Project Structure

### Documentation (this feature)

```text
specs/001-reserva-cita-online/
├── plan.md              # Este archivo
├── spec.md              # Especificación funcional
├── research.md          # Decisiones técnicas
├── data-model.md        # Modelo de dominio y esquema de BD
├── quickstart.md        # Guía de validación E2E
├── contracts/
│   └── citamedicos-api.yaml   # Contrato OpenAPI 3.1
└── checklists/
    └── requirements.md
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/org/ups/citamedicos/
│   │   ├── domain/
│   │   │   ├── model/
│   │   │   │   ├── Paciente.java
│   │   │   │   ├── Medico.java
│   │   │   │   ├── FranjaHoraria.java
│   │   │   │   ├── Cita.java
│   │   │   │   └── Notificacion.java
│   │   │   ├── valueobject/
│   │   │   │   ├── CodigoCita.java
│   │   │   │   ├── NumeroWhatsApp.java
│   │   │   │   ├── EstadoCita.java
│   │   │   │   ├── EstadoFranja.java
│   │   │   │   ├── TipoNotificacion.java
│   │   │   │   └── EstadoEnvio.java
│   │   │   └── exception/
│   │   │       ├── FranjaNoDisponibleException.java
│   │   │       ├── CitaDuplicadaException.java
│   │   │       └── CancelacionFueraDePlazoException.java
│   │   ├── application/
│   │   │   ├── port/
│   │   │   │   ├── in/
│   │   │   │   │   ├── ReservarCitaUseCase.java
│   │   │   │   │   ├── ConsultarDisponibilidadUseCase.java
│   │   │   │   │   ├── CancelarCitaUseCase.java
│   │   │   │   │   ├── ConsultarCitasPacienteUseCase.java
│   │   │   │   │   └── BuscarMedicosUseCase.java
│   │   │   │   └── out/
│   │   │   │       ├── CitaRepositoryPort.java
│   │   │   │       ├── FranjaHorariaRepositoryPort.java
│   │   │   │       ├── MedicoRepositoryPort.java
│   │   │   │       ├── PacienteRepositoryPort.java
│   │   │   │       ├── NotificacionWhatsAppPort.java
│   │   │   │       └── NotificacionRepositoryPort.java
│   │   │   └── usecase/
│   │   │       ├── ReservarCitaUseCaseImpl.java
│   │   │       ├── ConsultarDisponibilidadUseCaseImpl.java
│   │   │       ├── CancelarCitaUseCaseImpl.java
│   │   │       ├── ConsultarCitasPacienteUseCaseImpl.java
│   │   │       └── BuscarMedicosUseCaseImpl.java
│   │   ├── adapter/
│   │   │   ├── in/
│   │   │   │   └── web/
│   │   │   │       ├── CitaController.java          # implements generated CitasApi
│   │   │   │       ├── MedicoController.java        # implements generated MedicosApi (GET /medicos + GET /medicos/{id}/disponibilidad)
│   │   │   │       └── mapper/
│   │   │   │           ├── CitaMapper.java
│   │   │   │           ├── FranjaHorariaMapper.java
│   │   │   │           └── MedicoMapper.java
│   │   │   └── out/
│   │   │       ├── persistence/
│   │   │       │   ├── entity/
│   │   │       │   │   ├── CitaEntity.java
│   │   │       │   │   ├── FranjaHorariaEntity.java
│   │   │       │   │   ├── MedicoEntity.java
│   │   │       │   │   ├── PacienteEntity.java
│   │   │       │   │   └── NotificacionEntity.java
│   │   │       │   ├── repository/
│   │   │       │   │   ├── CitaJpaRepository.java
│   │   │       │   │   ├── FranjaHorariaJpaRepository.java
│   │   │       │   │   ├── MedicoJpaRepository.java
│   │   │       │   │   ├── PacienteJpaRepository.java
│   │   │       │   │   └── NotificacionJpaRepository.java
│   │   │       │   ├── CitaJpaAdapter.java
│   │   │       │   ├── FranjaHorariaJpaAdapter.java
│   │   │       │   ├── MedicoJpaAdapter.java
│   │   │       │   ├── PacienteJpaAdapter.java
│   │   │       │   └── NotificacionJpaAdapter.java
│   │   │       └── notification/
│   │   │           └── WhatsAppCloudApiAdapter.java
│   │   └── infrastructure/
│   │       ├── config/
│   │       │   ├── UseCaseConfig.java        # @Bean wiring de use cases
│   │       │   ├── AsyncConfig.java          # @EnableAsync
│   │       │   └── WhatsAppProperties.java   # @ConfigurationProperties
│   │       └── CitamedicosServiceApplication.java
│   └── resources/
│       ├── openapi/
│       │   └── citamedicos-api.yaml          # Contrato OpenAPI (fuente de verdad)
│       ├── db/
│       │   ├── schema.sql                    # DDL: CREATE TABLE para las 5 tablas
│       │   └── data.sql                      # Seed: médicos, pacientes y franjas precargadas
│       └── application.yaml
└── test/
    └── java/org/ups/citamedicos/
        ├── unit/
        │   ├── domain/
        │   │   ├── CitaTest.java
        │   │   ├── FranjaHorariaTest.java
        │   │   ├── CodigoCitaTest.java
        │   │   └── NumeroWhatsAppTest.java
        │   └── application/
        │       ├── ReservarCitaUseCaseTest.java
        │       ├── ConsultarDisponibilidadUseCaseTest.java
        │       ├── CancelarCitaUseCaseTest.java
        │       └── BuscarMedicosUseCaseTest.java
        ├── integration/
        │   ├── persistence/
        │   │   ├── CitaJpaAdapterTest.java
        │   │   └── FranjaHorariaJpaAdapterTest.java
        │   ├── web/
        │   │   ├── CitaControllerMvcTest.java
        │   │   └── MedicoControllerMvcTest.java
        │   └── architecture/
        │       └── ArchitectureTest.java     # ArchUnit rules
        └── functional/
            ├── ReservarCitaFuncionalTest.java
            ├── FranjaNoDisponibleFuncionalTest.java
            └── CancelarCitaFuncionalTest.java

build/
└── generated/
    └── openapi/                              # Output de openapi-generator (excluido de JaCoCo)
```

**Structure Decision**: Single Spring Boot project (Option 1). El servicio es un
backend REST monolítico; no hay frontend en este alcance. Las 4 capas de Clean
Architecture mapean directamente a paquetes Java dentro del mismo módulo Gradle.

---

## Complexity Tracking

> No hay violaciones a la Constitución que deban justificarse en esta historia.

---

## DB Initialization (src/main/resources/db/)

La base de datos H2 se inicializa con scripts SQL propios en lugar de dejar que
Hibernate genere el DDL. Esto garantiza:

1. **Control total del esquema**: el DDL es legible, versionable y portable.
2. **Datos de prueba consistentes**: los tests funcionales y de integración parten
   de un estado conocido sin necesidad de fixtures en Java.
3. **Portabilidad**: los mismos scripts funcionan en PostgreSQL cambiando solo la
   configuración del datasource.

### `src/main/resources/db/schema.sql`

DDL que crea las 5 tablas definidas en `data-model.md`:

```sql
CREATE TABLE IF NOT EXISTS pacientes (
    id                VARCHAR(36)  PRIMARY KEY,
    documento         VARCHAR(20)  NOT NULL UNIQUE,
    nombre            VARCHAR(120) NOT NULL,
    telefono_whatsapp VARCHAR(20)  NOT NULL,
    email             VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS medicos (
    id             VARCHAR(36)  PRIMARY KEY,
    nombre         VARCHAR(120) NOT NULL,
    especialidad   VARCHAR(100) NOT NULL,
    lugar_atencion VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS franjas_horarias (
    id          VARCHAR(36) PRIMARY KEY,
    medico_id   VARCHAR(36) NOT NULL REFERENCES medicos(id),
    fecha       DATE        NOT NULL,
    hora_inicio TIME        NOT NULL,
    hora_fin    TIME        NOT NULL,
    estado      VARCHAR(10) NOT NULL DEFAULT 'LIBRE',
    version     BIGINT      NOT NULL DEFAULT 0,
    CONSTRAINT uq_franja UNIQUE (medico_id, fecha, hora_inicio)
);

CREATE TABLE IF NOT EXISTS citas (
    id                VARCHAR(36)  PRIMARY KEY,
    codigo            VARCHAR(36)  NOT NULL UNIQUE,
    paciente_id       VARCHAR(36)  NOT NULL REFERENCES pacientes(id),
    medico_id         VARCHAR(36)  NOT NULL REFERENCES medicos(id),
    franja_horaria_id VARCHAR(36)  NOT NULL REFERENCES franjas_horarias(id),
    estado            VARCHAR(15)  NOT NULL DEFAULT 'CONFIRMADA',
    idempotency_key   VARCHAR(36),
    creada_en         TIMESTAMP    NOT NULL,
    CONSTRAINT uq_idempotency UNIQUE (idempotency_key)
);

CREATE TABLE IF NOT EXISTS notificaciones (
    id                VARCHAR(36) PRIMARY KEY,
    cita_id           VARCHAR(36) NOT NULL REFERENCES citas(id),
    tipo              VARCHAR(15) NOT NULL,
    canal             VARCHAR(15) NOT NULL DEFAULT 'WHATSAPP',
    estado_envio      VARCHAR(10) NOT NULL DEFAULT 'PENDIENTE',
    intentos          INT         NOT NULL DEFAULT 0,
    creada_en         TIMESTAMP   NOT NULL,
    ultimo_intento_en TIMESTAMP
);
```

### `src/main/resources/db/data.sql`

Datos precargados que permiten arrancar el servicio con un estado realista y ejecutar
los tests funcionales sin fixtures adicionales:

```sql
-- Médicos (3 especialidades)
INSERT INTO medicos (id, nombre, especialidad, lugar_atencion) VALUES
  ('med-0001-0000-0000-000000000001', 'Dra. Ana García',    'Cardiología',  'Consultorios Norte, Piso 2'),
  ('med-0001-0000-0000-000000000002', 'Dr. Luis Herrera',   'Medicina General', 'Consultorios Sur, Piso 1'),
  ('med-0001-0000-0000-000000000003', 'Dra. Sofía Ramos',   'Pediatría',    'Torre B, Piso 3');

-- Pacientes (2 de prueba)
INSERT INTO pacientes (id, documento, nombre, telefono_whatsapp, email) VALUES
  ('pac-0001-0000-0000-000000000001', '1020304050', 'Carlos Mendoza', '+573001234567', 'carlos@example.com'),
  ('pac-0001-0000-0000-000000000002', '9080706050', 'María López',    '+573009876543', 'maria@example.com');

-- Franjas horarias libres — próximos 7 días (fechas relativas con función H2)
-- Nota: usar fechas fijas o DATEADD en H2 para seeds portables
INSERT INTO franjas_horarias (id, medico_id, fecha, hora_inicio, hora_fin, estado, version) VALUES
  ('fh-0001-0000-0000-000000000001', 'med-0001-0000-0000-000000000001', DATEADD('DAY', 1, CURRENT_DATE), '09:00', '09:30', 'LIBRE', 0),
  ('fh-0001-0000-0000-000000000002', 'med-0001-0000-0000-000000000001', DATEADD('DAY', 1, CURRENT_DATE), '09:30', '10:00', 'LIBRE', 0),
  ('fh-0001-0000-0000-000000000003', 'med-0001-0000-0000-000000000001', DATEADD('DAY', 2, CURRENT_DATE), '10:00', '10:30', 'LIBRE', 0),
  ('fh-0001-0000-0000-000000000004', 'med-0001-0000-0000-000000000002', DATEADD('DAY', 1, CURRENT_DATE), '08:00', '08:20', 'LIBRE', 0),
  ('fh-0001-0000-0000-000000000005', 'med-0001-0000-0000-000000000002', DATEADD('DAY', 1, CURRENT_DATE), '08:20', '08:40', 'LIBRE', 0),
  ('fh-0001-0000-0000-000000000006', 'med-0001-0000-0000-000000000003', DATEADD('DAY', 3, CURRENT_DATE), '14:00', '14:30', 'LIBRE', 0);
```

### Configuración `application.yaml` (sección de inicialización)

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:citamedicos;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    driver-class-name: org.h2.Driver
    username: sa
    password:
  sql:
    init:
      mode: always                                      # ejecutar scripts siempre en H2
      schema-locations: classpath:db/schema.sql
      data-locations: classpath:db/data.sql
  jpa:
    hibernate:
      ddl-auto: none                                    # Hibernate NO toca el esquema
    show-sql: false
    properties:
      hibernate:
        format_sql: true
  h2:
    console:
      enabled: true
      path: /h2-console
```

> **Para tests con `@DataJpaTest`**: Spring Boot reemplaza el datasource por H2 en
> memoria y ejecuta `schema.sql` + `data.sql` automáticamente si están en el classpath.
> No se necesita configuración adicional en los tests.

> **Para producción con PostgreSQL**: cambiar `spring.sql.init.mode=never`,
> configurar datasource PostgreSQL y añadir Flyway/Liquibase con las mismas migraciones.

---

## Build Configuration Changes

Los siguientes cambios en `build.gradle` son necesarios para cumplir los principios IV y V:

```groovy
plugins {
    // ... existing plugins ...
    id 'org.openapi.generator' version '7.6.0'
    id 'jacoco'
}

dependencies {
    // ... existing deps ...
    testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'
    implementation 'org.springframework.retry:spring-retry'
    implementation 'org.springframework:spring-aspects'
}

// Principle IV: API First — generate stubs from OpenAPI contract
openApiGenerate {
    generatorName = "spring"
    inputSpec = "$rootDir/src/main/resources/openapi/citamedicos-api.yaml"
    outputDir = "$buildDir/generated/openapi"
    apiPackage = "org.ups.citamedicos.adapter.in.web.generated"
    modelPackage = "org.ups.citamedicos.adapter.in.web.generated.dto"
    configOptions = [
        interfaceOnly      : "true",
        useSpringBoot3     : "true",
        generateApiTests   : "false",
        generateModelTests : "false"
    ]
}

sourceSets.main.java.srcDirs += "$buildDir/generated/openapi/src/main/java"
compileJava.dependsOn tasks.openApiGenerate

// Principle V: JaCoCo coverage gates
jacoco {
    toolVersion = "0.8.12"
}

jacocoTestReport {
    dependsOn test
    reports {
        xml.required = true
        html.required = true
    }
    afterEvaluate {
        classDirectories.setFrom(files(classDirectories.files.collect {
            fileTree(dir: it, exclude: [
                '**/CitamedicosServiceApplication*',
                '**/generated/**',
                '**/infrastructure/config/**'
            ])
        }))
    }
}

jacocoTestCoverageVerification {
    dependsOn jacocoTestReport
    violationRules {
        rule {
            limit {
                counter = 'INSTRUCTION'
                value   = 'COVEREDRATIO'
                minimum = 0.80
            }
        }
        rule {
            element = 'CLASS'
            excludes = [
                'org.ups.citamedicos.CitamedicosServiceApplication',
                'org.ups.citamedicos.adapter.in.web.generated.*',
                'org.ups.citamedicos.infrastructure.config.*'
            ]
            limit {
                counter = 'INSTRUCTION'
                value   = 'COVEREDRATIO'
                minimum = 0.80
            }
        }
    }
}

check.dependsOn jacocoTestCoverageVerification
```
