package app.kodanotes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.kodanotes.ai.AiService
import app.kodanotes.data.audio.WhisperTranscriber
import app.kodanotes.screens.ExamenScreen
import app.kodanotes.screens.FlashcardsScreen
import app.kodanotes.screens.HomeScreen
import app.kodanotes.screens.RecordScreen
import app.kodanotes.screens.SettingsScreen
import app.kodanotes.ui.theme.BackgroundLight
import app.kodanotes.ui.theme.DarkContainer
import app.kodanotes.ui.theme.KodaNotesTheme
import app.kodanotes.ui.theme.TealAccent
import app.kodanotes.ui.theme.TextMuted
import app.kodanotes.viewmodel.ExamenViewModel
import app.kodanotes.viewmodel.ExamenViewModelFactory
import app.kodanotes.viewmodel.FlashcardsViewModel
import app.kodanotes.viewmodel.FlashcardsViewModelFactory
import app.kodanotes.viewmodel.HomeViewModel
import app.kodanotes.viewmodel.HomeViewModelFactory
import app.kodanotes.viewmodel.RecordViewModel
import app.kodanotes.viewmodel.RecordViewModelFactory
import app.kodanotes.viewmodel.SettingsViewModel
import app.kodanotes.viewmodel.SettingsViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KodaNotesTheme(dynamicColor = false) {
                KodaNotesApp()
            }
        }
    }
}

@Composable
fun KodaNotesApp() {
    var currentDestination  by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var previousDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    val context     = LocalContext.current
    val application = context.applicationContext as KodaNotesApplication

    val authManager = application.authManager
    val aiService   = remember { AiService(authManager) }

    val audioRecorder      = application.audioRecorder
    val whisperTranscriber = remember { WhisperTranscriber(context) }

    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(application.repository, authManager)
    )
    val settingsViewModel: SettingsViewModel = viewModel(
        factory = SettingsViewModelFactory(authManager, aiService)
    )
    val recordViewModel: RecordViewModel =
        viewModel(
            factory = RecordViewModelFactory(
                application.repository,
                audioRecorder,
                aiService,
                whisperTranscriber,
                authManager
            )
        )
    val flashcardsViewModel: FlashcardsViewModel = viewModel(
        factory = FlashcardsViewModelFactory(application.repository, application.estudioRepository, aiService)
    )
    val examenViewModel: ExamenViewModel = viewModel(
        factory = ExamenViewModelFactory(application.repository, application.estudioRepository, aiService)
    )

    val destinationOrder = listOf(
        AppDestinations.HOME,
        AppDestinations.FLASHCARDS,
        AppDestinations.RECORD,
        AppDestinations.EXAMEN,
        AppDestinations.SETTINGS
    )

    LaunchedEffect(currentDestination) {
        if (currentDestination == AppDestinations.HOME &&
            previousDestination == AppDestinations.SETTINGS) {
            homeViewModel.refrescarNombre()
        }
    }

    Scaffold(
        containerColor = BackgroundLight,
        bottomBar = {
            KodaBottomBar(
                currentDestination  = currentDestination,
                onDestinationChange = { newDest ->
                    previousDestination = currentDestination
                    currentDestination  = newDest
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            val currentIndex  = destinationOrder.indexOf(currentDestination)
            val previousIndex = destinationOrder.indexOf(previousDestination)
            val slideForward  = currentIndex > previousIndex

            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    when {
                        targetState == AppDestinations.RECORD -> {
                            (slideInVertically(tween(350), initialOffsetY = { it / 2 }) +
                                    fadeIn(tween(300))) togetherWith
                                    (slideOutVertically(tween(260), targetOffsetY = { -it / 8 }) +
                                            fadeOut(tween(200)))
                        }
                        initialState == AppDestinations.RECORD -> {
                            (fadeIn(tween(300)) + scaleIn(tween(300), initialScale = 0.95f)) togetherWith
                                    (slideOutVertically(tween(300), targetOffsetY = { it / 2 }) +
                                            fadeOut(tween(200)))
                        }
                        slideForward -> {
                            (slideInHorizontally(
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                                initialOffsetX = { it / 3 }
                            ) + fadeIn(tween(250))) togetherWith
                                    (slideOutHorizontally(tween(220), targetOffsetX = { -it / 5 }) +
                                            fadeOut(tween(180)))
                        }
                        else -> {
                            (slideInHorizontally(
                                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
                                initialOffsetX = { -it / 3 }
                            ) + fadeIn(tween(250))) togetherWith
                                    (slideOutHorizontally(tween(220), targetOffsetX = { it / 5 }) +
                                            fadeOut(tween(180)))
                        }
                    }
                },
                label = "screen_transition"
            ) { destination ->
                when (destination) {
                    AppDestinations.HOME       -> HomeScreen(viewModel = homeViewModel)
                    AppDestinations.FLASHCARDS -> FlashcardsScreen(viewModel = flashcardsViewModel)
                    AppDestinations.RECORD     -> RecordScreen(viewModel = recordViewModel)
                    AppDestinations.EXAMEN     -> ExamenScreen(viewModel = examenViewModel)
                    AppDestinations.SETTINGS   -> SettingsScreen(viewModel = settingsViewModel)
                }
            }
        }
    }
}

