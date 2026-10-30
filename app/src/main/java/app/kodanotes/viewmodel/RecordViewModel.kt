package app.kodanotes.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.kodanotes.ai.AiPrompts
import app.kodanotes.ai.GeminiService
import app.kodanotes.data.audio.AudioRecorder
import app.kodanotes.data.audio.WhisperTranscriber
import app.kodanotes.data.local.entity.MateriaEntity
import app.kodanotes.data.local.entity.NotaEntity
import app.kodanotes.data.repository.MateriaRepository
import app.kodanotes.data.security.AuthManager
import app.kodanotes.service.RecordingService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.coroutineContext

// ─────────────────────────────────────────────────────────────────────────────
// Estados públicos
// ─────────────────────────────────────────────────────────────────────────────

enum class WhisperState { CARGANDO, LISTO, ERROR }

sealed class RecordState {
    object Idle                                                  : RecordState()
    object Recording                                             : RecordState()
    data class Transcribing(val actual: Int, val total: Int)     : RecordState()
    object Analyzing                                             : RecordState()
    object Success                                               : RecordState()
    object ErrorNoApi                                            : RecordState()
    data class ErrorGeneral(val mensaje: String)                 : RecordState()
}

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

class RecordViewModel(
    private val repository:         MateriaRepository,
    private val audioRecorder:      AudioRecorder,
    private val geminiService:      GeminiService,
    private val whisperTranscriber: WhisperTranscriber,
    private val authManager:        AuthManager
) : ViewModel() {

    val materias: StateFlow<List<MateriaEntity>> = repository.todasLasMaterias
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _materiaSeleccionada = MutableStateFlow<MateriaEntity?>(null)
    val materiaSeleccionada = _materiaSeleccionada.asStateFlow()

    private val _recordState = MutableStateFlow<RecordState>(RecordState.Idle)
    val recordState = _recordState.asStateFlow()

    private val _timerSeconds = MutableStateFlow(0)
    val timerSeconds = _timerSeconds.asStateFlow()

    private val _ultimaNotaGuardada = MutableStateFlow("")
    val ultimaNotaGuardada = _ultimaNotaGuardada.asStateFlow()

    val audioLevel: StateFlow<Float> = audioRecorder.audioLevel

    private val _whisperState = MutableStateFlow(WhisperState.CARGANDO)
    val whisperState: StateFlow<WhisperState> = _whisperState.asStateFlow()

    private var timerJob:      Job? = null
    private var processingJob: Job? = null
    private var nombreNotaOpcional = ""

    init {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("RECORD_VM", "Inicializando Whisper Base…")
            val ok = whisperTranscriber.inicializar()
            _whisperState.value = if (ok) WhisperState.LISTO else WhisperState.ERROR
            Log.d("RECORD_VM", "Whisper → ${_whisperState.value}")
        }

        viewModelScope.launch {
            audioRecorder.isRecordingState.collect { isHardwareRecording ->
                val isVmRecording = _recordState.value == RecordState.Recording

                if (isVmRecording && !isHardwareRecording) {
                    Log.w("RECORD_VM", "El hardware detuvo la grabación inesperadamente. Procesando...")
                    stopTimer()
                    iniciarProcesamiento()
                }
            }
        }
    }

    fun seleccionarMateria(materia: MateriaEntity?, context: Context? = null) {
        if (materia == null && _recordState.value == RecordState.Recording) {
            audioRecorder.stop()
            stopTimer()
            context?.let { RecordingService.stop(it) }
        }
        _materiaSeleccionada.value = materia
        resetEstado()
    }

    private fun resetEstado() {
        stopTimer()
        processingJob?.cancel()
        _timerSeconds.value = 0
        _recordState.value  = RecordState.Idle
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun gestionarGrabacion(context: Context, nombreNota: String = "") {
        when (_recordState.value) {
            is RecordState.Recording -> detenerGrabacion(context)

            is RecordState.Idle,
            is RecordState.Success,
            is RecordState.ErrorGeneral -> {
                if (_whisperState.value == WhisperState.CARGANDO) {
                    Log.w("RECORD_VM", "Whisper aún cargando")
                    return
                }
                if (!geminiService.tieneApiKey()) {
                    mostrarErrorTemporal(RecordState.ErrorNoApi)
                    return
                }
                iniciarGrabacion(context, nombreNota)
            }

            is RecordState.Transcribing,
            is RecordState.Analyzing,
            is RecordState.ErrorNoApi -> { /* no-op */ }
        }
    }

    @androidx.annotation.RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    private fun iniciarGrabacion(context: Context, nombreNota: String) {
        nombreNotaOpcional  = nombreNota
        _timerSeconds.value = 0
        _recordState.value  = RecordState.Recording

        startTimer()
        RecordingService.start(context)
        // Pasamos el context al AudioRecorder para gestionar el Foco de Audio
        audioRecorder.start(context = context, outputDir = context.cacheDir)
    }

    private fun detenerGrabacion(context: Context) {
        audioRecorder.stop()
        stopTimer()
        RecordingService.stop(context)

        iniciarProcesamiento()
    }

    private fun iniciarProcesamiento() {
        if (processingJob?.isActive == true) return

        processingJob = viewModelScope.launch(Dispatchers.IO) {
            audioRecorder.recordingJob?.join()
            delay(500)

            val archivos = audioRecorder.grabacionesSnapshot

            Log.d("RECORD_VM", "Procesamiento iniciado — ${archivos.size} chunk(s) capturados")

            if (archivos.isEmpty()) {
                mostrarErrorTemporal(RecordState.ErrorGeneral("No se detectó audio suficiente"))
                return@launch
            }

            val transcripcionCompleta = transcribirChunks(archivos)

            if (transcripcionCompleta.isBlank()) {
                mostrarErrorTemporal(RecordState.ErrorGeneral("No se pudo transcribir el audio. Verifica tu voz o el ruido."))
                return@launch
            }

            analizarYGuardar(transcripcionCompleta)
        }
    }

    private suspend fun transcribirChunks(archivos: List<java.io.File>): String {
        val total     = archivos.size
        val resultado = StringBuilder()

        for ((idx, archivo) in archivos.withIndex()) {
            if (!coroutineContext.isActive) break

            _recordState.value = RecordState.Transcribing(actual = idx + 1, total = total)
            Log.d("RECORD_VM", "Transcribiendo chunk ${idx + 1}/$total: ${archivo.name}")

            val texto = try {
                whisperTranscriber.transcribir(archivo)
            } catch (e: Exception) {
                Log.e("RECORD_VM", "Error en chunk ${idx + 1}: ${e.message}")
                null
            } finally {
                if (archivo.exists()) archivo.delete()
            }

            val textoLimpio = texto?.trim()
            val esAlucinacion = textoLimpio.isNullOrBlank() ||
                    textoLimpio.length < 3 ||
                    textoLimpio.contains("[Música]",   ignoreCase = true) ||
                    textoLimpio.contains("[Silencio]", ignoreCase = true) ||
                    textoLimpio.contains("[Music]",    ignoreCase = true) ||
                    textoLimpio.contains("[Silence]",  ignoreCase = true) ||
                    textoLimpio.matches(Regex("^[.,!?\\- ]+$"))

            if (!esAlucinacion && textoLimpio != null) {
                if (resultado.isNotEmpty()) resultado.append(" ")
                resultado.append(textoLimpio)
                Log.d("RECORD_VM", "Chunk ${idx + 1} OK: ${textoLimpio.take(60)}")
            } else {
                Log.w("RECORD_VM", "Chunk ${idx + 1} descartado por Whisper (alucinación/silencio): '$textoLimpio'")
            }
        }

        return resultado.toString().trim()
    }

    private suspend fun analizarYGuardar(transcripcion: String) {
        val materiaActual = _materiaSeleccionada.value ?: run {
            mostrarErrorTemporal(RecordState.ErrorGeneral("Sin materia seleccionada"))
            return
        }

        _recordState.value = RecordState.Analyzing
        Log.d("RECORD_VM", "Analizando transcripción (${transcripcion.length} chars)…")

        val resumen = try {
            withTimeout(60_000L) {
                geminiService.validarYGenerar(
                    AiPrompts.analisisSegmento(
                        transcripcion = transcripcion,
                        numero        = 1,
                        totalEstimado = 1
                    )
                )
            }
        } catch (e: TimeoutCancellationException) {
            Log.e("RECORD_VM", "Timeout esperando a Gemini")
            null
        } catch (e: Exception) {
            Log.e("RECORD_VM", "Error Gemini: ${e.message}")
            null
        }

        if (resumen == null || resumen.startsWith("Error")) {
            mostrarErrorTemporal(RecordState.ErrorGeneral("Error de conexión al generar las notas"))
            return
        }

        val titulo = if (nombreNotaOpcional.isNotBlank())
            nombreNotaOpcional
        else
            "Clase — ${fechaActual()}"

        repository.agregarNotaSinContador(
            NotaEntity(materiaId = materiaActual.id, titulo = titulo, contenido = resumen)
        )
        _ultimaNotaGuardada.value = titulo

        if (transcripcion.length > 2000) {
            val cheatSheet = extraerCheatSheet(resumen)
            if (cheatSheet.isNotBlank()) {
                val resumenGlobal = try {
                    withTimeout(60_000L) {
                        geminiService.validarYGenerar(
                            AiPrompts.resumenGlobal(listOf(cheatSheet))
                        )
                    }
                } catch (e: Exception) { null }

                if (resumenGlobal != null && !resumenGlobal.startsWith("Error")) {
                    val tituloGlobal = if (nombreNotaOpcional.isNotBlank())
                        "📋 $nombreNotaOpcional — Resumen"
                    else
                        "📋 Clase ${fechaActual()} — Resumen"

                    repository.agregarNota(
                        NotaEntity(materiaId = materiaActual.id,
                            titulo    = tituloGlobal,
                            contenido = resumenGlobal)
                    )
                    _ultimaNotaGuardada.value = tituloGlobal
                }
            }
        }

        _recordState.value = RecordState.Success
        delay(4000L)
        if (_recordState.value == RecordState.Success)
            _recordState.value = RecordState.Idle
        _timerSeconds.value = 0
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive && _recordState.value == RecordState.Recording) {
                delay(1000L)
                _timerSeconds.value++
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun mostrarErrorTemporal(estado: RecordState) {
        _recordState.value = estado
        viewModelScope.launch {
            delay(4000L)
            if (_recordState.value == estado)
                _recordState.value = RecordState.Idle
        }
    }

    private fun extraerCheatSheet(resumen: String): String {
        val bloques = resumen.split("|||")
        return bloques.getOrNull(2)?.trim() ?: bloques.lastOrNull()?.trim() ?: ""
    }

    private fun fechaActual(): String =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern(
                    "dd MMM, HH:mm", java.util.Locale.getDefault()
                )
            )
        } else {
            java.text.SimpleDateFormat("dd MMM, HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date())
        }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        processingJob?.cancel()
        whisperTranscriber.liberar()
    }
}

class RecordViewModelFactory(
    private val repository:         MateriaRepository,
    private val audioRecorder:      AudioRecorder,
    private val geminiService:      GeminiService,
    private val whisperTranscriber: WhisperTranscriber,
    private val authManager:        AuthManager
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RecordViewModel(
                repository, audioRecorder, geminiService, whisperTranscriber, authManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}