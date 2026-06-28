<!--
## Sync Impact Report

**Version change**: [PROJECT_NAME] Constitution (blank template) → 1.0.0

### Modified principles
- All principles: NEW (no previous principles existed — template placeholders replaced)

### Added sections
- Core Principles (5 principles)
- Quality Standards
- Development Workflow
- Governance

### Removed sections
- None

### Templates requiring updates
- ✅ `.specify/templates/plan-template.md` — Constitution Check gates align with all 5 principles
- ✅ `.specify/templates/spec-template.md` — BDD acceptance scenarios already present; OpenAPI contract artifact noted
- ✅ `.specify/templates/tasks-template.md` — Testing phases and contract tasks align with BDD + API-First principles

### Deferred TODOs
- TODO(RATIFICATION_DATE): Exact project start date not recorded; using 2026-06-27 (first constitution authoring date).
-->

# citamedicos-service Constitution

## Core Principles

### I. Clean Architecture (NON-NEGOTIABLE)

The system MUST be structured following Robert C. Martin's Clean Architecture:

- **Domain layer** (entities, value objects, domain events): zero external dependencies.
  No Spring annotations, no JPA annotations, no framework types.
- **Application layer** (use cases / interactors): orchestrates domain objects and declares
  ports (interfaces). Depends only on the domain layer.
- **Adapters layer** (controllers, repository implementations, mappers): translates between
  the application layer and external mechanisms. Depends inward on application/domain.
- **Infrastructure layer** (Spring Boot configuration, JPA implementations, HTTP clients,
  message brokers): depends only inward; never referenced by inner layers.
- The **Dependency Rule** is absolute: source code dependencies MUST point inward.
  Inner layers MUST NOT import from outer layers.
- Cross-layer communication MUST go through declared interfaces (ports/adapters pattern).
- Every use case class MUST have a single public method (`execute`) and implement a
  well-named interface from the application layer.

**Rationale**: Keeps business logic free from framework churn and enables independent
testability at each layer without bootstrapping the full Spring context.

### II. BDD Testing Strategy (NON-NEGOTIABLE)

All tests MUST follow the Given-When-Then (BDD) style and be organized into three tiers:

- **Unit tests** (`src/test/java/.../unit/`): test a single class in isolation.
  Dependencies MUST be mocked (Mockito). Cover all use cases, domain entities, and
  value objects. Each test method MUST follow the naming convention:
  `given_<context>_when_<action>_then_<outcome>`.
- **Integration tests** (`src/test/java/.../integration/`): test the interaction between
  two or more components with real dependencies (e.g., JPA repository against H2,
  Spring MVC with `MockMvc`). MUST NOT call external systems in CI.
- **Functional/acceptance tests** (`src/test/java/.../functional/`): test complete
  end-to-end flows from the HTTP API layer using an in-process Spring Boot context
  (`@SpringBootTest`). Scenarios MUST map 1-to-1 with spec.md acceptance scenarios.
- Tests for a feature MUST be written and confirmed failing BEFORE implementation begins
  (red phase of Red-Green-Refactor).
- No production code may be merged without a corresponding test covering the happy path
  and at least one failure/edge case.

**Rationale**: BDD-style tests serve as living documentation and catch regressions at the
right layer, reducing both the cost of failure discovery and the cognitive load of
understanding requirements.

### III. Code Quality Principles (SOLID, DRY, YAGNI)

All production code MUST adhere to the following:

- **Single Responsibility**: every class and method has exactly one reason to change.
  Use cases, repositories, and controllers MUST NOT accumulate unrelated logic.
- **Open/Closed**: domain and application abstractions MUST be open for extension via
  new implementations, closed for modification of existing contracts.
- **Liskov Substitution**: every port implementation MUST be substitutable without
  altering correctness of calling code.
- **Interface Segregation**: ports and repository interfaces MUST be narrow — no client
  MUST depend on methods it does not use. Prefer multiple focused interfaces over
  one large one.
- **Dependency Inversion**: high-level modules (use cases) MUST depend on abstractions
  (port interfaces), never on concrete adapters or Spring beans directly.
- **DRY**: shared logic MUST be extracted into domain services or utility classes rather
  than duplicated across use cases or adapters.
- **YAGNI**: no speculative abstractions, generic frameworks, or placeholder extension
  points MUST be introduced without a concrete, immediate requirement.

**Rationale**: These constraints prevent the codebase from drifting into an anemic domain
model or a Big Ball of Mud as the service grows.

### IV. API First with OpenAPI (NON-NEGOTIABLE)

The API contract MUST be defined BEFORE any implementation begins:

- Every HTTP endpoint MUST have a corresponding definition in an OpenAPI 3.x contract
  file located at `src/main/resources/openapi/citamedicos-api.yaml`.
