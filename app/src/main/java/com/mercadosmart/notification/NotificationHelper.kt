package com.mercadosmart.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.mercadosmart.MainActivity
import com.mercadosmart.R
import com.mercadosmart.data.AppDatabase
import com.mercadosmart.data.model.PaymentBudget
import com.mercadosmart.data.model.Reminder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

private const val CHANNEL_ID = "mercado_smart_reminders"
private const val BUDGET_RECHARGE_REQUEST_OFFSET = 100_000

internal fun showAppNotification(context: Context, title: String, body: String) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            enableVibration(true)
        }
        notificationManager.createNotificationChannel(channel)
    }

    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setDefaults(NotificationCompat.DEFAULT_ALL)
        .setSound(soundUri)
        .setVibrate(longArrayOf(0, 400, 200, 400))
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "CaboApp:ReminderWakeLock"
        )
        try {
            wakeLock.acquire(5_000L)
            val title = intent.getStringExtra("title") ?: "Lembrete de Compras"
            val note = intent.getStringExtra("note") ?: "Hora de conferir sua lista de compras!"
            val reminderId = intent.getLongExtra("id", -1L)

            showAppNotification(context, "⏰ $title", note)

            if (reminderId > 0L) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val reminder = AppDatabase.getInstance(context).reminderDao()
                            .getAllOnce()
                            .firstOrNull { it.id == reminderId && it.isActive }
                        if (reminder != null) {
                            AlarmScheduler.scheduleReminder(context, reminder)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }
}

class BudgetRechargeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val budgetId = intent.getLongExtra("budgetId", -1L)
        if (budgetId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.getInstance(context).paymentBudgetDao()
                val budget = dao.getById(budgetId)
                if (budget != null && budget.rechargeDay != null && budget.rechargeAmount > 0.0) {
                    val cal = Calendar.getInstance()
                    val monthKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}"

                    val updatedBudget = if (budget.lastRechargeMonthKey != monthKey) {
                        val updated = budget.copy(
                            amount = budget.amount + budget.rechargeAmount,
                            lastRechargeMonthKey = monthKey,
                            updatedAt = System.currentTimeMillis()
                        )
                        dao.update(updated)
                        updated
                    } else {
                        budget
                    }

                    showAppNotification(
                        context,
                        "💰 Saldo Recarregado!",
                        "O saldo de ${updatedBudget.displayName()} foi recarregado com R$ ${String.format("%.2f", updatedBudget.rechargeAmount)}."
                    )

                    AlarmScheduler.scheduleBudgetRecharge(context, updatedBudget)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    db.reminderDao().getAllOnce().filter { it.isActive }.forEach {
                        AlarmScheduler.scheduleReminder(context, it)
                    }
                    db.paymentBudgetDao().getAllOnce()
                        .filter { it.rechargeDay != null && it.rechargeAmount > 0.0 }
                        .forEach {
                            AlarmScheduler.scheduleBudgetRecharge(context, it)
                        }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}

object AlarmScheduler {
    fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    private fun scheduleExact(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
                return
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    internal fun parseHourMinute(timeStr: String): Pair<Int, Int> {
        val clean = timeStr.trim().lowercase()
        val match = Regex("""(\d{1,2})\s*[:h.]\s*(\d{1,2})?""").find(clean)
        val hours = if (match != null) {
            match.groupValues[1].toIntOrNull()?.coerceIn(0, 23) ?: 9
        } else {
            clean.filter { it.isDigit() }.take(2).toIntOrNull()?.coerceIn(0, 23) ?: 9
        }
        val minutes = if (match != null && match.groupValues[2].isNotBlank()) {
            match.groupValues[2].toIntOrNull()?.coerceIn(0, 59) ?: 0
        } else {
            0
        }
        return hours to minutes
    }

    fun computeNextReminderTriggerMillis(reminder: Reminder): Long {
        val (hours, minutes) = parseHourMinute(reminder.time)
        val nowMillis = System.currentTimeMillis()

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hours)
            set(Calendar.MINUTE, minutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            val dayMap = mapOf(
                "domingo" to Calendar.SUNDAY,
                "segunda" to Calendar.MONDAY,
                "terça" to Calendar.TUESDAY,
                "terca" to Calendar.TUESDAY,
                "quarta" to Calendar.WEDNESDAY,
                "quinta" to Calendar.THURSDAY,
                "sexta" to Calendar.FRIDAY,
                "sábado" to Calendar.SATURDAY,
                "sabado" to Calendar.SATURDAY
            )

            val targetDay = dayMap[reminder.dayOfWeek.trim().lowercase()]
            if (targetDay != null) {
                if (get(Calendar.DAY_OF_WEEK) != targetDay || timeInMillis <= nowMillis) {
                    if (get(Calendar.DAY_OF_WEEK) == targetDay && timeInMillis <= nowMillis) {
                        add(Calendar.DAY_OF_YEAR, 7)
                    } else {
                        while (get(Calendar.DAY_OF_WEEK) != targetDay) {
                            add(Calendar.DAY_OF_YEAR, 1)
                        }
                    }
                }
            } else if (timeInMillis <= nowMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return calendar.timeInMillis
    }

    fun scheduleReminder(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra("title", reminder.title)
            putExtra("note", if (reminder.note.isNotBlank()) reminder.note else "Hora de conferir sua despensa e lista!")
            putExtra("id", reminder.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAtMillis = computeNextReminderTriggerMillis(reminder)

        // Usa setAlarmClock para acordar o aparelho exatamente no horário mesmo em Doze mode
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                scheduleExact(alarmManager, triggerAtMillis, pendingIntent)
                return
            }
            val showAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val showPendingIntent = PendingIntent.getActivity(
                context,
                reminder.id.toInt(),
                showAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent),
                pendingIntent
            )
        } catch (_: SecurityException) {
            scheduleExact(alarmManager, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelReminder(context: Context, reminder: Reminder) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun scheduleBudgetRecharge(context: Context, budget: PaymentBudget) {
        val targetDay = budget.rechargeDay?.coerceIn(1, 31) ?: return
        if (budget.rechargeAmount <= 0.0 || budget.id <= 0L) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BudgetRechargeReceiver::class.java).apply {
            putExtra("budgetId", budget.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BUDGET_RECHARGE_REQUEST_OFFSET + budget.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val now = Calendar.getInstance()
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.DAY_OF_MONTH, targetDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))

            if (timeInMillis <= now.timeInMillis) {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, targetDay.coerceAtMost(getActualMaximum(Calendar.DAY_OF_MONTH)))
            }
        }

        scheduleExact(alarmManager, calendar.timeInMillis, pendingIntent)
    }

    fun cancelBudgetRecharge(context: Context, budget: PaymentBudget) {
        if (budget.id <= 0L) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, BudgetRechargeReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            BUDGET_RECHARGE_REQUEST_OFFSET + budget.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
