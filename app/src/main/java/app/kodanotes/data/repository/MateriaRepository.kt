package app.kodanotes.data.repository

import app.kodanotes.data.local.dao.MateriaDao
import app.kodanotes.data.local.entity.MateriaEntity
import app.kodanotes.data.local.entity.NotaEntity
import kotlinx.coroutines.flow.Flow

class MateriaRepository(
    private val dao: MateriaDao,
    private val estudioRepository: EstudioRepository? = null
) {

    val todasLasMaterias: Flow<List<MateriaEntity>> = dao.obtenerTodasLasMaterias()

    suspend fun agregarMateria(materia: MateriaEntity) = dao.insertarMateria(materia)

    suspend fun eliminarMateria(materia: MateriaEntity) {
        dao.borrarTodasLasNotasDe(materia.id)
        estudioRepository?.borrarFlashcardsDe(materia.id)
        estudioRepository?.borrarExamenesDe(materia.id)
        dao.borrarMateria(materia)
    }

    fun obtenerNotasPorMateria(materiaId: Int): Flow<List<NotaEntity>> =
        dao.obtenerNotasPorMateria(materiaId)

    fun obtenerNotasRecientes(limite: Int = 5): Flow<List<NotaEntity>> =
        dao.obtenerNotasRecientes(limite)

    /** Inserta una nota e incrementa el contador de docs de la materia. */
    suspend fun agregarNota(nota: NotaEntity) {
        dao.insertarNota(nota)
        dao.incrementarContadorDocs(nota.materiaId)
    }

    /** Inserta una nota sin modificar el contador de materias. */
    suspend fun agregarNotaSinContador(nota: NotaEntity) {
        dao.insertarNota(nota)
    }

    suspend fun eliminarNota(nota: NotaEntity) {
        dao.borrarNota(nota)
        dao.decrementarContadorDocs(nota.materiaId)
    }
}