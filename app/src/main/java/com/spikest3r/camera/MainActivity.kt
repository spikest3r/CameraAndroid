package com.spikest3r.camera

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.spikest3r.camera.ui.theme.SomeTestingTheme
import android.Manifest
import android.content.ContentValues
import android.hardware.display.DisplayManager
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.Surface
import android.widget.Toast
import androidx.camera.core.CameraControl
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraFront
import androidx.compose.material.icons.filled.CameraRear
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    lateinit var cameraProvider: ProcessCameraProvider
    lateinit var previewView: PreviewView
    lateinit var cameraControl: CameraControl

    override fun onCreate(savedInstanceState: Bundle?) {
        val cameraPermission = Manifest.permission.CAMERA
        if (ContextCompat.checkSelfPermission(this, cameraPermission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(cameraPermission), 1001)
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SomeTestingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    App()
                }
            }
        }
    }

    var lensFacing = CameraSelector.LENS_FACING_BACK

    @Composable
    fun rememberDisplayRotation(): Int {
        val context = LocalContext.current
        val rotationState = remember { mutableStateOf(context.display.rotation) }

        DisposableEffect(Unit) {
            val listener = object : DisplayManager.DisplayListener {
                override fun onDisplayChanged(displayId: Int) {
                    val rotation = context.display.rotation
                    rotationState.value = rotation
                }
                override fun onDisplayAdded(displayId: Int) {}
                override fun onDisplayRemoved(displayId: Int) {}
            }

            val displayManager = context.getSystemService(DISPLAY_SERVICE) as DisplayManager
            displayManager.registerDisplayListener(listener, null)

            onDispose {
                displayManager.unregisterDisplayListener(listener)
            }
        }
        return rotationState.value
    }

    // ui

    @Composable
    fun App(

    ) {
        val context = LocalContext.current
        val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
        val rotation = rememberDisplayRotation()
        var flashSetting by remember {mutableStateOf(ImageCapture.FLASH_MODE_OFF)}
        var isFlashAvailable by remember {mutableStateOf(false)}

        val imageCapture = remember(rotation) {
            ImageCapture.Builder()
                .setTargetRotation(rotation)
                .build()
        }

        Box(modifier = Modifier.fillMaxSize()) {
            val captureButtonWidth = 96.dp
            val switchButtonSize = 64.dp
            val gap = 48.dp

            val btnCaptureModifier = remember(rotation) {
                Modifier
                    .align(if (rotation == Surface.ROTATION_0) Alignment.BottomCenter else Alignment.CenterEnd)
                    .padding(bottom = if (rotation == Surface.ROTATION_0) 96.dp else 0.dp,
                        end = if(rotation != Surface.ROTATION_0) 96.dp else 0.dp)
                    .size(96.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            }

            val btnSwitchLensModifier = remember(rotation) {
                Modifier
                    .align(if (rotation == Surface.ROTATION_0) Alignment.BottomCenter else Alignment.CenterEnd)
                    .padding(bottom = if (rotation == Surface.ROTATION_0) 112.dp else 0.dp,
                        end = if (rotation != Surface.ROTATION_0) 112.dp else 0.dp)
                    .offset(x = if(rotation == Surface.ROTATION_0) captureButtonWidth / 2 + gap + switchButtonSize / 2 else 0.dp,
                        y = -if(rotation != Surface.ROTATION_0) captureButtonWidth / 2 + gap + switchButtonSize / 2 else 0.dp)
                    .size(switchButtonSize)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            }

            val btnFlashModifier = remember(rotation) {
                Modifier
                    .align(if (rotation == Surface.ROTATION_0) Alignment.BottomCenter else Alignment.CenterEnd)
                    .padding(bottom = if (rotation == Surface.ROTATION_0) 112.dp else 0.dp,
                        end = if (rotation != Surface.ROTATION_0) 112.dp else 0.dp)
                    .offset(x = -if(rotation == Surface.ROTATION_0) captureButtonWidth / 2 + gap + switchButtonSize / 2 else 0.dp,
                        y = if(rotation != Surface.ROTATION_0) captureButtonWidth / 2 + gap + switchButtonSize / 2 else 0.dp)
                    .size(switchButtonSize)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            }

            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                context = context,
                lifecycleOwner = lifecycleOwner,
                imageCapture = imageCapture,
                setIsFlashAvailable = {flash -> isFlashAvailable = flash}
            )

            Box(
                modifier = btnCaptureModifier,
                contentAlignment = Alignment.Center
            ) {
                ButtonCapture(
                    modifier = Modifier.fillMaxSize(),
                    context = context,
                    imageCapture = imageCapture,
                    flashSetting
                )
            }

            Box(
                modifier = btnSwitchLensModifier,
                contentAlignment = Alignment.Center
            ) {
                SwitchLens(
                    modifier = Modifier.fillMaxSize(),
                    lifecycleOwner = lifecycleOwner,
                    imageCapture = imageCapture,
                    setIsFlashAvailable = {flash -> isFlashAvailable = flash}
                )
            }

            if(isFlashAvailable) {
                Box(
                    modifier = btnFlashModifier,
                    contentAlignment = Alignment.Center
                ) {
                    Flash(
                        modifier = Modifier.fillMaxSize(),
                        flashSetting = flashSetting,
                        setFlashSetting = { newSetting -> flashSetting = newSetting },
                    )
                }
            }
        }
    }

    @Composable
    fun ButtonCapture(modifier: Modifier = Modifier, context: Context, imageCapture: ImageCapture, flashSetting: Int) {
        IconButton(
            onClick = { takePhoto(context, imageCapture, flashSetting) },
            modifier = modifier
        ) {
            Icon(
                imageVector = Icons.Filled.Camera,
                contentDescription = "Capture a picture",
                tint = Color.White,
                modifier = Modifier.size(80.dp)
            )
        }
    }

    @Composable
    fun SwitchLens(modifier: Modifier = Modifier, lifecycleOwner: LifecycleOwner, imageCapture: ImageCapture, setIsFlashAvailable: (Boolean) -> Unit) {
        var icon by remember {mutableStateOf(Icons.Filled.CameraFront)} // store icon of camera we want to switch to
        IconButton(
            onClick = {
                lensFacing = if(lensFacing == CameraSelector.LENS_FACING_BACK)
                    CameraSelector.LENS_FACING_FRONT
                else
                    CameraSelector.LENS_FACING_BACK
                icon = if(lensFacing == CameraSelector.LENS_FACING_BACK)
                    Icons.Filled.CameraFront
                else
                    Icons.Filled.CameraRear
                cameraControl = rebindCamera(lifecycleOwner, imageCapture, setIsFlashAvailable)
            },
            modifier = modifier
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Switch lens",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }

    @Composable
    fun Flash(modifier: Modifier = Modifier, flashSetting: Int, setFlashSetting: (Int) -> Unit) {
        val icon = remember(flashSetting) {
            when(flashSetting) {
                ImageCapture.FLASH_MODE_OFF -> Icons.Filled.FlashOff
                ImageCapture.FLASH_MODE_ON -> Icons.Filled.FlashOn
                ImageCapture.FLASH_MODE_AUTO -> Icons.Filled.FlashAuto
                else -> Icons.Filled.FlashOff
            }
        }
        IconButton(
            onClick = {
                val nextFlashSetting = when(flashSetting) {
                    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                    ImageCapture.FLASH_MODE_ON ->  ImageCapture.FLASH_MODE_OFF
                    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                    else -> ImageCapture.FLASH_MODE_OFF
                }
                setFlashSetting(nextFlashSetting)
            },
            modifier = modifier
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Flash setting",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }

    @Composable
    fun CameraPreview(modifier: Modifier = Modifier, context: Context, lifecycleOwner: LifecycleOwner, imageCapture: ImageCapture, setIsFlashAvailable: (Boolean) -> Unit) {
        AndroidView(modifier = modifier,
            factory = {
                previewView = PreviewView(context)
                var cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProvider = cameraProviderFuture.get()

                cameraProviderFuture.addListener({
                    cameraControl = rebindCamera(lifecycleOwner, imageCapture, setIsFlashAvailable)

                    previewView.setOnTouchListener { v, event ->
                        if(event.action == MotionEvent.ACTION_DOWN) {
                            cameraControl.let { control ->
                                val factory = previewView.meteringPointFactory
                                val point = factory.createPoint(event.x,event.y)
                                val action = FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                                    .setAutoCancelDuration(3, TimeUnit.SECONDS)
                                    .build()
                                control.startFocusAndMetering(action)
                            }
                            v.performClick()
                            true
                        } else {
                            false
                        }
                    }
                }, ContextCompat.getMainExecutor(context))

                previewView
            }
        )
    }

    // Helper functions

    fun rebindCamera(lifecycleOwner: LifecycleOwner,
                     imageCapture: ImageCapture, setIsFlashAvailable: (Boolean) -> Unit): CameraControl {
        val preview = androidx.camera.core.Preview.Builder().build(). also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()
        cameraProvider.unbindAll()
        val camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            preview,
            imageCapture
        )
        val cameraInfo = camera.cameraInfo
        setIsFlashAvailable(cameraInfo.hasFlashUnit())
        return camera.cameraControl
    }

    fun takePhoto(context: Context, imageCapture: ImageCapture, flashSetting: Int) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "pic_${System.currentTimeMillis()}")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MyAppPhotos")
        }
        imageCapture.flashMode = flashSetting
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(context.contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(context, "An error has occurred!", Toast.LENGTH_SHORT).show()
                    Log.e("takePhoto", exc.message.toString())
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(context,"Picture captured and saved to Pictures folder", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}