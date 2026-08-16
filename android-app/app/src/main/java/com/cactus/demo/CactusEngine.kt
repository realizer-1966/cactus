package com.cactus.demo

import com.cactus.CactusJNI

/**
 * Thin wrapper around the Cactus JNI bindings for the demo app.
 * Loads the native library and exposes a simple chat-completion API.
 */
object CactusEngine {
    private var handle: Long = 0L

    val isLoaded: Boolean
        get() = handle != 0L

    /**
     * Initialize the engine with a model directory.
     * @param modelDir absolute path to the folder containing the Cactus weights.
     */
    fun init(modelDir: String) {
        if (handle != 0L) return
        handle = CactusJNI.nativeInit(modelDir, null, false)
        if (handle == 0L) {
            throw RuntimeException(CactusJNI.nativeGetLastError().ifEmpty { "Failed to init Cactus" })
        }
    }

    /**
     * Run a chat completion. Returns the raw JSON response from the engine.
     */
    fun complete(messagesJson: String, optionsJson: String? = null, toolsJson: String? = null): String {
        checkLoaded()
        val buffer = ByteArray(1024 * 1024)
        val rc = CactusJNI.nativeComplete(handle, messagesJson, buffer, optionsJson, toolsJson, null, null)
        if (rc < 0) throw RuntimeException(CactusJNI.nativeGetLastError().ifEmpty { "complete failed" })
        return buffer.decodeToString().trimEnd('\u0000')
    }

    fun reset() {
        if (handle != 0L) CactusJNI.nativeReset(handle)
    }

    fun destroy() {
        if (handle != 0L) {
            CactusJNI.nativeDestroy(handle)
            handle = 0L
        }
    }

    private fun checkLoaded() {
        if (handle == 0L) throw IllegalStateException("Cactus engine not initialized")
    }
}
