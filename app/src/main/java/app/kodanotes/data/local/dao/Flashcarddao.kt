package app.kodanotes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.kodanotes.data.local.entity.FlashcardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {

    // ── Inserción ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarFlashcards(cards: List<FlashcardEntity>)

    // ── Consultas ─────────────────────────────────────────────────────────────

    /** Todas las sesiones distintas de una materia, ordenadas por más reciente */
    @Query("""
        SELECT DISTINCT sesionId, materiaId, fechaCreacion
        FROM tabla_flashcards
        WHERE materiaId = :materiaId
        ORDER BY fechaCreacion DESC
    """)
    fun obtenerSesionesPorMateria(materiaId: Int): Flow<List<SesionResumen>>

    /** Cards de una sesión específica */
    @Query("SELECT * FROM tabla_flashcards WHERE sesionId = :sesionId ORDER BY id ASC")
    suspend fun obtenerCardsDeSesion(sesionId: Long): List<FlashcardEntity>

    /** Cards de una materia completa (para estadísticas) */
    @Query("SELECT * FROM tabla_flashcards WHERE materiaId = :materiaId ORDER BY fechaCreacion DESC")
    fun obtenerCardsPorMateria(materiaId: Int): Flow<List<FlashcardEntity>>

    // ── Actualización ─────────────────────────────────────────────────────────

    /** Incrementa contadores de veces vista y veces acertada */
    @Query("""
        UPDATE tabla_flashcards
        SET vecesVista    = vecesVista + 1,
            vecesAcertada = vecesAcertada + :acerto
        WHERE id = :cardId
    """)
    suspend fun registrarIntento(cardId: Int, acerto: Int)

    // ── Borrado ───────────────────────────────────────────────────────────────

    @Query("DELETE FROM tabla_flashcards WHERE sesionId = :sesionId")
    suspend fun borrarSesion(sesionId: Long)

    @Query("DELETE FROM tabla_flashcards WHERE materiaId = :materiaId")
    suspend fun borrarFlashcardsDe(materiaId: Int)
}

/**
 * Proyección ligera para listar sesiones sin cargar todas las cards.
 * Room mapea esto automáticamente desde la query.
 */
data class SesionResumen(
    val sesionId:       Long,
    val materiaId:      Int,
    val fechaCreacion:  Long
)