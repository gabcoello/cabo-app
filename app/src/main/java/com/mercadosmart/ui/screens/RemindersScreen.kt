package com.mercadosmart.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.mercadosmart.data.model.Reminder
import com.mercadosmart.notification.AlarmScheduler
import com.mercadosmart.ui.theme.*
import com.mercadosmart.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun RemindersScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val reminders by viewModel.reminders.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var reminderToEdit by remember { mutableStateOf<Reminder?>(null) }
    var canExactAlarms by remember { mutableStateOf(AlarmScheduler.canScheduleExactAlarms(context)) }

    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        canExactAlarms = AlarmScheduler.canScheduleExactAlarms(context)
    }

    // Launcher de permissão de notificação Android 13+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Card Informativo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
                    .border(1.dp, Border, RoundedCornerShape(14.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Notifications, contentDescription = null, tint = Primary, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Lembretes com Alarme Sonoro", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Text("Configure dias e horários para planejar e repor o estoque de casa.", fontSize = 12.sp, color = TextSecondary)
                }
            }

            // Aviso para liberar Alarmes Exatos no Android 12+ caso esteja desativado
            if (!canExactAlarms && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(WarningLight)
                        .border(1.dp, Warning.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Permissão de alarme exato desativada", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                        Text("Ative para o alarme despertar exatamente no minuto escolhido.", fontSize = 11.sp, color = TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                Uri.parse("package:${context.packageName}")
                            )
                            exactAlarmSettingsLauncher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Warning),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Ativar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }

            if (reminders.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Alarm, contentDescription = null, tint = TextMuted, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Nenhum lembrete cadastrado", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Toque no botão abaixo para adicionar lembretes semanais para compras e feira.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reminders, key = { it.id }) { reminder ->
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    reminderToEdit = reminder
                                    showDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Toggle Ativo/Inativo
                                IconButton(
                                    onClick = { viewModel.toggleReminder(reminder) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        if (reminder.isActive) Icons.Default.Notifications else Icons.Default.NotificationsOff,
                                        contentDescription = null,
                                        tint = if (reminder.isActive) Primary else TextMuted
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reminder.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (reminder.isActive) TextPrimary else TextMuted,
                                        textDecoration = if (reminder.isActive) TextDecoration.None else TextDecoration.LineThrough
                                    )

                                    Row(
                                        modifier = Modifier.padding(top = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("${reminder.dayOfWeek} às ${reminder.time}", fontSize = 12.sp, color = TextSecondary)

                                        if (reminder.isActive) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Ativo",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = PrimaryDark,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(PrimaryLight)
                                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                                            )
                                        }
                                    }

                                    if (reminder.note.isNotBlank()) {
                                        Text(reminder.note, fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 2.dp))
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        reminderToEdit = reminder
                                        showDialog = true
                                    },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = PrimaryDark, modifier = Modifier.size(18.dp))
                                }

                                IconButton(
                                    onClick = { viewModel.deleteReminder(reminder) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Deletar", tint = Danger, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Botão Novo Lembrete no Rodapé
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Button(
                onClick = {
                    reminderToEdit = null
                    showDialog = true
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Novo Lembrete", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        // Diálogo de criação / edição
        if (showDialog) {
            ReminderDialog(
                reminderToEdit = reminderToEdit,
                onDismiss = {
                    showDialog = false
                    reminderToEdit = null
                },
                onSave = { saved ->
                    if (reminderToEdit == null) {
                        viewModel.addReminder(saved)
                    } else {
                        viewModel.updateReminder(saved)
                    }
                    val triggerMillis = AlarmScheduler.computeNextReminderTriggerMillis(saved)
                    val fmt = SimpleDateFormat("dd/MM 'às' HH:mm", Locale("pt", "BR"))
                    Toast.makeText(
                        context,
                        "Alarme agendado para ${fmt.format(Date(triggerMillis))}",
                        Toast.LENGTH_LONG
                    ).show()
                    showDialog = false
                    reminderToEdit = null
                }
            )
        }
    }
}

@Composable
fun ReminderDialog(
    reminderToEdit: Reminder? = null,
    onDismiss: () -> Unit,
    onSave: (Reminder) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(reminderToEdit?.title ?: "") }
    val daysOfWeek = listOf("Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado")
    val todayIdx = remember { Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1 }
    val todayName = daysOfWeek.getOrElse(todayIdx) { "Sábado" }
    var selectedDay by remember { mutableStateOf(reminderToEdit?.dayOfWeek ?: todayName) }

    // Sugere hora atual + 5 minutos quando for novo
    val defaultTime = remember {
        val cal = Calendar.getInstance().apply { add(Calendar.MINUTE, 5) }
        String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }
    var time by remember { mutableStateOf(reminderToEdit?.time ?: defaultTime) }
    var note by remember { mutableStateOf(reminderToEdit?.note ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Surface),
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (reminderToEdit == null) "Criar Lembrete" else "Editar Lembrete",
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("TÍTULO DO LEMBRETE *", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Ex: Feira de Domingo, Reposição...", color = TextMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("DIA DA SEMANA", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .horizontalScroll(rememberScrollState())
                ) {
                    daysOfWeek.forEach { d ->
                        val isSelected = selectedDay == d
                        Box(
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Primary else TagBg)
                                .clickable { selectedDay = d }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = d,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("HORÁRIO DO ALARME (HH:mm)", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it },
                    placeholder = { Text("09:00", color = TextMuted) },
                    trailingIcon = {
                        IconButton(onClick = {
                            val (h, m) = AlarmScheduler.parseHourMinute(time)
                            TimePickerDialog(
                                context,
                                { _, pickedHour, pickedMinute ->
                                    time = String.format("%02d:%02d", pickedHour, pickedMinute)
                                },
                                h,
                                m,
                                true
                            ).show()
                        }) {
                            Icon(Icons.Default.Schedule, contentDescription = "Escolher horário", tint = PrimaryDark)
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (selectedDay == todayName) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Hoje é $todayName: se o horário ainda não passou, despertará hoje.",
                        fontSize = 11.sp,
                        color = PrimaryDark
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text("ANOTAÇÃO (OPCIONAL)", style = MaterialTheme.typography.labelSmall)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    placeholder = { Text("Ex: Conferir se ainda tem café...", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth().height(70.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            val (h, m) = AlarmScheduler.parseHourMinute(time)
                            val normalizedTime = String.format("%02d:%02d", h, m)
                            onSave(
                                reminderToEdit?.copy(
                                    title = title.trim(),
                                    dayOfWeek = selectedDay,
                                    time = normalizedTime,
                                    note = note.trim()
                                ) ?: Reminder(
                                    title = title.trim(),
                                    dayOfWeek = selectedDay,
                                    time = normalizedTime,
                                    note = note.trim(),
                                    isActive = true
                                )
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = if (reminderToEdit == null) "Salvar e Ativar Alarme" else "Salvar Alterações",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
