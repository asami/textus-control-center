<!-- cncf-phase-finding:HYG-CC4-S0-SBTCOZY-LEGACY-NAMING-01:begin -->
### HYG-CC4-S0-SBTCOZY-LEGACY-NAMING-01

- Status: OPEN
- Discovery: REVIEW / CC4-S0 / 2026-10-05
- Source review: phase4-CC4-S0-step-review-r1
- Category/priority: hygiene / P3
- Repository: sbt-cozy
- Evidence: Unchanged declarations in src/main/scala/org/goldenport/cozy/CozyPlugin.scala include currentoutputs at line 2626, resolveGenerate parameter delegatecommand at line 3531, and relativepath at line 4195. The frozen base contains the same names.
- Risk: Legacy default-public naming differs from RULE.md camelCase conventions; no introduced producer naming violation or behavior regression was found.
- Disposition: Nonblocking Hygiene; existing declarations remain outside the CC4-S0 correction boundary.
- Follow-up: Admit a separate sbt-cozy naming maintenance task, inspect callers and named arguments, then apply canonical names with required compatibility handling. Do not perform incidental renaming during CC4-S0.
<!-- cncf-phase-finding:HYG-CC4-S0-SBTCOZY-LEGACY-NAMING-01:end -->

<!-- cncf-phase-finding:HYG-CC4-S0-SBTCOZY-SOURCE-SIZE-01:begin -->
### HYG-CC4-S0-SBTCOZY-SOURCE-SIZE-01

- Status: OPEN
- Discovery: REVIEW / CC4-S0 / 2026-10-05
- Source review: phase4-CC4-S0-step-review-r1
- Category/priority: hygiene / P2
- Repository: sbt-cozy
- Evidence: src/main/scala/org/goldenport/cozy/CozyPlugin.scala has 4198 physical lines; frozen base has 4163. CozyPlugin spans lines 679–2145, or 1467 lines. The physical file crosses the 1000-, 1500- and 2000-line ordinary-source thresholds and combines generation, publication, packaging, parsing, caching and delegate responsibilities.
- Risk: Existing responsibility concentration increases maintenance and review cost; no CC4-S0 correctness defect was observed.
- Disposition: Nonblocking Hygiene; retain existing behavior and initialization boundaries during this producer Step.
- Follow-up: Separately plan a comprehensive sbt-cozy source split, including affected consumer/API ownership and proportionate validation. Do not add this cleanup to CC4-S0 or make it a Phase 4 acceptance condition.
<!-- cncf-phase-finding:HYG-CC4-S0-SBTCOZY-SOURCE-SIZE-01:end -->

<!-- cncf-phase-finding:HYG-CC4-S0B-PACKAGER-SOURCE-SIZE-002:begin -->
OPEN — Directly affected package-admission caller CozyArchivePackager.scala contains 1545 lines, exceeding both the 1000-line source-debt threshold and ordinary 1500-line upper guidance. Existing helper groups cover archive construction, dependency/runtime admission, descriptors, provenance snapshots and staging verification. This inherited file is unchanged by the four-path accumulator; its provenance call still snapshots evidence and invokes the authoritative validator before archive writing. No current defect attributable to its size was identified. Owner: Cozy maintainer. Target: independent archive-packaging source-size maintenance task, with an explicit split assessment and proportionate packaging validation before implementation. This observation grants no additional implementation or acceptance scope to CC4-S0B.
<!-- cncf-phase-finding:HYG-CC4-S0B-PACKAGER-SOURCE-SIZE-002:end -->

<!-- cncf-phase-finding:HYG-CC4-S0B-PROVENANCE-SOURCE-SIZE-001:begin -->
OPEN — GenerationProvenanceAggregate.scala remains 1093 lines, exceeding the shared 1000-line source-debt threshold; original base12abad contained 1098 lines. Its single primary object contains aggregate models, delegated-input admission, source/output validation, JSON parsing, canonical digest construction and atomic publication. CC4-S0B narrows validation and reduces size without adding unrelated responsibilities or exposing a correctness failure. Retain the frozen Hygiene disposition. A coherent follow-up should assess these responsibilities together with GenerationProvenance.scala, the directly affected 1077-line V1 authority exposing aggregate model aliases and package/rebind entry points. Aligning the duplicated V1/V2 payload and validation responsibilities crosses existing source boundaries. Owner: Cozy maintainer. Target: separately admitted Cozy provenance maintenance task after this Step. Preserve schemas, aliases, canonical bytes, deterministic diagnostics, V1 compatibility and publication order; do not perform cosmetic compression or arbitrary extraction in CC4-S0B.
<!-- cncf-phase-finding:HYG-CC4-S0B-PROVENANCE-SOURCE-SIZE-001:end -->

