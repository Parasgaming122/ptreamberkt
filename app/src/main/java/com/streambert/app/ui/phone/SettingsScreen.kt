package com.streambert.app.ui.phone

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.streambert.app.data.api.AccentPresets
import com.streambert.app.data.local.Prefs
import com.streambert.app.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val ctx = LocalContext.current
    val vm: SettingsViewModel = viewModel()
    val theme by vm.theme
    val accentColor by vm.accentColor
    val tvMode by vm.tvMode
    val tmdbLang by vm.tmdbLang
    val ageLimit by vm.ageLimit
    val ratingCountry by vm.ratingCountry
    val autoplayNext by vm.autoplayNext
    val autoplayDuration by vm.autoplayDuration
    val introSkipMode by vm.introSkipMode
    val historyEnabled by vm.historyEnabled
    val watchedThreshold by vm.watchedThreshold
    val compactMode by vm.compactMode
    val invidiousBase by vm.invidiousBase

    LaunchedEffect(Unit) { vm.load(ctx) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // ── General ──
        SectionHeader("General")

        // TMDB API Read Access Token (Bearer)
        SettingsTextField(
            label = "TMDB API Read Access Token (Bearer)",
            value = vm.tmdbKey,
            onValueChange = { vm.tmdbKey.value = it },
            isSecret = true,
        )
        Button(onClick = { vm.saveTmdbKey(ctx) }, modifier = Modifier.fillMaxWidth()) {
            Text("Save API Key")
        }

        // Metadata Language
        SettingsDropdown("Metadata Language", tmdbLang, LANGUAGES) { vm.setLang(ctx, it) }

        // TV Mode toggle
        SettingsSwitch("TV Mode (requires restart)", tvMode) { vm.setTvMode(ctx, it) }

        // ── Appearance ──
        SectionHeader("Appearance")

        SettingsDropdown("Theme", theme, listOf("dark" to "Dark", "amoled" to "AMOLED", "mocha" to "Mocha", "slate" to "Slate", "light" to "Light")) { vm.setTheme(ctx, it) }

        // Accent colors
        Text("Accent Color", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AccentPresets.all.forEach { (name, color) ->
                val selected = accentColor == color.toString()
                Surface(
                    modifier = Modifier.size(40.dp).clickable { vm.setAccent(ctx, color.toString()) },
                    shape = MaterialTheme.shapes.medium,
                    color = color,
                    border = if (selected) androidx.compose.foundation.BorderStroke(3.dp, Color.White) else null,
                ) {}
            }
        }

        SettingsSwitch("Compact Mode", compactMode) { vm.setCompactMode(ctx, it) }

        // ── Content / Parental Controls ──
        SectionHeader("Content (Parental Controls)")
        SettingsDropdown("Rating Country", ratingCountry, RATING_COUNTRY_OPTIONS) { vm.setRatingCountry(ctx, it) }
        SettingsDropdown("Age Limit", ageLimit ?: "", AGE_LIMIT_OPTIONS) { vm.setAgeLimit(ctx, it) }

        // ── Playback ──
        SectionHeader("Playback")
        SettingsTextField(label = "Auto-Watched Threshold (seconds)", value = watchedThreshold.toString(), onValueChange = { it.toIntOrNull()?.let { v -> vm.setWatchedThreshold(ctx, v) } })
        SettingsSwitch("Autoplay Next Episode", autoplayNext) { vm.setAutoplayNext(ctx, it) }
        SettingsDropdown("Intro Skip Mode (Anime)", introSkipMode, listOf("off" to "Off", "auto" to "Auto Skip", "manual" to "Manual Skip")) { vm.setIntroSkip(ctx, it) }

        // ── Library ──
        SectionHeader("Library")
        SettingsSwitch("Record Watch History", historyEnabled) { vm.setHistoryEnabled(ctx, it) }

        // ── Backup & Restore ──
        SectionHeader("Backup & Restore")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = { vm.exportBackup(ctx) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export")
            }
            OutlinedButton(onClick = { vm.importBackup(ctx) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import")
            }
        }

        // ── Storage ──
        SectionHeader("Storage & Data")
        OutlinedButton(onClick = { vm.clearCache(ctx) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Clear Cache")
        }
        OutlinedButton(
            onClick = { vm.clearWatchData(ctx) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Clear Watch Progress")
        }
        OutlinedButton(
            onClick = { vm.resetApp(ctx) },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Reset App")
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

@Composable
fun SettingsTextField(label: String, value: String, onValueChange: (String) -> Unit, isSecret: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        visualTransformation = if (isSecret) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary,
        ),
    )
}

@Composable
fun SettingsSwitch(label: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        Switch(checked = checked, onCheckedChange = onChecked, colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.primary))
    }
}

