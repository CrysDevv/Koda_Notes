package app.kodanotes.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class AuthManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "kodanotes_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val normalPrefs = context.getSharedPreferences(
        "kodanotes_prefs", Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_API       = "ai_api_key"
        private const val KEY_MODELO    = "modelo_seleccionado"
        private const val KEY_INTERVALO = "auto_save_interval_sec"
        private const val KEY_NOMBRE    = "nombre_usuario"
        private const val DEFAULT_MODELO   = "gemini-2.5-flash"
        private const val DEFAULT_NOMBRE   = "Estudiante"

        const val INTERVALO_3_MIN  = 3  * 60
        const val INTERVALO_5_MIN  = 5  * 60
        const val INTERVALO_10_MIN = 10 * 60
        const val DEFAULT_INTERVALO = INTERVALO_5_MIN
    }

    fun guardarApiKey(apiKey: String) = prefs.edit().putString(KEY_API, apiKey).apply()
    fun obtenerApiKey(): String?      = prefs.getString(KEY_API, null) ?: prefs.getString("gemini_api_key", null)

    fun guardarModelo(modelo: String) = prefs.edit().putString(KEY_MODELO, modelo).apply()
    fun obtenerModelo(): String       = prefs.getString(KEY_MODELO, DEFAULT_MODELO) ?: DEFAULT_MODELO

    fun guardarNombre(nombre: String) =
        normalPrefs.edit().putString(KEY_NOMBRE, nombre.trim().ifBlank { DEFAULT_NOMBRE }).apply()

    fun obtenerNombre(): String =
        normalPrefs.getString(KEY_NOMBRE, DEFAULT_NOMBRE) ?: DEFAULT_NOMBRE

    /** Devuelve el intervalo en segundos configurado por el usuario (default 300). */
    fun obtenerIntervaloGuardado(): Int =
        normalPrefs.getInt(KEY_INTERVALO, DEFAULT_INTERVALO)

    /** Guarda la preferencia de intervalo elegida en Ajustes. */
    fun guardarIntervalo(segundos: Int) =
        normalPrefs.edit().putInt(KEY_INTERVALO, segundos).apply()
}