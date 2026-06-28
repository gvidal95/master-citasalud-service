# Research: Reserva de Cita en Línea 24/7

**Feature**: US-01 · Reserva de Cita en Línea
**Date**: 2026-06-27

---

## Decisión 1: Integración con WhatsApp para notificaciones

**Decision**: Usar la **WhatsApp Business Cloud API** (Meta) directamente a través de
llamadas HTTP desde la capa de infraestructura, encapsuladas detrás del puerto
`NotificacionWhatsAppPort`.

**Rationale**:
- Es la API oficial de Meta, disponible sin coste adicional de tercero.
- Permite envío de mensajes de plantilla (template messages) aprobados, que son
  obligatorios para mensajes iniciados por la empresa (outbound).
- Encapsulada detrás de un puerto (Principle I), puede sustituirse por Twilio/360dialog
  sin tocar la capa de aplicación.
- El envío asíncrono (FR-011) se implementa con un reintento en la capa de
  infraestructura: si el primer intento falla, se encola el reintento con backoff
  exponencial usando `@Async` + `@Retryable` de Spring.

**Alternatives considered**:
- **Twilio WhatsApp API**: más sencilla de integrar pero añade coste por mensaje;
  rechazada por YAGNI (no hay requisito de pago por volumen en esta historia).
- **Email como canal de notificación**: no especificado en los criterios de aceptación;
  fuera del alcance de esta historia.

---

## Decisión 2: Control de concurrencia para prevenir doble reserva

**Decision**: **Bloqueo optimista con `@Version` de JPA** sobre la entidad
`FranjaHorariaEntity`, combinado con una restricción `UNIQUE` a nivel de base de datos
sobre `(medico_id, fecha, hora_inicio)`.

**Rationale**:
- El escenario de colisión (dos pacientes reservan la misma franja simultáneamente)
  es poco frecuente; el bloqueo optimista minimiza la contención en el caso común.
- Si se detecta colisión (`OptimisticLockException`), el caso de uso lanza
  `FranjaNoDisponibleException`, que el controlador mapea a HTTP 409 Conflict.
- La restricción UNIQUE en BD actúa como red de seguridad independiente del ORM.
- Cumple SC-003 (cero citas duplicadas bajo intentos concurrentes).

**Alternatives considered**:
- **Bloqueo pesimista (`SELECT FOR UPDATE`)**: garantía más fuerte pero mayor
  contención; rechazado porque el patrón de uso (pocas colisiones, muchas lecturas)
  no justifica el coste de bloqueo (YAGNI).
- **Saga / reserva en dos fases**: over-engineering para un servicio monolítico;
  rechazado (YAGNI).

---

## Decisión 3: Estrategia de código único de cita (FR-006)

**Decision**: **UUID v4** generado en el dominio al crear la `Cita`, almacenado como
`VARCHAR(36)` con índice único en la tabla `citas`.

**Rationale**:
- Sin dependencia de la secuencia auto-incremental de la BD; la entidad de dominio
  puede generar su propio identificador sin conocer la infraestructura (Principle I).
- UUIDs son globalmente únicos y seguros de exponer en URLs y notificaciones.
- Longitud fija facilita la validación en el contrato OpenAPI (pattern regex).

**Alternatives considered**:
- **Código alfanumérico corto** (ej. `CIT-2026-00001`): legible para humanos pero
  requiere secuencia gestionada por la BD; introduce acoplamiento al esquema de BD
  en la capa de dominio.
- **ULID**: monotónico y ordenable, pero añade dependencia sin beneficio claro para
  este caso de uso.

---

## Decisión 4: Validación de arquitectura limpia en tiempo de test

**Decision**: **ArchUnit** (biblioteca de reglas de arquitectura para JUnit 5) incluida
en `testImplementation`, con una clase `ArchitectureTest` en el paquete de integración
que valida las reglas de dependencia entre capas.

**Rationale**:
- Cumple el requisito de la Constitución (Quality Standards): "No production code may
  import from `org.springframework` in the domain or application layers".
- Falla el build si un futuro desarrollador viola la Dependency Rule accidentalmente.
- No requiere configuración externa; se ejecuta como test JUnit dentro de `./gradlew check`.

**Rules to enforce**:
```
domain   → ninguna dependencia externa
application → solo domain
adapter  → application, domain (nunca infrastructure directamente)
infrastructure → cualquier capa
```

---

## Decisión 5: Generación de stubs OpenAPI

**Decision**: **`openapi-generator-gradle-plugin`** v7.x con generador `spring`,
configurado para producir únicamente las **interfaces** de controlador y los **DTOs**
de request/response. Las implementaciones de controlador se escriben manualmente en la
capa de adaptadores, implementando las interfaces generadas.

**Rationale**:
- Alineado con Principle IV: el contrato es la fuente de verdad; el código se genera,
  no se escribe.
- `generateApiTests=false` y `generateModelTests=false` evitan que el plugin genere
  tests que colisionen con los tests BDD manuales.
- Output en `build/generated/openapi/` excluido de JaCoCo (Principle V).

**Gradle config sketch**:
```groovy
openApiGenerate {
    generatorName = "spring"
    inputSpec = "$rootDir/src/main/resources/openapi/citamedicos-api.yaml"
    outputDir = "$buildDir/generated/openapi"
    apiPackage = "org.ups.citamedicos.adapter.in.web.generated"
    modelPackage = "org.ups.citamedicos.adapter.in.web.generated.dto"
    configOptions = [
        interfaceOnly: "true",
        useSpringBoot3: "true",
        generateApiTests: "false",
        generateModelTests: "false"
    ]
}
```

---

## Decisión 6: Estrategia de idempotencia (edge case — pérdida de conexión)

**Decision**: El cliente envía un **`Idempotency-Key`** (UUID) en el header de la
petición `POST /api/v1/citas`. Si el servidor ya procesó esa clave, devuelve la cita
existente sin crear un duplicado.

**Rationale**:
- Resuelve el edge case de pérdida de conexión (spec.md Edge Cases §1).
- La clave se almacena en la tabla `citas` con índice único; el intento duplicado
  retorna 200 con la cita original.
- Implementado en la capa de adaptadores (controlador) sin contaminar la capa de dominio.

---

## Decisión 7: Persistencia — H2 en desarrollo, compatible con PostgreSQL

**Decision**: Usar **H2** (ya declarado en `build.gradle`) para desarrollo y tests,
con SQL ANSI-compatible para garantizar portabilidad a PostgreSQL en producción.

**Rationale**:
- H2 está ya configurado; no se cambia el stack sin un requisito explícito (YAGNI).
- Las migraciones de esquema se gestionarán con Flyway o Liquibase en el momento en
  que se produzca la migración a PostgreSQL (fuera del alcance de esta historia).
- JPA DDL-auto en `create-drop` solo para tests; `validate` para producción.

---

## Resolución de NEEDS CLARIFICATION

No había marcadores `[NEEDS CLARIFICATION]` en spec.md. Todos los detalles técnicos
han sido resueltos mediante las decisiones anteriores.
