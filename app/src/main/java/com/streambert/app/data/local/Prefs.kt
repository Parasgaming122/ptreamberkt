package com.streambert.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.streambert.app.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "streambert_prefs")

object Prefs {
    // ── Preference keys ──
    private val TMDB_KEY = stringPreferencesKey("tmdb_key")
    private val IS_TV_MODE = booleanPreferencesKey("is_tv_mode")
    private val START_PAGE = stringPreferencesKey("start_page")
    private val THEME = stringPreferencesKey("theme")
    private val ACCENT_COLOR = stringPreferencesKey("accent_color")
    private val FONT_SIZE = stringPreferencesKey("font_size")
    private val COMPACT_MODE = booleanPreferencesKey("compact_mode")
    private val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
    private val ACCENT_IN_PLAYER = booleanPreferencesKey("accent_in_player")
    private val CUSTOM_THEME = stringPreferencesKey("custom_theme_vars")
    private val TMDB_LANG = stringPreferencesKey("tmdb_lang")
    private val AGE_LIMIT = stringPreferencesKey("age_limit")
    private val RATING_COUNTRY = stringPreferencesKey("rating_country")
    private val WATCHED_THRESHOLD = intPreferencesKey("watched_threshold")
    private val HISTORY_ENABLED = booleanPreferencesKey("history_enabled")
    private val LIBRARY_SORT = stringPreferencesKey("library_sort")
    private val PLAYER_SOURCE = stringPreferencesKey("player_source")
    private val ALLMANGA_DUB_MODE = stringPreferencesKey("allmanga_dub_mode")
    private val AUTOPLAY_NEXT_ENABLED = booleanPreferencesKey("autoplay_next_enabled")
    private val AUTOPLAY_NEXT_DURATION = intPreferencesKey("autoplay_next_duration")
    private val AUTOPLAY_NEXT_LAYOUT = stringPreferencesKey("autoplay_next_layout")
    private val INTRO_SKIP_MODE = stringPreferencesKey("intro_skip_mode")
    private val INVIDIOUS_BASE = stringPreferencesKey("invidious_base")
    private val HOME_ROW_ORDER = stringPreferencesKey("home_row_order")
    private val HOME_ROW_VISIBLE = stringPreferencesKey("home_row_visible")
    private val HOME_VIEW_MODE = stringPreferencesKey("home_view_mode")
    private val SUBTITLE_ENABLED = booleanPreferencesKey("subtitle_enabled")
    private val SUBTITLE_LANG = stringPreferencesKey("subtitle_lang")
    private val NOTIFY_NEW_EPISODE = booleanPreferencesKey("notify_new_episode")
    private val WATCH_PROGRESS = stringPreferencesKey("watch_progress")
    private val WATCHED = stringPreferencesKey("watched")
    private val HISTORY = stringPreferencesKey("watch_history")
    private val SAVED = stringPreferencesKey("saved")
    private val SAVED_ORDER = stringPreferencesKey("saved_order")
    private val SEARCH_HISTORY = stringPreferencesKey("search_history")
    private val SOURCE_FAILOVER = stringPreferencesKey("source_failover_cache")
    private val SETUP_DONE = booleanPreferencesKey("setup_done")

    private val gson = Gson()

    // ── TV Mode ──
    suspend fun setTvMode(ctx: Context, enabled: Boolean) {
        ctx.dataStore.edit { it[IS_TV_MODE] = enabled }
    }

    suspend fun isTvMode(ctx: Context): Boolean {
        return ctx.dataStore.data.first()[IS_TV_MODE] ?: false
    }

    fun isTvModeFlow(ctx: Context): Flow<Boolean> {
        return ctx.dataStore.data.map { it[IS_TV_MODE] ?: false }
    }

    // ── Setup ──
    suspend fun isSetupDone(ctx: Context): Boolean {
        return ctx.dataStore.data.first()[SETUP_DONE] ?: false
    }

    suspend fun setSetupDone(ctx: Context, done: Boolean) {
        ctx.dataStore.edit { it[SETUP_DONE] = done }
    }

    // ── TMDB API Key ──
    suspend fun getTmdbKey(ctx: Context): String? {
        return ctx.dataStore.data.first()[TMDB_KEY]
    }

    suspend fun setTmdbKey(ctx: Context, key: String) {
        ctx.dataStore.edit { it[TMDB_KEY] = key }
    }

    // ── Theme ──
    suspend fun getTheme(ctx: Context): String {
        return ctx.dataStore.data.first()[THEME] ?: "dark"
    }

    suspend fun setTheme(ctx: Context, theme: String) {
        ctx.dataStore.edit { it[THEME] = theme }
    }

