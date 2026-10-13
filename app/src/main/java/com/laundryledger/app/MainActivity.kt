package com.laundryledger.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

private val AZ_LOCALE = Locale.forLanguageTag("az-AZ")

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val contact: String = "",
    val note: String = "",
    // Kept so existing databases and backups retain the value from earlier app versions.
    val defaultRateCents: Long? = null,
    val archived: Boolean = false
)

@Entity(tableName = "work_entries", indices = [Index("customerId"), Index("date")])
data class WorkEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Long,
    val date: String,
    val kilograms: Double? = null,
    val amountCents: Long,
    val appliedRateCents: Long? = null,
    val note: String = "",
    @ColumnInfo(defaultValue = "'IN'") val direction: String = "IN"
)

@Dao
interface LedgerDao {
    @Query("SELECT * FROM customers ORDER BY archived, name COLLATE NOCASE") fun customers(): Flow<List<Customer>>
    @Query("SELECT * FROM work_entries ORDER BY date DESC, id DESC") fun entries(): Flow<List<WorkEntry>>
    @Query("SELECT * FROM customers ORDER BY id") suspend fun allCustomers(): List<Customer>
    @Query("SELECT * FROM work_entries ORDER BY id") suspend fun allEntries(): List<WorkEntry>
    @Insert suspend fun addCustomer(customer: Customer): Long
    @Update suspend fun updateCustomer(customer: Customer)
    @Insert suspend fun addEntry(entry: WorkEntry): Long
    @Update suspend fun updateEntry(entry: WorkEntry)
    @Query("DELETE FROM work_entries WHERE id = :id") suspend fun deleteEntry(id: Long)
    @Query("DELETE FROM work_entries WHERE customerId = :customerId") suspend fun deleteCustomerEntries(customerId: Long)
    @Query("DELETE FROM customers WHERE id = :customerId") suspend fun deleteCustomer(customerId: Long)
    @Query("DELETE FROM work_entries") suspend fun clearEntries()
    @Query("DELETE FROM customers") suspend fun clearCustomers()
    @Insert suspend fun insertCustomers(items: List<Customer>)
    @Insert suspend fun insertEntries(items: List<WorkEntry>)
    @Transaction suspend fun replaceAll(customers: List<Customer>, entries: List<WorkEntry>) {
        clearEntries(); clearCustomers()
        if (customers.isNotEmpty()) insertCustomers(customers)
        if (entries.isNotEmpty()) insertEntries(entries)
    }
    @Transaction suspend fun permanentlyDeleteCustomer(customerId: Long) {
        deleteCustomerEntries(customerId)
        deleteCustomer(customerId)
    }
}

@Database(entities = [Customer::class, WorkEntry::class], version = 2, exportSchema = true)
abstract class LedgerDatabase : RoomDatabase() {
    abstract fun dao(): LedgerDao
    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `work_entries` ADD COLUMN `direction` TEXT NOT NULL DEFAULT 'IN'")
                // Older negative amounts represented money going out. Keep their net value intact.
                db.execSQL("UPDATE `work_entries` SET `direction` = 'OUT', `amountCents` = ABS(`amountCents`) WHERE `amountCents` < 0")
            }
        }

        @Volatile private var instance: LedgerDatabase? = null
        fun get(context: android.content.Context) = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context, LedgerDatabase::class.java, "laundry-ledger.db")
                .addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}

class MainActivity : ComponentActivity() {
    private val dao by lazy { LedgerDatabase.get(this).dao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val customers by dao.customers().collectAsStateWithLifecycle(emptyList())
            val entries by dao.entries().collectAsStateWithLifecycle(emptyList())
            val scope = rememberCoroutineScope()
            var backupMessage by remember { mutableStateOf("") }
            val backupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
                if (uri != null) scope.launch(Dispatchers.IO) {
                    runCatching {
                        val output = contentResolver.openOutputStream(uri) ?: error("Fayl açıla bilmədi")
                        output.bufferedWriter().use { it.write(makeBackup(dao.allCustomers(), dao.allEntries())) }
                    }.onSuccess { withContext(Dispatchers.Main) { backupMessage = "Ehtiyat nüsxəsi saxlanıldı." } }
                        .onFailure { withContext(Dispatchers.Main) { backupMessage = "Ehtiyat nüsxəsi saxlanmadı: ${it.message}" } }
                }
            }
            val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
                if (uri != null) scope.launch(Dispatchers.IO) {
                    runCatching {
                        val raw = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: error("Fayl oxuna bilmədi")
                        val parsed = parseBackup(raw)
                        dao.replaceAll(parsed.first, parsed.second)
                    }.onSuccess { withContext(Dispatchers.Main) { backupMessage = "Ehtiyat nüsxəsi bərpa edildi." } }
                        .onFailure { withContext(Dispatchers.Main) { backupMessage = "Bərpa alınmadı: ${it.message}" } }
                }
            }
            MaterialTheme(colorScheme = lightColorScheme(
                primary = androidx.compose.ui.graphics.Color(0xFF176B5B),
                secondary = androidx.compose.ui.graphics.Color(0xFFC17B43)
            )) {
                LaundryLedgerApp(
                    customers = customers,
                    entries = entries,
                    dao = dao,
                    backup = { backupLauncher.launch("camasirxana-ehtiyat-nusxesi.json") },
                    restore = { restoreLauncher.launch(arrayOf("application/json", "text/*")) },
                    backupMessage = backupMessage,
                    clearBackupMessage = { backupMessage = "" }
                )
            }
        }
    }
}

