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
 * Subobjeto anidado `"ref": {"ref_modulo": "...", "ref_id": "..."}` —
 * mismo esquema que `RefAnidada` en OracionesService.swift (iOS).
 * Coexiste en el corpus con las claves planas legacy `ref_modulo`/
 * `ref_id` a nivel de átomo (bloques 1-10); el anidado (bloque11+)
 * gana cuando ambos están presentes — ver `Atomo.refModuloEfectivo`.
 */
@Serializable
data class RefAnidada(
    @SerialName("ref_modulo") val refModulo: String? = null,
    @SerialName("ref_id") val refId: String? = null,
)

/**
 * Un átomo de oración — refleja el esquema de bloque*.json /
 * oraciones_preservados.json (campo "oraciones" o "atomos"). Se
 * modela aparte de Salmo porque trae campos propios (categoria,
 * momentos, nombre_hebreo, referencias) que Salmo no tiene.
 *
 * `hebreo` tiene default `emptyList()` — NO todos los átomos lo
 * traen. Hallazgo real 2026-09-21 parseando bloque11_shabbat.json:
 * el átomo "hamotzi" es una referencia PURA a nivel de átomo completo
 * (campo `ref: {ref_modulo, ref_id}`, sin `hebreo` en absoluto,
 * `traducciones.es.texto: null`) — un segundo tipo de referencia,
 * DISTINTO del placeholder inline `«ref_id: ...»` dentro de una línea
 * de `hebreo[]` (ver esPlaceholderRefId() más abajo).
 *
 * `_fuente` es TOP-LEVEL acá (a diferencia de Salmo, donde vive por
 * traducción dentro de TraduccionContenido) — verificado contra el
 * JSON real de bloque1/4/11: ninguna entrada de `traducciones.es`
 * trae `_fuente` propio, solo el átomo completo.
 *
 * NINGÚN tipo de referencia se resuelve todavía — ver TODO en
 * `esRef`/`esRefASalmo` más abajo. `secretos`, `aplicabilidad`
 * (filtro por nusaj), `variantes`, `rubrica_por_nusaj`,
 * `ketiv_variantes`, `simanim_meta`, `omer_meta`, `agitacion_meta`
 * del modelo real de iOS NO se modelan acá (ignoreUnknownKeys los
 * descarta) — fuera de alcance de este pase, ver reporte 2026-09-21.
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
    val ref: RefAnidada? = null,
    @SerialName("ref_modulo") val refModuloPlano: String? = null,
    @SerialName("ref_id") val refIdPlano: String? = null,
    @SerialName("_fuente") val fuente: String? = null,
    @SerialName("_nota") val nota: String? = null,
    @SerialName("_parcial") val parcial: Boolean? = null,
    @SerialName("_verificar") val verificar: Boolean? = null,
) {
    fun traduccion(idioma: String): TraduccionContenido? = traducciones[idioma]

    /** Prioriza el objeto anidado sobre las claves planas — mismo
     *  criterio que `Atomo.init(from:)` en iOS. */
    val refModuloEfectivo: String? get() = ref?.refModulo ?: refModuloPlano
    val refIdEfectivo: String? get() = ref?.refId ?: refIdPlano

    /** true si tipo=="ref" O si trae un ref_id resoluble — mismo
     *  auto-tipado que iOS (`tipoExplicito ?? (refId != nil ? "ref" : "texto")`). */
    val esRef: Boolean get() = tipo == "ref" || refIdEfectivo != null

    /** true si es una referencia a un Salmo completo. En iOS esto
     *  redirige al lector NATIVO de Salmos (Pesukei deZimrá, etc.).
     *  TODO(oraciones-ref-salmo): Android NO implementa ese redirect
     *  todavía — requeriría compartir SalmosViewModel/nav entre
     *  módulos independientes; por ahora se trata igual que cualquier
     *  otra referencia sin resolver (placeholder genérico). Simplificación
     *  explícita, documentada en el reporte del 2026-09-21. */
    val esRefASalmo: Boolean get() = esRef && refModuloEfectivo == "salmos"

    /** _parcial o _verificar — mismo criterio que `porCompletar` en
     *  iOS: para el usuario ambas banderas significan "no es final". */
    val porCompletar: Boolean get() = parcial == true || verificar == true
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