    suspend fun getAccentColor(ctx: Context): String {
        return ctx.dataStore.data.first()[ACCENT_COLOR] ?: "#e50914"
    }

    suspend fun setAccentColor(ctx: Context, color: String) {
        ctx.dataStore.edit { it[ACCENT_COLOR] = color }
    }

    // ── Language ──
    suspend fun getTmdbLang(ctx: Context): String {
        return ctx.dataStore.data.first()[TMDB_LANG] ?: "en-US"
    }

    suspend fun setTmdbLang(ctx: Context, lang: String) {
        ctx.dataStore.edit { it[TMDB_LANG] = lang }
    }

    // ── Age Rating ──
    suspend fun getAgeLimit(ctx: Context): Int? {
        val raw = ctx.dataStore.data.first()[AGE_LIMIT]
        return if (raw.isNullOrEmpty()) null else raw.toIntOrNull()
    }

    suspend fun setAgeLimit(ctx: Context, limit: Int?) {
        ctx.dataStore.edit { it[AGE_LIMIT] = limit?.toString() ?: "" }
    }

    suspend fun getRatingCountry(ctx: Context): String {
        return ctx.dataStore.data.first()[RATING_COUNTRY] ?: "US"
    }

    suspend fun setRatingCountry(ctx: Context, country: String) {
        ctx.dataStore.edit { it[RATING_COUNTRY] = country }
    }

    // ── Watch History ──
    suspend fun getHistory(ctx: Context): List<HistoryEntry> {
        val json = ctx.dataStore.data.first()[HISTORY] ?: "[]"
        return try { gson.fromJson(json, object : TypeToken<List<HistoryEntry>>() {}.type) } catch (_: Exception) { emptyList() }
    }

    suspend fun setHistory(ctx: Context, list: List<HistoryEntry>) {
        val trimmed = list.take(100)
        ctx.dataStore.edit { it[HISTORY] = gson.toJson(trimmed) }
    }

    suspend fun addHistoryEntry(ctx: Context, entry: HistoryEntry) {
        val current = getHistory(ctx).toMutableList()
        val key = "${entry.mediaType}_${entry.id}_s${entry.season ?: ""}e${entry.episode ?: ""}"
        val idx = current.indexOfFirst {
            "${it.mediaType}_${it.id}_s${it.season ?: ""}e${it.episode ?: ""}" == key
        }
        if (idx >= 0) current.removeAt(idx)
        current.add(0, entry)
        setHistory(ctx, current)
    }

    suspend fun removeHistoryEntry(ctx: Context, id: Int, mediaType: String, season: Int?, episode: Int?) {
        val current = getHistory(ctx).filterNot {
            it.id == id && it.mediaType == mediaType && it.season == season && it.episode == episode
        }
        setHistory(ctx, current)
    }

    // ── Watch Progress ──
    suspend fun getProgress(ctx: Context): Map<String, Float> {
        val json = ctx.dataStore.data.first()[WATCH_PROGRESS] ?: "{}"
        return try { gson.fromJson(json, object : TypeToken<Map<String, Float>>() {}.type) } catch (_: Exception) { emptyMap() }
    }

    suspend fun setProgress(ctx: Context, progress: Map<String, Float>) {
        ctx.dataStore.edit { it[WATCH_PROGRESS] = gson.toJson(progress) }
    }

    suspend fun updateProgress(ctx: Context, key: String, percentage: Float) {
        val current = getProgress(ctx).toMutableMap()
        current[key] = percentage
        setProgress(ctx, current)
    }

    // ── Watched ──
    suspend fun getWatched(ctx: Context): Map<String, Boolean> {
        val json = ctx.dataStore.data.first()[WATCHED] ?: "{}"
        return try { gson.fromJson(json, object : TypeToken<Map<String, Boolean>>() {}.type) } catch (_: Exception) { emptyMap() }
    }

    suspend fun setWatched(ctx: Context, watched: Map<String, Boolean>) {
        ctx.dataStore.edit { it[WATCHED] = gson.toJson(watched) }
    }

    suspend fun markWatched(ctx: Context, key: String) {
        val current = getWatched(ctx).toMutableMap()
        current[key] = true
        setWatched(ctx, current)
    }

    suspend fun markUnwatched(ctx: Context, key: String) {
        val current = getWatched(ctx).toMutableMap()
        current.remove(key)
        setWatched(ctx, current)
    }

    // ── Saved / Watchlist ──
    suspend fun getSaved(ctx: Context): Map<String, SavedItem> {
        val json = ctx.dataStore.data.first()[SAVED] ?: "{}"
        return try { gson.fromJson(json, object : TypeToken<Map<String, SavedItem>>() {}.type) } catch (_: Exception) { emptyMap() }
    }

