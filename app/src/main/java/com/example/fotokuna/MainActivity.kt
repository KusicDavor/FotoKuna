package com.example.fotokuna

import android.Manifest.permission.CAMERA
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.text.isDigitsOnly
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.text.TextBlock
import com.google.android.gms.vision.text.TextRecognizer
import kotlinx.android.synthetic.main.activity_main.*
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

class MainActivity : AppCompatActivity() {

    private val MY_PERMISSIONS_REQUEST_CAMERA: Int = 101
    private lateinit var mCameraSource: CameraSource
    private lateinit var textRecognizer: TextRecognizer
    private val tag: String? = "MainActivity"
    private lateinit var iznosEuro: String
    private var postoji = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestforPermission()

        textRecognizer = TextRecognizer.Builder(this).build()
        if (!textRecognizer.isOperational) {
            Toast.makeText(
                this,
                "Dependencies are not loaded yet...please try after few moment!!",
                Toast.LENGTH_SHORT
            ).show()
            Log.e(tag, "Dependencies are downloading....try after few moment")
            return
        }
        mCameraSource = CameraSource.Builder(applicationContext, textRecognizer)
            .setFacing(CameraSource.CAMERA_FACING_BACK)
            .setRequestedPreviewSize(1280, 1024)
            .setAutoFocusEnabled(true)
            .setRequestedFps(2.0f)
            .build()

        surface_camera_preview.holder.addCallback(object : SurfaceHolder.Callback {
            @SuppressLint("MissingPermission")
            override fun surfaceCreated(p0: SurfaceHolder) {
                try {
                    if (isCameraPermissionGranted()) {
                        mCameraSource.start(surface_camera_preview.holder)
                    } else {
                        requestforPermission()
                    }
                } catch (e: Exception) {
                    toast("Error: " + e.message)
                }
            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                mCameraSource.stop()
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
            }
        }
        )

        textRecognizer.setProcessor(object : Detector.Processor<TextBlock> {
            override fun release() {}
            var kunski_iznos: String = ""

            override fun receiveDetections(detections: Detector.Detections<TextBlock>) {
                val items = detections.detectedItems
                if (items.size() <= 0) {
                    return
                }

                tv_result.post {
                    iznosEuro = prepoznajEure(items)
                    kunski_iznos = pretvoriUKunu(iznosEuro)
                    postoji(kunski_iznos)
                }
            }
        }
        )
    }

    private fun postoji(kunski_iznos : String) {
        if (iznosEuro != "" && tv_result.text == "SKENIRAM") {
            tv_result.text = iznosEuro + "€" + "\n" + kunski_iznos
        }
    }

    private fun pretvoriUKunu(stringEuro: String): String {
        var kuna : Int
        val nf = NumberFormat.getInstance(Locale.getDefault())
        val dec = DecimalFormat("#,###.##")
        try {
            kuna = ((nf.parse(stringEuro).toDouble() * 7.53450).toInt())
            return dec.format(kuna) + " KN"
        } catch (e: Exception) {
            return "pogreška u konverziji"
        }
    }

    private fun prepoznajEure(items: SparseArray<TextBlock>): String {
        var string = ""
        var ca: CharArray
        for (i in 0 until items.size()) {
            val item = items.valueAt(i).value
            if (item.contains("€") || item.contains("EUR")) {
                string += item.substringBefore("€")
                ca = string.toCharArray()
                string = ""
                for (c: Char in ca) {
                    if (c.toString() == "," || c.toString() == "." || c.toString().isDigitsOnly()) {
                        string += c
                    }
                }
                break
            }
        }
        return string
    }

    private fun requestforPermission() {
        if (ContextCompat.checkSelfPermission(
                this@MainActivity, CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this@MainActivity, CAMERA)) {
            } else {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(CAMERA), MY_PERMISSIONS_REQUEST_CAMERA
                )
            }
        } else {

        }
    }

    private fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this@MainActivity,
            CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun toast(text: String) {
        Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_CAMERA -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                } else {
                    requestforPermission()
                }
                return
            }
            else -> {}
        }
    }

    fun resetiraj(view: View) {
        tv_result.post{
            postoji == false
            tv_result.text = "SKENIRAM"
        }
    }
}