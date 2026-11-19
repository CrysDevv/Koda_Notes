package app.kodanotes.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.kodanotes.data.local.entity.MateriaEntity
import app.kodanotes.data.local.entity.NotaEntity
import app.kodanotes.ui.theme.BackgroundLight
import app.kodanotes.ui.theme.CardWhite
import app.kodanotes.ui.theme.PurpleMain
import app.kodanotes.ui.theme.TextDark
import app.kodanotes.viewmodel.CourseDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val H1Color   = Color(0xFF1A1A2E)
private val H2Color   = Color(0xFF2D2D44)
private val H3Color   = Color(0xFF3D3D5C)
private val BodyColor = Color(0xFF444455)
private val BulletAccent = PurpleMain

@Composable
fun CourseDetailScreen(
    materia: MateriaEntity,
    viewModel: CourseDetailViewModel,
    onBackClick: () -> Unit
) {
    val notas by viewModel.notas.collectAsState(initial = emptyList())
    var claseSeleccionada by remember { mutableStateOf<NotaEntity?>(null) }
    var notaAEliminar     by remember { mutableStateOf<NotaEntity?>(null) }

    Scaffold(containerColor = BackgroundLight) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, spotColor = Color(0x10000000))
                    .background(CardWhite)
                    .padding(horizontal = 20.dp, vertical = 18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F3FB))
                            .clickable { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextDark,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.size(14.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(materia.colorHex)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(materia.simbolo, fontSize = 18.sp)
                    }
                    Spacer(Modifier.size(12.dp))
                    Column {
                        Text(
                            materia.nombre,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDark,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            "LISTADO DE CLASES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9E9EA7),
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            if (notas.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0xFFF0EFFF)),
                            contentAlignment = Alignment.Center
                        ) { Text("📂", fontSize = 36.sp) }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No hay notas para esta clase",
                            color = Color(0xFF6E6E82),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Graba una clase para empezar",
                            color = Color(0xFF9E9EA7),
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                Text(
                    "← Desliza para eliminar",
                    fontSize = 12.sp,
                    color = Color(0xFF9E9EA7),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    items(notas, key = { it.id }) { nota ->
                        ItemNotaSwipeable(
                            nota     = nota,
                            onClick  = { claseSeleccionada = nota },
                            onDelete = { notaAEliminar = nota }
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }

    if (notaAEliminar != null) {
        AlertDialog(
            onDismissRequest = { notaAEliminar = null },
            containerColor = CardWhite,
            shape = RoundedCornerShape(24.dp),
            title = { Text("Eliminar nota", fontWeight = FontWeight.Bold, color = TextDark) },
            text  = { Text("¿Eliminar \"${notaAEliminar!!.titulo}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.eliminarNota(notaAEliminar!!); notaAEliminar = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                    shape  = RoundedCornerShape(12.dp)
                ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { notaAEliminar = null }) {
                    Text("Cancelar", color = Color(0xFF9E9EA7))
                }
            }
        )
    }

    if (claseSeleccionada != null) {
        DialogLectorClase(nota = claseSeleccionada!!, onDismiss = { claseSeleccionada = null })
    }
}

@Composable
fun ItemNotaSwipeable(nota: NotaEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    val fechaFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(nota.fecha))
    var offsetX by remember { mutableFloatStateOf(0f) }
    val maxSwipe = -160f

    val bgColor by animateColorAsState(
        if (offsetX < -40f) Color(0xFFFFEEEE) else CardWhite,
        tween(200), label = "bg"
    )

    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bgColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(76.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(76.dp).clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFFF3B30), modifier = Modifier.size(24.dp))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color(0x12000000))
                .background(bgColor, RoundedCornerShape(20.dp))
                .clickable { onClick() }
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd    = { offsetX = if (offsetX < maxSwipe / 2) maxSwipe else 0f },
                        onDragCancel = { offsetX = 0f }
                    ) { _, drag -> offsetX = (offsetX + drag).coerceIn(maxSwipe, 0f) }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFF0EFFF)),
                contentAlignment = Alignment.Center
            ) {
                Text("📄", fontSize = 22.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(nota.titulo, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextDark)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(PurpleMain.copy(0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Análisis IA ✓", fontSize = 11.sp, color = PurpleMain, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(fechaFormat, fontSize = 11.sp, color = Color(0xFF9E9EA7))
                }
            }
        }
    }
}

