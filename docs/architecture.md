
# WayMate Architecture

## Status
Initial architecture proposal.
This document must be updated when decisions change.

## Android Client
- Kotlin and Jetpack Compose.
- Keep UI, domain logic, and data access separated.
- Add local persistence only when required by an implemented feature.
- Keep network requests outside UI components.

## Backend
- Cloudflare Workers: HTTP API and request validation.
- Cloudflare D1: structured persistent application data.
- Cloudflare R2: not used in the current plan.
- Real-time transport: not selected yet.
- File and image storage: not selected yet.

## Security
- Validate requests on the server.
- Apply authorization to every protected resource.
- Keep credentials outside source control.
- Use synthetic data for initial demonstrations.

## Offline Support
Offline storage and synchronization are planned features.
Conflict resolution, retry policy, and idempotency
must be designed before implementing synchronization.

## Mapping
The map SDK and tile provider are not selected yet.
Online and offline map sources must have compatible
licenses and documented usage restrictions.

## Decisions Pending
- Final Android SDK and dependency versions.
- Map library and tile provider.
- Authentication and session design.
- Real-time communication mechanism.
- Local encrypted storage strategy.
- File and image storage strategy.
- Backend deployment and quota validation.
