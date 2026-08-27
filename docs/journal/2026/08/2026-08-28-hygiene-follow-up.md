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

- Status: `OPEN`
- Discovered: 2026-08-28, `cncf-validated-commit` preparation
- Repository: `textus-control-center`
- Location: `src/main/scala/org/simplemodeling/textus/controlcenter/impl/ComponentFactory.scala`
- Evidence: the target source is 1,320 lines, exceeding the 1,000-line source-size debt threshold.
- Category: source responsibility and reviewability
- Risk: catalog, inventory, lifecycle, and projection responsibilities are costly to review as one physical source.
- Priority: medium
- Outside current boundary: a safe split crosses generated component factory integration and several existing service implementations, so it is not a local mechanical extraction.
- Proposed task: split service-factory responsibilities under a dedicated compatibility-preserving maintenance task without changing generated ownership or public component contracts.
