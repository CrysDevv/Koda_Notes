package app.kodanotes.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.kodanotes.data.local.dao.ExamenDao
import app.kodanotes.data.local.dao.FlashcardDao
import app.kodanotes.data.local.dao.MateriaDao
import app.kodanotes.data.local.entity.ExamenEntity
import app.kodanotes.data.local.entity.FlashcardEntity
import app.kodanotes.data.local.entity.MateriaEntity
import app.kodanotes.data.local.entity.NotaEntity

@Database(
    entities = [
        MateriaEntity::class,
        NotaEntity::class,
        FlashcardEntity::class,
        ExamenEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class KodaNotesDatabase : RoomDatabase() {

    abstract fun materiaDao():   MateriaDao
    abstract fun flashcardDao(): FlashcardDao
    abstract fun examenDao():    ExamenDao

    companion object {
        @Volatile
        private var INSTANCE: KodaNotesDatabase? = null

        fun getDatabase(context: Context): KodaNotesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KodaNotesDatabase::class.java,
                    "kodanotes_db"
                )
                    .fallbackToDestructiveMigration(true)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}