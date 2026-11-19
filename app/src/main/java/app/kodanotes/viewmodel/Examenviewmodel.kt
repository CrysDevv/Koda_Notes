package app.kodanotes.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.kodanotes.ai.AiPrompts
import app.kodanotes.ai.AiService
import app.kodanotes.data.local.entity.ExamenEntity
import app.kodanotes.data.local.entity.MateriaEntity
import app.kodanotes.data.local.entity.NotaEntity
import app.kodanotes.data.repository.EstudioRepository
import app.kodanotes.data.repository.MateriaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class ExamenState {
    IDLE,
    GENERANDO,
    EN_EXAMEN,
    RESULTADO,
    REPASO,
    ERROR
}

/** Representa una pregunta durante la sesión activa del examen. */
data class PreguntaActiva(
    val pregunta:          String,
    val opciones:          List<String>,
    val respuestaCorrecta: String,
    val explicacion:       String,
    var seleccionada:      String? = null
)

class ExamenViewModel(
    private val materiaRepository: MateriaRepository,
    private val estudioRepository: EstudioRepository,
    private val aiService:         AiService
) : ViewModel() {

    val materias: StateFlow<List<MateriaEntity>> = materiaRepository.todasLasMaterias
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _state = MutableStateFlow(ExamenState.IDLE)
    val state = _state.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg = _errorMsg.asStateFlow()

    private val _materiaSeleccionada = MutableStateFlow<MateriaEntity?>(null)
    val materiaSeleccionada = _materiaSeleccionada.asStateFlow()

    private val _notasDeMateriaSeleccionada = MutableStateFlow<List<NotaEntity>>(emptyList())
    val notasDeMateriaSeleccionada = _notasDeMateriaSeleccionada.asStateFlow()

    private val _notasSeleccionadas = MutableStateFlow<Set<Int>>(emptySet())
    val notasSeleccionadas = _notasSeleccionadas.asStateFlow()

    private val _cantidadPreguntas = MutableStateFlow(10)
    val cantidadPreguntas = _cantidadPreguntas.asStateFlow()

    private val _dificultad = MutableStateFlow("Media")
    val dificultad = _dificultad.asStateFlow()

    private val _preguntas = MutableStateFlow<List<PreguntaActiva>>(emptyList())
    val preguntas = _preguntas.asStateFlow()

    private val _indiceActual = MutableStateFlow(0)
    val indiceActual = _indiceActual.asStateFlow()

    val preguntaActual: StateFlow<PreguntaActiva?> =
        combine(_preguntas, _indiceActual) { preguntas, indice ->
            preguntas.getOrNull(indice)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _examenGuardado = MutableStateFlow<ExamenEntity?>(null)
    val examenGuardado = _examenGuardado.asStateFlow()

    private val _historial = MutableStateFlow<List<ExamenEntity>>(emptyList())
    val historial = _historial.asStateFlow()

    private val _mejorNota   = MutableStateFlow<Float?>(null)
    val mejorNota   = _mejorNota.asStateFlow()

    private val _promedioNota = MutableStateFlow<Float?>(null)
    val promedioNota = _promedioNota.asStateFlow()

    private val _preguntasRepaso = MutableStateFlow<List<PreguntaActiva>>(emptyList())
    val preguntasRepaso = _preguntasRepaso.asStateFlow()

    private val _indiceRepaso = MutableStateFlow(0)
    val indiceRepaso = _indiceRepaso.asStateFlow()

    fun seleccionarMateria(materia: MateriaEntity) {
        _materiaSeleccionada.value = materia
        _notasSeleccionadas.value  = emptySet()
        _notasDeMateriaSeleccionada.value = emptyList()
        cargarHistorial(materia.id)

        viewModelScope.launch {
            materiaRepository.obtenerNotasPorMateria(materia.id)
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
                .collect { notas ->
                    _notasDeMateriaSeleccionada.value = notas
                }
        }
    }

    fun toggleNota(notaId: Int) {
        val actual = _notasSeleccionadas.value.toMutableSet()
        if (notaId in actual) actual.remove(notaId) else actual.add(notaId)
        _notasSeleccionadas.value = actual
    }

    fun setCantidadPreguntas(cantidad: Int) {
        _cantidadPreguntas.value = cantidad.coerceIn(5, 20)
    }

    fun setDificultad(nivel: String) {
        _dificultad.value = nivel
    }

    fun generarExamen() {
        val notasIds = _notasSeleccionadas.value
        if (notasIds.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _state.value    = ExamenState.GENERANDO
            _errorMsg.value = null

            val contenido = _notasDeMateriaSeleccionada.value
                .filter { it.id in notasIds }
                .joinToString("\n\n---\n\n") { it.contenido }

            val prompt    = AiPrompts.examenSimulado(
                contenidoClase = contenido,
                cantidad       = _cantidadPreguntas.value,
                dificultad     = _dificultad.value
            )
            val respuesta = aiService.validarYGenerar(prompt)

            if (respuesta == null || respuesta.startsWith("Error")) {
                _errorMsg.value = respuesta ?: "Error al conectar con la IA"
                _state.value    = ExamenState.ERROR
                return@launch
            }

            val preguntas = parsearPreguntas(respuesta)
            if (preguntas.isEmpty()) {
                _errorMsg.value = "La IA no devolvió preguntas válidas"
                _state.value    = ExamenState.ERROR
                return@launch
            }

            _preguntas.value    = preguntas
            _indiceActual.value = 0
            _state.value        = ExamenState.EN_EXAMEN
        }
    }

    fun responder(opcion: String) {
        val lista   = _preguntas.value.toMutableList()
        val indice  = _indiceActual.value
        val pregunta = lista.getOrNull(indice) ?: return
        lista[indice] = pregunta.copy(seleccionada = opcion)
        _preguntas.value = lista
    }

    fun siguientePregunta() {
        val total = _preguntas.value.size
        if (_indiceActual.value < total - 1) {
            _indiceActual.value++
        }
    }

    fun anteriorPregunta() {
        if (_indiceActual.value > 0) _indiceActual.value--
    }

    fun omitirPregunta() {
        siguientePregunta()
    }

    /** Termina el examen, calcula resultado y guarda en BD. */
    fun terminarExamen() {
        val materia   = _materiaSeleccionada.value ?: return
        val preguntas = _preguntas.value
        if (preguntas.isEmpty()) return

        val correctas   = preguntas.count { it.seleccionada == it.respuestaCorrecta }
        val omitidas    = preguntas.count { it.seleccionada == null }
        val incorrectas = preguntas.size - correctas - omitidas
        val nota        = (correctas.toFloat() / preguntas.size.toFloat()) * 10f

        _preguntasRepaso.value = preguntas.filter {
            it.seleccionada != null && it.seleccionada != it.respuestaCorrecta
        }
        _indiceRepaso.value = 0

        viewModelScope.launch(Dispatchers.IO) {
            val examen = ExamenEntity(
                materiaId      = materia.id,
                totalPreguntas = preguntas.size,
                dificultad     = _dificultad.value,
                correctas      = correctas,
                incorrectas    = incorrectas,
                omitidas       = omitidas,
                nota           = nota,
                preguntasJson  = serializarPreguntas(preguntas)
            )
            estudioRepository.guardarExamen(examen)
            _examenGuardado.value = examen
            cargarHistorial(materia.id)
            _state.value = ExamenState.RESULTADO
        }
    }

    fun iniciarRepaso() {
        if (_preguntasRepaso.value.isEmpty()) return
        _indiceRepaso.value = 0
        _state.value        = ExamenState.REPASO
    }

    fun siguienteRepaso() {
        val total = _preguntasRepaso.value.size
        if (_indiceRepaso.value < total - 1) _indiceRepaso.value++
    }

    fun anteriorRepaso() {
        if (_indiceRepaso.value > 0) _indiceRepaso.value--
    }

    fun volverDeRepaso() {
        _state.value = ExamenState.RESULTADO
    }

    private fun cargarHistorial(materiaId: Int) {
        viewModelScope.launch {
            estudioRepository.obtenerHistorialPorMateria(materiaId)
                .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
                .collect { lista ->
                    _historial.value = lista
                }
        }
        viewModelScope.launch(Dispatchers.IO) {
            _mejorNota.value    = estudioRepository.mejorNota(materiaId)
            _promedioNota.value = estudioRepository.promedioNotas(materiaId)
        }
    }

    suspend fun obtenerExamenPorId(id: Int): ExamenEntity? =
        estudioRepository.obtenerExamenPorId(id)

    fun borrarExamen(examenId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            estudioRepository.borrarExamen(examenId)
            _materiaSeleccionada.value?.id?.let { cargarHistorial(it) }
        }
    }

    fun volverAConfigurar() {
        _state.value              = ExamenState.IDLE
        _preguntas.value          = emptyList()
        _examenGuardado.value     = null
        _notasSeleccionadas.value = emptySet()
        _indiceActual.value       = 0
    }

    private fun parsearPreguntas(jsonStr: String): List<PreguntaActiva> {
        return try {
            var jsonLimpio = jsonStr.trim()
            if (jsonLimpio.startsWith("```json")) {
                jsonLimpio = jsonLimpio.removePrefix("```json")
            } else if (jsonLimpio.startsWith("```")) {
                jsonLimpio = jsonLimpio.removePrefix("```")
            }
            if (jsonLimpio.endsWith("```")) {
                jsonLimpio = jsonLimpio.removeSuffix("```")
            }
            jsonLimpio = jsonLimpio.trim()

            val array = JSONArray(jsonLimpio)
            (0 until array.length()).mapNotNull { i ->
                val obj        = array.getJSONObject(i)
                val pregunta   = obj.optString("pregunta").trim()
                val opciones   = (0 until obj.getJSONArray("opciones").length())
                    .map { obj.getJSONArray("opciones").getString(it) }
                val correcta   = obj.optString("respuestaCorrecta").trim()
                val explicacion = obj.optString("explicacion").trim()
                if (pregunta.isBlank() || opciones.isEmpty() || correcta.isBlank()) null
                else PreguntaActiva(pregunta, opciones, correcta, explicacion)
            }
        } catch (e: Exception) {
            Log.e("EXAMEN_VM", "Error parseando JSON de preguntas: ${e.message}\nJSON recibido: $jsonStr")
            emptyList()
        }
    }

    private fun serializarPreguntas(preguntas: List<PreguntaActiva>): String {
        return try {
            val array = JSONArray()
            preguntas.forEach { p ->
                val obj = JSONObject()
                obj.put("pregunta",          p.pregunta)
                obj.put("opciones",          JSONArray(p.opciones))
                obj.put("respuestaCorrecta", p.respuestaCorrecta)
                obj.put("seleccionada",      p.seleccionada ?: "")
                obj.put("explicacion",       p.explicacion)
                array.put(obj)
            }
            array.toString()
        } catch (e: Exception) {
            "[]"
        }
    }
}

class ExamenViewModelFactory(
    private val materiaRepository: MateriaRepository,
    private val estudioRepository: EstudioRepository,
    private val aiService:         AiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExamenViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ExamenViewModel(materiaRepository, estudioRepository, aiService) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}
