package com.hacz.jojmakabbalah.corpus

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Contenido de una traducción para un idioma concreto — refleja
 * traducciones["es"|"en"] tal como aparece en el corpus real
 * (salmos_app.json, bloque*.json): texto[] y transliteracion[] son
 * arrays paralelos a hebreo[], ambos opcionales (no todo idioma tiene
 * ambos poblados todavía). _fuente se ignora en runtime, solo
 * trazabilidad en el JSON — igual que en el lado iOS
 * (OracionesService.swift / SalmosService.swift comentan lo mismo).
 */
@Serializable
data class TraduccionContenido(
    val texto: List<String>? = null,
    val transliteracion: List<String>? = null,
    @SerialName("_fuente") val fuente: String? = null,
)

/**
 * Traducción que la UI va a mostrar de hecho, con el idioma
 * REALMENTE resuelto al lado del pedido, para que la vista sepa si
 * avisar del fallback — paridad exacta con `TraduccionResuelta` en
 * IdiomaContenido.swift (iOS).
 */
data class TraduccionResuelta(
    val lineas: List<String>,
    /** Idioma de `lineas`. Puede diferir de `pedido` si hubo fallback. */
    val idioma: String,
    /** Idioma que la UI pidió. */
    val pedido: String,
) {
    val esFallback: Boolean get() = idioma != pedido
}

/**
 * Resuelve el texto a mostrar: idioma pedido → español → null.
 *
 * Paridad INTENCIONAL con `Dictionary.textoResuelto(preferido:)` de
 * IdiomaContenido.swift (iOS) — misma regla, traducida a Kotlin, no
 * reinventada. Un array vacío cuenta como ausente (mismo criterio que
 * iOS: hay átomos con la llave del idioma presente pero `texto` sin
 * poblar).
 *
 * IMPORTANTE, también paridad con iOS: la TRANSLITERACIÓN no tiene
 * fallback — no existe un `transliteracionResuelta()` equivalente.
 * La convención fonética es distinta por idioma; la española leída
 * como si fuera inglesa sería simplemente incorrecta (mismo criterio
 * que `Dictionary.transliteracion(idioma:)` en iOS, que documenta
 * esto explícitamente). Quien necesite transliteración debe seguir
 * consultando `traducciones[idioma]?.transliteracion` directo, sin
 * fallback, y aceptar `null`/vacío si ese idioma no la tiene.
 */
fun Map<String, TraduccionContenido>.textoResuelto(preferido: String): TraduccionResuelta? {
    this[preferido]?.texto?.takeIf { it.isNotEmpty() }?.let {
        return TraduccionResuelta(it, preferido, preferido)
    }
    this["es"]?.texto?.takeIf { it.isNotEmpty() }?.let {
        return TraduccionResuelta(it, "es", preferido)
    }
    return null
}

/** Kavaná embebida en un versículo — mismo esquema que Kavana.swift. */
@Serializable
data class Kavana(
    val versiculo: Int,
    val palabra: Int? = null,
    val nombre: String,
    val aplicacion: String,
    @SerialName("nombre_dios") val nombreDios: String,
    @SerialName("letras_pos") val letrasPos: List<Int>? = null,
)

/** Un Salmo — refleja salmos_app.json ("salmos": [...]) tal cual. */
@Serializable
data class Salmo(
    val id: String,
    val numero: Int,
    val titulo: String,
    val autor: String? = null,
    val hebreo: List<String>,
    val traducciones: Map<String, TraduccionContenido>,
    val explicacion: String? = null,
    val kavanot: List<Kavana> = emptyList(),
) {
    /** Traducción para el idioma pedido, o null si esa clave no existe. */
    fun traduccion(idioma: String): TraduccionContenido? = traducciones[idioma]
}

/** Raíz de salmos_app.json. `_meta` se ignora (ignoreUnknownKeys). */
@Serializable
data class SalmosApp(
    val salmos: List<Salmo>,
)

/**
 * Un átomo de oración — refleja el esquema de bloque*.json /
 * oraciones_preservados.json (campo "oraciones" o "atomos"). Se
 * modela aparte de Salmo porque trae campos propios (categoria,
 * momentos, nombre_hebreo) que Salmo no tiene.
 *
 * `hebreo` tiene default `emptyList()` — NO todos los átomos lo
 * traen. Hallazgo real 2026-09-21 parseando bloque11_shabbat.json:
 * el átomo "hamotzi" es una referencia PURA a nivel de átomo completo
 * (campo `ref: {ref_modulo, ref_id}`, sin `hebreo` en absoluto,
 * `traducciones.es.texto: null`) — un segundo tipo de referencia,
 * DISTINTO del placeholder inline `«ref_id: ...»` dentro de una línea
 * de `hebreo[]` (ver esPlaceholderRefId() más abajo). Ninguno de los
 * dos tipos se resuelve todavía — ambos son TODO explícito. El campo
 * `ref` en sí no se modela aquí (ignoreUnknownKeys lo descarta); si
 * se necesita resolverlo habrá que agregarlo.
 */
@Serializable
data class Atomo(
    val id: String,
    val nombre: String,
    @SerialName("nombre_hebreo") val nombreHebreo: String? = null,
    val tipo: String? = null,
    val categoria: String? = null,
    val momentos: List<String> = emptyList(),
    val hebreo: List<String> = emptyList(),
    val traducciones: Map<String, TraduccionContenido>,
    val explicacion: String? = null,
) {
    fun traduccion(idioma: String): TraduccionContenido? = traducciones[idioma]
}

/** Raíz tolerante a los dos esquemas históricos (oraciones vs atomos)
 *  — mismo criterio que atomos_de() en scripts/translit.py y el
 *  decoder de OracionesService.swift. */
@Serializable
data class BloqueOraciones(
    val oraciones: List<Atomo>? = null,
    val atomos: List<Atomo>? = null,
) {
    val items: List<Atomo> get() = oraciones ?: atomos ?: emptyList()
}

private val PATRON_REF_ID = Regex("""«ref_id:\s*([\w-]+)\s*»""")

/**
 * true si esta línea de `hebreo[]` es un placeholder de referencia a
 * otro átomo (ej. "«ref_id: vaijulu»") en vez de hebreo real — mismo
 * formato que ya reconoce transliterar_linea() en scripts/translit.py
 * y que OracionLectorView.swift resuelve en runtime del lado iOS.
 */
fun esPlaceholderRefId(linea: String): Boolean = linea.startsWith("«ref_id:")

/** Extrae el id referenciado de un placeholder, o null si la línea
 *  no es un placeholder ref_id. */
fun idReferenciado(linea: String): String? = PATRON_REF_ID.find(linea)?.groupValues?.get(1)

// TODO(oraciones-ref-id): esto solo DETECTA el placeholder — no lo
// RESUELVE. Resolver de verdad significa: buscar el Atomo con ese id
// en la MISMA colección ya descargada (puede vivir en otro bloque —
// ver bloquesAdicionales en OracionesService.swift del lado iOS para
// el orden real de dependencias) y copiar su hebreo[]/traducciones en
// la posición correspondiente. No implementado todavía — pendiente
// explícito para cuando se construya la UI real de Oraciones (esta
// pieza es solo parsing + fallback de idioma, ver commit).