@Composable
fun DialogLectorClase(nota: NotaEntity, onDismiss: () -> Unit) {
    val niveles = nota.contenido.split("|||").map { it.trim() }
    val bloques = listOf(
        niveles.getOrNull(0) ?: nota.contenido,
        niveles.getOrNull(1) ?: "No se generó resumen intermedio.",
        niveles.getOrNull(2) ?: "No hay análisis avanzado para esta clase."
    )
    var tabSeleccionado by remember { mutableStateOf(0) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = BackgroundLight)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardWhite)
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        nota.titulo,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark,
                        modifier = Modifier.weight(1f),
                        letterSpacing = (-0.3).sp
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BackgroundLight)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Close, null, tint = Color(0xFF9E9EA7), modifier = Modifier.size(18.dp))
                    }
                }

                TabRow(
                    selectedTabIndex = tabSeleccionado,
                    containerColor   = CardWhite,
                    contentColor     = PurpleMain,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[tabSeleccionado]),
                            height = 3.dp, color = PurpleMain
                        )
                    },
                    divider = {}
                ) {
                    listOf("💡 Síntesis", "📝 Guía", "🧠 Cheat Sheet").forEachIndexed { idx, label ->
                        Tab(
                            selected = tabSeleccionado == idx,
                            onClick  = { tabSeleccionado = idx },
                            text = {
                                Text(
                                    label,
                                    fontWeight = if (tabSeleccionado == idx) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (tabSeleccionado == idx) PurpleMain else Color(0xFF9E9EA7)
                                )
                            }
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    item {
                        MarkdownContent(texto = bloques[tabSeleccionado])
                    }
                }
            }
        }
    }
}

@Composable
fun MarkdownContent(texto: String) {
    val lineas = texto.lines()
    var i = 0

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        while (i < lineas.size) {
            val linea = lineas[i].trimEnd()

            when {
                linea.startsWith("# ") && !linea.startsWith("## ") -> {
                    val content = linea.removePrefix("# ").trim()
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text       = parsearInline(content),
                        fontSize   = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = H1Color,
                        lineHeight = 28.sp,
                        letterSpacing = (-0.3).sp
                    )
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(
                        color     = PurpleMain.copy(alpha = 0.15f),
                        thickness = 1.dp
                    )
                    Spacer(Modifier.height(8.dp))
                }

                linea.startsWith("## ") && !linea.startsWith("### ") -> {
                    val content = linea.removePrefix("## ").trim()
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text       = parsearInline(content),
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color      = H2Color,
                        lineHeight = 24.sp,
                        letterSpacing = (-0.2).sp
                    )
                    Spacer(Modifier.height(6.dp))
                    HorizontalDivider(
                        color     = Color(0xFFE8E8F0),
                        thickness = 1.dp
                    )
                    Spacer(Modifier.height(4.dp))
                }

                linea.startsWith("### ") -> {
                    val content = linea.removePrefix("### ").trim()
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(width = 3.dp, height = 18.dp)
                                .background(PurpleMain, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text       = parsearInline(content),
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color      = H3Color,
                            lineHeight = 22.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }

                (linea.startsWith("- ") || linea.startsWith("* ")) -> {
                    val content = linea.removePrefix("- ").removePrefix("* ").trim()
                    Row(
                        modifier = Modifier.padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .size(6.dp)
                                .background(BulletAccent.copy(0.6f), CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text       = parsearInline(content),
                            fontSize   = 15.sp,
                            lineHeight = 24.sp,
                            color      = BodyColor,
                            modifier   = Modifier.weight(1f)
                        )
                    }
                }

                linea.isBlank() -> {
                    Spacer(Modifier.height(8.dp))
                }

                else -> {
                    Text(
                        text       = parsearInline(linea),
                        fontSize   = 15.sp,
                        lineHeight = 26.sp,
                        color      = BodyColor
                    )
                }
            }
            i++
        }
    }
}

@Composable
private fun parsearInline(texto: String): AnnotatedString {
    return buildAnnotatedString {
        var idx = 0
        val s = texto
        while (idx < s.length) {
            when {
                s.startsWith("**", idx) -> {
                    val end = s.indexOf("**", idx + 2)
                    if (end != -1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextDark)) {
                            append(s.substring(idx + 2, end))
                        }
                        idx = end + 2
                    } else {
                        append(s[idx]); idx++
                    }
                }
                s.startsWith("*", idx) -> {
                    val end = s.indexOf("*", idx + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = BodyColor)) {
                            append(s.substring(idx + 1, end))
                        }
                        idx = end + 1
                    } else {
                        append(s[idx]); idx++
                    }
                }
                s.startsWith("`", idx) -> {
                    val end = s.indexOf("`", idx + 1)
                    if (end != -1) {
                        withStyle(SpanStyle(
                            fontWeight = FontWeight.Medium,
                            background = Color(0xFFF0EFFF),
                            color      = PurpleMain
                        )) {
                            append(" ${s.substring(idx + 1, end)} ")
                        }
                        idx = end + 1
                    } else {
                        append(s[idx]); idx++
                    }
                }
                else -> { append(s[idx]); idx++ }
            }
        }
    }
}