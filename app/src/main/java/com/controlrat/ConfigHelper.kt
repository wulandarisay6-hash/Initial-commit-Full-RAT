package com.controlrat
import android.content.Context
object ConfigHelper { fun getAppId(context: Context): String = context.getSharedPreferences("rat_pref", Context.MODE_PRIVATE).getString("app_id", "unknown") ?: "unknown" }
