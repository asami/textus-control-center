# Hygiene Follow-up — 2026-08-28

## HYG-CONTROL-CENTER-ABI-BASELINE-MISSING

- Status: `OPEN`
- Discovered: 2026-08-28, `cncf-validated-commit` focused review
- Repository: `textus-control-center`
- Location: `src/main/car/abi-manifest.json`
- Evidence: strict CNCF CAR lint completed successfully but reported `abi.baseline.missing`; the generated `0.1.1-SNAPSHOT` CAR identity and current manifest were otherwise accepted.
- Category: release-maintenance evidence
- Risk: compatibility with the previously released CAR is not compared against an explicit ABI baseline during lint.
- Priority: medium
- Outside current boundary: the current commit only advances the development coordinate and aligns existing descriptors; selecting or creating a release ABI baseline is a separate release-maintenance decision.
- Proposed task: establish the authoritative released-CAR ABI baseline input and rerun strict CAR lint before the next public release.

## HYG-CONTROL-CENTER-COMPONENT-FACTORY-SIZE

- Status: `CLOSED`
- Resolution: Moved the major service factories into `SubsystemInventoryServiceFactoryImpl.scala`, `CarCatalogServiceFactoryImpl.scala`, `OperationalManagementServiceFactoryImpl.scala`, `LauncherEvidenceServiceFactoryImpl.scala`, and `LifecycleControlServiceFactoryImpl.scala`, preserving their public FQNs and ComponentFactory service order. Required validation boundary: focused ComponentFactory/SubsystemInventory specs, then `test`, `cozyBuildCar`, and strict cncf-car-lint.
- Discovered: 2026-08-28, `cncf-validated-commit` preparation
- Repository: `textus-control-center`
- Location: `src/main/scala/org/simplemodeling/textus/controlcenter/impl/ComponentFactory.scala`
- Evidence: the target source is 1,320 lines, exceeding the 1,000-line source-size debt threshold.
- Category: source responsibility and reviewability
- Risk: catalog, inventory, lifecycle, and projection responsibilities are costly to review as one physical source.
- Priority: medium
- Outside current boundary: a safe split crosses generated component factory integration and several existing service implementations, so it is not a local mechanical extraction.
- Proposed task: split service-factory responsibilities under a dedicated compatibility-preserving maintenance task without changing generated ownership or public component contracts.

## HYG-CONTROL-CENTER-TEST-SCRIPT-PLACEMENT

- Status: `OPEN`
- Discovered: 2026-08-29, `cncf-validated-commit` focused review
- Repository: `textus-control-center`
- Location: `scripts/check-standalone-bootstrap.sh`
- Evidence: the script is a test-only standalone-bootstrap acceptance check but resides in the operational `scripts/` root; it is intentionally invoked through `bash` and is not installed as an executable operation script.
- Category: repository organization
- Risk: operators can mistake a test harness for an operational command, and future maintenance can apply the wrong script contract.
- Priority: low
- Outside current boundary: this commit changes the established launcher timeout and validates its bootstrap contract; moving the test harness is unrelated structural cleanup.
- Proposed task: move the acceptance check under `scripts/test/` and update its callers without changing the tested bootstrap behavior.
