package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.BuildConfig
import com.example.model.ThemePreference
import com.example.ui.screens.AssistantScreen
import com.example.ui.screens.ComparisonScreen
import com.example.ui.screens.DesignStudioScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MyDesignsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SubscriptionScreen
import com.example.ui.theme.RoomAITheme
import com.example.util.ImageUtils
import com.example.util.configureAppCheck
import com.example.util.readAppCheckDebugSecret
import com.example.viewmodel.RoomAiViewModel
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.delay

object RoomRoutes {
    const val ROUTE_HOME = "home"
    const val ROUTE_DESIGN = "design"
    const val ROUTE_MY_DESIGNS = "my_designs"
    const val ROUTE_PROFILE = "profile"
    const val ROUTE_COMPARISON = "comparison"
    const val ROUTE_ASSISTANT = "assistant"
    const val ROUTE_SUBSCRIPTION = "subscription"
}

private data class BottomNavTab(
    val route: String,
    val labelHu: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val BOTTOM_NAV_TABS = listOf(
    BottomNavTab(
        route = RoomRoutes.ROUTE_HOME,
        labelHu = "Kezdőlap",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home,
        testTag = "nav_tab_home"
    ),
    BottomNavTab(
        route = RoomRoutes.ROUTE_DESIGN,
        labelHu = "Tervezés",
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_tab_design"
    ),
    BottomNavTab(
        route = RoomRoutes.ROUTE_MY_DESIGNS,
        labelHu = "Saját terveim",
        selectedIcon = Icons.Filled.Collections,
        unselectedIcon = Icons.Outlined.Collections,
        testTag = "nav_tab_gallery"
    ),
    BottomNavTab(
        route = RoomRoutes.ROUTE_PROFILE,
        labelHu = "Profil",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person,
        testTag = "nav_tab_profile"
    )
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI or Firebase operations
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        // STEP 2: Initialize UI with dynamic ThemePreference support
        setContent {
            RoomAiAppRoot()
        }
    }
}

