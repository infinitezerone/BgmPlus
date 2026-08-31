# :core:navigation

## Scope & Responsibilities
- Centralizes Navigation 3 runtime contracts, route declarations (`NavKey`), and state management.
- Exposes `BgmNavState` (`rememberBgmNavState`) for multi-backstack orchestration, state restoration, and exit-through-home semantics.
- Exposes `TopLevelDestination` enums for top-level navigation bars, rails, and drawers.

## Dependency Topology
- **Depends on**: Jetpack Navigation 3 runtime/ui, Jetpack Compose Material 3, `kotlinx.serialization`.
- **Depended on by**: `:app`, all `:feature:*` modules, and optional presentation libraries.
- **Does NOT depend on**: `:app` or any `:feature:*` modules.

## Invariants & Redlines
1. Must not import or reference any concrete feature screens or ViewModels.
2. All route classes/objects must implement `androidx.navigation3.runtime.NavKey` and be `@Serializable`.
3. Navigation state management must support process death survival via `rememberSaveable`.
