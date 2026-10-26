# تقرير المرحلة الأولى: تأسيس وبنية مشروع ميزان المخزون

> **ملاحظة تاريخية:** إشارات `com.example` في هذا التقرير تصف لقطة المشروع وقت المرحلة الأولى فقط. تم تغيير الحزمة لاحقًا في PHASE 02A، والحزمة الحالية هي `com.sulo.mizan.inventory`.
# PHASE 01 — PROJECT FOUNDATION & ARCHITECTURE

**تاريخ التوثيق:** 23 أكتوبر 2026  
**اسم المشروع:** ميزان المخزون — Mizan Inventory  
**حزمة التطبيق وقت المرحلة الأولى:** `com.example`  
**حزمة التطبيق الحالية بعد PHASE 02A:** `com.sulo.mizan.inventory`  
**معرّف التطبيق الحالي:** `com.sulo.mizan.inventory`  
**المنصة المستهدفة:** Android (Kotlin & Jetpack Compose)  

---

## A. ملخص التنفيذ (Execution Summary)

### ما الذي كان موجودًا قبل البدء؟
1. قالب مشروع Android حديث فارغ يعتمد لغة Kotlin مع Jetpack Compose و Material Design 3.
2. حزمة المشروع كانت معرّفة كـ `com.example` وتضم نشاطًا رئيسيًا واحدًا (`MainActivity.kt`) يعرض نصًا تمهيديًا (`Greeting("Android")`).
3. إعدادات Gradle كانت تحتوي على اعتماديات جاهزة ومعلقة جزئيًا (مثل `room-ktx`، `ksp`، `navigation-compose`، `datastore-preferences`).
4. لم تكن هناك أي بنية معمارية، ولا طبقة بيانات، ولا نماذج للمجال (Domain Models)، ولا نظام ملاحة، ولا أي نموذج مالي دقيق.

### ما الذي تم إنجازه فعليًا في PHASE 1؟
1. **الفحص والتدقيق التقني الشامل:** تم فحص كافة ملفات التكوين (Gradle, Version Catalog, Manifest, SDKs) واختبار البناء الأولي بنجاح.
2. **تأسيس النواة المالية الصارمة (Strict Financial Engine):**
   - تم اتخاذ قرار معماري ملزم بمنع استخدام `Float` أو `Double` منعًا باتًا في أي عمليات أو قيم مالية.
   - تم بناء كائن القيمة النقدية `Money` المعتمد بنسبة 100% على `java.math.BigDecimal` مع تحديد دقة الحساب ونمط التقريب المحاسبي الموحد (`RoundingMode.HALF_EVEN`).
   - تم بناء كائن العملة `Currency` الداعم لترميز ISO والأسماء العربية والرموز (`YER`, `SAR`, `USD`).
   - تم تطبيق حماية صارمة لمنع خلط أو جمع المبالغ من عملات مختلفة بصورة مباشرة عبر رمي استثناء مخصص `CurrencyMismatchException` يمنع العمليات غير المتوافقة دون سعر صرف صريح.
   - تم إنشاء كائن الكميات المخزنية `Quantity` لمنع الانزلاق العشري في كسور الوحدات.
   - تم إنشاء كائن أسعار الصرف الصريح `ExchangeRate` لتهيئة التحويل المحاسبي في المراحل اللاحقة.
3. **تجهيز طبقة التخزين المحلي المستقر (Data & Storage Layer Foundation):**
   - إعداد قاعدة بيانات `Room` المحلية (`AppDatabase`) مع محولات مخصصة (`Converters`) تحفظ قيم `BigDecimal` كنصوص (`String`) في SQLite لضمان الدقة المطلقة بدون أي فقدان تقريبي ثنائي.
   - تفعيل مستودع تفضيلات التطبيق `AppSettingsRepository` بالاعتماد على Jetpack `DataStore Preferences` لإدارة العملة الأساسية وإعدادات التاجر.
