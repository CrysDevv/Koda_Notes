package app.kodanotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.kodanotes.ai.AiService
import app.kodanotes.data.security.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authManager: AuthManager,
    private val aiService: AiService
) : ViewModel() {

    private val _modelosDisponibles = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val modelosDisponibles = _modelosDisponibles.asStateFlow()

    private val _apiKeyActual = MutableStateFlow(authManager.obtenerApiKey() ?: "")
    val apiKeyActual: StateFlow<String> = _apiKeyActual.asStateFlow()

    private val _modeloSeleccionado = MutableStateFlow(authManager.obtenerModelo())
    val modeloSeleccionado: StateFlow<String> = _modeloSeleccionado.asStateFlow()

    private val _isValidating = MutableStateFlow(false)
    val isValidating = _isValidating.asStateFlow()

    private val _validationError = MutableStateFlow<String?>(null)
    val validationError = _validationError.asStateFlow()

    private val _nombreUsuario = MutableStateFlow(authManager.obtenerNombre())
    val nombreUsuario: StateFlow<String> = _nombreUsuario.asStateFlow()

    init {
        cargarModelosIniciales()
    }

    private fun cargarModelosIniciales() {
        val key = _apiKeyActual.value
        if (key.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                val modelos = aiService.obtenerModelosDisponibles(key)
                if (modelos.isNotEmpty()) {
                    _modelosDisponibles.value = modelos
                    val modeloActual = _modeloSeleccionado.value
                    if (modelos.none { it.first == modeloActual }) {
                        val nuevoModelo = modelos.first().first
                        authManager.guardarModelo(nuevoModelo)
                        _modeloSeleccionado.value = nuevoModelo
                    }
                }
            }
        }
    }

    fun guardarNombre(nombre: String) {
        authManager.guardarNombre(nombre)
        _nombreUsuario.value = authManager.obtenerNombre()
    }

    val opcionesIntervalo = listOf(
        AuthManager.INTERVALO_3_MIN  to "3 minutos  — Clases densas",
        AuthManager.INTERVALO_5_MIN  to "5 minutos  — Equilibrado (recomendado)",
        AuthManager.INTERVALO_10_MIN to "10 minutos — Ahorra tokens"
    )

    private val _intervaloSeleccionado = MutableStateFlow(authManager.obtenerIntervaloGuardado())
    val intervaloSeleccionado: StateFlow<Int> = _intervaloSeleccionado.asStateFlow()

    fun guardarNuevoModelo(id: String) {
        authManager.guardarModelo(id)
        _modeloSeleccionado.value = id
    }

    fun validarYGuardarLlave(nuevaLlave: String, onSuccess: () -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isValidating.value    = true
                _validationError.value = null

                val keyLimpia = nuevaLlave.trim()
                val modelos = aiService.obtenerModelosDisponibles(keyLimpia)

                if (modelos.isNotEmpty()) {
                    authManager.guardarApiKey(keyLimpia)
                    _apiKeyActual.value = keyLimpia
                    _modelosDisponibles.value = modelos

                    val modeloActual = _modeloSeleccionado.value
                    val existe = modelos.any { it.first == modeloActual }
                    if (!existe) {
                        val nuevoModelo = modelos.first().first
                        authManager.guardarModelo(nuevoModelo)
                        _modeloSeleccionado.value = nuevoModelo
                    }
                    _validationError.value = null
                    onSuccess()
                } else {
                    _validationError.value = "La API Key ingresada no es válida o no se pudieron obtener modelos asociados a ella."
                }
            } catch (e: Exception) {
                _validationError.value = "Error al validar la clave: ${e.localizedMessage}"
            } finally {
                _isValidating.value = false
            }
        }
    }

    /** Guarda el intervalo elegido y actualiza el StateFlow. */
    fun guardarIntervalo(segundos: Int) {
        authManager.guardarIntervalo(segundos)
        _intervaloSeleccionado.value = segundos
    }
}

class SettingsViewModelFactory(
    private val authManager: AuthManager,
    private val aiService: AiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(authManager, aiService) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}
