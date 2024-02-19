package com.example.facemaker

import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Choreographer
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.facemaker.databinding.ActivityGameBinding
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.random.Random
import com.example.facemaker.ml.FacialExpressionRecognitionModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

//lateinit var bitmap: Bitmap
class Game : AppCompatActivity() {
    private val emotionList = arrayOf("Neutral", "Happy", "Sad", "Surprise", "Fear", "Disgust", "Anger", "Contempt")

    private val faces = mapOf(
        "happy" to 0x1F60A,
        "sad" to 0x1F614,
        "angry" to 0x1F620
    )
    private var currScore : Int = 0
    private var currFace :String= "happy"
    private var timeLeft :Long = 60000000000

    // Camera stuff
    private val mainBinding: ActivityGameBinding by lazy{
        ActivityGameBinding.inflate(layoutInflater)
    }
    private val multiplePermissionId = 14
    private val multiplePermissionNameList = if (Build.VERSION.SDK_INT >= 33) {
        arrayListOf(
            android.Manifest.permission.CAMERA
        )
    } else {
        arrayListOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private lateinit var imageCapture: ImageCapture
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera
    private lateinit var  cameraSelector: CameraSelector
    private var lensFacing = CameraSelector.LENS_FACING_FRONT

    private var isCapturing = false
    private val captureInterval = 100L
    private var isCameraReady = false

    private lateinit var imageProcessor : ImageProcessor
    private lateinit var model : FacialExpressionRecognitionModel

    //bitmap
    //
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(mainBinding.root)
        InitialiseML()

        //check for camera permission
        if (checkMultiplePermission()) {
            startCamera()
        }

       // Log.d("DEBUGCAPTURE", "After function startCamera()")
        currScore = 0
        timeLeft = 60000000000
        currFace = faces.keys.toList()[Random.nextInt(faces.size)]
        updateFace()
        setScore()

        // only captures when button pressed (to change)
//        mainBinding.captureIB.setOnClickListener{
//           Log.d("DEBUG", "About to take photo")
//            takePhoto()
//
//        }

        val timer = object : CountDownTimer(61000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = millisUntilFinished / 1000
                findViewById<TextView>(R.id.timer).text = "Time Left: " + secondsLeft
            }

            override fun onFinish() {
                val intent = Intent(this@Game, GameOver::class.java)
                intent.putExtra("score", currScore)
                startActivity(intent)
            }
        }

