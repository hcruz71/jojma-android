package com.hacz.jojmakabbalah.festividades

import com.hacz.jojmakabbalah.calendario.EstadoFestividad
import com.hacz.jojmakabbalah.corpus.Atomo

sealed interface PantallaFestividades {
    data object Lista : PantallaFestividades
    data class Detalle(val id: String) : PantallaFestividades
}

data class FestividadesUiState(
    /** Cálculo local (CalendarioHebreoService, sin red) — disponible
     *  de inmediato, no espera `cargando`. */
    val estados: List<EstadoFestividad> = emptyList(),
    /** Contenido rico por id, desde festividades_info.json (red). */
    val infoPorId: Map<String, FestividadInfo> = emptyMap(),
    val cargando: Boolean = false,
    val error: String? = null,
    val pantalla: PantallaFestividades = PantallaFestividades.Lista,
    /** Átomos de los bloques_oracion de la festividad ABIERTA
     *  actualmente — se puebla bajo demanda (ver
     *  FestividadesViewModel.cargarBloquesOracion), no todas las
     *  festividades a la vez. */
    val atomosDeOracion: List<Atomo> = emptyList(),
) {
    val estadoAbierto: EstadoFestividad?
        get() {
            val p = pantalla
            return if (p is PantallaFestividades.Detalle) estados.find { it.id == p.id } else null
        }

    val infoAbierta: FestividadInfo?
        get() {
            val p = pantalla
            return if (p is PantallaFestividades.Detalle) infoPorId[p.id] else null
        }
}
