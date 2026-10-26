# تقرير المرحلة الثانية: تكوين العملات وأسعار الصرف اليدوية والتحويل المالي الدقيق

> **ملاحظة تحديث:** بعض المسارات وادعاءات الاختبارات في هذا التقرير تاريخية. الحزمة الحالية هي `com.sulo.mizan.inventory`، ولم يُعَد تشغيل مجموعة Gradle الكاملة على آخر نسخة ضمن هذا التدقيق. راجع `PROJECT_FINAL_AUDIT_AND_PHASE_STATUS.md` للحالة المحدثة.
# PHASE 02 — CURRENCY CONFIGURATION & MANUAL EXCHANGE RATES

**تاريخ التوثيق:** 23 أكتوبر 2026  
**اسم المشروع:** ميزان المخزون — Mizan Inventory  
**حزمة التطبيق (Package Name):** `com.sulo.mizan.inventory`  
**المنصة المستهدفة:** Android (Kotlin & Jetpack Compose)  
**حالة البناء والاختبارات في التقرير الأصلي:** تم تسجيل `BUILD SUCCESSFUL` واجتياز 26 اختبارًا في حينه؛ هذه نتيجة تاريخية ولا تثبت نجاح آخر نسخة بعد تغييرات المراحل اللاحقة.  

---

## 1. ملخص ما كان موجودًا قبل PHASE 2 (Pre-Phase 2 State)

قبل بدء المرحلة الثانية، كان المشروع يحتوي على الأساس الأولي الذي أُنجز في المرحلة الأولى (PHASE 1):
1. **النموذج المالي الأولي:**
   - كائن `Money` المعتمد على `BigDecimal` مع دقة 8 منازل وحظر تام لـ `Float` و `Double`.
   - كائن `Currency` محدود بثلاث عملات فقط (`YER`, `SAR`, `USD`) كقيم ثابتة دون دعم الأسماء الإنجليزية أو التعيين الديناميكي.
   - كائن `ExchangeRate` أولي دون تمييز لأنواع الأسعار أو حفظ السجل الزمني أو دعم التحويل التقاطعي.
2. **قاعدة البيانات:**
   - قاعدة بيانات Room بالإصدار 1 تضم جدولاً واحداً فقط: `system_config` لتخزين مفاتيح التكوين النصية البسيطة.
   - غياب أي جداول لتخزين أسعار الصرف التاريخية أو إعدادات العملات المتعددة في SQLite.
3. **مستودع التفضيلات:**
   - كان `AppSettingsRepository` (DataStore) يحفظ العملة الأساسية افتراضياً كـ `YER`، مما شكّل خطر وجود مصدرين متضاربين للبيانات عند بناء عمليات محاسبية في Room.
4. **شاشات التطبيق:**
   - شاشة إعدادات أولية تعرض العملات الثلاث السابقة في RadioButtons مرتبطة بـ DataStore فقط.
   - غياب أي حاسبة تحويل أو واجهة لإدخال أسعار الصرف اليدوية أو استعراض سجل الأسعار.

---

## 2. الملفات التي تم إنشاؤها أو تعديلها في PHASE 2

