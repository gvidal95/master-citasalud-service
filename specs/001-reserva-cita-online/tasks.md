---
description: "Task list for US-01 · Reserva de Cita en Línea 24/7"
---

# Tasks: Reserva de Cita en Línea 24/7

**Input**: Design documents from `specs/001-reserva-cita-online/`

**Prerequisites**: plan.md ✅ | spec.md ✅ | data-model.md ✅ | contracts/ ✅ | research.md ✅

**Constitution**: Clean Architecture · BDD (3 tiers) · SOLID/DRY/YAGNI · API First · JaCoCo ≥ 80%

**Format**: `- [X] [ID] [P?] [Story?] Description — path/to/file`
- `[P]` = parallelizable (distinct files, no incomplete deps)
- `[US#]` = user story label (Phase 3+)

---

## Phase 1: Setup — Build Configuration

**Purpose**: Prepare Gradle build for OpenAPI generation, JaCoCo coverage, ArchUnit, and Spring Retry.
All tasks in this phase are blocking prerequisites for all subsequent phases.

- [X] T001 Copy OpenAPI contract to canonical source location — copy `specs/001-reserva-cita-online/contracts/citamedicos-api.yaml` to `src/main/resources/openapi/citamedicos-api.yaml`
- [X] T002 Add `openapi-generator-gradle-plugin 7.6.0` to `build.gradle`: `generatorName=spring`, `inputSpec=$rootDir/src/main/resources/openapi/citamedicos-api.yaml`, `outputDir=$buildDir/generated/openapi`, packages `org.ups.citamedicos.adapter.in.web.generated` / `.dto`, `interfaceOnly=true`, `useSpringBoot3=true`; wire `compileJava.dependsOn openApiGenerate` and add generated src to `sourceSets.main.java.srcDirs` — `build.gradle`
- [X] T003 [P] Add `jacoco` plugin to `build.gradle`; configure `jacocoTestReport` (HTML + XML output) and `jacocoTestCoverageVerification` (global ≥ 80% instruction coverage, per-class ≥ 80% excluding `CitamedicosServiceApplication`, `infrastructure.config.*`, `adapter.in.web.generated.*`); wire `check.dependsOn jacocoTestCoverageVerification` — `build.gradle`
- [X] T004 [P] Add dependencies to `build.gradle`: `archunit-junit5:1.3.0` in `testImplementation`; `spring-retry` and `spring-aspects` in `implementation` — `build.gradle`
- [X] T005 Run `./gradlew openApiGenerate` to verify stub generation succeeds and interfaces appear in `build/generated/openapi/src/main/java/`; fix any YAML validation errors

---

## Phase 2: Foundational — Domain Core, DB Init & Shared Infrastructure

**Purpose**: DB schema, seed data, domain value objects, exceptions, entities, JPA config and ArchUnit rule that ALL user stories depend on.

⚠️ **CRITICAL**: No user story implementation can begin until this phase is complete.

### DB Initialization Scripts

- [X] T006 Create `src/main/resources/db/schema.sql` with DDL for all 5 tables using ANSI SQL + `CREATE TABLE IF NOT EXISTS`: `pacientes`, `medicos`, `franjas_horarias` (with `@Version` column `version BIGINT DEFAULT 0` and `UNIQUE(medico_id, fecha, hora_inicio)`), `citas` (with `UNIQUE(codigo)`, `UNIQUE(idempotency_key)`), `notificaciones`; all FK constraints and CHECK constraints per data-model.md — `src/main/resources/db/schema.sql`
- [X] T007 [P] Create `src/main/resources/db/data.sql` with seed INSERT statements: 3 médicos (`Cardiología`, `Medicina General`, `Pediatría`), 2 pacientes with E.164 WhatsApp numbers, 6 franjas horarias LIBRE using `DATEADD('DAY', N, CURRENT_DATE)` for relative dates (so tests never use stale fixed dates) — `src/main/resources/db/data.sql`
- [X] T008 [P] Configure `src/main/resources/application.yaml`: datasource H2 in-memory (`jdbc:h2:mem:citamedicos;DB_CLOSE_DELAY=-1`), `spring.sql.init.mode=always`, `schema-locations: classpath:db/schema.sql`, `data-locations: classpath:db/data.sql`, `jpa.hibernate.ddl-auto=none`, `h2.console.enabled=true`, `server.port=8080` — `src/main/resources/application.yaml`

### Value Objects & Enums (parallelizable — no deps)

- [X] T009 [P] Create `CodigoCita` value object — `src/main/java/org/ups/citamedicos/domain/valueobject/CodigoCita.java` (wraps UUID v4 String; static factory `generate()` using UUID.randomUUID(); validates UUID pattern on construction; immutable)
- [X] T010 [P] Create `NumeroWhatsApp` value object — `src/main/java/org/ups/citamedicos/domain/valueobject/NumeroWhatsApp.java` (validates E.164 format `^\+[1-9]\d{7,14}$` on construction; immutable)
- [X] T011 [P] Create `EstadoCita` enum — `src/main/java/org/ups/citamedicos/domain/valueobject/EstadoCita.java` (values: `CONFIRMADA`, `CANCELADA`, `ATENDIDA`)
- [X] T012 [P] Create `EstadoFranja` enum — `src/main/java/org/ups/citamedicos/domain/valueobject/EstadoFranja.java` (values: `LIBRE`, `OCUPADA`, `BLOQUEADA`)
- [X] T013 [P] Create `TipoNotificacion` enum — `src/main/java/org/ups/citamedicos/domain/valueobject/TipoNotificacion.java` (values: `CONFIRMACION`, `CANCELACION`)
- [X] T014 [P] Create `EstadoEnvio` enum — `src/main/java/org/ups/citamedicos/domain/valueobject/EstadoEnvio.java` (values: `PENDIENTE`, `ENVIADA`, `FALLIDA`)
- [X] T014b [P] Create `CanalNotificacion` enum — `src/main/java/org/ups/citamedicos/domain/valueobject/CanalNotificacion.java` (values: `WHATSAPP`; required by `Notificacion` entity T022; parallel to T014)

