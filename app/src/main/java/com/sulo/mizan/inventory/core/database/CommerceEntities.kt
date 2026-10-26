package com.sulo.mizan.inventory.core.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** All monetary and quantity amounts are stored as decimal strings, never SQLite REAL. */
@Entity(tableName = "warehouses")
data class WarehouseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val location: String = "",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "products",
    indices = [Index(value = ["sku"], unique = true), Index(value = ["barcode"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sku: String,
    val barcode: String? = null,
    val name: String,
    val category: String = "عام",
    val unitName: String = "قطعة",
    val minimumStock: String = "0",
    val averageCostAmount: String = "0",
    val valuationCurrencyCode: String = "SDG",
    val replacementCostAmount: String = "0",
    val sellingPriceAmount: String = "0",
    val priceCurrencyCode: String = "SDG",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "stock_movements",
    indices = [Index(value = ["productId"]), Index(value = ["warehouseId"]), Index(value = ["occurredAt"])]
)
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val warehouseId: Long,
    /** OPENING, PURCHASE, SALE, SALE_RETURN, PURCHASE_RETURN, ADJUSTMENT_IN/OUT, TRANSFER_IN/OUT */
    val movementType: String,
    val quantity: String,
    val unitCostAmount: String,
    val currencyCode: String,
    val sourceType: String = "MANUAL",
    val sourceId: Long = 0,
    val note: String = "",
    val occurredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "shipments", indices = [Index(value = ["trackingNumber"])])
data class ShipmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val trackingNumber: String,
    val description: String = "",
    val supplierName: String = "",
    val costAmount: String = "0",
    val currencyCode: String = "SDG",
    val exchangeRateToBase: String = "1",
    val costBaseAmount: String = "0",
    val status: String = "RECEIVED",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchase_invoices", indices = [Index(value = ["invoiceNumber"], unique = true), Index(value = ["createdAt"])])
data class PurchaseInvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val supplierName: String,
    val warehouseId: Long,
    val shipmentId: Long? = null,
    val currencyCode: String,
    val exchangeRateToBase: String,
    val subtotalAmount: String,
    val shippingAmount: String,
    val totalAmount: String,
    val totalBaseAmount: String,
    val paidAmount: String,
    val status: String = "POSTED",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchase_invoice_lines", indices = [Index(value = ["invoiceId"]), Index(value = ["productId"])])
data class PurchaseInvoiceLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val productId: Long,
    val quantity: String,
    val unitCostAmount: String,
    val lineTotalAmount: String,
    /** Landed unit cost in the valuation/base currency, including allocated freight. */
    val unitCostBaseAmount: String,
    val returnedQuantity: String = "0"
)

@Entity(tableName = "sales_invoices", indices = [Index(value = ["invoiceNumber"], unique = true), Index(value = ["createdAt"])])
data class SalesInvoiceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceNumber: String,
    val customerName: String,
    val warehouseId: Long,
    val currencyCode: String,
    val exchangeRateToBase: String,
    val subtotalAmount: String,
    val discountAmount: String,
    val taxAmount: String,
    val totalAmount: String,
    val totalBaseAmount: String,
    val paidAmount: String,
    val status: String = "POSTED",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales_invoice_lines", indices = [Index(value = ["invoiceId"]), Index(value = ["productId"])])
data class SalesInvoiceLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val productId: Long,
    val quantity: String,
    val unitPriceAmount: String,
    val lineTotalAmount: String,
    val revenueBaseAmount: String,
    val unitCostBaseAmount: String,
    val costTotalBaseAmount: String,
    val returnedQuantity: String = "0"
)

@Entity(tableName = "sales_returns", indices = [Index(value = ["invoiceLineId"])])
data class SalesReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceLineId: Long,
    val productId: Long,
    val warehouseId: Long,
    val quantity: String,
    val amount: String,
    val currencyCode: String,
    val exchangeRateToBase: String,
    val amountBase: String,
    val costRestoredBaseAmount: String,
    @ColumnInfo(defaultValue = "'0'") val refundedAmount: String = "0",
    val note: String = "",
    val occurredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchase_returns", indices = [Index(value = ["invoiceLineId"])])
data class PurchaseReturnEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceLineId: Long,
    val productId: Long,
    val warehouseId: Long,
    val quantity: String,
    val amount: String,
    val currencyCode: String,
    val exchangeRateToBase: String,
    val amountBase: String,
    val costRemovedBaseAmount: String,
    @ColumnInfo(defaultValue = "'0'") val refundReceivedAmount: String = "0",
    val note: String = "",
    val occurredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses", indices = [Index(value = ["occurredAt"])])
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val description: String,
    val amount: String,
    val currencyCode: String,
    val exchangeRateToBase: String,
    val amountBase: String,
    val paid: Boolean,
    val cashAccountId: Long? = null,
    val occurredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cash_accounts", indices = [Index(value = ["currencyCode"]), Index(value = ["name", "currencyCode"], unique = true)])
data class CashAccountEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val currencyCode: String,
    val openingBalanceAmount: String = "0",
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cash_transactions", indices = [Index(value = ["accountId"]), Index(value = ["occurredAt"])])
data class CashTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val accountId: Long,
    /** CREDIT or DEBIT; amount is always stored positive. */
    val direction: String,
    val sourceType: String = "MANUAL",
    val sourceId: Long = 0,
    val amount: String,
    val note: String = "",
    val occurredAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stocktakes", indices = [Index(value = ["createdAt"])])
data class StocktakeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val warehouseId: Long,
    val status: String = "FINALIZED",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val finalizedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "stocktake_lines", indices = [Index(value = ["stocktakeId"]), Index(value = ["productId"])])
data class StocktakeLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val stocktakeId: Long,
    val productId: Long,
    val expectedQuantity: String,
    val countedQuantity: String,
    val differenceQuantity: String,
    val note: String = ""
)
