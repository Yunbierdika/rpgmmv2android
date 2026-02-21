package yunbierdika.rpgmmv2android.utils

import android.content.Context
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.util.Date
import java.util.Locale

object WriteLogToLocal {
    private lateinit var logDir: File
    private const val TAG: String = "WebView"

    // 初始化路径
    fun init(context: Context) {
        logDir = context.applicationContext.getExternalFilesDir(null)
            ?: throw IllegalStateException("无法访问外部存储目录")
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
        // 错误日志文件
        val logFile = File(logDir, "log.txt")

        try {
            // 追加写入模式
            FileOutputStream(logFile, true).use { fos ->
                OutputStreamWriter(fos, StandardCharsets.UTF_8).use { osw ->
                    BufferedWriter(osw).use { writer ->
                        // 构建日志条目
                        val timestamp = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
                            DateFormat.MEDIUM, Locale.getDefault()).format(Date())

                        val logEntry = when (type) {
                            "debug" -> "[$timestamp] DEBUG: $message"
                            else -> "[$timestamp] ERROR: $message"
                        }

                        writer.write(logEntry)
                        writer.newLine()

                        e?.let {
                            writer.write(Log.getStackTraceString(e))
                            writer.newLine()
                        }
                    }
                }
            }
        } catch (ex: IOException) {
            Log.e(TAG, "写入日志文件失败", ex)
        }
    }
}