### Domain Exceptions (parallelizable — no deps)

- [X] T015 [P] Create `FranjaNoDisponibleException` (unchecked) — `src/main/java/org/ups/citamedicos/domain/exception/FranjaNoDisponibleException.java` (constructor receives `UUID franjaId` and `UUID medicoId`; message: "Franja horaria {franjaId} del médico {medicoId} no disponible"; both fields exposed via getters so `GlobalExceptionHandler` can query alternative slots for the same médico — FR-007)
- [X] T016 [P] Create `CitaDuplicadaException` (unchecked) — `src/main/java/org/ups/citamedicos/domain/exception/CitaDuplicadaException.java`
- [X] T017 [P] Create `CancelacionFueraDePlazoException` (unchecked) — `src/main/java/org/ups/citamedicos/domain/exception/CancelacionFueraDePlazoException.java` (constructor receives `CodigoCita codigo`)

### Domain Entities (depend on T009–T017)

- [X] T018 [P] Create `Paciente` domain entity — `src/main/java/org/ups/citamedicos/domain/model/Paciente.java` (fields: `id UUID`, `documento String`, `nombre String`, `telefonoWhatsApp NumeroWhatsApp`, `email String`; zero Spring/JPA imports; no Lombok)
- [X] T019 [P] Create `Medico` domain entity — `src/main/java/org/ups/citamedicos/domain/model/Medico.java` (fields: `id UUID`, `nombre`, `especialidad`, `lugarAtencion`; zero Spring/JPA imports; no Lombok)
- [X] T020 Create `FranjaHoraria` domain entity — `src/main/java/org/ups/citamedicos/domain/model/FranjaHoraria.java` (fields per data-model.md; method `reservar()` transitions `LIBRE→OCUPADA` or throws `FranjaNoDisponibleException`; method `liberar()` transitions `OCUPADA→LIBRE`; zero Spring/JPA imports; no Lombok; depends on T012, T015)
- [X] T021 Create `Cita` domain entity — `src/main/java/org/ups/citamedicos/domain/model/Cita.java` (fields per data-model.md; static factory `nueva(pacienteId, medicoId, franjaId, idempotencyKey)` calls `CodigoCita.generate()`; method `cancelar(Instant ahora)` validates 24h rule against `franja.fechaHoraInicio`, throws `CancelacionFueraDePlazoException`; zero Spring/JPA imports; no Lombok; depends on T009, T011, T017)
- [X] T022 [P] Create `Notificacion` domain entity — `src/main/java/org/ups/citamedicos/domain/model/Notificacion.java` (fields per data-model.md including `canal CanalNotificacion`; method `registrarIntento(boolean exito)` increments `intentos` and sets `estadoEnvio`; zero Spring/JPA imports; no Lombok; depends on T013, T014, T014b)

### Shared Infrastructure Setup

- [X] T023 [P] Create `AsyncConfig` — `src/main/java/org/ups/citamedicos/infrastructure/config/AsyncConfig.java` (@Configuration @EnableAsync; `ThreadPoolTaskExecutor` with corePoolSize=2, maxPoolSize=10, queueCapacity=50, threadNamePrefix="async-")
- [X] T024 [P] Create `GlobalExceptionHandler` skeleton — `src/main/java/org/ups/citamedicos/adapter/in/web/GlobalExceptionHandler.java` (@RestControllerAdvice; placeholder handler for generic `Exception` → 500 `ErrorResponse`; will be expanded per story phase)

### Architecture Validation

- [X] T025 [P] Create `ArchitectureTest` — `src/test/java/org/ups/citamedicos/integration/architecture/ArchitectureTest.java` (ArchUnit JUnit5 @AnalyzeClasses; rules: `domain` has no deps outside itself; `application` depends only on `domain`; `adapter` depends on `application` + `domain`; no `org.springframework.*` or `jakarta.persistence.*` imports in `domain` or `application` packages)

**Checkpoint**: Schema + seed data loadable (`./gradlew bootRun` starts, H2 console shows data). Domain compiles. ArchUnit test runs (may pass or fail on empty adapter — expected). User story phases can begin.

---

## Phase 3: User Story 1 — Reserva Exitosa (Priority: P1) 🎯 MVP

**Goal**: El paciente reserva una cita 24/7; la franja queda OCUPADA; recibe confirmación por WhatsApp.

**Independent Test**: `POST http://localhost:8080/api/v1/citas` con `pacienteId` y `franjaHorariaId` del seed → HTTP 201 + `"estado": "CONFIRMADA"` + franja OCUPADA en H2 + log de notificación WhatsApp.

### Tests BDD — US1 (write first; MUST FAIL before any impl task)

