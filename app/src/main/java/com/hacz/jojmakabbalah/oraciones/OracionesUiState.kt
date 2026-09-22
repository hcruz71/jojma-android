package com.hacz.jojmakabbalah.oraciones

import com.hacz.jojmakabbalah.corpus.Atomo
import com.hacz.jojmakabbalah.corpus.TraduccionResuelta
import com.hacz.jojmakabbalah.corpus.textoResuelto

/** Mismo patrón que PantallaSalmos/SalmosUiState — ver ese archivo
 *  para el razonamiento (NavHost solo refleja `pantalla`, testable
 *  sin NavController). */
sealed interface PantallaOraciones {
    data object Lista : PantallaOraciones
    data class Detalle(val id: String) : PantallaOraciones
}

data class OracionesUiState(
    val atomos: List<Atomo> = emptyList(),
    val cargando: Boolean = false,
    val error: String? = null,
    val pantalla: PantallaOraciones = PantallaOraciones.Lista,
    val idioma: String = "es",
) {
    val atomoAbierto: Atomo?
        get() {
            val p = pantalla
            return if (p is PantallaOraciones.Detalle) atomos.find { it.id == p.id } else null
        }

    /** Reutiliza el mismo `textoResuelto()` compartido con Salmo — sin
     *  duplicar la lógica de fallback (idioma pedido → español → null). */
    val textoResueltoActual: TraduccionResuelta?
        get() = atomoAbierto?.traducciones?.textoResuelto(idioma)

    val textoMostrado: List<String>?
        get() = textoResueltoActual?.lineas
}
