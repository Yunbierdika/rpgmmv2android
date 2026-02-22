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
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.webkit.WebViewAssetLoader
import yunbierdika.rpgmmv2android.utils.JavaScriptInterface
import yunbierdika.rpgmmv2android.utils.RenderConfigManager
import yunbierdika.rpgmmv2android.utils.WriteLogToLocal

class MainActivity : ComponentActivity() {
    // RPGMMV渲染模式
    private lateinit var rpgRenderMode: String
    // WebView渲染模式
    private lateinit var webviewLayer: String
    // WebView实例
    private lateinit var gameWebView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 动态设置屏幕方向为横屏
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 初始化日志输出实例
        WriteLogToLocal.init(this)

        // 初始化配置文件
        RenderConfigManager.init(this)

        // 读取config.txt得到渲染模式配置
        val map = RenderConfigManager.readConfig()
        rpgRenderMode = map?.get("rpg_render_mode").toString().lowercase()
        webviewLayer = map?.get("webview_layer").toString().lowercase()

        // 初始化 WebView
        gameWebView = setupGameWebView()
        setContentView(gameWebView)

        // 仅在savedInstanceState为空时加载初始URL
        if (savedInstanceState == null) {
            // 加载游戏
            val url = "https://appassets.androidplatform.net/assets/index.html"
            when(rpgRenderMode) {
                "webgl" -> gameWebView.loadUrl("$url?webgl")
                "canvas" -> gameWebView.loadUrl("$url?canvas")
                else -> gameWebView.loadUrl(url)
            }
        }

        // 拦截返回键误操作
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 弹出确认框
                AlertDialog.Builder(this@MainActivity)
                    .setTitle(R.string.back_pressed_title)
                    .setMessage(R.string.back_pressed_message)
                    .setPositiveButton(R.string.back_pressed_positive) { _: DialogInterface?, _: Int -> finish() }
                    .setNegativeButton(R.string.back_pressed_negative, null)
                    .show()
            }
        })
    }

    // 启动游戏方法
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupGameWebView(): WebView {
        val webView = WebView(this)

        // 配置WebView设置
        webView.settings.apply {
            // 核心功能：启用 JavaScript、DOM 存储、自动播放
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false

            // 移动端适配：启用视口支持，使页面自适应屏幕
            useWideViewPort = true
            loadWithOverviewMode = true

            // 限制不必要的文件访问，提高安全性
            allowFileAccess = false          // 禁止 file:// 协议访问
            allowContentAccess = false       // 禁止 content:// 协议访问

            // 禁用缩放，避免页面抖动（部分设备缩放后显示异常）
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false

            // 固定字体缩放比例，防止系统字体大小影响布局
            textZoom = 100

            // 多窗口支持（可选配置，若页面无需打开新窗口可移除）
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
        }

        // 背景色
        webView.setBackgroundColor(Color.BLACK)

        // 加速策略
        when(webviewLayer) {
            "hardware" -> webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            "software" -> webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
            else -> webView.setLayerType(View.LAYER_TYPE_NONE, null)
        }

        // 添加JavaScript接口
        webView.addJavascriptInterface(JavaScriptInterface(this), "AndroidBridge")

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
                WriteLogToLocal.logError("WebView Error: ${error?.description} URL: ${request?.url}")
            }

            // 捕获 HTTP 错误
            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)
                WriteLogToLocal.logError("HTTP Error: ${errorResponse?.statusCode} URL: ${request?.url}")
            }
        }

        // 捕获控制台日志
        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    val cleanSource = it.sourceId()?.removePrefix("https://appassets.androidplatform.net/") ?: "unknown"
                    val message = it.message() ?: ""
                    val lineNumber = it.lineNumber()

                    WriteLogToLocal.logDebug("[JS:${cleanSource}:${lineNumber}] $message")
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
        if (hasFocus) hideSystemUI()
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
        gameWebView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        gameWebView.restoreState(savedInstanceState)
    }
}