package yunbierdika.rpgmmv2android

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.webkit.WebViewAssetLoader
import yunbierdika.rpgmmv2android.utils.JavaScriptInterface
import yunbierdika.rpgmmv2android.utils.WriteLogToLocal

class MainActivity : ComponentActivity() {

    // WebView实例
    private lateinit var gameWebView: WebView

    // 日志输出实例
    private lateinit var writeLogToLocal: WriteLogToLocal

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 动态设置屏幕方向为横屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 初始化日志输出实例
        writeLogToLocal = WriteLogToLocal(this)

        // 初始化 WebView
        gameWebView = setupGameWebView()
        setContentView(gameWebView)

        // 仅在savedInstanceState为空时加载初始URL
        if (savedInstanceState == null) {
            // 加载游戏
            gameWebView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        } else {
            gameWebView.restoreState(savedInstanceState)
        }

        // 创建 OnBackPressedCallback，拦截返回键误操作
        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 弹出确认框
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.back_pressed_title)
                    .setMessage(R.string.back_pressed_message)
                    .setPositiveButton(R.string.back_pressed_positive) { dialog: DialogInterface?, which: Int -> finish() }
                    .setNegativeButton(R.string.back_pressed_negative, null)
                    .show()
            }
        }

        // 将 callback 添加到 OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, callback)
    }

    // 启动游戏方法
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupGameWebView(): WebView {
        val webView = WebView(this)

        // 配置WebView设置
        webView.settings.apply {
            // 启用WebView中的JavaScript支持
            javaScriptEnabled = true
            // 启用DOM存储支持
            domStorageEnabled = true
            // 允许媒体内容在没有用户手势的情况下自动播放
            mediaPlaybackRequiresUserGesture = false

            // 允许WebView适应屏幕宽度（启用视口支持）
            useWideViewPort = true
            loadWithOverviewMode = true

            // 允许WebView访问本地文件
            allowFileAccess = true

            // 设置缓存模式为本地缓存模式
            cacheMode = WebSettings.LOAD_CACHE_ONLY

            // 图片自动加载
            loadsImagesAutomatically = true

            // 多窗口支持
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true

            // 禁止系统自动缩放字体，避免文字抽搐、变大
            textZoom = 100

            // 优化缩放体验
            builtInZoomControls = false
            displayZoomControls = false

            // 防止某些设备缩放导致页面抖动
            setSupportZoom(false)
        }

        // 背景色
        webView.setBackgroundColor(Color.BLACK)

        // 加速策略改为默认
        webView.setLayerType(View.LAYER_TYPE_NONE, null)

        // 添加JavaScript接口
        webView.addJavascriptInterface(
            JavaScriptInterface(this, writeLogToLocal),
            "AndroidBridge"
        )

        // 使用 WebViewAssetLoader 代替已弃用的 allowUniversalAccessFromFileURLs 方法
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                return assetLoader.shouldInterceptRequest(request.url)
            }

            // 捕获 WebView 错误
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                writeLogToLocal.logError("WebView Error: ${error?.description} URL: ${request?.url}")
            }

            // 捕获 HTTP 错误
            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)
                writeLogToLocal.logError("HTTP Error: ${errorResponse?.statusCode} URL: ${request?.url}")
            }
        }

        // 捕获控制台日志
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    val cleanSource = it.sourceId()?.removePrefix("https://appassets.androidplatform.net/") ?: "unknown"
                    val message = it.message() ?: ""
                    val lineNumber = it.lineNumber()

                    writeLogToLocal.logDebug("[JS:${cleanSource}:${lineNumber}] $message")
                }
                return true
            }
        }

        return webView
    }

    // 隐藏系统 UI
    private fun hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11+ 的新方法
            window.insetsController?.let {
                it.hide(WindowInsets.Type.systemBars())
                it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            // Android 10 及以下的旧方法
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                    )
        }
    }

    // 视图重新聚焦时执行全屏模式
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    // 配置变化时重新隐藏系统 UI
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        hideSystemUI()
    }

    override fun onPause() {
        super.onPause()
        gameWebView.onPause()   // 暂停 WebView
        gameWebView.pauseTimers()   // 暂停 WebView 中的定时器
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        gameWebView.onResume() // 恢复 WebView
        gameWebView.resumeTimers() // 恢复 WebView 中的定时器
    }

    override fun onDestroy() {
        // 销毁 WebView
        gameWebView.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
        super.onDestroy()
    }

    // 保存 WebView 状态，应对切换应用时刷新问题
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 保存状态
        gameWebView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // 恢复状态
        gameWebView.restoreState(savedInstanceState)
    }
}