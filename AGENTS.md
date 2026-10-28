
# WayMate — Agent Instructions

## Project Goal
WayMate is a personal educational Android project.
The initial goal is a stable, testable Android application
with a clear architecture and a reproducible build.

## Current Technology
- Android native application
- Kotlin
- Jetpack Compose
- Gradle Kotlin DSL
- Android Studio
- Git for version control

## Backend Direction
- Cloudflare Workers for the API
- Cloudflare D1 for structured persistent data
- Do not introduce Cloudflare R2.
- Do not add backend code until the Android baseline is verified.
- Keep backend code separate from the Android app.

## Development Rules
- Inspect existing files before modifying them.
- Make small, focused changes.
- Do not rewrite unrelated files.
- Do not upgrade dependencies without a clear reason.
- Do not invent APIs, SDK versions, or build results.
- Never claim tests passed unless they were actually run.
- Preserve the ability to build and run the app after each milestone.
- Add tests for meaningful business logic.
- Document significant architectural decisions.

## Security and Privacy
- Never hardcode credentials, API tokens, or private keys.
- Never commit secrets or local machine configuration.
- Enforce authorization on the server, not only in the UI.
- Use synthetic test data during development.
- Request location permission only when needed and explain why.
- Make location sharing visible and controllable by the user.
- Minimize retention of sensitive data.

## External Dependencies
- Check maintenance status, compatibility, and licensing
  before adding a dependency.
- Prefer established libraries with clear documentation.
- Record important dependency and architecture decisions.

## Before Finishing a Task
1. Summarize the changes.
2. List the files changed.
3. Report the exact build and test commands executed.
4. Report actual results and any unresolved issues.
5. Do not silently skip failing tests.
