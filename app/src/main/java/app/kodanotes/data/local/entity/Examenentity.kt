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

    // Fecha de realización (timestamp)
    val fecha: Long = System.currentTimeMillis(),

    // Configuración del examen
    val totalPreguntas: Int,
    val dificultad: String,   // "Fácil" | "Media" | "Difícil"

    // Resultados
    val correctas:   Int,
    val incorrectas: Int,
    val omitidas:    Int,

    // Nota sobre 10 calculada en el ViewModel (correctas / total * 10)
    val nota: Float,

    // JSON completo de preguntas + respuestas del usuario para poder repasar
    // Formato: [{"pregunta":"...","opciones":[],"correcta":"A","seleccionada":"B","explicacion":"..."}]
    val preguntasJson: String
)