- [X] T026 [P] [US1] Create `CodigoCitaTest` (unit) — `src/test/java/org/ups/citamedicos/unit/domain/CodigoCitaTest.java` (`given_generate_when_called_then_validUUIDv4`; `given_invalidString_when_createFromValue_then_throwsIllegalArgument`)
- [X] T027 [P] [US1] Create `NumeroWhatsAppTest` (unit) — `src/test/java/org/ups/citamedicos/unit/domain/NumeroWhatsAppTest.java` (`given_e164Format_when_create_then_accepted`; `given_noPlus_when_create_then_throwsIllegalArgument`; `given_tooShort_when_create_then_throwsIllegalArgument`)
- [X] T028 [P] [US1] Create `FranjaHorariaTest` (unit) — `src/test/java/org/ups/citamedicos/unit/domain/FranjaHorariaTest.java` (`given_franjaLibre_when_reservar_then_estadoOcupada`; `given_franjaOcupada_when_reservar_then_throwsFranjaNoDisponibleException`; `given_franjaOcupada_when_liberar_then_estadoLibre`)
- [X] T029 [P] [US1] Create `CitaTest` (unit) — `src/test/java/org/ups/citamedicos/unit/domain/CitaTest.java` (`given_validData_when_nueva_then_estadoConfirmada_andCodigoNotNull`; `given_citaConfirmada_andMoreThan24h_when_cancelar_then_estadoCancelada`; `given_citaConfirmada_andLessThan24h_when_cancelar_then_throwsCancelacionFueraDePlazoException`)
- [X] T030 [P] [US1] Create `ReservarCitaUseCaseTest` (unit, mocked ports) — `src/test/java/org/ups/citamedicos/unit/application/ReservarCitaUseCaseTest.java` (`given_franjaLibre_when_execute_then_citaSavedAndNotificacionDispatched`; `given_idempotencyKeyExists_when_execute_then_returnExistingCita`; `given_pacienteAlreadyHasCitaSameDia_when_execute_then_throwsCitaDuplicadaException`; `given_franjaOcupada_when_execute_then_throwsFranjaNoDisponibleException`)
- [X] T031 [P] [US1] Create `CitaJpaAdapterTest` (@DataJpaTest) — `src/test/java/org/ups/citamedicos/integration/persistence/CitaJpaAdapterTest.java` (`given_seededMedicoAndPaciente_when_saveCita_then_persisted`; `given_citaSaved_when_findByCodigo_then_returnsNonEmpty`; `given_idempotencyKey_when_findByIdempotencyKey_then_returnsNonEmpty`)
- [X] T032 [P] [US1] Create `FranjaHorariaJpaAdapterTest` (@DataJpaTest) — `src/test/java/org/ups/citamedicos/integration/persistence/FranjaHorariaJpaAdapterTest.java` (`given_seededFranjaLibre_when_findById_then_returnsLibre`; `given_franjaReserved_when_saveWithVersion0_then_versionIncremented`; `given_staleVersion_when_save_then_throwsOptimisticLockException`)
- [X] T033 [US1] Create `CitaControllerPostMvcTest` (@WebMvcTest) — `src/test/java/org/ups/citamedicos/integration/web/CitaControllerPostMvcTest.java` (`given_validRequest_when_postCitas_then_201_withCitaResponse`; `given_missingPacienteId_when_postCitas_then_400`; `given_franjaOcupada_when_postCitas_then_409_withFRANJA_NO_DISPONIBLE`)
- [X] T034 [US1] Create `ReservarCitaFuncionalTest` (@SpringBootTest RANDOM_PORT) — `src/test/java/org/ups/citamedicos/functional/ReservarCitaFuncionalTest.java` (`given_seededFranjaLibre_when_postCitas_then_201_andFranjaNowOcupada`; `given_sameIdempotencyKey_when_postCitasTwice_then_sameCodigo_and_noDuplicate`)

### Implementation — US1 Output Ports (parallelizable)

- [X] T035 [P] [US1] Create `CitaRepositoryPort` interface — `src/main/java/org/ups/citamedicos/application/port/out/CitaRepositoryPort.java` (methods: `Cita save(Cita)`; `Optional<Cita> findByCodigo(CodigoCita)`; `Optional<Cita> findByIdempotencyKey(String)`; `boolean existsConfirmadaByPacienteAndMedicoAndFecha(UUID,UUID,LocalDate)`)
- [X] T036 [P] [US1] Create `FranjaHorariaRepositoryPort` interface — `src/main/java/org/ups/citamedicos/application/port/out/FranjaHorariaRepositoryPort.java` (methods: `Optional<FranjaHoraria> findById(UUID)`; `FranjaHoraria save(FranjaHoraria)`)
- [X] T037 [P] [US1] Create `PacienteRepositoryPort` interface — `src/main/java/org/ups/citamedicos/application/port/out/PacienteRepositoryPort.java` (method: `Optional<Paciente> findById(UUID)`)
- [X] T038 [P] [US1] Create `NotificacionWhatsAppPort` interface — `src/main/java/org/ups/citamedicos/application/port/out/NotificacionWhatsAppPort.java` (method: `void enviar(Notificacion, Paciente, Cita, Medico)`)
- [X] T039 [P] [US1] Create `NotificacionRepositoryPort` interface — `src/main/java/org/ups/citamedicos/application/port/out/NotificacionRepositoryPort.java` (methods: `Notificacion save(Notificacion)`; `List<Notificacion> findPendientesConIntentosInsuficientes(int maxIntentos)`)

### Implementation — US1 Input Port + Command

- [X] T040 [P] [US1] Create `ReservarCitaCommand` record + `ReservarCitaUseCase` interface — `src/main/java/org/ups/citamedicos/application/port/in/ReservarCitaUseCase.java` (command: `UUID pacienteId`, `UUID franjaHorariaId`, `String idempotencyKey`; interface: `Cita execute(ReservarCitaCommand)`)

### Implementation — US1 Use Case

