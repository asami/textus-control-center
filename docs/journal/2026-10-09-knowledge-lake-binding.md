# Knowledge Lake binding

- Date: 2026-10-09
- Status: Current design decision

This component is bound to the simplemodeling.org BoK Knowledge Lake through `.textus/knowledge-lake.yaml`.

The repository stores only the provider-neutral logical binding:

```text
textus:klake:simplemodeling.org
base: components/textus-control-center
```

Material references use the TKL canonical form:

```text
textus:material:<lake>:<logical-path>
```

Example:

```text
textus:material:simplemodeling.org:components/textus-control-center/journal/2026/10/2026-10-09-ai-operations-architecture
```

Google Drive folder IDs are adapter/resolver concerns and are intentionally not stored in the repository binding.
