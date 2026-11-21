package app.kodanotes.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kodanotes.ui.theme.BackgroundLight
import app.kodanotes.ui.theme.CardWhite
import app.kodanotes.ui.theme.DarkContainer
import app.kodanotes.ui.theme.PeachCard
import app.kodanotes.ui.theme.TealAccent
import app.kodanotes.ui.theme.TextDark
import app.kodanotes.ui.theme.TextMuted
import app.kodanotes.viewmodel.FlashcardsState
import app.kodanotes.viewmodel.FlashcardsViewModel

private val VerdeCard  = Color(0xFF10B981)
private val AmarilloCard = Color(0xFFF59E0B)
private val RojoCard   = Color(0xFFEF4444)

@Composable
fun FlashcardsScreen(viewModel: FlashcardsViewModel) {
    val state              by viewModel.state.collectAsState()
    val materias           by viewModel.materias.collectAsState()
    val materiaSeleccionada by viewModel.materiaSeleccionada.collectAsState()
    val notas              by viewModel.notasDeMateriaSeleccionada.collectAsState()
    val notasSeleccionadas by viewModel.notasSeleccionadas.collectAsState()
    val cantidad           by viewModel.cantidadCards.collectAsState()
    val cardActual         by viewModel.cardActual.collectAsState()
    val indice             by viewModel.indiceActual.collectAsState()
    val total              by viewModel.totalCards.collectAsState()
    val contadorSe         by viewModel.contadorSe.collectAsState()
    val contadorMasMenos   by viewModel.contadorMasMenos.collectAsState()
    val contadorNoSe       by viewModel.contadorNoSe.collectAsState()
    val errorMsg           by viewModel.errorMsg.collectAsState()

    AnimatedContent(
        targetState = state,
        transitionSpec = {
            fadeIn(tween(280)) togetherWith fadeOut(tween(200))
        },
        label = "flashcards_state"
    ) { estadoActual ->
        when (estadoActual) {

            FlashcardsState.IDLE, FlashcardsState.ERROR -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight)
                        .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 100.dp)
                ) {
                    item {
                        Text(
                            "Flashcards",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = TextDark,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            "Selecciona notas para practicar",
                            fontSize = 14.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(24.dp))

                        AnimatedVisibility(estadoActual == FlashcardsState.ERROR) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFFEE2E2))
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "⚠ ${errorMsg ?: "Error desconocido"}",
                                    fontSize = 13.sp,
                                    color = Color(0xFFDC2626)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        Text(
                            "MATERIA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            materias.forEach { materia ->
                                val seleccionada = materiaSeleccionada?.id == materia.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (seleccionada) DarkContainer else CardWhite)
                                        .border(1.dp, if (seleccionada) DarkContainer else Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
                                        .clickable { viewModel.seleccionarMateria(materia) }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        materia.nombre,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (seleccionada) Color.White else TextDark
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    if (materiaSeleccionada == null) {
                        item {
                            FlashcardSiluetaCard(mensaje = "Selecciona una materia arriba para preparar tus tarjetas de estudio con IA.")
                            Spacer(Modifier.height(20.dp))
                        }
                    } else if (notas.isEmpty()) {
                        item {
                            FlashcardSiluetaCard(mensaje = "Esta materia no tiene clases grabadas aún. Graba un audio para generar tarjetas.")
                            Spacer(Modifier.height(20.dp))
                        }
                    } else {
                        item {
                            Text(
                                "SELECCIONA LAS NOTAS BASE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        items(notas) { nota ->
                            val seleccionada = nota.id in notasSeleccionadas
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .shadow(
                                        if (seleccionada) 4.dp else 0.dp,
                                        RoundedCornerShape(14.dp),
                                        spotColor = Color(0x20000000)
                                    )
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CardWhite)
                                    .clickable { viewModel.toggleNota(nota.id) }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(22.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (seleccionada) DarkContainer else Color(0xFFE8EAF3))
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (seleccionada) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        nota.titulo,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextDark,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }

                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = CardWhite)
                            ) {
                                Column(Modifier.padding(18.dp)) {
                                    Text(
                                        "Cantidad de flashcards",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StepperButton("-") { viewModel.setCantidadCards(cantidad - 1) }
                                        Spacer(Modifier.width(24.dp))
                                        Text(
                                            "$cantidad",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextDark
                                        )
                                        Spacer(Modifier.width(24.dp))
                                        StepperButton("+") { viewModel.setCantidadCards(cantidad + 1) }
                                    }
                                    Text(
                                        "Mínimo 5 — máximo 20",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Spacer(Modifier.height(18.dp))

                            Button(
                                onClick = { viewModel.generarFlashcards() },
                                enabled = notasSeleccionadas.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkContainer,
                                    disabledContainerColor = Color(0xFFD1D5DB)
                                )
                            ) {
                                Text(
                                    if (notasSeleccionadas.isEmpty())
                                        "Selecciona al menos una nota"
                                    else
                                        "Generar ${notasSeleccionadas.size} nota(s) → $cantidad cards",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            FlashcardsState.GENERANDO -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(BackgroundLight),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = DarkContainer, modifier = Modifier.size(52.dp), strokeWidth = 4.dp)
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Generando flashcards con IA...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            "Sintetizando puntos clave",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            FlashcardsState.SESION -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight)
                        .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Flashcards",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = TextDark,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                "Selecciona notas para practicar",
                                fontSize = 14.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(DarkContainer)
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text("Todas", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        materias.take(3).forEach { m ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(CardWhite)
                                    .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(m.nombre, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDark)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .shadow(2.dp, RoundedCornerShape(24.dp), spotColor = Color(0x10000000))
                            .clip(RoundedCornerShape(24.dp))
                            .background(CardWhite)
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Dominio del mazo",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        val pct = if (total > 0) ((contadorSe.toFloat() / total) * 100).toInt() else 27
                        Text(
                            "$pct%  >>  ${100 - pct}%",
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = TealAccent
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    cardActual?.let { card ->
                        var volteada by remember(card.id) { mutableStateOf(false) }
                        val rotacion by animateFloatAsState(
                            targetValue = if (volteada) 180f else 0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "card_flip"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp)
                                .shadow(8.dp, RoundedCornerShape(28.dp), spotColor = Color(0x15000000))
                                .clip(RoundedCornerShape(28.dp))
                                .background(PeachCard)
                                .clickable { volteada = !volteada }
                                .graphicsLayer {
                                    rotationY = rotacion
                                    cameraDistance = 14f * density
                                }
                                .padding(20.dp)
                        ) {
                            if (rotacion <= 90f) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(CardWhite)
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                materiaSeleccionada?.nombre ?: "Asignatura",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextDark
                                            )
                                        }
                                        Text(
                                            "${indice + 1} / $total",
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                    }

                                    Spacer(Modifier.weight(1f))

                                    Text(
                                        card.pregunta,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextDark,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 24.sp,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                    )

                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "(Toca la tarjeta para ver la respuesta)",
                                        fontSize = 12.sp,
                                        color = Color(0xFF9E8B76),
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(Modifier.weight(1f))

                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(CircleShape)
                                                .background(DarkContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Refresh,
                                                contentDescription = "Voltear",
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(CardWhite)
                                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                "Respuesta",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = VerdeCard
                                            )
                                        }
                                        Text(
                                            "${indice + 1} / $total",
                                            fontSize = 13.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                    }

                                    Spacer(Modifier.weight(1f))

                                    Text(
                                        card.respuesta,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 23.sp,
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                    )

                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RespuestaPillBtn(
                                dotColor = RojoCard,
                                texto = "Difícil",
                                modifier = Modifier.weight(1f),
                                onClick = { volteada = false; viewModel.responder(0) }
                            )
                            RespuestaPillBtn(
                                dotColor = AmarilloCard,
                                texto = "Bien",
                                modifier = Modifier.weight(1f),
                                onClick = { volteada = false; viewModel.responder(1) }
                            )
                            RespuestaPillBtn(
                                dotColor = VerdeCard,
                                texto = "Fácil",
                                modifier = Modifier.weight(1f),
                                onClick = { volteada = false; viewModel.responder(2) }
                            )
                        }
                    }
                }
            }

            FlashcardsState.COMPLETADO -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE6F7F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                null,
                                tint = VerdeCard,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "¡Sesión completada!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDark
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Has repasado todas las tarjetas",
                            fontSize = 14.sp,
                            color = TextMuted
                        )
                        Spacer(Modifier.height(30.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ResumenChip("$contadorSe", "Lo sé", VerdeCard, Modifier.weight(1f))
                            ResumenChip("$contadorMasMenos", "Más o menos", AmarilloCard, Modifier.weight(1f))
                            ResumenChip("$contadorNoSe", "Repasar", RojoCard, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(32.dp))

                        Button(
                            onClick = { viewModel.reiniciarSesion() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkContainer)
                        ) {
                            Text("Repetir sesión", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        TextButton(
                            onClick = { viewModel.volverAConfigurar() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Nueva sesión", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlashcardSiluetaCard(mensaje: String = "Selecciona una materia para generar tus flashcards") {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(26.dp), spotColor = Color(0x12000000)),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = PeachCard),
        border = BorderStroke(1.dp, Color(0xFFFED7AA))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardWhite)
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Text(
                        "🎴 Vista previa de tarjeta",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                Text(
                    "1 / 10",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF9E8B76)
                )
            }

            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFFE5D5C5))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFFE5D5C5))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))

            Text(
                "(Toca la tarjeta para ver la respuesta)",
                fontSize = 11.sp,
                color = Color(0xFFB09C88),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardWhite)
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💡 ", fontSize = 14.sp)
                    Text(
                        mensaje,
                        fontSize = 12.sp,
                        color = TextDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF1F3FB))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextDark)
    }
}

@Composable
private fun RespuestaPillBtn(
    dotColor: Color,
    texto: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "btn_scale"
    )

    Box(
        modifier = modifier
            .height(48.dp)
            .scale(scale)
            .shadow(2.dp, RoundedCornerShape(24.dp), spotColor = Color(0x10000000))
            .clip(RoundedCornerShape(24.dp))
            .background(CardWhite)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).background(dotColor, CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                texto,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
    }
}

@Composable
private fun ResumenChip(valor: String, label: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x0C000000))
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(valor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Medium)
    }
}
