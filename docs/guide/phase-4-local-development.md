# Phase 4 local source development

This guide describes the opt-in build setup for developing Control Center,
Textus Supervisor, CNCF, goldenport-core, sbt-cozy, Cozy, and SimpleModeler
together. It is non-normative development guidance.
Real placement and runtime validation follow in later work; these commands do
not establish Phase or Step closure.

## GitHub synchronization timing

As decided on October 16, 2026, synchronize the locally operated Cozy and
CNCF repositories with GitHub after Control Center Phase 4 is complete.
Keep developing and validating the dedicated local worktrees during Phase 4;
local Step commits continue under the existing acceptance process.
GitHub synchronization is not a prerequisite for resuming Phase 4. Defer
fetch/pull, integration of GitHub updates, and push for these repositories
to the post-Phase-4 synchronization work. Preserve the current branches and
unfinished changes until that work reconciles them with GitHub.

## Select dedicated source checkouts

Keep dedicated development checkouts and branches for each repository under
one developer's or task's ownership. The three producer roots must be pairwise
canonically disjoint: equal roots or nesting between CNCF, sbt-cozy, and Cozy
are rejected. Ordinary external source roots must be disjoint from each
consumer, and a consumer cannot be inside a source root or select itself.
The only project-contained source placement is a canonical direct child of
that consumer's `.codex-workflow/checkouts/`, with an ASCII leaf matching
`[A-Za-z0-9_][A-Za-z0-9_.-]*`. Both the normalized absolute selected path and
its canonical path must have the normalized absolute lexical checkout parent;
canonicalization cannot admit a symlinked checkout parent or arbitrary nested
path through this exception. For example,
`/absolute/path/to/consumer/.codex-workflow/checkouts/goldenport-cncf-source`
is an eligible placement. There is no general nested-source exception.
Select the intended checkout explicitly; the builds do not search for a sibling
repository or switch another developer's main checkout or branch. Commit no
machine-specific checkout paths.

| Environment variable | Consumer | Selected source |
| --- | --- | --- |
| `TEXTUS_CNCF_SOURCE_ROOT` | Control Center and Textus Supervisor, built separately | CNCF checkout with `build.sbt` and `src/main/scala/org/goldenport/cncf/spi/supervisor/Supervisor.scala` |
| `TEXTUS_GOLDENPORT_CORE_CNCF_SOURCE_ROOT` | CNCF, built standalone or as the selected source dependency of a consumer | Core checkout with `build.sbt` and `src/main/scala/org/goldenport/http/HttpResponse.scala` |
| `TEXTUS_SBT_COZY_CNCF_SOURCE_ROOT` | Each consumer's SBT meta-build | sbt-cozy checkout with `build.sbt` and `src/main/scala/org/goldenport/cozy/CozyPlugin.scala` |
| `TEXTUS_COZY_CNCF_SOURCE_ROOT` | Each consumer's generation delegate | Cozy checkout with `build.sbt` and `src/main/scala/cozy/Cozy.scala` |
| `TEXTUS_SIMPLEMODELER_CNCF_SOURCE_ROOT` | Cozy backend preparation | SimpleModeler checkout with `build.sbt` and `src/main/scala/org/simplemodeling/SimpleModeler/SimpleModeler.scala` |

For human CLI development, replace these placeholders with the absolute paths
to the dedicated source checkouts:

```sh
export TEXTUS_CNCF_SOURCE_ROOT="/absolute/path/to/dedicated/goldenport-cncf"
export TEXTUS_SBT_COZY_CNCF_SOURCE_ROOT="/absolute/path/to/dedicated/sbt-cozy"
export TEXTUS_COZY_CNCF_SOURCE_ROOT="/absolute/path/to/dedicated/cozy"
```

