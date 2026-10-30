package app.kodanotes.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.kodanotes.data.security.AuthManager
import app.kodanotes.ui.theme.BackgroundLight
import app.kodanotes.ui.theme.CardWhite
import app.kodanotes.ui.theme.PurpleMain
import app.kodanotes.ui.theme.TextDark
import app.kodanotes.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val llaveActual         by viewModel.apiKeyActual.collectAsState()
    val modeloActual        by viewModel.modeloSeleccionado.collectAsState()
    val modelosDisponibles  by viewModel.modelosDisponibles.collectAsState()
    val isValidating        by viewModel.isValidating.collectAsState()
    val error               by viewModel.validationError.collectAsState()
    val intervaloActual     by viewModel.intervaloSeleccionado.collectAsState()
    val nombreActual        by viewModel.nombreUsuario.collectAsState()

    var nuevaLlave      by remember { mutableStateOf("") }
    var editandoLlave   by remember { mutableStateOf(llaveActual.isEmpty()) }
    var expandidoModelo by remember { mutableStateOf(false) }
    var nombreEditado   by remember(nombreActual) { mutableStateOf(nombreActual) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
            .verticalScroll(rememberScrollState())
            .padding(top = 60.dp, start = 20.dp, end = 20.dp, bottom = 32.dp)
    ) {
        Text(
            "Ajustes", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold,
            color = TextDark, letterSpacing = (-0.5).sp
        )

        Spacer(Modifier.height(28.dp))

        // ── SECCIÓN: Perfil ───────────────────────────────────────────────────
        SectionLabel("PERFIL")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Tu nombre", fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp, color = TextDark)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = nombreEditado,
                    onValueChange = { nombreEditado = it },
                    singleLine    = true,
                    placeholder   = { Text("Ej: Ana García") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(14.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PurpleMain,
                        unfocusedBorderColor = Color(0xFFE0E0E8)
                    ),
                    trailingIcon  = {
                        if (nombreEditado != nombreActual && nombreEditado.isNotBlank()) {
                            androidx.compose.material3.IconButton(
                                onClick = { viewModel.guardarNombre(nombreEditado) }
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Guardar nombre",
                                    tint = PurpleMain
                                )
                            }
                        }
                    }
                )
                Text(
                    "Aparece en el saludo de la pantalla principal.",
                    fontSize = 11.sp, color = Color(0xFFB0B0BA),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── SECCIÓN: API Key ──────────────────────────────────────────────────
        SectionLabel("SERVICIOS DE IA")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFEEF0FF)),
                            contentAlignment = Alignment.Center
                        ) { Text("🤖", fontSize = 22.sp) }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Google Gemini API", fontWeight = FontWeight.Bold,
                                fontSize = 15.sp, color = TextDark)
                            Text("Procesamiento y resumen", fontSize = 12.sp, color = Color(0xFF9E9EA7))
                        }
                    }
                    if (!editandoLlave && llaveActual.isNotEmpty()) {
                        IconButton(onClick = { editandoLlave = true }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Edit, null, tint = Color(0xFFB0B0BA),
                                modifier = Modifier.size(18.dp))
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (!editandoLlave && llaveActual.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0FBF0)).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Clave Vinculada", fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20), fontSize = 13.sp)
                            Text("Listo para grabar y analizar", color = Color(0xFF388E3C), fontSize = 11.sp)
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = nuevaLlave, onValueChange = { nuevaLlave = it },
                        label = { Text("Introducir API Key...") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        isError = error != null, enabled = !isValidating,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PurpleMain, focusedLabelColor = PurpleMain
                        )
                    )
                    if (error != null) {
                        Text(error!!, color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("🔒 Tus llaves se guardan localmente y están cifradas.",
                        fontSize = 11.sp, color = Color(0xFFB0B0BA),
                        modifier = Modifier.padding(start = 4.dp))
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = {
                            viewModel.validarYGuardarLlave(nuevaLlave)
                            if (error == null) editandoLlave = false
                        },
                        modifier = Modifier.align(Alignment.End),
                        enabled = !isValidating && nuevaLlave.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleMain),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isValidating) CircularProgressIndicator(
                            Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp
                        )
                        else Text("Vincular", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── SECCIÓN: Modelo de IA ─────────────────────────────────────────────
        SectionLabel("CEREBRO IA")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Selecciona el motor de análisis. Flash Lite ahorra tokens, Flash es más preciso.",
                    fontSize = 12.sp, color = Color(0xFF9E9EA7), lineHeight = 18.sp
                )
                Spacer(Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedCard(
                        onClick = { if (llaveActual.isNotEmpty()) expandidoModelo = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.5.dp,
                            if (llaveActual.isNotEmpty()) PurpleMain.copy(0.3f) else Color(0xFFE8E8EE)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val display = modelosDisponibles.find { it.first == modeloActual }?.second ?: "Selecciona un modelo"
                                Text(display, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                                    color = if (llaveActual.isNotEmpty()) TextDark else Color(0xFFB0B0BA))
                                Text(modeloActual, fontSize = 10.sp,
                                    color = PurpleMain.copy(if (llaveActual.isNotEmpty()) 1f else 0.4f))
                            }
                            Icon(Icons.Default.ArrowDropDown, null,
                                tint = if (llaveActual.isNotEmpty()) PurpleMain else Color(0xFFB0B0BA))
                        }
                    }
                    DropdownMenu(
                        expanded = expandidoModelo,
                        onDismissRequest = { expandidoModelo = false },
                        modifier = Modifier.fillMaxWidth(0.85f).background(CardWhite)
                    ) {
                        modelosDisponibles.forEach { (id, nombre) ->
                            DropdownMenuItem(
                                text = { Text(nombre, fontSize = 14.sp, fontWeight = FontWeight.Medium) },
                                onClick = { viewModel.guardarNuevoModelo(id); expandidoModelo = false },
                                leadingIcon = { RadioButton(selected = id == modeloActual, onClick = null) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ── SECCIÓN: Intervalo de guardado automático ─────────────────────────
        SectionLabel("GRABACIÓN")

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "¿Cada cuánto tiempo guardar un segmento durante la grabación?",
                    fontSize = 12.sp, color = Color(0xFF9E9EA7), lineHeight = 18.sp
                )
                Spacer(Modifier.height(14.dp))

                viewModel.opcionesIntervalo.forEach { (segundos, etiqueta) ->
                    val seleccionado = intervaloActual == segundos
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (seleccionado) PurpleMain.copy(alpha = 0.07f)
                                else Color.Transparent
                            )
                            .clickable { viewModel.guardarIntervalo(segundos) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = seleccionado,
                            onClick = { viewModel.guardarIntervalo(segundos) }
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            // Título de la opción (parte antes del "—")
                            val partes = etiqueta.split("—")
                            Text(
                                partes[0].trim(),
                                fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp,
                                color = if (seleccionado) PurpleMain else TextDark
                            )
                            if (partes.size > 1) {
                                Text(
                                    partes[1].trim(),
                                    fontSize = 11.sp,
                                    color = Color(0xFF9E9EA7)
                                )
                            }
                        }
                        // Badge "Recomendado"
                        if (segundos == AuthManager.INTERVALO_5_MIN) {
                            Spacer(Modifier.weight(1f))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(PurpleMain.copy(0.1f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("Recomendado", fontSize = 10.sp, color = PurpleMain,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    if (segundos != AuthManager.INTERVALO_10_MIN) {
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionLabel(texto: String) {
    Text(
        texto, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        color = Color(0xFFB0B0BA), letterSpacing = 1.5.sp,
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
    )
}