package app.kodanotes.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kodanotes.data.local.entity.ExamenEntity
import app.kodanotes.ui.theme.BackgroundLight
import app.kodanotes.ui.theme.CardWhite
import app.kodanotes.ui.theme.PurpleMain
import app.kodanotes.ui.theme.TextDark
import app.kodanotes.viewmodel.ExamenState
import app.kodanotes.viewmodel.ExamenViewModel
import app.kodanotes.viewmodel.PreguntaActiva
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val VerdeExamen  = Color(0xFF34C759)
private val RojoExamen   = Color(0xFFFF3B30)
private val AmbarExamen  = Color(0xFFFF9500)

@Composable
fun ExamenScreen(viewModel: ExamenViewModel) {
    val state                by viewModel.state.collectAsState()
    val materias             by viewModel.materias.collectAsState()
    val materiaSeleccionada  by viewModel.materiaSeleccionada.collectAsState()
    val notas                by viewModel.notasDeMateriaSeleccionada.collectAsState()
    val notasSeleccionadas   by viewModel.notasSeleccionadas.collectAsState()
    val cantidad             by viewModel.cantidadPreguntas.collectAsState()
    val dificultad           by viewModel.dificultad.collectAsState()
    val preguntas            by viewModel.preguntas.collectAsState()
    val indice               by viewModel.indiceActual.collectAsState()
    val examenGuardado       by viewModel.examenGuardado.collectAsState()
    val historial            by viewModel.historial.collectAsState()
    val mejorNota            by viewModel.mejorNota.collectAsState()
    val promedioNota         by viewModel.promedioNota.collectAsState()
    val preguntasRepaso      by viewModel.preguntasRepaso.collectAsState()
    val indiceRepaso         by viewModel.indiceRepaso.collectAsState()
    val errorMsg             by viewModel.errorMsg.collectAsState()

    AnimatedContent(
        targetState = state,
        transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(200)) },
        label = "examen_state"
    ) { estadoActual ->
        when (estadoActual) {

            // ── Configuración ─────────────────────────────────────────────────
            ExamenState.IDLE, ExamenState.ERROR -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight)
                        .padding(top = 56.dp, start = 24.dp, end = 24.dp, bottom = 100.dp)
                ) {
                    item {
                        Text("Modo examen", fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDark, letterSpacing = (-0.5).sp)
                        Text("Pon a prueba tu conocimiento",
                            fontSize = 14.sp, color = Color(0xFF9E9EA7),
                            fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(24.dp))

                        AnimatedVisibility(estadoActual == ExamenState.ERROR) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFFF0EF))
                                        .padding(12.dp)
                                ) {
                                    Text("⚠ ${errorMsg ?: "Error desconocido"}",
                                        fontSize = 13.sp, color = Color(0xFFCC1A12))
                                }
                                Spacer(Modifier.height(12.dp))
                            }
                        }

                        Text("Materia", fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold, color = Color(0xFF9E9EA7))
                        Spacer(Modifier.height(8.dp))
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            materias.forEach { materia ->
                                val sel = materiaSeleccionada?.id == materia.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(
                                            if (sel) Color(materia.colorHex).copy(0.15f)
                                            else Color(0xFFF0F0F5)
                                        )
                                        .clickable { viewModel.seleccionarMateria(materia) }
                                        .padding(horizontal = 14.dp, vertical = 7.dp)
                                ) {
                                    Text("${materia.simbolo} ${materia.nombre}",
                                        fontSize = 13.sp,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (sel) Color(materia.colorHex)
                                        else Color(0xFF9E9EA7))
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    if (notas.isNotEmpty()) {
                        item {
                            Text("Selecciona notas", fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold, color = Color(0xFF9E9EA7))
                            Spacer(Modifier.height(8.dp))
                        }
                        items(notas) { nota ->
                            val sel = nota.id in notasSeleccionadas
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .shadow(if (sel) 4.dp else 0.dp,
                                        RoundedCornerShape(12.dp),
                                        spotColor = PurpleMain.copy(0.1f))
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardWhite)
                                    .clickable { viewModel.toggleNota(nota.id) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (sel) PurpleMain else Color(0xFFEEEEF4))
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (sel) Icon(Icons.Default.Check, null,
                                        tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(nota.titulo, fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium, color = TextDark,
                                    maxLines = 1, modifier = Modifier.weight(1f))
                            }
                        }
                        item { Spacer(Modifier.height(16.dp)) }
                    }

                    if (materiaSeleccionada != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = CardWhite)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    // Cantidad de preguntas
                                    Text("Número de preguntas", fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold, color = TextDark)
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ExamenStepperBtn("-") {
                                            viewModel.setCantidadPreguntas(cantidad - 1)
                                        }
                                        Spacer(Modifier.width(24.dp))
                                        Text("$cantidad", fontSize = 28.sp,
                                            fontWeight = FontWeight.ExtraBold, color = PurpleMain)
                                        Spacer(Modifier.width(24.dp))
                                        ExamenStepperBtn("+") {
                                            viewModel.setCantidadPreguntas(cantidad + 1)
                                        }
                                    }
                                    Text("Mínimo 5 — máximo 20", fontSize = 11.sp,
                                        color = Color(0xFFB0B0BA),
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center)

                                    Spacer(Modifier.height(20.dp))

                                    // Dificultad
                                    Text("Dificultad", fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold, color = TextDark)
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            "Fácil"   to VerdeExamen,
                                            "Media"   to AmbarExamen,
                                            "Difícil" to RojoExamen
                                        ).forEach { (nivel, color) ->
                                            val activo = dificultad == nivel
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(40.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (activo) color.copy(0.12f)
                                                        else Color(0xFFF0F0F5)
                                                    )
                                                    .clickable { viewModel.setDificultad(nivel) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(nivel, fontSize = 13.sp,
                                                    fontWeight = if (activo) FontWeight.Bold
                                                    else FontWeight.Normal,
                                                    color = if (activo) color
                                                    else Color(0xFF9E9EA7))
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.generarExamen() },
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
                                        "Generar examen · $cantidad preguntas · $dificultad",
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Historial
                    if (historial.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(28.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Historial", fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold, color = TextDark)
                                Column(horizontalAlignment = Alignment.End) {
                                    mejorNota?.let {
                                        Text("Mejor: ${"%.1f".format(it)}",
                                            fontSize = 12.sp, color = VerdeExamen,
                                            fontWeight = FontWeight.SemiBold)
                                    }
                                    promedioNota?.let {
                                        Text("Promedio: ${"%.1f".format(it)}",
                                            fontSize = 12.sp, color = Color(0xFF9E9EA7))
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                        items(historial) { examen ->
                            ExamenHistorialItem(
                                examen = examen,
                                onBorrar = { viewModel.borrarExamen(examen.id) }
                            )
                        }
                    }
                }
            }

            // ── Generando ─────────────────────────────────────────────────────
            ExamenState.GENERANDO -> {
                Box(Modifier.fillMaxSize().background(BackgroundLight),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = PurpleMain,
                            modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(20.dp))
                        Text("Generando examen con IA...",
                            fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TextDark)
                        Text("Esto puede tardar unos segundos",
                            fontSize = 13.sp, color = Color(0xFF9E9EA7))
                    }
                }
            }

            // ── En examen ─────────────────────────────────────────────────────
            ExamenState.EN_EXAMEN -> {
                val pregunta = preguntas.getOrNull(indice)
                if (pregunta == null) return@AnimatedContent

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight)
                        .padding(top = 56.dp, start = 24.dp, end = 24.dp, bottom = 100.dp)
                ) {
                    // Progreso (Fijo arriba)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Pregunta ${indice + 1} de ${preguntas.size}",
                            fontSize = 13.sp, color = Color(0xFF9E9EA7))
                        Text(viewModel.dificultad.collectAsState().value,
                            fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                            color = when (dificultad) {
                                "Fácil"   -> VerdeExamen
                                "Difícil" -> RojoExamen
                                else      -> AmbarExamen
                            })
                    }
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFEEEEF4))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((indice + 1).toFloat() / preguntas.size.toFloat())
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(PurpleMain)
                        )
                    }
                    Spacer(Modifier.height(20.dp))

                    // FIX: Contenedor con Scroll para la pregunta y opciones
                    Column(
                        modifier = Modifier
                            .weight(1f) // Ocupa el espacio sobrante
                            .verticalScroll(rememberScrollState()) // Permite deslizar
                    ) {
                        // Pregunta
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = CardWhite)
                        ) {
                            Text(
                                pregunta.pregunta,
                                modifier = Modifier.padding(20.dp),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextDark,
                                lineHeight = 24.sp
                            )
                        }
                        Spacer(Modifier.height(16.dp))

                        // Opciones
                        pregunta.opciones.forEach { opcion ->
                            val letra = opcion.take(1)
                            val seleccionada = pregunta.seleccionada == letra
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .shadow(if (seleccionada) 4.dp else 0.dp,
                                        RoundedCornerShape(14.dp),
                                        spotColor = PurpleMain.copy(0.2f))
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (seleccionada) Color(0xFFEEEDFE) else CardWhite
                                    )
                                    .clickable { viewModel.responder(letra) }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (seleccionada) PurpleMain
                                            else Color(0xFFF0F0F5)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(letra, fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (seleccionada) Color.White
                                        else Color(0xFF9E9EA7))
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(opcion.drop(3), fontSize = 14.sp,
                                    color = if (seleccionada) PurpleMain else TextDark,
                                    fontWeight = if (seleccionada) FontWeight.Medium
                                    else FontWeight.Normal,
                                    modifier = Modifier.weight(1f))
                            }
                        }
                        // Pequeño espacio extra al final del scroll para que no quede pegado a los botones
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    // Navegación (Fijo abajo)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (indice > 0) {
                            Button(
                                onClick = { viewModel.anteriorPregunta() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF0F0F5)
                                )
                            ) {
                                Text("← Anterior", color = TextDark,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (indice < preguntas.size - 1) {
                            Button(
                                onClick = { viewModel.siguientePregunta() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PurpleMain)
                            ) {
                                Text("Siguiente →", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.terminarExamen() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = VerdeExamen
                                )
                            ) {
                                Text("Terminar examen", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // ── Resultado ─────────────────────────────────────────────────────
            ExamenState.RESULTADO -> {
                val examen = examenGuardado ?: return@AnimatedContent
                val aprobado = examen.nota >= 6f

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight)
                        .padding(top = 56.dp, start = 24.dp, end = 24.dp, bottom = 100.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        Icon(
                            if (aprobado) Icons.Default.CheckCircle
                            else Icons.AutoMirrored.Filled.ArrowBack,
                            null,
                            tint = if (aprobado) VerdeExamen else RojoExamen,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "${"%.1f".format(examen.nota)} / 10",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (aprobado) VerdeExamen else RojoExamen
                        )
                        Text(
                            if (aprobado) "¡Aprobado!" else "Reprobado",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (aprobado) VerdeExamen else RojoExamen
                        )
                        Text(
                            "${examen.dificultad} · ${examen.totalPreguntas} preguntas",
                            fontSize = 13.sp, color = Color(0xFF9E9EA7)
                        )
                        Spacer(Modifier.height(24.dp))

                        // Stats
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ResultadoChip("${examen.correctas}", "Correctas",
                                VerdeExamen, Modifier.weight(1f))
                            ResultadoChip("${examen.incorrectas}", "Incorrectas",
                                RojoExamen, Modifier.weight(1f))
                            ResultadoChip("${examen.omitidas}", "Omitidas",
                                Color(0xFF9E9EA7), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(24.dp))

                        if (preguntasRepaso.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.iniciarRepaso() },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFF0EF)
                                )
                            ) {
                                Text(
                                    "Repasar ${preguntasRepaso.size} respuesta(s) incorrecta(s)",
                                    fontWeight = FontWeight.Bold,
                                    color = RojoExamen, fontSize = 14.sp
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                        }

                        Button(
                            onClick = { viewModel.volverAConfigurar() },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PurpleMain)
                        ) {
                            Text("Nuevo examen", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Guardado en historial",
                            fontSize = 12.sp, color = Color(0xFFB0B0BA))
                    }
                }
            }

            // ── Repaso de errores ─────────────────────────────────────────────
            ExamenState.REPASO -> {
                val pregunta = preguntasRepaso.getOrNull(indiceRepaso)
                if (pregunta == null) return@AnimatedContent

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight)
                        .padding(top = 56.dp, start = 24.dp, end = 24.dp, bottom = 100.dp)
                ) {
                    // Header (Fijo arriba)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, null,
                            tint = Color(0xFF9E9EA7),
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { viewModel.volverDeRepaso() }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Repaso · ${indiceRepaso + 1} / ${preguntasRepaso.size}",
                            fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
                    }
                    Spacer(Modifier.height(20.dp))

                    // FIX: Contenedor con Scroll para la tarjeta de repaso
                    Column(
                        modifier = Modifier
                            .weight(1f) // Ocupa el espacio sobrante
                            .verticalScroll(rememberScrollState()) // Permite deslizar
                    ) {
                        RepasoPreguntaCard(pregunta)
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    // Navegación (Fijo abajo)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (indiceRepaso > 0) {
                            Button(
                                onClick = { viewModel.anteriorRepaso() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFF0F0F5)
                                )
                            ) {
                                Text("← Anterior", color = TextDark,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (indiceRepaso < preguntasRepaso.size - 1) {
                            Button(
                                onClick = { viewModel.siguienteRepaso() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PurpleMain)
                            ) {
                                Text("Siguiente →", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.volverDeRepaso() },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VerdeExamen)
                            ) {
                                Text("Finalizar repaso", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Componentes privados ──────────────────────────────────────────────────────

@Composable
private fun ExamenStepperBtn(label: String, onClick: () -> Unit) {
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
private fun ResultadoChip(valor: String, label: String, color: Color, modifier: Modifier) {
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

@Composable
private fun ExamenHistorialItem(examen: ExamenEntity, onBorrar: () -> Unit) {
    val fecha = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        .format(Date(examen.fecha))
    val aprobado = examen.nota >= 6f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(CardWhite)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(fecha, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
            Text("${examen.totalPreguntas} preguntas · ${examen.dificultad}",
                fontSize = 12.sp, color = Color(0xFF9E9EA7))
        }
        Text(
            "${"%.1f".format(examen.nota)}",
            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
            color = if (aprobado) VerdeExamen else RojoExamen
        )
        Spacer(Modifier.width(12.dp))
        TextButton(onClick = onBorrar) {
            Text("Borrar", fontSize = 12.sp, color = Color(0xFFB0B0BA))
        }
    }
}

@Composable
private fun RepasoPreguntaCard(pregunta: PreguntaActiva) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(pregunta.pregunta, fontSize = 15.sp,
                fontWeight = FontWeight.Medium, color = TextDark, lineHeight = 22.sp)
            Spacer(Modifier.height(16.dp))

            pregunta.opciones.forEach { opcion ->
                val letra = opcion.take(1)
                val esCorrecta   = letra == pregunta.respuestaCorrecta
                val esSeleccionada = letra == pregunta.seleccionada

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            when {
                                esCorrecta   -> Color(0xFFEAFAEE)
                                esSeleccionada -> Color(0xFFFFF0EF)
                                else         -> Color(0xFFF8F8FC)
                            }
                        )
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    esCorrecta   -> VerdeExamen
                                    esSeleccionada -> RojoExamen
                                    else         -> Color(0xFFEEEEF4)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(letra, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                            color = if (esCorrecta || esSeleccionada) Color.White
                            else Color(0xFF9E9EA7))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(opcion.drop(3), fontSize = 13.sp,
                        color = when {
                            esCorrecta   -> VerdeExamen
                            esSeleccionada -> RojoExamen
                            else         -> Color(0xFF9E9EA7)
                        },
                        fontWeight = if (esCorrecta || esSeleccionada) FontWeight.SemiBold
                        else FontWeight.Normal)
                }
            }

            Spacer(Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFEEEDFE))
                    .padding(12.dp)
            ) {
                Text("💡 ${pregunta.explicacion}", fontSize = 13.sp,
                    color = PurpleMain, lineHeight = 20.sp)
            }
        }
    }
}

// Needed for CircleShape reference
private val CircleShape = RoundedCornerShape(50)