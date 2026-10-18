<!-- cncf-phase-finding:HYG-CC5-WORKFLOW-STORAGE-01:begin -->
Pre-existing exact CLI specification names describe rejection where their expectations deliberately accept mutable input. test_changed_executable_is_rejected at common/codex/skills/cozy-command-execution/scripts/test/test_execute_exact_cli.py:268 expects exit 0; the substitution scenario at line 239 also accepts several substitutions. Rename these scenarios to describe their actual supported behavior in a separate specification-maintenance task. This storage change did not introduce the names or alter those expectations. Risk: misleading executable documentation; nonblocking.
<!-- cncf-phase-finding:HYG-CC5-WORKFLOW-STORAGE-01:end -->

<!-- cncf-phase-finding:HYG-CC5-WORKFLOW-STORAGE-02:begin -->
Several pre-existing scenario bodies in the exact CLI and session specifications lack complete adjacent Given/When/Then clauses. Examples are test_execute_exact_cli.py:239,261,268,304 and test_command_runner_session.py:236,258,271,290. Complete the semantic clauses in a separate specification-maintenance task without changing behavior. The seven scenarios authored or materially changed for project storage have adjacent semantic clauses and do not have this authoring failure. Risk: existing specification readability debt; nonblocking.
<!-- cncf-phase-finding:HYG-CC5-WORKFLOW-STORAGE-02:end -->
