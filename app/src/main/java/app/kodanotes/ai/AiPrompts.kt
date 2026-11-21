package app.kodanotes.ai

/**
 * Prompts centralizados de KodaNotes AI.
 *
 * Arquitectura de tokens optimizada:
 * - Cada segmento de 5 min se analiza de forma INDEPENDIENTE → costo fijo por segmento
 * - No se acumula contexto entre segmentos → sin crecimiento de tokens
 * - Resumen global: 1 sola llamada opcional al final, usando solo los Cheat Sheets
 */
object AiPrompts {

    const val TRANSCRIPCION_CHUNK =
        "Transcribe literalmente el siguiente fragmento de audio de una clase universitaria. " +
                "Devuelve ÚNICAMENTE el texto hablado, sin puntuación excesiva, sin comentarios. " +
                "Si no hay voz, devuelve cadena vacía."

    /** Genera el prompt para analizar un segmento específico de la clase. */
    fun analisisSegmento(
        transcripcion:   String,
        numero:          Int,
        totalEstimado:   Int
    ): String = """
Eres un asistente académico universitario. Estás analizando el segmento $numero de $totalEstimado de una clase.

TRANSCRIPCIÓN DEL SEGMENTO $numero:
$transcripcion

REGLAS IRROMPIBLES:
1. Analiza SOLO este segmento. No inventes contenido de otros momentos de la clase.
2. Si el segmento empieza en medio de un tema, trabaja con lo que hay.
3. FILTRO DE ALUCINACIONES (CRÍTICO): Esta transcripción fue generada por una IA de voz y contiene errores. Si ves palabras sin sentido, frases extrañas o términos entre comillas ("") que claramente son un error de escucha o una mala traducción, OMÍTELOS POR COMPLETO o corrígelos según el contexto académico. Nunca incluyas información errónea solo porque estaba en la transcripción.
4. Usa Markdown profesional (# ## ### **negritas** - listas).
5. SEPARA los 3 bloques EXACTAMENTE con ||| (tres barras solas en su propia línea).

# 💡 Síntesis
Qué se explicó en este segmento, en 2-3 párrafos claros.
|||
# 📝 Guía de Estudio
Apunte detallado del contenido: conceptos, ejemplos, pasos, leyes o técnicas mencionadas. (Omite los errores de transcripción).
|||
# 🧠 Cheat Sheet
Solo datos duros: términos técnicos reales, fórmulas, fechas, nombres clave. En viñetas directas. No incluyas palabras entre comillas si son errores evidentes.
    """.trimIndent()

    /** Genera el prompt para analizar una clase corta completa en una sola llamada. */
    fun analisisClase(transcripcion: String): String = """
Eres el mejor asistente académico universitario. Convierte esta transcripción en material de estudio.

REGLAS IRROMPIBLES:
1. Filtra ruido, muletillas y digresiones. Solo contenido académico.
2. FILTRO DE ALUCINACIONES (CRÍTICO): El texto proviene de un sistema de voz a texto y tiene errores. Identifica y OMITE cualquier palabra, frase o término (incluso si está entre comillas) que sea incoherente, esté mal traducido o sea un error del micrófono. Prioriza que el apunte final tenga sentido académico perfecto.
3. Usa Markdown profesional (# ## ### **negritas** - listas).
4. NUNCA escribas frases de apertura o cierre de cortesía.
5. SEPARA los 3 bloques EXACTAMENTE con ||| (tres barras solas en su propia línea).

TRANSCRIPCIÓN:
$transcripcion

# 💡 Síntesis de la Clase
2-3 párrafos: qué se vio y cuáles fueron los conceptos principales.
|||
# 📝 Guía de Estudio Completa
Apunte académico completo para estudiar un examen final sin otra fuente.
Usa ### para subtemas, párrafos explicativos, listas para ejemplos.
|||
# 🧠 Cheat Sheet — Datos Clave
Solo datos duros reales y verificables del contexto de la clase.
Todo en viñetas directas, sin relleno. Ignora errores de la transcripción.
    """.trimIndent()

