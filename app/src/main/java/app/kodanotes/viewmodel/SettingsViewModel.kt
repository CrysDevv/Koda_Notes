package app.kodanotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.kodanotes.ai.GeminiService
import app.kodanotes.data.security.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authManager: AuthManager,
    private val geminiService: GeminiService
) : ViewModel() {

    private val _modelosDisponibles = MutableStateFlow(
        listOf(
            "gemini-2.5-flash-lite"  to "Gemini 2.5 Flash Lite  — Rápido, bajo consumo",
            "gemini-2.5-flash"       to "Gemini 2.5 Flash       — Equilibrado (recomendado)",
            "gemini-2.5-pro"         to "Gemini 2.5 Pro         — Máxima calidad"
        )
    )
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

    fun validarYGuardarLlave(nuevaLlave: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _isValidating.value    = true
                _validationError.value = null
                authManager.guardarApiKey(nuevaLlave)
                val esValida = geminiService.validarConexion()
                if (esValida) {
                    _apiKeyActual.value = nuevaLlave
                } else {
                    _validationError.value = "La API Key no respondió correctamente."
                }
            } catch (e: Exception) {
                _validationError.value = "Error de conexión: ${e.localizedMessage}"
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
    private val geminiService: GeminiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(authManager, geminiService) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}