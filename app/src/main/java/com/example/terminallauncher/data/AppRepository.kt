package com.example.terminallauncher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

class AppRepository(private val context: Context) {

    private var cachedApps: List<AppModel> = emptyList()
    private var autoAliases: Map<String, AppModel> = emptyMap()

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

        generateAutoAliases()
        return cachedApps
    }

    private fun generateAutoAliases() {
        val aliases = mutableMapOf<String, AppModel>()
        val groupedByLabel = cachedApps.groupBy { it.label.lowercase() }

        groupedByLabel.filter { it.value.size > 1 }.forEach { (lowercasedLabel, appList) ->
            val cleanBaseLabel = lowercasedLabel.replace(" ", "")
            appList.forEachIndexed { idx, app ->
                val pkgLower = app.packageName.lowercase()
                val knownBrand = listOf("google", "blackberry", "samsung", "xiaomi", "huawei", "sony", "lg", "motorola", "oneplus", "microsoft", "sec")
                    .firstOrNull { pkgLower.contains(it) }

                val pkgParts = app.packageName.split(".").filter { it.isNotEmpty() }
                val lastPart = pkgParts.lastOrNull() ?: (idx + 1).toString()
                val genericWords = setOf("app", "apps", "android", "mobile", "client", "ui")

                val suffix = when {
                    knownBrand != null -> knownBrand
                    pkgParts.size >= 2 && !genericWords.contains(pkgParts[pkgParts.size - 2].lowercase()) -> pkgParts[pkgParts.size - 2].lowercase()
                    else -> lastPart.lowercase()
                }

                var aliasKey = "$cleanBaseLabel.$suffix"
                if (aliases.containsKey(aliasKey)) {
                    aliasKey = "$cleanBaseLabel.${lastPart.lowercase()}"
                }
                if (aliases.containsKey(aliasKey)) {
                    aliasKey = "$cleanBaseLabel.${idx + 1}"
                }
                aliases[aliasKey] = app
            }
        }
        autoAliases = aliases
    }

    fun getApps(): List<AppModel> {
        if (cachedApps.isEmpty()) {
            refreshApps()
        }
        return cachedApps
    }

    fun getAutoAliases(): Map<String, AppModel> {
        return autoAliases
    }

    fun findApp(query: String): AppModel? {
        val apps = getApps()
        val q = query.trim().lowercase()
        if (q.isEmpty()) return null

        // 1. Check auto-generated disambiguation aliases (e.g. files.google, files.blackberry)
        autoAliases[q]?.let { return it }

        // 2. Exact package name match
        apps.firstOrNull { it.packageName.lowercase() == q }?.let { return it }

        // 3. Exact label match
        apps.firstOrNull { it.label.lowercase() == q }?.let { return it }

        // 4. Prefix label match
        apps.firstOrNull { it.label.lowercase().startsWith(q) }?.let { return it }

        // 5. Word-boundary or acronym match (e.g. "yt" -> "YouTube", "gc" -> "Google Chrome")
        apps.firstOrNull { matchesAcronym(it.label, q) }?.let { return it }

        // 6. Substring match
        apps.firstOrNull { it.label.lowercase().contains(q) }?.let { return it }

        return null
    }

    fun findDuplicateApps(query: String): List<AppModel> {
        val apps = getApps()
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()

        val exactMatches = apps.filter { it.label.lowercase() == q }
        if (exactMatches.size > 1) return exactMatches

        val prefixMatches = apps.filter { it.label.lowercase().startsWith(q) }
        if (prefixMatches.size > 1) return prefixMatches

        return emptyList()
    }

    fun getCompletionRemaining(input: String): String? {
        val trimmed = input.lowercase()
        if (trimmed.isEmpty()) return null

        val apps = getApps()
        // Check auto-generated aliases first
        val autoMatch = autoAliases.keys.firstOrNull { it.startsWith(trimmed) }
        if (autoMatch != null) {
            return autoMatch.substring(input.length)
        }

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
        } catch (_: Exception) {
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
