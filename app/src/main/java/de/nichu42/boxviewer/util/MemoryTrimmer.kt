package de.nichu42.boxviewer.util

import android.content.ComponentCallbacks2

/**
 * Central registry for memory-trim callbacks. ViewModels and singletons register
 * a lambda that clears their in-memory caches when the OS signals memory pressure.
 * Called from [de.nichu42.boxviewer.BoxViewerApplication.onTrimMemory].
 */
object MemoryTrimmer {
    private val callbacks = mutableListOf<() -> Unit>()

    fun register(callback: () -> Unit) {
        synchronized(callbacks) { callbacks.add(callback) }
    }

    fun trim(level: Int) {
        val aggressive = level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND
                || level >= ComponentCallbacks2.TRIM_MEMORY_MODERATE
                || level >= ComponentCallbacks2.TRIM_MEMORY_COMPLETE
                || level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN

        if (!aggressive) return

        synchronized(callbacks) {
            for (cb in callbacks) {
                try { cb() } catch (_: Exception) { }
            }
        }

        // Always clear global singletons that survive ViewModel lifecycle
        try { ApiLogger.responseCache.clear() } catch (_: Exception) { }
        try { de.nichu42.boxviewer.data.repository.SenseBoxRepository.clearMemoryCaches() } catch (_: Exception) { }
    }

    fun trimAllForLowMemory() {
        trim(ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
    }
}
