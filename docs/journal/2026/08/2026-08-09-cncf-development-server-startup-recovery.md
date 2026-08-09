# CNCF Development Server Startup Recovery

date=2026-08-09
status=recovered
target=/Users/asami/src/dev2026/textus-control-center
command=`cncf . server`

## Goal

Start Textus Control Center from its development directory with `cncf . server`,
keep the process alive, bind the configured standalone port, and load the
canonical Control Center and Supervisor components through the development CAR
runtime.

This journal is an executable development handoff. It records the failures
crossed and the verified fresh-local procedure for starting Control Center.
It is not Phase-completion or release evidence.

## Original Symptom

From `/Users/asami/src/dev2026/textus-control-center`:

```console
$ cncf . server
warning: Textus Control Center registration register-subsystem connection to http://127.0.0.1:18000/rest/v1/textus-control-center/subsystem-inventory/register-subsystem failed; continuing server startup.
[Enum(Warn):warn] status=404 statusText=Not Found detailCode=101099020100 operation.not-found-operation:--repository-component-dev-dir=/Users/asami/src/dev2026/textus-control-center
```

The registration warning is intentionally non-blocking. The defect was that a
launcher repository option escaped into application operation dispatch.

## Failure Sequence and Current Interpretation

The investigation advanced through these boundaries:

1. The repository development-directory option reached operation dispatch.
2. Runtime resolution then exposed a `ConfigurationValueCodec` linkage failure.
3. After resolving the current CNCF `0.5.2-SNAPSHOT` runtime, the development
   runtime manifest was missing; `sbt cozyPrepareRuntime` generated it.
4. Development admission rejected the generated schema-3 descriptor as though
   it required the legacy root `name` field.
5. Assembly loading rejected the unqualified `textus-control-center` identity.
6. Canonical `namespace` / `id` assembly records reached a legacy-only
   `component/componentName/name` decoder.
7. After canonical decoding, runtime binding reached the next real dependency
   boundary: `org.simplemodeling.textus.Supervisor` has no exact Core/artifact
   identity match.
8. A stale user-wide warehouse made `cozyPublishLocalCar` fail with
   `component.release-coordinate.mismatch source=index expected=namespace actual=missing`.
9. After preserving that warehouse and publishing from a clean one, the
   canonical Supervisor CAR was written successfully, but CNCF still searched
   the old flat CAR layout.
10. After canonical repository-path resolution was added, packaged admission
    exposed two producer/consumer contract mismatches: runtime `car.name` is a
    Maven artifact projection, and the current canonical ABI format is v2.
11. After strict canonical admission was aligned and its focused tests passed,
    `cncf . server` still reached the generic exact Core/artifact mismatch for
    `org.simplemodeling.textus.Supervisor` before binding port 18000.
12. A direct probe proved that the fresh Supervisor CAR creates one component
    whose name, Core identity, and artifact identity are all exactly
    `org.simplemodeling.textus.Supervisor:0.1.0-SNAPSHOT`.
13. The remaining mismatch was repository projection: when an explicit
    Control Center development repository was active, GenericSubsystemFactory
    dropped the default local CAR search repository. Runtime projection now
    retains the explicit development repository first and appends the default
    local CAR repository unless `--no-default-components` is present.
14. With Supervisor discovery restored, startup correctly enforced the next
    contract: the standalone assembly needed
    `textus.subsystem.user-mode: standalone`. Adding that canonical setting
    allowed the server to bind port 18000.

The startup incident is recovered without a legacy identity, ABI-v1, index-v1,
flat-path, or a packaged fallback for the explicitly selected Control Center
development component. Default packaged/local CAR repositories remain
searchable for other assembly dependencies such as Supervisor.

## Phase 56 Build Versus Local Publication

Phase 56 rebuilt and tested representative CARs with `cozyBuildCar`, but did
not publish those rebuilt CARs into the user-wide `/Users/asami/.cncf/local`
warehouse. Its recorded Scraper CAR publications were deliberately confined to
target-local test warehouses for ArtScene and the Semantic Integration Engine.
The Phase 56 ledger explicitly records that the user-wide local warehouse was
not used.