@Composable
fun RoomAiAppRoot() {
    val context = LocalContext.current
    var debugSecret by remember { mutableStateOf<String?>(null) }
    var showDebugDialog by remember { mutableStateOf(BuildConfig.DEBUG) }
    if (BuildConfig.DEBUG) {
        LaunchedEffect(Unit) {
            // Trigger debug-provider secret generation; a rejected token is expected until allowlisted.
            FirebaseAppCheck.getInstance().getAppCheckToken(false)
            repeat(30) {
                debugSecret = readAppCheckDebugSecret(context)
                if (!debugSecret.isNullOrBlank()) return@LaunchedEffect
                delay(500)
            }
        }
        if (showDebugDialog) {
            AlertDialog(
                onDismissRequest = { showDebugDialog = false },
                title = { Text("Firebase App Check – debug token") },
                text = {
                    Text(
                        debugSecret ?: "Token generálása folyamatban… Ha nem jelenik meg, indítsd újra az alkalmazást."
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        debugSecret?.let { secret ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Firebase App Check debug token", secret))
                        }
                    }, enabled = !debugSecret.isNullOrBlank()) { Text("Token másolása") }
                },
                dismissButton = {
                    TextButton(onClick = { showDebugDialog = false }) { Text("Bezárás") }
                }
            )
        }
    }
    val viewModel: RoomAiViewModel = viewModel(
        factory = RoomAiViewModel.provideFactory(context)
    )

    val studioState by viewModel.studioState.collectAsStateWithLifecycle()
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isAssistantThinking by viewModel.isAssistantThinking.collectAsStateWithLifecycle()
    val billingConnectionState by viewModel.billingConnectionState.collectAsStateWithLifecycle()
    val billingTransactions by viewModel.billingTransactions.collectAsStateWithLifecycle()
    val aiGenerationLogs by viewModel.aiGenerationLogs.collectAsStateWithLifecycle()

    val isSystemDark = isSystemInDarkTheme()
    val useDarkTheme = when (userProfile.themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM -> isSystemDark
    }

    RoomAITheme(darkTheme = useDarkTheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: RoomRoutes.ROUTE_HOME

            val snackbarHostState = remember { SnackbarHostState() }

            LaunchedEffect(studioState.snackbarMessage) {
                studioState.snackbarMessage?.let { msg ->
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearSnackbarMessage()
                }
            }

            // Android Photo Picker Launcher (Zero-permission compliant)
            val photoPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.PickVisualMedia()
            ) { uri: Uri? ->
                if (uri != null) {
                    viewModel.onPhotoSelected(uri)
                    if (currentRoute != RoomRoutes.ROUTE_DESIGN) {
                        navController.navigate(RoomRoutes.ROUTE_DESIGN) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            }

            // Camera Capture Launcher & Runtime Permission Flow
            var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

            val cameraTakePictureLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicture()
            ) { success: Boolean ->
                val capturedUri = pendingCameraUri
                if (success && capturedUri != null) {
                    viewModel.onPhotoSelected(capturedUri)
                }
            }

            val cameraPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    try {
                        val uri = ImageUtils.createCameraCaptureUri(context)
                        pendingCameraUri = uri
                        cameraTakePictureLauncher.launch(uri)
                    } catch (e: Exception) {
                        viewModel.showSnackbar("Nem sikerült elindítani a kamerát: ${e.localizedMessage}")
                    }
                } else {
                    viewModel.showSnackbar("A kamera engedély megtagadva. Választhatsz fotót a Galériából is!")
                }
            }

            val triggerPhotoPicker: () -> Unit = {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }

            val triggerCameraCapture: () -> Unit = {
                val hasPerm = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                    try {
                        val uri = ImageUtils.createCameraCaptureUri(context)
                        pendingCameraUri = uri
                        cameraTakePictureLauncher.launch(uri)
                    } catch (e: Exception) {
                        viewModel.showSnackbar("Nem sikerült elindítani a kamerát: ${e.localizedMessage}")
                    }
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            }

            val navigateToTab: (String) -> Unit = { targetRoute ->
                navController.navigate(targetRoute) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }

            val showBottomBar = currentRoute in BOTTOM_NAV_TABS.map { it.route }

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                bottomBar = {
                    if (showBottomBar) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp
                        ) {
                            BOTTOM_NAV_TABS.forEach { tab ->
                                val selected = currentRoute == tab.route
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.labelHu
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.labelHu,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                    ),
                                    modifier = Modifier.testTag(tab.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = RoomRoutes.ROUTE_HOME,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(RoomRoutes.ROUTE_HOME) {
                        HomeScreen(
                            userProfile = userProfile,
                            recentProjects = savedProjects,
                            onStartNewDesign = { navigateToTab(RoomRoutes.ROUTE_DESIGN) },
                            onUploadPhotoClick = triggerPhotoPicker,
                            onStyleClick = { style ->
                                viewModel.selectStyle(style)
                                navigateToTab(RoomRoutes.ROUTE_DESIGN)
                            },
                            onOpenProject = { project ->
                                viewModel.openSavedProjectInComparison(project) {
                                    navController.navigate(RoomRoutes.ROUTE_COMPARISON)
                                }
                            },
                            onOpenAllProjects = { navigateToTab(RoomRoutes.ROUTE_MY_DESIGNS) },
                            onOpenPremium = { navController.navigate(RoomRoutes.ROUTE_SUBSCRIPTION) },
                            onOpenAssistant = { navController.navigate(RoomRoutes.ROUTE_ASSISTANT) },
                            onToggleTheme = { viewModel.toggleLightDarkTheme() }
                        )
                    }

                    composable(RoomRoutes.ROUTE_DESIGN) {
                        DesignStudioScreen(
                            studioState = studioState,
                            userProfile = userProfile,
                            onPickFromGallery = triggerPhotoPicker,
                            onTakeCameraPhoto = triggerCameraCapture,
                            onLoadSamplePhoto = { runLive -> viewModel.loadSampleRoomPhotograph(runLive) },
                            onRemovePhoto = { viewModel.removeUploadedPhoto() },
                            onRetryAnalysis = { viewModel.analyzeCurrentRoom() },
                            onSelectStyle = { viewModel.selectStyle(it) },
                            onSelectColorPalette = { viewModel.selectColorPalette(it) },
                            onSelectBudget = { viewModel.selectBudget(it) },
                            onSelectAtmosphere = { viewModel.selectAtmosphere(it) },
                            onSelectDecorDensity = { viewModel.selectDecorDensity(it) },
                            onToggleFurniturePref = { viewModel.toggleFurniturePreference(it) },
                            onCustomInstructionsChange = { viewModel.updateCustomInstructions(it) },
                            onAppendQuickInstruction = { viewModel.appendQuickInstruction(it) },
                            onTransformRoom = {
                                viewModel.generateRoomRedesign {
                                    navController.navigate(RoomRoutes.ROUTE_COMPARISON)
                                }
                            },
                            onLaunchDemoComparison = {
                                viewModel.launchInteractiveDemoShowcase {
                                    navController.navigate(RoomRoutes.ROUTE_COMPARISON)
                                }
                            },
                            onOpenPremium = { navController.navigate(RoomRoutes.ROUTE_SUBSCRIPTION) }
                        )
                    }

                    composable(RoomRoutes.ROUTE_MY_DESIGNS) {
                        MyDesignsScreen(
                            projects = savedProjects,
                            isPremium = userProfile.isPremium,
                            onOpenProjectComparison = { project ->
                                viewModel.openSavedProjectInComparison(project) {
                                    navController.navigate(RoomRoutes.ROUTE_COMPARISON)
                                }
                            },
                            onDeleteProject = { viewModel.deleteSavedProject(it) },
                            onStartNewDesign = { navigateToTab(RoomRoutes.ROUTE_DESIGN) },
                            onShowMessage = { viewModel.showSnackbar(it) }
                        )
                    }

                    composable(RoomRoutes.ROUTE_PROFILE) {
                        ProfileScreen(
                            userProfile = userProfile,
                            savedProjectCount = savedProjects.size,
                            chatMessageCount = chatMessages.size,
                            billingTransactions = billingTransactions,
                            aiGenerationLogs = aiGenerationLogs,
                            onOpenSubscription = { navController.navigate(RoomRoutes.ROUTE_SUBSCRIPTION) },
                            onSelectThemePreference = { viewModel.selectThemePreference(it) },
                            onSelectLanguage = { viewModel.selectLanguage(it) },
                            onUpdateProfile = { name, email -> viewModel.updateUserProfile(name, email) },
                            onDeleteAccount = { viewModel.deleteAccountAndLocalData() }
                        )
                    }

                    composable(RoomRoutes.ROUTE_COMPARISON) {
                        ComparisonScreen(
                            studioState = studioState,
                            onBack = { navController.popBackStack() },
                            onSaveToDevice = { viewModel.saveCurrentComparisonToDevice() },
                            onShareImage = { viewModel.shareCurrentComparisonImage() },
                            onCustomInstructionsChange = { viewModel.updateCustomInstructions(it) },
                            onRegenerateWithAi = {
                                viewModel.generateRoomRedesign {
                                    // Stay on ComparisonScreen with newly generated image
                                }
                            }
                        )
                    }

                    composable(RoomRoutes.ROUTE_ASSISTANT) {
                        AssistantScreen(
                            studioState = studioState,
                            messages = chatMessages,
                            isThinking = isAssistantThinking,
                            onSendMessage = { viewModel.sendAssistantMessage(it) },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(RoomRoutes.ROUTE_SUBSCRIPTION) {
                        SubscriptionScreen(
                            userProfile = userProfile,
                            billingConnectionState = billingConnectionState,
                            transactions = billingTransactions,
                            onTryNativePlayBilling = { act, plan ->
                                viewModel.tryStartNativePlayBilling(act, plan)
                            },
                            onCompleteSandboxPurchase = { plan, methodLabel, decline ->
                                viewModel.completePlaySandboxPurchase(plan, methodLabel, decline) {
                                    // Purchase recorded in SQL; user can inspect receipt on screen
                                }
                            },
                            onRefundTransaction = { tx -> viewModel.refundBillingTransaction(tx) },
                            onRefreshBilling = { viewModel.refreshPlayBillingStatus() },
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
