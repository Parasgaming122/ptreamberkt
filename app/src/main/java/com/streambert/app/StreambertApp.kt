package com.streambert.app

import android.app.Application
import android.util.Log
import com.streambert.app.data.local.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class StreambertApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Pre-initialize API config from stored key
        appScope.launch {
            try {
                val key = Prefs.getTmdbKey(this@StreambertApp)
                if (!key.isNullOrBlank()) {
                    val lang = Prefs.getTmdbLang(this@StreambertApp)
                    com.streambert.app.data.repository.MediaRepository.configureApi(key, lang)
                }
            } catch (e: Exception) {
                Log.w("StreambertApp", "Failed to init API config", e)
            }
        }
    }

    companion object {
        lateinit var instance: StreambertApp
            private set
    }
}