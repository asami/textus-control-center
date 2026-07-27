status = current
scope = Textus official CAR/SAR default server ports

# Default Server Port Registry Specification

## 1. Responsibility

Textus Control Center owns the catalog of default server ports assigned to
official Textus CAR and SAR artifacts. CNCF owns the generic allocation and
runtime resolution policy.

The authored CAR/SAR definition is the source of each assignment:

- CAR: `project.yaml` at `project.component.config.textus.server.default-port`;
- legacy CAR without `project.yaml`: `component-descriptor.json` at
  `config.textus.server.default-port`;
- SAR: `subsystem-descriptor.yaml` at
  `config.textus.server.default-port`.

`~/.cncf/server-port-assignments.json` is a machine-local runtime mirror. It is
not the authoritative catalog.

## 2. Numbering Policy

- Textus Control Center is the official CAR anchor at `18000` (`base + 0`).
- Other official CAR ports start at `18001` and retain the established
  development order among those artifacts.
- Official SAR ports start at `28000` and increase independently by development
  order.
- Additional instances use the CNCF-managed dynamic range beginning at `38000`.
- Samples, fixtures, examples, generated files, and non-official CAR/SAR
  projects are not assigned here.
- Once an official assignment is published, it is stable. Removing an artifact
  does not implicitly renumber published successors.

For the initial catalog, development order is the oldest Git commit in each
repository. `textus-aws` has no commit history, so its directory creation time
is used.

## 3. Official Assignments

| Port | Kind | Artifact | Development start |
| ---: | --- | --- | --- |
| 18000 | CAR | `textus-control-center` | 2026-07-18 |
| 18001 | CAR | `textus-user-account` | 2026-03-22 |
| 18002 | CAR | `textus-ai-runtime` | 2026-04-05 |
| 18003 | CAR | `textus-aws` | 2026-04-28 |
| 18004 | CAR | `textus-blog` | 2026-04-29 |
| 18005 | CAR | `textus-user-notification` | 2026-05-07 |
| 18006 | CAR | `textus-semantic-integration-engine` | 2026-05-15 |
| 18007 | CAR | `textus-knowledge-editor` | 2026-05-21 |
| 18008 | CAR | `textus-sanpomap` | 2026-06-29 |
| 18009 | CAR | `textus-georesolver` | 2026-07-01 |
| 18010 | CAR | `textus-toolchain-runner` | 2026-07-02 |
| 18011 | CAR | `textus-art-scene` | 2026-07-06 |
| 18012 | CAR | `textus-scraper` | 2026-07-11 |
| 18013 | CAR | `textus-cbd-support` | 2026-07-14 |
| 28000 | SAR | `textus-identity` | 2026-03-26 |

`nict-knowledgehub` is not a Textus official CAR and has no assignment in this
registry.

## 4. Change Procedure

When adding an official artifact:

1. establish its official CAR or SAR status;
2. append the next number in the applicable range;
3. write the value to the artifact definition;
4. update this table;
5. synchronize the machine-local registry used for validation;
6. verify uniqueness and CNCF range compliance.

A port change is incomplete unless the artifact definition and this registry
agree. Runtime-only edits to `~/.cncf/server-port-assignments.json` do not change
the official assignment.