- [X] T041 [US1] Create `ReservarCitaUseCaseImpl` — `src/main/java/org/ups/citamedicos/application/usecase/ReservarCitaUseCaseImpl.java` (implements `ReservarCitaUseCase`; logic: 1) check idempotency key → return existing if found; 2) load `FranjaHoraria` (404 if absent); 3) check `CitaDuplicada` (same paciente+medico+fecha); 4) call `franja.reservar()`; 5) `Cita.nueva(...)`; 6) save Cita + save Franja; 7) async send notification; single `execute` method; depends on T035–T040)

### Implementation — US1 JPA Entities (parallelizable)

- [X] T042 [P] [US1] Create `PacienteEntity` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/entity/PacienteEntity.java` (@Entity @Table("pacientes"); @Data @Builder @NoArgsConstructor @AllArgsConstructor Lombok; VARCHAR(36) id PK; fields per schema.sql)
- [X] T043 [P] [US1] Create `MedicoEntity` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/entity/MedicoEntity.java` (@Entity @Table("medicos"); @Data @Builder @NoArgsConstructor @AllArgsConstructor; fields per schema.sql; needed as FK target by CitaEntity)
- [X] T044 [P] [US1] Create `FranjaHorariaEntity` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/entity/FranjaHorariaEntity.java` (@Entity @Table("franjas_horarias"); @Version Long version; estado as String; @Table(uniqueConstraints) on (medico_id,fecha,hora_inicio); @Data @Builder)
- [X] T045 [US1] Create `CitaEntity` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/entity/CitaEntity.java` (@Entity @Table("citas"); FKs to PacienteEntity, MedicoEntity, FranjaHorariaEntity via @ManyToOne; @Column(unique=true) on codigo and idempotency_key; @Data @Builder; depends on T042–T044)
- [X] T046 [P] [US1] Create `NotificacionEntity` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/entity/NotificacionEntity.java` (@Entity @Table("notificaciones"); @ManyToOne CitaEntity; @Data @Builder)

### Implementation — US1 Spring Data Repositories (parallelizable)

- [X] T047 [P] [US1] Create `CitaJpaRepository` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/repository/CitaJpaRepository.java` (extends `JpaRepository<CitaEntity,String>`; `findByCodigo(String)`; `findByIdempotencyKey(String)`; `existsByPacienteIdAndMedicoIdAndFranjaHorariaFechaAndEstado(...)`)
- [X] T048 [P] [US1] Create `FranjaHorariaJpaRepository` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/repository/FranjaHorariaJpaRepository.java` (extends `JpaRepository<FranjaHorariaEntity,String>`; use standard `findById` — concurrency is handled by `@Version` on `FranjaHorariaEntity` + UNIQUE constraint on `(medico_id,fecha,hora_inicio)`; do NOT add `@Lock(PESSIMISTIC_WRITE)` — see research.md Decisión 2)
- [X] T049 [P] [US1] Create `NotificacionJpaRepository` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/repository/NotificacionJpaRepository.java` (extends `JpaRepository<NotificacionEntity,String>`; JPQL query: `findByEstadoEnvioInAndIntentosLessThan`)
- [X] T049b [P] [US1] Create `PacienteJpaRepository` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/repository/PacienteJpaRepository.java` (extends `JpaRepository<PacienteEntity,String>`; `Optional<PacienteEntity> findByDocumento(String documento)`; needed by `PacienteJpaAdapter` T052)

### Implementation — US1 JPA Adapters + Notification

- [X] T050 [US1] Create `CitaJpaAdapter` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/CitaJpaAdapter.java` (implements `CitaRepositoryPort`; maps `CitaEntity ↔ Cita`; depends on T045, T047)
- [X] T051 [US1] Create `FranjaHorariaJpaAdapter` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/FranjaHorariaJpaAdapter.java` (implements `FranjaHorariaRepositoryPort`; catches `OptimisticLockException` → rethrows as `FranjaNoDisponibleException`; maps `FranjaHorariaEntity ↔ FranjaHoraria`)
- [X] T052 [P] [US1] Create `PacienteJpaAdapter` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/PacienteJpaAdapter.java` (implements `PacienteRepositoryPort`; maps `PacienteEntity ↔ Paciente`)
- [X] T053 [P] [US1] Create `NotificacionJpaAdapter` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/NotificacionJpaAdapter.java` (implements `NotificacionRepositoryPort`; maps `NotificacionEntity ↔ Notificacion`)
- [X] T054 [P] [US1] Create `WhatsAppProperties` — `src/main/java/org/ups/citamedicos/infrastructure/config/WhatsAppProperties.java` (@ConfigurationProperties(prefix="whatsapp"); fields: `apiUrl`, `bearerToken`, `phoneNumberId`; add `@EnableConfigurationProperties` to UseCaseConfig or a dedicated config class)
- [X] T055 [US1] Create `WhatsAppCloudApiAdapter` — `src/main/java/org/ups/citamedicos/adapter/out/notification/WhatsAppCloudApiAdapter.java` (implements `NotificacionWhatsAppPort`; @Async; @Retryable(maxAttempts=3, backoff=@Backoff(delay=2000)); calls Meta Cloud API `/messages`; on final failure sets notification `estadoEnvio=FALLIDA`; depends on T054)

### Implementation — US1 Mapper + Controller + Config

- [X] T056 [P] [US1] Create `CitaMapper` — `src/main/java/org/ups/citamedicos/adapter/in/web/mapper/CitaMapper.java` (static `toResponse(Cita, Medico, FranjaHoraria): CitaResponse`; maps domain objects to generated DTO)
- [X] T057 [US1] Create `CitaController` — `src/main/java/org/ups/citamedicos/adapter/in/web/CitaController.java` (implements generated `CitasApi`; @RestController; injects `ReservarCitaUseCase`; `POST /api/v1/citas`: extracts `Idempotency-Key` header, builds `ReservarCitaCommand`, calls use case, returns 201 + `CitaResponse`)
- [X] T058 [US1] Create `UseCaseConfig` — `src/main/java/org/ups/citamedicos/infrastructure/config/UseCaseConfig.java` (@Configuration; @Bean `ReservarCitaUseCaseImpl` wiring all output port adapters; `@Bean` for `WhatsAppProperties`)
- [X] T059 [US1] Update `GlobalExceptionHandler` — `src/main/java/org/ups/citamedicos/adapter/in/web/GlobalExceptionHandler.java` (add: `FranjaNoDisponibleException` → 409 `FRANJA_NO_DISPONIBLE` using `FranjaNoDisponibleErrorResponse`; the handler MUST inject `ConsultarDisponibilidadUseCase` and populate the `alternativas` field with the next available LIBRE slots for the same `medicoId` extracted from the exception — satisfying FR-007; `CitaDuplicadaException` → 422 `CITA_DUPLICADA`; Bean Validation errors → 400)

**Checkpoint**: `./gradlew test --tests "*.unit.*" --tests "*.functional.ReservarCitaFuncionalTest"` MUST pass. US1 is independently functional and demonstrable.

---

## Phase 4: User Story 2 — Franja No Disponible (Priority: P2)

**Goal**: El sistema expone disponibilidad actualizada (solo franjas LIBRE) y rechaza reservas sobre franjas OCUPADA con HTTP 409 y mensaje orientador.

**Independent Test**: `GET /api/v1/medicos` lista médicos → `GET /api/v1/medicos/{id}/disponibilidad?fechaDesde=...&fechaHasta=...` retorna solo franjas LIBRE (seed data verifiable) → realizar dos reservas sobre la misma franja → segunda retorna 409.

### Tests BDD — US2 (write first; MUST FAIL before impl)

- [X] T060 [P] [US2] Create `BuscarMedicosUseCaseTest` (unit) — `src/test/java/org/ups/citamedicos/unit/application/BuscarMedicosUseCaseTest.java` (`given_especialidadFilter_when_execute_then_returnMatchingMedicos`; `given_noFilter_when_execute_then_returnAll`; `given_emptyResult_when_execute_then_returnEmptyList`)
- [X] T061 [P] [US2] Create `ConsultarDisponibilidadUseCaseTest` (unit) — `src/test/java/org/ups/citamedicos/unit/application/ConsultarDisponibilidadUseCaseTest.java` (`given_medicoWithFranjasLibres_when_execute_then_returnOnlyLibres`; `given_rangoMayorA60Dias_when_execute_then_throwsIllegalArgumentException`)
- [X] T062 [P] [US2] Create `MedicoJpaAdapterTest` (@DataJpaTest) — `src/test/java/org/ups/citamedicos/integration/persistence/MedicoJpaAdapterTest.java` (`given_seededMedicos_when_findByEspecialidad_then_filtered`; `given_seededMedicos_when_findAll_then_returnsThree`)
- [X] T063 [P] [US2] Create `FranjaHorariaDisponibilidadAdapterTest` (@DataJpaTest) — `src/test/java/org/ups/citamedicos/integration/persistence/FranjaHorariaDisponibilidadAdapterTest.java` (`given_seededFranjas_when_findDisponibles_then_onlyLibreInDateRange`; `given_allFranjasOcupadas_when_findDisponibles_then_emptyList`)
- [X] T064 [P] [US2] Create `MedicoControllerMvcTest` (@WebMvcTest) — `src/test/java/org/ups/citamedicos/integration/web/MedicoControllerMvcTest.java` (`given_validRequest_when_getMedicos_then_200_withList`; `given_validMedicoId_when_getDisponibilidad_then_200_onlyFranjasLibres`; `given_unknownMedicoId_when_getDisponibilidad_then_404`)
- [X] T065 [US2] Create `FranjaNoDisponibleFuncionalTest` (@SpringBootTest) — `src/test/java/org/ups/citamedicos/functional/FranjaNoDisponibleFuncionalTest.java` (`given_seededFranjaLibre_when_book_then_201`; `given_sameFranja_when_secondBook_then_409`; `given_seededMedico_when_getDisponibilidad_then_returnsOnlyLibreFranjas`)

### Implementation — US2 Ports + Commands (parallelizable)

- [X] T066 [P] [US2] Create `BuscarMedicosQuery` record + `BuscarMedicosUseCase` interface — `src/main/java/org/ups/citamedicos/application/port/in/BuscarMedicosUseCase.java` (query: `String especialidad` nullable, `String nombre` nullable; returns `List<Medico>`)
- [X] T067 [P] [US2] Create `ConsultarDisponibilidadQuery` record + `ConsultarDisponibilidadUseCase` interface — `src/main/java/org/ups/citamedicos/application/port/in/ConsultarDisponibilidadUseCase.java` (query: `UUID medicoId`, `LocalDate fechaDesde`, `LocalDate hasta`, `EstadoFranja estadoFiltro` nullable — null returns all estados, `LIBRE` returns only free slots; returns `List<FranjaHoraria>`; supports FR-002 visual differentiation of LIBRE/OCUPADA/BLOQUEADA)
- [X] T068 [P] [US2] Create `MedicoRepositoryPort` interface — `src/main/java/org/ups/citamedicos/application/port/out/MedicoRepositoryPort.java` (methods: `List<Medico> findByEspecialidadAndNombre(String,String)`; `Optional<Medico> findById(UUID)`)
- [X] T069 [P] [US2] Update `FranjaHorariaRepositoryPort` — `src/main/java/org/ups/citamedicos/application/port/out/FranjaHorariaRepositoryPort.java` (add: `List<FranjaHoraria> findByMedicoAndRangoFecha(UUID medicoId, LocalDate desde, LocalDate hasta, EstadoFranja estadoFiltro)` — null estadoFiltro returns all estados, enabling FR-002 visual differentiation)

### Implementation — US2 Use Cases

- [X] T070 [US2] Create `BuscarMedicosUseCaseImpl` — `src/main/java/org/ups/citamedicos/application/usecase/BuscarMedicosUseCaseImpl.java` (implements `BuscarMedicosUseCase`; delegates to `MedicoRepositoryPort`; single `execute` method)
- [X] T071 [US2] Create `ConsultarDisponibilidadUseCaseImpl` — `src/main/java/org/ups/citamedicos/application/usecase/ConsultarDisponibilidadUseCaseImpl.java` (implements `ConsultarDisponibilidadUseCase`; validates `hasta - desde ≤ 60 days` → `IllegalArgumentException`; validates `medicoId` exists via `MedicoRepositoryPort`; delegates to `FranjaHorariaRepositoryPort`)

### Implementation — US2 JPA Repository + Adapters + Mappers + Controllers

- [X] T072 [P] [US2] Create `MedicoJpaRepository` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/repository/MedicoJpaRepository.java` (extends `JpaRepository<MedicoEntity,String>`; `findByEspecialidadContainingIgnoreCaseOrNombreContainingIgnoreCase(String,String)`)
- [X] T073 [US2] Create `MedicoJpaAdapter` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/MedicoJpaAdapter.java` (implements `MedicoRepositoryPort`; maps `MedicoEntity ↔ Medico`)
- [X] T074 [P] [US2] Update `FranjaHorariaJpaRepository` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/repository/FranjaHorariaJpaRepository.java` (add JPQL: `findByMedicoIdAndFechaBetweenAndEstado(String medicoId, LocalDate desde, LocalDate hasta, String estado)`)
- [X] T075 [US2] Update `FranjaHorariaJpaAdapter` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/FranjaHorariaJpaAdapter.java` (implement `findDisponiblesByMedicoAndRangoFecha` using T074 query with `estado="LIBRE"`)
- [X] T076 [P] [US2] Create `FranjaHorariaMapper` — `src/main/java/org/ups/citamedicos/adapter/in/web/mapper/FranjaHorariaMapper.java` (static `toResponse(FranjaHoraria): FranjaHorariaResponse`)
- [X] T077 [P] [US2] Create `MedicoMapper` — `src/main/java/org/ups/citamedicos/adapter/in/web/mapper/MedicoMapper.java` (static `toResponse(Medico): MedicoResponse`)
- [X] T078 [US2] Create `MedicoController` — `src/main/java/org/ups/citamedicos/adapter/in/web/MedicoController.java` (implements generated `MedicosApi`; `GET /api/v1/medicos`: calls `BuscarMedicosUseCase`, returns list; `GET /api/v1/medicos/{medicoId}/disponibilidad`: calls `ConsultarDisponibilidadUseCase`, returns list)
- [X] T079 [US2] Update `UseCaseConfig` — `src/main/java/org/ups/citamedicos/infrastructure/config/UseCaseConfig.java` (add `@Bean` for `BuscarMedicosUseCaseImpl` and `ConsultarDisponibilidadUseCaseImpl`)
- [X] T080 [US2] Update `GlobalExceptionHandler` — `src/main/java/org/ups/citamedicos/adapter/in/web/GlobalExceptionHandler.java` (add: `IllegalArgumentException` on date range → 400; `NoSuchElementException` → 404 `RECURSO_NO_ENCONTRADO`)

**Checkpoint**: `./gradlew test --tests "*.unit.*" --tests "*.functional.FranjaNoDisponibleFuncionalTest"` MUST pass. US1 + US2 independently verifiable.

---

## Phase 5: User Story 3 — Consulta y Cancelación de Citas (Priority: P3)

**Goal**: El paciente ve sus citas activas y puede cancelar con ≥ 24h de antelación; la franja vuelve a LIBRE y se envía notificación de cancelación.

**Independent Test**: (1) crear cita → (2) `GET /api/v1/pacientes/{id}/citas` retorna la cita en CONFIRMADA → (3) `DELETE /api/v1/citas/{codigo}` → 204 → (4) franja vuelve a LIBRE en H2 → (5) nuevo intento de `DELETE` → 409 (ya CANCELADA).

### Tests BDD — US3 (write first; MUST FAIL before impl)

- [X] T081 [P] [US3] Create `CancelarCitaUseCaseTest` (unit) — `src/test/java/org/ups/citamedicos/unit/application/CancelarCitaUseCaseTest.java` (`given_citaConfirmadaWith25hAntelacion_when_execute_then_estadoCancelada_andFranjaLibre`; `given_citaConfirmadaWith12h_when_execute_then_throwsCancelacionFueraDePlazoException`; `given_citaYaCancelada_when_execute_then_throwsIllegalState`)
- [X] T082 [P] [US3] Create `ConsultarCitasPacienteUseCaseTest` (unit) — `src/test/java/org/ups/citamedicos/unit/application/ConsultarCitasPacienteUseCaseTest.java` (`given_pacienteWithCitas_when_execute_then_returnConfirmadasSortedByFecha`; `given_pacienteNotFound_when_execute_then_throwsNoSuchElement`)
- [X] T083 [P] [US3] Create `CitaControllerDeleteMvcTest` (@WebMvcTest) — `src/test/java/org/ups/citamedicos/integration/web/CitaControllerDeleteMvcTest.java` (`given_validCodigo_when_delete_then_204`; `given_citaFueraDePlazo_when_delete_then_400_CANCELACION_FUERA_DE_PLAZO`; `given_unknownCodigo_when_delete_then_404`)
- [X] T084 [P] [US3] Create `CitaControllerGetMvcTest` (@WebMvcTest) — `src/test/java/org/ups/citamedicos/integration/web/CitaControllerGetMvcTest.java` (`given_pacienteWithCitas_when_getCitas_then_200_withList`; `given_unknownPaciente_when_getCitas_then_404`)
- [X] T085 [US3] Create `CancelarCitaFuncionalTest` (@SpringBootTest) — `src/test/java/org/ups/citamedicos/functional/CancelarCitaFuncionalTest.java` (must cover BOTH US3 acceptance scenarios 1-to-1 per Constitution Principle II: AC-1: `given_pacienteWithCita_when_getCitasPaciente_then_200_withCitaListedAndEstadoConfirmada`; AC-2: `given_seededFranja_when_bookThenCancel_then_204_andFranjaLibreAgain`; additional: `given_cancelledCita_when_cancelAgain_then_409`; `given_citaWith12h_when_cancel_then_400`)

### Implementation — US3 Ports + Commands (parallelizable)

- [X] T086 [P] [US3] Create `CancelarCitaCommand` record + `CancelarCitaUseCase` interface — `src/main/java/org/ups/citamedicos/application/port/in/CancelarCitaUseCase.java` (command: `CodigoCita codigoCita`; interface: `void execute(CancelarCitaCommand)`)
- [X] T087 [P] [US3] Create `ConsultarCitasPacienteQuery` record + `ConsultarCitasPacienteUseCase` interface — `src/main/java/org/ups/citamedicos/application/port/in/ConsultarCitasPacienteUseCase.java` (query: `UUID pacienteId`; interface: `List<Cita> execute(ConsultarCitasPacienteQuery)`)
- [X] T088 [P] [US3] Update `CitaRepositoryPort` — `src/main/java/org/ups/citamedicos/application/port/out/CitaRepositoryPort.java` (add: `Optional<Cita> findByCodigoForUpdate(CodigoCita)`; `List<Cita> findConfirmadasByPacienteId(UUID pacienteId)`)

### Implementation — US3 Use Cases

- [X] T089 [US3] Create `CancelarCitaUseCaseImpl` — `src/main/java/org/ups/citamedicos/application/usecase/CancelarCitaUseCaseImpl.java` (implements `CancelarCitaUseCase`; load cita by codigo (404 if absent); call `cita.cancelar(Instant.now())`; save CANCELADA cita; load franja → `franja.liberar()` → save LIBRE franja; async notify `CANCELACION`)
- [X] T090 [US3] Create `ConsultarCitasPacienteUseCaseImpl` — `src/main/java/org/ups/citamedicos/application/usecase/ConsultarCitasPacienteUseCaseImpl.java` (implements `ConsultarCitasPacienteUseCase`; validates paciente exists via `PacienteRepositoryPort`; returns `findConfirmadasByPacienteId` sorted by fecha+horaInicio)

### Implementation — US3 Repository Updates + Controller + Config

- [X] T091 [P] [US3] Update `CitaJpaRepository` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/repository/CitaJpaRepository.java` (add: `List<CitaEntity> findByPacienteIdAndEstadoOrderByFranjaHorariaFechaAscFranjaHorariaHoraInicioAsc(String pacienteId, String estado)`)
- [X] T092 [US3] Update `CitaJpaAdapter` — `src/main/java/org/ups/citamedicos/adapter/out/persistence/CitaJpaAdapter.java` (implement `findByCodigoForUpdate` and `findConfirmadasByPacienteId` from T088)
- [X] T093 [US3] Update `CitaController` — `src/main/java/org/ups/citamedicos/adapter/in/web/CitaController.java` (add: `DELETE /api/v1/citas/{codigoCita}` → inject `CancelarCitaUseCase` → 204; add: `GET /api/v1/pacientes/{pacienteId}/citas` → inject `ConsultarCitasPacienteUseCase` → 200 list)
- [X] T094 [US3] Update `UseCaseConfig` — `src/main/java/org/ups/citamedicos/infrastructure/config/UseCaseConfig.java` (add `@Bean` for `CancelarCitaUseCaseImpl` and `ConsultarCitasPacienteUseCaseImpl`)
- [X] T095 [US3] Update `GlobalExceptionHandler` — `src/main/java/org/ups/citamedicos/adapter/in/web/GlobalExceptionHandler.java` (add: `CancelacionFueraDePlazoException` → 400 `CANCELACION_FUERA_DE_PLAZO`; `IllegalStateException` on already-cancelled cita → 409)