| الملف | نوع العملية | الغرض المعماري والدور الوظيفي |
|---|---|---|
| `app/src/main/java/com/example/core/model/Currency.kt` | تعديل | إضافة دعم الأسماء بالإنجليزية (`displayNameEn`)، والمنازل العشرية المخصصة (مثل 3 لدينار الكويت و0 للين الياباني)، وتعريف العملات الافتراضية الثلاث المعتمدة للمرحلة (`SDG` أساسية، `USD` ثانية، `GBP` ثالثة)، وكتالوج موسع للعملات المدعومة (`ALL_SUPPORTED_CURRENCIES`). |
| `app/src/main/java/com/example/core/model/RateType.kt` | إنشاء | تعداد صارم لأنواع أسعار الصرف (`REFERENCE`, `BUY`, `SELL`, `TRANSACTIONAL`) لمنع الخلط بين أسعار الشراء والبيع والأسعار المرجعية. |
| `app/src/main/java/com/example/core/model/ExchangeRate.kt` | تعديل | إضافة الحقول الإلزامية: `rateType`، `timestamp`، `sourceOrNote`، وحظر الأسعار الصفرية والسالبة والربط الذاتي، مع كائن نتيجة التحويل الموثق `ConversionResult`. |
| `app/src/main/java/com/example/core/database/Converters.kt` | تعديل | إضافة محولات Room لتخزين وقراءة نوع السعر `RateType` كنص موحد في SQLite مع الحفاظ على تحويل `BigDecimal` كنص لمنع التقريب الثنائي. |
| `app/src/main/java/com/example/core/database/CurrencyConfigEntity.kt` | إنشاء | كيان Room لتمثيل العملة المخزنة في جدول `currency_configs` مع مؤشرات `isBase` و `isActive` وترتيب العرض. |
| `app/src/main/java/com/example/core/database/CurrencyConfigDao.kt` | إنشاء | واجهة الوصول لبيانات العملات وعمليات المعاملات المجمعة `Transaction` لضمان عدم وجود أكثر من عملة أساسية واحدة وتفعيل حتى 3 عملات فقط. |
| `app/src/main/java/com/example/core/database/ExchangeRateEntity.kt` | إنشاء | كيان Room لتمثيل سجل أسعار الصرف في جدول `exchange_rates` مع فهارس البحث (`Indices`) للزوج العملي والتاريخ ونوع السعر. |
| `app/src/main/java/com/example/core/database/ExchangeRateDao.kt` | إنشاء | واجهة استرجاع أحدث الأسعار ومراقبة السجل التاريخي المتراكم دون الكتابة فوق السجلات القديمة. |
| `app/src/main/java/com/example/core/database/AppDatabase.kt` | تعديل | رفع إصدار قاعدة البيانات إلى `version = 2`، وتطبيق ترحيل منظم `MIGRATION_1_2` ينشئ الجداول والفهارس ويبذر العملات الافتراضية، مع حظر التدمير التلقائي `fallbackToDestructiveMigration(false)`. |
| `app/src/main/java/com/example/core/repository/CurrencyRepository.kt` | إنشاء | المستودع المركزي والمصدر الموحد الموثوق لإدارة العملات النشطة والعملة الأساسية وأسعار الصرف ومحرك التحويل المالي المباشر والتقاطعي. |
| `app/src/main/java/com/example/core/preferences/AppSettingsRepository.kt` | تعديل | تحديث العملة الأساسية الافتراضية إلى `SDG`، وحصر نطاق DataStore في مظهر المتجر والواجهات وتوجيه العمليات المالية لـ Room. |
| `app/src/main/res/values/strings.xml` | تعديل | إضافة النصوص والمسميات العربية للمرحلة الثانية وشاشة العملات وأسعار الصرف. |
| `app/src/main/java/com/example/core/navigation/Screen.kt` | تعديل | إضافة وجهة التنقل المخصصة `Screen.Currencies` مع أيقونة M3 المتوافقة. |
| `app/src/main/java/com/example/features/currency/CurrencyScreen.kt` | إنشاء | واجهة المستخدم الكاملة للمرحلة الثانية: إدارة العملات النشطة الثلاث، تسجيل أسعار الصرف اليدوية، استعراض السجل الزمني، وحاسبة التحويل المالي الدقيق. |
| `app/src/main/java/com/example/features/settings/SettingsScreen.kt` | تعديل | ربط شاشة الإعدادات بـ `CurrencyRepository` كـ Single Source of Truth وإضافة بطاقة وصول سريعة لشاشة العملات. |
| `app/src/main/java/com/example/features/dashboard/DashboardScreen.kt` | تعديل | تحديث بطاقات مراحل التطبيق وإبراز المرحلة 2 كـ "نشط" مع تمكين الانتقال لشاشة العملات وأداة فحص الدقة. |
| `app/src/main/java/com/example/features/inventory/InventoryScreen.kt` | تعديل | تحديث بطاقة التأسيس لتشير إلى أن المخزون هو المرحلة 3 التالية. |
| `app/src/main/java/com/example/MainActivity.kt` | تعديل | تسجيل وجهة `Screen.Currencies` في `NavHost` للربط السلس بين شاشات التطبيق. |
| `app/src/test/java/com/example/CurrencyAndExchangeRatePhase2Test.kt` | إنشاء | اختبارات قبول شاملة تغطي كافة متطلبات المرحلة الحسابية والدقة والمنازل العشرية وأنواع الأسعار. |
| `app/src/test/java/com/example/CurrencyRoomDatabaseMigrationTest.kt` | إنشاء | اختبارات تكاملية عبر Robolectric للتحقق من عمليات Room والترحيل `MIGRATION_1_2` ومستودع العملات. |
| `PHASES/PHASE_02_CURRENCY_CONFIGURATION.md` | إنشاء | هذا التقرير التوثيقي الشامل المعتمد للمرحلة الثانية في جذر المشروع. |