    suspend fun setSaved(ctx: Context, saved: Map<String, SavedItem>) {
        ctx.dataStore.edit { it[SAVED] = gson.toJson(saved) }
    }

    suspend fun toggleSaved(ctx: Context, item: SavedItem) {
        val current = getSaved(ctx).toMutableMap()
        val key = "${item.mediaType}_${item.id}"
        if (current.containsKey(key)) current.remove(key)
        else current[key] = item
        setSaved(ctx, current)
    }

    suspend fun isSaved(ctx: Context, id: Int, mediaType: String): Boolean {
        return getSaved(ctx).containsKey("${mediaType}_$id")
    }

    suspend fun getSavedOrder(ctx: Context): List<String> {
        val json = ctx.dataStore.data.first()[SAVED_ORDER] ?: "[]"
        return try { gson.fromJson(json, object : TypeToken<List<String>>() {}.type) } catch (_: Exception) { emptyList() }
    }

    suspend fun setSavedOrder(ctx: Context, order: List<String>) {
        ctx.dataStore.edit { it[SAVED_ORDER] = gson.toJson(order) }
    }

    // ── Search History ──
    suspend fun getSearchHistory(ctx: Context): List<String> {
        val json = ctx.dataStore.data.first()[SEARCH_HISTORY] ?: "[]"
        return try { gson.fromJson(json, object : TypeToken<List<String>>() {}.type) } catch (_: Exception) { emptyList() }
    }

    suspend fun addSearchTerm(ctx: Context, term: String) {
        val current = getSearchHistory(ctx).toMutableList()
        current.remove(term)
        current.add(0, term)
        ctx.dataStore.edit { it[SEARCH_HISTORY] = gson.toJson(current.take(12)) }
    }

    suspend fun removeSearchTerm(ctx: Context, term: String) {
        val current = getSearchHistory(ctx).filterNot { it == term }
        ctx.dataStore.edit { it[SEARCH_HISTORY] = gson.toJson(current) }
    }

    suspend fun clearSearchHistory(ctx: Context) {
        ctx.dataStore.edit { it[SEARCH_HISTORY] = "[]" }
    }

    // ── Settings getters/setters ──
    suspend fun getWatchedThreshold(ctx: Context): Int {
        return ctx.dataStore.data.first()[WATCHED_THRESHOLD] ?: 20
    }

    suspend fun setWatchedThreshold(ctx: Context, seconds: Int) {
        ctx.dataStore.edit { it[WATCHED_THRESHOLD] = seconds }
    }

    suspend fun isHistoryEnabled(ctx: Context): Boolean {
        return ctx.dataStore.data.first()[HISTORY_ENABLED] ?: true
    }

    suspend fun setHistoryEnabled(ctx: Context, enabled: Boolean) {
        ctx.dataStore.edit { it[HISTORY_ENABLED] = enabled }
    }

    suspend fun getPlayerSource(ctx: Context): String {
        return ctx.dataStore.data.first()[PLAYER_SOURCE] ?: ""
    }

    suspend fun setPlayerSource(ctx: Context, sourceId: String) {
        ctx.dataStore.edit { it[PLAYER_SOURCE] = sourceId }
    }

    suspend fun getAllmangaDubMode(ctx: Context): String {
        return ctx.dataStore.data.first()[ALLMANGA_DUB_MODE] ?: "sub"
    }

    suspend fun setAllmangaDubMode(ctx: Context, mode: String) {
        ctx.dataStore.edit { it[ALLMANGA_DUB_MODE] = mode }
    }

    suspend fun isAutoplayNextEnabled(ctx: Context): Boolean {
        return ctx.dataStore.data.first()[AUTOPLAY_NEXT_ENABLED] ?: true
    }

    suspend fun setAutoplayNextEnabled(ctx: Context, enabled: Boolean) {
        ctx.dataStore.edit { it[AUTOPLAY_NEXT_ENABLED] = enabled }
    }

    suspend fun getAutoplayNextDuration(ctx: Context): Int {
        return ctx.dataStore.data.first()[AUTOPLAY_NEXT_DURATION] ?: 5
    }

    suspend fun setAutoplayNextDuration(ctx: Context, seconds: Int) {
        ctx.dataStore.edit { it[AUTOPLAY_NEXT_DURATION] = seconds }
    }

    suspend fun getIntroSkipMode(ctx: Context): String {
        return ctx.dataStore.data.first()[INTRO_SKIP_MODE] ?: "off"
    }

    suspend fun setIntroSkipMode(ctx: Context, mode: String) {
        ctx.dataStore.edit { it[INTRO_SKIP_MODE] = mode }
    }