4. **تأسيس نظام الواجهات والملاحة (UI & Navigation System):**
   - تصميم سمة Material Design 3 تجارية راقية مستوحاة من الهوية المالية والمخزنية (ألوان النيلي الداكن، الأخضر الزمردي المتوازن، والكهرماني الذهبي للعملات).
   - فرض اتجاه الواجهة من اليمين إلى اليسار (`RTL`) لتقديم تجربة عربية أصيلة للتاجر مع نصوص عربية كاملة في `strings.xml`.
   - بناء شريط تنقل سفلي (`MizanBottomBar`) وشريط علوي تفاعلي (`MizanTopAppBar`) يربطان 5 وجهات رئيسية بنظام `Navigation Compose`.
   - تمييز الأقسام غير المنفذة ببطاقات تعريفية شفافة وموثقة (`SectionPlaceholderCard`) توضح حدود المسؤوليات لكل مرحلة قادمة وتخلو تمامًا من البيانات الوهمية المضللة.
   - بناء لوحة تحكم تفاعلية (`DashboardScreen`) تتضمن أداة اختبار حية للدقة المالية تؤكد للمطور والمستخدم عدم وجود أخطاء التقريب الثنائي وحظر الجمع بين عملات مختلفة.
5. **إنشاء أيقونة تكيفية مخصصة (Custom Adaptive Icon):**
   - استبدال الأيقونة الافتراضية بأيقونة متجهة تكيفية تمثل ميزان التجارة وسجل المخزون بالألوان المعتمدة.
6. **الاختبارات الآلية والتحقق:**
   - كتابة وتنفيذ اختبارات وحدة شاملة (`MoneyFinancialPrecisionTest.kt`) للتحقق من سلامة العمليات المالية وحظر خلط العملات.
   - تحديث واجتياز اختبارات `Robolectric` (`ExampleRobolectricTest.kt`) والتحقق من الموارد البرمجية بنجاح بنسبة 100%.

---

## B. حالة المشروع قبل التعديل (Pre-Modification State)

* **البنية العامة:** مشروع قياسي ذو وحدة واحدة (`:app`) داخل Gradle متعدد الوحدات.
* **إصدار Kotlin:** `2.2.10` مع دعم مدمج لـ Compose Plugin.
* **إصدار Android Gradle Plugin (AGP):** `9.1.1`.
* **إصدار KSP:** `2.3.5`.
* **إعدادات Android SDK:**
  - `compileSdk`: 36 (minorApiLevel = 1).
  - `targetSdk`: 36.
  - `minSdk`: 24 (يدعم Android 7.0 فأحدث، وهو مناسب جدًا للأجهزة اللوحية وهواتف نقاط البيع).
* **إصدار Compose BOM:** `2024.09.00`.
* **اسم الحزمة عند إتمام PHASE 1:** `com.example` (ثم تغيّر بصورة موثقة في PHASE 02A).
* **معرّف التطبيق الحالي في لقطة PHASE 1:** `com.example` (تم تغييره لاحقًا إلى `com.sulo.mizan.inventory`).
* **اسم التطبيق السابق:** `My Application`.

---

## C. التغييرات المنفذة (Executed Changes)