---

## 3. كيفية إدارة العملات الثلاث النشطة (Active Currencies Management)

1. **القيم الافتراضية عند التثبيت لأول مرة:**
   - **العملة المحلية الأساسية:** الجنيه السوداني (`SDG` - "جنيه سوداني" - `ج.س` - دقة منزلتين).
   - **العملة الثانية:** الدولار الأمريكي (`USD` - "دولار أمريكي" - `$` - دقة منزلتين).
   - **العملة الثالثة:** الجنيه الإسترليني (`GBP` - "جنيه إسترليني" - `£` - دقة منزلتين).
2. **سقف العملات النشطة:**
   - يفرض `CurrencyRepository` حداً أقصى صارماً يبلغ **3 عملات نشطة فقط**.
   - في حال محاولة تمرير أكثر من 3 عملات، يرمي النظام استثناء `IllegalArgumentException` ويرفض العملية فوراً.
3. **تغيير العملات:**
   - يستطيع التاجر استبدال أي عملة نشطة بأي عملة أخرى من قائمة العملات المدعومة (`ALL_SUPPORTED_CURRENCIES` مثل الريال السعودي، الدرهم الإماراتي، اليورو، الدينار الكويتي، إلخ).
4. **حماية السجلات التاريخية:**
   - عند تعطيل أو استبدال عملة نشطة، **لا يتم حذف أي سعر صرف مسجل تاريخياً لها من قاعدة البيانات**؛ تبقى السجلات في جدول `exchange_rates` غير متأثرة مع الحفاظ على دقتها الكاملة.
5. **العملة الأساسية للتقارير:**
   - يشترط النظام أن تكون العملة الأساسية واحدة من العملات الثلاث النشطة.
   - تغيير العملة الأساسية يتم داخل عملية `Transaction` تضمن تصفير مؤشر الأساس القديم وتعيين الجديد بصورة ذرية.

---

## 4. مصدر البيانات المعتمد ونظام التخزين (Single Source of Truth)

- **القرار المعماري:** تم اعتماد **قاعدة بيانات Room (`AppDatabase`)** بصفتها المصدر الوحيد الموثوق والمؤتمن لبيانات العملات وأسعار الصرف التي تؤثر في العمليات المالية والمحاسبية.
- **عزل DataStore:** تم قصر دور Jetpack DataStore على تفضيلات واجهة المستخدم والمظهر (`themeMode`) واسم المتجر التجاري (`merchantName`).
- **منع التضارب:** تم ربط كافة واجهات المستخدم (`CurrencyScreen` و `SettingsScreen` و `DashboardScreen`) مباشرة بتدفقات `CurrencyRepository` المستندة إلى Room عبر Flow، مما يقضي تماماً على إمكانية وجود قيم متضاربة للعملة الأساسية.

---

## 5. تصميم نماذج العملات وأسعار الصرف (Data Models)

### كائن العملة (`Currency`):
```kotlin
data class Currency(
    val code: String,             // رمز ISO 4217 (مثل SDG, USD, GBP, KWD, JPY)
    val displayNameAr: String,    // الاسم العربي (مثل "جنيه سوداني")
    val symbol: String,           // الرمز المرئي (مثل "ج.س", "$", "£", "د.ك")
    val defaultDecimals: Int = 2, // عدد المنازل العشرية المعتمد
    val displayNameEn: String = "" // الاسم بالإنجليزية (مثل "Sudanese Pound")
)
```

### كائن نوع السعر (`RateType`):
- `REFERENCE`: سعر مرجعي (تقديري) — سعر السوق أو السعر الإرشادي العام.
- `BUY`: سعر شراء العملة من منظور التاجر — السعر الذي يشتري به التاجر العملة الأجنبية.
- `SELL`: سعر بيع العملة من منظور التاجر — السعر الذي يبيع به التاجر العملة الأجنبية.
- `TRANSACTIONAL`: سعر معاملة فعلية — السعر المتفق عليه والمنفذ في عملية حقيقية.