<!-- cncf-phase-finding:HYG-CC4-S0C-001:begin -->
development-workstation: Existing repository-addition behavior scenarios retained from c4982b74, including test_two_additions_keep_original_history_and_use_selected_worktrees and test_wrong_branch_source_duplicate_and_historical_rewrite_are_rejected, omit adjacent Given/When/Then narrative boundaries. Their bodies are unchanged by CC4-S0C; newly added relocation scenarios provide those boundaries. Record as nonblocking specification-documentation maintenance for a later bounded task, preserving existing behavior and assertions.
<!-- cncf-phase-finding:HYG-CC4-S0C-001:end -->

<!-- cncf-phase-finding:HYG-CC4-S0C-002:begin -->
development-workstation: Several unchanged source-build behavior scenarios retained from c4982b74, including test_existing_phase_authority_and_canonical_phase_requirements_remain and test_executor_sets_exact_selection_even_if_parent_environment_differs, omit adjacent Given/When/Then narrative boundaries. Newly added nested-source scenarios use the required boundaries. Record as nonblocking specification-documentation maintenance for a later bounded task; do not broaden the current blocker-fix scope solely for this debt.
<!-- cncf-phase-finding:HYG-CC4-S0C-002:end -->

<!-- cncf-phase-finding:HYG-CC4-S0D-001:begin -->
development-workstation: reassessment_repair.py lines 291–299 retain unreachable legacy conformance-writing code after the unconditional return at line 290. The same block exists at Step-start b8ccbd3138d9be8ef76942fcf99b43a9d1077b86. It obscures the effective atomic-writer path but does not execute or affect CC4-S0D relocation accounting. Remove the dead block in a separately admitted maintenance task while preserving current conformance persistence semantics.
<!-- cncf-phase-finding:HYG-CC4-S0D-001:end -->

<!-- cncf-phase-finding:HYG-CC4-S0D-002:begin -->
development-workstation: untouched existing scenarios lack adjacent Given/When/Then documentation, including test_wrong_wire_variants_and_invented_phase_fields_are_rejected at test_development_source_build.py:188 and test_two_additions_keep_original_history_and_use_selected_worktrees at test_repository_addition.py:414. Ordinary Git inspection confirms these scenario bodies already existed at Step-start b8ccbd3138d9be8ef76942fcf99b43a9d1077b86. Their assertions remain useful, but their narrative readability needs a separate specification-maintenance task. The newly added CC4-S0D scenarios supply semantic setup/action/expectation clauses and are not included in this debt.
<!-- cncf-phase-finding:HYG-CC4-S0D-002:end -->

<!-- cncf-phase-finding:HYG-CC4-S0D-003:begin -->
development-workstation: phase_review.py lines 12–14 retain unused stat, subprocess and tempfile imports following extraction of physical observation and writing into phase_review_io.py. The complete module has no remaining references to these imported modules; Path.stat() is an instance method. This is nonbehavioral extraction residue. Remove the unused imports in a separately admitted maintenance task without changing public declarations or IO delegation.
<!-- cncf-phase-finding:HYG-CC4-S0D-003:end -->

<!-- cncf-phase-finding:HYG-CC4-S0E-SUPPORTING-SPEC-CONFORMANCE-001:begin -->
HYG-CC4-S0E-SUPPORTING-SPEC-CONFORMANCE-001 — Pre-existing conformance debt in unchanged supporting specifications. Phase51Cv03BuildIntegrationSpec.scala:13-16 imports and uses scala.concurrent.ExecutionContext without the required alias. Both that file and Phase51Cv05BuildProvenanceSpec.scala lack version-history headers. Their fixture helpers use OS-global Files.createTempDirectory(prefix), at CV03:199 and CV05:157, instead of repository target work areas. The accepted-base-to-current diff for both files is empty. Risk: ambiguous execution-context naming, missing source history, and verification artifacts outside the prescribed workspace. Nonblocking for S0E: these issues neither change the optional runtime-directory contract nor invalidate the observed successful behavioral results; the new S0E specification uses compliant names, headers and target-owned fixtures. Proposed maintenance boundary: a separate supporting-spec conformance task preserving existing behavioral coverage, recovering reliable header dates, aliasing ScalaExecutionContext and relocating only these fixtures beneath target.
<!-- cncf-phase-finding:HYG-CC4-S0E-SUPPORTING-SPEC-CONFORMANCE-001:end -->

