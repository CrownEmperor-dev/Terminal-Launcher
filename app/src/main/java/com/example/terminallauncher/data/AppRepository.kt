package com.example.terminallauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

class AppRepository(private val context: Context) {

    private var cachedApps: List<AppModel> = emptyList()

    init {
        refreshApps()
    }

    fun refreshApps(): List<AppModel> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        cachedApps = resolveInfos.mapNotNull { resolveInfo ->
            val label = resolveInfo.loadLabel(pm).toString().trim()
            val packageName = resolveInfo.activityInfo.packageName
            val className = resolveInfo.activityInfo.name
            if (label.isNotEmpty() && packageName != context.packageName) {
                AppModel(
                    label = label,
                    packageName = packageName,
                    className = className,
                    icon = null // Loaded lazily if needed
                )
            } else {
                null
            }
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }

        return cachedApps
    }

    fun getApps(): List<AppModel> {
        if (cachedApps.isEmpty()) {
            refreshApps()
        }
        return cachedApps
    }

    fun findApp(query: String): AppModel? {
        val apps = getApps()
        val q = query.trim().lowercase()
        if (q.isEmpty()) return null

        // 1. Exact package name match
        apps.firstOrNull { it.packageName.lowercase() == q }?.let { return it }

        // 2. Exact label match
        apps.firstOrNull { it.label.lowercase() == q }?.let { return it }

        // 3. Prefix label match
        apps.firstOrNull { it.label.lowercase().startsWith(q) }?.let { return it }

        // 4. Word-boundary or acronym match (e.g. "yt" -> "YouTube", "gc" -> "Google Chrome")
        apps.firstOrNull { matchesAcronym(it.label, q) }?.let { return it }

        // 5. Substring match
        apps.firstOrNull { it.label.lowercase().contains(q) }?.let { return it }

        return null
    }

    fun getCompletionRemaining(input: String): String? {
        val trimmed = input.lowercase()
        if (trimmed.isEmpty()) return null

        val apps = getApps()
        // Find first app label starting with input
        val match = apps.firstOrNull { it.label.lowercase().startsWith(trimmed) }
        return match?.label?.substring(input.length)
    }

    fun launchApp(app: AppModel): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(app.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun matchesAcronym(label: String, query: String): Boolean {
        val words = label.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (words.size < 2) return false
        val initials = words.map { it.first().lowercaseChar() }.joinToString("")
        return initials.startsWith(query) || initials == query
    }
}
