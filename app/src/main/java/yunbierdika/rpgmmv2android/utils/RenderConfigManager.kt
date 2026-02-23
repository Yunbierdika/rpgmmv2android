package yunbierdika.rpgmmv2android.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException

object RenderConfigManager {
    private lateinit var configFile: File
    private const val TAG = "RenderConfigManager"
    private const val DEFAULT_RPG_RENDER_MOD = "webgl"
    private const val DEFAULT_WEBVIEW_LAYER = "hardware"

    fun init(context: Context) {
        val configDirPath = context.applicationContext.getExternalFilesDir(null)
            ?: throw IllegalStateException("无法访问外部存储目录")

        configFile = File(configDirPath, "config.txt")
        if (!configFile.exists()) {
            try {
                FileWriter(configFile).use { fileWriter ->
                    fileWriter.write("rpg_render_mode=$DEFAULT_RPG_RENDER_MOD\n")
                    fileWriter.write("webview_layer=$DEFAULT_WEBVIEW_LAYER\n")
                    fileWriter.write("\n")
                    fileWriter.write("// rpg_render_mode为RPGMMV的渲染模式，可选择'webgl'和" +
                            "'canvas'、'auto'，默认的'webgl'更流畅，部分设备出现图像错误时请改为'canvas'\n")
                    fileWriter.write("// webview_layer为运行游戏的WeView加速策略，可选择'hardware'和" +
                            "'software'、'auto'，默认的'hardware'更流畅，部分设备出现图像错误时请改为'auto'" +
                            "或'software'")
                }
            } catch (e: IOException) {
                Log.e(TAG, "配置文件创建失败", e)
            }
        }
    }

    fun readConfig(): HashMap<String, String>? {
        if (!configFile.exists()) return null
        val map = HashMap<String, String>()
        try {
            FileReader(configFile).use { fr ->
                for (line in fr.readLines()) {
                    if (line.contains("=")) {
                        val parts = line.split("=")
                        if (parts.size == 2) {
                            map[parts[0].trim()] = parts[1].trim()
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "读取配置文件失败", e)
        }
        return map
    }
}