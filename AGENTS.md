# BgmPlus AI Agent & Developer Guidelines

Architectural context, coding standards, and verification workflow for **BgmPlus** — a modern Bangumi (bgm.tv) on-air schedule, anime tracking, and collection management client. Modular Clean Architecture modeled on Google's *Now in Android*. Written for human developers and AI coding agents alike.

## Tech Stack

Exact versions live in `gradle/libs.versions.toml` and `build-logic` convention plugins — treat those as the source of truth. Only the constraints below affect everyday coding decisions:

- **Language**: Kotlin (K2); JVM target 25 with core library desugaring (governs the available `java.*` API surface)
- **SDK levels**: minSdk 31, compileSdk/targetSdk 37 (governs the available `android.*` APIs)
- **Multiplatform**: all `:core:*` modules are Kotlin Multiplatform (`commonMain`, androidTarget only — no desktop/iOS targets); `:app` and `:core:designsystem` are Android-only
- **UI**: Jetpack Compose + Material 3 Expressive
- **Stack**: Ktor 3 + kotlinx.serialization, Room 3 (KMP) + DataStore, Coil 3, Koin 4

## Module Map

```
BgmPlus/
├── app/                        # Entry point, MainActivity, Koin init, OAuth deep-link handling
├── build-logic/                # Convention plugins (bgmplus.* ids) — all shared module config lives here
├── core/
│   ├── model/                  # Pure Kotlin data classes (Subject, Episode, AirSchedule, ...)
│   ├── common/                 # AppResult, BgmDispatchers, TimeUtils
│   ├── network/                # Ktor dual-client setup, Bangumi REST v0 API, OAuth token refresh loop
│   ├── database/               # Room 3 entities, DAOs, BgmDatabase
│   ├── datastore/              # UserPreferences (commonMain); Keystore-encrypted AuthTokensDataSource (androidMain)
│   ├── data/                   # Repositories: AuthRepository, ScheduleRepository, SubjectRepository
│   ├── designsystem/           # BgmPlusTheme, M3 tokens, CoverImage
│   └── testing/                # Test doubles (Fake repositories), MainDispatcherRule, TestData
├── worker (moved)              # OAuth token-exchange proxy: maintained in a separate private Cloudflare Workers repo; deploys to bgmplus-auth.shadow2go.dpdns.org via its own CI
└── feature/                    # (Planned, not yet scaffolded — convention plugin bgmplus.android.feature is ready)
    ├── schedule/               # Weekly on-air schedule & countdown
    ├── subject/                # Subject detail & episode list
    ├── user/                   # Collections & profile
    └── search/                 # Search & tag discovery
```

## Module Rules

1. **Feature isolation**: `:feature:A` must never depend on `:feature:B`; inter-feature navigation goes through type-safe route contracts. Feature modules use the `bgmplus.android.feature` convention plugin.
2. **`:core:model` is pure Kotlin**: no `android.*` or UI framework dependencies.
3. **Single source of truth**: repositories in `:core:data` coordinate `:core:network` and `:core:database`; UI layers never touch network or database directly.
4. **Convention plugins first**: new modules apply a `bgmplus.*` plugin from the catalog instead of hand-configuring Android/Kotlin settings.
5. **Lightweight module docs**: module-level `README.md` files only declare **scope/responsibilities**, **dependency topology**, and **architectural invariants/redlines** — avoid internal implementation details or class inventories to prevent documentation rot.
6. **Navigation**: app uses **Navigation 3** (`androidx.navigation3`) — `@Serializable` `NavKey` routes declared under `:app` `navigation/`, rendered by `NavDisplay`; per-tab `NavBackStack`s live in `BgmNavState` (exit-through-home, survives process death). Don't relocate route contracts or rewire the navigation stack without a deliberate decision.

## Coding Guidelines

### Architecture & State
- Unidirectional Data Flow (MVI): ViewModels expose a single, immutable `StateFlow<UiState>`; one-off events (snackbars, navigation) go through `Channel`/`SharedFlow`.
- Wrap data/domain operations in `AppResult<T>` (`Success`/`Error`/`Loading`, defined in `:core:common`).