**Checkpoint**: `./gradlew test` — all 3 functional tests pass. All 3 stories independently demonstrable via quickstart.md.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Coverage enforcement, ArchUnit validation, contract validation, and final documentation.

- [X] T096 Run `./gradlew check` and confirm all unit + integration + functional tests pass and JaCoCo thresholds met (global ≥ 80%, per-class ≥ 80%) — fix any failing classes before proceeding
- [X] T097 [P] Open `build/reports/jacoco/test/html/index.html`; identify any production class with instruction coverage < 80%; add missing BDD test cases to reach the threshold
- [X] T098 [P] Confirm `ArchitectureTest` passes with zero violations: no Spring imports in `domain` or `application`, no `jakarta.persistence` in `domain`; fix any violations
- [X] T099 [P] Run `./gradlew openApiGenerate` and verify no drift between contract `src/main/resources/openapi/citamedicos-api.yaml` and generated interfaces; update contract if any controller signature diverged
- [X] T100 [P] Verify `@DataJpaTest` and `@SpringBootTest` tests load seed data from `classpath:db/schema.sql` and `classpath:db/data.sql` correctly; if any test context needs isolation add targeted `@Sql(scripts=..., executionPhase=BEFORE_TEST_METHOD)` annotations rather than duplicating seed files
- [X] T101 [P] Add `@Generated` marker to `CitamedicosServiceApplication` to exclude it from JaCoCo per-class verification — `src/main/java/org/ups/citamedicos/infrastructure/CitamedicosServiceApplication.java`
- [X] T102 Validate all quickstart.md steps work end-to-end with `./gradlew bootRun`; update commands or expected outputs if they diverged from implementation — `specs/001-reserva-cita-online/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No deps — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 — BLOCKS all user stories
- **Phase 3 (US1)**: Depends on Phase 2 complete
- **Phase 4 (US2)**: Depends on Phase 2; may start in parallel with Phase 3 (reuses `MedicoEntity` from T043 — coordinate if parallel)
- **Phase 5 (US3)**: Depends on Phase 3 complete (`CitaEntity`, `CitaJpaAdapter`, `CitaRepositoryPort`)
- **Phase 6 (Polish)**: Depends on all story phases complete

### User Story Dependencies

- **US1 (P1)**: Independent after Phase 2
- **US2 (P2)**: Independent after Phase 2; shares `MedicoEntity`/`FranjaHorariaJpaAdapter` with US1
- **US3 (P3)**: Extends US1 ports and adapters — start after T045, T050 merged

### Within Each User Story

1. BDD tests written → confirmed **FAILING** (red)
2. Output port interfaces defined (needed for test compilation)
3. Use case implementation (unit tests go **GREEN**)
4. JPA entities + repositories (integration tests go **GREEN**)
5. JPA adapters + controller (functional tests go **GREEN**)
6. Checkpoint: story passes its functional test independently

### Parallel Opportunities per Phase

```text
Phase 2 parallel group A (T009–T014): all value objects + enums
Phase 2 parallel group B (T015–T017): all exceptions
Phase 2 parallel group C (T018, T019, T022): independent entities

