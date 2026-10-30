package app.kodanotes.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kodanotes.ui.theme.BackgroundLight
import app.kodanotes.ui.theme.CardWhite
import app.kodanotes.ui.theme.PurpleMain
import app.kodanotes.ui.theme.TextDark
import app.kodanotes.viewmodel.FlashcardsState
import app.kodanotes.viewmodel.FlashcardsViewModel

private val VerdeCard  = Color(0xFF34C759)
private val RojoCard   = Color(0xFFFF3B30)
private val GrisCard   = Color(0xFF8E8E93)

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
                // ── Pantalla de configuración ─────────────────────────────────
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight)
                        .padding(top = 56.dp, start = 24.dp, end = 24.dp, bottom = 100.dp)
                ) {
                    item {
                        Text("Flashcards", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
                            color = TextDark, letterSpacing = (-0.5).sp)
                        Text("Selecciona notas para estudiar", fontSize = 14.sp,
                            color = Color(0xFF9E9EA7), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(24.dp))

                        // Error
                        AnimatedVisibility(estadoActual == FlashcardsState.ERROR) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFFFF0EF))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚠ ${errorMsg ?: "Error desconocido"}",
                                    fontSize = 13.sp, color = Color(0xFFCC1A12))
                            }
                            Spacer(Modifier.height(12.dp))
                        }

                        // Selector de materia
                        Text("Materia", fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF9E9EA7))
                        Spacer(Modifier.height(8.dp))
                    }

                    // Chips de materias
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
                                        .background(
                                            if (seleccionada) Color(materia.colorHex).copy(0.15f)
                                            else Color(0xFFF0F0F5)
                                        )
                                        .clickable { viewModel.seleccionarMateria(materia) }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        "${materia.simbolo} ${materia.nombre}",
                                        fontSize = 13.sp,
                                        fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Normal,
                                        color = if (seleccionada) Color(materia.colorHex) else Color(0xFF9E9EA7)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    // Notas de la materia seleccionada
                    if (notas.isNotEmpty()) {
                        item {
                            Text("Selecciona notas", fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = Color(0xFF9E9EA7))
                            Spacer(Modifier.height(8.dp))
                        }
                        items(notas) { nota ->
                            val seleccionada = nota.id in notasSeleccionadas
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .shadow(if (seleccionada) 4.dp else 0.dp,
                                        RoundedCornerShape(12.dp), spotColor = PurpleMain.copy(0.1f))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardWhite)
                                    .clickable { viewModel.toggleNota(nota.id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Checkbox
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (seleccionada) PurpleMain else Color(0xFFEEEEF4))
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (seleccionada) {
                                        Icon(Icons.Default.Check, null,
                                            tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(nota.titulo, fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium, color = TextDark,
                                        maxLines = 1)
                                }
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }

                    // Configuración
                    if (materiaSeleccionada != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardWhite)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text("Cantidad de flashcards",
                                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                                        color = TextDark)
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        StepperButton("-") { viewModel.setCantidadCards(cantidad - 1) }
                                        Spacer(Modifier.width(24.dp))
                                        Text("$cantidad", fontSize = 28.sp,
                                            fontWeight = FontWeight.ExtraBold, color = PurpleMain)
                                        Spacer(Modifier.width(24.dp))
                                        StepperButton("+") { viewModel.setCantidadCards(cantidad + 1) }
                                    }
                                    Text("Mínimo 5 — máximo 20",
                                        fontSize = 11.sp, color = Color(0xFFB0B0BA),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center)
                                }
                            }
                            Spacer(Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.generarFlashcards() },
                                enabled = notasSeleccionadas.isNotEmpty(),
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PurpleMain,
                                    disabledContainerColor = Color(0xFFCCCCD4)
                                )
                            ) {
                                Text(
                                    if (notasSeleccionadas.isEmpty())
                                        "Selecciona al menos una nota"
                                    else
                                        "Generar ${notasSeleccionadas.size} nota(s) → $cantidad cards",
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            FlashcardsState.GENERANDO -> {
                // ── Pantalla de carga ─────────────────────────────────────────
                Box(Modifier.fillMaxSize().background(BackgroundLight),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PurpleMain, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(20.dp))
                        Text("Generando flashcards con IA...",
                            fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextDark)
                        Text("Esto puede tardar unos segundos",
                            fontSize = 13.sp, color = Color(0xFF9E9EA7))
                    }
                }
            }

            FlashcardsState.SESION -> {
                // ── Sesión de estudio ─────────────────────────────────────────
                val colaRestante = total - contadorSe - contadorMasMenos
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight)
                        .padding(top = 56.dp, start = 24.dp, end = 24.dp, bottom = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Barra de progreso
                    val progreso = if (total > 0)
                        (contadorSe + contadorMasMenos).toFloat() / total.toFloat()
                    else 0f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFEEEEF4))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progreso)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(PurpleMain)
                        )
                    }
                    Spacer(Modifier.height(16.dp))

                    // Contadores
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ContadorChip(contadorSe, "Sé", VerdeCard)
                        ContadorChip(colaRestante, "Restantes", Color(0xFF9E9EA7))
                        ContadorChip(contadorNoSe, "Repasar", RojoCard)
                    }
                    Spacer(Modifier.height(24.dp))

                    // Card con flip
                    cardActual?.let { card ->
                        var volteada by remember(card.id) { mutableStateOf(false) }
                        val rotacion by animateFloatAsState(
                            targetValue = if (volteada) 180f else 0f,
                            animationSpec = tween(400),
                            label = "card_flip"
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .shadow(8.dp, RoundedCornerShape(24.dp),
                                    spotColor = PurpleMain.copy(0.15f))
                                .clip(RoundedCornerShape(24.dp))
                                .background(CardWhite)
                                .clickable { volteada = !volteada }
                                .graphicsLayer {
                                    rotationY = rotacion
                                    cameraDistance = 12f * density
                                }
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (rotacion <= 90f) {
                                // Frente
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFFEEEDFE))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Pregunta", fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold, color = PurpleMain)
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(card.pregunta, fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium, color = TextDark,
                                        textAlign = TextAlign.Center, lineHeight = 24.sp)
                                    Spacer(Modifier.height(12.dp))
                                    Text("Toca para ver la respuesta",
                                        fontSize = 12.sp, color = Color(0xFFB0B0BA))
                                }
                            } else {
                                // Reverso (compensar el espejo)
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(0xFFEAFAEE))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text("Respuesta", fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold, color = VerdeCard)
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Text(card.respuesta, fontSize = 15.sp,
                                        fontWeight = FontWeight.Normal, color = TextDark,
                                        textAlign = TextAlign.Center, lineHeight = 22.sp)
                                }
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Botones de respuesta
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            RespuestaBtn(
                                texto = "No lo sé",
                                color = RojoCard,
                                bgColor = Color(0xFFFFF0EF),
                                modifier = Modifier.weight(1f),
                                onClick = { volteada = false; viewModel.responder(0) }
                            )
                            RespuestaBtn(
                                texto = "Más o menos",
                                color = GrisCard,
                                bgColor = Color(0xFFF0F0F5),
                                modifier = Modifier.weight(1f),
                                onClick = { volteada = false; viewModel.responder(1) }
                            )
                            RespuestaBtn(
                                texto = "Lo sé",
                                color = VerdeCard,
                                bgColor = Color(0xFFEAFAEE),
                                modifier = Modifier.weight(1f),
                                onClick = { volteada = false; viewModel.responder(2) }
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        TextButton(
                            onClick = { viewModel.saltarCard() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Saltar →", color = Color(0xFFB0B0BA), fontSize = 13.sp)
                        }
                    }
                }
            }

            FlashcardsState.COMPLETADO -> {
                // ── Pantalla de completado ────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, null,
                            tint = VerdeCard, modifier = Modifier.size(72.dp))
                        Spacer(Modifier.height(20.dp))
                        Text("¡Sesión completada!", fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold, color = TextDark)
                        Spacer(Modifier.height(8.dp))
                        Text("Has repasado todas las flashcards",
                            fontSize = 14.sp, color = Color(0xFF9E9EA7))
                        Spacer(Modifier.height(32.dp))

                        // Resumen
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ResumenChip("$contadorSe", "Lo sé", VerdeCard,
                                Modifier.weight(1f))
                            ResumenChip("$contadorMasMenos", "Más o menos", GrisCard,
                                Modifier.weight(1f))
                            ResumenChip("$contadorNoSe", "Repasar", RojoCard,
                                Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(32.dp))

                        Button(
                            onClick = { viewModel.reiniciarSesion() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleMain)
                        ) {
                            Text("Repetir sesión", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        TextButton(
                            onClick = { viewModel.volverAConfigurar() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Nueva sesión", color = Color(0xFF9E9EA7), fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// ── Componentes privados ──────────────────────────────────────────────────────

@Composable
private fun StepperButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF0F0F5))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 22.sp, fontWeight = FontWeight.Medium, color = TextDark)
    }
}

@Composable
private fun ContadorChip(valor: Int, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$valor", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 11.sp, color = Color(0xFF9E9EA7), fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RespuestaBtn(
    texto: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(texto, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = color,
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun ResumenChip(valor: String, label: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CardWhite)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(valor, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 11.sp, color = Color(0xFF9E9EA7))
    }
}