        timer.start()
    }

    private fun startCaptureTimer() {
        //Log.d("DEBUGCAPTURE", "startCaptureTimer")
        if (!isCapturing) {
            isCapturing = true
            //Log.d("DEBUGCAPTURE", "before setting handler")
            val handler = Handler(Looper.getMainLooper())
            //Log.d("DEBUGCAPTURE","before setting runnable")
            val runnable = object : java.lang.Runnable {
                override fun run() {
                    //Log.d("DEBUGCAPTURE", "before if statement")
                    if (isCapturing && isCameraReady) {
                        //Log.d("DEBUGCAPTURE", "before takePhoto()")
                        takePhoto()
                        handler.postDelayed(this, captureInterval)
                    }
                }
            }
            handler.post(runnable)
        }
    }

    private fun stopCaptureTimer() {
        isCapturing = false
    }

    override fun onDestroy() {
        super.onDestroy()
        stopCaptureTimer()
    }

    private fun checkMultiplePermission(): Boolean {
       // Log.d("DEBUGCAPTURE", "checkMultiplePermission")
        val listPermissionNeeded = arrayListOf<String>()
        for (permission in multiplePermissionNameList) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                listPermissionNeeded.add(permission)
            }
        }
        if (listPermissionNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                listPermissionNeeded.toTypedArray(),
                multiplePermissionId
            )
            return false
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        //Log.d("DEBUGCAPTURE", "onRequestPermissionsResult")
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == multiplePermissionId) {
            if (grantResults.isNotEmpty()) {
                var isGrant = true
                for (element in grantResults) {
                    if (element == PackageManager.PERMISSION_DENIED) {
                        isGrant = false
                    }
                }
                if (isGrant) {
                    // here all permission granted successfully
                    startCamera()
                } else {
                    var someDenied = false
                    for (permission in permissions) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                permission
                            )
                        ) {
                            if (ActivityCompat.checkSelfPermission(
                                    this,
                                    permission
                                ) == PackageManager.PERMISSION_DENIED
                            ) {
                                someDenied = true
                            }
                        }
                    }
                    if (someDenied) {
                        // here app Setting open because all permission is not granted
                        // and permanent denied
                        appSettingOpen(this)
                    } else {
                        // here warning permission show
                        warningPermissionDialog(this) { _: DialogInterface, which: Int ->
                            when (which) {
                                DialogInterface.BUTTON_POSITIVE ->
                                    checkMultiplePermission()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun takePhoto() {
       // Log.d("CAMERA", "takePhoto")

//        val imageFolder = File(
//            Environment.getExternalStoragePublicDirectory(
//                Environment.DIRECTORY_PICTURES), "Images"
//        )
//        if (!imageFolder.exists()){
//            imageFolder.mkdir()
//        }
//
//        var fileName = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis()) + ".jpeg"
//        var imageFile = File(imageFolder, fileName)
//        var count = 1
//
//        while (imageFile.exists()){
//            fileName = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.getDefault()).format(System.currentTimeMillis()) + "_$count.jpeg"
//            imageFile = File(imageFolder, fileName)
//            count++
//        }
//        val outputOption = ImageCapture.OutputFileOptions.Builder(imageFile).build()

        //Log.d("CAMERA", "reached here before image capture")

        imageCapture.takePicture(
//        outputOption,
//        ContextCompat.getMainExecutor(this),
//        object : ImageCapture.OnImageSavedCallback {
//            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
//                val message = "Photo Capture Success!"
//                Toast.makeText(
//                    this@Game,
//                    message,
//                    Toast.LENGTH_LONG
//                ).show()
//            }
//
//            override fun onError(exception: ImageCaptureException) {
//                Toast.makeText(
//                    this@Game,
//                    exception.message.toString(),
//                    Toast.LENGTH_LONG
//                ).show()
//                //Log.d("ERROR", exception.message.toString());
//            }
//        }

                // in future will change to onCaptureSuccess after testing
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        Log.d("CAMERA", "onCaptureSuccess")
                        val bitmap = imageProxyToBitmap(image)
                        image.close()
                        super.onCaptureSuccess(image)

                        // Run facial expression model
                        var intResult = runMLModel(bitmap)
                        var result = convertToEmotion(intResult)
                        Log.d("logging", result)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Log.d("CAMERA", "onCaptureError")
                        super.onError(exception)
                    }
                }
        )
        //Log.d("CAMERA", "onCaptureSuccess Finish calling")
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun startCamera(){
        //Log.d("DEBUGCAPTURE", "startCamera")
        val camreProviderFuture = ProcessCameraProvider.getInstance(this)
        camreProviderFuture.addListener({
            cameraProvider = camreProviderFuture.get()
            bindCameraUserCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUserCases() {
        //Log.d("DEBUGCAPTURE", "bindCameraUserCases")
        // set resolution to 244 by 244
        val targetResolution = Size(244,244)
        //val screenAspectRatio = aspectRatio(mainBinding.previewView.width, mainBinding.previewView.height)
        val rotation = mainBinding.previewView.display.rotation
        //val resolutionSelector = ResolutionSelector.Builder().addTargetResolution(resolution).build()

        val preview = Preview.Builder().setTargetResolution(targetResolution)
            .setTargetRotation(rotation).build().also{
                it.setSurfaceProvider(mainBinding.previewView.surfaceProvider)
            }

        imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY).setTargetResolution(targetResolution)
            .setTargetRotation(rotation).build()
        cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        //Log.d("DEBUGCAPTURE","before isCameraReady set to true")
        isCameraReady = true

        try{
            //Log.d("DEBUGCAPTURE","came into try")
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(this as LifecycleOwner, cameraSelector, preview, imageCapture)
        } catch (e:Exception){
            //Log.d("DEBUGCAPTURE", "came")
            e.printStackTrace()
        }

        startCaptureTimer()
    }

    fun getCurrentFace() : String {
        return currFace
    }

    fun incrementScore() {
        ++currScore
        setScore()
    }

    private fun setScore() {
        findViewById<TextView>(R.id.score).text = "Score:\n" + currScore
    }

    fun nextFace() {
        var randomFace : String = currFace
        while (randomFace == currFace) {
            randomFace = faces.keys.toList()[Random.nextInt(faces.size)]
        }
        currFace = randomFace
        updateFace()
    }

    private fun updateFace() {
        var faceInt : Int = faces[currFace]?:0x1F620
        findViewById<TextView>(R.id.faceToMake).setText(String(Character.toChars(faceInt)))
    }

    fun InitialiseML() {
        // Initialise ML stuff
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224,224, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0.0f, 255.0f))
            .build()
        model = FacialExpressionRecognitionModel.newInstance(this)
    }

    fun runMLModel(_bitmap : Bitmap): Int {

        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(_bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // Creates inputs for reference.
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)

        inputFeature0.loadBuffer(tensorImage.buffer)

        // Runs model inference and gets result.
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        val result = GetEmotionIdx(outputFeature0.floatArray)
        return result
    }

    fun convertToEmotion(i : Int) : String {
        return emotionList[i];
    }

    fun GetEmotionIdx(arr: FloatArray): Int {
        var max = 1
        for (i in 1 until arr.size) { // This will always skip neutral expression
//            Log.d("logging", arr[i].toString())
            if (arr[i] > arr[max]) {
                max = i
            }
        }
        return max
    }
}