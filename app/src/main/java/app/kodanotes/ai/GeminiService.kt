package app.kodanotes.ai

import android.util.Base64
import android.util.Log
import app.kodanotes.data.security.AuthManager
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.File
import java.util.concurrent.TimeUnit

interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generarContenido(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @GET("v1beta/models")
    suspend fun listarModelos(@Query("key") apiKey: String): ModelListResponse
}

data class ModelListResponse(val models: List<ModelInfo>)
data class ModelInfo(val name: String, val displayName: String, val supportedGenerationMethods: List<String>)
data class GeminiRequest(val contents: List<Content>, val systemInstruction: Content? = null)
data class Content(val parts: List<Part>)
data class Part(val text: String? = null, val inlineData: InlineData? = null)
data class InlineData(val mimeType: String, val data: String)
data class GeminiResponse(val candidates: List<Candidate>? = null, val error: GeminiError? = null)
data class Candidate(val content: Content)
data class GeminiError(val message: String)

class GeminiService(private val authManager: AuthManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(300, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .build()

    private val api = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeminiApi::class.java)

    /** Verifica si hay una API key configurada sin hacer llamadas de red. */
    fun tieneApiKey(): Boolean = !authManager.obtenerApiKey().isNullOrBlank()

    /** Valida los credenciales e invoca la API de Gemini para generar respuesta al prompt dado. */
    suspend fun validarYGenerar(prompt: String): String? {
        val key = authManager.obtenerApiKey()
        var modelo = authManager.obtenerModelo()
        if (key.isNullOrBlank()) return "Error: Falta API Key"
        if (modelo.isEmpty()) modelo = "gemini-2.5-flash-lite"

        val request = GeminiRequest(listOf(Content(listOf(Part(text = prompt)))))
        val maxReintentos = 3
        var reintentoActual = 0
        var tiempoEsperaMs = 2000L

        while (reintentoActual < maxReintentos) {
            try {
                val response = api.generarContenido(modelo, key, request)
                return if (response.error != null) {
                    "Error de la IA: ${response.error.message}"
                } else {
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                }
            } catch (e: HttpException) {
                if (e.code() == 503 || e.code() == 429) {
                    Log.w("GEMINI_REST", "Error ${e.code()}. Reintentando en ${tiempoEsperaMs}ms...")
                    delay(tiempoEsperaMs)
                    reintentoActual++
                    tiempoEsperaMs *= 2
                } else {
                    Log.e("GEMINI_REST", "Error HTTP: ${e.code()} - ${e.message()}")
                    return "Error de servidor: ${e.code()}"
                }
            } catch (e: Exception) {
                Log.e("GEMINI_REST", "Fallo de red: ${e.message}")
                return "Error de conexión: ${e.localizedMessage}"
            }
        }
        return "Error: Los servidores de la IA están ocupados. Intenta de nuevo más tarde."
    }

    /**
     * Transcribe un archivo WAV enviándolo a Gemini como audio inline.
     * El archivo se codifica en Base64 en bloques de 8KB.
     * Devuelve el texto transcrito o null si hay error.
     */
    suspend fun transcribirAudio(audioFile: File): String? {
        val key    = authManager.obtenerApiKey()
        val modelo = authManager.obtenerModelo().ifEmpty { "gemini-3-flash-preview" }
        if (key.isNullOrBlank()) return null
        if (!audioFile.exists() || audioFile.length() == 0L) return null

        return try {
            val base64 = audioFile.inputStream().use { stream ->
                val buffer = ByteArray(8 * 1024)
                val sb     = StringBuilder((audioFile.length() * 1.37).toInt())
                var read: Int
                while (stream.read(buffer).also { read = it } != -1) {
                    sb.append(Base64.encodeToString(buffer, 0, read, Base64.NO_WRAP))
                }
                sb.toString()
            }
            val request = GeminiRequest(
                contents = listOf(Content(listOf(
                    Part(inlineData = InlineData(mimeType = "audio/wav", data = base64)),
                    Part(text = AiPrompts.TRANSCRIPCION_CHUNK)
                )))
            )
            val response = api.generarContenido(modelo, key, request)
            if (response.error != null) {
                Log.e("GEMINI_AUDIO", "Error: ${response.error.message}")
                null
            } else {
                response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            }
        } catch (e: HttpException) {
            Log.e("GEMINI_AUDIO", "HTTP ${e.code()}: ${e.response()?.errorBody()?.string()}")
            null
        } catch (e: Exception) {
            Log.e("GEMINI_AUDIO", "Error: ${e.message}")
            null
        }
    }

    /** Comprueba si la conexión con Gemini funciona correctamente enviando un prompt de prueba. */
    suspend fun validarConexion(): Boolean {
        val respuesta = validarYGenerar(AiPrompts.VALIDAR_CONEXION)
        return respuesta != null && (respuesta.contains("OK", ignoreCase = true) || respuesta.length > 2)
    }

    /** Obtiene el listado de modelos disponibles en la API de Gemini. */
    suspend fun obtenerModelosDisponibles(): List<Pair<String, String>> {
        val key = authManager.obtenerApiKey() ?: return emptyList()
        return try {
            api.listarModelos(key).models
                .filter { it.supportedGenerationMethods.contains("generateContent") }
                .map { it.name.replace("models/", "") to it.displayName }
        } catch (e: Exception) { emptyList() }
    }
}