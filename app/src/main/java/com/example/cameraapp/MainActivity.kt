package com.example.cameraapp

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.drawable.AdaptiveIconDrawable
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.DrawableRes
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.cameraapp.ui.theme.CameraAppTheme
import kotlinx.coroutines.launch
import java.util.concurrent.Executor
import java.util.concurrent.Executors

public class MainActivity : ComponentActivity() {

    companion object {
        private val TAG: String = MainActivity::class.java.getSimpleName()
        public fun newIntent(context: Context): Intent = Intent(context.applicationContext, MainActivity::class.java)
    }

    private val viewModel : CameraViewModel by viewModels<CameraViewModel>()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition { viewModel.keepSplashAlive }
        super.onCreate(savedInstanceState)
        setContent {
            val navController : NavHostController = rememberNavController()
            //val sheetState : SheetState = rememberModalBottomSheetState()
            //var isSheetOpen : Boolean by rememberSaveable { mutableStateOf(false) }
            val scaffoldState : BottomSheetScaffoldState = rememberBottomSheetScaffoldState()
            val scope = rememberCoroutineScope()
            CameraAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                    content = {
                        BottomSheetScaffold (
                            scaffoldState = scaffoldState,
                            sheetContent = { OptionComposable(navController, scaffoldState) },
                            sheetPeekHeight = 0.dp,
                            content = {
                                NavHostComposable(navController = navController, scaffoldState)
                            }
                        )
                    }
                )
            }
        }
    }

    @Composable
    private fun adaptiveIconPainterResource(@DrawableRes id : Int) : Painter {
        val resources : Resources = LocalContext.current.resources
        val theme : Resources.Theme = LocalContext.current.theme
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val adaptiveIcon = ResourcesCompat.getDrawable(resources, id, theme) as? AdaptiveIconDrawable
            if (adaptiveIcon != null) {
                BitmapPainter(adaptiveIcon.toBitmap().asImageBitmap())
            } else {
                painterResource(id)
            }
        } else {
            painterResource(id)
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun NavHostComposable(navController : NavHostController, scaffoldState : BottomSheetScaffoldState) {
        val scope = rememberCoroutineScope()
        NavHost (
            navController = navController,
            startDestination = viewModel.getMainRoute(),
            builder = {
                composable (
                    route = viewModel.getMainRoute(),
                    content = {
                        Box (
                            modifier = Modifier.fillMaxSize().border(2.dp, Color.Black),
                            contentAlignment = Alignment.Center,
                            content = {
                                IconButton(
                                    content = { MainComposable() },
                                    onClick = { //isSheetOpen = true
                                        scope.launch {
                                            scaffoldState.bottomSheetState.expand()
                                        }
                                    }
                                )
                            }
                        )
                        /*
                        if(isSheetOpen) {
                            ModalBottomSheet(
                                sheetState = sheetState,
                                content = { },
                                onDismissRequest = { isSheetOpen = false }
                            )
                        }
                        */
                    }
                )
                composable (
                    route = viewModel.getCameraRoute(),
                    content = {
                        val isGranted : Boolean by viewModel.observeCameraPermission().observeAsState(false)
                        if (isGranted) {
                            CameraComposable()
                        } else {
                            Text(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                text = stringResource(id = R.string.camera_not_granted),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                )
                composable (
                    route = viewModel.getVideoRoute(),
                    content = {
                        val isGranted : Boolean by viewModel.observeVideoPermission().observeAsState(false)
                        if (isGranted && viewModel.checkVideoPermission()) {
                            VideoComposable()
                        } else {
                            Text(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                text = stringResource(id = R.string.video_not_granted),
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                )
            }
        )
    }

    @Composable
    private fun MainComposable() {
        Image(painter = adaptiveIconPainterResource(id = R.mipmap.ic_launcher), contentDescription = null)
    }

    @Composable
    private fun CameraComposable() {
        val context : Context = LocalContext.current
        val previewView : PreviewView = remember { PreviewView(context) }
        val cameraController : LifecycleCameraController = remember { LifecycleCameraController(context) }
        val lifecycleOwner : LifecycleOwner = LocalLifecycleOwner.current
        val executor : Executor = remember { Executors.newSingleThreadExecutor() }
        cameraController.bindToLifecycle(lifecycleOwner)
        cameraController.setCameraSelector(viewModel.getCameraSelector())
        cameraController.setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        previewView.setController(cameraController)
        ConstraintLayout(
            modifier = Modifier.fillMaxSize().background(Color.White),
            content = {
                val leadGuideline = createGuidelineFromStart(0.05f)
                val trailGuideline = createGuidelineFromEnd(0.05f)
                val bottomGuideline = createGuidelineFromBottom(0.05f)
                val (preview, shutter, flip) = createRefs()
                Box (
                    modifier = Modifier.constrainAs(preview) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    },
                    content = {
                        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                    }
                )
                IconButton (
                    modifier = Modifier.constrainAs(shutter) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(bottomGuideline)
                    },
                    onClick = {
                        viewModel.playShutter()
                        viewModel.playVibrate()
                        cameraController.takePicture (
                            viewModel.getOutputFileOptions(null),
                            executor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output : ImageCapture.OutputFileResults) {
                                    viewModel.logImageSaved(output)
                                }
                                override fun onError(exc : ImageCaptureException) {
                                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                                }
                            }
                        )
                    },
                    content = {
                        Image(painter = painterResource(id = R.drawable.ic_shutter), contentDescription = null)
                    }
                )
                IconButton(
                    modifier = Modifier.constrainAs(flip) {
                        end.linkTo(trailGuideline)
                        bottom.linkTo(bottomGuideline)
                    },
                    onClick = {
                        viewModel.flipCamera()
                        cameraController.setCameraSelector (viewModel.getCameraSelector())
                    },
                    content = {
                        Image(painter = painterResource(id = R.drawable.ic_change), contentDescription = null)
                    }
                )
            }
        )
    }

    @Composable
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun VideoComposable() {
        val context : Context = LocalContext.current
        val previewView : PreviewView = remember { PreviewView(context) }
        val cameraController : LifecycleCameraController = remember { LifecycleCameraController(context) }
        val lifecycleOwner : LifecycleOwner = LocalLifecycleOwner.current
        val isRecording : Boolean by viewModel.observeRecording().collectAsState(initial = false)
        cameraController.bindToLifecycle(lifecycleOwner)
        cameraController.setCameraSelector(viewModel.getCameraSelector())
        cameraController.setEnabledUseCases(CameraController.VIDEO_CAPTURE)
        previewView.setController(cameraController)
        ConstraintLayout (
            modifier = Modifier.fillMaxSize().background(Color.White),
            content = {
                val leadGuideline = createGuidelineFromStart(0.05f)
                val trailGuideline = createGuidelineFromEnd(0.05f)
                val bottomGuideline = createGuidelineFromBottom(0.05f)
                val (preview, shutter, flip) = createRefs()
                Box (
                    modifier = Modifier.constrainAs(preview) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    },
                    content = {
                        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
                    }
                )
                IconButton (
                    modifier = Modifier.constrainAs(shutter) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(bottomGuideline)
                    },
                    onClick = {
                        viewModel.playRecording()
                        viewModel.playVibrate()
                        viewModel.toggleRecording()
                    },
                    content = {
                        if (isRecording) {
                            Image(painter = adaptiveIconPainterResource(id = R.drawable.ic_recording), contentDescription = null)
                            captureVideo() //TODO: Fix Video Recording
                        } else {
                            Image(painter = adaptiveIconPainterResource(id = R.drawable.ic_record), contentDescription = null)
                            viewModel.stopRecording() //TODO: Fix Video Recording
                        }
                    }
                )
                IconButton (
                    modifier = Modifier.constrainAs(flip) {
                        end.linkTo(trailGuideline)
                        bottom.linkTo(bottomGuideline)
                    },
                    onClick = { viewModel.flipCamera() },
                    content = {
                        Image(painter = painterResource(id = R.drawable.ic_change), contentDescription = null)
                    }
                )
            }
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun OptionComposable(navController : NavHostController, scaffoldState : BottomSheetScaffoldState) {
        val scope = rememberCoroutineScope()
        ConstraintLayout (
            modifier = Modifier.fillMaxWidth().wrapContentHeight().background(Color.Transparent),
            content = {
                val (title, buttonTakePhoto, buttonRecordVideo, buttonChoosePhoto, viewDivider, buttonCancel) = createRefs()
                val leadGuideline = createGuidelineFromStart(0.05f)
                val trailGuideline = createGuidelineFromEnd(0.05f)
                Text (
                    modifier = Modifier.constrainAs(title) {
                        top.linkTo(parent.top)
                        start.linkTo(leadGuideline)
                        end.linkTo(trailGuideline)
                        width = Dimension.wrapContent
                        height = Dimension.wrapContent
                    },
                    style = MaterialTheme.typography.titleLarge,
                    text = stringResource(id = R.string.camera_x),
                )
                Button (
                    onClick = {
                        scope.launch {
                            navController.navigate(viewModel.getCameraRoute())
                            viewModel.checkCameraPermission(requestPermissionLauncher)
                            scaffoldState.bottomSheetState.partialExpand()
                        }
                    },
                    modifier = Modifier.constrainAs(buttonTakePhoto) {
                            top.linkTo(title.bottom)
                            start.linkTo(leadGuideline)
                            end.linkTo(trailGuideline)
                            width = Dimension.fillToConstraints
                            height = Dimension.wrapContent
                        }.fillMaxWidth(),
                    content = { Text(text = viewModel.getCameraRoute()) }
                )
                Button (
                    onClick = {
                        scope.launch {
                            navController.navigate(viewModel.getVideoRoute())
                            viewModel.checkVideoPermission(requestPermissionsLauncher) //TODO: Video Request Permission
                            scaffoldState.bottomSheetState.partialExpand()
                        }
                    },
                    modifier = Modifier.constrainAs(buttonRecordVideo) {
                            top.linkTo(buttonTakePhoto.bottom)
                            start.linkTo(leadGuideline)
                            end.linkTo(trailGuideline)
                            width = Dimension.fillToConstraints
                            height = Dimension.wrapContent
                        }.fillMaxWidth(),
                    content = { Text(text = viewModel.getVideoRoute()) }
                )
                Button (
                    onClick = {
                        scope.launch {
                            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                            galleryLauncher.launch(galleryIntent)
                            scaffoldState.bottomSheetState.partialExpand()
                        }
                    },
                    modifier = Modifier.constrainAs(buttonChoosePhoto) {
                            top.linkTo(buttonRecordVideo.bottom)
                            start.linkTo(leadGuideline)
                            end.linkTo(trailGuideline)
                            width = Dimension.fillToConstraints
                            height = Dimension.wrapContent
                        }.fillMaxWidth(),
                    content = {
                        Text(text = stringResource(id = R.string.choose_photo))
                    }
                )
                Divider (
                    color = Color.DarkGray,
                    modifier = Modifier.constrainAs(viewDivider) {
                            top.linkTo(buttonChoosePhoto.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            width = Dimension.fillToConstraints
                            height = Dimension.wrapContent
                        }.fillMaxWidth(),
                    thickness = 1.dp,
                )
                TextButton (
                    onClick = {
                        scope.launch {
                            scaffoldState.bottomSheetState.partialExpand()
                        }
                    },
                    modifier = Modifier.constrainAs(buttonCancel) {
                            top.linkTo(viewDivider.bottom)
                            start.linkTo(leadGuideline)
                            end.linkTo(trailGuideline)
                            width = Dimension.fillToConstraints
                            height = Dimension.wrapContent
                        }.fillMaxWidth(),
                    //colors = ButtonDefaults.buttonColors( contentColor = Color(red = 255, green = 100, blue = 100) ),
                    content = {
                        Text(
                            color = Color(red = 255, green = 100, blue = 100),
                            text = stringResource(id = R.string.cancel)
                        )
                    }
                )
            }
        )
    }

    @Composable
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun captureVideo() {
        val executor : Executor = remember { Executors.newSingleThreadExecutor() }
        viewModel.startRecording (
            this@MainActivity.getContentResolver(),
            viewModel.getContentValues(null, null),
            executor,
            viewModel.getRecordingListener()
        )
    }

    private val requestPermissionLauncher : ActivityResultLauncher<String> = registerForActivityResult( ActivityResultContracts.RequestPermission(),) { isGranted ->
        Log.d("$TAG PERMISSIONS", "Camera Request Launcher result: " + isGranted.toString())
        if (isGranted) {
            viewModel.grantedCameraPermission()
        } else {
            viewModel.deniedCameraPermission()
        }
    }

    private val requestPermissionsLauncher : ActivityResultLauncher<Array<String>> = registerForActivityResult( ActivityResultContracts.RequestMultiplePermissions()) { areGranted ->
        Log.d("$TAG PERMISSIONS", "Video Request Launcher result: " + areGranted.toString())
        if (areGranted.filter { isGranted -> isGranted.value == false }.isEmpty()) {
            viewModel.grantedVideoPermission()
        } else {
            viewModel.deniedVideoPermission()
        }
    }

    val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d("$TAG PERMISSIONS", "Gallery Launcher result: ${result.resultCode} ${result.data}")
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
        }
    }
}