| الملف | نوع العملية | سبب التغيير |
|---|---|---|
| `metadata.json` | تعديل | مزامنة اسم المشروع ووصفه مع المنصة وتسميته "Mizan Inventory". |
| `settings.gradle.kts` | تعديل | تحديث `rootProject.name` ليصبح `"Mizan Inventory"`. |
| `app/build.gradle.kts` | تعديل | تفعيل اعتماديات `navigation-compose` و `datastore-preferences` لتأسيس نظام الملاحة والتفضيلات. |
| `app/src/main/res/values/strings.xml` | تعديل | إضافة نصوص الواجهة العربية الشاملة، وعناوين الأقسام، وبطاقات التأسيس وتسمية التطبيق "ميزان المخزون". |
| `app/src/main/res/drawable/ic_launcher_background.xml` | تعديل | إعداد الخلفية الداكنة للأيقونة التكيفية بلون الميزان المعتمد `#0F172A`. |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | تعديل | رسم متجه ميزان المخزون وسجل الحسابات المالي كأيقونة أمامية تكيفية. |
| `app/src/main/java/com/example/core/model/Currency.kt` | إنشاء | نموذج كائن العملات والرموز المعتمدة والتنسيق الافتراضي للكسور. |
| `app/src/main/java/com/example/core/model/Money.kt` | إنشاء | كائن القيمة النقدية الصارم المعتمد على `BigDecimal` وحظر `Float/Double` تمامًا ومنع خلط العملات. |
| `app/src/main/java/com/example/core/model/Quantity.kt` | إنشاء | كائن الكميات المخزنية الدقيقة للوحدات والكسور دون تقريب ثنائي عائم. |
| `app/src/main/java/com/example/core/model/ExchangeRate.kt` | إنشاء | نموذج كائن أسعار الصرف المنضبط لعمليات التحويل الصريحة. |
| `app/src/main/java/com/example/core/database/Converters.kt` | إنشاء | محولات Room لحفظ قيم `BigDecimal` والعملات كنصوص نقية في SQLite دون فقدان للدقة. |
| `app/src/main/java/com/example/core/database/SystemConfigEntity.kt` | إنشاء | كيان Room لتخزين إعدادات وتكوين النظام الأساسي في قاعدة البيانات. |
| `app/src/main/java/com/example/core/database/SystemConfigDao.kt` | إنشاء | واجهة الوصول لبيانات تكوين النظام عبر العمليات غير المتزامنة وتدفقات Flow. |
| `app/src/main/java/com/example/core/database/AppDatabase.kt` | إنشاء | فئة قاعدة بيانات Room المركزية وتجهيز النمط الأحادي (Singleton) مع المحولات المعتمدة. |
| `app/src/main/java/com/example/core/preferences/AppSettingsRepository.kt` | إنشاء | مستودع إدارة تفضيلات التطبيق والعملة الأساسية بالاعتماد على Jetpack DataStore Preferences. |
| `app/src/main/java/com/example/ui/theme/Color.kt` | تعديل | تعريف لوحة الألوان التجارية والمالية لتطبيق ميزان المخزون (نيلي، زمردي، ذهبي). |
| `app/src/main/java/com/example/ui/theme/Theme.kt` | تعديل | تطبيق ثيم Material 3 ودعم الوضعين الفاتح والداكن مع فرض اتجاه RTL للغة العربية. |
| `app/src/main/java/com/example/core/navigation/Screen.kt` | إنشاء | تعريف وجهات التنقل الخمس الرئيسية مع المعرفات وأيقونات M3 وعلامات الاختبار `testTag`. |
| `app/src/main/java/com/example/core/navigation/MizanBottomBar.kt` | إنشاء | شريط الملاحة السفلي التفاعلي المعتمد على Material 3 NavigationBar. |
| `app/src/main/java/com/example/core/ui/CommonComponents.kt` | إنشاء | مكونات الواجهة المشتركة: `MizanTopAppBar`، `StatusBadge`، و `SectionPlaceholderCard`. |
| `app/src/main/java/com/example/features/dashboard/DashboardScreen.kt` | إنشاء | شاشة لوحة التحكم الرئيسية مع ملخص ركائز البنية وأداة التحقق المالي التفاعلية وروابط الأقسام. |
| `app/src/main/java/com/example/features/inventory/InventoryScreen.kt` | إنشاء | شاشة واجهة المخزون التأسيسية التي توضح حدود مسؤوليات المرحلة 2 دون بيانات وهمية. |
| `app/src/main/java/com/example/features/operations/OperationsScreen.kt` | إنشاء | شاشة واجهة العمليات والمبيعات التأسيسية التي توضح حدود مسؤوليات المرحلة 3. |
| `app/src/main/java/com/example/features/reports/ReportsScreen.kt` | إنشاء | شاشة واجهة التقارير المالية التأسيسية التي توضح حدود مسؤوليات المرحلة 4. |
| `app/src/main/java/com/example/features/settings/SettingsScreen.kt` | إنشاء | شاشة الإعدادات التفاعلية لتحديد العملة الأساسية وعرض بيانات فحص النظام (Diagnostics). |
| `app/src/main/java/com/example/MainActivity.kt` | تعديل | ربط نظام التنقل الكامل مع NavHost وشريط التطبيق والتنقل السفلي ودعم تبديل المظهر. |
| `app/src/test/java/com/example/MoneyFinancialPrecisionTest.kt` | إنشاء | اختبارات وحدة حسابية صارمة للتحقق من دقة `BigDecimal` وحظر الجمع بين عملات مختلفة. |
| `app/src/test/java/com/example/ExampleRobolectricTest.kt` | تعديل | تحديث اختبار Robolectric للتأكد من قراءة اسم التطبيق الجديد "ميزان المخزون" من السياق البرمجي. |
| `PHASES/PHASE_01_PROJECT_FOUNDATION.md` | إنشاء | هذا التقرير التوثيقي الشامل المعتمد للمرحلة الأولى في جذر المشروع. |