Export only these three consumer selectors. The SimpleModeler selector is
producer-only: select it with a command-local environment prefix on the Cozy
preparation command below. Select core with a command-local
`TEXTUS_GOLDENPORT_CORE_CNCF_SOURCE_ROOT` prefix on the standalone CNCF or
source-consumer command; the selected CNCF build consumes it.

The builds independently read and canonicalize each selected path before
constructing the CNCF or plugin SBT `RootProject`, or selecting the Cozy
delegate directory. Relative paths, empty or whitespace-only values,
canonicalization failures, missing build files or exact source markers, self
paths, and source/consumer overlap outside the narrow placement above are
rejected with a descriptive configuration error before compilation.
Canonicalization also detects overlap through symlinks. An explicitly invalid
selection never falls back to an artifact dependency. Unset a variable to
disable its selection; assigning an empty string does not disable source mode.
Restart SBT after changing the environment.

Selecting `TEXTUS_CNCF_SOURCE_ROOT` requires both
`TEXTUS_SBT_COZY_CNCF_SOURCE_ROOT` and `TEXTUS_COZY_CNCF_SOURCE_ROOT`.
A Cozy source selection also requires both CNCF and plugin selections.
The meta-build rejects every partial set involving CNCF or Cozy at load time,
so source generation cannot silently use an older published plugin or runtime.
That consumer meta-build contract still validates exactly its original three
selectors. The SimpleModeler selector is consumed by Cozy backend preparation.
The core selector is consumed by the nested CNCF build. Neither adds a new
partial-set requirement to either consumer meta-build.
Each meta-build
depends on the selected plugin `RootProject` through `compile->compile` and
omits the published sbt-cozy module, exposing the source plugin classes without
publication or duplicate plugin classes.

| CNCF selector | Plugin selector | Cozy selector | Mode |
| --- | --- | --- | --- |
| Set | Set | Set | Complete CNCF source mode |
| Unset | Set | Unset | Plugin-source-only mode |
| Unset | Unset | Unset | Published default mode |
| Any other combination | | | Configuration error |

Plugin-source-only development is supported: set
`TEXTUS_SBT_COZY_CNCF_SOURCE_ROOT` and unset both `TEXTUS_CNCF_SOURCE_ROOT` and
`TEXTUS_COZY_CNCF_SOURCE_ROOT` to develop the plugin while retaining the
consumer's default CNCF and Cozy metadata.
With all five selectors unset, both existing `project.yaml` dependency graphs remain
in effect, including the direct `goldenport-cncf` dependency at `0.5.2` and Cozy
`0.3.2.2`. The published plugin defaults to `0.1.16`; its existing version
override precedence is the `sbt.cozy.version` system property, then
`SBT_COZY_VERSION`, then that default.
Setting `TEXTUS_CNCF_SOURCE_ROOT` replaces only that unscoped direct compile
dependency with the selected CNCF source project through `compile->compile`.
All other dependencies and test scopes remain. The original three selectors
belong to the consumer meta-build; the additional selectors belong to Cozy
backend preparation and the selected CNCF build respectively.
There are no environment aliases or implicit alternatives.

Build Textus Supervisor in its own source checkout and Control Center in its
own checkout, selecting the same canonical CNCF root for each build. Control
Center consumes the standard Supervisor SPI and has no direct Supervisor
provider source or implementation classpath dependency. The real Supervisor CAR
and its standard SPI provider are placed in the later assembly. The current
development selection uses CNCF `0.5.3-SNAPSHOT`, Cozy `0.3.3-SNAPSHOT`, and
sbt-cozy `0.1.18-SNAPSHOT`. Supervisor and Control Center remain
`0.1.1-SNAPSHOT`.

## Select core inside the CNCF source build

Use a dedicated core checkout at the unchanged `org.goldenport:goldenport-core`
`0.4.3-SNAPSHOT` coordinate with Scala `3.3.8`. The core selector must name a
canonical disjoint root or a direct lexical and canonical child of the owning
CNCF checkout's `.codex-workflow/checkouts/`, under the same legal-leaf rule
above. Both exact source markers must remain inside that source even through
symlinks. A blank, relative, overlapping, escaping, or incomplete explicit
selection fails without an artifact fallback.