The former warehouse was moved, without deletion, to
`/Users/asami/.cncf/local.backup-20260809T082416+0900`. A new empty
`/Users/asami/.cncf/local` was created. Do not merge the backup into the active
warehouse; it is retained only for later investigation.

A fresh Supervisor build now publishes the canonical CAR at
`/Users/asami/.cncf/local/repository/car/org/simplemodeling/textus/textus-supervisor/0.1.0-SNAPSHOT/textus-supervisor-0.1.0-SNAPSHOT.car`.
Its root descriptor is schema 3 and its ABI manifest is v2 for
`org.simplemodeling.textus.Supervisor:0.1.0-SNAPSHOT`.

## Applied but Uncommitted Work

### CNCF launcher

Repository: `/Users/asami/src/dev2026/cncf-launcher`

The launcher argument/runtime-resolution repair currently modifies:

- `src/main/scala/cncf/launcher/CncfLauncher.scala`
- `src/main/scala/cncf/launcher/CncfRuntimeResolver.scala`
- `src/main/scala/cncf/launcher/DevSupport.scala`
- `src/main/scala/cncf/launcher/RuntimeCatalog.scala`
- `src/test/scala/cncf/launcher/CncfLauncherSpec.scala`

Focused evidence: invocation `25927-20260808T213904Z`, one suite, 117 tests
passed, exits zero, lock released.

### CNCF framework

Repository: `/Users/asami/src/dev2025/cloud-native-component-framework`

Development runtime admission now reads schema-3 descriptor identity and ABI v2
evidence. Generic subsystem assembly decoding now accepts canonical
`namespace` / `id` / `version` component bindings. Relevant focused evidence:

- `DevelopmentCarRuntimeAdmissionSpec`: invocation
  `36665-20260808T221154Z`, 11/11 passed;
- `GenericSubsystemDescriptorSpec` plus `GenericSubsystemFactorySpec`:
  invocation `50070-20260808T224133Z`, 63/63 passed; and
- framework `publishLocal`: invocation `48714-20260808T223900Z`, published
  `org.goldenport:goldenport-cncf_3:0.5.2-SNAPSHOT`.

The fresh-warehouse investigation additionally changes canonical packaged-CAR
resolution and admission:

- qualified component requests resolve only the canonical
  group/artifact/release CAR path;
- runtime `car.name` is checked against the Maven artifact projection;
- runtime `car.component` is checked against the qualified component identity;
- packaged ABI admission accepts canonical v2 only, without a v1 fallback.

Focused evidence: invocation `90042-20260809T000724Z`, two suites, 80/80 tests
passed. The resulting framework `0.5.2-SNAPSHOT` was published locally by
invocation `90605-20260809T000822Z`.

The final repository-projection repair additionally preserves default local
CAR search when an explicit development repository is present. The focused
scenario passed as invocation `5769-20260809T004117Z` (1/1), and invocation
`6186-20260809T004200Z` published the updated
`org.goldenport:goldenport-cncf_3:0.5.2-SNAPSHOT` locally.

These framework changes are still uncommitted and include transitional legacy
acceptance that is scheduled for Phase 57 compatibility retirement. Do not
expand that compatibility while completing this startup repair.

### Textus Control Center

Repository: `/Users/asami/src/dev2026/textus-control-center`

- `conf/cncf/assembly-standalone.yaml` declares canonical Control Center and
  Supervisor namespace/id/version identities and qualified SPI/config/security
  references, and declares `textus.subsystem.user-mode: standalone`.
- `scripts/check-standalone-assembly.sh` checks schema 3 and canonical assembly
  identities plus the standalone user-mode contract.
- `/bin/bash scripts/check-standalone-assembly.sh` passed with exit 0.
- the final fresh `clean; cozyPrepareRuntime` invocation
  `6439-20260809T004220Z` generated
  `target/cncf.d/runtime-classpath.txt` and
  `target/cncf.d/car-runtime-manifest.json` from the locally published
  framework `0.5.2-SNAPSHOT`.