---

## D. البنية المعمارية (Architecture & Layer Organization)

تم تنظيم المشروع باتباع معمارية نظيفة (Clean Architecture) مفصولة المسؤوليات بدقة داخل الحزمة `com.example`:

```
app/src/main/java/com/example/
├── MainActivity.kt                      // نقطة الدخول الرئيسية وحاوية التنقل
├── core/                                // الطبقة المشتركة والنواة الأساسية
│   ├── model/                           // كائنات القيمة ونماذج المجال (Domain Models)
│   │   ├── Currency.kt                  // تعريف العملات وخصائصها
│   │   ├── Money.kt                     // محرك الحساب المالي الصارم (BigDecimal)
│   │   ├── Quantity.kt                  // كائن الكميات المخزنية الدقيقة
│   │   └── ExchangeRate.kt              // نموذج أسعار الصرف والتحويل المحاسبي
│   ├── database/                        // طبقة التخزين المحلي (Room & SQLite)
│   │   ├── Converters.kt                // محولات حفظ القيم النقدية كنصوص
│   │   ├── SystemConfigEntity.kt        // كيان إعدادات النظام
│   │   ├── SystemConfigDao.kt           // واجهة استعلامات التكوين
│   │   └── AppDatabase.kt               // فئة قاعدة البيانات المحلية (RoomDatabase)
│   ├── preferences/                     // مستودع إعدادات التطبيق (DataStore)
│   │   └── AppSettingsRepository.kt     // حفظ العملة الأساسية وتفضيلات التاجر
│   ├── navigation/                      // تنظيم التنقل والملاحة
│   │   ├── Screen.kt                    // مسارات ووجهات الشاشات الخمس
│   │   └── MizanBottomBar.kt            // شريط التنقل السفلي المعتمد على M3
│   └── ui/                              // مكوّنات الواجهة المشتركة
│       └── CommonComponents.kt          // شريط العنوان العلوي وبطاقات التأسيس والوسوم
├── features/                            // الميزات المستقلة (Feature Packages)
│   ├── dashboard/                       // لوحة التحكم ومراقبة البنية
│   │   └── DashboardScreen.kt
│   ├── inventory/                       // المخزون والمستودعات (مرحلة 2)
│   │   └── InventoryScreen.kt
│   ├── operations/                      // المشتريات والمبيعات (مرحلة 3)
│   │   └── OperationsScreen.kt
│   ├── reports/                         // التقارير المالية والأرباح (مرحلة 4)
│   │   └── ReportsScreen.kt
│   └── settings/                        // إعدادات النظام والعملة
│       └── SettingsScreen.kt
└── ui/theme/                            // نظام التصميم والمظهر
    ├── Color.kt                         // لوحة الألوان التجارية
    ├── Theme.kt                         // تطبيق سمة Material 3 ودعم RTL
    └── Type.kt                          // مقاييس الخطوط
```

