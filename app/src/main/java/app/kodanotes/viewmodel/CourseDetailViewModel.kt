package app.kodanotes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.kodanotes.data.local.entity.NotaEntity
import app.kodanotes.data.repository.MateriaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseDetailViewModel(
    private val repository: MateriaRepository,
    private val materiaId: Int
) : ViewModel() {

    val notas: StateFlow<List<NotaEntity>> = repository.obtenerNotasPorMateria(materiaId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun agregarNota(titulo: String, contenido: String) {
        viewModelScope.launch {
            repository.agregarNota(
                NotaEntity(materiaId = materiaId, titulo = titulo, contenido = contenido)
            )
        }
    }

    fun eliminarNota(nota: NotaEntity) {
        viewModelScope.launch {
            repository.eliminarNota(nota)
        }
    }
}

class CourseDetailViewModelFactory(
    private val repository: MateriaRepository,
    private val materiaId: Int
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CourseDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CourseDetailViewModel(repository, materiaId) as T
        }
        throw IllegalArgumentException("ViewModel desconocido")
    }
}