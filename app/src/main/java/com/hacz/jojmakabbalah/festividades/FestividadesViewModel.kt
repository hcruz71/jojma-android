package com.hacz.jojmakabbalah.festividades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacz.jojmakabbalah.calendario.CalendarioHebreoService
import com.hacz.jojmakabbalah.corpus.OracionesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FestividadesViewModel(
    private val repo: FestividadesInfoRepository,
    private val oraciones: OracionesRepository = OracionesRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(
        // El cálculo de fechas es local y rápido — se puebla de una
        // vez en la construcción, sin esperar red (a diferencia de
        // Salmos/Oraciones, que dependen 100% de Storage).
        FestividadesUiState(estados = CalendarioHebreoService.listaProximoAnio()),
    )
    val state: StateFlow<FestividadesUiState> = _state.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _state.update { it.copy(cargando = true, error = null) }
            try {
                val info = repo.descargarInfo()
                _state.update { it.copy(infoPorId = info.associateBy { i -> i.id }, cargando = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: e.toString(), cargando = false) }
            }
        }
    }

    fun abrir(id: String) {
        _state.update { it.copy(pantalla = PantallaFestividades.Detalle(id), atomosDeOracion = emptyList()) }
    }

    fun volverALaLista() {
        _state.update { it.copy(pantalla = PantallaFestividades.Lista) }
    }

    /** Descarga los bloques de oración de LA festividad abierta —
     *  bajo demanda (no se bajan los 15*N bloques de golpe). Tolerante
     *  a bloques individuales que fallen, mismo criterio que
     *  OracionesRepository.descargarTodo(). */
    fun cargarBloquesOracion(bloques: List<String>) {
        if (bloques.isEmpty()) return
        viewModelScope.launch {
            val atomos = bloques
                .map { nombre -> runCatching { oraciones.descargarBloque(nombre) }.getOrDefault(emptyList()) }
                .flatten()
                .distinctBy { it.id }
            _state.update { it.copy(atomosDeOracion = atomos) }
        }
    }
}
