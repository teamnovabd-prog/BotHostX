package com.example.utils

import java.util.Locale

object FormatUtils {
    fun formatBdt(amount: Double): String {
        return "৳" + String.format(Locale.US, "%,.2f", amount)
    }

    fun formatBytes(bytes: Int): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> String.format(Locale.US, "%.2f MB", bytes / (1024.0 * 1024.0))
        }
    }

    fun formatStorageMb(mb: Int): String {
        return if (mb >= 1024) {
            String.format(Locale.US, "%.1f GB", mb / 1024.0)
        } else {
            "$mb MB"
        }
    }

    fun formatUptime(seconds: Int): String {
        if (seconds <= 0) return "0m"
        val days = seconds / 86400
        val hours = (seconds % 86400) / 3600
        val mins = (seconds % 3600) / 60
        return when {
            days > 0 -> "${days}d ${hours}h"
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
    }

    fun shortDate(iso: String?): String {
        if (iso.isNullOrBlank()) return "N/A"
        return iso.take(16).replace("T", " ")
    }
}
