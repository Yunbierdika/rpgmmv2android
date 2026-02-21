package yunbierdika.rpgmmv2android.utils

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileWriter

object RenderConfigManager {
    private lateinit var configDir: File
    private lateinit var configFile: File
    private const val TAG: String = "RenderConfigManager"
    private const val DEFAULT_RPG_RENDER_MOD = "webgl"
    private const val DEFAULT_WEBVIEW_LAYER = "hardware"

    fun init(context: Context) {
        configDir = context.applicationContext.getExternalFilesDir(null)
            ?: throw IllegalStateException("无法访问外部存储目录")

        configFile = File(configDir, "config.txt")
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
                            "或'software'\n")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun readConfig(): HashMap<String, String>? {
        if (!configFile.exists()) return null
        val map = HashMap<String, String>()
        try {
            FileReader(configFile).use { fr ->
                BufferedReader(fr).use { br ->
                    var line: String?
                    while ((br.readLine().also { line = it }) != null) {
                        if (line!!.contains("=")) {
                            val parts = line.split("=")
                            if (parts.size == 2) {
                                map[parts[0].trim()] = parts[1].trim()
                            }
                        }
                    }
                }
            }
        } catch (ex: Exception) {
            Log.e(TAG, "读取配置文件失败", ex)
        }
        return map
    }
}