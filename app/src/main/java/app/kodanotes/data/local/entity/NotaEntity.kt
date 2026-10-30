package app.kodanotes.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "tabla_notas",
    foreignKeys = [
        ForeignKey(
            entity = MateriaEntity::class,
            parentColumns = ["id"],
            childColumns = ["materiaId"],
            onDelete = ForeignKey.CASCADE // Si borras la materia, se borran sus notas automáticamente
        )
    ]
)
data class NotaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val materiaId: Int, // Relación con la materia
    val titulo: String,
    val contenido: String,
    val fecha: Long = System.currentTimeMillis()
)