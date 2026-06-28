# Quickstart — Reserva de Cita en Línea 24/7

**Feature**: US-01 | **Date**: 2026-06-27

Guía de validación para confirmar que el flujo de reserva funciona de extremo a extremo.

---

## Prerrequisitos

| Herramienta | Versión mínima | Verificar con |
|-------------|---------------|--------------|
| Java JDK | 25 | `java --version` |
| Gradle | Wrapper incluido | `./gradlew --version` |
| curl o Postman | cualquier versión | `curl --version` |

---

## 1. Levantar el servicio

```bash
# Desde la raíz del repositorio
./gradlew bootRun
```

El servicio queda disponible en `http://localhost:8080/api/v1`.
H2 Console (solo desarrollo): `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Usuario: `sa` / Contraseña: (vacía)

---

## 2. Validación: US1 — Reserva exitosa (Happy path · P1)

### 2.1 Buscar médicos disponibles

```bash
curl -s "http://localhost:8080/api/v1/medicos?especialidad=Cardiolog" | jq .
```

**Resultado esperado**: array JSON con al menos un médico; anota el campo `id`.

### 2.2 Consultar disponibilidad del médico

```bash
MEDICO_ID="<id del paso anterior>"

curl -s "http://localhost:8080/api/v1/medicos/${MEDICO_ID}/disponibilidad\
?fechaDesde=2026-07-01&fechaHasta=2026-07-07" | jq .
```

**Resultado esperado**: array de franjas con `"estado": "LIBRE"`.
Anota el `id` de una franja.

### 2.3 Reservar la cita

```bash
FRANJA_ID="<id de la franja del paso anterior>"
PACIENTE_ID="<id del paciente de prueba>"
IDEM_KEY=$(uuidgen)   # o cualquier UUID v4

curl -s -X POST "http://localhost:8080/api/v1/citas" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: ${IDEM_KEY}" \
  -d "{\"pacienteId\": \"${PACIENTE_ID}\", \"franjaHorariaId\": \"${FRANJA_ID}\"}" \
  | jq .
```

**Resultado esperado**:
- HTTP 201 Created.
- Cuerpo con `"estado": "CONFIRMADA"` y campo `"codigo"` (UUID).
- La notificación WhatsApp es asíncrona; verificar en logs: `INFO Notificacion enviada`.

### 2.4 Verificar idempotencia

Repetir exactamente el mismo `curl` del paso 2.3 con el **mismo `Idempotency-Key`**.

**Resultado esperado**: HTTP 201 con la **misma cita**; no se crea un registro duplicado.

---

## 3. Validación: US2 — Franja no disponible (P2)

### 3.1 Intentar reservar la misma franja con otro paciente

```bash
OTRO_PACIENTE_ID="<id de otro paciente>"

curl -s -X POST "http://localhost:8080/api/v1/citas" \
  -H "Content-Type: application/json" \
  -d "{\"pacienteId\": \"${OTRO_PACIENTE_ID}\", \"franjaHorariaId\": \"${FRANJA_ID}\"}" \
  | jq .
```

**Resultado esperado**:
- HTTP 409 Conflict.
- Cuerpo con `"codigo": "FRANJA_NO_DISPONIBLE"`.
- El sistema no registra ninguna cita adicional.

---

## 4. Validación: US3 — Consulta y cancelación (P3)

### 4.1 Consultar citas del paciente

```bash
curl -s "http://localhost:8080/api/v1/pacientes/${PACIENTE_ID}/citas" | jq .
```

**Resultado esperado**: array con la cita creada en el paso 2.3 en estado `CONFIRMADA`.

### 4.2 Cancelar la cita

> Nota: la franja debe estar a más de 24 horas del momento actual.
> Si el entorno de prueba no lo cumple, usa datos de prueba con fecha futura.

```bash
CODIGO_CITA="<codigo del paso 2.3>"

curl -s -X DELETE "http://localhost:8080/api/v1/citas/${CODIGO_CITA}" -v
```

**Resultado esperado**:
- HTTP 204 No Content.
- Al consultar nuevamente la disponibilidad del médico, la franja aparece con
  `"estado": "LIBRE"`.
- En logs: `INFO Notificacion de cancelacion enviada`.

### 4.3 Intentar cancelar fuera de plazo

Repetir el DELETE con una cita cuya franja esté a menos de 24 horas.

**Resultado esperado**: HTTP 400 con `"codigo": "CANCELACION_FUERA_DE_PLAZO"`.

---

## 5. Ejecutar la suite de tests completa

```bash
./gradlew check
```

**Resultado esperado**:
- Todos los tests pasan (unit + integration + functional).
- JaCoCo report generado en `build/reports/jacoco/test/html/index.html`.
- Coverage global ≥ 80%, cobertura por clase ≥ 80%.
- `BUILD SUCCESSFUL`.

---

## Referencias

- Contrato OpenAPI: [`contracts/citamedicos-api.yaml`](contracts/citamedicos-api.yaml)
- Modelo de datos: [`data-model.md`](data-model.md)
- Decisiones técnicas: [`research.md`](research.md)
- Plan de implementación: [`plan.md`](plan.md)