<!-- cncf-phase-finding:HYG-CC4-S0F-BACKEND-SOURCE-SIZE-002:begin -->
OPEN — Existing SimpleModeler generator source-size debt. The file containing the selected runtime generator class has 4371 lines; Scala3ClassGeneratorExecutor occupies lines 73–4371, or 4299 lines. The unchanged native HEAD 494c72a5 has the same size. It crosses the 1000-line debt threshold, normal 1500-line limit and 2000-line high-priority threshold, combining class/companion emission, validation, record and datastore conversion, aggregate methods, schema generation and parameter rendering. No generator source change is admitted by CC4-S0F, and current real generation and compilation succeeded. Owner: SimpleModeler maintainer. Follow-up: a separately planned generator modularization that assesses ComponentPart/self-type boundaries, shared generator vocabulary and behavioral specifications. This is inherited nonblocking debt, grants no generator repair scope, and must not become a local compatibility workaround.
<!-- cncf-phase-finding:HYG-CC4-S0F-BACKEND-SOURCE-SIZE-002:end -->

<!-- cncf-phase-finding:HYG-CC4-S0F-LEGACY-BUILD-NAMING-001:begin -->
OPEN — Existing Cozy build-local naming debt. Unchanged task bodies retain lowerCamel local names such as currentVersion at line 340, allowNonLatest at line 345, sourceCatalog at line 348 and warehouseCatalog at line 349, contrary to the shared flatcase local-variable rule. Native accepted HEAD bdc04136 contains these same declarations. New CC4-S0F declarations follow their visibility categories; this inherited naming debt has no observed effect on source selection or generation. Owner: Cozy maintainer. Follow-up: separately admitted build-naming maintenance, preserving publication behavior and settings initialization. Do not include incidental renaming in the current blocker correction.
<!-- cncf-phase-finding:HYG-CC4-S0F-LEGACY-BUILD-NAMING-001:end -->

<!-- cncf-phase-finding:HYG-CC4-S0H-001:begin -->
HYG-CC4-S0H-001 — Existing typed commit-control source-size debt. common/codex/skills/cncf-commit-execution/scripts/typed_commit_control.py contains 1,838 lines; native HEAD already contains 1,825 lines, exceeding the shared rule's 1,000-line debt threshold and normal 1,500-line ordinary-source limit. The file combines typed models/codecs, intake/history admission, Git inspection/staging, platform rendering, and release-closure execution. Risk: changes across these responsibilities are harder to review and maintain. This is nonblocking existing debt: S0H adds only the selected native-HEAD comparison and explicit allocator argument, without introducing another responsibility or an observed correctness failure. A coherent split must account for shared model dependencies, imported entrypoints, initialization and behavioral integration specifications; it is outside the frozen S0H edit boundary. Proposed maintenance boundary: a separately planned commit-control modularization task preserving public imports, schemas, initialization order and execution behavior.
<!-- cncf-phase-finding:HYG-CC4-S0H-001:end -->

<!-- cncf-phase-finding:HYG-CC4-S2-CNCF-LAUNCHER-SOURCE-SIZE-001:begin -->
Existing source-size debt: CncfLauncher.scala measures 1065 lines, above the 1000-line debt threshold, versus the frozen 1082-line baseline. It combines runtime selection, development execution, repository/install commands, registration/evidence integration, and configuration routing, with ten constructor dependencies. The current delta reduces its size and does not authorize broad extraction. Follow up in a dedicated Launcher maintenance task that coordinates caller/injection wiring and existing behavioral specifications; preserve canonical invocation, lifecycle, initialization, and public API contracts.
<!-- cncf-phase-finding:HYG-CC4-S2-CNCF-LAUNCHER-SOURCE-SIZE-001:end -->

