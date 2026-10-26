package com.sulo.mizan.inventory.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Main Room database instance for Mizan Inventory.
 * Pre-configured with TypeConverters for BigDecimal, Currency, and RateType.
 *
 * Database Version History:
 * - Version 1: Initial SystemConfigEntity
 * - Version 2 (PHASE 2): Added CurrencyConfigEntity and ExchangeRateEntity with Migration 1->2
 * - Version 3 (PHASES 3-7): Inventory, invoices, returns, cash, expenses, and stocktakes
 * - Version 4 (PHASES 4-5): Track customer refunds and supplier refunds separately from returns
 */
@Database(
    entities = [
        SystemConfigEntity::class,
        CurrencyConfigEntity::class,
        ExchangeRateEntity::class,
        WarehouseEntity::class,
        ProductEntity::class,
        StockMovementEntity::class,
        ShipmentEntity::class,
        PurchaseInvoiceEntity::class,
        PurchaseInvoiceLineEntity::class,
        SalesInvoiceEntity::class,
        SalesInvoiceLineEntity::class,
        SalesReturnEntity::class,
        PurchaseReturnEntity::class,
        ExpenseEntity::class,
        CashAccountEntity::class,
        CashTransactionEntity::class,
        StocktakeEntity::class,
        StocktakeLineEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun systemConfigDao(): SystemConfigDao
    abstract fun currencyConfigDao(): CurrencyConfigDao
    abstract fun exchangeRateDao(): ExchangeRateDao
    abstract fun commerceDao(): CommerceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration from Version 1 (SystemConfigEntity only)
         * to Version 2 (Added currency_configs and exchange_rates tables).
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create currency_configs table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `currency_configs` (
                        `code` TEXT NOT NULL,
                        `displayNameAr` TEXT NOT NULL,
                        `displayNameEn` TEXT NOT NULL,
                        `symbol` TEXT NOT NULL,
                        `decimals` INTEGER NOT NULL,
                        `isBase` INTEGER NOT NULL,
                        `isActive` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`code`)
                    )
                    """.trimIndent()
                )

                // 2. Pre-seed default currencies: SDG (Base), USD, GBP
                val now = System.currentTimeMillis()
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `currency_configs` 
                    (`code`, `displayNameAr`, `displayNameEn`, `symbol`, `decimals`, `isBase`, `isActive`, `sortOrder`, `updatedAt`)
                    VALUES 
                        ('SDG', 'جنيه سوداني', 'Sudanese Pound', 'ج.س', 2, 1, 1, 1, $now),
                        ('USD', 'دولار أمريكي', 'US Dollar', '$', 2, 0, 1, 2, $now),
                        ('GBP', 'جنيه إسترليني', 'British Pound', '£', 2, 0, 1, 3, $now)
                    """.trimIndent()
                )

                // 3. Create exchange_rates table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exchange_rates` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `fromCurrencyCode` TEXT NOT NULL,
                        `toCurrencyCode` TEXT NOT NULL,
                        `rate` TEXT NOT NULL,
                        `rateType` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL,
                        `sourceOrNote` TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                // 4. Create indices for exchange_rates
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exchange_rates_fromCurrencyCode_toCurrencyCode` ON `exchange_rates` (`fromCurrencyCode`, `toCurrencyCode`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exchange_rates_timestamp` ON `exchange_rates` (`timestamp`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exchange_rates_rateType` ON `exchange_rates` (`rateType`)"
                )
            }
        }

        /** Phase 3-7 schema: inventory, commerce, expenses, cash, returns, and stocktaking. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val tables = listOf(
                    """CREATE TABLE IF NOT EXISTS `warehouses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `location` TEXT NOT NULL, `active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)""",
                    """CREATE TABLE IF NOT EXISTS `products` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `sku` TEXT NOT NULL, `barcode` TEXT, `name` TEXT NOT NULL, `category` TEXT NOT NULL, `unitName` TEXT NOT NULL, `minimumStock` TEXT NOT NULL, `averageCostAmount` TEXT NOT NULL, `valuationCurrencyCode` TEXT NOT NULL, `replacementCostAmount` TEXT NOT NULL, `sellingPriceAmount` TEXT NOT NULL, `priceCurrencyCode` TEXT NOT NULL, `active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)""",
                    """CREATE TABLE IF NOT EXISTS `stock_movements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `productId` INTEGER NOT NULL, `warehouseId` INTEGER NOT NULL, `movementType` TEXT NOT NULL, `quantity` TEXT NOT NULL, `unitCostAmount` TEXT NOT NULL, `currencyCode` TEXT NOT NULL, `sourceType` TEXT NOT NULL, `sourceId` INTEGER NOT NULL, `note` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL)""",
                    """CREATE TABLE IF NOT EXISTS `shipments` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `trackingNumber` TEXT NOT NULL, `description` TEXT NOT NULL, `supplierName` TEXT NOT NULL, `costAmount` TEXT NOT NULL, `currencyCode` TEXT NOT NULL, `exchangeRateToBase` TEXT NOT NULL, `costBaseAmount` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)""",
                    """CREATE TABLE IF NOT EXISTS `purchase_invoices` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `invoiceNumber` TEXT NOT NULL, `supplierName` TEXT NOT NULL, `warehouseId` INTEGER NOT NULL, `shipmentId` INTEGER, `currencyCode` TEXT NOT NULL, `exchangeRateToBase` TEXT NOT NULL, `subtotalAmount` TEXT NOT NULL, `shippingAmount` TEXT NOT NULL, `totalAmount` TEXT NOT NULL, `totalBaseAmount` TEXT NOT NULL, `paidAmount` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)""",
                    """CREATE TABLE IF NOT EXISTS `purchase_invoice_lines` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `invoiceId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `quantity` TEXT NOT NULL, `unitCostAmount` TEXT NOT NULL, `lineTotalAmount` TEXT NOT NULL, `unitCostBaseAmount` TEXT NOT NULL, `returnedQuantity` TEXT NOT NULL)""",
                    """CREATE TABLE IF NOT EXISTS `sales_invoices` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `invoiceNumber` TEXT NOT NULL, `customerName` TEXT NOT NULL, `warehouseId` INTEGER NOT NULL, `currencyCode` TEXT NOT NULL, `exchangeRateToBase` TEXT NOT NULL, `subtotalAmount` TEXT NOT NULL, `discountAmount` TEXT NOT NULL, `taxAmount` TEXT NOT NULL, `totalAmount` TEXT NOT NULL, `totalBaseAmount` TEXT NOT NULL, `paidAmount` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)""",
                    """CREATE TABLE IF NOT EXISTS `sales_invoice_lines` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `invoiceId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `quantity` TEXT NOT NULL, `unitPriceAmount` TEXT NOT NULL, `lineTotalAmount` TEXT NOT NULL, `revenueBaseAmount` TEXT NOT NULL, `unitCostBaseAmount` TEXT NOT NULL, `costTotalBaseAmount` TEXT NOT NULL, `returnedQuantity` TEXT NOT NULL)""",
                    """CREATE TABLE IF NOT EXISTS `sales_returns` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `invoiceLineId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `warehouseId` INTEGER NOT NULL, `quantity` TEXT NOT NULL, `amount` TEXT NOT NULL, `currencyCode` TEXT NOT NULL, `exchangeRateToBase` TEXT NOT NULL, `amountBase` TEXT NOT NULL, `costRestoredBaseAmount` TEXT NOT NULL, `note` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL)""",
                    """CREATE TABLE IF NOT EXISTS `purchase_returns` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `invoiceLineId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `warehouseId` INTEGER NOT NULL, `quantity` TEXT NOT NULL, `amount` TEXT NOT NULL, `currencyCode` TEXT NOT NULL, `exchangeRateToBase` TEXT NOT NULL, `amountBase` TEXT NOT NULL, `costRemovedBaseAmount` TEXT NOT NULL, `note` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL)""",
                    """CREATE TABLE IF NOT EXISTS `expenses` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, `description` TEXT NOT NULL, `amount` TEXT NOT NULL, `currencyCode` TEXT NOT NULL, `exchangeRateToBase` TEXT NOT NULL, `amountBase` TEXT NOT NULL, `paid` INTEGER NOT NULL, `cashAccountId` INTEGER, `occurredAt` INTEGER NOT NULL)""",
                    """CREATE TABLE IF NOT EXISTS `cash_accounts` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `currencyCode` TEXT NOT NULL, `openingBalanceAmount` TEXT NOT NULL, `active` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)""",
                    """CREATE TABLE IF NOT EXISTS `cash_transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `accountId` INTEGER NOT NULL, `direction` TEXT NOT NULL, `sourceType` TEXT NOT NULL, `sourceId` INTEGER NOT NULL, `amount` TEXT NOT NULL, `note` TEXT NOT NULL, `occurredAt` INTEGER NOT NULL)""",
                    """CREATE TABLE IF NOT EXISTS `stocktakes` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `reference` TEXT NOT NULL, `warehouseId` INTEGER NOT NULL, `status` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `finalizedAt` INTEGER NOT NULL)""",
                    """CREATE TABLE IF NOT EXISTS `stocktake_lines` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `stocktakeId` INTEGER NOT NULL, `productId` INTEGER NOT NULL, `expectedQuantity` TEXT NOT NULL, `countedQuantity` TEXT NOT NULL, `differenceQuantity` TEXT NOT NULL, `note` TEXT NOT NULL)"""
                )
                tables.forEach { sql -> db.execSQL(sql) }
                val indices = listOf(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_products_sku` ON `products` (`sku`)",
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_products_barcode` ON `products` (`barcode`)",
                    "CREATE INDEX IF NOT EXISTS `index_stock_movements_productId` ON `stock_movements` (`productId`)",
                    "CREATE INDEX IF NOT EXISTS `index_stock_movements_warehouseId` ON `stock_movements` (`warehouseId`)",
                    "CREATE INDEX IF NOT EXISTS `index_stock_movements_occurredAt` ON `stock_movements` (`occurredAt`)",
                    "CREATE INDEX IF NOT EXISTS `index_shipments_trackingNumber` ON `shipments` (`trackingNumber`)",
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_purchase_invoices_invoiceNumber` ON `purchase_invoices` (`invoiceNumber`)",
                    "CREATE INDEX IF NOT EXISTS `index_purchase_invoices_createdAt` ON `purchase_invoices` (`createdAt`)",
                    "CREATE INDEX IF NOT EXISTS `index_purchase_invoice_lines_invoiceId` ON `purchase_invoice_lines` (`invoiceId`)",
                    "CREATE INDEX IF NOT EXISTS `index_purchase_invoice_lines_productId` ON `purchase_invoice_lines` (`productId`)",
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_sales_invoices_invoiceNumber` ON `sales_invoices` (`invoiceNumber`)",
                    "CREATE INDEX IF NOT EXISTS `index_sales_invoices_createdAt` ON `sales_invoices` (`createdAt`)",
                    "CREATE INDEX IF NOT EXISTS `index_sales_invoice_lines_invoiceId` ON `sales_invoice_lines` (`invoiceId`)",
                    "CREATE INDEX IF NOT EXISTS `index_sales_invoice_lines_productId` ON `sales_invoice_lines` (`productId`)",
                    "CREATE INDEX IF NOT EXISTS `index_sales_returns_invoiceLineId` ON `sales_returns` (`invoiceLineId`)",
                    "CREATE INDEX IF NOT EXISTS `index_purchase_returns_invoiceLineId` ON `purchase_returns` (`invoiceLineId`)",
                    "CREATE INDEX IF NOT EXISTS `index_expenses_occurredAt` ON `expenses` (`occurredAt`)",
                    "CREATE INDEX IF NOT EXISTS `index_cash_accounts_currencyCode` ON `cash_accounts` (`currencyCode`)",
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_cash_accounts_name_currencyCode` ON `cash_accounts` (`name`, `currencyCode`)",
                    "CREATE INDEX IF NOT EXISTS `index_cash_transactions_accountId` ON `cash_transactions` (`accountId`)",
                    "CREATE INDEX IF NOT EXISTS `index_cash_transactions_occurredAt` ON `cash_transactions` (`occurredAt`)",
                    "CREATE INDEX IF NOT EXISTS `index_stocktakes_createdAt` ON `stocktakes` (`createdAt`)",
                    "CREATE INDEX IF NOT EXISTS `index_stocktake_lines_stocktakeId` ON `stocktake_lines` (`stocktakeId`)",
                    "CREATE INDEX IF NOT EXISTS `index_stocktake_lines_productId` ON `stocktake_lines` (`productId`)"
                )
                indices.forEach { sql -> db.execSQL(sql) }
                val now = System.currentTimeMillis()
                db.execSQL("INSERT OR IGNORE INTO `warehouses` (`id`, `name`, `location`, `active`, `createdAt`) VALUES (1, 'المستودع الرئيسي', '', 1, $now)")
                db.execSQL("INSERT OR IGNORE INTO `cash_accounts` (`id`, `name`, `currencyCode`, `openingBalanceAmount`, `active`, `createdAt`) VALUES (1, 'الصندوق الرئيسي', 'SDG', '0', 1, $now)")
            }
        }

        /** Tracks how much of a sales-return credit has been refunded and supplier credits received. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `sales_returns` ADD COLUMN `refundedAmount` TEXT NOT NULL DEFAULT '0'")
                db.execSQL("ALTER TABLE `purchase_returns` ADD COLUMN `refundReceivedAmount` TEXT NOT NULL DEFAULT '0'")
            }
        }

        private val CALLBACK = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                val now = System.currentTimeMillis()
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `currency_configs` 
                    (`code`, `displayNameAr`, `displayNameEn`, `symbol`, `decimals`, `isBase`, `isActive`, `sortOrder`, `updatedAt`)
                    VALUES 
                        ('SDG', 'جنيه سوداني', 'Sudanese Pound', 'ج.س', 2, 1, 1, 1, $now),
                        ('USD', 'دولار أمريكي', 'US Dollar', '$', 2, 0, 1, 2, $now),
                        ('GBP', 'جنيه إسترليني', 'British Pound', '£', 2, 0, 1, 3, $now)
                    """.trimIndent()
                )
                db.execSQL("INSERT OR IGNORE INTO `warehouses` (`id`, `name`, `location`, `active`, `createdAt`) VALUES (1, 'المستودع الرئيسي', '', 1, $now)")
                db.execSQL("INSERT OR IGNORE INTO `cash_accounts` (`id`, `name`, `currencyCode`, `openingBalanceAmount`, `active`, `createdAt`) VALUES (1, 'الصندوق الرئيسي', 'SDG', '0', 1, $now)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mizan_inventory.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(CALLBACK)
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
