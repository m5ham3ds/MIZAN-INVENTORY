# PHASE 09 — Full UI Layout Redesign

## Goal
Replace the former long, vertically stacked page composition with an information hierarchy that makes the primary tasks visible and keeps section navigation available while content scrolls. This phase changes layout, spacing, component geometry and information grouping, not only colors.

## Screen changes
- **Dashboard:** compact heading, two-by-two quick-action grid for inventory, invoices, reports and currencies, followed by the live business summary, a compact system-reliability strip and the existing financial precision tester. Existing test tags remain in place.
- **Inventory:** fixed heading, stock KPIs and tabs for balances, product creation, warehouses and stock tools. The list below the tabs scrolls independently. Pricing updates, inter-warehouse transfers and stocktaking are grouped under stock tools.
- **Operations:** fixed heading, purchase/sales/draft-line KPIs and tabs for a new invoice, invoice history, returns and shipments. Existing invoice posting, refund/return and shipment logic is retained.
- **Reports:** fixed heading, quick financial metrics and tabs for summary, receivables/payables/refunds, cash accounts and expenses. Expense creation and the expense register stay in the expenses tab; settlement remains in debt/refund workflows.
- **Settings:** fixed heading/tabs with a separate scroll area for general information, currency, backup/restore and diagnostics.
- **Currencies:** compact shared page heading and shorter tab labels. The reference-rate update card appears in the exchange-rate tab instead of occupying space above every tab.
- **Shared components:** redesigned section headers, reduced card emphasis, consistent field geometry, clearer dropdown labels and selected values, compact page headers and a lighter bottom-navigation separator.

## Scope guard
This phase does not intentionally change the database schema, repository rules, inventory calculations, invoice posting, exchange-rate semantics, cash accounting or backup/restore service. The existing financial-engine test controls and automation tags are preserved.

## Validation
Kotlin source files passed a PSI syntax parse. Static checks confirmed the fixed tabs, page groups, retained invoice/return workflows, report expense list and dashboard test tags. A Gradle compile could not be run because the configured Gradle 9.8.2 wrapper distribution could not be downloaded in this environment (`UnknownHostException: services.gradle.org`). A successful Android build is therefore **not claimed**; run GitHub Actions to verify dependency/type compilation.