CNCF exposes `cncfCoreDevelopmentDirectory` and
`validateCncfCoreDevelopment`. The latter validates the actual loaded core
`organization`, `moduleName`, `version`, and `scalaVersion` against
`org.goldenport`, `goldenport-core`, `0.4.3-SNAPSHOT`, and `3.3.8` during build
settings initialization. Editing a version string in source text does not
substitute for that loaded-project validation.

The selected core is an ordinary source `RootProject` dependency of CNCF with
`compile->compile;runtime->runtime`. CNCF omits its direct core binary edge and
excludes transitive `org.goldenport:goldenport-core_3` only in this mode. The
actual classpath must contain the selected core target classes, exactly one
`HttpStatus` class resource, and no competing core artifact JAR. The logical
core ModuleID remains input to runtime descriptor generation, runtime catalog
generation, and catalog export, so `baseProvided` continues to include
`org.goldenport:goldenport-core_3`. No source or JAR copying and no publication
are required. With the core selector unset, CNCF retains its original direct
`0.4.3-SNAPSHOT` artifact, exclusions, versions, and runtime graph. The normative
contracts are CNCF's `docs/spec/core-development-source.md` and core's
`docs/spec/http-no-content.md` in their respective source checkouts.

Control Center and Supervisor continue to depend on the selected CNCF project.
Their meta-builds do not interpret the core selector or add a separate direct
core source dependency. Their nested CNCF build uses the same explicit core
selection. Cozy preparation and SimpleModeler provenance retain their existing
producer ownership.

## Consumer generation and metadata

In CNCF source mode, each consumer projects its loaded `CozyProjectConfig`
immutably in memory. It requires exactly one compile coordinate with the exact
`org.goldenport::goldenport-cncf:version` or
`org.goldenport:goldenport-cncf:version` shape, replaces only that coordinate's
version with the actual selected source project's `version`, and sets
`build.cozyVersion` to the settled `0.3.3-SNAPSHOT` generator pair. All other
metadata, test coordinates, component/CAR identity, and runtime policy remain.
The consumers' `project.yaml` files and release coordinates are not rewritten.

In CNCF source mode, `cozyDelegateProjectDir` is `Some` of the canonical selected
Cozy directory, and `cozyDelegateCommand` is
`Seq("cozy", "--runtime", "0.3.3-SNAPSHOT")`. The existing plugin launcher
branch appends `--runtime-dev-dir /absolute/path/to/dedicated/cozy` and the
bridge request arguments. The effective command therefore selects the Cozy
development directory together with the exact `--runtime 0.3.3-SNAPSHOT` pin,
matching the generator pair projected into source metadata.
Plain `cozy` would let the on-disk default metadata select `0.3.2.2`; changing
the in-memory metadata alone does not select the executing runtime. The source
command therefore selects `0.3.3-SNAPSHOT` while preserving the strict actual
executing-version and provenance checks. With CNCF source mode disabled, the
delegate project directory remains `None` and the command remains plain `cozy`.
In CNCF source mode, leave `SBT_COZY_LEGACY_DELEGATE_ROUTE` unset or empty,
or set it to `launcher`; any other nonempty value fails at build load.
The `cs` and `sbt` routes are rejected before runtime resolution or nested SBT.

## Prepare the Cozy development runtime

Use a dedicated SimpleModeler source checkout on `1.1.26-SNAPSHOT` with Scala
`2.12.18`, selected by `TEXTUS_SIMPLEMODELER_CNCF_SOURCE_ROOT`. Cozy admits
disjoint canonical roots or the same narrow direct lexical and canonical
checkout-child placement described above. A lexical contained path cannot
escape through a symlinked checkout namespace or source directory; required
marker links must remain within the source. Invalid explicit selection fails
at graph construction without published-backend fallback.