<!-- cncf-phase-finding:HYG-CC4-S2-CNCF-LEGACY-CONFORMANCE-003:begin -->
Existing target-program naming debt remains in the private RegistrationTarget model at CncfLauncher.scala:646, whose internal fields include artifactId, executionMode, developmentDirectory, subsystemName, and subsystemVersion. The same fields are present in accepted HEAD and were not introduced or changed by this Step. Normalize only in a separately bounded maintenance task with its private construction/access sites; preserve the distinct public serialized registration-report field names. No introduced declaration naming failure was found in the reviewed Step delta.
<!-- cncf-phase-finding:HYG-CC4-S2-CNCF-LEGACY-CONFORMANCE-003:end -->

<!-- cncf-phase-finding:HYG-CC4-S2-CNCF-SPEC-SIZE-002:begin -->
Existing specification-size debt: CncfLauncherSpec.scala measures 5016 lines and 121 in blocks, exceeding the 5000-line and approximately 100-example thresholds; the frozen baseline was 5019 lines. It combines command parsing, runtime/catalog/install/development behavior, evidence/registration, and historical supervisor prototypes, with a separate manual-main accumulator calling its scenario methods. Split by observable behavior in a dedicated maintenance task, coordinating that accumulator and shared fixtures so coverage and semantic grouping remain intact. The new notification suite correctly avoids adding its substantive scenarios to this monolith.
<!-- cncf-phase-finding:HYG-CC4-S2-CNCF-SPEC-SIZE-002:end -->

<!-- cncf-phase-finding:HYG-CC4-S3-SUPERVISOR-SOURCE-SIZE-001:begin -->
HYG-CC4-S3-SUPERVISOR-SOURCE-SIZE-001 — Owner: textus-supervisor. src/main/scala/org/simplemodeling/textus/supervisor/impl/StandaloneTextusSupervisor.scala contains 1,016 lines, exceeding RULE.md's 1,000-line ordinary-source debt threshold. The unchanged file combines launch-profile/catalog discovery, native process readiness and recovery, snapshot codecs/storage, and lifecycle ownership orchestration. The accepted HEAD contains the same 1,016 lines; CC4-S3A neither changes nor increases this debt. Follow up in a separate Supervisor source-organization maintenance task: evaluate cohesive file boundaries and preserve package visibility, initialization, public APIs, persistence encoding and ownership behavior. This is nonblocking Hygiene and does not authorize extraction or other edits in the current protected Service Step.
<!-- cncf-phase-finding:HYG-CC4-S3-SUPERVISOR-SOURCE-SIZE-001:end -->

<!-- cncf-phase-finding:HYG-CC4-S4-CORE-LEGACY-NAMING-001:begin -->
HYG-CC4-S4-CORE-LEGACY-NAMING-001 — OPEN. Owner: goldenport-core. HttpResponse.scala:130 and :149 retain the public parseHtml/parseXml parameter name contenttype rather than canonical public contentType. Native accepted HEAD bd242864637482e4d012665ab4a979c75dfd4b14 contains the same declarations; the current status-only delta neither introduces nor changes them. This is nonblocking naming maintenance with no observed effect on NoContent, redirection, or parser behavior. Follow up in a separately admitted core API naming task that checks named-argument callers and any explicit compatibility contract. Do not rename these public parameters incidentally in CC4-S4.
<!-- cncf-phase-finding:HYG-CC4-S4-CORE-LEGACY-NAMING-001:end -->

<!-- cncf-phase-finding:HYG-CC4-S4-UOW-HTTP-SPEC-CONFORMANCE-003:begin -->
HYG-CC4-S4-UOW-HTTP-SPEC-CONFORMANCE-003 — OPEN. Owner: goldenport-cncf. The unchanged supporting accumulator UnitOfWorkHttpSpec.scala has three behavior scenarios at lines 82, 98, and 110 without Given/When/Then clauses, and its _response_ok helper uses the lowerCamel local contentType at line 70. Its accepted-base-to-current diff is empty. The existing assertions remain meaningful and all three cases passed in the selected current accumulator; new BoundedHttpDriverSpec scenarios supply semantic boundaries. This is nonblocking supporting-spec documentation and naming debt. Follow up in a separate bounded conformance task, preserving the Free/direct dispatch and property propagation assertions; do not expand the current blocker repair solely for this maintenance.
<!-- cncf-phase-finding:HYG-CC4-S4-UOW-HTTP-SPEC-CONFORMANCE-003:end -->