### مسؤوليات الطبقات:
1. **طبقة المجال (Domain Layer - `core/model`):** مستقلة تمامًا عن أي مكتبات أندرويد. تضمن تطبيق القواعد الحسابية والمالية بدقة مطلقة، وتمنع تشويه البيانات أو الانزلاق العشري الناتج عن الأرقام العائمة.
2. **طبقة البيانات (Data Layer - `core/database` & `core/preferences`):** مسؤولة عن التخزين المحلي الآمن والمستقر. تعتمد Room لحفظ الكيانات المعقدة، و DataStore للخيارات الخفيفة.
3. **طبقة الواجهات (Presentation Layer - `features/*` & `core/ui`):** مبنية بالكامل باستخدام Jetpack Compose بنمط تصريحي (Declarative)، وتعتمد على تدفقات `Flow` والحالات المراقبة (`State`) لعرض التغييرات تلقائيًا.

---

## E. الاعتماديات وإعدادات البناء (Dependencies & Build Configuration)

تم استخدام الاعتماديات المتوافقة والمثبتة مسبقًا في كتالوج الإصدارات `libs.versions.toml` دون إدخال مكتبات غير موثوقة:
* **Android Gradle Plugin:** `9.1.1`
* **Kotlin:** `2.2.10`
* **KSP (Kotlin Symbol Processing):** `2.3.5` (بديل KAPT لمعالجة معالجات Room و Moshi بكفاءة عالية)
* **Jetpack Compose BOM:** `2024.09.00`
* **Material Design 3:** `androidx.compose.material3:material3`
* **Room Database:** `2.7.0` (`room-runtime` و `room-ktx` مع المترجم `room-compiler` عبر KSP)
* **DataStore Preferences:** `1.1.7` (`androidx.datastore:datastore-preferences`)
* **Navigation Compose:** `2.8.9` (`androidx.navigation:navigation-compose`)
* **Coroutines:** `1.10.2` (`kotlinx-coroutines-android` و `kotlinx-coroutines-core`)
* **Testing:** `Robolectric 4.16.1` و `JUnit 4.13.2` و `kotlinx-coroutines-test 1.10.2`

---

## F. نتائج البناء والاختبارات الفعلية (Build & Test Results)

تم تنفيذ عمليات البناء والاختبار الفعلي على بيئة السحاب، وجاءت النتائج كالتالي:

### 1. فحص البناء الأولي (Baseline Compilation)
* **الأداة المستخدمة:** `compile_applet`
* **النتيجة:** `Build succeeded - the applet is compiled`
* **التوثيق:** نجح تجميع التطبيق في حالته الابتدائية قبل التعديل.

### 2. فحص بناء التطبيق النهائي مع كافة المكونات (Applet Build Verification)
* **الأداة المستخدمة:** `compile_applet`
* **النتيجة:** `Build succeeded - the applet is compiled` (زمن التنفيذ: 30 ثانية).
* **التوثيق:** تم التحقق من نجاح تجميع جميع كائنات القيمة، وقاعدة بيانات Room، والمحولات، ومستودع DataStore، وشاشات Jetpack Compose الخمس، ونظام التنقل Navigation Compose بالكامل دون أي أخطاء تجميع.

### 3. تشغيل اختبارات الوحدة واختبارات Robolectric
* **الأمر المنفذ:** `gradle :app:testDebugUnitTest`
* **النتيجة الفعلية:**
  ```
  > Task :app:compileDebugKotlin UP-TO-DATE
  > Task :app:kspDebugUnitTestKotlin
  > Task :app:compileDebugUnitTestKotlin
  > Task :app:testDebugUnitTest
  BUILD SUCCESSFUL in 23s
  33 actionable tasks: 10 executed, 23 up-to-date
  ```
