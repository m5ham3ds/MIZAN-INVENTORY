package com.sulo.mizan.inventory.features.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sulo.mizan.inventory.core.repository.MizanBackupService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupRestorePanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val backupService = remember { MizanBackupService(context) }
    var status by remember { mutableStateOf("النسخ الاحتياطي يتضمن البيانات المالية والمخزون والفواتير والعملات والإعدادات.") }
    var pendingRestoreUri by remember { mutableStateOf<android.net.Uri?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            status = "جارٍ إنشاء النسخة الاحتياطية…"
            runCatching { backupService.exportTo(uri) }
                .onSuccess { status = "تم تصدير النسخة الاحتياطية بنجاح." }
                .onFailure { status = "فشل التصدير: ${it.message ?: "خطأ غير معروف"}" }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestoreUri = uri
    }

    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Text("النسخ الاحتياطي والتصدير والاستعادة (PHASE 7)", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Text(status, style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {
                    val name = "MizanInventory_Backup_${SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())}.json"
                    exportLauncher.launch(name)
                }, modifier = Modifier.weight(1f)) { Text("تصدير نسخة") }
                OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/json", "application/octet-stream")) }, modifier = Modifier.weight(1f)) { Text("استعادة نسخة") }
            }
            Text("تحذير: الاستعادة تستبدل بيانات المشروع الحالية بالكامل. خزّن نسخة من البيانات الحالية أولًا.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }

    pendingRestoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text("تأكيد استعادة النسخة") },
            text = { Text("سيتم استبدال الأصناف والفواتير والحركات والصناديق والبيانات المالية الحالية بما في ملف النسخة الاحتياطية. لا يمكن التراجع عن ذلك تلقائيًا. هل تريد المتابعة؟") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRestoreUri = null
                    scope.launch {
                        status = "جارٍ التحقق من النسخة واستعادتها…"
                        runCatching { backupService.restoreFrom(uri) }
                            .onSuccess { status = "تمت الاستعادة. أعد فتح الأقسام لتحديث البيانات المعروضة." }
                            .onFailure { status = "فشلت الاستعادة، وتم إلغاء معاملة قاعدة البيانات: ${it.message ?: "ملف غير صالح"}" }
                    }
                }) { Text("استعادة واستبدال البيانات") }
            },
            dismissButton = { TextButton(onClick = { pendingRestoreUri = null }) { Text("إلغاء") } }
        )
    }
}