Cozy loads the selected SimpleModeler `RootProject` and validates its actual
`organization`, `moduleName`, `version`, and `scalaVersion` during settings
initialization. They must be `org.simplemodeling`, `simplemodeler`, the owning
Cozy `simplemodelerVersion`, and the exact Cozy Scala version. It omits only the
direct published SimpleModeler dependency and excludes transitive
`simplemodeler_2.12`, so the running generator has one selected source owner.
This directory is build input; it changes neither generation target nor Cozy
generator coordinate and supplies no compatibility proof. When the SimpleModeler
selector is unset, Cozy retains its published backend and existing
`simplemodeler.version` > `SIMPLEMODELER_VERSION` > `1.1.26-SNAPSHOT`
version precedence.

Before executing either consumer, prepare the selected Cozy checkout's
`Runtime / fullClasspath` and version export. From that Cozy checkout, the
supported producer command for human CLI development is:

```sh
cd /absolute/path/to/dedicated/cozy
TEXTUS_SIMPLEMODELER_CNCF_SOURCE_ROOT="/absolute/path/to/dedicated/simple-modeler-source" \
sbt --batch cozyExportRuntimeClasspath
```

For agents, the parent routes this producer command through the registered
`cncf_command_runner` and shared SBT serialization wrapper with its admitted
Cozy root and output scope. It must finish before a consumer is dispatched.
With the SimpleModeler selector set, its typed source dependency also names the
canonical selected SimpleModeler root, `TEXTUS_SIMPLEMODELER_CNCF_SOURCE_ROOT`,
and that checkout's `target`, `project/target`, and `project/project/target`
output roots. The producer's classpath and export receipts must contain the
selected SimpleModeler target classes and no published backend JAR. The
generator class-origin specification requires exactly one class resource
owned by the selected source target.
The task writes `target/cozy.d/runtime-classpath.txt` from
`Runtime / fullClasspath` and `target/cozy.d/runtime-version.txt` from the
selected Cozy build's version, which must be `0.3.3-SNAPSHOT` for this pair.
Regenerate the export after changing the producer build or compiled runtime.

The launcher consumes the prepared classpath and executes Java outside the
Cozy build. The consumer adds no Cozy `RootProject` dependency and starts no
Cozy SBT bootstrap. A missing or stale export, unavailable classpath entry, or
contradictory exported/actual runtime version must fail; repair the selected
producer export before retrying. It must not fall back to a published runtime,
Coursier, nested SBT, or another checkout.

The descriptor input has a causal SBT task dependency on the selected CNCF
project's `generateCncfRuntimeDescriptor` task and consumes its returned file.
Generation therefore prepares the descriptor from the selected source build
rather than constructing a path to a stale resource. Exact version, pin,
merge, and provenance checks remain active; metadata projection does not bypass
those checks.

No publishing is required. Normal SBT project dependencies compile the source
checkouts directly; do not copy source trees or jars, add unmanaged sources or
JARs, or use local or remote publication for this setup. Normal dependency
caches and resolvers continue to be used; global plugin changes and cache
workarounds are not part of this setup.

After Cozy preparation, the standalone CNCF generation/provenance prefix uses
its existing Cozy-directory selector and the prepared backend export. To also
develop core from source, prefix that command with the core selector. From
the dedicated CNCF source checkout, the human CLI command is:

