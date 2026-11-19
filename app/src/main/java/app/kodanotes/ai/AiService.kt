package app.kodanotes.ai

import android.util.Base64
import android.util.Log
import app.kodanotes.data.security.AuthManager
import kotlinx.coroutines.delay
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part as RetrofitPart
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.File
import java.util.concurrent.TimeUnit

// --- Gemini API ---
interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generarContenido(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @GET("v1beta/models")
    suspend fun listarModelos(@Query("key") apiKey: String): GeminiModelListResponse
}

data class GeminiModelListResponse(val models: List<GeminiModelInfo>? = null)
data class GeminiModelInfo(
    val name: String,
    val displayName: String? = null,
    val description: String? = null,
    val supportedGenerationMethods: List<String>? = null
)
data class GeminiRequest(val contents: List<GeminiContent>, val systemInstruction: GeminiContent? = null)
data class GeminiContent(val parts: List<GeminiPart>)
data class GeminiPart(val text: String? = null, val inlineData: InlineData? = null)
data class InlineData(val mimeType: String, val data: String)
data class GeminiResponse(val candidates: List<GeminiCandidate>? = null, val error: GeminiError? = null)
data class GeminiCandidate(val content: GeminiContent)
data class GeminiError(val message: String)

// --- OpenAI API ---
interface OpenAiApi {
    @GET("v1/models")
    suspend fun listarModelos(@Header("Authorization") authHeader: String): OpenAiModelListResponse

    @POST("v1/chat/completions")
    suspend fun generarContenido(
        @Header("Authorization") authHeader: String,
        @Body request: OpenAiChatRequest
    ): OpenAiChatResponse

    @Multipart
    @POST("v1/audio/transcriptions")
    suspend fun transcribirAudio(
        @Header("Authorization") authHeader: String,
        @RetrofitPart file: MultipartBody.Part,
        @RetrofitPart("model") model: RequestBody
    ): OpenAiTranscriptionResponse
}

data class OpenAiModelListResponse(val data: List<OpenAiModelInfo>? = null)
data class OpenAiModelInfo(val id: String, val objectType: String? = null)
data class OpenAiChatRequest(val model: String, val messages: List<OpenAiMessage>)
data class OpenAiMessage(val role: String, val content: String)
data class OpenAiChatResponse(val choices: List<OpenAiChoice>? = null, val error: OpenAiError? = null)
data class OpenAiChoice(val message: OpenAiMessage)
data class OpenAiError(val message: String)
data class OpenAiTranscriptionResponse(val text: String? = null)

