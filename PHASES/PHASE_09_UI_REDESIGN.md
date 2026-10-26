# PHASE 09 — Mizan Inventory Visual Redesign

## Objective
Replace the inconsistent, developer-oriented presentation with a coherent, polished Arabic-first Material 3 visual system while preserving inventory, invoice, exchange-rate, reporting, backup, and persistence behavior.

## Visual system
- Botanical teal primary palette, slate secondary palette, and restrained amber accent.
- Coordinated light/dark schemes and consistent rounded shapes.
- Clear typography hierarchy for page titles, section titles, data labels, and financial values.
- Shared branded app bar and refined bottom navigation.
- Consistent reusable page headers, section cards, text fields, dropdowns, and action buttons.

## Page updates
- Dashboard: clearer live-business KPI tiles, separated financial indicators, a dedicated receivable/payable summary, and more user-facing Arabic labels.
- Inventory: dedicated inventory page heading and consistent section/form cards.
- Operations: dedicated invoice/returns heading and consistent entry controls.
- Reports: dedicated financial analytics heading and consistent summary cards.
- Settings: clear settings heading and softened store/currency surfaces.
- Currency: rounded, inset currency and reference-rate panels.

## Behavior and data safety
This phase is intended as a UI/presentation update. It does not alter repository APIs, Room entities or migrations, financial calculations, invoice posting logic, exchange-rate semantics, or backup/restore data format. Existing actions and test tags are retained.

## Verification status
- Static checks passed for duplicate imports, unexpected control characters, required design components, and balanced braces/parentheses in changed Kotlin files.
- The Android Gradle compile could not run in this environment because the Gradle wrapper distribution is not cached and DNS/network access to `services.gradle.org` failed. Run the project's GitHub Actions build before publishing or distributing an APK.
