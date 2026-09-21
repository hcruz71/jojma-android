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
 */
@Serializable
data class Atomo(
    val id: String,
    val nombre: String,
    @SerialName("nombre_hebreo") val nombreHebreo: String? = null,
    val tipo: String? = null,
    val categoria: String? = null,
    val momentos: List<String> = emptyList(),
    val hebreo: List<String>,
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
