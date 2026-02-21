package yunbierdika.rpgmmv2android.utils

import android.app.Activity
import android.webkit.JavascriptInterface
import java.io.File

class JavaScriptInterface(private val activity: Activity) {
    init {
        // 初始化存档管理器实例
        SaveFileManager.init(activity)
    }

    // 获取存档目录
    private fun getSaveDir(): File {
        val saveDir = File(activity.getExternalFilesDir(null), "save")
        if (!saveDir.exists() && !saveDir.mkdirs()) {
            WriteLogToLocal.logError("Failed to create save directory: ${saveDir.absolutePath}")
        }
        return saveDir
    }

    // 使结束游戏按钮功能可用
    @JavascriptInterface
    fun closeGame() {
        // 调用 Activity 的 finish() 方法
        activity.runOnUiThread { activity.finish() }
    }

    // 将发送过来的存档保存到指定目录
    @JavascriptInterface
    fun saveGameData(saveData: String, fileName: String) {
        SaveFileManager.saveGameData(saveData, fileName)
    }

    // 加载存档，返回值为存档文件里的内容
    @JavascriptInterface
    fun loadGameData(fileName: String): String? {
        return SaveFileManager.loadGameData(fileName)
    }

    // 判断存档文件是否存在
    @JavascriptInterface
    fun existsGameSave(fileName: String): Boolean {
        val saveDir = getSaveDir()
        val saveFile = File(saveDir, fileName)
        return saveFile.exists()
    }

    // 删除CommonSave插件的专用存档（用于跨周目继承点数的存档）
    @JavascriptInterface
    fun removeCommonSave() {
        val targetDir = getSaveDir()
        val saveFile = File(targetDir, "common.rpgsave")

        if (saveFile.exists() && !saveFile.delete()) {
            WriteLogToLocal.logError("Failed to delete common save: ${saveFile.absolutePath}")
        }
    }
}