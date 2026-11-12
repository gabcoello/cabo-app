package com.mercadosmart.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
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

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(body)
        .setStyle(NotificationCompat.BigTextStyle().bigText(body))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Lembrete de Compras"
        val note = intent.getStringExtra("note") ?: "Hora de conferir sua lista de compras!"
        showAppNotification(context, "🛒 $title", note)
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
    private fun scheduleExact(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
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

        val parts = reminder.time.split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: 9
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hours)
            set(Calendar.MINUTE, minutes)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            val dayMap = mapOf(
                "Domingo" to Calendar.SUNDAY,
                "Segunda" to Calendar.MONDAY,
                "Terça" to Calendar.TUESDAY,
                "Quarta" to Calendar.WEDNESDAY,
                "Quinta" to Calendar.THURSDAY,
                "Sexta" to Calendar.FRIDAY,
                "Sábado" to Calendar.SATURDAY
            )

            val targetDay = dayMap[reminder.dayOfWeek]
            if (targetDay != null) {
                while (get(Calendar.DAY_OF_WEEK) != targetDay || timeInMillis <= System.currentTimeMillis()) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            } else if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        scheduleExact(alarmManager, calendar.timeInMillis, pendingIntent)
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