### كائن سعر الصرف (`ExchangeRate`):
```kotlin
data class ExchangeRate(
    val fromCurrency: Currency,
    val toCurrency: Currency,
    val rate: BigDecimal,
    val rateType: RateType = RateType.REFERENCE,
    val timestamp: Long = System.currentTimeMillis(),
    val sourceOrNote: String = "",
    val id: Long = 0L
)
```
- **تعريف الاتجاه الصريح:** السعر يعبر عن كم وحدة من `toCurrency` تعادل وحدة واحدة من `fromCurrency`.
  *مثال:* `1 USD = 2,500.00 SDG` يتم تسجيلها كـ `from = USD`, `to = SDG`, `rate = 2500.00`.
- **القيود الصارمة:**
  - يمنع السعر الصفري أو السالب (`rate > BigDecimal.ZERO`).
  - يمنع ربط العملة بنفسها إلا في حالة التطابق الذاتي `identity` حيث السعر يساوي 1.0 بالضبط.

---

## 6. قواعد التحويل المالي والتقريب والتحويل التقاطعي

1. **التحويل المباشر:**
   - عند توفر سعر صرف مباشر مسجل بين العملتين (بالنوع المفضل المطلوب)، يُضرب المبلغ بالقيمة:
     $$\text{Target Amount} = \text{Source Amount} \times \text{Rate}$$
   - يتم ضبط عدد المنازل العشرية وفق دقة العملة الهدف (`toCurrency.defaultDecimals`).
   - يتم تطبيق تقريب المصرفيين المالي القياسي (`RoundingMode.HALF_EVEN`).
2. **التحويل التقاطعي (Cross-Rate Conversion):**
   - في حال عدم وجود سعر مباشر بين عملتين ثانويتين (مثل `USD` إلى `GBP`) مع كونهما عملتين نشطتين غير العملة الأساسية (`SDG`):
     - الخطوة الأولى: تحويل المبلغ من العملة المصدر إلى العملة الأساسية عبر سعر `USD -> SDG`.
     - الخطوة الثانية: تحويل المبلغ الناتج من العملة الأساسية إلى العملة الهدف عبر سعر `SDG -> GBP`.
     - يتم توثيق وحساب السعر التقاطعي المركب مع توثيق المسار والأسعار المستخدمة في كائن `ConversionResult`.
3. **عدم عكس أسعار الشراء والبيع تلقائياً:**
   - نظراً لأن هامش الشراء والبيع (Spread) يجعل مقلوب سعر الشراء غير متطابق مع سعر البيع، يحظر النظام عكس أسعار البيع والشراء عشوائياً.
4. **رفض التحويل عند غياب السعر:**
   - إذا تعذر إيجاد سعر مباشر أو مسار تقاطعي موثق، يرمي المحرك استثناء صريحاً `MissingExchangeRateException` مع إظهار رسالة واضحة في الواجهة تطلب إدخال السعر أولاً.

---

## 7. مخطط Room والترحيل المنفذ (Database Schema & Migration)

تمت ترقية `AppDatabase` من الإصدار 1 إلى الإصدار 2 مع ترحيل آمن `MIGRATION_1_2`:

### الجداول الجديدة في الإصدار 2:
```sql
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
);

CREATE TABLE IF NOT EXISTS `exchange_rates` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `fromCurrencyCode` TEXT NOT NULL,
    `toCurrencyCode` TEXT NOT NULL,
    `rate` TEXT NOT NULL,
    `rateType` TEXT NOT NULL,
    `timestamp` INTEGER NOT NULL,
    `sourceOrNote` TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS `index_exchange_rates_fromCurrencyCode_toCurrencyCode` ON `exchange_rates` (`fromCurrencyCode`, `toCurrencyCode`);
CREATE INDEX IF NOT EXISTS `index_exchange_rates_timestamp` ON `exchange_rates` (`timestamp`);
CREATE INDEX IF NOT EXISTS `index_exchange_rates_rateType` ON `exchange_rates` (`rateType`);
```

### ضمانات الأمان:
- تم تعطيل تدمير قاعدة البيانات التلقائي `fallbackToDestructiveMigration(false)`.
- تم الحفاظ على جدول `system_config` وبياناته السابقة من المرحلة 1.
- تم تزويد قاعدة البيانات بـ `Callback.onCreate` وخطوة ترحيل لضمان بذر العملات الافتراضية الثلاث تلقائياً عند التثبيت أو الترقية.

---

## 8. نتائج الاختبارات الفعلية (Actual Test Results)

