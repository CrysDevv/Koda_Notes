package app.kodanotes.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import app.kodanotes.data.local.entity.MateriaEntity
import app.kodanotes.ui.theme.BackgroundLight
import app.kodanotes.ui.theme.CardWhite
import app.kodanotes.ui.theme.DarkContainer
import app.kodanotes.ui.theme.PurpleMain
import app.kodanotes.ui.theme.TextDark
import app.kodanotes.ui.theme.TextMuted
import app.kodanotes.viewmodel.RecordState
import app.kodanotes.viewmodel.RecordViewModel
import app.kodanotes.viewmodel.WhisperState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val RecordRed     = Color(0xFFEF4444)
private val SuccessGreen  = Color(0xFF10B981)
private val WarningOrange = Color(0xFFF59E0B)

@Composable
fun RecordScreen(viewModel: RecordViewModel) {
    val context       = LocalContext.current
    val materias      by viewModel.materias.collectAsState(initial = emptyList())
    val seleccionada  by viewModel.materiaSeleccionada.collectAsState()
    val recordState   by viewModel.recordState.collectAsState()
    val timerSeconds  by viewModel.timerSeconds.collectAsState()
    val ultimaNota    by viewModel.ultimaNotaGuardada.collectAsState()
    val audioLevel    by viewModel.audioLevel.collectAsState()

    val whisperState  by viewModel.whisperState.collectAsState()
    val whisperListo  = whisperState == WhisperState.LISTO

    val grabando       = recordState is RecordState.Recording
    val transcribiendo = recordState is RecordState.Transcribing
    val analizando     = recordState is RecordState.Analyzing
    val procesando     = transcribiendo || analizando
    val exitoso        = recordState is RecordState.Success
    val sinApi         = recordState is RecordState.ErrorNoApi
    val errorGen       = recordState is RecordState.ErrorGeneral

    val enClase        = grabando

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            solicitarIgnorarOptimizacionBateria(context)
            viewModel.gestionarGrabacion(context)
        } else {
            Toast.makeText(context, "Permiso de micrófono denegado", Toast.LENGTH_SHORT).show()
        }
    }

    val toggleGrabacion = {
        if (!whisperListo) {
            Toast.makeText(context, "Cargando modelo de voz, espera un momento...", Toast.LENGTH_SHORT).show()
        } else {
            val ok = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
            if (ok) {
                if (!grabando) solicitarIgnorarOptimizacionBateria(context)
                viewModel.gestionarGrabacion(context)
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    val minutos  = timerSeconds / 60
    val segundos = timerSeconds % 60

    var mostrarDialogoSalir by remember { mutableStateOf(false) }

    BackHandler(enabled = enClase || procesando) {
        if (enClase) mostrarDialogoSalir = true
    }

    if (mostrarDialogoSalir) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoSalir = false },
            containerColor   = CardWhite,
            shape            = RoundedCornerShape(24.dp),
            title = {
                Text("¿Detener la grabación?",
                    fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = TextDark)
            },
            text = {
                Text(
                    "Se perderá el segmento actual (${minutos}m ${segundos}s). " +
                            "Los segmentos ya guardados se conservan.",
                    color = Color(0xFF9E9EA7), fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        mostrarDialogoSalir = false
                        viewModel.gestionarGrabacion(context)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RecordRed),
                    shape  = RoundedCornerShape(12.dp)
                ) { Text("Detener", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoSalir = false }) {
                    Text("Seguir grabando", color = PurpleMain, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    AnimatedVisibility(
        visible = seleccionada == null,
        enter = fadeIn(tween(280)),
        exit  = fadeOut(tween(200))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().background(BackgroundLight)
                .padding(top = 56.dp, start = 24.dp, end = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Nueva Grabación",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "¿A qué asignatura pertenece esta clase?",
                        fontSize = 14.sp,
                        color = Color(0xFF9E9EA7),
                        fontStyle = FontStyle.Italic
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(4.dp, CircleShape, spotColor = Color(0x10000000))
                        .clip(CircleShape)
                        .background(CardWhite)
                        .clickable(remember { MutableInteractionSource() }, null) {},
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, tint = Color(0xFF9E9EA7), modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(28.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(materias) { m -> SubjectListItem(m) { viewModel.seleccionarMateria(m) } }
            }
        }
    }

    AnimatedVisibility(
        visible = seleccionada != null,
        enter = slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(380)),
        exit  = slideOutVertically(tween(260)) { it / 2 } + fadeOut(tween(260))
    ) {
        val materiaActual = seleccionada ?: return@AnimatedVisibility

        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 1.08f,
            animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse"
        )
        val ringAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f, targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
            label = "ringAlpha"
        )
        val ringScale by infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 1.5f,
            animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
            label = "ringScale"
        )
        val micEffectiveScale by animateFloatAsState(
            targetValue = if (grabando) pulseScale else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "micScale"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundLight)
                .padding(top = 52.dp, start = 24.dp, end = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .shadow(4.dp, CircleShape, spotColor = Color(0x10000000))
                        .clip(CircleShape)
                        .background(CardWhite)
                        .clickable(enabled = !enClase && !procesando) {
                            viewModel.seleccionarMateria(null, context)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = if (enClase || procesando) Color(0xFFD1D5DB) else Color(0xFF6E6E82),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    "Grabación de Clase",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(Modifier.width(42.dp))
            }

            Spacer(Modifier.height(20.dp))

            // Subject Hero Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = Color(0x0A000000)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(materiaActual.colorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(materiaActual.simbolo, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            materiaActual.nombre,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDark,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            when {
                                grabando       -> "Grabación en curso"
                                transcribiendo -> "Transcribiendo audio"
                                analizando     -> "Analizando con IA"
                                exitoso        -> "Clase procesada"
                                else           -> "Listo para grabar"
                            },
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Animated Status Badges
            AnimatedVisibility(
                visible = whisperState == WhisperState.CARGANDO,
                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit  = scaleOut(spring()) + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF0EFFF))
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier  = Modifier.size(13.dp),
                        color     = PurpleMain,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "CARGANDO MODELO DE VOZ...",
                        color      = PurpleMain,
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            AnimatedVisibility(grabando, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(), exit = scaleOut(spring()) + fadeOut()) {
                StatusBadge(color = Color(0xFFFEE2E2), dotColor = RecordRed, textColor = RecordRed, text = "GRABANDO AUDIO...")
            }

            AnimatedVisibility(transcribiendo, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(), exit = scaleOut(spring()) + fadeOut()) {
                val state = recordState as? RecordState.Transcribing
                val chunkInfo = if (state != null) " ${state.actual}/${state.total}" else ""

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFEFF6FF))
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(13.dp), color = Color(0xFF3B82F6), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("TRANSCRIBIENDO$chunkInfo...", color = Color(0xFF2563EB), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }
            }

            AnimatedVisibility(analizando, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(), exit = scaleOut(spring()) + fadeOut()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF0EFFF))
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(Modifier.size(13.dp), color = PurpleMain, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("ANALIZANDO CON IA...", color = PurpleMain, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }
            }

            AnimatedVisibility(exitoso, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(), exit = scaleOut(spring()) + fadeOut()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFE6F7F0))
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("¡CLASE GUARDADA!", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }
            }

            AnimatedVisibility(sinApi, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(), exit = scaleOut(spring()) + fadeOut()) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFFEF3C7))
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, null, tint = WarningOrange, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("CONFIGURA TU API KEY", color = WarningOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                }
            }

            AnimatedVisibility(errorGen, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(), exit = scaleOut(spring()) + fadeOut()) {
                val state = recordState as? RecordState.ErrorGeneral
                StatusBadge(color = Color(0xFFFEE2E2), dotColor = RecordRed, textColor = RecordRed, text = state?.mensaje?.uppercase() ?: "ERROR AL PROCESAR")
            }

            Spacer(Modifier.weight(1f))

            // Timer Display Area
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "TIEMPO TRANSCURRIDO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.2.sp
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (grabando) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(RecordRed)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    AnimatedContent(
                        targetState = "%02d".format(minutos),
                        transitionSpec = { slideInVertically { -it } + fadeIn(tween(180)) togetherWith slideOutVertically { it } + fadeOut(tween(180)) },
                        label = "mm"
                    ) { mm ->
                        Text(
                            mm,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        ":",
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (grabando) RecordRed else Color(0xFFC7C7CC),
                        letterSpacing = 1.sp
                    )
                    AnimatedContent(
                        targetState = "%02d".format(segundos),
                        transitionSpec = { slideInVertically { -it } + fadeIn(tween(180)) togetherWith slideOutVertically { it } + fadeOut(tween(180)) },
                        label = "ss"
                    ) { ss ->
                        Text(
                            ss,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Symmetrical Audio Waveform
            AudioWaveformBars(
                audioLevel  = audioLevel,
                isRecording = grabando,
                modifier    = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 24.dp)
            )

            AnimatedVisibility(exitoso, enter = fadeIn(tween(300)), exit = fadeOut(tween(200))) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x0A000000)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardWhite)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Análisis IA completado", color = SuccessGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "\"$ultimaNota\"",
                            color = TextDark,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            AnimatedVisibility(sinApi, enter = fadeIn(tween(300)), exit = fadeOut(tween(200))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Ve a Ajustes → Configuración IA para ingresar tu API Key", color = WarningOrange, fontWeight = FontWeight.Medium, fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.weight(1f))

            // Hero Control Button
            Box(contentAlignment = Alignment.Center) {
                if (grabando) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(ringScale)
                            .clip(CircleShape)
                            .background(RecordRed.copy(alpha = ringAlpha))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(116.dp)
                        .scale(micEffectiveScale)
                        .shadow(
                            if (grabando) 20.dp else 12.dp, CircleShape,
                            spotColor = if (grabando) RecordRed.copy(0.4f) else DarkContainer.copy(0.25f),
                            ambientColor = Color(0x1A000000)
                        )
                        .clip(CircleShape)
                        .background(
                            when {
                                exitoso  -> Brush.radialGradient(listOf(SuccessGreen, Color(0xFF059669)))
                                grabando -> Brush.radialGradient(listOf(Color(0xFFEF4444), Color(0xFFDC2626)))
                                sinApi   -> Brush.radialGradient(listOf(WarningOrange, Color(0xFFD97706)))
                                else     -> Brush.radialGradient(listOf(DarkContainer, Color(0xFF22242B)))
                            }
                        )
                        .clickable(
                            enabled = !procesando && whisperListo,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { toggleGrabacion() },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        procesando -> CircularProgressIndicator(Modifier.size(44.dp), color = Color.White, strokeWidth = 3.5.dp)
                        exitoso    -> Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(52.dp))
                        grabando   -> Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(52.dp))
                        else       -> Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(52.dp))
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // Action Label Pill Trigger
            Box(
                modifier = Modifier
                    .shadow(
                        6.dp,
                        RoundedCornerShape(24.dp),
                        spotColor = Color(0x10000000)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        when {
                            grabando -> RecordRed
                            exitoso  -> SuccessGreen
                            sinApi   -> WarningOrange
                            else     -> DarkContainer
                        }
                    )
                    .clickable(
                        enabled = !procesando && whisperListo,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { toggleGrabacion() }
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        transcribiendo -> {
                            Text("Transcribiendo...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        analizando -> {
                            Text("Analizando clase...", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        exitoso -> {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Clase guardada", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        sinApi -> {
                            Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Ir a Ajustes", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        grabando -> {
                            Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Detener Grabación", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        else -> {
                            Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Toca para Grabar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(color: Color, dotColor: Color, textColor: Color, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).background(dotColor, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
    }
}

@Composable
fun SubjectListItem(materia: MateriaEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color(0x0F000000))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(materia.colorHex)),
                contentAlignment = Alignment.Center
            ) {
                Text(materia.simbolo, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Text(
                materia.nombre,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFFD1D5DB),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun AudioWaveformBars(
    audioLevel: Float,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val barCount = 28
    val currentLevel by rememberUpdatedState(audioLevel)

    val history = remember {
        mutableStateListOf<Float>().apply {
            repeat(barCount) { add(0.08f) }
        }
    }

    LaunchedEffect(isRecording, currentLevel) {
        while (isActive) {
            if (history.isNotEmpty()) {
                history.removeAt(0)
            }
            val targetVal = if (isRecording) currentLevel.coerceIn(0.08f, 1f) else 0.08f
            history.add(targetVal)
            delay(45)
        }
    }

    val activeColor = Color(0xFFEF4444)
    val idleColor   = Color(0xFFE5E7EB)

    Canvas(modifier = modifier) {
        val barWidth  = (size.width / barCount) * 0.45f
        val gap       = (size.width / barCount) * 0.55f
        val maxHeight = size.height
        val centerY   = size.height / 2f

        history.forEachIndexed { i, level ->
            val barHeight = (level * maxHeight).coerceIn(4.dp.toPx(), maxHeight)
            val x         = i * (barWidth + gap) + gap / 2f
            val alpha     = if (isRecording) (0.35f + (level * 0.65f)) else 0.5f
            val color     = if (isRecording) activeColor else idleColor

            drawRoundRect(
                color        = color.copy(alpha = alpha),
                topLeft      = Offset(x, centerY - barHeight / 2f),
                size         = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
        }
    }
}

fun solicitarIgnorarOptimizacionBateria(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val packageName = context.packageName

        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
