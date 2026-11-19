package app.kodanotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.kodanotes.data.local.entity.MateriaEntity
import app.kodanotes.data.repository.MateriaRepository
import app.kodanotes.data.security.AuthManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: MateriaRepository,
    private val authManager: AuthManager
) : ViewModel() {

    val materias: StateFlow<List<MateriaEntity>> = repository.todasLasMaterias
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _nombreUsuario = MutableStateFlow(authManager.obtenerNombre())
    val nombreUsuario: StateFlow<String> = _nombreUsuario.asStateFlow()

    fun refrescarNombre() {
        _nombreUsuario.value = authManager.obtenerNombre()
    }

    fun agregarNuevaMateria(nombre: String, simbolo: String, colorHex: Long) {
        viewModelScope.launch {
            repository.agregarMateria(
                MateriaEntity(nombre = nombre, simbolo = simbolo, colorHex = colorHex, cantidadDocs = 0)
            )
        }
    }

    fun eliminarMateria(materia: MateriaEntity) {
        viewModelScope.launch {
            repository.eliminarMateria(materia)
        }
    }
}

class HomeViewModelFactory(
    private val repository: MateriaRepository,
    private val authManager: AuthManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HomeViewModel(repository, authManager) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}