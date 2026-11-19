package app.kodanotes.data.repository

import app.kodanotes.data.local.dao.ExamenDao
import app.kodanotes.data.local.dao.FlashcardDao
import app.kodanotes.data.local.dao.SesionResumen
import app.kodanotes.data.local.entity.ExamenEntity
import app.kodanotes.data.local.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow

class EstudioRepository(
    private val flashcardDao: FlashcardDao,
    private val examenDao:    ExamenDao
) {

    /** Guarda un lote de cards generadas por la IA en una misma sesión. */
    suspend fun guardarFlashcards(cards: List<FlashcardEntity>) {
        flashcardDao.insertarFlashcards(cards)
    }

    /** Sesiones disponibles de una materia para mostrar en el listado. */
    fun obtenerSesionesPorMateria(materiaId: Int): Flow<List<SesionResumen>> =
        flashcardDao.obtenerSesionesPorMateria(materiaId)

    /** Cards de una sesión concreta para iniciar la sesión de estudio. */
    suspend fun obtenerCardsDeSesion(sesionId: Long): List<FlashcardEntity> =
        flashcardDao.obtenerCardsDeSesion(sesionId)

    /** Registra si el usuario acertó o no una card (actualiza contadores). */
    suspend fun registrarIntento(cardId: Int, acerto: Boolean) {
        flashcardDao.registrarIntento(cardId, if (acerto) 1 else 0)
    }

    /** Elimina una sesión completa de flashcards. */
    suspend fun borrarSesion(sesionId: Long) {
        flashcardDao.borrarSesion(sesionId)
    }

    /** Elimina todas las flashcards de una materia. */
    suspend fun borrarFlashcardsDe(materiaId: Int) {
        flashcardDao.borrarFlashcardsDe(materiaId)
    }

    /** Guarda el resultado completo de un examen terminado. */
    suspend fun guardarExamen(examen: ExamenEntity): Long =
        examenDao.insertarExamen(examen)

    /** Historial de exámenes de una materia para la pantalla de Exámenes. */
    fun obtenerHistorialPorMateria(materiaId: Int): Flow<List<ExamenEntity>> =
        examenDao.obtenerHistorialPorMateria(materiaId)

    /** Un examen concreto para la pantalla de repaso de errores. */
    suspend fun obtenerExamenPorId(examenId: Int): ExamenEntity? =
        examenDao.obtenerExamenPorId(examenId)

    /** Mejor nota histórica de una materia. */
    suspend fun mejorNota(materiaId: Int): Float? =
        examenDao.mejorNota(materiaId)

    /** Promedio de notas de una materia. */
    suspend fun promedioNotas(materiaId: Int): Float? =
        examenDao.promedioNotas(materiaId)

    /** Elimina un examen del historial. */
    suspend fun borrarExamen(examenId: Int) {
        examenDao.borrarExamen(examenId)
    }

    /** Elimina todos los exámenes de una materia. */
    suspend fun borrarExamenesDe(materiaId: Int) {
        examenDao.borrarExamenesDe(materiaId)
    }
}