### Textus Supervisor

Repository: `/Users/asami/src/dev2026/textus-supervisor`

Fresh `clean; cozyPublishLocalCar` invocation
`68036-20260808T232507Z` regenerated the component, built its CAR, and published
the canonical `0.1.0-SNAPSHOT` coordinate into the new warehouse. No source
change was required for this publication.

## Verified Fresh-Local Startup Procedure

Run top-level SBT only through the serialized wrapper and stop at the first
failure.

### 1. Recreate the fresh local prerequisite

Normally reuse the current clean warehouse and published CAR. If it must be
recreated, keep the recorded backup untouched and run in
`/Users/asami/src/dev2026/textus-supervisor`:

```text
/Users/asami/.codex/skills/cncf-sbt-serial-execution/scripts/run-sbt-serial.sh --batch 'clean; cozyPublishLocalCar'
```

Confirm the exact canonical path and schema-3/v2 identity recorded above.

### 2. Refresh Control Center development runtime evidence

In `/Users/asami/src/dev2026/textus-control-center`, run:

```text
/Users/asami/.codex/skills/cncf-sbt-serial-execution/scripts/run-sbt-serial.sh --batch cozyPrepareRuntime
```

Then run:

```text
/bin/bash scripts/check-standalone-assembly.sh
```

Both commands must pass. Confirm the regenerated runtime manifest/classpath
refer to the current `0.5.2-SNAPSHOT` runtime and canonical component evidence.

### 3. Start the server

Confirm port `127.0.0.1:18000` is free, then run from the Control Center root:

```text
cncf . server
```

The registration connection warning may appear before the server is ready and
is not by itself a failure. Success requires all of the following:

- no repository option reaches operation dispatch;
- no `ConfigurationValueCodec`, descriptor-name, unqualified-ID,
  component/name, or exact Supervisor Core/artifact error appears;
- the process remains alive and binds port 18000; and
- the existing Control Center readiness/smoke path responds successfully.

Terminate only the process started by this verification after evidence is
captured.

Verified session `75436` emitted `event=info scope=Subsystem name=cncf started`,
bound Ember to `[::]:18000`, and reported `HTTP server started on port 18000.`
The verification-owned process was then stopped with Ctrl-C and port 18000 was
confirmed free.

### 4. Close or continue development

Run only focused tests directly associated with the final changed producers and
the standalone startup boundary. Review the launcher, framework, Supervisor,
and Control Center deltas once as one integration repair, then commit coherent
repository-local changes. Broad Phase 57 validation remains its own gate.

## Constraints

- Keep CNCF `0.5.2-SNAPSHOT`, Cozy `0.3.4-SNAPSHOT`, sbt-cozy
  `0.1.20-SNAPSHOT`, Supervisor `0.1.0-SNAPSHOT`, and Control Center
  `0.1.0-SNAPSHOT` unless an explicit version instruction is given.
- Do not recover an explicitly selected component development directory by
  falling back to a packaged copy of that same selected component. Default
  packaged/local CAR repositories remain searchable for other assembly
  dependencies such as Supervisor.
- Do not restore Phase 56 legacy identity/index behavior to make the local
  warehouse readable.
- Do not treat the non-blocking self-registration warning as server readiness.
- Preserve the canonical local CAR layout and standalone user-mode setting in
  later changes.

## Current Status

`RECOVERED / SERVER_BOUND_PORT_18000`

The exact `cncf . server` command has reached readiness. The remaining
self-registration 404 warning is non-blocking startup behavior and is a
separate integration concern, not part of this recovered launch failure.

## Web Surface Follow-up

The first successful server start exposed two additional Phase 56 identity
migration omissions in the Control Center Web surface:

1. `src/main/web-inf/web.yaml` targeted the legacy component name
   `TextusControlCenter`. The alias now targets the canonical
   `org.simplemodeling.textus.ControlCenter` identity.