### Network (Ktor 3)
- Every `BgmHttpClient` request carries `User-Agent: BgmPlus/<versionName> (android) (https://github.com/infinitezerone/BgmPlus)` via `DefaultRequest` — never strip or override it; keep the version string in sync with the app's `versionName`.
- Reuse `BgmHttpClient.jsonConfig` (`ignoreUnknownKeys`, `isLenient`, `coerceInputValues`, ...) instead of hand-rolling `Json` instances.
- Errors surface as typed `BgmNetworkException` subclasses (`Unauthorized`, `Forbidden`, `NotFound`, `RateLimited`, `ServerError`, `Unknown`), mapped from HTTP status codes.
- Business API calls go direct to `api.bgm.tv`; only token exchange/refresh goes through the Cloudflare Worker proxy. The Ktor auth plugin auto-refreshes on 401 and clears credentials on unrecoverable refresh failures (auto-logout).
- OAuth code redemption is guarded by a Worker-enforced PKCE equivalent (bgm.tv lacks PKCE): `BgmPkce` generates a local `verifier` whose `sha256` fingerprint rides as the OAuth `state`; the exchange must carry `code + state + verifier` or the Worker rejects with `verifier_mismatch`. Never bypass or weaken this check.

### Persistence (Room 3 & DataStore)
- Room DAO reads return `Flow<T>`; writes/upserts are `suspend` functions.
- OAuth tokens never live in `UserPreferences` — they go through `AuthTokensDataSource` (AndroidKeyStore-encrypted, excluded from device backups).

### Build System (AGP 9)
- `:app` / `:core:designsystem` use **AGP built-in Kotlin** — never re-add `org.jetbrains.kotlin.android`; Kotlin compile config goes through `android.compileOptions` (jvmTarget defaults to `targetCompatibility`).
- KMP modules use `com.android.kotlin.multiplatform.library` (applied by `bgmplus.kmp.library`), which is **single-variant** (no debug/release) and has **no top-level `android {}` extension** — Android config (namespace, desugaring, host tests) goes through `Project.kmpAndroidLibrary { }` (finalizeDsl) in `build-logic`, and test source sets are named `androidHostTest` / `androidDeviceTest` with tests **opt-in** (`withHostTest` is already enabled in the convention plugin).
- The android unit test task is `testAndroid` (per module) or `allTests` (KMP aggregate); `testDebugUnitTest` no longer exists.
- **Green ≠ tested**: a misnamed or empty test source set fails silently (the `androidUnitTest` → `androidHostTest` incident shipped a build where tests ran zero cases, all green). After any build-script or source-set change, verify `build/test-results/<task>/*.xml` exists with `tests > 0` before claiming tests pass — BUILD SUCCESSFUL alone proves nothing.

### Compose & Design System
- Always build under `BgmPlusTheme` using tokens from `:core:designsystem`; no hardcoded colors or typography in features.
- Keep recomposition cheap: immutable state classes, `@Stable` where useful, stable lambdas.
- Support edge-to-edge: `enableEdgeToEdge()` plus proper `WindowInsets` padding.

## Build & Verification

```bash
./gradlew :app:assembleDebug                  # assemble debug APK — also the cross-module compile gate
./gradlew :core:network:testAndroid           # unit tests for ONE module; substitute the module you touched
./gradlew spotlessCheck                       # ktlint + whitespace gate (spotlessApply to auto-fix)
./gradlew allTests                             # FULL test suite — only for cross-cutting changes (see rule 2)
./gradlew clean                               # rarely needed
```

**Rules for AI agents:**

1. **Verify before claiming**: the default loop for everyday changes is targeted, not full-suite — `./gradlew spotlessCheck`, then tests for each touched module (`./gradlew <module>:testAndroid`), then `./gradlew :app:assembleDebug`. Report failures honestly.
2. **Full `./gradlew allTests` only for cross-cutting changes**: touching `build-logic/`, `gradle/libs.versions.toml`, or the shared bases `:core:model` / `:core:common` (everything depends on them), and before opening a PR.
3. **Declare dependencies in the catalog first**: add versions/libraries/plugins to `gradle/libs.versions.toml`, then reference them via type-safe accessors (`libs.xxx`).
4. **Respect module boundaries**: no circular dependencies; never violate feature isolation (see Module Rules).

## Git & Commits

- Conventional Commits with module scope: `feat(core:datastore): ...`, `fix(app): ...`, `build: ...`.
- One commit = one purpose: don't mix unrelated reformatting or churn into a functional change.
