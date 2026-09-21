package com.hacz.jojmakabbalah.salmos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacz.jojmakabbalah.corpus.Salmo
import com.hacz.jojmakabbalah.corpus.SalmosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel real de androidx.lifecycle — SIN dependencia de
 * Robolectric ni emulador para testearlo: `abrir`/`volverALaLista`/
 * `alternarIdioma` son actualizaciones síncronas de StateFlow, no
 * necesitan coroutines-test. Solo `cargar()` usa viewModelScope
 * (red real vía SalmosRepository); los tests la evitan sembrando el
 * estado con `salmosIniciales` en el constructor.
 */
class SalmosViewModel(
    private val repo: SalmosRepository,
    salmosIniciales: List<Salmo> = emptyList(),
) : ViewModel() {

    private val _state = MutableStateFlow(SalmosUiState(salmos = salmosIniciales))
    val state: StateFlow<SalmosUiState> = _state.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _state.update { it.copy(cargando = true, error = null) }
            try {
                val lista = repo.descargarTodos()
                _state.update { it.copy(salmos = lista, cargando = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: e.toString(), cargando = false) }
            }
        }
    }

    fun abrir(numero: Int) {
        // Idioma siempre arranca en "es" al abrir un salmo nuevo —
        // mismo criterio que la pieza anterior (sin persistencia).
        _state.update { it.copy(pantalla = PantallaSalmos.Detalle(numero), idioma = "es") }
    }

    fun volverALaLista() {
        _state.update { it.copy(pantalla = PantallaSalmos.Lista) }
    }

    fun alternarIdioma() {
        _state.update { it.copy(idioma = if (it.idioma == "es") "en" else "es") }
    }
}