2. The Operational Components, Subsystem Inventory, and CAR Catalog browser
   assets called `/rest/v1/textus-control-center/...`. Their endpoint constants
   now use the canonical REST slug
   `/rest/v1/org-simplemodeling-textus-control-center/...`.

Live evidence established the boundary: the legacy Operational Management URL
returned 404, while the canonical URL returned 200 with an empty valid result.
The running server immediately served the corrected JavaScript, so a browser
hard reload is sufficient for the REST fix. A server restart is required to
reload the corrected Web alias descriptor.

## Non-running Operational Component Candidates

Operational Components must be derived from the configured CAR catalog, not
only from currently running subsystem instances. A stopped or never-started
development component is therefore still a valid operating candidate.

The first live check returned empty catalog and operational lists because the
catalog refresh step was omitted. The Web panel's required sequence is:

1. call `car-catalog/refresh-car-catalog`;
2. list managed CARs; and
3. list operational components.

The investigation also found a launcher identity boundary left behind by the
Phase 56 canonical migration. The implicit Control Center configuration check
recognized only legacy `project.name: textus-control-center`. It now recognizes
the canonical pair `project.namespace: org.simplemodeling.textus` and
`project.id: ControlCenter`, while retaining the legacy check only at this
launcher-private detection boundary.

Focused launcher validation `24874-20260809T012958Z` passed 117 of 117 tests.
After the fixture was aligned with the real quoted canonical `project.yaml` and
the exact `. server` form, validation `27873-20260809T013602Z` also passed 117
of 117 tests. Both invocations exited successfully and released the shared SBT
lock.

The final live verification used the implicit configuration path, with no
explicit `--cncf-config`. On port 18001 the refresh returned HTTP 200 with
`adapter_state=configured` and five refreshed sources. The managed CAR list
contained five `not-running` candidates. Four development candidates
(`textus-corpus`, `textus-experiment`, `textus-georesolver`, and
`textus-sanpomap`) were projected into Operational Components as
`auto-managed`; the public `textus-user-notification` candidate remained in the
catalog. The verification-owned server was stopped and port 18001 was released;
the user-owned server on port 18000 was not touched.

### Canonical discovery and Launcher evidence follow-up

A later browser check exposed two additional defects behind an incomplete
Operational Components view.

First, the standalone development catalog provider still parsed legacy
`project.name` and `component.name` text. Canonical CAR projects such as
ArtScene declare `project.namespace`, `project.id`, and
`project.component.version`, so they were silently omitted. The provider now
parses `project.yaml` structurally, admits canonical CAR identity only, and
uses the shared component identity projection for the artifact ID. It does not
add a legacy fallback. The focused provider and Launcher evidence validation
passed in invocation `55076-20260809T021433Z` with two suites and four tests.

Second, the Control Center evidence subprocess inherited two launcher-private
variables from the development launcher. `CNCF_LAUNCHER_DEV_DELEGATED` disabled
development delegation, and `CNCF_LAUNCHER_ARGS_FILE` caused the evidence
command arguments to be replaced with the parent `. server` arguments. The
evidence client now removes exactly those two private variables before starting
the fixed `cncf launcher evidence` command. Its default timeout was raised from
five to fifteen seconds, within the existing thirty-second maximum, because
the delegated JVM command exceeded five seconds under the live server even
though a standalone warm invocation took approximately three seconds. The
final focused validation passed in invocation `60245-20260809T022509Z` with
three of three tests.

The final implicit-config live verification on port 18001 produced fifteen
refreshed catalog sources and fourteen managed CARs. ArtScene appeared as a
development source at `0.1.2-SNAPSHOT`, with runtime state `not-running`, and
was retained by Operational Components as `auto-managed`. Launcher evidence
refresh returned HTTP 200 with 512 retained entries, including multiple
ArtScene `historical-stopped` records. The verification-owned server was
stopped, port 18001 was released, and the user-owned port 18000 process remained
untouched.
