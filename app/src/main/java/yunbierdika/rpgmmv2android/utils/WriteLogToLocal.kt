package yunbierdika.rpgmmv2android.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DateFormat
import java.util.Date
import java.util.Locale

object WriteLogToLocal {
    private lateinit var logFile: File
    private const val TAG = "WebView"

    // 初始化路径
    fun init(context: Context) {
        val logDir = context.applicationContext.getExternalFilesDir(null)
            ?: throw IllegalStateException("无法访问外部存储目录")

        logFile = File(logDir, "log.txt")
    }

    // 输出无跟踪错误日志
    fun logError(message: String) { logError(message, null) }

    // 输出有跟踪错误日志
    fun logError(message: String, e: Throwable?) {
        Log.e(TAG, message, e)
        writeToLogFile("error", message, e)
    }

    // 输出调试日志
    fun logDebug(message: String) {
        Log.d(TAG, message)
        writeToLogFile("debug", message, null)
    }

    // 将错误日志保存到指定目录
    fun writeToLogFile(type: String, message: String, e: Throwable?) {
        try {
            FileOutputStream(logFile, true).bufferedWriter(Charsets.UTF_8).use { bw ->
                // 构建日志条目
                val timestamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                    DateFormat.MEDIUM, Locale.getDefault()).format(Date())

                val logEntry = when (type) {
                    "debug" -> "[$timestamp] DEBUG: $message"
                    else -> "[$timestamp] ERROR: $message"
                }

                bw.write(logEntry)
                bw.newLine()

                e?.let {
                    bw.write(Log.getStackTraceString(e))
                    bw.newLine()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "写入日志文件失败", e)
        }
    }
}