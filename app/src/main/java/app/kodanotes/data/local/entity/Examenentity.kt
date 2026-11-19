package app.kodanotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Resultado de un examen completado.
 *
 * preguntasJson guarda las preguntas y respuestas del examen en formato JSON
 * para poder mostrar el detalle completo en "Repasar respuestas incorrectas".
 */
@Entity(tableName = "tabla_examenes")
data class ExamenEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val materiaId: Int,
    val fecha: Long = System.currentTimeMillis(),
    val totalPreguntas: Int,
    val dificultad: String,
    val correctas:   Int,
    val incorrectas: Int,
    val omitidas:    Int,
    val nota: Float,
    val preguntasJson: String
)