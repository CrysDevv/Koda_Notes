package app.kodanotes.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kodanotes.data.local.entity.MateriaEntity
import app.kodanotes.ui.theme.BackgroundLight
import app.kodanotes.ui.theme.CardWhite
import app.kodanotes.ui.theme.PurpleMain
import app.kodanotes.ui.theme.TextDark
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

    // P3 FIX: viewModel() siempre se llama, fuera de cualquier condicional.
    // Usa key nula cuando no hay materia seleccionada para no crear el VM innecesariamente.
    val context = androidx.compose.ui.platform.LocalContext.current
    val application = context.applicationContext as app.kodanotes.KodaNotesApplication
    val detailViewModel: CourseDetailViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        key = "detail_${materiaSeleccionada?.id ?: "none"}",
        factory = CourseDetailViewModelFactory(
            application.repository,
            materiaSeleccionada?.id ?: -1
        )
    )

    // P2 FIX: AnimatedContent en lugar de return anticipado.
    // Ambas ramas coexisten en el árbol de Compose — solo una es visible a la vez.
    AnimatedContent(
        targetState = materiaSeleccionada,
        transitionSpec = {
            if (targetState != null) {
                // Navegar a detalle: entra desde la derecha
                slideInHorizontally { it } + fadeIn(tween(220)) togetherWith
                        slideOutHorizontally { -it / 3 } + fadeOut(tween(180))
            } else {
                // Volver al home: entra desde la izquierda
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
                        Text("KodaNotes", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
                            color = TextDark, letterSpacing = (-0.5).sp)
                        Text("$nombreUsuario 👋", color = Color(0xFF9E9EA7), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    Box(modifier = Modifier.size(44.dp).shadow(4.dp, CircleShape).clip(CircleShape)
                        .background(Color(0xFFFFD6CC)), contentAlignment = Alignment.Center) {
                        Text("👩", fontSize = 22.sp)
                    }
                }

                Spacer(Modifier.height(32.dp))

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tus Asignaturas", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextDark)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (materias.isNotEmpty()) {
                            Text("Gestionar", color = Color(0xFF9E9EA7), fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp, modifier = Modifier.clickable { mostrarDialogoGestionar = true })
                        }
                        Text("+ Nueva", color = PurpleMain, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                            modifier = Modifier.clickable { mostrarDialogoAgregar = true })
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (materias.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 60.dp),
                        contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("📚", fontSize = 48.sp)
                            Spacer(Modifier.height(16.dp))
                            Text("Sin asignaturas aún", color = Color(0xFFBBBBC2),
                                fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Spacer(Modifier.height(12.dp))
                            Box(modifier = Modifier.clip(RoundedCornerShape(14.dp)).background(PurpleMain)
                                .clickable { mostrarDialogoAgregar = true }
                                .padding(horizontal = 24.dp, vertical = 12.dp)) {
                                Text("Crear primera asignatura", color = Color.White,
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(materias, key = { it.id }) { materia ->
                            AnimatedVisibility(
                                visible = true,
                                enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn()
                            ) {
                                CourseCard(
                                    materiaEntity = materia,
                                    onClick = { materiaSeleccionada = materia },
                                    onLongPress = { materiaAEliminar = materia }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
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
                    containerColor = CardWhite, shape = RoundedCornerShape(24.dp),
                    title = { Text("Eliminar asignatura", fontWeight = FontWeight.Bold, color = TextDark) },
                    text = { Text("¿Eliminar \"${materiaAEliminar!!.nombre}\"? Se borrarán también todas sus notas.") },
                    confirmButton = {
                        Button(onClick = { viewModel.eliminarMateria(materiaAEliminar!!); materiaAEliminar = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                            shape = RoundedCornerShape(12.dp)) { Text("Eliminar", fontWeight = FontWeight.Bold) }
                    },
                    dismissButton = {
                        TextButton(onClick = { materiaAEliminar = null }) { Text("Cancelar", color = Color(0xFF9E9EA7)) }
                    }
                )
            }
        } // cierra else (rama home)
    } // cierra AnimatedContent
}

@Composable
fun DialogAgregarMateria(onDismiss: () -> Unit, onConfirm: (String, String, Int) -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var simbolo by remember { mutableStateOf("") }
    var colorIdx by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite, shape = RoundedCornerShape(24.dp),
        title = { Text("Nueva Asignatura", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(nombre, { nombre = it }, label = { Text("Nombre") },
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurpleMain, focusedLabelColor = PurpleMain))
                OutlinedTextField(simbolo, { if (it.length <= 2) simbolo = it }, label = { Text("Emoji (ej: 🔬)") },
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PurpleMain, focusedLabelColor = PurpleMain))
                Text("Color de la asignatura", fontSize = 13.sp, color = Color(0xFF9E9EA7), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    COLORES_MATERIAS.forEachIndexed { idx, color ->
                        Box(modifier = Modifier.size(34.dp)
                            .shadow(if (colorIdx == idx) 6.dp else 0.dp, CircleShape, spotColor = Color(color).copy(0.4f))
                            .clip(CircleShape).background(Color(color)).clickable { colorIdx = idx },
                            contentAlignment = Alignment.Center) {
                            if (colorIdx == idx) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (nombre.isNotBlank() && simbolo.isNotBlank()) onConfirm(nombre, simbolo, colorIdx) },
                enabled = nombre.isNotBlank() && simbolo.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PurpleMain),
                shape = RoundedCornerShape(12.dp)) { Text("Guardar", fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color(0xFF9E9EA7)) } }
    )
}

@Composable
fun DialogGestionarMaterias(materias: List<MateriaEntity>, onDismiss: () -> Unit, onEliminar: (MateriaEntity) -> Unit) {
    var confirmando by remember { mutableStateOf<MateriaEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardWhite, shape = RoundedCornerShape(24.dp),
        title = { Text("Gestionar Asignaturas", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = TextDark) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Desliza hacia la izquierda para eliminar.", fontSize = 13.sp, color = Color(0xFF9E9EA7))
                Spacer(Modifier.height(4.dp))
                materias.forEach { materia ->
                    SwipeToDeleteRow(materia = materia, onDelete = { confirmando = materia })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Listo", color = PurpleMain, fontWeight = FontWeight.Bold) } }
    )

    if (confirmando != null) {
        AlertDialog(
            onDismissRequest = { confirmando = null },
            containerColor = CardWhite, shape = RoundedCornerShape(20.dp),
            title = { Text("¿Eliminar?", fontWeight = FontWeight.Bold, color = TextDark) },
            text = { Text("Se borrará \"${confirmando!!.nombre}\" y todas sus notas.") },
            confirmButton = {
                Button(onClick = { onEliminar(confirmando!!); confirmando = null },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                    shape = RoundedCornerShape(10.dp)) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmando = null }) { Text("Cancelar", color = Color(0xFF9E9EA7)) } }
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
        Row(modifier = Modifier.fillMaxWidth().height(56.dp),
            horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(56.dp).clickable { onDelete() }, contentAlignment = Alignment.Center) {
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
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color(materia.colorHex)),
                contentAlignment = Alignment.Center) { Text(materia.simbolo, fontSize = 18.sp) }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(materia.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextDark)
                Text("${materia.cantidadDocs} docs", fontSize = 11.sp, color = Color(0xFF9E9EA7))
            }
            Icon(Icons.Default.Edit, null, tint = Color(0xFFCCCCD4), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun CourseCard(materiaEntity: MateriaEntity, onClick: () -> Unit, onLongPress: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            .shadow(6.dp, RoundedCornerShape(24.dp), spotColor = Color(0x14000000))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite)
    ) {
        Column(modifier = Modifier.padding(18.dp).fillMaxSize(), verticalArrangement = Arrangement.Center) {
            Box(modifier = Modifier.size(52.dp).background(Color(materiaEntity.colorHex), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center) {
                Text(materiaEntity.simbolo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
            }
            Spacer(Modifier.height(16.dp))
            Text(materiaEntity.nombre, fontWeight = FontWeight.Bold, color = TextDark,
                fontSize = 15.sp, lineHeight = 20.sp, maxLines = 2)
            Spacer(Modifier.height(6.dp))
            Text("${materiaEntity.cantidadDocs} DOCS", fontSize = 11.sp, color = Color(0xFFB0B0BA),
                fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp)
        }
    }
}