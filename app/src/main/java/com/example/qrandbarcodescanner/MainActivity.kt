package com.example.qrandbarcodescanner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.qrandbarcodescanner.databinding.ActivityMainBinding
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraPermission: Array<String>
    private lateinit var storagePermission: Array<String>
    private var imageUri: Uri? = null
    private var barcodeScannerOptions: BarcodeScannerOptions? = null
    private var barcodeScanner: BarcodeScanner? = null

    companion object {
        private const val CAMERA_REQUEST_CODE = 100
        private const val STORAGE_REQUEST_CODE = 101
        private const val TAG = "MAIN_TAG"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraPermission = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        storagePermission = arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

        barcodeScannerOptions =
            BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()

        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions!!)

        binding.cameraBtn.setOnClickListener {
            if (checkCameraPermissions()) {
                pickImageCamera()
            } else {
                requestCameraPermission()
            }
        }
        binding.galleryBtn.setOnClickListener {
            if (checkStoragePermission()) {
                pickImageGallery()
            } else {
                requestStoragePermission()
            }
        }
        binding.scanBtn.setOnClickListener {
            if (imageUri != null) {
                showToast("pick image first")
            } else {
                detectResultFromImage()
            }
        }
    }

    private fun detectResultFromImage() {
        try {
            val inputImage = InputImage.fromFilePath(this, imageUri!!)
            val barcodeResult = barcodeScanner!!.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    extractBarcodeQrCodeInfo(barcodes)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "detectResultFromImage: ", e)
                    showToast("failed scanning due to ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "detectResultFromImage: ", e)
            showToast("failed due to ${e.message} ")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun extractBarcodeQrCodeInfo(barcodes: List<Barcode>) {
        for (barcode in barcodes) {
            val bount = barcode.boundingBox
            val corners = barcode.cornerPoints

            val rawValue = barcode.rawValue
            Log.d(TAG, "extractBarcodeQrCodeInfo: rawValue: $rawValue")

            val valueType = barcode.valueType
            when (valueType) {
                Barcode.TYPE_WIFI -> {
                    val typeWifi = barcode.wifi
                    val ssid = "${typeWifi?.ssid}"
                    val password = "${typeWifi?.password}"
                    var encryptionType = "${typeWifi?.encryptionType}"

                    if (encryptionType == "1") {
                        encryptionType = "OPEN"
                    } else if (encryptionType == "2") {
                        encryptionType = "WPA"
                    } else if (encryptionType == "3") {
                        encryptionType = "WEP"
                    }

                    Log.d(TAG, "extractBarcodeQrCodeInfo: TYPE_WIFI")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: ssid :$ssid")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: password :$password")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: encryptionType :$encryptionType")

                    binding.resultTv.text =
                        "TYPE_WIFI \nssid:$ssid \npassword:$password  \nencryptionType:$encryptionType \n\nrawValue: $rawValue"
                }

                Barcode.TYPE_URL -> {
                    val typeUrl = barcode.url
                    val title = "${typeUrl?.title}"
                    val url = "${typeUrl?.url}"
                    Log.d(TAG, "extractBarcodeQrCodeInfo: TYPE_URL")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: title:$title")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: url:$url")

                    binding.resultTv.text =
                        "TYPE_URL \ntitle : $title \nurl $url \n\nrawValue $rawValue"
                }

                Barcode.TYPE_EMAIL -> {
                    val typeEmail = barcode.email
                    val address = "${typeEmail?.address}"
                    val body = "${typeEmail?.body}"
                    val subject = "${typeEmail?.subject}"

                    Log.d(TAG, "extractBarcodeQrCodeInfo:TYPE_EMAIL ")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: address :$address")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: body :$body")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: subject :$subject")

                    binding.resultTv.text = ""
                }

                Barcode.TYPE_CONTACT_INFO -> {
                    val typeContact = barcode.contactInfo
                    val title = "${typeContact?.title}"
                    val organization = "${typeContact?.organization}"
                    val name = "${typeContact?.name?.first} ${typeContact?.name?.last}"
                    val phone = "${typeContact?.name?.first} ${typeContact?.phones?.get(0)?.number}"

                    Log.d(TAG, "extractBarcodeQrCodeInfo:TYPE_CONTACT_INFO ")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: title : $title")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: organization : $organization")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: name : $name")
                    Log.d(TAG, "extractBarcodeQrCodeInfo: phone : $phone")

                    binding.resultTv.text =
                        "TYPE_CONTACT_INFO \ntitle:$title \norganization :$organization \nname :$name \nphone :$phone \nrawValue :$rawValue"
                }

                else -> {
                    binding.resultTv.text = "rawValue:$rawValue"
                }
            }
        }
    }

    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        galleryActivityResultLauncher.launch(intent)
    }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->

        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            imageUri = data?.data
            Log.d(TAG, ": imageUri: $imageUri")
            binding.imageIv.setImageURI(imageUri)
        } else {
            showToast("cancelllllll")
        }
    }

    private fun pickImageCamera() {
        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, "sample image")
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "sample image description")

        imageUri =
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
        cameraActivityResultLauncher.launch(intent)
    }

    private val cameraActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            Log.d(TAG, "cameraActivityResultLauncher imageUri: $imageUri")

            binding.imageIv.setImageURI(imageUri)
        }

    }

    private fun checkStoragePermission(): Boolean {
        val result = (ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)
        return result
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE)
    }

    private fun checkCameraPermissions(): Boolean {
        val resultCamera = (ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED)
        val resultStorage = (ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED)
        return resultCamera && resultStorage
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    val storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED

                    if (cameraAccepted && storageAccepted) {
                        pickImageCamera()
                    } else {
                        showToast("Camera  and storage  permission are required")
                    }
                }
            }

            STORAGE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty()) {
                    val storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    if (storageAccepted) {
                        pickImageGallery()
                    } else {
                        showToast("storage permission is required")
                    }
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}