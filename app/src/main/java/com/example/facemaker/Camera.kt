package com.example.facemaker

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.CameraAccessException
import android.media.ImageReader
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.HandlerThread
import android.view.TextureView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Handler
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraCaptureSession
import java.util.concurrent.Semaphore
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.IOException

class Camera : AppCompatActivity() {
    // Create camera manager instance
    // Using lazy initialization to postpone the creation of camera instance until necessary
    private val cameraManager by lazy {
        getSystemService(CAMERA_SERVICE) as android.hardware.camera2.CameraManager
    }
    // find front facing camera ID to locate the intended front camera for use
    private val cameraId by lazy {
        cameraManager.cameraIdList.firstOrNull { cameraManager.getCameraCharacteristics(it).get(android.hardware.camera2.CameraCharacteristics.LENS_FACING) == android.hardware.camera2.CameraCharacteristics.LENS_FACING_FRONT }
    }

    private lateinit var textureView: TextureView
    // for opened camera
    private var cameraDevice: android.hardware.camera2.CameraDevice? = null
    // represents configured camera session (setting up and managing capture process)
    private var cameraCaptureSession: android.hardware.camera2.CameraCaptureSession? = null
    // semaphore to control camera opening and closing (since camera operations are asynchronous,
    // make sure that multiple threads do not open and close the camera simultaneously
    private val cameraOpenCloseLock = Semaphore(1)
    // ImageReader for capturing image
    private lateinit var imageReader: ImageReader
    private lateinit var bgHandler: Handler
    // set the capture interval (50 ms = 0.05 sec)
    private val captureInterval = 50L

    private val cameraStateCallback = object : android.hardware.camera2.CameraDevice.StateCallback() {
        // call when camera open
        override fun onOpened(camera: android.hardware.camera2.CameraDevice) {
            cameraDevice = camera
            createCameraPreviewSession()
        }

        // call when camera disconnected
        override fun onDisconnected(camera: android.hardware.camera2.CameraDevice) {
            cameraOpenCloseLock.release()
            camera.close()
            cameraDevice = null
        }

        // call when error triggered
        override fun onError(camera: android.hardware.camera2.CameraDevice, error: Int) {
            onDisconnected(camera)
            finish()
        }
    }

    // used for camera capture sessions (*to be implemented*)
    private val captureCallback = object : android.hardware.camera2.CameraCaptureSession.CaptureCallback() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        textureView = findViewById<TextureView>(R.id.textureView)

        // (max_width, max_height, image_format, max_images)
        imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)
    }

    private fun startBGThread()
    {
        val handlerThread = HandlerThread("CameraBG")
        handlerThread.start()
        bgHandler = Handler(handlerThread.looper)
    }

    private fun scheduleCapture()
    {
        bgHandler.postDelayed(
            {
                imageCapture()
                scheduleCapture()
            }, captureInterval)
    }

    private fun imageCapture()
    {
        // Store cameraDevice in a local variable to avoid threading issues
        val currentCameraDevice = cameraDevice
        // Ensure that cameraDevice is not null
        if (currentCameraDevice == null) return
        try {
            val captureBuilder = currentCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(imageReader.surface)

            val file = createImageFile()
            val imageReaderListener = ImageReader.OnImageAvailableListener { reader ->
                val image = reader.acquireLatestImage()
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                imageSave(bytes, file)
                image.close()
            }
            imageReader.setOnImageAvailableListener(imageReaderListener, bgHandler)
            val captureListener = object : CameraCaptureSession.CaptureCallback() {}
            currentCameraDevice.createCaptureSession(
                listOf(imageReader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        try {
                            session.capture(captureBuilder.build(), captureListener, bgHandler)
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, bgHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun createImageFile(): File
    {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "JPEG_$timeStamp"
        val directory = getExternalFilesDir(null)
        return File.createTempFile(fileName, ".jpg", directory)
    }

    private fun imageSave(bytes: ByteArray, file: File)
    {
        try {
            val outputStream = FileOutputStream(file)
            outputStream.write(bytes)
            outputStream.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // checks for CAMERA access permission, will request permission from user if found not allowed yet
    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    // when activity pauses, closeCamera() called to release resources (semaphore comes into play)
    override fun onPause() {
        closeCamera()
        super.onPause()
    }

    // opens camera using camera manager, checks if cameraID is not null (means suitable camera
    // is found) and if camera permission was enabled
    private fun openCamera() {
        if (cameraId == null) {
            // No front-facing camera found
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraManager.openCamera(cameraId!!, cameraStateCallback, null)
        }
    }

    // releases camera resource
    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            cameraDevice?.close()
            cameraDevice = null
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
    }

    // creates camera preview session. configures textureview surface as the target for camera preview,
    // sets autofocus mode and starts repeating capture req for continuous preview
    private fun createCameraPreviewSession() {
        try {
            val texture = textureView.surfaceTexture
            texture?.setDefaultBufferSize(textureView.width, textureView.height)

            val surface = android.view.Surface(texture)

            val captureRequestBuilder =
                cameraDevice?.createCaptureRequest(android.hardware.camera2.CameraDevice.TEMPLATE_PREVIEW)
            captureRequestBuilder?.addTarget(surface)

            cameraDevice?.createCaptureSession(
                listOf(surface),
                object : android.hardware.camera2.CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: android.hardware.camera2.CameraCaptureSession) {
                        if (cameraDevice == null) return

                        cameraCaptureSession = session
                        try {
                            captureRequestBuilder?.set(
                                android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE,
                                android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            session.setRepeatingRequest(
                                captureRequestBuilder?.build()!!,
                                captureCallback,
                                null
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    override fun onConfigureFailed(session: android.hardware.camera2.CameraCaptureSession) {}
                },
                null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // handle user's response to camera permission req
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    companion object {
        private const val CAMERA_PERMISSION_REQUEST_CODE = 100
    }
}