```sh
cd /absolute/path/to/dedicated/goldenport-cncf
unset TEXTUS_SIMPLEMODELER_CNCF_SOURCE_ROOT
TEXTUS_COZY_CNCF_SOURCE_ROOT="/absolute/path/to/dedicated/cozy" \
TEXTUS_GOLDENPORT_CORE_CNCF_SOURCE_ROOT="/absolute/path/to/dedicated/goldenport-core-source" \
sbt --batch 'show cncfCozyDevelopmentDirectory' \
  'show cncfCoreDevelopmentDirectory' 'show validateCncfCoreDevelopment' \
  'show Runtime / fullClasspath' 'show libraryDependencies' 'show excludeDependencies' \
  exportTextusRuntimeCatalog \
  verifyInformationCmlGenerationDeterminism \
  'testOnly org.goldenport.cncf.phase51.build.CncfCoreDevelopmentRuntimeSpec org.goldenport.cncf.phase51.build.CncfCozyDevelopmentRuntimeSpec org.goldenport.cncf.phase51.build.Phase51Cv03BuildIntegrationSpec org.goldenport.cncf.phase51.build.Phase51Cv05BuildProvenanceSpec'
```

The actual generated Scala must compile and the provenance specifications must
pass; generation alone does not establish compatibility. The independent
Supervisor and Control Center builds below then consume the same exported
Cozy runtime. Neither consumer adds a SimpleModeler source-project dependency
or starts a producer bootstrap.

For agent execution, standalone CNCF owns the selected Cozy directory binding,
`TEXTUS_COZY_CNCF_SOURCE_ROOT`, and that checkout's `target` output binding. If
the core selector is selected, the typed command also carries an explicit
`SourceBuildDependency` for the canonical core root,
`TEXTUS_GOLDENPORT_CORE_CNCF_SOURCE_ROOT`, and that checkout's `target`,
`project/target`, and `project/project/target` output roots. With core selection
absent, omit that binding. It reads the prepared Cozy export after successful
Cozy producer compilation and specifications. SimpleModeler remains transitive read-only
provenance through the preceding producer receipt; this command consumes no
SimpleModeler selector, adds no SimpleModeler source dependency, grants no
SimpleModeler output write authority, and starts no nested bootstrap.

## Focused build and specification commands

The shell examples below are human CLI development commands. Agent execution
requires three admitted typed `SourceBuildDependency` bindings: the canonical
CNCF root and `TEXTUS_CNCF_SOURCE_ROOT`, the canonical sbt-cozy root and
`TEXTUS_SBT_COZY_CNCF_SOURCE_ROOT`, and the canonical Cozy root and
`TEXTUS_COZY_CNCF_SOURCE_ROOT`. CNCF and plugin bindings each name exactly their
own checkout's `target`, `project/target`, and `project/project/target` output
roots. The Cozy binding names its `target` output and consumes the prepared
export; it does not start Cozy SBT inside the consumer. The command binding
retains these three source bindings for Supervisor and Control Center. If
`TEXTUS_GOLDENPORT_CORE_CNCF_SOURCE_ROOT` is selected, it additionally carries
the explicit core `SourceBuildDependency`, its canonical root and exact
selector, and that core checkout's `target`, `project/target`, and
`project/project/target` output roots. This conditional fourth binding covers
the source project loaded by CNCF; it does not change the three-selector
consumer meta-build contract. When core selection is absent, omit the core
binding and output authority.
The preceding Cozy producer alone owns the SimpleModeler source dependency,
its selector, and its three target outputs above. A downstream receipt may
reference that producer receipt for transitive SimpleModeler provenance; it
creates no downstream consumed selector, source dependency, output write
authority, or nested bootstrap. The command binding
must also name the consumer and its admitted
target outputs. After inspecting the actual wiring, the parent must construct
the actual typed command authority and complete fresh permission intake before
each dispatch to the registered `cncf_command_runner` with the shared SBT
serialization wrapper. Commands run sequentially and require terminal
lock-release evidence. An environment export or prefix alone is not an agent
execution binding; agents must not invoke SBT directly.

From the dedicated Textus Supervisor checkout, with the three consumer
selectors set and core selected for this command:

```sh
unset TEXTUS_SIMPLEMODELER_CNCF_SOURCE_ROOT
TEXTUS_GOLDENPORT_CORE_CNCF_SOURCE_ROOT="/absolute/path/to/dedicated/goldenport-core-source" \
sbt --batch 'set cozyWebDescriptorSync := false' \
  'show cozyProjectMetadata' 'show cozyDelegateProjectDir' \
  'show cozyDelegateCommand' 'show cozyDelegateCoursierVersion' \
  'show libraryDependencies' 'show Compile / fullClasspath' \
  'testOnly org.simplemodeling.textus.supervisor.impl.TextusSupervisorProviderSpec'
```

From the Control Center checkout, with the same three canonical source roots
set and the same core selection:

```sh
unset TEXTUS_SIMPLEMODELER_CNCF_SOURCE_ROOT
TEXTUS_GOLDENPORT_CORE_CNCF_SOURCE_ROOT="/absolute/path/to/dedicated/goldenport-core-source" \
sbt --batch 'set cozyWebDescriptorSync := false' \
  'show cozyProjectMetadata' 'show cozyDelegateProjectDir' \
  'show cozyDelegateCommand' 'show cozyDelegateCoursierVersion' \
  'show libraryDependencies' 'show Compile / fullClasspath' \
  'testOnly org.simplemodeling.textus.controlcenter.supervisor.EmbeddedTextusSupervisorSpec'
```

The focused `cozyWebDescriptorSync := false` setting suppresses only unrelated
tracked Web descriptor synchronization. Generation, exact versions, provenance,
and component behavior checks remain active; ordinary default Web behavior and
full suites remain required at Phase release.

Both metadata receipts should show CNCF `0.5.3-SNAPSHOT` from the selected
source project and Cozy `0.3.3-SNAPSHOT`, with both consumer identities still
`0.1.1-SNAPSHOT`. Both delegate-project receipts should show `Some` of the
selected canonical Cozy directory. Both delegate-command receipts should show
`Seq(cozy, --runtime, 0.3.3-SNAPSHOT)`, and both delegate-version receipts should
show `Some(0.3.3-SNAPSHOT)`. Generation must execute that exact runtime with a
matching actual executing version and valid provenance. Generation receipts
must confirm descriptor preparation and the coherent source version pair.
Both classpath receipts should identify the selected CNCF classes and contain
no duplicate released CNCF jar. With core selected, both classpaths must also
identify the selected core target classes with no competing core JAR; the
generated CNCF descriptor and exported catalog must retain the logical
`org.goldenport:goldenport-core_3` identity. The standalone core-origin
specification exercises the actual public 204 mapping. The separate Supervisor build compiles the real
provider source; Control Center's classpath remains provider-neutral. The
existing specifications cover the provider and adapter behavior; the build
observations establish source selection.

For the default graph, disable all five selectors and inspect each consumer separately:

```sh
unset TEXTUS_CNCF_SOURCE_ROOT
unset TEXTUS_SBT_COZY_CNCF_SOURCE_ROOT
unset TEXTUS_COZY_CNCF_SOURCE_ROOT
unset TEXTUS_SIMPLEMODELER_CNCF_SOURCE_ROOT
unset TEXTUS_GOLDENPORT_CORE_CNCF_SOURCE_ROOT
sbt --batch 'show cozyProjectMetadata' 'show cozyDelegateProjectDir' \
  'show cozyDelegateCommand' 'show cozyDelegateCoursierVersion' \
  'show libraryDependencies'
```

Both consumers should retain the direct CNCF `0.5.2` dependency and Cozy
`0.3.2.2` metadata through published plugin `0.1.16`. Both delegate-project
receipts should show `None`. Both delegate-command
receipts should show plain `Seq(cozy)`, and both delegate-version receipts should
show `Some(0.3.2.2)`. Agent default
graph commands carry no source build dependency. Build and classpath receipts
remain required before accepting this development setup. Actual Supervisor CAR
placement, embedded and external assembly, startup, and runtime behavior are
validated later; these focused builds do not establish that placement acceptance.
The original Phase lifecycle, panel, idempotency, and restart acceptance
obligations also remain.