- Server-side stubs (interfaces, request/response DTOs) MUST be generated using
  `openapi-generator` as part of the Gradle build (`openapi-generator-gradle-plugin`).
  Hand-writing DTOs that duplicate OpenAPI schema definitions is forbidden.
- The generated controller interface MUST be implemented by the adapter-layer controller.
  The controller MUST NOT deviate from the generated method signatures.
- Any change to an existing endpoint (path, method, request/response shape) MUST first
  update the OpenAPI contract; the implementation follows.
- The contract MUST include: description, operationId, request schema, response schemas
  (including error responses 400, 404, 422, 500), and at least one usage example.
- Breaking changes to the contract MUST be versioned via the API path prefix
  (e.g., `/api/v1/`, `/api/v2/`).

**Rationale**: API First decouples consumer and producer development, ensures accurate
documentation, and eliminates drift between docs and implementation.

### V. Coverage Gates with JaCoCo (NON-NEGOTIABLE)

Code coverage MUST meet the following minimum thresholds, enforced by the CI build:

- **Per-class coverage**: MUST be ≥ 80% (instruction coverage) for every non-generated,
  non-configuration class. Classes with lower coverage cause the build to FAIL.
- **Global coverage**: MUST be ≥ 80% (instruction coverage) across all production classes.
- Generated sources (openapi-generator output under `build/generated/`) MUST be excluded
  from JaCoCo measurement.
- JaCoCo MUST be configured in `build.gradle` with:
  - `jacocoTestReport` task producing HTML + XML reports under `build/reports/jacoco/`.
  - `jacocoTestCoverageVerification` task with the above rules, bound to the `check`
    lifecycle task so `./gradlew check` enforces coverage.
- Coverage reports MUST be produced and archived in CI on every pull request.
- Suppressing coverage via `@Generated` or exclusion patterns is only permitted for
  Lombok-generated bytecode, Spring Boot entry point, and openapi-generator artifacts.

**Rationale**: Explicit coverage gates prevent gradual test erosion and give reviewers
an objective signal that new code is adequately tested.

## Quality Standards

The following non-negotiable quality gates MUST pass before any feature branch is merged:

- `./gradlew check` (compiles, runs all tests, and enforces JaCoCo coverage thresholds) MUST
  exit with code 0.
- No Checkstyle or SpotBugs violations above severity WARN (when configured).
- OpenAPI contract MUST be valid according to the OpenAPI 3.x specification
  (`openapi-generator validate`).
- No production code may import from `org.springframework` in the domain or application
  layers — verified by ArchUnit rules in the integration test suite.
- Lombok annotations (`@Data`, `@Builder`, `@Value`, etc.) are permitted only in the
  adapters and infrastructure layers. Domain entities MUST NOT use Lombok.

## Development Workflow

1. **Contract first**: update or create the OpenAPI definition in
   `src/main/resources/openapi/citamedicos-api.yaml`.
2. **Regenerate stubs**: run `./gradlew openApiGenerate` to produce updated interfaces
   and DTOs.
3. **Write BDD tests**: add/update unit, integration, and functional tests in
   the appropriate package. Confirm tests fail (red).
4. **Implement**: write the domain, application, and adapter code to make tests pass
   (green).
5. **Refactor**: clean up without breaking tests. Enforce SOLID/DRY/YAGNI.
6. **Verify gates**: `./gradlew check` MUST pass (tests + coverage).
7. **PR**: description MUST reference the relevant spec.md user stories and confirm
   all five Constitution principles are satisfied.

Each pull request MUST include a brief "Constitution Compliance" section confirming
which principles were exercised or noting any justified exceptions.

## Governance

This Constitution is the highest-authority document for the citamedicos-service project.
All technical decisions, design choices, and code review feedback MUST be measured
against these principles.

**Amendment procedure**:
1. Propose the change in a pull request that modifies this file.
2. The PR description MUST explain the motivation, impact on existing code, and any
   migration plan required.
3. Approval requires explicit sign-off from at least one other project stakeholder.
4. The version number MUST be bumped (MAJOR for principle removal/redefinition,
   MINOR for new principle or section, PATCH for clarifications).

**Versioning policy**:
- MAJOR: Backward-incompatible removal or redefinition of a principle.
- MINOR: New principle or new mandatory section added.
- PATCH: Wording clarification, typo fix, or non-semantic refinement.

**Compliance review**: Constitution compliance MUST be verified at every pull request
review. Reviewers MUST reject PRs that violate any NON-NEGOTIABLE principle without an
explicit, documented exception approved via the amendment procedure.

**Runtime guidance**: Use `CLAUDE.md` and the `.specify/` artifact set for session-level
development guidance. They MUST NOT contradict this Constitution; if they do, the
Constitution takes precedence.

**Version**: 1.0.0 | **Ratified**: 2026-06-27 | **Last Amended**: 2026-06-27
