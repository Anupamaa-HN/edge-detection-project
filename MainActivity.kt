package com.example.edgeviewer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.util.Size
import android.view.Surface
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.hardware.camera2.*
import android.os.Handler
import android.os.HandlerThread
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

class MainActivity : AppCompatActivity() {

    companion object {
        init { System.loadLibrary("native-lib") }
    }

    external fun nativeInit()
    external fun nativeProcessFrame(nv21: ByteArray, width: Int, height: Int): ByteArray
    external fun nativeRelease()

    private val CAMERA_PERMISSION = 0x100

    private lateinit var cameraManager: CameraManager
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var imageReader: ImageReader
    private lateinit var handler: Handler
    private lateinit var glView: MyGLSurfaceView

    private val size = Size(640, 480)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        glView = findViewById(R.id.glView)
        val btnExport: Button = findViewById(R.id.btnExport)

        val thread = HandlerThread("CameraBg")
        thread.start()
        handler = Handler(thread.looper)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION)
        } else {
            startCamera()
        }

        nativeInit()

        btnExport.setOnClickListener { exportLastFrame() }
    }

    private fun startCamera() {
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        val camId = cameraManager.cameraIdList.first { id ->
            val map = cameraManager.getCameraCharacteristics(id)
            map.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }

        imageReader = ImageReader.newInstance(size.width, size.height, android.graphics.ImageFormat.YUV_420_888, 2)
        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val nv21 = yuv420ToNV21(image)
            image.close()
            val out = nativeProcessFrame(nv21, size.width, size.height)
            val bmp = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888)
            bmp.copyPixelsFromBuffer(ByteBuffer.wrap(out))
            glView.renderer?.updateFrame(bmp)
            lastBitmap = bmp
            glView.requestRender()
        }, handler)

        cameraManager.openCamera(camId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                val previewSurface = glView.getSurface()
                val targets = listOf(previewSurface, imageReader.surface)
                camera.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        captureSession = session
                        val req = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        req.addTarget(previewSurface)
                        req.addTarget(imageReader.surface)
                        req.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                        session.setRepeatingRequest(req.build(), null, handler)
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, handler)
            }
            override fun onDisconnected(camera: CameraDevice) { camera.close() }
            override fun onError(camera: CameraDevice, error: Int) { camera.close() }
        }, handler)
    }

    private fun yuv420ToNV21(image: Image): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 2
        val nv21 = ByteArray(ySize + uvSize)
        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer
        val rowStrideY = image.planes[0].rowStride
        if (rowStrideY == width) {
            yBuffer.get(nv21, 0, ySize)
        } else {
            val row = ByteArray(rowStrideY)
            var p = 0
            for (i in 0 until height) {
                yBuffer.get(row, 0, rowStrideY)
                System.arraycopy(row, 0, nv21, p, width)
                p += width
            }
        }
        val uRowStride = image.planes[1].rowStride
        val uPixelStride = image.planes[1].pixelStride
        val vRowStride = image.planes[2].rowStride
        val vPixelStride = image.planes[2].pixelStride
        val uBytes = ByteArray(uRowStride * (height / 2))
        val vBytes = ByteArray(vRowStride * (height / 2))
        uBuffer.get(uBytes)
        vBuffer.get(vBytes)
        var uvPos = ySize
        for (row in 0 until height / 2) {
            var col = 0
            while (col < width) {
                val vIndex = row * vRowStride + col * vPixelStride
                val uIndex = row * uRowStride + col * uPixelStride
                nv21[uvPos++] = vBytes[vIndex]
                nv21[uvPos++] = uBytes[uIndex]
                col += 2
            }
        }
        return nv21
    }

    private var lastBitmap: Bitmap? = null

    private fun exportLastFrame() {
        lastBitmap?.let { bmp ->
            val file = File(getExternalFilesDir(null), "processed_frame.png")
            FileOutputStream(file).use { out -> bmp.compress(Bitmap.CompressFormat.PNG, 100, out) }
        }
    }

    override fun onDestroy() {
        nativeRelease()
        cameraDevice?.close()
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == CAMERA_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }
}
