package app.kodanotes.data.audio

import android.content.Context
import android.util.Log
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineStream
import com.k2fsa.sherpa.onnx.OfflineWhisperModelConfig
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Transcriptor local con sherpa-onnx + Whisper Base.
 *
 * Archivos requeridos en assets/sherpa/:
 * base-encoder.int8.onnx
 * base-decoder.int8.onnx
 * base-tokens.txt
 *
 * Con el nuevo diseño de dos fases, este transcriptor trabaja DESPUÉS de que
 * el AudioRecord ya fue liberado — sin competencia de recursos con el micro.
 */
class WhisperTranscriber(private val context: Context) {

    private var recognizer: OfflineRecognizer? = null

    // ─────────────────────────────────────────────────────────────────────────
    // Inicialización
    // ─────────────────────────────────────────────────────────────────────────

    fun inicializar(): Boolean {
        return try {
            val dir = File(context.filesDir, "whisper").also { it.mkdirs() }

            val archivos = listOf(
                "base-encoder.int8.onnx",
                "base-decoder.int8.onnx",
                "base-tokens.txt"
            )

            for (nombre in archivos) {
                val destino = File(dir, nombre)
                if (!destino.exists() || destino.length() == 0L) {
                    context.assets.open("sherpa/$nombre").use { input ->
                        FileOutputStream(destino).use { output -> input.copyTo(output) }
                    }
                    Log.d("WHISPER", "Copiado: $nombre (${destino.length()} bytes)")
                }
            }

            for (nombre in archivos) {
                val f = File(dir, nombre)
                if (!f.exists() || f.length() == 0L) {
                    Log.e("WHISPER", "Archivo faltante: $nombre")
                    return false
                }
            }

            recognizer = OfflineRecognizer(
                assetManager = null,
                config = OfflineRecognizerConfig(
                    modelConfig = OfflineModelConfig(
                        whisper = OfflineWhisperModelConfig(
                            encoder  = File(dir, "base-encoder.int8.onnx").absolutePath,
                            decoder  = File(dir, "base-decoder.int8.onnx").absolutePath,
                            language = "es",
                            task     = "transcribe"
                        ),
                        tokens     = File(dir, "base-tokens.txt").absolutePath,
                        numThreads = 2,
                        debug      = false
                    )
                )
            )

            Log.d("WHISPER", "Whisper Base inicializado (Español)")
            true
        } catch (e: Exception) {
            Log.e("WHISPER", "Error inicializando: ${e.message}", e)
            false
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Transcripción de un archivo WAV 16kHz mono PCM
    // ─────────────────────────────────────────────────────────────────────────

    fun transcribir(wavFile: File): String? {
        val rec = recognizer ?: run {
            Log.e("WHISPER", "Recognizer no inicializado")
            return null
        }

        return try {
            val muestras = leerPCMDesdeWAV(wavFile) ?: return null

            // Mínimo 0.5 s para que Whisper tenga contexto suficiente
            if (muestras.size < 8_000) {
                Log.w("WHISPER", "Audio muy corto: ${muestras.size} muestras")
                return null
            }

            val durSeg = muestras.size / 16_000f
            Log.d("WHISPER", "Transcribiendo ${muestras.size} muestras (${"%.1f".format(durSeg)}s)…")

            val stream: OfflineStream = rec.createStream()
            stream.acceptWaveform(muestras, sampleRate = 16000)
            rec.decode(stream)
            val texto = rec.getResult(stream).text.trim()
            stream.release()

            Log.d("WHISPER", "Resultado: '$texto'")
            if (texto.isBlank()) null else texto

        } catch (e: Exception) {
            Log.e("WHISPER", "Error transcribiendo: ${e.message}", e)
            null
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers WAV
    // ─────────────────────────────────────────────────────────────────────────

    private fun leerPCMDesdeWAV(file: File): FloatArray? {
        return try {
            val bytes = file.readBytes()
            if (bytes.size < 44) {
                Log.w("WHISPER", "WAV demasiado pequeño: ${bytes.size} bytes")
                return null
            }
            val dataOffset = encontrarDataChunk(bytes) ?: 44
            val pcmBytes   = bytes.size - dataOffset
            val numSamples = pcmBytes / 2
            if (numSamples <= 0) return null

            val buf = ByteBuffer.wrap(bytes, dataOffset, pcmBytes).order(ByteOrder.LITTLE_ENDIAN)
            FloatArray(numSamples) { buf.short.toFloat() / 32768.0f }
        } catch (e: Exception) {
            Log.e("WHISPER", "Error leyendo WAV: ${e.message}")
            null
        }
    }

    private fun encontrarDataChunk(bytes: ByteArray): Int? {
        var i = 12
        while (i + 8 <= bytes.size) {
            val tag  = String(bytes, i, 4, Charsets.US_ASCII)
            val size = ByteBuffer.wrap(bytes, i + 4, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (tag == "data") return i + 8
            if (size <= 0) {
                Log.w("WHISPER", "Chunk WAV malformado (size=$size) en offset $i")
                return null
            }
            i += 8 + size
        }
        return null
    }

    fun liberar() {
        try { recognizer?.release() } catch (e: Exception) {
            Log.w("WHISPER", "Error liberando recognizer: ${e.message}")
        }
        recognizer = null
        Log.d("WHISPER", "Recursos liberados")
    }
}