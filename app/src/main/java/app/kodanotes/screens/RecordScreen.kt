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
import app.kodanotes.ui.theme.PurpleMain
import app.kodanotes.ui.theme.TextDark
import app.kodanotes.viewmodel.RecordState
import app.kodanotes.viewmodel.RecordViewModel
import app.kodanotes.viewmodel.WhisperState
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val RecordRed     = Color(0xFFFF3B30)
private val SuccessGreen  = Color(0xFF34C759)
private val WarningOrange = Color(0xFFFF9500)

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

    // Lanzador de permisos actualizado con solicitud de batería
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
                // Si ya tiene micrófono, también aseguramos el permiso de batería
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
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Nueva Grabación", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                        color = TextDark, letterSpacing = (-0.5).sp)
                    Spacer(Modifier.height(4.dp))
                    Text("¿A qué asignatura pertenece esta clase?", fontSize = 14.sp,
                        color = Color(0xFF9E9EA7), fontStyle = FontStyle.Italic)
                }
                Box(modifier = Modifier.size(40.dp)
                    .shadow(3.dp, CircleShape, spotColor = Color(0x14000000))
                    .clip(CircleShape).background(CardWhite)
                    .clickable(remember { MutableInteractionSource() }, null) {},
                    contentAlignment = Alignment.Center) {
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
            initialValue = 1f, targetValue = 1.1f,
            animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulse"
        )
        val ringAlpha by infiniteTransition.animateFloat(
            initialValue = 0.45f, targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
            label = "ringAlpha"
        )
        val ringScale by infiniteTransition.animateFloat(
            initialValue = 1f, targetValue = 1.55f,
            animationSpec = infiniteRepeatable(tween(1100, easing = LinearEasing), RepeatMode.Restart),
            label = "ringScale"
        )
        val micEffectiveScale by animateFloatAsState(
            targetValue = if (grabando) pulseScale else 1f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
            label = "micScale"
        )

        val btnGradient = when {
            grabando  -> Brush.horizontalGradient(listOf(Color(0xFFFF6158), RecordRed))
            exitoso   -> Brush.horizontalGradient(listOf(SuccessGreen, Color(0xFF28A745)))
            sinApi    -> Brush.horizontalGradient(listOf(WarningOrange, Color(0xFFE67E22)))
            else      -> Brush.horizontalGradient(listOf(PurpleMain, Color(0xFF8B5CF6)))
        }

        Column(
            modifier = Modifier.fillMaxSize().background(BackgroundLight)
                .padding(top = 52.dp, start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                Box(
                    modifier = Modifier.size(44.dp)
                        .shadow(3.dp, CircleShape, spotColor = Color(0x10000000))
                        .clip(CircleShape).background(CardWhite)
                        .clickable(enabled = !enClase && !procesando) {
                            viewModel.seleccionarMateria(null, context)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, null,
                        tint = if (enClase || procesando) Color(0xFFD0D0D8) else Color(0xFF9E9EA7),
                        modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier.size(80.dp)
                    .shadow(16.dp, RoundedCornerShape(24.dp),
                        spotColor = Color(materiaActual.colorHex).copy(0.45f))
                    .background(Color(materiaActual.colorHex), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(materiaActual.simbolo, color = Color.White, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            Text(materiaActual.nombre, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                color = TextDark, letterSpacing = (-0.3).sp)

            Spacer(Modifier.height(14.dp))

            AnimatedVisibility(
                visible = whisperState == WhisperState.CARGANDO,
                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit  = scaleOut(spring()) + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF5F5FF))
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier  = Modifier.size(14.dp),
                        color     = PurpleMain,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "CARGANDO MODELO DE VOZ...",
                        color      = PurpleMain,
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            AnimatedVisibility(grabando, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut(spring()) + fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusBadge(color = Color(0xFFFFF0EF), dotColor = RecordRed, textColor = RecordRed,
                        text = "● GRABANDO AUDIO...")
                }
            }

            AnimatedVisibility(transcribiendo, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut(spring()) + fadeOut()) {
                val state = recordState as? RecordState.Transcribing
                val chunkInfo = if (state != null) " ${state.actual}/${state.total}" else ""

                Row(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFEEF6FF))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(14.dp), color = Color(0xFF2196F3), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("TRANSCRIBIENDO$chunkInfo...", color = Color(0xFF1565C0), fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }

            AnimatedVisibility(analizando, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut(spring()) + fadeOut()) {
                Row(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFF0EFFF))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(14.dp), color = PurpleMain, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("ANALIZANDO CON IA...", color = PurpleMain, fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }

            AnimatedVisibility(exitoso, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut(spring()) + fadeOut()) {
                Row(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFEAFAEE))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("¡CLASE GUARDADA!", color = SuccessGreen, fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }

            AnimatedVisibility(sinApi, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut(spring()) + fadeOut()) {
                Row(modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFFFF5E6))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, null, tint = WarningOrange, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("CONFIGURA TU API KEY", color = WarningOrange, fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
            }

            AnimatedVisibility(errorGen, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut(spring()) + fadeOut()) {
                val state = recordState as? RecordState.ErrorGeneral
                StatusBadge(color = Color(0xFFFFF0EF), dotColor = RecordRed, textColor = RecordRed,
                    text = state?.mensaje?.uppercase() ?: "ERROR AL PROCESAR")
            }

            Spacer(Modifier.weight(1f))

            Box(contentAlignment = Alignment.Center) {
                if (grabando) {
                    Box(modifier = Modifier.size(148.dp).scale(ringScale)
                        .clip(CircleShape).background(RecordRed.copy(alpha = ringAlpha)))
                }
                Box(
                    modifier = Modifier.size(148.dp).scale(micEffectiveScale)
                        .shadow(if (grabando) 24.dp else 10.dp, CircleShape,
                            spotColor   = RecordRed.copy(if (grabando) 0.5f else 0.25f),
                            ambientColor = RecordRed.copy(0.12f))
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                when {
                                    exitoso  -> listOf(SuccessGreen, Color(0xFF28A745))
                                    grabando -> listOf(Color(0xFFFF6158), RecordRed)
                                    else     -> listOf(RecordRed, Color(0xFFCC1A12))
                                }
                            )
                        )
                        .clickable(
                            enabled = !procesando && whisperListo,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { toggleGrabacion() },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        procesando -> CircularProgressIndicator(Modifier.size(48.dp), color = Color.White, strokeWidth = 4.dp)
                        exitoso    -> Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(60.dp))
                        grabando   -> Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(60.dp))
                        else       -> Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(60.dp))
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            AnimatedVisibility(
                visible = enClase,
                enter   = fadeIn(tween(300)),
                exit    = fadeOut(tween(200))
            ) {
                AudioWaveformBars(
                    audioLevel = audioLevel,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 32.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(
                    targetState = "%02d".format(minutos),
                    transitionSpec = { slideInVertically { -it } + fadeIn(tween(180)) togetherWith slideOutVertically { it } + fadeOut(tween(180)) },
                    label = "mm"
                ) { mm -> Text(mm, fontSize = 52.sp, fontWeight = FontWeight.Black,
                    color = if (grabando) RecordRed else TextDark, letterSpacing = 2.sp) }
                Text(":", fontSize = 52.sp, fontWeight = FontWeight.Black,
                    color = if (grabando) RecordRed.copy(0.6f) else Color(0xFFCCCCD4), letterSpacing = 2.sp)
                AnimatedContent(
                    targetState = "%02d".format(segundos),
                    transitionSpec = { slideInVertically { -it } + fadeIn(tween(180)) togetherWith slideOutVertically { it } + fadeOut(tween(180)) },
                    label = "ss"
                ) { ss -> Text(ss, fontSize = 52.sp, fontWeight = FontWeight.Black,
                    color = if (grabando) RecordRed else TextDark, letterSpacing = 2.sp) }
            }

            AnimatedVisibility(exitoso, enter = fadeIn(tween(300)), exit = fadeOut(tween(200))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(8.dp))
                    Text("✅ Análisis IA completado", color = SuccessGreen,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("\"$ultimaNota\"", color = Color(0xFF9E9EA7),
                        fontSize = 12.sp, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp))
                }
            }

            AnimatedVisibility(sinApi, enter = fadeIn(tween(300)), exit = fadeOut(tween(200))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(8.dp))
                    Text("Ve a Ajustes → Configuración IA", color = WarningOrange,
                        fontWeight = FontWeight.Medium, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
            }

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier.fillMaxWidth().height(60.dp)
                    .shadow(10.dp, RoundedCornerShape(30.dp),
                        spotColor = (if (grabando) RecordRed else PurpleMain).copy(0.4f))
                    .clip(RoundedCornerShape(30.dp))
                    .background(btnGradient)
                    .clickable(
                        enabled = !procesando && whisperListo,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { toggleGrabacion() },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    when {
                        transcribiendo -> {
                            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Transcribiendo...", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        analizando -> {
                            CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.5.dp)
                            Spacer(Modifier.width(10.dp))
                            Text("Analizando clase...", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        exitoso -> {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("¡Nota guardada!", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        sinApi -> {
                            Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Ir a Ajustes", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        grabando -> {
                            Icon(Icons.Default.Stop, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Detener Grabación", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        else -> {
                            Icon(Icons.Default.Mic, null, tint = Color.White, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Comenzar", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(color: Color, dotColor: Color, textColor: Color, text: String) {
    Row(modifier = Modifier.clip(RoundedCornerShape(50)).background(color)
        .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).background(dotColor, CircleShape))
        Spacer(Modifier.width(8.dp))
        Text(text, color = textColor, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
    }
}

@Composable
fun SubjectListItem(materia: MateriaEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color(0x0F000000))
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                .background(Color(materia.colorHex)), contentAlignment = Alignment.Center) {
                Text(materia.simbolo, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Text(materia.nombre, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                color = TextDark, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                tint = Color(0xFFDDDDE0), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
fun AudioWaveformBars(
    audioLevel: Float,
    modifier: Modifier = Modifier
) {
    val barCount = 28

    val currentLevel by rememberUpdatedState(audioLevel)

    val history = remember {
        mutableStateListOf<Float>().apply {
            repeat(barCount) { add(0f) }
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            if (history.isNotEmpty()) {
                history.removeAt(0)
            }
            history.add(currentLevel.coerceIn(0f, 1f))
            delay(50)
        }
    }

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val barWidth   = (size.width / barCount) * 0.55f
        val gap        = (size.width / barCount) * 0.45f
        val maxHeight  = size.height
        val centerY    = size.height / 2f

        history.forEachIndexed { i, level ->
            val barHeight  = (level * maxHeight * 1.5f).coerceIn(4f, maxHeight)
            val x          = i * (barWidth + gap) + gap / 2
            val alpha      = 0.3f + (level * 0.7f)

            drawRoundRect(
                color        = RecordRed.copy(alpha = alpha),
                topLeft      = Offset(x, centerY - barHeight / 2),
                size         = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2)
            )
        }
    }
}

/**
 * Solicita al usuario ignorar las optimizaciones de batería para que Android
 * no asesine el servicio de grabación (Deep Sleep) en teléfonos Xiaomi, Samsung, etc.
 */
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