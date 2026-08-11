# Standalone Assembly and State

## Purpose

The standalone Control Center assembly is the default local deployment profile.
It supplies one installation-scoped operator subject, one machine registration
provider, and an application datastore managed through the supported CNCF
component datastore policy. It does not introduce a standalone-only managed
Subsystem entity or access another component's datastore.

## Datastore Boundary

The assembly declares the `local-default` application datastore policy for the
canonical derived selector `org.simplemodeling.textus.control-center`. CNCF
resolves the local data location through its standard component datastore
mechanism. The registry uses that application datastore through the existing
entity/Operation path, so a restarted standalone runtime opens the same accepted
registrations.

Tests supply an isolated target-owned SQLite file through the normal
`textus.local-data.org.simplemodeling.textus.control-center.application.path`
parameter. Production standalone configuration must use the corresponding
machine-local state path under the Control Center locator root; tests never
create user-home state.

When an initialized standalone installation is reused, bootstrap atomically
migrates only the legacy Control Center datastore policy and application-path
keys to these canonical keys. The migration preserves the installation identity
and launcher credential; canonical keys remain authoritative.

## Assembly Boundary

The packaged descriptor and `conf/cncf/assembly-standalone.yaml` describe the
same standalone profile. A future control-plane assembly must require an
explicit external datastore and human identity provider. It must not silently
reuse the standalone local operator, credential, or datastore policy.