<!-- cncf-phase-finding:HYG-CC4-S4-UOW-INTERPRETER-SOURCE-SIZE-002:begin -->
HYG-CC4-S4-UOW-INTERPRETER-SOURCE-SIZE-002 — OPEN. Owner: goldenport-cncf. The directly affected ordinary HTTP dispatcher UnitOfWorkInterpreter.scala measures 2,188 lines, exceeding the 1,000-line debt threshold, normal 1,500-line limit, and 2,000-line high-priority threshold. Accepted Step base 1bbbfeb10e324cfa019ffa6a9cb1e957b622c8bb contains the same 2,188 lines and the current diff for this file is empty. Its operation match, state-machine dispatch, datastore/entity access and authorization, CallTree handling, and process-resource cleanup form multiple independently changing helper groups. The HTTP branches at lines 227-245 preserve the ordinary selected-driver and CallTree path; no current HTTP defect attributable to this size was found. Record nonblocking inherited debt and plan a separate interpreter modularization task that preserves public entry points, dispatch semantics, transaction/resource cleanup, observability, and dependency direction. This observation grants no extraction scope to CC4-S4.
<!-- cncf-phase-finding:HYG-CC4-S4-UOW-INTERPRETER-SOURCE-SIZE-002:end -->

<!-- cncf-phase-finding:HYG-CC4-S5A-CNCF-HTTP-SERVER-SIZE-001:begin -->
HYG-CC4-S5A-CNCF-HTTP-SERVER-SIZE-001 — OPEN. Owner: goldenport-cncf. The directly consumed HTTP ingress implementation Http4sHttpServer.scala contains 8,118 physical lines; its primary class spans lines 74–7919, or 7,846 lines. Native HEAD has the same 8,118 lines and this Step changes none of them. The file exceeds the 1,000-line debt threshold, normal 1,500-line limit and 2,000-line high-priority threshold, combining server lifecycle, REST/MCP routing, assets, administration, help/manual rendering, forms, jobs, authentication/security integration and response adaptation. The new fixture uses its existing package-visible routes rather than copying implementation. No current defect attributable to this inherited size was identified. Record nonblocking Hygiene and plan a separate CNCF HTTP-server modularization task coordinating private helper groups, ingress security, initialization and route specifications. This observation grants no extraction or upstream-edit scope to CC4-S5A.
<!-- cncf-phase-finding:HYG-CC4-S5A-CNCF-HTTP-SERVER-SIZE-001:end -->

<!-- cncf-phase-finding:HYG-CC4-S5A-CONTROL-CENTER-SUPPORTING-NAMING-002:begin -->
HYG-CC4-S5A-CONTROL-CENTER-SUPPORTING-NAMING-002 — OPEN. Owner: textus-control-center. The unchanged supporting factory retains protected component_core parameter componentid at ComponentFactory.scala:80 instead of canonical componentId. Native HEAD contains the same declaration and use. This is pre-existing nonbehavioral naming debt outside the thirteen-path Step delta; the reviewed Supervisor factory uses the canonical public/protected parameter spelling. Follow up in a separately admitted naming-maintenance task that checks named-argument callers and preserves component construction and public/protected compatibility. Do not include incidental renaming in the current blocker correction.
<!-- cncf-phase-finding:HYG-CC4-S5A-CONTROL-CENTER-SUPPORTING-NAMING-002:end -->

<!-- cncf-phase-finding:HYG-CC4-S5A-SPEC-SOURCE-LINKS-003:begin -->
HYG-CC4-S5A-SPEC-SOURCE-LINKS-003 — OPEN. Owner: textus-control-center. launcher-lifecycle-control.md:227 and :230 link executable source through the task-private .codex-workflow/checkouts/textus-supervisor directory. These links resolve in the selected development workspace, but that checkout is not a tracked part of the Control Center repository, so ordinary repository readers cannot follow them. The referenced specifications were reviewed directly and this presentation issue does not invalidate their behavioral evidence. In a separate documentation-maintenance boundary, replace private-workspace links with durable repository-qualified references to the same source specifications, retaining their XP descriptions and acceptance limitations. No publication or remote mutation is implied.
<!-- cncf-phase-finding:HYG-CC4-S5A-SPEC-SOURCE-LINKS-003:end -->

