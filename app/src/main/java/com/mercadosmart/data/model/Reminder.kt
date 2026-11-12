package com.mercadosmart.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val dayOfWeek: String = "Sábado", // "Segunda", "Terça", etc.
    val time: String = "09:00",       // Formato HH:mm
    val note: String = "",
    val isActive: Boolean = true,
    val alarmRequestCode: Int = 0     // Código do PendingIntent para cancelamento
)
