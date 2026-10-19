# Phase 5 Checklist: Summary API and First Menu Bar Launch

Status: IN-PROGRESS; CC5-S1 accepted, actual HTTP/native human route passed; CC5-S2 acceptance pending
Plan: [Phase 5](phase-5.md)
Design: [Operational View and native presentation](../design/macos-menu-bar-and-operational-view.md)
Handoff: [First-route operator and successor input](../journal/2026/10/2026-10-19-phase-5-first-route-handoff.md)

This is the sole Phase 5 completion ledger after the 2026-10-18 split.
Reuse Phase 4 evidence at its actual scope; this checklist does not reopen it.
Original CC5-01..04 stay here unchanged. Original CC5-05..10 are explicitly
relocated, still unaccepted and with unchanged meaning, to the sole
[Phase 5.1 ledger](phase-5.1-checklist.md); this is relocation, not deletion or
a completion claim. Do not duplicate their unchecked rows here.

- [ ] CC5-01: Existing operational/catalog/Launcher/CNCF sources are reconciled
      and the minimum common Summary input/result/state contract is specified.
      Actual Operation signatures, target/instance identity, overall-health and
      unavailable/not-applicable semantics, URL handling and bounded refresh are
      settled before implementing their callers; no duplicated runtime registry.
- [ ] CC5-02: The normal Control Center Operation/HTTP JSON surface returns
      attributable status, version, uptime, overall health, configured runtime
      summary and Dashboard URL. It is reusable by Web/native presentations,
      with normal access rules and no HTML/private-dashboard-JSON scraping.
- [ ] CC5-03: A thin Swift/SwiftUI Menu Bar application consumes that actual API,
      shows status/summary, refreshes, distinguishes disconnection from service
      health and opens the Dashboard. No duplicated discovery or health logic.
- [ ] CC5-04: The same built .app is packaged and actually launched through the
      first selected supported parent-bundled or separate-CAR path, with normal
      launcher/login integration demonstrated. Record the selected mode and host;
      allocation, compilation or a mocked UI response alone is insufficient.
- [ ] CC5-A1: The narrowed API -> Menu Bar -> Dashboard -> first packaged
      install/login route has attributable actual results, meaningful focused
      and affected regression validation, independent Phase review without
      unresolved blockers and accepted Steps. Setup, selected host/profile/form
      and limitations are documented.
- [ ] CC5-A2: Closure ledger, durable API/native/producer/first-form handoff and
      this Phase's release are recorded. Control Center repository-complete
      validation is explicitly deferred-not-run to PHASE-5.1 under the reciprocal
      final-only/sbt-full-suite policy; no full-suite pass is claimed.

Do not check off a whole item from partial coverage. Missing producer/host
evidence stays an explicit gap; unrelated ready work may proceed. Both CAR
distribution forms remain required by the complete sequence. Phase 5.1 owns the
remaining observations, second form, integrated full validation and closure.
No source, test, release, deployment or Goal is created by this planning split.

## Current validation route — 2026-10-19

The user's progress-first instruction replaces native UI automation with a
manual smoke check. Automated UI selection/action and additional AX diagnostics
are not closure requirements. Existing focused specifications and actual
HTTP/CAR/install/launch/login/cleanup results remain applicable at their scopes;
all failed automation runs and original repair counters remain unchanged.

- Exact displayed Summary versus the actual API response: human PASS in r22 for
  Version `0.1.1-SNAPSHOT`, Health (control-center) `ok`, and Registered runtimes
  `0 total; 0 returned`.
- Actual Open Dashboard button opens the expected Dashboard: human PASS in r22.
  Exact button-feedback wording was not separately reported; none is inferred.
- Incomplete native AX diagnostic additions: already parked privately and excluded.
- Independent Step/Phase reviews and local acceptance/release commits: still required.

Normal build r3 passed 21 Swift specifications, release compilation and strict
ad-hoc signature verification. Normal CAR r4 was parent-bundled for macOS 13+ /
Darwin arm64 and the local Operator profile. Actual r22 passed normal staging,
private installation, enabled login, detached launch, installed production
Summary client and sixteen-second liveness. Summary 200, invalid bearer 403,
invalid limit 400 and Dashboard 200 passed. The human replied “summary ok,
open dashboardで開いた。”. Owned cleanup completed: own login unregistered,
only the new serverURL key deleted, exact native and owned groups/readers
ended, port reusable. Exit 0, no timeout/signal/force, terminal success.

