package app.kodanotes.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.kodanotes.KodaNotesApplication
import app.kodanotes.data.local.entity.MateriaEntity
import app.kodanotes.ui.theme.BackgroundLight
import app.kodanotes.ui.theme.CardWhite
import app.kodanotes.ui.theme.CoursePalette
import app.kodanotes.ui.theme.DarkCardBg
import app.kodanotes.ui.theme.DarkContainer
import app.kodanotes.ui.theme.MintBg
import app.kodanotes.ui.theme.PeachBg
import app.kodanotes.ui.theme.TealAccent
import app.kodanotes.ui.theme.TextDark
import app.kodanotes.ui.theme.TextMuted
import app.kodanotes.ui.theme.TextWhite
import app.kodanotes.viewmodel.CourseDetailViewModel
import app.kodanotes.viewmodel.CourseDetailViewModelFactory
import app.kodanotes.viewmodel.HomeViewModel
import kotlin.math.roundToInt

private val COLORES_MATERIAS = listOf(
    0xFF4A90E2L, 0xFF9B59B6L, 0xFFE67E22L, 0xFF27AE60L,
    0xFFE74C3CL, 0xFF16A085L, 0xFF2C3E50L, 0xFFF39C12L
)

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val materias       by viewModel.materias.collectAsState()
    val nombreUsuario  by viewModel.nombreUsuario.collectAsState()
    var mostrarDialogoAgregar by remember { mutableStateOf(false) }
    var mostrarDialogoGestionar by remember { mutableStateOf(false) }
    var materiaSeleccionada by remember { mutableStateOf<MateriaEntity?>(null) }
    var materiaAEliminar by remember { mutableStateOf<MateriaEntity?>(null) }

    val context = LocalContext.current
    val application = context.applicationContext as KodaNotesApplication
    val detailViewModel: CourseDetailViewModel = viewModel(
        key = "detail_${materiaSeleccionada?.id ?: "none"}",
        factory = CourseDetailViewModelFactory(
            application.repository,
            materiaSeleccionada?.id ?: -1
        )
    )

    var tabModoVista by remember { mutableStateOf(0) }

    AnimatedContent(
        targetState = materiaSeleccionada,
        transitionSpec = {
            if (targetState != null) {
                slideInHorizontally { it } + fadeIn(tween(220)) togetherWith
                        slideOutHorizontally { -it / 3 } + fadeOut(tween(180))
            } else {
                slideInHorizontally { -it / 3 } + fadeIn(tween(220)) togetherWith
                        slideOutHorizontally { it } + fadeOut(tween(180))
            }
        },
        label = "home_detail_transition"
    ) { materia ->
        if (materia != null) {
            CourseDetailScreen(
                materia     = materia,
                viewModel   = detailViewModel,
                onBackClick = { materiaSeleccionada = null }
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundLight)
                    .padding(top = 48.dp, start = 20.dp, end = 20.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "KodaNotes",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextDark,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(TealAccent, CircleShape)
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Hola, ", color = TextMuted, fontSize = 14.sp)
                                Text(
                                    nombreUsuario,
                                    color = TextDark,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.width(4.dp))
                                Text("👋", fontSize = 14.sp)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PeachBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🎓", fontSize = 22.sp)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(RoundedCornerShape(23.dp))
                            .background(Color(0xFFE5E7EB))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .shadow(
                                    if (tabModoVista == 0) 2.dp else 0.dp,
                                    RoundedCornerShape(20.dp)
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (tabModoVista == 0) CardWhite else Color.Transparent)
                                .clickable { tabModoVista = 0 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Con Cursos (${materias.size})",
                                fontSize = 13.sp,
                                fontWeight = if (tabModoVista == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (tabModoVista == 0) TextDark else TextMuted
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .shadow(
                                    if (tabModoVista == 1) 2.dp else 0.dp,
                                    RoundedCornerShape(20.dp)
                                )
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (tabModoVista == 1) CardWhite else Color.Transparent)
                                .clickable { tabModoVista = 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Vista Vacía",
                                fontSize = 13.sp,
                                fontWeight = if (tabModoVista == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (tabModoVista == 1) TextDark else TextMuted
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "RESUMEN DE ESTUDIO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                        val totalClases = materias.sumOf { it.cantidadDocs }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(CardWhite)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(
                                "$totalClases clases en texto",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(PeachBg)
                                .clickable { mostrarDialogoAgregar = true }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("+  ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                                Text("+ Nueva", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(DarkContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1.3f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .background(MintBg)
                                .clickable { mostrarDialogoAgregar = true }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Grabar Audio", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                                Spacer(Modifier.width(6.dp))
                                Text("→", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextDark)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Tus Asignaturas",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDark,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            "${materias.size} materias",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextMuted
                        )
                    }
                }

                item {
                    if (tabModoVista == 1 || materias.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(28.dp), spotColor = Color(0x30000000))
                                .clip(RoundedCornerShape(28.dp))
                                .background(DarkContainer)
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(DarkCardBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Folder,
                                        null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                                Spacer(Modifier.height(14.dp))
                                Text(
                                    "Sin asignaturas",
                                    color = TextWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Toca + Nueva para agregar una materia",
                                    color = TextMuted,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(12.dp, RoundedCornerShape(28.dp), spotColor = Color(0x40000000))
                                .clip(RoundedCornerShape(28.dp))
                                .background(DarkContainer)
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                materias.forEachIndexed { idx, materia ->
                                    val badgeColor = CoursePalette[idx % CoursePalette.size]
                                    SubjectDarkRow(
                                        materia = materia,
                                        badgeColor = badgeColor,
                                        onClick = { materiaSeleccionada = materia },
                                        onLongPress = { materiaAEliminar = materia }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (mostrarDialogoAgregar) {
                DialogAgregarMateria(
                    onDismiss = { mostrarDialogoAgregar = false },
                    onConfirm = { nombre, simbolo, colorIdx ->
                        viewModel.agregarNuevaMateria(nombre, simbolo, COLORES_MATERIAS[colorIdx % COLORES_MATERIAS.size])
                        mostrarDialogoAgregar = false
                    }
                )
            }

            if (mostrarDialogoGestionar) {
                DialogGestionarMaterias(
                    materias = materias,
                    onDismiss = { mostrarDialogoGestionar = false },
                    onEliminar = { viewModel.eliminarMateria(it) }
                )
            }

            if (materiaAEliminar != null) {
                AlertDialog(
                    onDismissRequest = { materiaAEliminar = null },
                    containerColor = CardWhite,
                    shape = RoundedCornerShape(24.dp),
                    title = { Text("Eliminar asignatura", fontWeight = FontWeight.Bold, color = TextDark) },
                    text = { Text("¿Eliminar \"${materiaAEliminar!!.nombre}\"? Se borrarán también todas sus notas.") },
                    confirmButton = {
                        Button(
                            onClick = { viewModel.eliminarMateria(materiaAEliminar!!); materiaAEliminar = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("Eliminar", fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { materiaAEliminar = null }) {
                            Text("Cancelar", color = TextMuted)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SubjectDarkRow(
    materia: MateriaEntity,
    badgeColor: Color,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DarkCardBg)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(badgeColor),
            contentAlignment = Alignment.Center
        ) {
            Text(materia.simbolo, fontSize = 20.sp)
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                materia.nombre,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = TextWhite
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "${materia.cantidadDocs} clases grabadas · Transcrito ES",
                fontSize = 12.sp,
                color = TextMuted,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF282A30)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun DialogAgregarMateria(onDismiss: () -> Unit, onConfirm: (String, String, Int) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var simbolo by remember { mutableStateOf("") }
    var colorIdx by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Nueva Asignatura", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la materia") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkContainer,
                        focusedLabelColor = DarkContainer
                    )
                )
                OutlinedTextField(
                    value = simbolo,
                    onValueChange = { if (it.length <= 2) simbolo = it },
                    label = { Text("Emoji (ej: 🔬)") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkContainer,
                        focusedLabelColor = DarkContainer
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { if (nombre.isNotBlank() && simbolo.isNotBlank()) onConfirm(nombre, simbolo, colorIdx) },
                enabled = nombre.isNotBlank() && simbolo.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DarkContainer),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Guardar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = TextMuted) }
        }
    )
}

@Composable
fun DialogGestionarMaterias(materias: List<MateriaEntity>, onDismiss: () -> Unit, onEliminar: (MateriaEntity) -> Unit) {
    var confirmando by remember { mutableStateOf<MateriaEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Gestionar Asignaturas", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Desliza hacia la izquierda para eliminar.", fontSize = 13.sp, color = TextMuted)
                Spacer(Modifier.height(4.dp))
                materias.forEach { materia ->
                    SwipeToDeleteRow(materia = materia, onDelete = { confirmando = materia })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Listo", color = DarkContainer, fontWeight = FontWeight.Bold) }
        }
    )

    if (confirmando != null) {
        AlertDialog(
            onDismissRequest = { confirmando = null },
            containerColor = CardWhite,
            shape = RoundedCornerShape(20.dp),
            title = { Text("¿Eliminar?", fontWeight = FontWeight.Bold, color = TextDark) },
            text = { Text("Se borrará \"${confirmando!!.nombre}\" y todas sus notas.") },
            confirmButton = {
                Button(
                    onClick = { onEliminar(confirmando!!); confirmando = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmando = null }) { Text("Cancelar", color = TextMuted) } }
        )
    }
}

@Composable
fun SwipeToDeleteRow(materia: MateriaEntity, onDelete: () -> Unit) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    val maxSwipe = -160f
    val bgColor by animateColorAsState(
        if (offsetX < -40f) Color(0xFFFFEEEE) else Color(0xFFF8F8FC),
        tween(200), label = "bg"
    )
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(bgColor)) {
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(56.dp).clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFFF3B30), modifier = Modifier.size(22.dp))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth()
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .background(bgColor, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = { offsetX = if (offsetX < maxSwipe / 2) maxSwipe else 0f },
                        onDragCancel = { offsetX = 0f }
                    ) { _, dragAmount -> offsetX = (offsetX + dragAmount).coerceIn(maxSwipe, 0f) }
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(materia.colorHex)),
                contentAlignment = Alignment.Center
            ) { Text(materia.simbolo, fontSize = 18.sp) }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(materia.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                Text("${materia.cantidadDocs} docs", fontSize = 11.sp, color = TextMuted)
            }
            Icon(Icons.Default.Edit, null, tint = Color(0xFFCCCCD4), modifier = Modifier.size(18.dp))
        }
    }
}