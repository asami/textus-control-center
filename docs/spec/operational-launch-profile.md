status = draft
scope = Phase 4 safe standalone launch-profile resolution

# Operational Launch Profile Specification

## 1. Purpose

An operational launch profile is the resolved, safe input that allows a local
launcher supervisor to start one selected deployment of an
`OperationalComponent`. It is not an arbitrary shell command and it must not
contain credentials or unrestricted environment data.

## 2. Profile Variants

| Variant | Required locator | Start authority |
| --- | --- | --- |
| `development-directory` | validated configured development directory and `project.yaml` | canonical `cncf <directory> server` execution |
| `local-car` | validated local CAR archive identity and catalog metadata | canonical local CAR launcher execution |
| `public-car` | explicitly subscribed artifact with a resolved local usable archive | canonical local CAR launcher execution |

Public repository metadata alone is not executable. It must resolve to a local
archive through an explicit later installation path before the profile becomes
startable.

## 3. Port Resolution

The profile uses the artifact's declared default port from the authoritative
CAR descriptor. The standalone initial profile permits one selected default
port per operational component. It does not guess a dynamic port or reuse a
port currently owned by another active profile.

Before submitting Start or Restart, the supervisor validates source presence,
descriptor validity, runtime compatibility, dependency resolution, and port
availability. Failure returns a stable structured code and changes neither the
operating target nor existing instances.

## 4. Privacy and Projection

The main panel may show source kind, port, usability, and a short diagnostic.
Development directories, archive paths, configuration locators, credential
references, complete command lines, and environment values are protected
administrative detail only. Secrets are never accepted as profile fields.

## 5. Future Extension

A later multi-host profile adds an explicit host/supervisor identity and a
deployment identity. It must not reinterpret a standalone profile as a remote
execution request.
