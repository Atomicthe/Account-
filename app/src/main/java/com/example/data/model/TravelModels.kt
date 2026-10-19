package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "travels")
data class TravelLedger(
    @PrimaryKey val id: String,
    val userId: String = "local_user",
    val name: String, // e.g. "東京五日遊 🗼", "京都紅葉行 🍁"
    val destination: String = "", // e.g. "日本", "歐洲"
    val currency: String = "JPY", // Default currency for this trip
    val budget: Double = 0.0, // Trip budget in trip currency
    val startDate: String = "",
    val endDate: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
