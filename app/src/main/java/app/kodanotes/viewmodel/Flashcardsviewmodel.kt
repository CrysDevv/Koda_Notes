package app.kodanotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.kodanotes.ai.AiPrompts
import app.kodanotes.ai.GeminiService
import app.kodanotes.data.local.entity.FlashcardEntity
import app.kodanotes.data.local.entity.MateriaEntity
import app.kodanotes.data.local.entity.NotaEntity
import app.kodanotes.data.repository.EstudioRepository
import app.kodanotes.data.repository.MateriaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray

enum class FlashcardsState {
    IDLE,           // Pantalla de configuración
    GENERANDO,      // Llamada a Gemini en curso
    SESION,         // Sesión de estudio activa
    COMPLETADO,     // Todas las cards vistas al menos una vez
    ERROR
}

class FlashcardsViewModel(
    private val materiaRepository: MateriaRepository,
    private val estudioRepository: EstudioRepository,
    private val geminiService:     GeminiService
) : ViewModel() {

    // ── States públicos ───────────────────────────────────────────────────────

    val materias: StateFlow<List<MateriaEntity>> = materiaRepository.todasLasMaterias
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _state = MutableStateFlow(FlashcardsState.IDLE)
    val state = _state.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg = _errorMsg.asStateFlow()

    // ── Configuración ─────────────────────────────────────────────────────────

    private val _materiaSeleccionada = MutableStateFlow<MateriaEntity?>(null)
    val materiaSeleccionada = _materiaSeleccionada.asStateFlow()

    private val _notasDeMateriaSeleccionada = MutableStateFlow<List<NotaEntity>>(emptyList())
    val notasDeMateriaSeleccionada = _notasDeMateriaSeleccionada.asStateFlow()

    private val _notasSeleccionadas = MutableStateFlow<Set<Int>>(emptySet())
    val notasSeleccionadas = _notasSeleccionadas.asStateFlow()

    private val _cantidadCards = MutableStateFlow(10)
    val cantidadCards = _cantidadCards.asStateFlow()

    // ── Sesión activa ─────────────────────────────────────────────────────────

    // Cola de cards en juego (se mezcla cuando hay "No lo sé")
    private val _colaCards = MutableStateFlow<List<FlashcardEntity>>(emptyList())

    // FIX #5: Guardar las cards originales para poder reiniciar la sesión
    // correctamente. Cuando el estado es COMPLETADO la cola ya está vacía
    // porque todas las cards fueron retiradas, así que reiniciarSesion()
    // necesita esta copia para reconstruirla.
    private var cardsOriginales: List<FlashcardEntity> = emptyList()

    private val _indiceActual = MutableStateFlow(0)
    val indiceActual = _indiceActual.asStateFlow()

    private val _cardActual = MutableStateFlow<FlashcardEntity?>(null)
    val cardActual = _cardActual.asStateFlow()

    private val _contadorSe      = MutableStateFlow(0)
    val contadorSe      = _contadorSe.asStateFlow()

    private val _contadorMasMenos = MutableStateFlow(0)
    val contadorMasMenos = _contadorMasMenos.asStateFlow()

    private val _contadorNoSe    = MutableStateFlow(0)
    val contadorNoSe    = _contadorNoSe.asStateFlow()

    private val _totalCards = MutableStateFlow(0)
    val totalCards = _totalCards.asStateFlow()

    // ─────────────────────────────────────────────────────────────────────────
    // Configuración
    // ─────────────────────────────────────────────────────────────────────────

    fun seleccionarMateria(materia: MateriaEntity) {
        _materiaSeleccionada.value  = materia
        _notasSeleccionadas.value   = emptySet()
        _notasDeMateriaSeleccionada.value = emptyList()

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

    fun setCantidadCards(cantidad: Int) {
        _cantidadCards.value = cantidad.coerceIn(5, 20)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Generación
    // ─────────────────────────────────────────────────────────────────────────

    fun generarFlashcards() {
        val notasIds = _notasSeleccionadas.value
        val materia  = _materiaSeleccionada.value ?: return
        if (notasIds.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _state.value    = FlashcardsState.GENERANDO
            _errorMsg.value = null

            val contenido = _notasDeMateriaSeleccionada.value
                .filter { it.id in notasIds }
                .joinToString("\n\n---\n\n") { it.contenido }

            val prompt    = AiPrompts.flashcards(contenido, _cantidadCards.value)
            val respuesta = geminiService.validarYGenerar(prompt)

            if (respuesta == null || respuesta.startsWith("Error")) {
                _errorMsg.value = respuesta ?: "Error al conectar con la IA"
                _state.value    = FlashcardsState.ERROR
                return@launch
            }

            val cards = parsearFlashcards(respuesta, materia.id)
            if (cards.isEmpty()) {
                _errorMsg.value = "La IA no devolvió flashcards válidas"
                _state.value    = FlashcardsState.ERROR
                return@launch
            }

            estudioRepository.guardarFlashcards(cards)
            iniciarSesion(cards)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Sesión de estudio
    // ─────────────────────────────────────────────────────────────────────────

    private fun iniciarSesion(cards: List<FlashcardEntity>) {
        // FIX #5: Guardar copia de las cards originales antes de mezclarlas
        cardsOriginales       = cards
        _colaCards.value      = cards.shuffled()
        _indiceActual.value   = 0
        _contadorSe.value     = 0
        _contadorMasMenos.value = 0
        _contadorNoSe.value   = 0
        _totalCards.value     = cards.size
        _cardActual.value     = _colaCards.value.firstOrNull()
        _state.value          = FlashcardsState.SESION
    }

    /**
     * El usuario responde a la card actual.
     * - "Lo sé" (2)       → se retira de la cola, suma al contador verde
     * - "Más o menos" (1) → se retira de la cola, suma al contador gris
     * - "No lo sé" (0)    → vuelve a la cola en posición aleatoria
     */
    fun responder(resultado: Int) {
        val cola   = _colaCards.value.toMutableList()
        val indice = _indiceActual.value
        val card   = cola.getOrNull(indice) ?: return

        viewModelScope.launch(Dispatchers.IO) {
            estudioRepository.registrarIntento(card.id, resultado == 2)
        }

        when (resultado) {
            2 -> {
                cola.removeAt(indice)
                _contadorSe.value++
            }
            1 -> {
                cola.removeAt(indice)
                _contadorMasMenos.value++
            }
            0 -> {
                cola.removeAt(indice)
                _contadorNoSe.value++
                if (cola.isNotEmpty()) {
                    val nuevaPosicion = (1..cola.size).random()
                    cola.add(nuevaPosicion, card)
                } else {
                    cola.add(card)
                }
            }
        }

        _colaCards.value = cola

        if (cola.isEmpty()) {
            _state.value      = FlashcardsState.COMPLETADO
            _cardActual.value = null
            return
        }

        val nuevoIndice     = indice.coerceAtMost(cola.size - 1)
        _indiceActual.value = nuevoIndice
        _cardActual.value   = cola[nuevoIndice]
    }

    fun saltarCard() {
        val cola = _colaCards.value
        if (cola.isEmpty()) return
        val siguiente   = (_indiceActual.value + 1) % cola.size
        _indiceActual.value = siguiente
        _cardActual.value   = cola[siguiente]
    }

    // FIX #5: Usar cardsOriginales en lugar de _colaCards (que ya está vacía
    // en estado COMPLETADO porque todas las cards fueron retiradas).
    fun reiniciarSesion() {
        if (cardsOriginales.isEmpty()) return
        iniciarSesion(cardsOriginales)
    }

    fun volverAConfigurar() {
        _state.value              = FlashcardsState.IDLE
        _cardActual.value         = null
        _colaCards.value          = emptyList()
        cardsOriginales           = emptyList()
        _notasSeleccionadas.value = emptySet()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Parser JSON
    // ─────────────────────────────────────────────────────────────────────────

    private fun parsearFlashcards(json: String, materiaId: Int): List<FlashcardEntity> {
        return try {
            val sesionId = System.currentTimeMillis()

            // 1. Limpiamos las etiquetas markdown que la IA suele añadir
            val cleanJson = json.replace("```json", "")
                .replace("```", "")
                .trim()

            // 2. Ahora sí parseamos el JSON limpio
            val array = JSONArray(cleanJson)

            (0 until array.length()).mapNotNull { i ->
                val obj       = array.getJSONObject(i)
                val pregunta  = obj.optString("pregunta").trim()
                val respuesta = obj.optString("respuesta").trim()
                if (pregunta.isBlank() || respuesta.isBlank()) null
                else FlashcardEntity(
                    materiaId = materiaId,
                    sesionId  = sesionId,
                    pregunta  = pregunta,
                    respuesta = respuesta
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

class FlashcardsViewModelFactory(
    private val materiaRepository: MateriaRepository,
    private val estudioRepository: EstudioRepository,
    private val geminiService:     GeminiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FlashcardsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FlashcardsViewModel(materiaRepository, estudioRepository, geminiService) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}