private enum class Page(val title: String) {
    OVERVIEW("İcmal"), CUSTOMERS("Müştərilər"), CALENDAR("Təqvim"), CALCULATOR("Hesablayıcı"), SETTINGS("Parametrlər"), CUSTOMER_DETAIL("Müştəri")
}

private enum class CalcOperator(val symbol: String) { ADD("+"), SUBTRACT("−"), MULTIPLY("×"), DIVIDE("÷") }

@Composable
private fun LaundryLedgerApp(
    customers: List<Customer>,
    entries: List<WorkEntry>,
    dao: LedgerDao,
    backup: () -> Unit,
    restore: () -> Unit,
    backupMessage: String,
    clearBackupMessage: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var page by remember { mutableStateOf(Page.OVERVIEW) }
    var selectedPeriod by remember { mutableStateOf("Həftə") }
    var focusDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedCustomerId by remember { mutableLongStateOf(0L) }
    var calculatorCustomerId by remember { mutableLongStateOf(0L) }
    var calculatorDate by remember { mutableStateOf(LocalDate.now().format(INPUT_DATE_FORMAT)) }
    var calculatorReason by remember { mutableStateOf("") }
    var calculatorDirection by remember { mutableStateOf("IN") }
    var calculatorSourceEntryId by remember { mutableLongStateOf(0L) }
    var showCalculatorDatePicker by remember { mutableStateOf(false) }
    var calculatorDisplay by remember { mutableStateOf("0") }
    var calculatorAccumulator by remember { mutableStateOf<BigDecimal?>(null) }
    var calculatorOperator by remember { mutableStateOf<CalcOperator?>(null) }
    var calculatorReplaceDisplay by remember { mutableStateOf(true) }
    var calculatorMessage by remember { mutableStateOf("") }

    var showCustomerDialog by remember { mutableStateOf(false) }
    var customerBeingEdited by remember { mutableStateOf<Customer?>(null) }
    var showArchived by remember { mutableStateOf(false) }
    var customerSearch by remember { mutableStateOf("") }
    var showTransactionDialog by remember { mutableStateOf(false) }
    var transactionId by remember { mutableLongStateOf(0L) }
    var transactionCustomerId by remember { mutableLongStateOf(0L) }
    var transactionDate by remember { mutableStateOf(LocalDate.now().format(INPUT_DATE_FORMAT)) }
    var transactionDirection by remember { mutableStateOf("IN") }
    var transactionAmount by remember { mutableStateOf("") }
    var transactionKilograms by remember { mutableStateOf("") }
    var transactionReason by remember { mutableStateOf("") }
    var transactionError by remember { mutableStateOf("") }
    var deleteEntryId by remember { mutableLongStateOf(0L) }
    var deleteCustomerId by remember { mutableLongStateOf(0L) }
    var confirmRestore by remember { mutableStateOf(false) }

    val activeCustomers = customers.filterNot { it.archived }
    val visibleCustomers = customers.filter { it.archived == showArchived && it.name.contains(customerSearch, ignoreCase = true) }
    val selectedCustomer = customers.firstOrNull { it.id == selectedCustomerId }
    val customerName: (Long) -> String = { id -> customers.firstOrNull { it.id == id }?.name ?: "Naməlum müştəri" }

    fun openNewTransaction(customerId: Long, direction: String = "IN") {
        transactionId = 0L
        transactionCustomerId = customerId
        transactionDate = LocalDate.now().format(INPUT_DATE_FORMAT)
        transactionDirection = direction
        transactionAmount = ""
        transactionKilograms = ""
        transactionReason = ""
        transactionError = ""
        showTransactionDialog = true
    }

    fun openExistingTransaction(entry: WorkEntry) {
        transactionId = entry.id
        transactionCustomerId = entry.customerId
        transactionDate = runCatching { LocalDate.parse(entry.date).format(INPUT_DATE_FORMAT) }.getOrDefault(entry.date)
        transactionDirection = entry.direction
        transactionAmount = centsToText(entry.amountCents)
        transactionKilograms = entry.kilograms?.let(::formatKilograms).orEmpty()
        transactionReason = entry.note
        transactionError = ""
        showTransactionDialog = true
    }

    fun saveTransaction() {
        val date = parseDate(transactionDate)
        val amountCents = textToCents(transactionAmount)
        val kilograms = transactionKilograms.trim().replace(',', '.').toDoubleOrNull()
        if (transactionCustomerId == 0L) transactionError = "Əvvəlcə müştəri seçin."
        else if (date == null) transactionError = "Tarixi gg.aa.iiii formatında daxil edin."
        else if (amountCents == null || amountCents < 0) transactionError = "Məbləği düzgün daxil edin."
        else if (transactionKilograms.isNotBlank() && (kilograms == null || kilograms < 0)) transactionError = "Kiloqramı düzgün daxil edin."
        else {
            val old = entries.firstOrNull { it.id == transactionId }
            val entry = WorkEntry(
                id = transactionId,
                customerId = transactionCustomerId,
                date = date.toString(),
                kilograms = kilograms,
                amountCents = amountCents,
                appliedRateCents = old?.appliedRateCents,
                note = transactionReason.trim(),
                direction = transactionDirection
            )
            scope.launch(Dispatchers.IO) {
                if (entry.id == 0L) dao.addEntry(entry) else dao.updateEntry(entry)
            }
            showTransactionDialog = false
        }
    }

    fun loadTransactionIntoCalculator(entry: WorkEntry) {
        selectedCustomerId = entry.customerId
        calculatorCustomerId = entry.customerId
        calculatorDate = runCatching { LocalDate.parse(entry.date).format(INPUT_DATE_FORMAT) }.getOrDefault(entry.date)
        calculatorReason = entry.note
        calculatorDirection = entry.direction
        calculatorSourceEntryId = entry.id
        calculatorDisplay = centsToText(entry.amountCents)
        calculatorAccumulator = null
        calculatorOperator = null
        calculatorReplaceDisplay = true
        calculatorMessage = "Məbləğ hesablayıcıya yükləndi."
        page = Page.CALCULATOR
    }

    fun clearCalculator() {
        calculatorDisplay = "0"
        calculatorAccumulator = null
        calculatorOperator = null
        calculatorReplaceDisplay = true
        calculatorMessage = ""
    }

    fun appendDigit(digit: String) {
        if (calculatorDisplay == "Xəta" || calculatorReplaceDisplay) {
            calculatorDisplay = if (digit == "0") "0" else digit
            calculatorReplaceDisplay = false
        } else if (calculatorDisplay.length < 18) calculatorDisplay += digit
    }

    fun appendDecimalSeparator() {
        if (calculatorDisplay == "Xəta" || calculatorReplaceDisplay) {
            calculatorDisplay = "0,"
            calculatorReplaceDisplay = false
        } else if (!calculatorDisplay.contains(',') && !calculatorDisplay.contains('.')) calculatorDisplay += ","
    }

    fun calculate(left: BigDecimal, right: BigDecimal, operator: CalcOperator): BigDecimal? = when (operator) {
        CalcOperator.ADD -> left.add(right)
        CalcOperator.SUBTRACT -> left.subtract(right)
        CalcOperator.MULTIPLY -> left.multiply(right)
        CalcOperator.DIVIDE -> if (right.compareTo(BigDecimal.ZERO) == 0) null else left.divide(right, 8, RoundingMode.HALF_UP).stripTrailingZeros()
    }

    fun pickOperator(operator: CalcOperator) {
        val current = parseCalculatorNumber(calculatorDisplay) ?: return
        val pending = calculatorOperator
        val accumulator = calculatorAccumulator
        if (pending != null && accumulator != null && !calculatorReplaceDisplay) {
            val result = calculate(accumulator, current, pending)
            if (result == null) {
                calculatorDisplay = "Xəta"
                calculatorAccumulator = null
                calculatorOperator = null
                calculatorReplaceDisplay = true
                return
            }
            calculatorAccumulator = result
            calculatorDisplay = formatCalculatorNumber(result)
        } else if (accumulator == null) calculatorAccumulator = current
        calculatorOperator = operator
        calculatorReplaceDisplay = true
    }

    fun calculateResult() {
        val left = calculatorAccumulator ?: return
        val right = parseCalculatorNumber(calculatorDisplay) ?: return
        val operator = calculatorOperator ?: return
        val result = calculate(left, right, operator)
        if (result == null) calculatorDisplay = "Xəta" else calculatorDisplay = formatCalculatorNumber(result)
        calculatorAccumulator = null
        calculatorOperator = null
        calculatorReplaceDisplay = true
    }

    fun exportCalculatorAmount() {
        val customerId = calculatorCustomerId
        val date = parseDate(calculatorDate)
        val amountCents = textToCents(calculatorDisplay)
        if (customerId == 0L || date == null || amountCents == null || amountCents < 0) {
            calculatorMessage = "Müştəri, düzgün tarix və sıfırdan kiçik olmayan məbləğ daxil edin."
            return
        }
        val source = entries.firstOrNull { it.id == calculatorSourceEntryId }
        val entry = WorkEntry(
            id = source?.id ?: 0L,
            customerId = customerId,
            date = date.toString(),
            kilograms = source?.kilograms,
            amountCents = amountCents,
            appliedRateCents = source?.appliedRateCents,
            note = calculatorReason.trim(),
            direction = calculatorDirection
        )
        scope.launch(Dispatchers.IO) {
            if (entry.id == 0L) dao.addEntry(entry) else dao.updateEntry(entry)
        }
        calculatorSourceEntryId = 0L
        calculatorMessage = if (source == null) "Əməliyyat müştəriyə əlavə edildi." else "Əməliyyat yeniləndi."
    }

    Scaffold(bottomBar = {
        NavigationBar {
            Page.entries.filter { it != Page.CUSTOMER_DETAIL }.forEach { destination ->
                NavigationBarItem(
                    selected = page == destination || (page == Page.CUSTOMER_DETAIL && destination == Page.CUSTOMERS),
                    onClick = { page = destination },
                    icon = { Text(destination.title.take(1)) },
                    label = { Text(destination.title) }
                )
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Text("Camaşırxana dəftəri", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp, bottom = 8.dp))

            when (page) {
                Page.OVERVIEW -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Gün", "Həftə", "Ay").forEach { period ->
                            FilterChip(selected = selectedPeriod == period, onClick = { selectedPeriod = period }, label = { Text(period) })
                        }
                    }
                    PeriodPicker(focusDate, { focusDate = it })
                    val subset = entries.filter { inPeriod(it.date, focusDate, selectedPeriod) }
                    Summary(subset)
                    Text("Son əməliyyatlar", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                    if (subset.isEmpty()) EmptyMessage("Bu dövr üçün əməliyyat yoxdur.")
                    else LazyColumn(Modifier.weight(1f)) {
                        items(subset.take(30), key = { it.id }) { entry ->
                            TransactionCard(entry, customerName(entry.customerId), onClick = { openExistingTransaction(entry) }, onDelete = { deleteEntryId = entry.id })
                        }
                    }
                }

                Page.CUSTOMERS -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (showArchived) "Arxivlənmiş müştərilər" else "Müştərilər", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = { showArchived = !showArchived }) { Text(if (showArchived) "Aktivlər" else "Arxiv") }
                        Button(onClick = { customerBeingEdited = null; showCustomerDialog = true }) { Text("Əlavə et") }
                    }
                    OutlinedTextField(customerSearch, { customerSearch = it }, label = { Text("Axtar") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    if (visibleCustomers.isEmpty()) EmptyMessage(if (showArchived) "Arxivdə müştəri yoxdur." else "Hələ müştəri əlavə edilməyib.")
                    else LazyColumn(Modifier.weight(1f)) {
                        items(visibleCustomers, key = { it.id }) { customer ->
                            CustomerCard(customer, onOpen = { selectedCustomerId = customer.id; page = Page.CUSTOMER_DETAIL })
                        }
                    }
                }

                Page.CUSTOMER_DETAIL -> {
                    if (selectedCustomer == null) {
                        TextButton(onClick = { page = Page.CUSTOMERS }) { Text("‹ Müştərilərə qayıt") }
                        EmptyMessage("Müştəri tapılmadı.")
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(onClick = { page = Page.CUSTOMERS }) { Text("‹ Geri") }
                            Text(selectedCustomer.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            TextButton(onClick = { customerBeingEdited = selectedCustomer; showCustomerDialog = true }) { Text("Düzəlt") }
                        }
                        if (selectedCustomer.contact.isNotBlank()) Text(selectedCustomer.contact, style = MaterialTheme.typography.bodyMedium)
                        if (selectedCustomer.note.isNotBlank()) Text(selectedCustomer.note, style = MaterialTheme.typography.bodyMedium)
                        val customerEntries = entries.filter { it.customerId == selectedCustomer.id }
                        Summary(customerEntries)
                        if (!selectedCustomer.archived) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = { openNewTransaction(selectedCustomer.id, "IN") }, modifier = Modifier.weight(1f)) { Text("Mədaxil əlavə et") }
                                OutlinedButton(onClick = { openNewTransaction(selectedCustomer.id, "OUT") }, modifier = Modifier.weight(1f)) { Text("Məxaric əlavə et") }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            if (!selectedCustomer.archived) {
                                TextButton(onClick = { scope.launch(Dispatchers.IO) { dao.updateCustomer(selectedCustomer.copy(archived = true)) } }) { Text("Arxivlə") }
                            } else {
                                TextButton(onClick = { scope.launch(Dispatchers.IO) { dao.updateCustomer(selectedCustomer.copy(archived = false)) } }) { Text("Arxivdən çıxar") }
                                TextButton(onClick = { deleteCustomerId = selectedCustomer.id }) { Text("Tam sil") }
                            }
                        }
                        Text("Əməliyyat jurnalı", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp, bottom = 4.dp))
                        if (customerEntries.isEmpty()) EmptyMessage("Bu müştəri üçün əməliyyat yoxdur.")
                        else LazyColumn(Modifier.weight(1f)) {
                            items(customerEntries, key = { it.id }) { entry ->
                                TransactionCard(
                                    entry = entry,
                                    customerName = null,
                                    onClick = { openExistingTransaction(entry) },
                                    onDelete = { deleteEntryId = entry.id },
                                    onLoadIntoCalculator = { loadTransactionIntoCalculator(entry) }
                                )
                            }
                        }
                    }
                }

                Page.CALENDAR -> {
                    Text("Təqvim və əməliyyatlar", style = MaterialTheme.typography.titleLarge)
                    LazyColumn(Modifier.weight(1f)) {
                        item {
                            MonthCalendar(focusDate, entries.map { it.date }.toSet(), { focusDate = it })
                            PeriodPicker(focusDate, { focusDate = it })
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("Gün", "Həftə", "Ay").forEach { period ->
                                    FilterChip(selected = selectedPeriod == period, onClick = { selectedPeriod = period }, label = { Text(period) })
                                }
                            }
                            val dayEntries = entries.filter { it.date == focusDate.toString() }
                            Summary(dayEntries)
                            Text("${formatDate(focusDate)} üçün əməliyyatlar", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                        }
                        val dayEntries = entries.filter { it.date == focusDate.toString() }
                        if (dayEntries.isEmpty()) item { EmptyMessage("Bu gün üçün əməliyyat yoxdur.") }
                        else items(dayEntries, key = { it.id }) { entry ->
                            TransactionCard(entry, customerName(entry.customerId), onClick = { openExistingTransaction(entry) }, onDelete = { deleteEntryId = entry.id })
                        }
                        item {
                            Text("Seçilmiş dövrün cəmi", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                            Summary(entries.filter { inPeriod(it.date, focusDate, selectedPeriod) })
                        }
                    }
                }

                Page.CALCULATOR -> {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
                        Text("Adi hesablayıcı", style = MaterialTheme.typography.titleLarge)
                        CalculatorDisplay(calculatorAccumulator, calculatorOperator, calculatorDisplay)
                        val keypad = listOf(
                            listOf("AC", "⌫", "%", "÷"),
                            listOf("7", "8", "9", "×"),
                            listOf("4", "5", "6", "−"),
                            listOf("1", "2", "3", "+"),
                            listOf("±", "0", ",", "=")
                        )
                        keypad.forEach { row ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { key ->
                                    CalculatorButton(key, Modifier.weight(1f)) {
                                        when (key) {
                                            "AC" -> clearCalculator()
                                            "⌫" -> {
                                                calculatorDisplay = if (calculatorReplaceDisplay || calculatorDisplay == "Xəta") "0" else calculatorDisplay.dropLast(1).ifBlank { "0" }
                                                calculatorReplaceDisplay = false
                                            }
                                            "%" -> parseCalculatorNumber(calculatorDisplay)?.let { value ->
                                                val base = calculatorAccumulator
                                                val percent = if (base != null && calculatorOperator in setOf(CalcOperator.ADD, CalcOperator.SUBTRACT)) {
                                                    base.multiply(value.movePointLeft(2))
                                                } else value.movePointLeft(2)
                                                calculatorDisplay = formatCalculatorNumber(percent)
                                                calculatorReplaceDisplay = true
                                            }
                                            "÷" -> pickOperator(CalcOperator.DIVIDE)
                                            "×" -> pickOperator(CalcOperator.MULTIPLY)
                                            "−" -> pickOperator(CalcOperator.SUBTRACT)
                                            "+" -> pickOperator(CalcOperator.ADD)
                                            "=" -> calculateResult()
                                            "±" -> parseCalculatorNumber(calculatorDisplay)?.let { calculatorDisplay = formatCalculatorNumber(it.negate()) }
                                            "," -> appendDecimalSeparator()
                                            else -> appendDigit(key)
                                        }
                                    }
                                }
                            }
                        }
                        Text("Nəticəni müştəriyə göndər", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
                        val calculatorCustomers = activeCustomers + customers.filter { it.archived && it.id == calculatorCustomerId }
                        CustomerPicker(calculatorCustomers, calculatorCustomerId) { id ->
                            calculatorCustomerId = id
                            if (calculatorSourceEntryId != 0L && id != entries.firstOrNull { it.id == calculatorSourceEntryId }?.customerId) {
                                calculatorSourceEntryId = 0L
                            }
                        }
                        OutlinedButton(onClick = { showCalculatorDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                            val date = parseDate(calculatorDate) ?: LocalDate.now()
                            Text("Tarix: ${formatDate(date)}  ▾")
                        }
                        DirectionPicker(calculatorDirection, { calculatorDirection = it })
                        OutlinedTextField(calculatorReason, { calculatorReason = it }, label = { Text("Səbəb (istəyə görə)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Button(onClick = { exportCalculatorAmount() }, enabled = calculatorCustomerId != 0L && textToCents(calculatorDisplay)?.let { it >= 0 } == true, modifier = Modifier.fillMaxWidth()) {
                            Text(if (calculatorSourceEntryId == 0L) "Nəticəni müştəriyə əlavə et" else "Müştəri əməliyyatını yenilə")
                        }
                        if (calculatorMessage.isNotBlank()) Text(calculatorMessage, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
                    }
                }

                Page.SETTINGS -> {
                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        Text("Parametrlər", style = MaterialTheme.typography.titleLarge)
                        Text("Məlumatlar bu cihazda saxlanılır. Ehtiyat nüsxəsini mütəmadi yaradın.", modifier = Modifier.padding(vertical = 10.dp))
                        Button(onClick = backup, modifier = Modifier.fillMaxWidth()) { Text("Ehtiyat nüsxəsini ixrac et") }
                        OutlinedButton(onClick = { confirmRestore = true }, modifier = Modifier.fillMaxWidth()) { Text("Ehtiyat nüsxəsini bərpa et") }
                        if (backupMessage.isNotBlank()) TextButton(onClick = clearBackupMessage) { Text(backupMessage) }
                    }
                }
            }
        }
    }

    if (showCustomerDialog) {
        CustomerDialog(
            customer = customerBeingEdited,
            onDismiss = { showCustomerDialog = false },
            onSave = { customer ->
                scope.launch(Dispatchers.IO) { if (customer.id == 0L) dao.addCustomer(customer) else dao.updateCustomer(customer) }
                showCustomerDialog = false
            }
        )
    }

    if (showCalculatorDatePicker) {
        val currentDate = parseDate(calculatorDate) ?: LocalDate.now()
        AlertDialog(
            onDismissRequest = { showCalculatorDatePicker = false },
            title = { Text("Tarix seçin") },
            text = {
                MonthCalendar(
                    selected = currentDate,
                    activityDates = emptySet(),
                    onSelect = { date -> calculatorDate = date.format(INPUT_DATE_FORMAT); showCalculatorDatePicker = false }
                )
            },
            confirmButton = { TextButton(onClick = { calculatorDate = LocalDate.now().format(INPUT_DATE_FORMAT); showCalculatorDatePicker = false }) { Text("Bu gün") } },
            dismissButton = { TextButton(onClick = { showCalculatorDatePicker = false }) { Text("Bağla") } }
        )
    }

    if (showTransactionDialog) {
        TransactionDialog(
            customers = if (transactionId == 0L) activeCustomers else customers,
            selectedCustomerId = transactionCustomerId,
            date = transactionDate,
            direction = transactionDirection,
            amount = transactionAmount,
            kilograms = transactionKilograms,
            reason = transactionReason,
            error = transactionError,
            isEditing = transactionId != 0L,
            onCustomer = { transactionCustomerId = it },
            onDate = { transactionDate = it },
            onDirection = { transactionDirection = it },
            onAmount = { transactionAmount = it },
            onKilograms = { transactionKilograms = it },
            onReason = { transactionReason = it },
            onSave = { saveTransaction() },
            onDismiss = { showTransactionDialog = false }
        )
    }

    if (confirmRestore) AlertDialog(
        onDismissRequest = { confirmRestore = false },
        title = { Text("Ehtiyat nüsxəsi bərpa edilsin?") },
        text = { Text("Bərpa cari müştəriləri və əməliyyatları əvəz edəcək.") },
        confirmButton = { TextButton(onClick = { confirmRestore = false; restore() }) { Text("Fayl seç") } },
        dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("Ləğv et") } }
    )

    if (deleteEntryId != 0L) AlertDialog(
        onDismissRequest = { deleteEntryId = 0L },
        title = { Text("Əməliyyat silinsin?") },
        text = { Text("Bu əməliyyat həmişəlik silinəcək.") },
        confirmButton = { TextButton(onClick = { val id = deleteEntryId; deleteEntryId = 0L; scope.launch(Dispatchers.IO) { dao.deleteEntry(id) } }) { Text("Sil") } },
        dismissButton = { TextButton(onClick = { deleteEntryId = 0L }) { Text("Ləğv et") } }
    )

    if (deleteCustomerId != 0L) AlertDialog(
        onDismissRequest = { deleteCustomerId = 0L },
        title = { Text("Müştəri və əməliyyatları silinsin?") },
        text = { Text("Müştəri və ona aid bütün əməliyyatlar bu cihazdan həmişəlik silinəcək. Lazımdırsa əvvəlcə ehtiyat nüsxəsi yaradın.") },
        confirmButton = { TextButton(onClick = { val id = deleteCustomerId; deleteCustomerId = 0L; scope.launch(Dispatchers.IO) { dao.permanentlyDeleteCustomer(id) }; page = Page.CUSTOMERS }) { Text("Həmişəlik sil") } },
        dismissButton = { TextButton(onClick = { deleteCustomerId = 0L }) { Text("Ləğv et") } }
    )
}