<!-- cncf-phase-finding:HYG-CC4-S6B-CNCF-EVIDENCE-LEGACY-NAMING-001:begin -->
Nonblocking inherited target-program naming debt: CncfLocalServerEvidenceStore declares private val lock, whereas RULE.md requires private member values to use _snake_case. The same declaration exists at the Step base; the observational tuple change preserves the existing monitor and common file-lock behavior. No current behavior or validation failure results. In a separate naming-maintenance boundary, rename this private member and its references while preserving monitor identity, lock ordering, persistence and public/schema contracts. This review does not authorize that maintenance.
<!-- cncf-phase-finding:HYG-CC4-S6B-CNCF-EVIDENCE-LEGACY-NAMING-001:end -->

<!-- cncf-phase-finding:HYG-CC4-S6B-TEXTUS-EVIDENCE-LEGACY-NAMING-002:begin -->
Nonblocking inherited target-program naming debt: LocalServerEvidenceStore declares private val lock, whereas RULE.md requires private member values to use _snake_case. The same declaration exists at the Step base; the observational tuple change preserves the existing monitor and common file-lock behavior. No current behavior or validation failure results. In a separate naming-maintenance boundary, rename this private member and its references while preserving monitor identity, lock ordering, persistence and public/schema contracts. This review does not authorize that maintenance.
<!-- cncf-phase-finding:HYG-CC4-S6B-TEXTUS-EVIDENCE-LEGACY-NAMING-002:end -->

<!-- cncf-phase-finding:HYG-CC4-S7-TEXTUS-LEGACY-API-NAMING-001:begin -->
### HYG-CC4-S7-TEXTUS-LEGACY-API-NAMING-001

- Status: OPEN.
- Discovery: REVIEW / CC4-S7 / 2026-10-17.
- Source review: phase4-CC4-S7-step-review-r1.
- Category/priority: Hygiene / P3.
- Owner: Textus Launcher maintainer.
- Evidence: TextusLauncher.scala:21-25 retains public constructor parameter labels runtimeresolver, cncfinvoker, launcherdevinvoker and registrationreporter; all exist unchanged at Step base 9d11f8b. RULE.md requires public parameter camelCase. They are distinct from existing HYG-CC4-S6B-TEXTUS-EVIDENCE-LEGACY-NAMING-002, which concerns the private evidence-store lock.
- Impact: Source naming inconsistency; no changed runtime behavior or introduced naming defect. Existing named-argument clients make an incidental rename a public source compatibility change.
- Disposition: Nonblocking inherited debt, outside the frozen local CAR activation delta.
- Follow-up: Plan a separate public-parameter migration covering named-argument clients and explicit compatibility requirements; preserve current constructor behavior and do not extend CC4-S7.
<!-- cncf-phase-finding:HYG-CC4-S7-TEXTUS-LEGACY-API-NAMING-001:end -->

<!-- cncf-phase-finding:HYG-CC4-S8-CNCF-RESOLVED-PARAMETER-NAMING-001:begin -->
### HYG-CC4-S8-CNCF-RESOLVED-PARAMETER-NAMING-001

- Status: OPEN.
- Discovery: REVIEW / CC4-S8 / 2026-10-17.
- Source review: phase4-CC4-S8-step-review-r1.
- Category/priority: Hygiene / P3.
- Owner: goldenport-cncf maintainer.
- Evidence: ResolvedParameter.scala:44 and57 retain public format_value and origin_label declarations. Ordinary base-to-working-tree diff against86f2c413 shows these public names and their callers are inherited unchanged.
- Impact: Existing public naming differs from shared RULE.md camelCase requirements; no introduced naming or behavioral defect.
- Disposition: Nonblocking maintenance, distinct from the26existing accepted Hygiene identities in ledger39. The same unaccepted candidate in parent mechanical evidence is not a second record.
- Follow-up: Separately admit public-method naming migration with caller and source compatibility assessment. Do not rename these APIs or expand the synchronized-map repair during CC4-S8.
<!-- cncf-phase-finding:HYG-CC4-S8-CNCF-RESOLVED-PARAMETER-NAMING-001:end -->

<!-- cncf-phase-finding:HYG-CC4-S10-EVIDENCE-PROTOCOL-SPEC-GWT-002:begin -->
### HYG-CC4-S10-EVIDENCE-PROTOCOL-SPEC-GWT-002

