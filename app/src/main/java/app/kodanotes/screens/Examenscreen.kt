package app.kodanotes.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kodanotes.data.local.entity.ExamenEntity
import app.kodanotes.ui.theme.BackgroundLight
import app.kodanotes.ui.theme.CardWhite
import app.kodanotes.ui.theme.DarkContainer
import app.kodanotes.ui.theme.LavenderBg
import app.kodanotes.ui.theme.TextDark
import app.kodanotes.ui.theme.TextMuted
import app.kodanotes.viewmodel.ExamenState
import app.kodanotes.viewmodel.ExamenViewModel
import app.kodanotes.viewmodel.PreguntaActiva
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val VerdeExamen  = Color(0xFF10B981)
private val RojoExamen   = Color(0xFFEF4444)
private val AmbarExamen  = Color(0xFFF59E0B)

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

            ExamenState.IDLE, ExamenState.ERROR -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight)
                        .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 100.dp)
                ) {
                    item {
                        Text(
                            "Modo Examen",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = TextDark,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            "Pon a prueba tu conocimiento",
                            fontSize = 14.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(24.dp))

                        AnimatedVisibility(estadoActual == ExamenState.ERROR) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFFEE2E2))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        "⚠ ${errorMsg ?: "Error desconocido"}",
                                        fontSize = 13.sp,
                                        color = Color(0xFFDC2626)
                                    )
                                }
                                Spacer(Modifier.height(12.dp))
                            }
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
                                val sel = materiaSeleccionada?.id == materia.id
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (sel) DarkContainer else CardWhite)
                                        .border(1.dp, if (sel) DarkContainer else Color(0xFFE5E7EB), RoundedCornerShape(20.dp))
                                        .clickable { viewModel.seleccionarMateria(materia) }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        materia.nombre,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sel) Color.White else TextDark
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    if (materiaSeleccionada == null) {
                        item {
                            ExamenSiluetaCard(mensaje = "Selecciona una materia arriba para preparar tu examen con IA.")
                            Spacer(Modifier.height(20.dp))
                        }
                    } else if (notas.isEmpty()) {
                        item {
                            ExamenSiluetaCard(mensaje = "Esta materia no tiene clases grabadas aún. Graba un audio para generar contenido de examen.")
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
                            val sel = nota.id in notasSeleccionadas
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .shadow(
                                        if (sel) 4.dp else 0.dp,
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
                                        .background(if (sel) DarkContainer else Color(0xFFE8EAF3))
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (sel) {
                                        Icon(
                                            Icons.Default.Check,
                                            null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    nota.titulo,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextDark,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
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
                                        "Número de preguntas",
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
                                        ExamenStepperBtn("-") { viewModel.setCantidadPreguntas(cantidad - 1) }
                                        Spacer(Modifier.width(24.dp))
                                        Text(
                                            "$cantidad",
                                            fontSize = 28.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TextDark
                                        )
                                        Spacer(Modifier.width(24.dp))
                                        ExamenStepperBtn("+") { viewModel.setCantidadPreguntas(cantidad + 1) }
                                    }
                                    Text(
                                        "Mínimo 5 — máximo 20",
                                        fontSize = 11.sp,
                                        color = TextMuted,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )

                                    Spacer(Modifier.height(20.dp))

                                    Text(
                                        "Dificultad",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
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
                                                    .height(42.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(
                                                        if (activo) color.copy(0.16f)
                                                        else Color(0xFFF1F3FB)
                                                    )
                                                    .clickable { viewModel.setDificultad(nivel) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    nivel,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (activo) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (activo) color else TextMuted
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(18.dp))

                            Button(
                                onClick = { viewModel.generarExamen() },
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
                                        "Generar examen · $cantidad preguntas · $dificultad",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    if (historial.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(28.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Historial",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextDark
                                )
                                Column(horizontalAlignment = Alignment.End) {
                                    mejorNota?.let {
                                        Text(
                                            "Mejor: ${"%.1f".format(it)}",
                                            fontSize = 12.sp,
                                            color = VerdeExamen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    promedioNota?.let {
                                        Text(
                                            "Promedio: ${"%.1f".format(it)}",
                                            fontSize = 12.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
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

            ExamenState.GENERANDO -> {
                Box(
                    modifier = Modifier.fillMaxSize().background(BackgroundLight),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = DarkContainer, modifier = Modifier.size(52.dp), strokeWidth = 4.dp)
                        Spacer(Modifier.height(20.dp))
                        Text(
                            "Generando examen con IA...",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            "Creando preguntas y respuestas",
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }
                }
            }

            ExamenState.EN_EXAMEN -> {
                val pregunta = preguntas.getOrNull(indice)
                if (pregunta == null) return@AnimatedContent

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight)
                        .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 100.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Modo Examen",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = TextDark,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                "Pon a prueba tu conocimiento",
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
                            Text("Todas las materias", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        materias.take(2).forEach { m ->
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

                    Spacer(Modifier.height(18.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = LavenderBg)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "PREGUNTA ${indice + 1}/${preguntas.size}",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDark
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(CardWhite)
                                            .padding(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            "Simulacro IA",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDark
                                        )
                                    }
                                }

                                Spacer(Modifier.height(14.dp))

                                Text(
                                    pregunta.pregunta,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextDark,
                                    lineHeight = 24.sp
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        pregunta.opciones.forEach { opcion ->
                            val letra = opcion.take(1)
                            val seleccionada = pregunta.seleccionada == letra

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp)
                                    .shadow(if (seleccionada) 4.dp else 1.dp, RoundedCornerShape(20.dp), spotColor = Color(0x10000000))
                                    .clickable { viewModel.responder(letra) },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (seleccionada) Color(0xFFF0EFFF) else CardWhite
                                ),
                                border = if (seleccionada) BorderStroke(1.5.dp, DarkContainer) else null
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        opcion.drop(3),
                                        fontSize = 13.sp,
                                        color = TextDark,
                                        fontWeight = if (seleccionada) FontWeight.Bold else FontWeight.Medium,
                                        lineHeight = 18.sp,
                                        modifier = Modifier.weight(1f)
                                    )

                                    Spacer(Modifier.width(12.dp))

                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (seleccionada) DarkContainer else Color(0xFFF1F3FB)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            letra,
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            color = if (seleccionada) Color.White else TextMuted
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (indice > 0) {
                            Button(
                                onClick = { viewModel.anteriorPregunta() },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5E7EB))
                            ) {
                                Text("← Anterior", color = TextDark, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (indice < preguntas.size - 1) {
                            Button(
                                onClick = { viewModel.siguientePregunta() },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkContainer)
                            ) {
                                Text("Siguiente →", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.terminarExamen() },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = VerdeExamen)
                            ) {
                                Text("Terminar examen", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

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
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(if (aprobado) Color(0xFFE6F7F0) else Color(0xFFFEE2E2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (aprobado) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.ArrowBack,
                                null,
                                tint = if (aprobado) VerdeExamen else RojoExamen,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "${"%.1f".format(examen.nota)} / 10",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (aprobado) VerdeExamen else RojoExamen
                        )
                        Text(
                            if (aprobado) "¡Excelente trabajo!" else "Necesita repasar",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (aprobado) VerdeExamen else RojoExamen
                        )
                        Text(
                            "${examen.dificultad} · ${examen.totalPreguntas} preguntas",
                            fontSize = 13.sp, color = TextMuted
                        )
                        Spacer(Modifier.height(26.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ResultadoChip("${examen.correctas}", "Correctas", VerdeExamen, Modifier.weight(1f))
                            ResultadoChip("${examen.incorrectas}", "Incorrectas", RojoExamen, Modifier.weight(1f))
                            ResultadoChip("${examen.omitidas}", "Omitidas", TextMuted, Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(28.dp))

                        if (preguntasRepaso.isNotEmpty()) {
                            Button(
                                onClick = { viewModel.iniciarRepaso() },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEE2E2))
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
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkContainer)
                        ) {
                            Text("Nuevo examen", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Guardado en historial",
                            fontSize = 12.sp, color = TextMuted
                        )
                    }
                }
            }

            ExamenState.REPASO -> {
                val pregunta = preguntasRepaso.getOrNull(indiceRepaso)
                if (pregunta == null) return@AnimatedContent

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(BackgroundLight)
                        .padding(top = 56.dp, start = 24.dp, end = 24.dp, bottom = 100.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF1F3FB))
                                .clickable { viewModel.volverDeRepaso() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack, null,
                                tint = TextDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Repaso · ${indiceRepaso + 1} / ${preguntasRepaso.size}",
                            fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextDark
                        )
                    }
                    Spacer(Modifier.height(20.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        RepasoPreguntaCard(pregunta)
                        Spacer(Modifier.height(8.dp))
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (indiceRepaso > 0) {
                            Button(
                                onClick = { viewModel.anteriorRepaso() },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5E7EB))
                            ) {
                                Text("← Anterior", color = TextDark, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (indiceRepaso < preguntasRepaso.size - 1) {
                            Button(
                                onClick = { viewModel.siguienteRepaso() },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkContainer)
                            ) {
                                Text("Siguiente →", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { viewModel.volverDeRepaso() },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(14.dp),
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

@Composable
fun ExamenSiluetaCard(mensaje: String = "Selecciona una materia para generar tu examen") {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = Color(0x10000000)),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = BorderStroke(1.dp, Color(0xFFE8EAF3))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE5E7EB))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "PREGUNTA 1 / 10",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9CA3AF)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF3F4F6))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("💡 Simulacro IA", fontSize = 11.sp, color = Color(0xFF9CA3AF), fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFFE5E7EB))
            )
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFFE5E7EB))
            )

            Spacer(Modifier.height(20.dp))

            listOf("A", "B", "C", "D").forEach { letra ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF9FAFB))
                        .border(1.dp, Color(0xFFF0F1F5), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5E7EB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(letra, fontSize = 11.sp, color = Color(0xFF9CA3AF), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(if (letra == "A") 0.7f else if (letra == "B") 0.5f else 0.8f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFE5E7EB))
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LavenderBg)
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
private fun ExamenStepperBtn(label: String, onClick: () -> Unit) {
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
private fun ResultadoChip(valor: String, label: String, color: Color, modifier: Modifier) {
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

@Composable
private fun ExamenHistorialItem(examen: ExamenEntity, onBorrar: () -> Unit) {
    val fecha = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(examen.fecha))
    val aprobado = examen.nota >= 6f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp), spotColor = Color(0x0C000000))
            .clip(RoundedCornerShape(16.dp))
            .background(CardWhite)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(fecha, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.height(2.dp))
            Text(
                "${examen.totalPreguntas} preguntas · ${examen.dificultad}",
                fontSize = 12.sp,
                color = TextMuted
            )
        }
        Text(
            "%.1f".format(examen.nota),
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (aprobado) VerdeExamen else RojoExamen
        )
        Spacer(Modifier.width(12.dp))
        TextButton(onClick = onBorrar) {
            Text("Borrar", fontSize = 12.sp, color = TextMuted)
        }
    }
}

@Composable
private fun RepasoPreguntaCard(pregunta: PreguntaActiva) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Column(Modifier.padding(22.dp)) {
            Text(
                pregunta.pregunta,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(18.dp))

            pregunta.opciones.forEach { opcion ->
                val letra = opcion.take(1)
                val esCorrecta   = letra == pregunta.respuestaCorrecta
                val esSeleccionada = letra == pregunta.seleccionada

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                esCorrecta   -> Color(0xFFE6F7F0)
                                esSeleccionada -> Color(0xFFFEE2E2)
                                else         -> Color(0xFFF1F3FB)
                            }
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    esCorrecta   -> VerdeExamen
                                    esSeleccionada -> RojoExamen
                                    else         -> Color(0xFFD1D5DB)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            letra,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (esCorrecta || esSeleccionada) Color.White else TextMuted
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        opcion.drop(3),
                        fontSize = 14.sp,
                        color = when {
                            esCorrecta   -> VerdeExamen
                            esSeleccionada -> RojoExamen
                            else         -> TextMuted
                        },
                        fontWeight = if (esCorrecta || esSeleccionada) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(LavenderBg)
                    .padding(14.dp)
            ) {
                Text(
                    "💡 ${pregunta.explicacion}",
                    fontSize = 13.sp,
                    color = TextDark,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
