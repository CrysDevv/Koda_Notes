package app.kodanotes.data.repository

import app.kodanotes.data.local.dao.MateriaDao
import app.kodanotes.data.local.entity.MateriaEntity
import app.kodanotes.data.local.entity.NotaEntity
import kotlinx.coroutines.flow.Flow

class MateriaRepository(
    private val dao: MateriaDao,
    private val estudioRepository: EstudioRepository? = null
) {

    // ── Materias ──────────────────────────────────────────────────────────────

    val todasLasMaterias: Flow<List<MateriaEntity>> = dao.obtenerTodasLasMaterias()

    suspend fun agregarMateria(materia: MateriaEntity) = dao.insertarMateria(materia)

    suspend fun eliminarMateria(materia: MateriaEntity) {
        dao.borrarTodasLasNotasDe(materia.id)
        // Cascade manual a flashcards y exámenes si el repositorio está disponible
        estudioRepository?.borrarFlashcardsDe(materia.id)
        estudioRepository?.borrarExamenesDe(materia.id)
        dao.borrarMateria(materia)
    }

    // ── Notas ─────────────────────────────────────────────────────────────────

    fun obtenerNotasPorMateria(materiaId: Int): Flow<List<NotaEntity>> =
        dao.obtenerNotasPorMateria(materiaId)

    fun obtenerNotasRecientes(limite: Int = 5): Flow<List<NotaEntity>> =
        dao.obtenerNotasRecientes(limite)

    /** Inserta una nota E incrementa el contador de docs de la materia (1 clase = 1 conteo). */
    suspend fun agregarNota(nota: NotaEntity) {
        dao.insertarNota(nota)
        dao.incrementarContadorDocs(nota.materiaId)
    }

    /**
     * Inserta una nota SIN tocar el contador.
     * Usar para las notas de segmento intermedias de una clase larga:
     * el contador solo se incrementa una vez por grabación completa,
     * desde agregarNota() al guardar el resumen global o la nota final.
     */
    suspend fun agregarNotaSinContador(nota: NotaEntity) {
        dao.insertarNota(nota)
    }

    suspend fun eliminarNota(nota: NotaEntity) {
        dao.borrarNota(nota)
        dao.decrementarContadorDocs(nota.materiaId)
    }
}