package app.kodanotes.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tabla_materias")
data class MateriaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val colorHex: Long,
    val simbolo: String,
    val cantidadDocs: Int = 0
)