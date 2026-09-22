package com.hacz.jojmakabbalah.oraciones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacz.jojmakabbalah.corpus.Atomo
import com.hacz.jojmakabbalah.corpus.OracionesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Mismo patrón que SalmosViewModel — ver ese archivo para el
 *  razonamiento (StateFlow síncrono, solo `cargar()` toca red). */
class OracionesViewModel(
    private val repo: OracionesRepository,
    atomosIniciales: List<Atomo> = emptyList(),
) : ViewModel() {

    private val _state = MutableStateFlow(OracionesUiState(atomos = atomosIniciales))
    val state: StateFlow<OracionesUiState> = _state.asStateFlow()

    fun cargar() {
        viewModelScope.launch {
            _state.update { it.copy(cargando = true, error = null) }
            try {
                val lista = repo.descargarTodo()
                _state.update { it.copy(atomos = lista, cargando = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: e.toString(), cargando = false) }
            }
        }
    }

    fun abrir(id: String) {
        _state.update { it.copy(pantalla = PantallaOraciones.Detalle(id), idioma = "es") }
    }

    fun volverALaLista() {
        _state.update { it.copy(pantalla = PantallaOraciones.Lista) }
    }

    fun alternarIdioma() {
        _state.update { it.copy(idioma = if (it.idioma == "es") "en" else "es") }
    }
}