- Status: OPEN.
- Discovery: REVIEW / CC4-S10 / 2026-10-17.
- Source review: phase4-CC4-S10-step-review-r1.
- Category/priority: Hygiene / P3.
- Owner: textus-control-center maintainer.
- Evidence: Supporting LauncherEvidenceProtocolSpec.scala:11,21,28 retains three behavior scenarios without GivenWhenThen or adjacent Given/When/Then clauses. The complete file is unchanged against Step base c2ebd8f3a70ef2ec42175c01242622aa981462bc. Its existing should assertions remain useful and the selected actual focused run passed.
- Impact: Inherited executable-documentation conformance debt; it neither weakens the new real-process regression specification nor invalidates the observed transport results.
- Disposition: Nonblocking supporting-spec maintenance, distinct from ledger revision 42's 27 accepted Hygiene identities.
- Follow-up: In a separately admitted specification-maintenance task, add semantic setup/action/expectation narration while preserving the three protocol/identity/fixed-command contracts. Do not expand the CC4-S10 blocker repair to perform this maintenance.
<!-- cncf-phase-finding:HYG-CC4-S10-EVIDENCE-PROTOCOL-SPEC-GWT-002:end -->

<!-- cncf-phase-finding:HYG-CC4-S10-EVIDENCE-SPEC-WORK-ARTIFACT-003:begin -->
### HYG-CC4-S10-EVIDENCE-SPEC-WORK-ARTIFACT-003

- Status: OPEN.
- Discovery: REVIEW / CC4-S10 / 2026-10-17.
- Source review: phase4-CC4-S10-step-review-r1.
- Category/priority: Hygiene / P3.
- Owner: textus-control-center maintainer.
- Evidence: The unchanged supporting evidence-reconciliation scenario in SubsystemInventoryActionSpec.scala:507 allocates Files.createTempDirectory("control-center-launcher-evidence") without a repository target parent. Shared RULE.md requires local verification work artifacts under target. The source file is unchanged against Step base c2ebd8f3a70ef2ec42175c01242622aa981462bc; the newly introduced LauncherEvidenceCommandRunnerSpec uses target/launcher-evidence-process-spec and explicit cleanup.
- Impact: Existing test fixture placement/retention debt outside the owned delta; no current transport or packaged runtime defect results.
- Disposition: Nonblocking inherited supporting-spec maintenance, distinct from the accepted Hygiene ledger identities.
- Follow-up: Separately admit fixture-work-area maintenance for this scenario, retaining the external-command option and existing behavioral assertions, and use repository target-owned fixture allocation with appropriate cleanup. Do not widen the current Step repair.
<!-- cncf-phase-finding:HYG-CC4-S10-EVIDENCE-SPEC-WORK-ARTIFACT-003:end -->

<!-- cncf-phase-finding:HYG-CC4-S10-EVIDENCE-SUPPORTING-API-NAMING-001:begin -->
### HYG-CC4-S10-EVIDENCE-SUPPORTING-API-NAMING-001

- Status: OPEN.
- Discovery: REVIEW / CC4-S10 / 2026-10-17.
- Source review: phase4-CC4-S10-step-review-r1.
- Category/priority: Hygiene / P3.
- Owner: textus-control-center maintainer.
- Evidence: Unchanged supporting LauncherEvidenceProtocol.scala:70 declares the public detail parameter instanceid. LauncherEvidenceServiceFactoryImpl.scala:148,154,183 declares protected parameters artifactid, registeredids and observedat. Shared RULE.md requires public/protected parameter camelCase. The ordinary diff against Step base c2ebd8f3a70ef2ec42175c01242622aa981462bc for both supporting files is empty.
- Impact: Existing source naming inconsistency; no introduced signature or transport behavior defect. New CC4-S10 Scala declarations satisfy their visibility-specific naming rules.
- Disposition: Nonblocking inherited supporting-file debt, distinct from all 27 accepted Hygiene identities in ledger revision 42.
- Follow-up: Separately admit an evidence-consumer parameter-naming maintenance task, inspect named-argument clients and any explicit compatibility contract, and preserve protocol fields, failure behavior and service semantics. Do not incidentally rename these parameters in the current Step.
<!-- cncf-phase-finding:HYG-CC4-S10-EVIDENCE-SUPPORTING-API-NAMING-001:end -->