تم تشغيل كامل حزمة اختبارات الوحدة واختبارات قاعدة البيانات بنجاح تام عبر الأمر:
`./gradlew :app:testDebugUnitTest`

**النتيجة الرسمية المنفذة:**
```text
BUILD SUCCESSFUL in 26s
33 actionable tasks: 3 executed, 30 up-to-date
```

### تفاصيل الاختبارات المجتازة (26 اختباراً، 0 فشل):

#### أ. اختبارات المرحلة 1 المحفوظة بنجاح (9 اختبارات):
1. `MoneyFinancialPrecisionTest.testBigDecimalPrecisionEliminatesBinaryFloatingPointErrors`: نجح (تأكيد دقة 0.1 + 0.2 = 0.3 دون انزلاق ثنائي).
2. `MoneyFinancialPrecisionTest.testCurrencyMismatchThrowsExceptionOnDirectAddition`: نجح (حظر جمع YER مع USD).
3. `MoneyFinancialPrecisionTest.testCurrencyMismatchThrowsExceptionOnDirectSubtraction`: نجح (حظر طرح عملات مختلفة).
4. `MoneyFinancialPrecisionTest.testMoneyScalingAndRounding`: نجح (التقريب المحاسبي HALF_EVEN).
5. `MoneyFinancialPrecisionTest.testMoneyMultiplicationWithFactorAndQuantity`: نجح.
6. `MoneyFinancialPrecisionTest.testExchangeRateConversion`: نجح (التحويل المالي المباشر).
7. `MoneyFinancialPrecisionTest.testQuantityValidation`: نجح (حظر المخزون السالب والكميات الصحيحة).
8. `ExampleUnitTest.addition_isCorrect`: نجح.
9. `ExampleRobolectricTest.read string from context`: نجح (قراءة اسم التطبيق "ميزان المخزون").

#### ب. اختبارات المرحلة 2 الجديدة لقواعد العملات والتحويل (10 اختبارات):
10. `CurrencyAndExchangeRatePhase2Test.testInitialDefaultCurrenciesAreSdgUsdGbp`: نجح (تأكيد العملات الافتراضية الثلاث والأساسية SDG).
11. `CurrencyAndExchangeRatePhase2Test.testCurrencyCatalogSupportsArabAndGlobalCurrenciesWithCorrectDecimals`: نجح (فحص دقة دينار كويت 3 منازل، والين 0، والجنيه 2).
12. `CurrencyAndExchangeRatePhase2Test.testExchangeRateRejectsZeroAndNegativeValues`: نجح (رفض الأسعار الصفرية والسالبة).
13. `CurrencyAndExchangeRatePhase2Test.testExchangeRateRejectsSameCurrencyUnlessIdentity`: نجح (حظر ربط العملة بنفسها إلا في التطابق).
14. `CurrencyAndExchangeRatePhase2Test.testExchangeRateDirectionDescription`: نجح (التحقق من نص اتجاه السعر مثل 1 USD = 2500 SDG).
15. `CurrencyAndExchangeRatePhase2Test.testDirectCurrencyConversionPrecision`: نجح (حساب التحويل الدقيق بـ BigDecimal).
16. `CurrencyAndExchangeRatePhase2Test.testTargetCurrencyCustomDecimalsRounding`: نجح (التحويل إلى عملات بمنازل 3 أو 0 مع التقريب الصحيح).
17. `CurrencyAndExchangeRatePhase2Test.testRateTypeDifferentiationPreventsConflation`: نجح (التمييز بين سعر الشراء والبيع والمرجعي).
18. `CurrencyAndExchangeRatePhase2Test.testCrossCurrencyConversionMath`: نجح (التحقق الرياضي من التحويل التقاطعي عبر وسيط).
19. `CurrencyAndExchangeRatePhase2Test.testDirectAdditionOfDifferentCurrenciesStillBlocked`: نجح (تأكيد استمرار حظر الجمع المباشر بين SDG و USD).

