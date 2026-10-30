package app.kodanotes.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.kodanotes.data.local.entity.ExamenEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamenDao {

    // ── Inserción ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarExamen(examen: ExamenEntity): Long

    // ── Consultas ─────────────────────────────────────────────────────────────

    /** Historial completo de una materia, del más reciente al más antiguo */
    @Query("SELECT * FROM tabla_examenes WHERE materiaId = :materiaId ORDER BY fecha DESC")
    fun obtenerHistorialPorMateria(materiaId: Int): Flow<List<ExamenEntity>>

    /** Un examen concreto por ID (para pantalla de repaso) */
    @Query("SELECT * FROM tabla_examenes WHERE id = :examenId")
    suspend fun obtenerExamenPorId(examenId: Int): ExamenEntity?

    /** Mejor nota histórica de una materia */
    @Query("SELECT MAX(nota) FROM tabla_examenes WHERE materiaId = :materiaId")
    suspend fun mejorNota(materiaId: Int): Float?

    /** Promedio de notas de una materia */
    @Query("SELECT AVG(nota) FROM tabla_examenes WHERE materiaId = :materiaId")
    suspend fun promedioNotas(materiaId: Int): Float?

    // ── Borrado ───────────────────────────────────────────────────────────────

    @Query("DELETE FROM tabla_examenes WHERE id = :examenId")
    suspend fun borrarExamen(examenId: Int)

    @Query("DELETE FROM tabla_examenes WHERE materiaId = :materiaId")
    suspend fun borrarExamenesDe(materiaId: Int)
}