Phase 3 parallel group A (T026–T030): all unit tests
Phase 3 parallel group B (T035–T040): all output ports + input port

Phase 4 parallel group A (T060–T064): all US2 unit + integration tests
Phase 4 parallel group B (T066–T069): all US2 ports

Phase 5 parallel group A (T081–T084): all US3 tests
Phase 5 parallel group B (T086–T088): all US3 ports
```

---

## Parallel Example: Phase 3 — Domain Tests + Ports

```bash
# Run in parallel (all distinct files, no deps):
Task T026: "Create CodigoCitaTest in .../unit/domain/"
Task T027: "Create NumeroWhatsAppTest in .../unit/domain/"
Task T028: "Create FranjaHorariaTest in .../unit/domain/"
Task T029: "Create CitaTest in .../unit/domain/"
Task T030: "Create ReservarCitaUseCaseTest in .../unit/application/"
Task T035: "Create CitaRepositoryPort in .../port/out/"
Task T036: "Create FranjaHorariaRepositoryPort in .../port/out/"
Task T037: "Create PacienteRepositoryPort in .../port/out/"
Task T038: "Create NotificacionWhatsAppPort in .../port/out/"
Task T040: "Create ReservarCitaCommand + ReservarCitaUseCase in .../port/in/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Phase 1: Setup (T001–T005)
2. Phase 2: Foundational (T006–T025) — includes DB schema + seed data
3. Phase 3: User Story 1 (T026–T059)
4. **STOP and VALIDATE**: `./gradlew test --tests "*.functional.ReservarCitaFuncionalTest"` MUST pass
5. Demo: `./gradlew bootRun` → use quickstart.md to book a cita from seed data → 201 + H2 console shows OCUPADA franja

