package com.hacz.jojmakabbalah.salmos

import com.hacz.jojmakabbalah.corpus.Salmo

/** Pantalla activa. El NavHost de Compose solo refleja este valor —
 *  la decisión de a dónde ir vive acá, testable sin NavController. */
sealed interface PantallaSalmos {
    data object Lista : PantallaSalmos
    data class Detalle(val numero: Int) : PantallaSalmos
}

/** Estado inmutable de toda la sección de Salmos — data class pura,
 *  sin dependencia de Android, 100% testable en JVM. */
data class SalmosUiState(
    val salmos: List<Salmo> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val pantalla: PantallaSalmos = PantallaSalmos.Lista,
    val idioma: String = "es",
) {
    val salmoAbierto: Salmo?
        get() {
            val p = pantalla
            return if (p is PantallaSalmos.Detalle) salmos.find { it.numero == p.numero } else null
        }

    /** Líneas de texto del salmo abierto en el idioma activo — null
     *  si no hay salmo abierto o ese idioma no tiene traducción. */
    val textoMostrado: List<String>?
        get() = salmoAbierto?.traduccion(idioma)?.texto
}