@Composable
fun SettingsDropdown(label: String, current: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = options.find { it.first == current }?.second ?: current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = currentLabel,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (value, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = { onSelect(value); expanded = false },
                        colors = MenuDefaults.itemColors(
                            textColor = if (value == current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                        ),
                    )
                }
            }
        }
    }
}

// ── Settings ViewModel ──
class SettingsViewModel : ViewModel() {
    val tmdbKey = mutableStateOf("")
    val theme = mutableStateOf("dark")
    val accentColor = mutableStateOf("#e50914")
    val tvMode = mutableStateOf(false)
    val tmdbLang = mutableStateOf("en-US")
    val ageLimit = mutableStateOf<String?>(null)
    val ratingCountry = mutableStateOf("US")
    val autoplayNext = mutableStateOf(true)
    val autoplayDuration = mutableStateOf(5)
    val introSkipMode = mutableStateOf("off")
    val historyEnabled = mutableStateOf(true)
    val watchedThreshold = mutableStateOf(20)
    val compactMode = mutableStateOf(false)
    val invidiousBase = mutableStateOf("")

    fun load(ctx: android.content.Context) {
        viewModelScope.launch {
            theme.value = Prefs.getTheme(ctx)
            accentColor.value = Prefs.getAccentColor(ctx)
            tvMode.value = Prefs.isTvMode(ctx)
            tmdbLang.value = Prefs.getTmdbLang(ctx)
            ageLimit.value = Prefs.getAgeLimit(ctx)?.toString()
            ratingCountry.value = Prefs.getRatingCountry(ctx)
            autoplayNext.value = Prefs.isAutoplayNextEnabled(ctx)
            autoplayDuration.value = Prefs.getAutoplayNextDuration(ctx)
            introSkipMode.value = Prefs.getIntroSkipMode(ctx)
            historyEnabled.value = Prefs.isHistoryEnabled(ctx)
            watchedThreshold.value = Prefs.getWatchedThreshold(ctx)
            compactMode.value = Prefs.getCompactMode(ctx)
            invidiousBase.value = Prefs.getInvidiousBase(ctx)
        }
    }

    fun saveTmdbKey(ctx: android.content.Context) {
        viewModelScope.launch {
            Prefs.setTmdbKey(ctx, tmdbKey.value)
            MediaRepository.configureApi(tmdbKey.value, tmdbLang.value)
            MediaRepository.clearCache()
        }
    }

    fun setLang(ctx: android.content.Context, lang: String) {
        viewModelScope.launch {
            Prefs.setTmdbLang(ctx, lang)
            tmdbLang.value = lang
            MediaRepository.configureApi(tmdbKey.value, lang)
            MediaRepository.clearCache()
        }
    }

    fun setTvMode(ctx: android.content.Context, enabled: Boolean) {
        viewModelScope.launch { Prefs.setTvMode(ctx, enabled); tvMode.value = enabled }
    }

    fun setTheme(ctx: android.content.Context, t: String) {
        viewModelScope.launch { Prefs.setTheme(ctx, t); theme.value = t }
    }

    fun setAccent(ctx: android.content.Context, c: String) {
        viewModelScope.launch { Prefs.setAccentColor(ctx, c); accentColor.value = c }
    }

    fun setRatingCountry(ctx: android.content.Context, c: String) {
        viewModelScope.launch { Prefs.setRatingCountry(ctx, c); ratingCountry.value = c }
    }

    fun setAgeLimit(ctx: android.content.Context, v: String) {
        viewModelScope.launch {
            val age = v.ifBlank { null }?.toIntOrNull()
            Prefs.setAgeLimit(ctx, age)
            ageLimit.value = v
        }
    }