@Composable
fun KodaBottomBar(
    currentDestination: AppDestinations,
    onDestinationChange: (AppDestinations) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation    = 20.dp,
                    shape        = RoundedCornerShape(36.dp),
                    spotColor    = Color(0x60000000),
                    ambientColor = Color(0x30000000)
                )
                .clip(RoundedCornerShape(36.dp))
                .background(DarkContainer)
                .height(68.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                NavTabItem(
                    icon     = Icons.Default.Folder,
                    label    = "CURSOS",
                    selected = currentDestination == AppDestinations.HOME,
                    onClick  = { onDestinationChange(AppDestinations.HOME) },
                    modifier = Modifier.weight(1f)
                )
                NavTabItem(
                    icon     = Icons.Default.Style,
                    label    = "CARDS",
                    selected = currentDestination == AppDestinations.FLASHCARDS,
                    onClick  = { onDestinationChange(AppDestinations.FLASHCARDS) },
                    modifier = Modifier.weight(1f)
                )

                val fabInteractionSource = remember { MutableInteractionSource() }
                val isFabPressed by fabInteractionSource.collectIsPressedAsState()

                val fabScale by animateFloatAsState(
                    targetValue    = if (isFabPressed) 0.88f else 1f,
                    animationSpec  = spring(stiffness = Spring.StiffnessHigh),
                    label          = "fab_scale"
                )
                val isRecord = currentDestination == AppDestinations.RECORD

                Box(
                    modifier = Modifier
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .scale(fabScale)
                            .clip(CircleShape)
                            .background(if (isRecord) Color(0xFFEF4444) else Color(0xFF1C1D22))
                            .border(1.5.dp, if (isRecord) Color(0xFFEF4444) else TealAccent.copy(0.6f), CircleShape)
                            .clickable(
                                interactionSource = fabInteractionSource,
                                indication        = null
                            ) {
                                onDestinationChange(
                                    if (isRecord) AppDestinations.HOME else AppDestinations.RECORD
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Grabación",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                NavTabItem(
                    icon     = Icons.Default.Quiz,
                    label    = "EXAMEN",
                    selected = currentDestination == AppDestinations.EXAMEN,
                    onClick  = { onDestinationChange(AppDestinations.EXAMEN) },
                    modifier = Modifier.weight(1f)
                )
                NavTabItem(
                    icon     = Icons.Default.Settings,
                    label    = "AJUSTES",
                    selected = currentDestination == AppDestinations.SETTINGS,
                    onClick  = { onDestinationChange(AppDestinations.SETTINGS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun NavTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "tab_scale"
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            )
            .scale(scale)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = if (selected) Color.White else TextMuted,
            modifier           = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text          = label,
            fontSize      = 9.sp,
            fontWeight    = if (selected) FontWeight.Bold else FontWeight.Medium,
            color         = if (selected) Color.White else TextMuted,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        if (selected) {
            Box(
                modifier = Modifier
                    .size(3.dp)
                    .background(Color.White, CircleShape)
            )
        } else {
            Spacer(modifier = Modifier.height(3.dp))
        }
    }
}

enum class AppDestinations {
    HOME,
    FLASHCARDS,
    RECORD,
    EXAMEN,
    SETTINGS
}