### Incremental Delivery

1. Setup + Foundational → compile passes, H2 boots, seed data queryable via H2 console
2. US1 → end-to-end booking works → **demo MVP**
3. US2 → availability query + 409 conflict → **demo US2**
4. US3 → list + cancel → **demo US3**
5. Polish → `./gradlew check` GREEN, JaCoCo report at ≥ 80%

### Parallel Team Strategy (after Phase 2 complete)

- Dev A: Phase 3 — US1 (T026–T059)
- Dev B: Phase 4 — US2 (T060–T080); coordinate on `MedicoEntity` T043 with Dev A
- Dev C: After Dev A completes T045 + T050 — Phase 5 US3 (T081–T095)

---

## Notes

- `[P]` tasks = distinct files with no dependency on incomplete tasks — safe to parallelize
- `[US#]` maps every task to its spec.md user story for full traceability
- BDD tests MUST be written and FAILING before the corresponding `UseCaseImpl` task starts (Principle II)
- `db/schema.sql` + `db/data.sql` in `src/main/resources/db/` are the single source of truth for schema; Hibernate `ddl-auto=none`
- `@DataJpaTest` contexts pick up `classpath:db/schema.sql` + `classpath:db/data.sql` automatically via `spring.sql.init`
- Generated code in `build/generated/` is NEVER edited manually (Principle IV)
- Domain entities (`Cita`, `FranjaHoraria`, etc.) have ZERO imports from `org.springframework.*` or `jakarta.persistence.*` (Principle I + ArchUnit T025)
- `./gradlew check` = compile + tests + JaCoCo thresholds — the single exit gate before merging
