package app.kodanotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una flashcard generada por la IA a partir de una o varias notas.
 *
 * vecesVista / vecesAcertada permiten calcular el porcentaje de dominio
 * por card para mostrar estadísticas en el futuro.
 */
@Entity(tableName = "tabla_flashcards")
data class FlashcardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // Materia a la que pertenece esta sesión de flashcards
    val materiaId: Int,

    // ID de sesión — agrupa las cards generadas en una misma llamada
    // (el usuario puede tener múltiples sesiones por materia)
    val sesionId: Long,

    // Contenido de la card
    val pregunta: String,
    val respuesta: String,

    // Metadatos de uso
    val fechaCreacion: Long = System.currentTimeMillis(),
    val vecesVista: Int    = 0,
    val vecesAcertada: Int = 0
)