    /** Genera el prompt para el resumen ejecutivo global a partir de los Cheat Sheets de cada segmento. */
    fun resumenGlobal(cheatSheets: List<String>): String {
        val numerados = cheatSheets.mapIndexed { i, cs ->
            "=== SEGMENTO ${i + 1} ===\n$cs"
        }.joinToString("\n\n")

        return """
Eres un asistente académico universitario. Tienes los Cheat Sheets de cada segmento de una clase larga.
Genera un resumen ejecutivo global de TODA la clase combinando toda la información.

CHEAT SHEETS POR SEGMENTO:
$numerados

REGLAS IRROMPIBLES:
1. SANEAMIENTO DE DATOS: Si notas que algún segmento incluyó un término extraño, fuera de lugar o un texto entre comillas sin sentido (producto de un error previo de transcripción), DESCÁRTALO de este resumen final. Solo mantén la información coherente.
2. SEPARA los 3 bloques EXACTAMENTE con ||| (tres barras solas en su propia línea).
3. Usa Markdown profesional (# ## ### **negritas** - listas).
4. No repitas contenido idéntico entre bloques.
5. NUNCA escribas frases de apertura o cierre de cortesía.

ESTRUCTURA EXACTA:

# 💡 Síntesis Global
2-3 párrafos que resuman toda la clase completa de forma cohesiva,
explicando el hilo conductor entre los distintos segmentos.
|||
# 📝 Guía de Estudio Completa
Apunte académico unificado de toda la clase, organizando los temas
por subtemas con ### y desarrollando cada concepto con el detalle
necesario para estudiar un examen final.
|||
# 🧠 Cheat Sheet Unificado
Lista consolidada de los conceptos clave.
        """.trimIndent()
    }

    /** Genera el prompt para crear flashcards de estudio en formato JSON. */
    fun flashcards(contenidoClase: String, cantidad: Int = 10): String = """
Genera exactamente $cantidad flashcards de estudio a partir del siguiente contenido.
Ignora cualquier palabra extraña o entre comillas que parezca un error de transcripción. Basa las preguntas solo en los conceptos coherentes y reales.

REGLA CRÍTICA: Responde EXCLUSIVAMENTE con un arreglo JSON válido. NO incluyas formato Markdown (```json), NI explicaciones. El primer carácter de tu respuesta debe ser '[' y el último ']'.

ESTRUCTURA DEL JSON:
[{"pregunta":"...","respuesta":"..."}]

CONTENIDO:
$contenidoClase
    """.trimIndent()

    /** Genera el prompt para crear un examen simulado de opción múltiple en formato JSON. */
    fun examenSimulado(contenidoClase: String, cantidad: Int = 10, dificultad: String = "Media"): String = """
Genera exactamente $cantidad preguntas de opción múltiple (A/B/C/D) de dificultad $dificultad basadas estrictamente en el contenido proporcionado.
CUIDADO: El contenido puede tener errores de transcripción de voz. Evita crear preguntas sobre términos extraños o frases entre comillas que no tengan sentido en el contexto de la materia.

Criterios de dificultad:
- Fácil: definiciones directas y conceptos básicos
- Media: aplicación de conceptos y relaciones entre ideas
- Difícil: análisis, casos prácticos y preguntas con opciones muy similares

REGLA CRÍTICA: Responde EXCLUSIVAMENTE con un arreglo JSON válido. NO incluyas formato Markdown (```json), NI explicaciones, NI saludos. El primer carácter de tu respuesta debe ser '[' y el último ']'.

ESTRUCTURA DEL JSON:
[{"pregunta":"...","opciones":["A. ...","B. ...","C. ...","D. ..."],"respuestaCorrecta":"A","explicacion":"..."}]

CONTENIDO DE LA CLASE:
$contenidoClase
    """.trimIndent()

    const val VALIDAR_CONEXION = "Responde solo con la palabra: OK"
}