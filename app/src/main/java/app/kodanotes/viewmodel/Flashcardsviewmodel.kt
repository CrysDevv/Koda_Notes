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
    IDLE,
    GENERANDO,
    SESION,
    COMPLETADO,
    ERROR
}

class FlashcardsViewModel(
    private val materiaRepository: MateriaRepository,
    private val estudioRepository: EstudioRepository,
    private val geminiService:     GeminiService
) : ViewModel() {

    val materias: StateFlow<List<MateriaEntity>> = materiaRepository.todasLasMaterias
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _state = MutableStateFlow(FlashcardsState.IDLE)
    val state = _state.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg = _errorMsg.asStateFlow()

    private val _materiaSeleccionada = MutableStateFlow<MateriaEntity?>(null)
    val materiaSeleccionada = _materiaSeleccionada.asStateFlow()

    private val _notasDeMateriaSeleccionada = MutableStateFlow<List<NotaEntity>>(emptyList())
    val notasDeMateriaSeleccionada = _notasDeMateriaSeleccionada.asStateFlow()

    private val _notasSeleccionadas = MutableStateFlow<Set<Int>>(emptySet())
    val notasSeleccionadas = _notasSeleccionadas.asStateFlow()

    private val _cantidadCards = MutableStateFlow(10)
    val cantidadCards = _cantidadCards.asStateFlow()

    private val _colaCards = MutableStateFlow<List<FlashcardEntity>>(emptyList())
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

    private fun iniciarSesion(cards: List<FlashcardEntity>) {
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
     * Registra la respuesta del usuario a la card actual.
     * - 2 ("Lo sé"): se retira de la cola.
     * - 1 ("Más o menos"): se retira de la cola.
     * - 0 ("No lo sé"): vuelve a la cola.
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

    private fun parsearFlashcards(json: String, materiaId: Int): List<FlashcardEntity> {
        return try {
            val sesionId = System.currentTimeMillis()

            val cleanJson = json.replace("```json", "")
                .replace("```", "")
                .trim()

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