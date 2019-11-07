package com.kornelvarga.mlkitdemo

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.graphics.drawable.toBitmap
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.custom.*
import com.kornelvarga.mlkitdemo.model.Prediction
import com.livinglifetechway.quickpermissions_kotlin.runWithPermissions
import com.livinglifetechway.quickpermissions_kotlin.util.QuickPermissionsOptions
import com.myhexaville.smartimagepicker.ImagePicker
import com.theartofdev.edmodo.cropper.CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE
import kotlinx.android.synthetic.main.activity_scene_classification.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SceneClassificationActivity : AppCompatActivity() {

    companion object {
        private const val IMG_SIZE_X = 224
        private const val IMG_SIZE_Y = 224
        private const val BATCH_SIZE = 1
        private const val PIXEL_SIZE = 3
        private const val IMAGE_MEAN = 128
        private const val IMAGE_STD = 128.0f
        private const val BYTES_PER_CHANNEL = 4
    }

    private lateinit var quickPermissionsOption: QuickPermissionsOptions
    private lateinit var imagePicker: ImagePicker

    private val remoteModel = FirebaseCustomRemoteModel.Builder("scene_classification").build()
    private val localModel =
        FirebaseCustomLocalModel.Builder().setAssetFilePath("scene_classification.tflite").build()

    private val inputOutputOptions = FirebaseModelInputOutputOptions.Builder()
        .setInputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 224, 224, 3))
        .setOutputFormat(0, FirebaseModelDataType.FLOAT32, intArrayOf(1, 6))
        .build()

    private lateinit var interpreter: FirebaseModelInterpreter
    private val intValues = IntArray(IMG_SIZE_X * IMG_SIZE_Y)
    private val labels = arrayOf("Buildings", "Forest", "Glacier", "Mountain", "Sea", "Street")


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scene_classification)

        quickPermissionsOption = QuickPermissionsOptions(
            handleRationale = false,
            permanentDeniedMethod = {
                Toast.makeText(this, "Go to settings...", Toast.LENGTH_LONG).show()
            }
        )

        imagePicker = ImagePicker(
            this, null
        ) { imageUri ->
            Log.d("asdf", "img uri: $imageUri")
            img_original.setImageURI(imageUri)

            val bitmap = img_original.drawable.toBitmap()
            downloadModel(bitmap)
        }

        btn_choose_image.setOnClickListener {
            choosePictures()
        }
    }

    private fun choosePictures() = this.runWithPermissions(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA,
        options = quickPermissionsOption
    ) {
        imagePicker.choosePicture(false)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            imagePicker.handleActivityResult(resultCode, requestCode, data)
        } else {
            if (resultCode == CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Log.w("TAG", "onActivityResult: Image picker Error")
            }
        }

    }

    private fun downloadModel(bitmap: Bitmap) {
        val conditions = FirebaseModelDownloadConditions.Builder()
            .requireWifi()
            .build()
        FirebaseModelManager.getInstance().download(remoteModel, conditions)
            .addOnCompleteListener {
                Log.d("asdf", "Model Downloaded succesfully...")
                val options = FirebaseModelInterpreterOptions.Builder(remoteModel).build()
                initializeInterpreter(options)
            }.continueWith {
                recogniseImage(bitmap)
            }
            .addOnFailureListener { ex ->
                Log.d("asdf", "error while downloading model: ${ex.message}")
                Log.d("asdf", "Using local model")
                val options = FirebaseModelInterpreterOptions.Builder(localModel).build()
                initializeInterpreter(options)
            }
    }

    private fun recogniseImage(bitmap: Bitmap) {
        val predictions = mutableListOf<Prediction>()

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

        val input: ByteBuffer = convertBitmapToByteBuffer(scaledBitmap)
        val inputs = FirebaseModelInputs.Builder().add(input).build()

        interpreter.run(inputs, inputOutputOptions)
            .addOnSuccessListener { result ->
                val output = result.getOutput<Array<FloatArray>>(0)

                for ((i, o) in output[0].withIndex()) {
                    val prediction = Prediction(labels[i], o)
                    predictions.add(prediction)
                }

                getTopLabels(predictions)

            }.addOnFailureListener { ex ->
                Log.e("asdf", "error: ${ex.message}")
            }
    }

    private fun initializeInterpreter(options: FirebaseModelInterpreterOptions) {
        interpreter = FirebaseModelInterpreter.getInstance(options)!!
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap?): ByteBuffer {
        //Clear the Bytebuffer for a new image
        val imgData =
            ByteBuffer.allocateDirect(BYTES_PER_CHANNEL * BATCH_SIZE * IMG_SIZE_X * IMG_SIZE_Y * PIXEL_SIZE)
        imgData.order(ByteOrder.nativeOrder())
        bitmap?.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        // Convert the image to floating point.
        var pixel = 0
        for (i in 0 until IMG_SIZE_X) {
            for (j in 0 until IMG_SIZE_Y) {
                val currPixel = intValues[pixel++]
                imgData.putFloat((Color.red(currPixel) - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat((Color.green(currPixel) - IMAGE_MEAN) / IMAGE_STD)
                imgData.putFloat((Color.blue(currPixel) - IMAGE_MEAN) / IMAGE_STD)
            }
        }
        return imgData
    }

    private fun getTopLabels(predictions: MutableList<Prediction>) {
        predictions.sortBy { it.probability }
        predictions.reverse()

        val topThree = predictions.take(3)
        chart.setPredictions(topThree as ArrayList<Prediction>)
    }
}
