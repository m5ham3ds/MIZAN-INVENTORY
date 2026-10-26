# PHASE 02C — Navigation Screen Initialization Crash Fix

## Observed failure

The device crash report shows:

`java.lang.NullPointerException: Attempt to invoke virtual method 'java.lang.String com.sulo.mizan.inventory.core.navigation.Screen.getRoute()' on a null object reference`

The reported frame is `MainActivityKt.MizanApp` during initial Compose composition.

## Root cause identified

`Screen.bottomNavItems` was an eager companion property:

```kotlin
val bottomNavItems = listOf(Dashboard, Inventory, Operations, Reports, Settings)
```

Accessing a nested Kotlin `data object`, such as `Screen.Dashboard`, can cause the superclass/companion initialization sequence to run first. Eagerly constructing the companion list at that moment risks reading a nested object's `INSTANCE` before the object initialization completes. That can leave a null entry in the list. `MizanApp` then iterates/searches the list and evaluates `screen.route`, matching the reported NPE.

## Change applied

- Changed `bottomNavItems` to a thread-safe lazy property, so the list is constructed only after companion/class initialization has completed.
- Added `ScreenNavigationInitializationTest.kt` to verify the startup access order, all five initialized navigation objects, nonblank and unique routes, and the separate currencies route.
- No database, business logic, package name, or Gradle versions were changed.

## Verification status

- Reproduced the original failure pattern with a standalone Kotlin program using the same eager `sealed class` object list: first access printed `eager=[null, B, C]`, and dereferencing the first route threw `NullPointerException`.
- Changed only the list initialization strategy to `by lazy(LazyThreadSafetyMode.SYNCHRONIZED)`; the same standalone reproduction then returned all initialized objects and completed without error.
- Added `ScreenNavigationInitializationTest.kt` to protect the real application against regression.
- Parsed all 10 Android XML resources successfully.
- The final project archive integrity is checked in `PHASES/PROJECT_FINAL_AUDIT_AND_PHASE_STATUS.md`.
- A full Gradle/Android build has not been verified in this environment because an Android SDK and a cached Gradle distribution are not available here. The new regression test should run in GitHub Actions with `:app:testDebugUnitTest`.


## Final audit update — 2026-10-24

- The user confirmed that after applying this change, the application opened and the startup crash was resolved.
- The regression harness compiled the actual `Screen.kt` source with lightweight stubs for Android/Compose-only types and passed 9 assertions covering access order, destinations, routes, uniqueness, and the currencies destination.
- The full Android Gradle suite was not re-run locally; the environment lacks Android SDK configuration and the user explicitly requested no local APK build.
- The product PHASE 03 remains the inventory/warehouses/stock-movements feature set. This diagnostics bug-fix phase is named PHASE 02C to avoid incorrectly implying that the product inventory phase is complete.
