package com.example.facemaker

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModelProvider
import com.example.facemaker.ml.FacialExpressionRecognitionModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer

class MainActivity : ComponentActivity() {
    private val emotionList = arrayOf("Neutral", "Happy", "Sad", "Surprise", "Fear", "Disgust", "Anger", "Contempt")
    private var bitmap = createBitmap(224,224, Bitmap.Config.RGB_565)

    private lateinit var imageProcessor : ImageProcessor
    private lateinit var model : FacialExpressionRecognitionModel
    private lateinit var viewModel: FacemakerViewModel

    @OptIn(DelicateCoroutinesApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setLogo()

        findViewById<Button>(R.id.startGame).setOnClickListener {
            val intent = Intent(this@MainActivity, Game::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.howToPlay).setOnClickListener {
            val intent = Intent(this@MainActivity, HowToPlay::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.credits).setOnClickListener {
            val intent = Intent(this@MainActivity, Credits::class.java)
            startActivity(intent)
        }

        findViewById<Button>(R.id.testBtn).setOnClickListener {
            GlobalScope.launch (Dispatchers.IO) {
                viewModel.insert(FacemakerStruct(score = 100))
            }
        }

        val application = applicationContext as FacemakerApplication
        viewModel = ViewModelProvider(
            this,
            FacemakerViewModelFactory(application.repository)
        )[FacemakerViewModel::class.java]

        // Observe LiveData
        viewModel.getAllValuesLive().observe(this) { score ->
            Log.d("Test Score", score[0].score.toString())
        }

        // Initialise the image bitmap
        bitmap = BitmapFactory.decodeResource(this.resources, R.drawable._image1)

        initialiseML()

        // Run facial expression model
        val intResult = runMLModel(bitmap)
        val result = convertToEmotion(intResult)
        Log.d("logging", result)


        // Releases model resources if no longer used.
        //model.close()

        /*findViewById<Button>(R.id.startGame).setOnClickListener {
            val intent = Intent(this@MainActivity, Camera::class.java)
            startActivity(intent)
        }*/
    }

    private fun setLogo() {
        val logo = "FaceMaker\n" + getEmoji(0x1F60A) + getEmoji(0x1F614) + getEmoji(0x1F620)
        findViewById<TextView>(R.id.logo).text = logo
    }
    private fun getEmoji(unicode : Int): String {
        return String(Character.toChars(unicode))
    }


    private fun getEmotionIdx(arr: FloatArray): Int {
        var max = 1
        for (i in 1 until arr.size) { // This will always skip neutral expression
//            Log.d("logging", arr[i].toString())
            if (arr[i] > arr[max]) {
                max = i
            }
        }
        return max
    }

    private fun initialiseML() {
        // Initialise ML stuff
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224,224, ResizeOp.ResizeMethod.BILINEAR))
            .add(NormalizeOp(0.0f, 255.0f))
            .build()
        model = FacialExpressionRecognitionModel.newInstance(this)
    }

    private fun runMLModel(_bitmap : Bitmap): Int {

        var tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(_bitmap)
        tensorImage = imageProcessor.process(tensorImage)

        // Creates inputs for reference.
        val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)

        inputFeature0.loadBuffer(tensorImage.buffer)

        // Runs model inference and gets result.
        val outputs = model.process(inputFeature0)
        val outputFeature0 = outputs.outputFeature0AsTensorBuffer

        return getEmotionIdx(outputFeature0.floatArray)
    }

    private fun convertToEmotion(i : Int) : String {
        return emotionList[i]
    }
}


