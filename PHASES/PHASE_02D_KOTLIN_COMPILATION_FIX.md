# PHASE 02D — Kotlin Compilation Fix

## Source of failure
The GitHub Actions run `40141307655` failed at `:app:compileDebugKotlin` before unit tests could execute.

## Fixes applied
1. `MizanBackupService.kt`: imported `AppSettingsRepository` from `core.preferences`; made `exportTo` and `restoreFrom` return `Unit` explicitly so the coroutine block contract is clear.
2. `CommerceComponents.kt`: moved `onClick` to the last parameter of `CommerceActionButton`. This matches current Compose call sites using trailing-lambda syntax, including call sites that name `enabled`.
3. `ReportsScreen.kt`: removed heterogeneous `if (isSaleDebt) sales else purchases` list expressions. Each branch now projects its invoice entity type into common `List<Pair<String, String>>` options; selected invoice IDs are computed separately.
4. `BackupRestorePanel.kt`: removed redundant nested `withContext(Dispatchers.IO)` calls because `MizanBackupService` already dispatches its I/O internally.

## Verification performed
- A standalone Kotlin contract test compiled and ran successfully for the revised trailing-lambda button signature and invoice-option projection.
- Static source assertions passed for the missing import, explicit service return types, button signature, typed report projections, and backup panel calls.
- No Gradle task, Android compilation, unit-test task, or APK build was run in this environment. The next authoritative check is GitHub Actions on this patched version.
