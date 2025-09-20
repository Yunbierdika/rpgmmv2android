package yunbierdika.rpgmmv2android.utils

import android.app.Activity
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class SaveFileManager(
    activity: Activity,
    private val writeLogToLocal: WriteLogToLocal
) {
    private val TAG = "SaveCache"

    // 需要跳过缓存的文件名
    private val skipFileNames: List<String> = listOf(
        "global.rpgsave",
        "config.rpgsave",
        "common.rpgsave",
    )

    // 目录：Android/data/包名/file/save
    private val saveDir = File(activity.getExternalFilesDir(null), "save")

    // 目录：Android/data/包名/cache/save
    private val cacheDir = File(activity.externalCacheDir, "save")

    init {
        if (!cacheDir.exists()) cacheDir.mkdirs()
    }

    // 加载存档
    fun loadGameData(fileName: String): String? {
        val saveFile = File(saveDir, fileName)
        if (!saveFile.exists()) {
            Log.w(TAG, "存档文件不存在: $fileName")
            return null
        }

        // 判断是否包含需要跳过的文件名
        val isContainSkipFileName = skipFileNames.contains(fileName)

        val cacheFile = File(cacheDir, "$fileName.cache")
        val metaFile = File(cacheDir, "$fileName.meta")

        // 校验缓存
        if (!isContainSkipFileName && cacheFile.exists() && metaFile.exists()) {
            val meta = metaFile.readText().split("|")
            if (meta.size == 2) {
                // 校验时间戳和文件大小
                val lastModified = meta[0].toLongOrNull()
                val size = meta[1].toLongOrNull()
                if (lastModified == saveFile.lastModified() && size == saveFile.length()) {
                    Log.d(TAG, "使用缓存: $fileName")
                    return cacheFile.readText()
                } else {
                    Log.d(TAG, "缓存失效，清除缓存: $fileName")
                    clearCache(fileName)
                }
            }
        }

        // 缓存失效 → 重新解压
        Log.d(TAG, "缓存失效，重新解压: $fileName")
        return try {
            FileInputStream(saveFile).use { fis ->
                val reader = BufferedReader(InputStreamReader(fis, StandardCharsets.UTF_8))
                val base64Data = reader.readText().trim()

                // 使用 LZString 解码
                val decodedData = LZString.decompressFromBase64(base64Data)

                // 将解码后的数据存入缓存
                if (!isContainSkipFileName) {
                    // 保存缓存
                    cacheFile.writeText(decodedData)
                    // 保存校验信息
                    metaFile.writeText("${saveFile.lastModified()}|${saveFile.length()}")
                }

                decodedData
            }
        } catch (e: IOException) {
            writeLogToLocal.logError("Failed to load game data: ${e.message}", e)
            null
        }
    }

    // 保存存档
    fun saveGameData(saveData: String, fileName: String) {
        // 判断是否包含需要跳过的文件名
        val isContainSkipFileName = skipFileNames.contains(fileName)

        if (!isContainSkipFileName) {
            // 清除旧缓存
            clearCache(fileName)
            val cacheFile = File(cacheDir, "$fileName.cache")
            // 写入缓存
            cacheFile.writeText(saveData)
            Log.d(TAG, "写入缓存: $fileName.cache")
        }

        val saveFile = File(saveDir, fileName)

        // 使用 LZString 加密压缩
        val compressed = LZString.compressToBase64(saveData)

        // 将存档数据写入文件
        try {
            FileOutputStream(saveFile).use { fos ->
                fos.write(compressed.toByteArray())
            }
        } catch (e: IOException) {
            writeLogToLocal.logError("Failed to save game data：${e.message}", e)
        }

        if (!isContainSkipFileName) {
            val metaFile = File(cacheDir, "$fileName.meta")
            // 保存校验信息
            metaFile.writeText("${saveFile.lastModified()}|${saveFile.length()}")
            Log.d(TAG, "保存校验: $fileName.meta")
        }
    }

    /**
     * 清除某个存档的缓存
     */
    fun clearCache(fileName: String) {
        val cacheFile = File(cacheDir, "$fileName.cache")
        val metaFile = File(cacheDir, "$fileName.meta")
        if (cacheFile.exists() && metaFile.exists()) {
            Log.d(TAG, "清除缓存: $fileName")
            cacheFile.delete()
            metaFile.delete()
        }
    }
}