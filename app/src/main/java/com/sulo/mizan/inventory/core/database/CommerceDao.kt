package com.sulo.mizan.inventory.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CommerceDao {
    @Query("SELECT * FROM warehouses ORDER BY active DESC, name") fun observeWarehouses(): Flow<List<WarehouseEntity>>
    @Query("SELECT * FROM warehouses ORDER BY active DESC, name") suspend fun getWarehouses(): List<WarehouseEntity>
    @Query("SELECT * FROM warehouses WHERE id = :id LIMIT 1") suspend fun getWarehouse(id: Long): WarehouseEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertWarehouse(entity: WarehouseEntity): Long
    @Update suspend fun updateWarehouse(entity: WarehouseEntity)
    @Query("DELETE FROM warehouses") suspend fun clearWarehouses()

    @Query("SELECT * FROM products WHERE active = 1 ORDER BY name COLLATE NOCASE") fun observeProducts(): Flow<List<ProductEntity>>
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE") suspend fun getProducts(): List<ProductEntity>
    @Query("SELECT * FROM products WHERE id = :id LIMIT 1") suspend fun getProduct(id: Long): ProductEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertProduct(entity: ProductEntity): Long
    @Update suspend fun updateProduct(entity: ProductEntity)
    @Query("DELETE FROM products") suspend fun clearProducts()

    @Query("SELECT * FROM stock_movements ORDER BY occurredAt DESC, id DESC") fun observeMovements(): Flow<List<StockMovementEntity>>
    @Query("SELECT * FROM stock_movements ORDER BY occurredAt DESC, id DESC") suspend fun getMovements(): List<StockMovementEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertMovement(entity: StockMovementEntity): Long
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertMovements(entities: List<StockMovementEntity>)
    @Query("DELETE FROM stock_movements") suspend fun clearMovements()

    @Query("SELECT * FROM shipments ORDER BY createdAt DESC") fun observeShipments(): Flow<List<ShipmentEntity>>
    @Query("SELECT * FROM shipments ORDER BY createdAt DESC") suspend fun getShipments(): List<ShipmentEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertShipment(entity: ShipmentEntity): Long
    @Query("DELETE FROM shipments") suspend fun clearShipments()

    @Query("SELECT * FROM purchase_invoices ORDER BY createdAt DESC") fun observePurchaseInvoices(): Flow<List<PurchaseInvoiceEntity>>
    @Query("SELECT * FROM purchase_invoices ORDER BY createdAt DESC") suspend fun getPurchaseInvoices(): List<PurchaseInvoiceEntity>
    @Query("SELECT * FROM purchase_invoices WHERE id = :id LIMIT 1") suspend fun getPurchaseInvoice(id: Long): PurchaseInvoiceEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertPurchaseInvoice(entity: PurchaseInvoiceEntity): Long
    @Update suspend fun updatePurchaseInvoice(entity: PurchaseInvoiceEntity)
    @Query("DELETE FROM purchase_invoices") suspend fun clearPurchaseInvoices()

    @Query("SELECT * FROM purchase_invoice_lines ORDER BY id") suspend fun getPurchaseLines(): List<PurchaseInvoiceLineEntity>
    @Query("SELECT * FROM purchase_invoice_lines ORDER BY id") fun observePurchaseLines(): Flow<List<PurchaseInvoiceLineEntity>>
    @Query("SELECT * FROM purchase_invoice_lines WHERE invoiceId = :invoiceId") suspend fun getPurchaseLines(invoiceId: Long): List<PurchaseInvoiceLineEntity>
    @Query("SELECT * FROM purchase_invoice_lines WHERE id = :id LIMIT 1") suspend fun getPurchaseLine(id: Long): PurchaseInvoiceLineEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertPurchaseLine(entity: PurchaseInvoiceLineEntity): Long
    @Update suspend fun updatePurchaseLine(entity: PurchaseInvoiceLineEntity)
    @Query("DELETE FROM purchase_invoice_lines") suspend fun clearPurchaseLines()

    @Query("SELECT * FROM sales_invoices ORDER BY createdAt DESC") fun observeSalesInvoices(): Flow<List<SalesInvoiceEntity>>
    @Query("SELECT * FROM sales_invoices ORDER BY createdAt DESC") suspend fun getSalesInvoices(): List<SalesInvoiceEntity>
    @Query("SELECT * FROM sales_invoices WHERE id = :id LIMIT 1") suspend fun getSalesInvoice(id: Long): SalesInvoiceEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSalesInvoice(entity: SalesInvoiceEntity): Long
    @Update suspend fun updateSalesInvoice(entity: SalesInvoiceEntity)
    @Query("DELETE FROM sales_invoices") suspend fun clearSalesInvoices()

    @Query("SELECT * FROM sales_invoice_lines ORDER BY id") suspend fun getSalesLines(): List<SalesInvoiceLineEntity>
    @Query("SELECT * FROM sales_invoice_lines ORDER BY id") fun observeSalesLines(): Flow<List<SalesInvoiceLineEntity>>
    @Query("SELECT * FROM sales_invoice_lines WHERE invoiceId = :invoiceId") suspend fun getSalesLines(invoiceId: Long): List<SalesInvoiceLineEntity>
    @Query("SELECT * FROM sales_invoice_lines WHERE id = :id LIMIT 1") suspend fun getSalesLine(id: Long): SalesInvoiceLineEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSalesLine(entity: SalesInvoiceLineEntity): Long
    @Update suspend fun updateSalesLine(entity: SalesInvoiceLineEntity)
    @Query("DELETE FROM sales_invoice_lines") suspend fun clearSalesLines()

    @Query("SELECT * FROM sales_returns ORDER BY occurredAt DESC") suspend fun getSalesReturns(): List<SalesReturnEntity>
    @Query("SELECT * FROM sales_returns WHERE id = :id LIMIT 1") suspend fun getSalesReturn(id: Long): SalesReturnEntity?
    @Query("SELECT * FROM sales_returns ORDER BY occurredAt DESC") fun observeSalesReturns(): Flow<List<SalesReturnEntity>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertSalesReturn(entity: SalesReturnEntity): Long
    @Update suspend fun updateSalesReturn(entity: SalesReturnEntity)
    @Query("DELETE FROM sales_returns") suspend fun clearSalesReturns()

    @Query("SELECT * FROM purchase_returns ORDER BY occurredAt DESC") suspend fun getPurchaseReturns(): List<PurchaseReturnEntity>
    @Query("SELECT * FROM purchase_returns WHERE id = :id LIMIT 1") suspend fun getPurchaseReturn(id: Long): PurchaseReturnEntity?
    @Query("SELECT * FROM purchase_returns ORDER BY occurredAt DESC") fun observePurchaseReturns(): Flow<List<PurchaseReturnEntity>>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertPurchaseReturn(entity: PurchaseReturnEntity): Long
    @Update suspend fun updatePurchaseReturn(entity: PurchaseReturnEntity)
    @Query("DELETE FROM purchase_returns") suspend fun clearPurchaseReturns()

    @Query("SELECT * FROM expenses ORDER BY occurredAt DESC") fun observeExpenses(): Flow<List<ExpenseEntity>>
    @Query("SELECT * FROM expenses ORDER BY occurredAt DESC") suspend fun getExpenses(): List<ExpenseEntity>
    @Query("SELECT * FROM expenses WHERE id = :id LIMIT 1") suspend fun getExpense(id: Long): ExpenseEntity?
    @Update suspend fun updateExpense(entity: ExpenseEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertExpense(entity: ExpenseEntity): Long
    @Query("DELETE FROM expenses") suspend fun clearExpenses()

    @Query("SELECT * FROM cash_accounts WHERE active = 1 ORDER BY id DESC") fun observeCashAccounts(): Flow<List<CashAccountEntity>>
    @Query("SELECT * FROM cash_accounts ORDER BY id") suspend fun getCashAccounts(): List<CashAccountEntity>
    @Query("SELECT * FROM cash_accounts WHERE id = :id LIMIT 1") suspend fun getCashAccount(id: Long): CashAccountEntity?
    @Query("SELECT * FROM cash_accounts WHERE currencyCode = :code AND active = 1 ORDER BY id DESC LIMIT 1") suspend fun getLatestCashAccountForCurrency(code: String): CashAccountEntity?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertCashAccount(entity: CashAccountEntity): Long
    @Update suspend fun updateCashAccount(entity: CashAccountEntity)
    @Query("DELETE FROM cash_accounts") suspend fun clearCashAccounts()

    @Query("SELECT * FROM cash_transactions ORDER BY occurredAt DESC") fun observeCashTransactions(): Flow<List<CashTransactionEntity>>
    @Query("SELECT * FROM cash_transactions ORDER BY occurredAt DESC") suspend fun getCashTransactions(): List<CashTransactionEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertCashTransaction(entity: CashTransactionEntity): Long
    @Query("DELETE FROM cash_transactions") suspend fun clearCashTransactions()

    @Query("SELECT * FROM stocktakes ORDER BY createdAt DESC") fun observeStocktakes(): Flow<List<StocktakeEntity>>
    @Query("SELECT * FROM stocktakes ORDER BY createdAt DESC") suspend fun getStocktakes(): List<StocktakeEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertStocktake(entity: StocktakeEntity): Long
    @Query("DELETE FROM stocktakes") suspend fun clearStocktakes()

    @Query("SELECT * FROM stocktake_lines ORDER BY id") suspend fun getStocktakeLines(): List<StocktakeLineEntity>
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertStocktakeLine(entity: StocktakeLineEntity): Long
    @Query("DELETE FROM stocktake_lines") suspend fun clearStocktakeLines()

    /** Replace helpers are used only while restoring a validated, versioned full backup. */
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreWarehouses(items: List<WarehouseEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreProducts(items: List<ProductEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreMovements(items: List<StockMovementEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreShipments(items: List<ShipmentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restorePurchaseInvoices(items: List<PurchaseInvoiceEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restorePurchaseLines(items: List<PurchaseInvoiceLineEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreSalesInvoices(items: List<SalesInvoiceEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreSalesLines(items: List<SalesInvoiceLineEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreSalesReturns(items: List<SalesReturnEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restorePurchaseReturns(items: List<PurchaseReturnEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreExpenses(items: List<ExpenseEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreCashAccounts(items: List<CashAccountEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreCashTransactions(items: List<CashTransactionEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreStocktakes(items: List<StocktakeEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun restoreStocktakeLines(items: List<StocktakeLineEntity>)
}
