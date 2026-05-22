# Task 30: ArchUnit Module Boundary Tests

**Status:** pending
**HLD Reference:** §Technical Implementation — Dependency rule, §Testing Strategy — Architecture

## Description

Enforce the modular monolith dependency rules using ArchUnit. The core rule: no module (except `api`) may import classes from another module. All modules may import from `domain`. These rules should fail the build if a developer accidentally creates a cross-module import.

## Acceptance Criteria

- [ ] `ArchUnit` dependency added to `build.gradle` (`com.tngtech.archunit:archunit-junit5`)
- [ ] `ModuleBoundaryTest` asserts:
  - `ingestion` does not import from `processing`, `anomaly`, `forecast`, `insight`
  - `processing` does not import from `ingestion`, `anomaly`, `forecast`, `insight`
  - `anomaly` does not import from `ingestion`, `processing`, `forecast`, `insight`
  - `forecast` does not import from `ingestion`, `processing`, `anomaly`, `insight`
  - `insight` does not import from `ingestion`, `processing`, `anomaly`, `forecast`
  - `domain` does not import from any other module (zero infrastructure deps)
  - `api` may import from all modules (it is the wiring layer)
- [ ] `DomainPurityTest` asserts no `jakarta.*`, `org.springframework.data.*`, or `org.springframework.integration.*` in `io.aether.domain`
- [ ] All tests pass with current codebase

## Dependencies

- **Depends on:** Tasks 04–23 (modules must exist for ArchUnit to analyze)
- **Blocks:** Task 34 (CI must pass all tests)

## Files to Modify/Create

| File | Action | Purpose |
|------|--------|---------|
| `build.gradle` | Modify | Add `testImplementation 'com.tngtech.archunit:archunit-junit5:1.3.0'` |
| `src/test/java/io/aether/arch/ModuleBoundaryTest.java` | Create | Cross-module import rules |
| `src/test/java/io/aether/arch/DomainPurityTest.java` | Create | Domain layer purity rules |

## Implementation Hints

- **ArchUnit rule:**
  ```java
  @AnalyzeClasses(packages = "io.aether")
  class ModuleBoundaryTest {
      @ArchTest
      static final ArchRule ingestion_does_not_depend_on_processing =
          noClasses().that().resideInAPackage("..ingestion..")
              .should().dependOnClassesThat().resideInAPackage("..processing..");

      @ArchTest
      static final ArchRule domain_is_pure =
          noClasses().that().resideInAPackage("..domain..")
              .should().dependOnClassesThat().resideInAnyPackage(
                  "jakarta..", "org.springframework.data..", "org.springframework.integration..");
  }
  ```
- **Key consideration:** `api` is explicitly allowed to depend on all modules. Write a positive rule: `classes in api may depend on any io.aether.* package`. This is cleaner than a whitelist of exceptions.

---

## Revision History

| Date       | Changes             |
|------------|---------------------|
| 2026-05-22 | Initial task created |