* **حالات الاختبار المختبرة والمجتازة بنجاح:**
  1. `MoneyFinancialPrecisionTest.testBigDecimalPrecisionEliminatesBinaryFloatingPointErrors`: أثبت الاختبار أن حاصل جمع `0.10 SAR + 0.20 SAR` يعطي `0.30 SAR` بدقة تامة دون أي خطأ انزلاق ثنائي.
  2. `MoneyFinancialPrecisionTest.testCurrencyMismatchThrowsExceptionOnDirectAddition`: أثبت الاختبار رمي استثناء `CurrencyMismatchException` عند محاولة جمع مبلغ بالريال اليمني ومبلغ بالدولار الأمريكي مباشرة.
  3. `MoneyFinancialPrecisionTest.testCurrencyMismatchThrowsExceptionOnDirectSubtraction`: أثبت الاختبار حظر الطرح المباشر بين عملتين مختلفتين.
  4. `MoneyFinancialPrecisionTest.testMoneyScalingAndRounding`: أثبت الاختبار صحة التقريب المصرفي (`HALF_EVEN`) بدقة منزلتين عشريتين.
  5. `MoneyFinancialPrecisionTest.testMoneyMultiplicationWithFactorAndQuantity`: أثبت صحة ضرب السعر في الكمية الصحيحة.
  6. `MoneyFinancialPrecisionTest.testExchangeRateConversion`: أثبت صحة تحويل العملات عبر كائن سعر الصرف الصريح.
  7. `MoneyFinancialPrecisionTest.testQuantityValidation`: أثبت حظر الكميات السالبة في المخزون واختبار سلامة جمع الوحدات.
  8. `ExampleRobolectricTest.read string from context`: أثبت قراءة المورد النصي `R.string.app_name` ومطابقته لاسم التطبيق المعتمد "ميزان المخزون".
  9. `ExampleUnitTest.addition_isCorrect`: اجتياز الاختبار الأساسي.

---

## G. القيود والمخاطر (Constraints & Risks)

1. **تحذير واجهة AWT في بيئة KSP:** أثناء تشغيل مهمة `testDebugUnitTest` يظهر تحذير داخلي من خادم KSP حول مكتبة IntelliJ (`AWT-EventQueue-0 NullPointerException`)؛ هذا التحذير لا يعيق بناء المشروع ولا يؤثر على صحة الاختبارات، ولكنه موثق هنا للأمانة الهندسية.
2. **استراتيجية ترحيل قاعدة البيانات (Database Migrations):** قاعدة بيانات Room الحالية مهيأة في الإصدار الأول (`version = 1`). عند إضافة جداول المخزون والمبيعات في المرحلة الثانية، يجب إما استخدام استراتيجية ترحيل واضحة (`Migration(1, 2)`) أو الاعتماد على تدمير المخطط المؤقت خلال فترة التطوير المبكرة (`fallbackToDestructiveMigration`).
3. **التشغيل بدون اتصال بالشبكة (Offline First):** التطبيق مصمم حاليًا ليعمل محليًا بالكامل بدون اتصال بخوادم خارجية أو واجهات برمجة تطبيقات لأسعار الصرف، وهو ما يتوافق تمامًا مع متطلبات المرحلة الأولى.

---

## H. الوظائف غير المنفذة في هذه المرحلة (Explicitly Unimplemented Features)

التزامًا بحدود **PHASE 1** وتجنبًا للاستباق غير المنضبط، **لم يتم تنفيذ** أي من الوظائف التالية، وتُرِكت عمدًا للمراحل المخصصة لها:
- ❌ لم يتم إنشاء جداول أو شاشات إضافة وحذف الأصناف المخزنية.
- ❌ لم يتم تطبيق خصم الكميات أو تسوية المخزون الفعلي.
- ❌ لم يتم إنشاء نماذج فواتير المشتريات أو المبيعات.
- ❌ لم يتم حساب تكلفة البضاعة المباعة أو تكلفة الاستبدال أو الأرباح.
- ❌ لم يتم ربط أي مزود خارجي لأسعار الصرف عبر الإنترنت.
- ❌ لم يتم إنشاء أي بيانات وهمية لتوهم المستخدم بوجود حركات بيع أو مخزون سابقة.
- ❌ لم يتم تنفيذ التوزيع المحاسبي لتكاليف الشحنات أو إدارة ديون العملاء والموردين.

