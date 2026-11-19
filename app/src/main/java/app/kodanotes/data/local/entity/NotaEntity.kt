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
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NotaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val materiaId: Int,
    val titulo: String,
    val contenido: String,
    val fecha: Long = System.currentTimeMillis()
)