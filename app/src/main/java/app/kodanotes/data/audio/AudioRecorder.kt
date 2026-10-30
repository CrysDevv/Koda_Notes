package app.kodanotes.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.log10
import kotlin.math.sqrt

class AudioRecorder(
    private val chunkDurationMs: Long   = 30_000L,
    private val minChunkSizeBytes: Long = 8_192L,
    private val silenceRmsThreshold: Float = 3f
) {
    companion object {
        const val SAMPLE_RATE      = 16_000
        const val CHANNEL_CONFIG   = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT     = AudioFormat.ENCODING_PCM_16BIT
        const val BYTES_PER_SAMPLE = 2
    }

    val grabaciones: CopyOnWriteArrayList<File> = CopyOnWriteArrayList()

    @Volatile
    var grabacionesSnapshot: List<File> = emptyList()
        private set

    var recordingJob: Job? = null
        private set

    private val _isRecording = MutableStateFlow(false)
    val isRecordingState: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val recorderScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var audioFocusRequest: AudioFocusRequest? = null
    private var audioManager: AudioManager? = null

    private fun requestAudioFocus(context: Context): Boolean {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                        focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        Log.w("AUDIO_RECORDER", "¡Perdimos el Foco de Audio! El sistema nos quitará el micrófono.")
                    }
                }
                .build()

            audioFocusRequest = focusRequest
            val result = audioManager?.requestAudioFocus(focusRequest)
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager?.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                        focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
                        Log.w("AUDIO_RECORDER", "¡Perdimos el Foco de Audio! El sistema nos quitará el micrófono.")
                    }
                },
                AudioManager.STREAM_VOICE_CALL,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager?.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager?.abandonAudioFocus(null)
        }
    }

    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    fun start(context: Context, outputDir: File) {
        if (_isRecording.value) return
        _isRecording.value = true
        grabaciones.clear()
        grabacionesSnapshot = emptyList()

        recordingJob = recorderScope.launch {

            if (!requestAudioFocus(context)) {
                Log.e("AUDIO_RECORDER", "No pudimos obtener exclusividad del micrófono.")
                _isRecording.value = false
                return@launch
            }

            val baseBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT
            )
            val safeBufferSize = maxOf(baseBufferSize * 10, 64_000)

            var intentosReconexion = 0

            // BUCLE EXTERNO: AUTO-RESURRECCIÓN
            while (_isRecording.value && isActive) {
                var audioRecord: AudioRecord? = null

                try {
                    audioRecord = AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        safeBufferSize
                    )

                    if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                        Log.e("AUDIO_RECORDER", "AudioRecord no inicializado. Reintentando...")
                        audioRecord.release()
                        kotlinx.coroutines.delay(500)
                        intentosReconexion++
                        if (intentosReconexion > 5) {
                            Log.e("AUDIO_RECORDER", "Hardware irrecuperable tras 5 intentos.")
                            break
                        }
                        continue
                    }

                    audioRecord.startRecording()
                    Log.d("AUDIO_RECORDER", "Grabación iniciada/reiniciada — VOICE_RECOGNITION")
                    intentosReconexion = 0

                    val readChunkSize = baseBufferSize.coerceAtLeast(4096)
                    val readBuffer    = ShortArray(readChunkSize)
                    val chunkSamples  = (SAMPLE_RATE * chunkDurationMs / 1000).toInt()
                    val chunkBuffer   = ShortArray(chunkSamples + readChunkSize)
                    var chunkFill     = 0
                    var fallosHardware = 0
                    var hardwareCrashed = false

                    // DETECTOR DE SECUESTRO SILENCIOSO
                    var silenciosAbsolutosConsecutivos = 0

                    // BUCLE INTERNO: LECTURA DE CHUNKS
                    while (_isRecording.value && isActive && !hardwareCrashed) {
                        val leidos = audioRecord.read(readBuffer, 0, readBuffer.size)

                        if (leidos < 0) {
                            fallosHardware++
                            Log.e("AUDIO_RECORDER", "Error de lectura: $leidos. Intento $fallosHardware")
                            if (fallosHardware > 10) {
                                Log.w("AUDIO_RECORDER", "Hardware colapsado. Activando Auto-Healing...")
                                hardwareCrashed = true
                                break
                            }
                            Thread.sleep(50)
                            continue
                        }

                        fallosHardware = 0

                        if (leidos > 0) {
                            val rms = calcularRMS(readBuffer, leidos)

                            // Verificamos si el sistema operativo nos está enviando silencio falso
                            if (rms == 0.0f) {
                                silenciosAbsolutosConsecutivos++
                                // Aprox. 3 segundos de silencio absoluto = secuestro
                                if (silenciosAbsolutosConsecutivos > 15) {
                                    Log.w("AUDIO_RECORDER", "Secuestro silencioso detectado (OS Mute). Forzando reinicio del hardware...")
                                    hardwareCrashed = true
                                    break
                                }
                            } else {
                                silenciosAbsolutosConsecutivos = 0
                            }

                            val copiar = minOf(leidos, chunkBuffer.size - chunkFill)
                            System.arraycopy(readBuffer, 0, chunkBuffer, chunkFill, copiar)
                            chunkFill += copiar

                            val db = if (rms > 0) 20.0 * log10(rms / 32768.0) else -100.0
                            _audioLevel.value = ((db + 70.0) / 70.0).coerceIn(0.0, 1.0).toFloat()

                            if (chunkFill >= chunkSamples) {
                                val muestras  = chunkBuffer.copyOf(chunkSamples)
                                val restantes = chunkFill - chunkSamples
                                if (restantes > 0)
                                    System.arraycopy(chunkBuffer, chunkSamples, chunkBuffer, 0, restantes)
                                chunkFill = restantes

                                recorderScope.launch(Dispatchers.IO) {
                                    guardarChunk(outputDir, muestras, chunkSamples, esFinal = false)
                                }
                            }
                        }
                    }

                    // Guardado del remanente al salir del bucle interno
                    val minimoMuestras = (SAMPLE_RATE * 0.5).toInt()
                    if (chunkFill >= minimoMuestras) {
                        val muestrasFinales = chunkBuffer.copyOf(chunkFill)
                        recorderScope.launch(Dispatchers.IO) {
                            guardarChunk(outputDir, muestrasFinales, chunkFill, esFinal = true)
                        }
                    }

                } catch (e: CancellationException) {
                    Log.d("AUDIO_RECORDER", "Grabación cancelada por el usuario")
                    break
                } catch (e: Exception) {
                    Log.e("AUDIO_RECORDER", "Excepción de hardware: ${e.message}")
                    kotlinx.coroutines.delay(500)
                } finally {
                    try {
                        if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                            audioRecord.stop()
                        }
                    } catch (e: Exception) { /* Ignorado en la limpieza */ }

                    try {
                        audioRecord?.release()
                    } catch (e: Exception) { /* Ignorado en la limpieza */ }

                    Log.d("AUDIO_RECORDER", "Limpieza de intento completada. ¿Detención manual?: ${!_isRecording.value}")
                }
            } // FIN BUCLE EXTERNO

            grabacionesSnapshot = grabaciones.toList()
            abandonAudioFocus()
            _audioLevel.value  = 0f
            _isRecording.value = false
            Log.d("AUDIO_RECORDER", "AudioRecord liberado totalmente y Foco abandonado.")
        }
    }

    fun stop() {
        _isRecording.value = false
    }

    private fun guardarChunk(dir: File, muestras: ShortArray, count: Int, esFinal: Boolean = false) {
        val rms = calcularRMS(muestras, count)

        val umbralEfectivo = if (esFinal) silenceRmsThreshold * 0.5f else silenceRmsThreshold
        if (rms < umbralEfectivo) {
            if (!esFinal) {
                Log.w("AUDIO_RECORDER", "Chunk descartado — silencio (RMS=${"%.1f".format(rms)} < $umbralEfectivo)")
                return
            }
        }

        val archivo = crearWav(dir, muestras, count) ?: return

        if (!esFinal && archivo.length() < minChunkSizeBytes) {
            archivo.delete()
            Log.w("AUDIO_RECORDER", "Chunk descartado — muy pequeño (${archivo.length()} bytes)")
            return
        }

        grabaciones.add(archivo)
        grabacionesSnapshot = grabaciones.toList()

        Log.d("AUDIO_RECORDER", "Chunk${if (esFinal) " FINAL" else ""} #${grabaciones.size} — ${archivo.length()} bytes | RMS=${"%.1f".format(rms)}")
    }

    private fun crearWav(dir: File, muestras: ShortArray, count: Int): File? {
        return try {
            val file      = File(dir, "chunk_${System.currentTimeMillis()}.wav")
            val dataSize  = count * BYTES_PER_SAMPLE
            val totalSize = 36 + dataSize

            FileOutputStream(file).use { fos ->
                fos.write("RIFF".toByteArray())
                fos.write(intLE(totalSize))
                fos.write("WAVE".toByteArray())
                fos.write("fmt ".toByteArray())
                fos.write(intLE(16))
                fos.write(shortLE(1))
                fos.write(shortLE(1))
                fos.write(intLE(SAMPLE_RATE))
                fos.write(intLE(SAMPLE_RATE * BYTES_PER_SAMPLE))
                fos.write(shortLE(BYTES_PER_SAMPLE))
                fos.write(shortLE(16))
                fos.write("data".toByteArray())
                fos.write(intLE(dataSize))

                val buf = ByteBuffer.allocate(dataSize).order(ByteOrder.LITTLE_ENDIAN)
                buf.asShortBuffer().put(muestras, 0, count)
                fos.write(buf.array())
            }
            file
        } catch (e: Exception) {
            Log.e("AUDIO_RECORDER", "Error creando WAV: ${e.message}")
            null
        }
    }

    private fun intLE(v: Int)   = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(v).array()
    private fun shortLE(v: Int) = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(v.toShort()).array()

    private fun calcularRMS(buffer: ShortArray, count: Int): Float {
        if (count <= 0) return 0f
        var sum = 0.0
        for (i in 0 until count) sum += buffer[i].toDouble() * buffer[i]
        return sqrt(sum / count).toFloat()
    }
}