    suspend fun getInvidiousBase(ctx: Context): String {
        return ctx.dataStore.data.first()[INVIDIOUS_BASE] ?: "https://inv.nadeko.net"
    }

    suspend fun setInvidiousBase(ctx: Context, url: String) {
        ctx.dataStore.edit { it[INVIDIOUS_BASE] = url }
    }

    suspend fun getCompactMode(ctx: Context): Boolean {
        return ctx.dataStore.data.first()[COMPACT_MODE] ?: false
    }

    suspend fun setCompactMode(ctx: Context, enabled: Boolean) {
        ctx.dataStore.edit { it[COMPACT_MODE] = enabled }
    }

    suspend fun getHomeViewMode(ctx: Context): String {
        return ctx.dataStore.data.first()[HOME_VIEW_MODE] ?: "carousel"
    }

    suspend fun setHomeViewMode(ctx: Context, mode: String) {
        ctx.dataStore.edit { it[HOME_VIEW_MODE] = mode }
    }

    suspend fun getHomeRowOrder(ctx: Context): List<String> {
        val json = ctx.dataStore.data.first()[HOME_ROW_ORDER] ?: null
        return if (json != null) {
            try { gson.fromJson(json, object : TypeToken<List<String>>() {}.type) } catch (_: Exception) { defaultRowOrder() }
        } else defaultRowOrder()
    }

    suspend fun setHomeRowOrder(ctx: Context, order: List<String>) {
        ctx.dataStore.edit { it[HOME_ROW_ORDER] = gson.toJson(order) }
    }

    suspend fun getHomeRowVisible(ctx: Context): Map<String, Boolean> {
        val json = ctx.dataStore.data.first()[HOME_ROW_VISIBLE] ?: null
        return if (json != null) {
            try { gson.fromJson(json, object : TypeToken<Map<String, Boolean>>() {}.type) } catch (_: Exception) { defaultRowVisible() }
        } else defaultRowVisible()
    }

    suspend fun setHomeRowVisible(ctx: Context, visible: Map<String, Boolean>) {
        ctx.dataStore.edit { it[HOME_ROW_VISIBLE] = gson.toJson(visible) }
    }

    fun defaultRowOrder(): List<String> = listOf("continue", "recommended", "trendingMovies", "trendingTV", "topRated")
    fun defaultRowVisible(): Map<String, Boolean> = mapOf(
        "continue" to true, "recommended" to true, "trendingMovies" to true,
        "trendingTV" to true, "topRated" to true,
    )

    // ── Source failover cache ──
    suspend fun getFailoverSource(ctx: Context, epKey: String): String? {
        val json = ctx.dataStore.data.first()[SOURCE_FAILOVER] ?: "{}"
        val map: Map<String, FailoverEntry> = try {
            gson.fromJson(json, object : TypeToken<Map<String, FailoverEntry>>() {}.type)
        } catch (_: Exception) { emptyMap() }
        return map[epKey]?.sourceId
    }

    suspend fun setFailoverSource(ctx: Context, epKey: String, sourceId: String) {
        val json = ctx.dataStore.data.first()[SOURCE_FAILOVER] ?: "{}"
        val map: MutableMap<String, FailoverEntry> = try {
            gson.fromJson(json, object : TypeToken<Map<String, FailoverEntry>>() {}.type)
        } catch (_: Exception) { mutableMapOf() }
        if (map.size >= 200) {
            val sorted = map.entries.sortedBy { it.value.ts }
            val toRemove = sorted.take(map.size - 199)
            toRemove.forEach { map.remove(it.key) }
        }
        map[epKey] = FailoverEntry(sourceId, System.currentTimeMillis())
        ctx.dataStore.edit { it[SOURCE_FAILOVER] = gson.toJson(map) }
    }

    // ── Backup/Restore ──
    suspend fun exportAll(ctx: Context): String {
        val data = ctx.dataStore.data.first().asMap()
            .filterKeys { it != IS_TV_MODE.name && it != SETUP_DONE.name }
            .mapKeys { it.key.name }
        return gson.toJson(data)
    }

    suspend fun importAll(ctx: Context, json: String): Boolean {
        return try {
            val map: Map<String, String> = gson.fromJson(json, object : TypeToken<Map<String, String>>() {}.type)
            ctx.dataStore.edit { prefs ->
                for ((key, value) in map) {
                    val prefKey = stringPreferencesKey(key)
                    prefs[prefKey] = value
                }
            }
            true
        } catch (_: Exception) { false }
    }

    suspend fun clearAll(ctx: Context) {
        ctx.dataStore.edit { it.clear() }
    }

    private data class FailoverEntry(val sourceId: String, val ts: Long)
}