#### ج. اختبارات تكامل قاعدة البيانات والترحيل عبر Robolectric (7 اختبارات):
20. `CurrencyRoomDatabaseMigrationTest.testEnsureInitializedSeedsDefaultThreeCurrencies`: نجح (بذر العملات الثلاث الافتراضية في Room).
21. `CurrencyRoomDatabaseMigrationTest.testMaxThreeActiveCurrenciesEnforced`: نجح (فرض سقف 3 عملات ورفض 4 عملات ورفض عملة أساسية غير نشطة).
22. `CurrencyRoomDatabaseMigrationTest.testSetBaseCurrencyPersistsCorrectly`: نجح (تغيير وحفظ العملة الأساسية واسترجاعها).
23. `CurrencyRoomDatabaseMigrationTest.testExchangeRateHistoryPreservesAllEntries`: نجح (تسجيل أسعار متعددة لنفس الزوج والتأكد من عدم الكتابة فوق السجلات القديمة).
24. `CurrencyRoomDatabaseMigrationTest.testCrossRateConversionViaBaseCurrency`: نجح (تنفيذ التحويل التقاطعي من خلال مستودع العملات).
25. `CurrencyRoomDatabaseMigrationTest.testMissingExchangeRateThrowsException`: نجح (رمي استثناء صريح عند فقدان السعر المطلوب).
26. `CurrencyRoomDatabaseMigrationTest.testSystemConfigAndRoomMigration1To2`: نجح (التأكد من سلامة ترحيل Room من v1 إلى v2 وبقاء جدول system_config).

---

## 9. الأخطاء المتبقية والمشاكل التي تم تداركها

- **مشكلة عائد دوال JUnit 4:** تم رصد خطأ `InvalidTestClassError` أثناء كتابة الاختبارات في البداية لأن الدوال استخدمت تعبير `= runBlocking` الذي أرجع قيماً غير فارغة، وتم تصحيحها فوراً لتصبح ذات عائد `void` (`Unit`) داخل جسم دالة عادي، واجتازت جميع الاختبارات بنجاح تام.
- **لا توجد أي أخطاء متبقية** في الكود أو قواعد البيانات أو الاختبارات أو بناء التطبيق (`compile_applet` ناجح).

---

## 10. الوظائف المؤجلة عمدًا (Scope Boundaries)

التزاماً بحدود المرحلة الثانية الصارمة، تم تأجيل الوظائف التالية للمراحل اللاحقة:
1. أي اتصال بشبكة الإنترنت أو جلب آلي لأسعار الصرف عبر واجهات برمجية (APIs).
2. أي مراقبة لحظية أو تنبيهات آلية لتغير الأسعار.
3. إنشاء جداول الأصناف أو الشحنات أو المستودعات (مؤجلة للمرحلة 3: المخزون).
4. تسجيل فواتير الشراء أو البيع أو تكاليف البضائع المباعة والأرباح (مؤجلة للمرحلة 4 و 5).
5. المزامنة السحابية.

---

## 11. قائمة القبول والتحقق (Acceptance Checklist)

- [x] تهيئة SDG و USD و GBP كعملات افتراضية أولية عند إعداد التطبيق.
- [x] السماح بتغيير العملات واختيار ثلاث عملات نشطة فقط كحد أقصى.
- [x] حفظ العملة الأساسية واسترجاعها بصورة صحيحة من Room.
- [x] رفض أسعار الصرف الصفرية والسالبة.
- [x] التحقق من اتجاه التحويل ووحدة قياس السعر بوضوح (1 من = X إلى).
- [x] حساب التحويل المباشر بدقة عشرية صارمة (`BigDecimal`).
- [x] تنفيذ التحويل التقاطعي عبر العملة الأساسية وتوثيق مسار الأسعار المستخدمة.
- [x] رفض التحويل وإظهار رسالة واضحة عند فقدان السعر المطلوب.
- [x] تطبيق التقريب المحاسبي المعتمد وفق دقة العملة الهدف.
- [x] منع خلط أو جمع مبالغ من عملات مختلفة بصورة مباشرة.
- [x] الاحتفاظ بتاريخ أسعار الصرف متراكماً دون الكتابة فوق السجلات السابقة.
- [x] اعتماد Room مصدراً موحداً وموثوقاً لإعدادات العملات والأسعار.
- [x] نجاح ترحيل قاعدة البيانات `MIGRATION_1_2` دون فقدان بيانات المرحلة 1.
- [x] الحفاظ على كافة اختبارات المرحلة الأولى واجتيازها بنسبة 100%.
- [x] بناء التطبيق كاملاً واجتياز أداة التحقق `compile_applet`.

---
**نهاية تقرير المرحلة الثانية — جاهز للمراجعة والاعتماد قبل الانتقال إلى المرحلة التالية.**
