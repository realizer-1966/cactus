package com.cactus.demo

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * Downloads a Cactus CQ model bundle from HuggingFace and extracts it into
 * app-private storage, then returns the bundle directory path.
 *
 * Bundle format (from Cactus-Compute/<model> repo):
 *   weights/<model>-cq<bits>.zip  ->  contains config.txt + components/manifest.json
 */
object ModelDownloader {

    data class Progress(val bytesDownloaded: Long, val totalBytes: Long) {
        val fraction: Float
            get() = if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f
    }

    private const val HF_BASE = "https://huggingface.co"
    private const val USER_AGENT = "cactus-android-demo/1.0"

    /**
     * Download and extract a model bundle.
     * @param modelId e.g. "Cactus-Compute/gemma-3-270m-it"
     * @param bits quantization bits (4 or 8)
     * @param onProgress callback with download progress
     * @return absolute path to the extracted bundle directory
     */
    suspend fun downloadModel(
        context: Context,
        modelId: String,
        bits: Int = 4,
        onProgress: (Progress) -> Unit = {}
    ): String = withContext(Dispatchers.IO) {
        val repoName = modelId.substringAfterLast('/').lowercase()
        val archiveName = "weights/$repoName-int$bits.zip"
        val url = "$HF_BASE/$modelId/resolve/main/$archiveName"

        val modelsDir = File(context.filesDir, "cactus-models")
        modelsDir.mkdirs()
        val bundleDir = File(modelsDir, "$repoName-cq$bits")

        // Already downloaded?
        if (File(bundleDir, "components/manifest.json").exists()) {
            return@withContext bundleDir.absolutePath
        }

        // Download to temp file
        val tmpZip = File(modelsDir, "$repoName-int$bits.zip.tmp")
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", USER_AGENT)
        conn.connectTimeout = 30_000
        conn.readTimeout = 60_000
        conn.instanceFollowRedirects = true

        val total = conn.contentLengthLong
        conn.inputStream.use { input ->
            FileOutputStream(tmpZip).use { output ->
                val buffer = ByteArray(64 * 1024)
                var downloaded = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read == -1) break
                    output.write(buffer, 0, read)
                    downloaded += read
                    onProgress(Progress(downloaded, total))
                }
            }
        }
        conn.disconnect()

        // Extract
        val finalZip = File(modelsDir, "$repoName-int$bits.zip")
        if (tmpZip.renameTo(finalZip)) {
            extractZip(finalZip, bundleDir)
            finalZip.delete()
        } else {
            extractZip(tmpZip, bundleDir)
            tmpZip.delete()
        }

        if (!File(bundleDir, "components/manifest.json").exists()) {
            throw RuntimeException("Downloaded bundle is missing components/manifest.json")
        }
        bundleDir.absolutePath
    }

    private fun extractZip(zipFile: File, destDir: File) {
        if (destDir.exists()) destDir.deleteRecursively()
        destDir.mkdirs()
        ZipInputStream(zipFile.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(destDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { out ->
                        zis.copyTo(out, 64 * 1024)
                    }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}