These actual human observations remain separate from the old failed automation
receipts. No further host action is required. Successful route execution does
not close a whole checklist item, accept CC5-S2 or close the Phase. The
[handoff](../journal/2026/10/2026-10-19-phase-5-first-route-handoff.md) records
validation at its actual scope; only Control Center complete validation is
`deferred-not-run` to Phase 5.1. That successor has not started.

## Step ledger — 2026-10-18

Current CC5-S2 route result is recorded above. Earlier pending statements below
remain historical at their original attempt scope; all failures, IDs, counters,
Step7/Phase0 and epoch2 are retained. Step/Phase acceptance is still pending.

- CC5-S1: CLOSED / accepted; Summary Operation, source projection and protected
  reads; focused Scala 36/36, CAR lint success, independent Step review PASS;
  local acceptance commit `2deb1b944edba69e975fe3249228a4b35752fc89`.
- CC5-S2: IN-PROGRESS / implementation; native 21 specifications and release
  build/signature verification passed. CNCF complete 169 plus legacy and Textus
  declared 25 plus legacy and new 16 specifications passed. Normal parent CAR
  packaging succeeded after the narrowly approved runner correction passed 56
  Python specifications and independent review. The real HTTP wire defect was
  repaired with 37 focused specifications/packageBin and independent focused
  re-review PASS. The regenerated parent CAR passed actual Summary 200,
  invalid-bearer 403, invalid-limit 400 and Dashboard HTML 200 checks; server
  cleanup was terminal. The original failure remains recorded. Native Menu Bar
  UI, Dashboard button and packaged installation/launch/login remain unaccepted:
  the first native session was denied by automatic approval review before any
  host execution. After explicit authorization, one native attempt passed HTTP
  and normal CAR staging but failed before app installation on the verification
  script's whole-user process-list capture limit. The original failure and
  incomplete cleanup receipt remain recorded; saved script/server/stage PIDs
  were absent in a separate read-only check. Exact-path PID-only discovery was
  repaired; syntax inspection and actual read-only positive/no-match OS queries
  passed. Independent prerequisite baseline review found no blockers. That audit
  does not accept CC5-S2; native launch/login/UI acceptance is still pending.
  A second expressly approved attempt passed discovery and installed the app,
  then crashed in own-suite UserDefaults initialization before origin write or
  login registration. The original failed/incomplete receipt remains intact.
  Standard application defaults now preserve the specified domain/key; a fresh
  build passed all 21 Swift specifications and strict signing. The built
  command-line entrypoint returned its expected input error without crashing.
  Fresh normal CAR packaging and independent prerequisite review passed;
  positive native evidence still required an actual host attempt. The newly
  approved r4 attempt passed installation, enabled login registration, detached
  launch, installed Summary client and sixteen-second liveness. The initial
  System Events query timed out; the OS log records an AppleEvent permission
  rejection. Menu observation and Dashboard-button activation were not reached.
  Native-process absence, login unregistration and new serverURL deletion were
  recorded, but server-group finalization raised EPERM. Preserve its original
  cleanup=false/incomplete receipt. Separate read-only observations found the
  four saved PIDs absent and no listener on the selected port; these do not
  establish successful cleanup. The single r4 attempt is consumed. An actual
  server-only exited-child diagnostic reproduced the unreaped-child EPERM;
  acquired-handle reap ordering was corrected without weakening group absence
  or output finalization. Real exited-child regression, HTTP/idle/cleanup and
  independent prerequisite code review passed. Missing original platform-object
  retention in the diagnostic runners remains an explicit evidence limitation;
  the original r4 failure is unchanged and its exclusive cause unproved.
  The own preference domain was left empty after sole-key deletion. The native
  precondition now accepts missing/empty only; eight actual-helper cases and the
  read-only actual host-domain check passed; independent prerequisite review
  found no blockers. Host automation permission and
  positive UI/Dashboard evidence remain open; no automatic retry, OS settings
  change, Step acceptance or Phase acceptance is authorized by these checks.
  Following user-reported Automation enablement and a new single-attempt
  authorization, r5 passed the same normal installation/login/launch/Summary
  path and sixteen-second liveness. The initial owned-PID System Events query
  again timed out; menu observation and Dashboard-button activation were not
  reached. This time owned cleanup passed, including native absence, login/key
  cleanup, server/group finalization and reusable port. Preserve r5's actual
  readiness=true, clients=false, cleanup=true, terminal=terminal, success=false;
  the successful cleanup does not accept the failed route. OS logs identify
  ChatGPT at /Applications/ChatGPT.app, internal ID com.openai.codex, as the
  responsible app. The user subsequently confirmed Allow; a later scoped OS
  query records access-result1 after the original query timed out and native
  cleanup completed. This establishes a late permission response, not UI
  acceptance. Fresh original runner objects are
  retained; the initial preparation-only loss and late resume disclosure are
  recorded separately. The single r5 authorization is consumed; no retry or
  completion is inferred. Same Goal, counters and all prior failures remain.
  After the next explicit one-attempt approval, r6 passed actual HTTP,
  normal staging/private installation, enabled own login, detached launch,
  production Summary client and sixteen-second liveness. UI observation failed
  with Accessibility error -25211 before menu values or Dashboard activation.
  The exact OS log records Accessibility for com.openai.codex, responsible
  ChatGPT.app, as Denied (System Set). The user later located Accessibility
  and reported ChatGPT enabled, with Codex Computer Use disabled. This human
  observation does not replace a successful native route.
  Own login unregistration, new serverURL deletion and native absence were
  recorded, but an owned child-group finalization raised EPERM. Keep r6's
  readiness=true, clients=false, cleanup=false, terminal=incomplete,
  success=false. Later read-only checks found the four selected saved PIDs and
  selected-port listener absent; they do not establish successful cleanup or
  the EPERM's exclusive cause. The runner failed to retain its original
  verify/render/submission/initial/poll/admission objects; this separate
  limitation is recorded without reconstruction. The single r6 authority is
  consumed; no further native attempt, Step or Phase acceptance is inferred.
  The user explicitly resumed the same Goal for cleanup investigation, retaining
  all counters and failures. A real HTTP-only server diagnostic with a 26-second
  idle interval passed Summary and owned cleanup; EPERM was not reproduced.
  Passive exception locations and exact owned-group before/after observations
  were saved. This narrower pass does not establish r6's cause or repair it.
  Its runner subsequently transcribed execution fields from tool history;
  these are attributed observations, not retained original platform objects.
  An initial exit-race diagnostic selection stopped before process acquisition
  because the parent chose an ID outside the existing HTTP-only pattern. Its
  failure and partial original-object retention remain recorded. A fresh
  correctly selected diagnostic then reproduced signal-zero EPERM both on an
  acquired short-lived child and on the real server while Popen.poll still
  reported no returncode; exact-group observations showed same-UID Z children.
  The cleanup helper now waits on its acquired handle within the remaining
  original TERM5/KILL3 deadline before its one same-group retry. Genuine
  permission failure, remaining descendants and missing output finalization
  remain failures. The same actual reproducer passed all 24 child groups and
  real server cleanup, including an observed EPERM followed by actual wait and
  group absence. The existing unreaped-Z child regression, real HTTP/26-second
  idle/cleanup and independent prerequisite review passed. Original runner
  objects for the reproduced failure and both fixed validations are retained.
  This verifies the focused harness repair; original native r6's exclusive
  cause remains unproved. Existing Goal, Step7/Phase0, separate counters and
  all prior failures remain; one new cleanup candidate is recorded separately.
  The next separately approved attempt r7 then passed HTTP, normal CAR
  staging/private installation, enabled own login, detached launch, production
  Summary client and sixteen-second liveness. Owned cleanup now completed
  successfully, including login unregistration and terminal process groups.
  UI observation failed at the fixture's ordinal first-menu-bar uniqueness
  check (-2700), before actual menu values or Dashboard activation. The exact
  first-bar contents were not retained. Complete original runner objects are
  retained. Its single approval is consumed and its failed receipt remains;
  no Step/Phase acceptance is inferred. The fixture's selection is being
  corrected to the exact owned application's AXExtrasMenuBar, the SDK's
  dedicated extras boundary. Compile-only syntax checking passed with normal
  platform dictionary access, and independent prerequisite source review
  passed. Actual corrected native verification remains pending; no additional
  native attempt is authorized by this correction. Existing counters and
  failures remain; this prerequisite does not accept the Step or Phase.
  The next explicitly approved attempt r8 passed the corrected owned extras-bar
  role/one-item selection and click, as well as HTTP, normal installation,
  login, detached launch, production client and sixteen-second liveness.
  Actual visible-window observation then failed at the existing five-second
  deadline. Summary UI values and Dashboard-button activation remain unproved.
  Owned login/key/native/group cleanup passed: readiness=true, clients=false,
  cleanup=true, terminal=terminal, success=false, exit1. Complete original
  runner objects and rejected completion admission remain retained. Its single
  approval is consumed. Saved own-PID AppKit evidence includes performClick,
  but does not distinguish no window from one excluded by the visible predicate;
  the first verbose OS query was truncated, while the narrowed query is saved.
  No product cause or repair is inferred. Bounded read-only diagnostics were
  added for the same extras item and at most eight windows of the owned process,
  preserving every action, deadline and acceptance requirement. Final syntax
  compilation and fresh independent prerequisite review passed. Actual
  instrumented native verification requires another explicit single-attempt
  authorization. Same Goal, original Step7/Phase0 and every failure remain;
  no new native attempt or Step/Phase acceptance follows from source review.
  After another explicit single-attempt approval and actual same-Goal resume,
  r9 captured the diagnostic metadata: the owned extras item was enabled with
  AXMenuBarItem/AXMenuExtra roles; all owned AX windows were zero before and
  after the unchanged click. The five-second window requirement still failed.
  This excludes visible-only filtering of an existing AX window at those
  snapshots, but proves neither visual absence nor a product root cause.
  HTTP/install/login/client/liveness and all owned cleanup passed; retain
  readiness=true, clients=false, cleanup=true, terminal=terminal, success=false,
  exit1/no timeout/signal/force and complete original platform objects. The r9
  approval is consumed. A diagnostic comparison controlling only the retained
  process's frontmost state is prepared; syntax compilation and fresh independent
  prerequisite review passed. It retains
  the existing click, observation deadlines, actual UI/Dashboard requirements
  and owned cleanup. This source preparation does not authorize another host
  run or establish a product repair. Same Goal/counters/all failures retained.
  The explicitly approved r10 comparison ran once and stopped before menu
  selection/click: owned frontmost was false before the request and on the
  immediate subsequent read. Normal HTTP/install/login/client/liveness and
  complete cleanup passed; readiness=true, clients=false, cleanup=true,
  terminal=terminal, success=false, exit1/no timeout/signal/force are retained.
  Its approval is consumed. Immediate state checking does not distinguish
  asynchronous activation from an unhonored request. A bounded observation
  correction passed syntax compilation and fresh independent prerequisite
  review: one frontmost request, followed by conditional state observation up
  to a two-second polling deadline under the original AppleEvent timeout.
  Neither a product cause nor UI acceptance is established. Original counters
  and all failed runs remain unchanged. A further host run needs a new explicit
  one-attempt authorization; none is allocated by this prerequisite review.
  After explicit approval, r11 executed once: System Events still reported
  frontmost=false before/after the bounded observation, so no menu action was
  reached. Normal HTTP/install/login/client/liveness and all owned cleanup
  passed; clients=false/success=false/exit1 and no timeout/signal/force remain.
  The approval is consumed. Own-PID OS logs identify LaunchServices launch,
  UIElement classification and CanBecomeFrontmost=true; this does not prove
  current AppKit activation policy or active state. Both frontmost comparisons
  remain unsuccessful; no longer timeout or product repair is justified.
  Same-PID AppKit diagnostic preparation passed final syntax compilation and
  fresh independent prerequisite review (ledger20). It reads only the retained
  process's PID, policy, active and finishedLaunching state before/after the
  existing frontmost request. Runtime bridge behavior and actual snapshots are
  unverified. Frontmost is a harness diagnostic precondition, not a product
  requirement. Combined completed2/nonconverging2 remains retained; a third
  same-root result without meaningful narrowing stops automatic diagnostic
  editing. No successor native attempt is authorized by this source review.
  After explicit same-Goal resume, approved r12 executed once. System Events
  reported frontmost=false before/after; subsequent same-PID AppKit snapshots
  reported policy1, finishedLaunching=true and active changing false to true.
  The workflow-added frontmost assertion stopped the run before menu action.
  Sequential snapshots are not atomic and establish no product root cause;
  they narrow the universal-unactivated hypothesis. HTTP/install/login/client/
  liveness and all owned cleanup passed. The terminal unsuccessful result and
  prior failures remain. The runner did not retain the required original native
  tool objects; it explicitly reported the gap and performed no reconstruction
  or replay. Saved runtime receipts/process/logs remain diagnostic evidence,
  with no positive route or Step/Phase acceptance. The r12 approval is consumed.
  The extraneous System Events frontmost assertion was replaced by a diagnostic
  comment only; no native-active or policy assertion replaces it. All owned UI
  selection, actual Summary/Dashboard requirements, bounds and cleanup remain.
  Final syntax and fresh independent prerequisite review passed (ledger21).
  Actual UI remains unverified; another native run requires new single-attempt
  authority and original native tool-object retention before submission.
  Approved r13 ran once under the same active Goal. It passed the removed
  extraneous gate and reached the owned extras click, but the five-second
  window observation still returned zero windows. Native state again reported
  policy1/finishedLaunching=true/active=true after the request; sequential
  observations do not prove atomic activation state. Normal HTTP/install/login/
  client/liveness and all owned cleanup passed; clients=false/success=false,
  exit1/no timeout/signal/force remain. Complete original native tool records
  were retained this time. The same-PID narrow OS log supplied no causal
  presentation evidence. No visual absence or product defect is inferred;
  Summary UI and Dashboard-button acceptance remain pending. Approval consumed.
  Failure-only diagnostics for the retained own extras item and process AX
  hierarchy passed syntax compilation and fresh independent prerequisite
  review (ledger22). They distinguish unavailable reads from zero children,
  use separate diagnostic state and explicit traversal/output bounds, and
  preserve the original window failure and all actual acceptance/cleanup.
  No new runtime observation or product-repair claim follows from this review.
  Window completed1/nonconverging1 and all original counters remain retained.
  Approved r14 ran once and failed the same actual UI window observation.
  Normal HTTP/install/login/client/liveness and owned cleanup passed; exit1,
  clients=false and success=false remain. Bounded own AX hierarchy diagnostics
  show an extras item with zero children and two application menu bars; the
  sample is truncated and proves no product cause or visual absence. The human
  reported an ordinary client window, excluding the fullscreen premise.
  Required original native tool objects were not retained by the runner;
  this disclosed historical gap remains unreconstructed. Direct saved receipts
  provide limited diagnostic facts and no route acceptance. Approval consumed.
  A source-only runner retention correction passed independent prerequisite
  review (ledger23) and typed candidate deployment; fresh behavior is untested.
  A bounded comparison now reads the same owned menu item's supported actions
  and sends one explicit AXPress when supported. Syntax and fresh independent
  prerequisite review passed (ledger24); actual UI success is still unverified.
  No product behavior or acceptance requirement changed. A new host attempt
  needs fresh single-attempt authority. Window completed2/nonconverging2 and
  original Step7/Phase0 remain; a third result without meaningful narrowing
  stops automatic repair. No successor native run has been allocated.
  Approved r15 executed once. The same owned extras item advertised AXPress;
  one explicit AXPress returned, but owned windows remained zero within five
  seconds. Summary UI and Dashboard-button verification still failed; normal
  HTTP/stage/install/login/client/liveness and all owned cleanup passed.
  Actual helper exit1 without timeout/signal/force and the unsuccessful receipt
  remain. All seven original native tool-result categories were retained and
  parent-inspected; the runner retention correction worked for this fresh
  failing-admission scenario. Historical r12/r14 gaps remain unreconstructed.
  Approval consumed. Current window completed3/nonconverging3 triggers the
  automatic-repair stop; original Step7/Phase0, epoch1 and review ledger24 remain.
  No actual human-visible absence or product/framework root cause is proven.
  A technical direction is required before another repair: the proposed route
  compares minimal constant-content MenuBarExtra with application-side display
  lifecycle observations. No successor host run or acceptance is authorized.
  The human selected minimal-reproduction investigation. This material evidence
  strategy is recorded as plan epoch2 while retaining epoch1, ledger history,
  Step7/Phase0, window completed3/nonconverging3 and all failed/gapped evidence.
  Diagnostic production-source and constant minimal apps were prepared without
  changing normal compiled behavior. Normal Swift specifications passed21/0;
  both diagnostic apps compiled and passed strict signature verification.
  The concrete comparison harness passed parse/syntax and a fresh independent
  nine-path prerequisite review (ledger25). The review's private protocol path
  typo was corrected and recorded as resolved Hygiene; no blocker remains in
  this preparation boundary. No comparison app or server has been launched.
  The future diagnostic comparison observes lifecycle logs and owned AX data,
  with honest unavailable/zero/truncation distinctions and matched cleanup;
  its successful collection cannot accept Summary UI, Dashboard, CAR or login.
  Actual comparison requires a separate single-attempt human approval. The
  previous automatic-repair stop and original acceptance obligations remain.
  After the separate actual human approval and actual same-Goal resume, the
  minimal/production diagnostic comparison ran once on 2026-10-19. Real Summary,
  authentication/range and Dashboard HTTP plus production Summary client passed.
  Both apps remained live, advertised AXPress returned, and their AX window
  counts remained zero. Both own application traces reached launch completion
  and activation; no contentAppeared record was captured. Missing log records
  are inconclusive, and the internal visible-window count does not identify
  a Summary/popover. This reproduces the observed AX outcome in constant minimal
  content without the production entry/client/view, but establishes no product
  or platform root cause and no human-visible absence. Collection and all owned
  cleanup passed, with actual helper exit0/no timeout/signal/force and all seven
  original native tool-result categories retained. Diagnostic productAcceptance
  is explicitly false. The single approval is consumed. Original Step7/Phase0,
  old window3/3, ledger25 and every failed/gapped result remain retained. A manual
  physical-click comparison is being prepared to distinguish synthetic action
  from presentation; it grants no successor host execution or acceptance.
  The manual-click comparison preparation subsequently passed Python parsing,
  AppleScript syntax compilation and independent source review (ledger26).
  The planned observation performs no synthetic click or activation; the user
  operates each app once, followed by bounded read-only observation. No manual
  comparison has run, and its execution requires a fresh single-attempt approval.
  Summary presentation and Dashboard-button acceptance remain outstanding.
  The separately approved manual comparison then ran once on 2026-10-19.
  Real HTTP/client checks, both bounded observation periods and all owned cleanup
  completed, with actual exit0/no timeout/signal/force and all seven original
  native result categories retained. The human explicitly reported that the
  production target icon could not be identified and was not clicked. Therefore
  this is successful diagnostic collection with productAcceptance=false, not
  evidence about presentation after a physical click. Both AX window counts0
  and missing content callbacks remain inconclusive. The single approval is
  consumed; no automatic rerun follows. User-requested audible notification and
  exact owned-item location guidance are being prepared before any new run.
  Target-location and requested audible-cue preparation then passed Python AST,
  selected AppleScript syntax and whitespace checks, plus fresh independent
  five-path source review (ledger27). Product sources/builds remain unchanged.
  Own-item geometry is not physical visibility/reachability; audio command
  completion cannot confirm hearing or clicking. No additional host session
  ran. One new bounded manual comparison awaits a fresh actual human answer;
  the original Goal, Step7/Phase0, old synthetic3/3 and failure history remain.
  The separately approved cue attempt then terminated with exit1 before server,
  app, sound or manual opportunity: the parent selected an ID rejected by the
  existing comparison request-ID guard. Receipt cleanup=true, terminal status
  and all seven native result categories were retained and inspected. This is
  an execution-selection error, not UI evidence. The successor selection now
  follows the existing cc5-summary-http-rN convention and passed static typed
  verification without runtime or source changes. Source review27 remains
  applicable; no additional run is authorized until a fresh one-attempt answer.
  After a fresh actual one-attempt answer, corrected cc5-summary-http-r11 ran
  once and passed real HTTP/client checks, both target-location/audio/manual
  periods, terminal cleanup and helper admission. All seven original native
  categories were retained and parent-inspected. The user expressly reported
  opening both minimal and production content by physical click; each own
  trace also captured contentAppeared. Later AX window samples still showed
  zero; sample timing and continuous visibility do not establish the cause.
  Production contentAppeared preceded its cue, so no tighter click/event
  correlation is claimed. This disproves a blanket display-failure claim
  for the compared diagnostic apps, while exact Summary-field, actual
  Dashboard-button and complete normal installed-route acceptance remain
  outstanding. productAcceptance=false and original harness click flags
  remain unchanged; human facts are separate. No additional host authority.
  Normal installed-route manual-opening preparation then passed Python AST,
  AppleScript syntax, whitespace and draft request verification, plus fresh
  independent six-path review (ledger28). Existing exact Summary values,
  actual Dashboard action and normal stage/install/login/cleanup remain
  required. No additional host run occurred; one fresh approval is pending.
  After explicit approval, the normal manual-opening route ran once with
  actual exit1 and rejected success admission. HTTP, CAR stage/install,
  enabled login, detached launch, installed Summary client/liveness, own
  target/audio cue and terminal cleanup passed. The UI observer returned
  no owned visible window in60seconds; exact UI values and the actual
  Dashboard button were not reached. The human explicitly reported clicking
  and keeping the display open, so this is an observation failure rather
  than proof of human-visible absence. All seven native original categories,
  the failed receipt and original counters/history are retained. One-attempt
  authority is consumed; independent read-only investigation follows.
  Independent read-only investigation localized the failure to the visible
  AX window acquisition gate; missing raw/process/extras diagnostics prevent
  a root-cause finding. Passive failure-only diagnostics then passed syntax,
  whitespace and independent six-path source audit. Native Goal remains
  blocked and typed Phase review ledger28 is retained; this source audit
  grants no Step/Phase acceptance. No further host run occurred. A fresh
  once-only diagnostic host answer and manual participation are pending.
  After explicit approval, the normal manual-opening route ran once with
  actual exit1 and rejected success admission. HTTP, CAR stage/install,
  enabled login, detached launch, installed Summary client/liveness, own
  target/audio cue and terminal cleanup passed. The UI observer returned
  no visible=true window in60seconds; raw AX diagnostics recorded one owned
  AXWindow/AXSystemDialog with AXVisible unavailable. Exact UI values and actual
  Dashboard button were not reached. The human explicitly reported clicking
  and keeping the display open, so this is an observation failure rather
  than proof of human-visible absence. All seven native original categories,
  the failed receipt and original counters/history are retained. One-attempt
  authority is consumed; fresh independent acquisition investigation follows.
  r17 diagnostic evidence identifies a sole owned AXWindow/AXSystemDialog
  with AXVisible unavailable while the visible=true observer selected none.
  The human reports clicking and holding the display. Fresh independent
  investigation localized the acquisition barrier; a manual-mode-only repair
  now selects one unfiltered owned AXWindow and retains all bounded content,
  exact API-value, unique-button and actual Dashboard action assertions.
  Static syntax/whitespace and independent six-path source review passed,
  admitted as preparation review30 on the same active Goal. r17 remains
  failed; no Step/Phase acceptance or additional host execution is claimed.
  Original counters/history remain. A fresh one-attempt host answer is pending.
  r18 ran exactly once after fresh precise approval. Exit1 without timeout,
  signal or forced termination; success admission rejected. Normal HTTP,
  CAR stage/private install/login/launch/client/liveness and cleanup passed.
  Manual acquisition now obtained one owned AXWindow; content traversal then
  failed the unique Open Dashboard button gate. Exact UI values and actual
  Dashboard action remain unverified. Human reports clicked/held display;
  all seven native original categories, failure and counters are retained.
  No reusable runtime authority remains. Independent read-only investigation
  follows; no product defect or selector cause is assumed.
  The human further reports Connected/Version Summary and Open Dashboard
  visually present during r18. This is separate human evidence, not exact
  API-value comparison or actual Dashboard action. Independent investigation
  found the retained UI process has the correct explicit serverURL; it did
  not establish a product defect or AX representation cause. Passive traversal
  diagnostics compiled and passed draft/whitespace checks, but independent
  preparation review found their stderr can overflow the unchanged combined
  child cap with successful stdout. Typed review31 retains that blocker.
  No new real-machine run occurred. A concrete one-file correction and one-case
  focused review transport exception await a human decision: the current V3
  route cannot continue this ordinary preparation baseline chain. No Step or
  Phase acceptance, counter reset or host authority is claimed.
  The human approved the one-case local preparation correction and focused
  review transport exception. Same native Goal was actually reactivated;
  the verifier-only output correction now buffers new diagnostics and emits
  them only on original failure within the unchanged shared output cap,
  preserving successful output and original errors. Syntax, whitespace and
  typed draft checks passed. One fresh independent semantic focused review
  passed and closed the original blocker through predecessor-linked review32;
  the initial service-capacity response was not a semantic review. All actual
  source/validation/review evidence and the approved exception are retained.
  No new host run occurred. Exact UI/API values and actual Dashboard operation
  remain unverified, and a separate fresh one-attempt host answer is pending.
  Original Goal/start/bases/epoch2, Step7/Phase0, old synthetic3/3 and all
  failures remain. No Step/Phase acceptance is claimed.
  r19 ran exactly once under the actual diagnostic-manual approval. Exit1
  without timeout, signal or forced termination; success admission rejected.
  Normal HTTP/CAR/private install/login/launch/client/liveness and owned
  cleanup passed. One owned AXWindow and Summary text were captured.
  Dashboard, Refresh and Quit matching counts were all zero. Diagnostic
  byte budget expired before attributes of final group children were
  retained, although original traversal completed. Cause remains unknown;
  exact UI/API comparison and actual Dashboard action remain unverified.
  Full native originals, failures, Step7/Phase0 and old3of3 are retained.
  Independent read-only evidence investigation follows; no runtime replay.
  Human r19 response is clicked-not/does-not-remember; no affirmative
  click/hold or physical-absence conclusion follows. Independent retained-
  evidence investigation confirms zero matching controls and unresolved
  final sibling attributes. Same epoch2 passive preparation now consolidates
  node/action fields, abbreviates diagnostic keys with a legend and records
  original already-read selector values. First size preflight insufficiency
  is retained; final syntax/whitespace/typed candidate and fresh complete
  six-path source preparation review PASS, admitted as review33. Output
  caps, acceptance/error/success behavior and owned lifecycle remain.
  Actual capture completeness and UI cause remain unverified; source PASS
  is no native/Step/Phase acceptance. No further host run occurred.
  Fresh precise once-only host answer is pending; all old counters and
  consumed approvals/failures are retained, with no reusable exception.
  r20 ran once under the actual compact-diagnostic approval. Exit1 without
  timeout/signal/force; normal transport/CAR/install/login/client/liveness
  and owned cleanup passed, success admission rejected. All26 visited
  nodes were recorded without byte-budget exhaustion. Three enabled
  immediate AXButtons advertise AXPress but expose empty original selector
  names/values and missing title/description; all matching counts remain0.
  Missing attributes keep diagnostic complete=false. Human reports icon
  click with closed/other display alternative, then clarifies no further
  screen click; that extra click was not required. Hold timing and cause
  remain unresolved. Exact UI/API comparison and actual Dashboard action
  remain unverified. All native originals/counters/failures preserved.
- Phase review and release: NOT RUN. Repository-complete validation remains
  explicitly deferred to PHASE-5.1; Phase 5.1 is not started.
