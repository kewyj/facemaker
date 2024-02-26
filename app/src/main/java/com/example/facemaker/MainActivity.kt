package com.example.facemaker

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.facemaker.ml.FacialExpressionRecognitionModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
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

    private lateinit var db : FirebaseFirestore
    private lateinit var username : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setLogo()

        db = Firebase.firestore

        val user = Firebase.auth.currentUser
        if (user == null) {
            val intent = Intent(this@MainActivity, Auth::class.java)
            startActivity(intent)
        }
        user?.let {
            val email = it.email
            username = email.toString().substringBefore('@').replace('.', '_')
            findViewById<TextView>(R.id.username).text = "Welcome, " + username
        }

        GetHighscore {hs->
            findViewById<TextView>(R.id.highscore).text = "Highscore: " + hs.toString()
        }



        findViewById<Button>(R.id.logout).setOnClickListener {
            Firebase.auth.signOut()
            val intent = Intent(this@MainActivity, Auth::class.java)
            startActivity(intent)
        }
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

        // Initialise the image bitmap
        bitmap = BitmapFactory.decodeResource(this.resources, R.drawable._image1);

        InitialiseML()

        // Run facial expression model
        var intResult = runMLModel(bitmap)
        var result = convertToEmotion(intResult)
        Log.d("logging", result)


        // Releases model resources if no longer used.
        //model.close()

        /*findViewById<Button>(R.id.startGame).setOnClickListener {
            val intent = Intent(this@MainActivity, Camera::class.java)
            startActivity(intent)
        }*/
    }

    private fun setLogo() {
        findViewById<TextView>(R.id.logo).setText("FaceMaker\n" + getEmoji(0x1F60A) + getEmoji(0x1F614) + getEmoji(0x1F620))
    }
    private fun getEmoji(unicode : Int): String? {
        return String(Character.toChars(unicode))
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

    fun GetHighscore(callback: (Int) -> Unit) {

        val docRef = db.collection("user_details").document(username)
        docRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val highscore = document.get("highscore").toString().toInt()
                    callback(highscore)
                } else {
                    Log.d("GameOver", "No such document")
                    callback(-1) // Or handle accordingly if document doesn't exist
                }
            }
            .addOnFailureListener { exception ->
                Log.d("GameOver", "get failed with ", exception)
                callback(-1) // Or handle failure accordingly
            }
    }
}