    fun setAutoplayNext(ctx: android.content.Context, enabled: Boolean) {
        viewModelScope.launch { Prefs.setAutoplayNextEnabled(ctx, enabled); autoplayNext.value = enabled }
    }

    fun setIntroSkip(ctx: android.content.Context, mode: String) {
        viewModelScope.launch { Prefs.setIntroSkipMode(ctx, mode); introSkipMode.value = mode }
    }

    fun setHistoryEnabled(ctx: android.content.Context, enabled: Boolean) {
        viewModelScope.launch { Prefs.setHistoryEnabled(ctx, enabled); historyEnabled.value = enabled }
    }

    fun setWatchedThreshold(ctx: android.content.Context, seconds: Int) {
        viewModelScope.launch { Prefs.setWatchedThreshold(ctx, seconds); watchedThreshold.value = seconds }
    }

    fun setCompactMode(ctx: android.content.Context, enabled: Boolean) {
        viewModelScope.launch { Prefs.setCompactMode(ctx, enabled); compactMode.value = enabled }
    }

    fun exportBackup(ctx: android.content.Context) {
        viewModelScope.launch {
            val json = Prefs.exportAll(ctx)
            val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("streambert_backup", json))
            android.widget.Toast.makeText(ctx, "Backup copied to clipboard (paste into a file)", Toast.LENGTH_LONG).show()
        }
    }

    fun importBackup(ctx: android.content.Context) {
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val json = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        viewModelScope.launch {
            if (Prefs.importAll(ctx, json)) {
                Toast.makeText(ctx, "Backup restored! Restart the app.", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(ctx, "Failed to import backup", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun clearCache(ctx: android.content.Context) {
        viewModelScope.launch {
            MediaRepository.clearCache()
            Toast.makeText(ctx, "Cache cleared", Toast.LENGTH_SHORT).show()
        }
    }

    fun clearWatchData(ctx: android.content.Context) {
        viewModelScope.launch {
            Prefs.setHistory(ctx, emptyList())
            Prefs.setProgress(ctx, emptyMap())
            Prefs.setWatched(ctx, emptyMap())
            Toast.makeText(ctx, "Watch data cleared", Toast.LENGTH_SHORT).show()
        }
    }

    fun resetApp(ctx: android.content.Context) {
        viewModelScope.launch {
            Prefs.clearAll(ctx)
            Toast.makeText(ctx, "App reset. Restarting...", Toast.LENGTH_LONG).show()
            Prefs.setSetupDone(ctx, false)
            ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        }
    }
}

private val LANGUAGES = listOf(
    "en-US" to "English (US)", "de-DE" to "Deutsch", "fr-FR" to "Français",
    "es-ES" to "Español", "ja-JP" to "日本語", "zh-CN" to "中文",
    "ko-KR" to "한국어", "pt-BR" to "Português", "it-IT" to "Italiano",
    "ar-SA" to "العربية", "hi-IN" to "हिन्दी", "tr-TR" to "Türkçe",
    "ru-RU" to "Русский", "pl-PL" to "Polski", "th-TH" to "ไทย",
    "vi-VN" to "Tiếng Việt", "id-ID" to "Bahasa Indonesia",
)

private val RATING_COUNTRY_OPTIONS = listOf(
    "US" to "United States (MPAA)", "DE" to "Germany (FSK)", "GB" to "UK (BBFC)",
    "FR" to "France (CNC)", "AU" to "Australia (ACB)", "NZ" to "New Zealand (OFLC)",
    "BR" to "Brazil (DEJUS)", "CA" to "Canada (CRTC)", "JP" to "Japan (EIRIN)",
)

private val AGE_LIMIT_OPTIONS = listOf(
    "" to "No restriction", "0" to "0 - All audiences (G)", "7" to "7 - Family friendly (PG)",
    "12" to "12 - Teens and up", "13" to "13 - PG-13", "15" to "15 - Older teens",
    "16" to "16 - FSK 16", "17" to "17 - R / 17+", "18" to "18 - Adults only (NC-18)",
)