package yunbierdika.rpgmmv2android.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object SaveFileManager {
    private lateinit var saveDir: File
    private lateinit var cacheDir: File
    private const val TAG = "SaveFileManager"

    // 需要跳过缓存的文件名
    private val skipFileNames = listOf(
        "global.rpgsave",
        "config.rpgsave",
        "common.rpgsave",
    )

    // 初始化路径
    fun init(context: Context) {
        val saveDirPath = context.applicationContext.getExternalFilesDir(null)
            ?: throw IllegalStateException("无法访问外部存储目录")
        val cacheDirPath = context.applicationContext.externalCacheDir
            ?: throw IllegalStateException("无法访问外部缓存目录")

        saveDir = File(saveDirPath, "save")
        if (!saveDir.exists()) saveDir.mkdirs()
        cacheDir = File(cacheDirPath, "save")
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
            FileInputStream(saveFile).bufferedReader(Charsets.UTF_8).use { br ->
                val base64Data = br.readText().trim()

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
            Log.e(TAG, "Failed to load game data: ${e.message}", e)
            WriteLogToLocal.logError("Failed to load game data：${e.message}", e)
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
            Log.e(TAG, "Failed to save game data: ${e.message}", e)
            WriteLogToLocal.logError("Failed to save game data：${e.message}", e)
        }

        if (!isContainSkipFileName) {
            val metaFile = File(cacheDir, "$fileName.meta")
            // 保存校验信息
            metaFile.writeText("${saveFile.lastModified()}|${saveFile.length()}")
            Log.d(TAG, "保存校验: $fileName.meta")
        }
    }

    // 清除某个存档的缓存
    fun clearCache(fileName: String) {
        val cacheFile = File(cacheDir, "$fileName.cache")
        val metaFile = File(cacheDir, "$fileName.meta")
        if (cacheFile.exists() && metaFile.exists()) {
            Log.d(TAG, "清除缓存: $fileName")
            cacheFile.delete()
            metaFile.delete()
        }
    }

    fun existsGameSave(fileName: String): Boolean {
        return File(saveDir, fileName).exists()
    }

    fun removeGameSave(fileName: String): Boolean {
        val saveFile = File(saveDir, fileName)
        if (saveFile.exists())
            return saveFile.delete()
        return false
    }
}