package com.example.photo_vocalizer

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.photo_vocalizer.ml.FruitModel
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder


class MainActivity : AppCompatActivity() {
    private lateinit var photoButton : Button
    private lateinit var galleryButton : Button
    private lateinit var listenButton : Button
    private lateinit var imageView : ImageView
    private lateinit var resultText : TextView
    private lateinit var image: Bitmap
    private lateinit var speechRecognizer : SpeechRecognizer
    private lateinit var speechRecognizerIntent : Intent
    private var isRecognizerSet : Boolean = false
    private var isImageSet : Boolean = false
    private val imageSize = 32
    private val cameraRequestCode = 1
    private val galleryRequestCode = 3
    private val cameraRequestPermissionCode = 101
    private val audioRequestPermissionCode = 102
    private val languageCode = "pl-PL"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        assignReferences()
        setUpSpeechRecognition()
    }

    private fun setUpSpeechRecognition(){
        if(checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){
            if (!isRecognizerSet)
                createSpeechRecognizer()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), audioRequestPermissionCode)
        }
    }

    private fun createSpeechRecognizer(){
        val localContext = this
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(bundle: Bundle) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(bytes: ByteArray) {}
            override fun onEndOfSpeech() {}
            override fun onError(i: Int) {}
            override fun onPartialResults(bundle: Bundle) {}
            override fun onEvent(i: Int, bundle: Bundle) {}
            override fun onResults(bundle: Bundle) {
                val data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                Toast.makeText(localContext,data!![0], Toast.LENGTH_LONG).show()
            }
        })
        isRecognizerSet = true
        setOnTouchListener()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setOnTouchListener(){
        listenButton.setOnTouchListener { v, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> {
                    speechRecognizer.startListening(speechRecognizerIntent)
                }
                MotionEvent.ACTION_UP -> {
                    speechRecognizer.stopListening()
                }
            }
            v?.onTouchEvent(event) ?: true
        }
    }

    fun takePhoto(view: View){
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(cameraIntent, cameraRequestCode)
        } else {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), cameraRequestPermissionCode)
        }
    }

    fun pickFromGallery(view: View){
        val cameraIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(cameraIntent, galleryRequestCode)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == cameraRequestCode) {
                image = (data?.extras!!["data"] as Bitmap?)!!
                val dimension = image.width.coerceAtMost(image.height)
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension)
                imageView.setImageBitmap(image)
                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false)
                isImageSet = true
            }

            if (requestCode == galleryRequestCode) {
                val dat: Uri? = data?.data
                try {
                    image = MediaStore.Images.Media.getBitmap(this.contentResolver, dat)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                imageView.setImageBitmap(image)
                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false)
                isImageSet = true
            }
        }
    }

     fun classifyImage(view: View) {
         if(isImageSet){
             //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
             try {
                 val model = FruitModel.newInstance(applicationContext)

                 // Creates inputs for reference.
                 val inputFeature0 =
                     TensorBuffer.createFixedSize(intArrayOf(1, 32, 32, 3), DataType.FLOAT32)
                 val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3)
                 byteBuffer.order(ByteOrder.nativeOrder())
                 val intValues = IntArray(imageSize * imageSize)
                 image.getPixels(intValues, 0, image.width, 0, 0, image.width, image.height)
                 var pixel = 0
                 for (i in 0 until imageSize) {
                     for (j in 0 until imageSize) {
                         val `val` = intValues[pixel++] // RGB
                         byteBuffer.putFloat((`val` shr 16 and 0xFF) * (1f / 1))
                         byteBuffer.putFloat((`val` shr 8 and 0xFF) * (1f / 1))
                         byteBuffer.putFloat((`val` and 0xFF) * (1f / 1))
                     }
                 }
                 inputFeature0.loadBuffer(byteBuffer)

                 // Runs model inference and gets result.
                 val outputs: FruitModel.Outputs = model.process(inputFeature0)
                 val outputFeature0: TensorBuffer = outputs.outputFeature0AsTensorBuffer
                 val confidences = outputFeature0.floatArray
                 // find the index of the class with the biggest confidence.
                 var maxPos = 0
                 var maxConfidence = 0f
                 for (i in confidences.indices) {
                     if (confidences[i] > maxConfidence) {
                         maxConfidence = confidences[i]
                         maxPos = i
                     }
                 }
                 val classes = arrayOf("Apple", "Banana", "Orange")
                 resultText.text = classes[maxPos]

                 // Releases model resources if no longer used.
                 model.close()
             } catch (e: Exception) {
                 // TODO Handle the exception
                 Log.i("TAG", "No image")
             }
         } else {
            Toast.makeText(this@MainActivity, "No image loaded", Toast.LENGTH_LONG).show()
         }
    }

    private fun assignReferences(){
        photoButton = findViewById(R.id.photoButton)
        galleryButton = findViewById(R.id.galleryButton)
        imageView = findViewById(R.id.imageView)
        resultText = findViewById(R.id.resultTextView)
        listenButton = findViewById(R.id.listenButton)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            cameraRequestPermissionCode -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i("TAG", "Permission has been denied by user")
                } else {
                    Log.i("TAG", "Permission has been granted by user")
                }
            }
            audioRequestPermissionCode -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i("TAG", "Permission has been denied by user")
                } else {
                    Log.i("TAG", "Permission has been granted by user")
                }
            }
        }
    }

}