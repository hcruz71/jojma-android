package com.hacz.jojmakabbalah.salmos

import com.hacz.jojmakabbalah.corpus.Salmo
import com.hacz.jojmakabbalah.corpus.TraduccionResuelta
import com.hacz.jojmakabbalah.corpus.textoResuelto

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

    /** Traducción resuelta del salmo abierto: idioma pedido → español
     *  → null. Paridad intencional con iOS — ver comentario completo
     *  en `textoResuelto()` (corpus/Modelos.kt). */
    val textoResueltoActual: TraduccionResuelta?
        get() = salmoAbierto?.traducciones?.textoResuelto(idioma)

    /** Líneas de texto a mostrar — ya con el fallback aplicado. */
    val textoMostrado: List<String>?
        get() = textoResueltoActual?.lineas
}
