package app.kodanotes.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.kodanotes.data.local.entity.MateriaEntity
import app.kodanotes.data.local.entity.NotaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MateriaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarMateria(materia: MateriaEntity)

    @Delete
    suspend fun borrarMateria(materia: MateriaEntity)

    @Query("SELECT * FROM tabla_materias ORDER BY nombre ASC")
    fun obtenerTodasLasMaterias(): Flow<List<MateriaEntity>>

    @Query("SELECT * FROM tabla_materias WHERE id = :id")
    suspend fun obtenerMateriaPorId(id: Int): MateriaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarNota(nota: NotaEntity)

    @Delete
    suspend fun borrarNota(nota: NotaEntity)

    @Query("DELETE FROM tabla_notas WHERE materiaId = :materiaId")
    suspend fun borrarTodasLasNotasDe(materiaId: Int)

    @Query("SELECT * FROM tabla_notas WHERE materiaId = :materiaId ORDER BY fecha DESC")
    fun obtenerNotasPorMateria(materiaId: Int): Flow<List<NotaEntity>>

    @Query("SELECT * FROM tabla_notas ORDER BY fecha DESC LIMIT :limite")
    fun obtenerNotasRecientes(limite: Int = 5): Flow<List<NotaEntity>>

    @Query("UPDATE tabla_materias SET cantidadDocs = cantidadDocs + 1 WHERE id = :materiaId")
    suspend fun incrementarContadorDocs(materiaId: Int)

    @Query("UPDATE tabla_materias SET cantidadDocs = MAX(0, cantidadDocs - 1) WHERE id = :materiaId")
    suspend fun decrementarContadorDocs(materiaId: Int)
}