private val INPUT_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy", AZ_LOCALE)
private fun parseDate(text: String): LocalDate? = runCatching {
    val value = text.trim()
    if (value.contains('.')) LocalDate.parse(value, INPUT_DATE_FORMAT) else LocalDate.parse(value)
}.getOrNull()
private fun formatDate(date: LocalDate): String = date.format(INPUT_DATE_FORMAT)
private fun shortWeekdayName(day: DayOfWeek): String = when (day) {
    DayOfWeek.MONDAY -> "B.e."
    DayOfWeek.TUESDAY -> "Ç.a."
    DayOfWeek.WEDNESDAY -> "Ç."
    DayOfWeek.THURSDAY -> "C.a."
    DayOfWeek.FRIDAY -> "C."
    DayOfWeek.SATURDAY -> "Ş."
    DayOfWeek.SUNDAY -> "B."
}

@Composable
private fun PeriodPicker(date: LocalDate, onDate: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        TextButton(onClick = { onDate(date.minusDays(1)) }) { Text("‹") }
        TextButton(onClick = { onDate(LocalDate.now()) }) { Text(formatDate(date)) }
        TextButton(onClick = { onDate(date.plusDays(1)) }) { Text("›") }
    }
}

@Composable
private fun MonthCalendar(selected: LocalDate, activityDates: Set<String>, onSelect: (LocalDate) -> Unit) {
    var month by remember(selected.year, selected.month) { mutableStateOf(selected.withDayOfMonth(1)) }
    val firstDay = WeekFields.of(AZ_LOCALE).firstDayOfWeek
    val offset = (month.dayOfWeek.value - firstDay.value + 7) % 7
    val cellCount = ((offset + month.lengthOfMonth() + 6) / 7) * 7
    Column(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { month = month.minusMonths(1) }) { Text("‹") }
            Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy", AZ_LOCALE)).replaceFirstChar { it.uppercase(AZ_LOCALE) }, fontWeight = FontWeight.SemiBold)
            TextButton(onClick = { month = month.plusMonths(1) }) { Text("›") }
        }
        Row(Modifier.fillMaxWidth()) {
            (0..6).forEach { column ->
                val day = DayOfWeek.of((firstDay.value - 1 + column) % 7 + 1)
                Text(shortWeekdayName(day), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
            }
        }
        (0 until cellCount step 7).forEach { weekStart ->
            Row(Modifier.fillMaxWidth()) {
                (weekStart until weekStart + 7).forEach { cell ->
                    val dayNumber = cell - offset + 1
                    if (dayNumber in 1..month.lengthOfMonth()) {
                        val date = month.withDayOfMonth(dayNumber)
                        val hasActivity = date.toString() in activityDates
                        Box(Modifier.weight(1f).height(44.dp).clickable { onSelect(date) }, contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Text("$dayNumber", color = if (date == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontWeight = if (date == selected) FontWeight.Bold else FontWeight.Normal)
                                Text(if (hasActivity) "•" else " ", color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.height(8.dp))
                            }
                        }
                    } else Spacer(Modifier.weight(1f).height(44.dp))
                }
            }
        }
    }
}

