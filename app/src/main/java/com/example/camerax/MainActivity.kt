package com.example.camerax

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.camerax.databinding.ActivityMainBinding
import com.example.camerax.retrofit.AuthRequest
import com.example.camerax.retrofit.MainApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors



class MainActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityMainBinding
    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private var myTimer: Timer? = null
    private var timerTask: TimerTask? = null
    private var res1: String = "null"
    private var res2: String = "null"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)


        val interceptor = HttpLoggingInterceptor()
        interceptor.level = HttpLoggingInterceptor.Level.BODY

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("http://192.168.1.68:8000/").client(client)
            .addConverterFactory(GsonConverterFactory.create()).build()
        val mainApi = retrofit.create(MainApi::class.java)

        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
        myTimer = Timer()
        timerTask = object : TimerTask() {
            override fun run() {
                res2 = res1
                takePhoto()
                CoroutineScope(Dispatchers.IO).launch {
                    mainApi.auth(AuthRequest("Popitka1.jpeg", res2 ))
                }
            }
        }
        myTimer?.scheduleAtFixedRate(timerTask, 30000, 30000) //задержка и период в миллисекундах
    }

    private fun takePhoto() {
        Log.d("TAT", "Privet1")
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        imageCapture.takePicture(Executors.newSingleThreadExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    super.onCaptureSuccess(image)
                    //get bitmap from image
                    val byteArrayOutputStream = ByteArrayOutputStream()
                    val bitmap = imageProxyToBitmap(image)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                    val imageBytes: ByteArray = byteArrayOutputStream.toByteArray()
                    res1 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
                    Log.d("TAT", "Privet7")
                    image.close()

                }
                private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
                    val planeProxy = image.planes[0]
                    val buffer: ByteBuffer = planeProxy.buffer
                    val bytes = ByteArray(buffer.remaining())
                    buffer.get(bytes)
                    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }
        )
    }

    private fun startCamera() {
        Log.d("TAT", "start")
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            // Preview
            val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder()
             .setJpegQuality(100)
                .build()
            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                // Bind use cases to camera
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch(exc: Exception) {
                Log.e("TAT", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}