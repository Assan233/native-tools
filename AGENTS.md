# Agent Instructions

## Project Context

- This repository is a personal Android native toolkit for automating repetitive phone tasks and improving daily efficiency.
- The maintainer is an experienced frontend developer but is not yet familiar with Android native development.
- Use Kotlin and Jetpack Compose unless an existing feature has a concrete reason to use another approach.
- The app currently supports Android 8.0+ (`minSdk = 26`) and targets Android 14 (`targetSdk = 34`).

## Communication

- Explain Android-specific concepts, lifecycle implications, permissions, and system constraints in frontend terms where that helps.
- Do not assume knowledge of Gradle, Activities, Services, Intents, Binder, lifecycle, or Android permission models.
- Keep explanations concise and practical. Include the exact file and command relevant to the change.
- When multiple native approaches are valid, recommend one and state the tradeoff rather than presenting an unexplained choice.
- Explicitly call out steps that must be completed in Android Studio, on an emulator, or on a physical device.

## Engineering Rules

- Follow the Kotlin coding conventions and Android's official architecture guidance.
- Prefer the smallest correct implementation. Do not introduce layers, modules, interfaces, or dependencies without a current need.
- Use Compose with unidirectional data flow. Hoist state when it must be shared or controlled by a caller.
- Keep UI, state coordination, domain logic, and Android system integration separate once a feature is large enough to benefit from those boundaries.
- Use `ViewModel` and `StateFlow` for non-trivial screen state. Do not store durable state only in an `Activity`, `Service`, or Composable.
- Use structured concurrency with Kotlin Coroutines. Never block the main thread with network, file, database, shell, or long-running work.
- Prefer immutable data and explicit state models. Avoid global mutable state and singleton service locators.
- Put user-visible text in Android string resources. Add accessibility semantics or content descriptions to interactive UI where needed.
- Handle configuration changes, process recreation, denied or revoked permissions, and unavailable system services when relevant.
- Add unit tests for business logic and focused instrumentation tests for critical Android integrations.
- Run `./gradlew test lint assembleDebug` after meaningful changes when the local environment allows it.

## Automation And Permissions

- Treat Accessibility Service, notification access, overlays, screen capture, background execution, and device administration as sensitive capabilities.
- Never add a sensitive permission or special service silently. Explain why it is required, its user-visible impact, and a lower-privilege alternative if one exists.
- Automation must be user-initiated, visibly active, cancellable, and bounded. It must fail safely when the target UI differs from expectations.
- Prefer semantic selectors such as package name, view ID, text, and content description over fixed screen coordinates.
- Do not automate payments, passwords, one-time codes, account security flows, or bypass platform protections.
- Keep private on-device data local unless the user explicitly requests networking and understands what will be transmitted.
- Do not use hidden APIs, root-only behavior, or ADB as an in-app production dependency unless the task explicitly requires a development-only tool and documents the limitation.

## Dependencies And Build

- Manage dependency and plugin versions in `gradle/libs.versions.toml`.
- Prefer AndroidX and actively maintained official libraries. Justify every new third-party dependency.
- Never commit `local.properties`, signing keys, API keys, tokens, device identifiers, or generated build output.
- Keep Debug and Release behavior aligned unless a difference is intentional and documented.
- Do not raise `minSdk`, `targetSdk`, `compileSdk`, AGP, Gradle, Kotlin, or Java versions as a side effect of an unrelated change.

## Feature Organization

- Keep the single `app` module until build time, ownership, or reuse creates a concrete reason to split it.
- Group growing code by feature, for example `features/autoclick/`, rather than by generic technical buckets alone.
- A typical non-trivial feature may contain a screen, a `ViewModel`, immutable UI state/events, and a repository or Android integration class. Omit any part that does not add value.
- Document manual setup and device-specific verification for system integrations in `README.md` or `docs/`.