@Composable
private fun Summary(entries: List<WorkEntry>) {
    val kilograms = entries.sumOf { it.kilograms ?: 0.0 }
    val incoming = entries.filter { it.direction == "IN" }.sumOf { it.amountCents }
    val outgoing = entries.filter { it.direction == "OUT" }.sumOf { it.amountCents }
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(0.8f)) {
                Text("Çəki", style = MaterialTheme.typography.labelMedium)
                Text("${formatKilograms(kilograms)} kq", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text("Mədaxil: ${centsToText(incoming)} ₼", style = MaterialTheme.typography.bodyMedium)
                Text("Məxaric: ${centsToText(outgoing)} ₼", style = MaterialTheme.typography.bodyMedium)
                Text("Balans: ${centsToText(incoming - outgoing)} ₼", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CustomerCard(customer: Customer, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable(onClick = onOpen)) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(customer.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                if (customer.contact.isNotBlank()) Text(customer.contact, style = MaterialTheme.typography.bodySmall)
                if (customer.archived) Text("Arxivdədir", style = MaterialTheme.typography.labelSmall)
            }
            Text("›", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun TransactionCard(
    entry: WorkEntry,
    customerName: String?,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onLoadIntoCalculator: (() -> Unit)? = null
) {
    val isOutgoing = entry.direction == "OUT"
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    if (!customerName.isNullOrBlank()) Text(customerName, fontWeight = FontWeight.SemiBold)
                    Text(entry.note.ifBlank { "Səbəb göstərilməyib" }, style = MaterialTheme.typography.bodyMedium)
                    val kgText = entry.kilograms?.let { " · ${formatKilograms(it)} kq" }.orEmpty()
                    Text("${runCatching { formatDate(LocalDate.parse(entry.date)) }.getOrDefault(entry.date)}$kgText", style = MaterialTheme.typography.bodySmall)
                }
                Text("${if (isOutgoing) "−" else "+"}${centsToText(entry.amountCents)} ₼", fontWeight = FontWeight.Bold, color = if (isOutgoing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (onLoadIntoCalculator != null) TextButton(onClick = onLoadIntoCalculator) { Text("Hesablayıcıya yüklə") }
                TextButton(onClick = onClick) { Text("Düzəlt") }
                TextButton(onClick = onDelete) { Text("Sil") }
            }
        }
    }
}

@Composable
private fun CustomerPicker(customers: List<Customer>, selected: Long, onSelect: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(customers.firstOrNull { it.id == selected }?.name ?: "Müştəri seçin")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            customers.forEach { customer -> DropdownMenuItem(text = { Text(customer.name) }, onClick = { onSelect(customer.id); expanded = false }) }
        }
    }
}