class AiService(private val authManager: AuthManager) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(300, TimeUnit.SECONDS)
        .readTimeout(300, TimeUnit.SECONDS)
        .writeTimeout(300, TimeUnit.SECONDS)
        .build()

    private val geminiApi: GeminiApi = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(GeminiApi::class.java)

    private val openAiApi: OpenAiApi = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenAiApi::class.java)

    /** Verifica si hay una API key configurada localmente. */
    fun tieneApiKey(): Boolean = !authManager.obtenerApiKey().isNullOrBlank()

    /** Obtiene la lista real de modelos disponibles consultando a la API según la clave proporcionada. */
    suspend fun obtenerModelosDisponibles(apiKey: String? = null): List<Pair<String, String>> {
        val key = (apiKey ?: authManager.obtenerApiKey())?.trim() ?: return emptyList()
        if (key.isBlank()) return emptyList()

        val esOpenAiKey = key.startsWith("sk-", ignoreCase = true)

        if (esOpenAiKey) {
            try {
                val response = openAiApi.listarModelos("Bearer $key")
                val modelos = response.data
                    ?.map { it.id }
                    ?.filter { id -> id.startsWith("gpt-") || id.startsWith("o1") || id.startsWith("o3") }
                    ?.sorted()
                    ?.map { id -> id to "OpenAI $id" }
                if (!modelos.isNullOrEmpty()) return modelos
            } catch (e: Exception) {
                Log.e("AI_SERVICE", "Error obteniendo modelos OpenAI: ${e.message}")
            }
        }

        // Probar servicio Gemini de Google
        try {
            val response = geminiApi.listarModelos(key)
            val modelos = response.models
                ?.filter { it.supportedGenerationMethods?.contains("generateContent") == true }
                ?.map { model ->
                    val cleanId = model.name.removePrefix("models/")
                    val displayName = model.displayName?.ifBlank { cleanId } ?: cleanId
                    cleanId to displayName
                }
            if (!modelos.isNullOrEmpty()) return modelos
        } catch (e: Exception) {
            Log.e("AI_SERVICE", "Error obteniendo modelos Gemini: ${e.message}")
        }

        // Si la clave no empezaba con sk- pero falló en Gemini, probar OpenAI
        if (!esOpenAiKey) {
            try {
                val response = openAiApi.listarModelos("Bearer $key")
                val modelos = response.data
                    ?.map { it.id }
                    ?.filter { id -> id.startsWith("gpt-") || id.startsWith("o1") || id.startsWith("o3") }
                    ?.sorted()
                    ?.map { id -> id to "OpenAI $id" }
                if (!modelos.isNullOrEmpty()) return modelos
            } catch (e: Exception) {
                Log.e("AI_SERVICE", "Error fallback OpenAI: ${e.message}")
            }
        }

        return emptyList()
    }

    /** Valida las credenciales e invoca la API seleccionada para generar respuesta al prompt dado. */
    suspend fun validarYGenerar(prompt: String): String? {
        val key = authManager.obtenerApiKey()
        var modelo = authManager.obtenerModelo()
        if (key.isNullOrBlank()) return "Error: Falta API Key"
        if (modelo.isEmpty()) modelo = "gemini-2.5-flash"

        val esOpenAi = key.startsWith("sk-", ignoreCase = true) || modelo.startsWith("gpt-") || modelo.startsWith("o1") || modelo.startsWith("o3")

        val maxReintentos = 3
        var reintentoActual = 0
        var tiempoEsperaMs = 2000L

        while (reintentoActual < maxReintentos) {
            try {
                if (esOpenAi) {
                    val request = OpenAiChatRequest(
                        model = modelo,
                        messages = listOf(OpenAiMessage(role = "user", content = prompt))
                    )
                    val response = openAiApi.generarContenido("Bearer $key", request)
                    return if (response.error != null) {
                        "Error de la IA: ${response.error.message}"
                    } else {
                        response.choices?.firstOrNull()?.message?.content
                    }
                } else {
                    val request = GeminiRequest(listOf(GeminiContent(listOf(GeminiPart(text = prompt)))))
                    val response = geminiApi.generarContenido(modelo, key, request)
                    return if (response.error != null) {
                        "Error de la IA: ${response.error.message}"
                    } else {
                        response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    }
                }
            } catch (e: HttpException) {
                if (e.code() == 503 || e.code() == 429) {
                    Log.w("AI_SERVICE", "Error ${e.code()}. Reintentando en ${tiempoEsperaMs}ms...")
                    delay(tiempoEsperaMs)
                    reintentoActual++
                    tiempoEsperaMs *= 2
                } else {
                    Log.e("AI_SERVICE", "Error HTTP: ${e.code()} - ${e.message()}")
                    return "Error de servidor: ${e.code()}"
                }
            } catch (e: Exception) {
                Log.e("AI_SERVICE", "Fallo de red: ${e.message}")
                return "Error de conexión: ${e.localizedMessage}"
            }
        }
        return "Error: Los servidores de la IA están ocupados. Intenta de nuevo más tarde."
    }

    /** Transcribe un archivo WAV enviándolo a la API de IA. */
    suspend fun transcribirAudio(audioFile: File): String? {
        val key = authManager.obtenerApiKey()
        val modelo = authManager.obtenerModelo().ifEmpty { "gemini-2.5-flash" }
        if (key.isNullOrBlank()) return null
        if (!audioFile.exists() || audioFile.length() == 0L) return null

        val esOpenAi = key.startsWith("sk-", ignoreCase = true)

        return try {
            if (esOpenAi) {
                val mediaTypeWav = MediaType.parse("audio/wav")
                val reqFile = RequestBody.create(mediaTypeWav, audioFile)
                val bodyPart = MultipartBody.Part.createFormData("file", audioFile.name, reqFile)
                val mediaTypeText = MediaType.parse("text/plain")
                val modelBody = RequestBody.create(mediaTypeText, "whisper-1")
                val response = openAiApi.transcribirAudio("Bearer $key", bodyPart, modelBody)
                response.text
            } else {
                val base64 = audioFile.inputStream().use { stream ->
                    val buffer = ByteArray(8 * 1024)
                    val sb = StringBuilder((audioFile.length() * 1.37).toInt())
                    var read: Int
                    while (stream.read(buffer).also { read = it } != -1) {
                        sb.append(Base64.encodeToString(buffer, 0, read, Base64.NO_WRAP))
                    }
                    sb.toString()
                }
                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            listOf(
                                GeminiPart(inlineData = InlineData(mimeType = "audio/wav", data = base64)),
                                GeminiPart(text = AiPrompts.TRANSCRIPCION_CHUNK)
                            )
                        )
                    )
                )
                val response = geminiApi.generarContenido(modelo, key, request)
                if (response.error != null) {
                    Log.e("AI_SERVICE_AUDIO", "Error: ${response.error.message}")
                    null
                } else {
                    response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                }
            }
        } catch (e: Exception) {
            Log.e("AI_SERVICE_AUDIO", "Error transcribiendo audio: ${e.message}")
            null
        }
    }

    /** Comprueba si la conexión con la IA funciona correctamente. */
    suspend fun validarConexion(apiKey: String? = null): Boolean {
        val modelos = obtenerModelosDisponibles(apiKey)
        return modelos.isNotEmpty()
    }
}
