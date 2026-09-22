package com.hacz.jojmakabbalah.festividades

import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Refleja Conexion en FestividadesInfoService.swift (iOS). */
@Serializable
data class ConexionInfo(
    val tipo: String,
    val texto: String,
    @SerialName("enlace_a") val enlaceA: String,
)

/** Refleja FestividadInfo en FestividadesInfoService.swift (iOS) —
 *  el contenido "rico" de una festividad (descripción, secretos,
 *  conexiones, bloques_oracion), a diferencia de EstadoFestividad
 *  (CalendarioHebreoService.kt), que solo trae la fecha/estado
 *  calculados. Mismo split de dos fuentes que usa iOS. */
@Serializable
data class FestividadInfo(
    val id: String,
    val nombre: String,
    @SerialName("nombre_hebreo") val nombreHebreo: String,
    @SerialName("fecha_hebrea") val fechaHebrea: String,
    @SerialName("fecha_aprox") val fechaAprox: String? = null,
    @SerialName("duracion_dias") val duracionDias: Int,
    val tono: String,
    val descripcion: String,
    val secretos: List<String> = emptyList(),
    @SerialName("fuente_descripcion") val fuenteDescripcion: String? = null,
    @SerialName("fuente_secretos") val fuenteSecretos: String? = null,
    val conexiones: List<ConexionInfo> = emptyList(),
    @SerialName("bloques_oracion") val bloquesOracion: List<String> = emptyList(),
)

@Serializable
private data class ArchivoFestividades(val festividades: List<FestividadInfo> = emptyList())

/** Descarga festividades_info.json (corpus/, Firebase Storage) —
 *  mismo archivo que ya usa iOS, ya republicado a producción. */
class FestividadesInfoRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val maxBytes: Long = 2L * 1024 * 1024

    suspend fun descargarInfo(): List<FestividadInfo> {
        val ref = Firebase.storage.reference.child("corpus/festividades_info.json")
        val bytes = ref.getBytes(maxBytes).await()
        val texto = bytes.toString(Charsets.UTF_8)
        return json.decodeFromString(ArchivoFestividades.serializer(), texto).festividades
    }
}