@Composable
private fun DirectionPicker(direction: String, onDirection: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = direction == "IN", onClick = { onDirection("IN") }, label = { Text("Mədaxil") })
        FilterChip(selected = direction == "OUT", onClick = { onDirection("OUT") }, label = { Text("Məxaric") })
    }
}

@Composable
private fun CalculatorDisplay(accumulator: BigDecimal?, operator: CalcOperator?, display: String) {
    Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), horizontalAlignment = Alignment.End) {
            Text(if (operator != null && accumulator != null) "${formatCalculatorNumber(accumulator)} ${operator.symbol}" else " ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(display, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, style = MaterialTheme.typography.displaySmall, maxLines = 1)
        }
    }
}

@Composable
private fun CalculatorButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isOperator = label in setOf("÷", "×", "−", "+", "=")
    FilledTonalButton(onClick = onClick, modifier = modifier.padding(vertical = 4.dp).height(58.dp)) {
        Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = if (isOperator) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Text(text, modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CustomerDialog(customer: Customer?, onDismiss: () -> Unit, onSave: (Customer) -> Unit) {
    var name by remember(customer) { mutableStateOf(customer?.name.orEmpty()) }
    var contact by remember(customer) { mutableStateOf(customer?.contact.orEmpty()) }
    var note by remember(customer) { mutableStateOf(customer?.note.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (customer == null) "Yeni müştəri" else "Müştəri məlumatları") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(name, { name = it }, label = { Text("Restoran və ya şirkətin adı") }, singleLine = true)
                OutlinedTextField(contact, { contact = it }, label = { Text("Əlaqə məlumatı (istəyə görə)") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Qeyd (istəyə görə)") }, minLines = 2)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = {
                val updated = if (customer == null) Customer(name = name.trim(), contact = contact.trim(), note = note.trim())
                else customer.copy(name = name.trim(), contact = contact.trim(), note = note.trim())
                onSave(updated)
            }) { Text("Yadda saxla") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ləğv et") } }
    )
}

@Composable
private fun TransactionDialog(
    customers: List<Customer>,
    selectedCustomerId: Long,
    date: String,
    direction: String,
    amount: String,
    kilograms: String,
    reason: String,
    error: String,
    isEditing: Boolean,
    onCustomer: (Long) -> Unit,
    onDate: (String) -> Unit,
    onDirection: (String) -> Unit,
    onAmount: (String) -> Unit,
    onKilograms: (String) -> Unit,
    onReason: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Əməliyyatı düzəlt" else "Yeni əməliyyat") },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomerPicker(customers, selectedCustomerId, onCustomer)
                DirectionPicker(direction, onDirection)
                OutlinedTextField(date, onDate, label = { Text("Tarix (gg.aa.iiii)") }, singleLine = true)
                OutlinedTextField(amount, onAmount, label = { Text("Məbləğ (₼)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(kilograms, onKilograms, label = { Text("Çəki (kq, istəyə görə)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                OutlinedTextField(reason, onReason, label = { Text("Səbəb (istəyə görə)") }, minLines = 2)
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text("Yadda saxla") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Ləğv et") } }
    )
}

private fun parseCalculatorNumber(text: String): BigDecimal? = text.trim().replace(',', '.').toBigDecimalOrNull()

private fun formatCalculatorNumber(number: BigDecimal): String {
    val rounded = number.setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()
    return (if (rounded.compareTo(BigDecimal.ZERO) == 0) "0" else rounded.toPlainString()).replace('.', ',')
}

private fun textToCents(text: String): Long? = try {
    text.trim().replace(" ", "").replace(',', '.').toBigDecimalOrNull()
        ?.movePointRight(2)?.setScale(0, RoundingMode.HALF_UP)?.longValueExact()
} catch (_: ArithmeticException) { null }

private fun centsToText(cents: Long): String = BigDecimal.valueOf(cents, 2).setScale(2).toPlainString().replace('.', ',')

private fun formatKilograms(kilograms: Double): String = BigDecimal.valueOf(kilograms)
    .setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString().replace('.', ',')

private fun inPeriod(rawDate: String, focus: LocalDate, period: String): Boolean {
    val date = runCatching { LocalDate.parse(rawDate) }.getOrNull() ?: return false
    return when (period) {
        "Gün" -> date == focus
        "Həftə" -> {
            val firstDay = WeekFields.of(AZ_LOCALE).firstDayOfWeek
            val start = focus.with(java.time.temporal.TemporalAdjusters.previousOrSame(firstDay))
            !date.isBefore(start) && date.isBefore(start.plusWeeks(1))
        }
        else -> date.year == focus.year && date.month == focus.month
    }
}

private fun makeBackup(customers: List<Customer>, entries: List<WorkEntry>): String = JSONObject().apply {
    put("format", "laundry-ledger-backup")
    put("version", 2)
    put("settings", JSONObject().put("language", "az"))
    put("customers", JSONArray().apply {
        customers.forEach { customer -> put(JSONObject().apply {
            put("id", customer.id)
            put("name", customer.name)
            put("contact", customer.contact)
            put("note", customer.note)
            if (customer.defaultRateCents != null) put("defaultRateCents", customer.defaultRateCents) else put("defaultRateCents", JSONObject.NULL)
            put("archived", customer.archived)
        }) }
    })
    put("entries", JSONArray().apply {
        entries.forEach { entry -> put(JSONObject().apply {
            put("id", entry.id)
            put("customerId", entry.customerId)
            put("date", entry.date)
            if (entry.kilograms != null) put("kilograms", entry.kilograms) else put("kilograms", JSONObject.NULL)
            put("amountCents", entry.amountCents)
            if (entry.appliedRateCents != null) put("appliedRateCents", entry.appliedRateCents) else put("appliedRateCents", JSONObject.NULL)
            put("note", entry.note)
            put("direction", entry.direction)
        }) }
    })
}.toString(2)

private fun parseBackup(raw: String): Pair<List<Customer>, List<WorkEntry>> {
    val json = JSONObject(raw)
    require(json.getString("format") == "laundry-ledger-backup") { "Ehtiyat nüsxəsinin formatı tanınmadı" }
    val version = json.getInt("version")
    require(version in 1..2) { "Ehtiyat nüsxəsinin versiyası dəstəklənmir" }
    val customers = buildList {
        val array = json.getJSONArray("customers")
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            add(Customer(
                id = item.getLong("id"),
                name = item.getString("name"),
                contact = item.optString("contact"),
                note = item.optString("note"),
                defaultRateCents = if (item.isNull("defaultRateCents")) null else item.optLong("defaultRateCents"),
                archived = item.optBoolean("archived")
            ))
        }
    }
    val customerIds = customers.map { it.id }.toSet()
    val entries = buildList {
        val array = json.getJSONArray("entries")
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val customerId = item.getLong("customerId")
            require(customerId in customerIds) { "Əməliyyat mövcud olmayan müştəriyə aiddir" }
            val direction = item.optString("direction", "IN")
            require(direction == "IN" || direction == "OUT") { "Əməliyyatın istiqaməti düzgün deyil" }
            add(WorkEntry(
                id = item.getLong("id"),
                customerId = customerId,
                date = LocalDate.parse(item.getString("date")).toString(),
                kilograms = if (item.isNull("kilograms")) null else item.getDouble("kilograms"),
                amountCents = item.getLong("amountCents").let { if (it < 0) -it else it },
                appliedRateCents = if (item.isNull("appliedRateCents")) null else item.optLong("appliedRateCents"),
                note = item.optString("note"),
                direction = if (item.getLong("amountCents") < 0) "OUT" else direction
            ))
        }
    }
    return customers to entries
}