---

## I. اقتراحات ومتطلبات PHASE 2 (Recommendations for Phase 2)

بناءً على نتائج التدقيق والتأسيس المعماري، يوصى بالتركيز في المرحلة الثانية على التالي:
1. **تصميم كيان الصنف المخزني (`ItemEntity`):**
   - حقول المعرّف، الاسم، الباركود الدولي، وحدة القياس الأساسية، سعر الشراء المرجعي، سعر البيع للمستهلك، والحد الأدنى للمخزون للتنبيه.
2. **تصميم كيان المستودعات والفئات (`WarehouseEntity`, `CategoryEntity`):**
   - تنظيم شجرة التصنيفات وربط الأصناف بمستودع افتراضي.
3. **تصميم سجل حركات المخزون (`StockMovementEntity`):**
   - تسجيل كل حركة إضافة، سحب، تالف، أو تعديل جرد مع توثيق التاريخ والكمية بدقة `Quantity`.
4. **تأسيس واجهات إدارة المخزون في `features/inventory`:**
   - شاشة عرض قائمة الأصناف مع البحث والتصفية بحسب الفئة.
   - نموذج إضافة وتعديل صنف مخزني مع التحقق الصارم من صحة المدخلات.
5. **إجراء اختبارات التخزين المحلي لـ Room (DAO Testing):**
   - اختبار عمليات الإدراج والاستعلام والبحث عن الأصناف والتأكد من استرجاع المبالغ النقدية عبر `Converters` دون أي تشويه.

---

## J. قائمة القبول والتحقق (Acceptance Checklist)

- [x] **تم فحص المشروع قبل التعديل:** تم تدقيق كافة ملفات Gradle، وإصدارات Kotlin، وملف المانيفست، والبنية السابقة وتوثيقها بدقة.
- [x] **تم الحفاظ على ملفات المشروع الأساسية وقت المرحلة الأولى:** كانت الحزمة `com.example` في تلك اللقطة؛ راجع PHASE 02A لتغيير الحزمة المعتمد لاحقًا.
- [x] **تم إعداد وتوثيق البنية المعمارية:** معمارية Clean / MVVM مفصولة الطبقات (`core/model`, `core/database`, `core/preferences`, `core/navigation`, `features/*`).
- [x] **تم تجهيز أساس الواجهة والتنقل:** ثيم Material 3 تجاري راقٍ، دعم كامل للغة العربية واتجاه RTL، وشريط ملاحة سفلي يربط الشاشات الرئيسية بنظام Navigation Compose.
- [x] **تم اتخاذ قرار واضح بشأن دقة القيم المالية:** اعتماد `BigDecimal` بنسبة 100% وحظر `Float/Double` نهائيًا، مع فرض عزل العملات ومنع خلطها عبر `CurrencyMismatchException`.
- [x] **تم إجراء اختبارات البناء المتاحة:** نجاح البناء عبر `compile_applet` وتشغيل كافة اختبارات الوحدة عبر `gradle :app:testDebugUnitTest`.
- [x] **تم تسجيل النتائج الحقيقية دون ادعاءات غير مثبتة:** توثيق النتائج الفعلية لمهام Gradle والزمن الحقيقي والتحذيرات غير المؤثرة.
- [x] **تم إنشاء تقرير PHASE 1 في المجلد المطلوب:** حفظ التقرير في مسار المشروع الفعلي `PHASES/PHASE_01_PROJECT_FOUNDATION.md`.
- [x] **لا توجد وظائف من المراحل التالية نُفذت خارج نطاق هذه المرحلة:** لم يتم إنشاء أي وظائف تجارية متقدمة أو بيانات وهمية خارج نطاق التأسيس المالي والمعماري.

---

**حالة المرحلة الأولى:** **مكتملة بنجاح (COMPLETED)**  
**التوجيه القادم:** التوقف بانتظار مراجعة التقرير والمصادقة عليه قبل طلب الانتقال إلى **PHASE 2**.
