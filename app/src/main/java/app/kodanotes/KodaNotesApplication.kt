package app.kodanotes

import android.app.Application
import app.kodanotes.data.audio.AudioRecorder
import app.kodanotes.data.local.KodaNotesDatabase
import app.kodanotes.data.repository.EstudioRepository
import app.kodanotes.data.repository.MateriaRepository
import app.kodanotes.data.security.AuthManager

class KodaNotesApplication : Application() {

    val database          by lazy { KodaNotesDatabase.getDatabase(this) }
    val estudioRepository by lazy {
        EstudioRepository(database.flashcardDao(), database.examenDao())
    }
    val repository        by lazy {
        MateriaRepository(database.materiaDao(), estudioRepository)
    }
    val authManager       by lazy { AuthManager(this) }

    val audioRecorder     by lazy { AudioRecorder() }
}