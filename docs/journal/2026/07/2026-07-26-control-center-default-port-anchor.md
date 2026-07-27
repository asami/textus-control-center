# Textus Control Center Default Port Anchor
Date: 2026-07-26

Status: decided

## Context

Official Textus CARs use stable default ports as machine-local application
numbers. The original registry assigned ports from `18000` in development
order, which placed `textus-control-center` at `18013`.

The Control Center is the operational entry point for a Textus installation.
Operators start it first and then perform normal component discovery,
inspection, lifecycle control, and other routine operational work through the
Control Center.

## Decision

Assign `textus-control-center` the CAR base port `18000` (`base + 0`).

Renumber the existing official CARs that preceded it to `18001` through
`18013`, preserving their relative order. Existing and future assignments after
the current Control Center boundary remain stable unless another explicit
registry decision changes them.

## Rationale

Port `18000` is easy to remember and identifies the operational entry point
without consulting the registry. This supports the expected startup sequence:

1. start Textus Control Center on `18000`;
2. open the Control Center;
3. perform routine Textus operational work from that surface.

The decision prioritizes operational usability over retaining the original
development-order number for the Control Center.

## Consequences

- The authored `project.yaml` and packaged component descriptor for
  `textus-control-center` declare `18000`.
- The official registry in
  `docs/spec/default-server-port-registry.md` is the source of the revised
  assignments.
- Other official CAR definitions and active operational defaults must match the
  revised registry.
- Machine-local assignment mirrors may need reconciliation when adopting the
  revised artifact definitions; they do not override the authored registry.
- Explicit operator port overrides and the dynamic additional-instance range
  remain unchanged.
