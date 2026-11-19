package app.kodanotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una flashcard generada por la IA a partir de una o varias notas.
 *
 * vecesVista / vecesAcertada permiten calcular el porcentaje de dominio
 * por card para mostrar estadísticas.
 */
@Entity(tableName = "tabla_flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val materiaId: Int,
    val sesionId: Long,
    val pregunta: String,
    val respuesta: String,
    val fechaCreacion: Long = System.currentTimeMillis(),
    val vecesVista: Int    = 0,
    val vecesAcertada: Int = 0
)