package com.hacz.jojmakabbalah.salmos

import com.hacz.jojmakabbalah.corpus.Salmo
import com.hacz.jojmakabbalah.corpus.SalmosApp
import com.hacz.jojmakabbalah.corpus.SalmosRepository
import com.hacz.jojmakabbalah.corpus.TraduccionContenido
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de SalmosViewModel — puros JVM, sin Robolectric ni emulador.
 * `abrir`/`volverALaLista`/`alternarIdioma` son actualizaciones
 * síncronas de StateFlow (no usan viewModelScope), así que se pueden
 * llamar directo y leer `.value` sin infraestructura de coroutines-test.
 *
 * Los 150 salmos reales se cargan una sola vez desde el mismo JSON de
 * salmos_app.json (test/resources), reutilizando el patrón de
 * SalmoParsingTest — no se inventan datos de prueba.
 */
class SalmosViewModelTest {

    companion object {
        // Cargado una sola vez para todos los tests de esta clase —
        // parsear 150 salmos en cada @Test sería lento y redundante.
        private val salmosReales: List<Salmo> by lazy {
            val json = Json { ignoreUnknownKeys = true }
            val texto = SalmosViewModelTest::class.java.classLoader!!
                .getResourceAsStream("salmos_app.json")!!
                .bufferedReader(Charsets.UTF_8)
                .readText()
            json.decodeFromString(SalmosApp.serializer(), texto).salmos
        }
    }

    private fun nuevoViewModel(): SalmosViewModel =
        // SalmosRepository real, pero nunca se llama a cargar() en
        // estos tests (sembramos el estado directo vía
        // salmosIniciales) — no se toca la red.
        SalmosViewModel(SalmosRepository(), salmosIniciales = salmosReales)

    @Test
    fun `los 150 salmos se cargan correctamente en el estado inicial`() {
        val vm = nuevoViewModel()
        assertEquals(150, vm.state.value.salmos.size)
        assertEquals(PantallaSalmos.Lista, vm.state.value.pantalla)
    }

    @Test
    fun `abrir navega a Detalle con el numero correcto y vuelve a la Lista`() {
        val vm = nuevoViewModel()

        vm.abrir(23)
        val pantallaAbierta = vm.state.value.pantalla
        assertTrue(pantallaAbierta is PantallaSalmos.Detalle)
        assertEquals(23, (pantallaAbierta as PantallaSalmos.Detalle).numero)
        assertNotNull("salmoAbierto debe resolver el Salmo 23 real", vm.state.value.salmoAbierto)
        assertEquals(23, vm.state.value.salmoAbierto!!.numero)

        vm.volverALaLista()
        assertEquals(PantallaSalmos.Lista, vm.state.value.pantalla)
        assertNull("sin pantalla Detalle, salmoAbierto debe ser null", vm.state.value.salmoAbierto)
    }

    @Test
    fun `abrir un salmo nuevo resetea el idioma a es`() {
        val vm = nuevoViewModel()
        vm.abrir(1)
        vm.alternarIdioma() // -> en
        assertEquals("en", vm.state.value.idioma)

        vm.abrir(2) // abrir otro salmo debe resetear
        assertEquals("es", vm.state.value.idioma)
    }

    // ── Toggle de idioma para 3 salmos reales distintos ──────────────

    @Test
    fun `toggle de idioma cambia el texto mostrado — Salmo 1 (caso de control, valores ya conocidos)`() {
        val vm = nuevoViewModel()
        vm.abrir(1)

        assertEquals("es", vm.state.value.idioma)
        val textoEs = vm.state.value.textoMostrado
        assertEquals(
            "Bienaventurado el varón que no anduvo en consejo de malos, " +
                "Ni estuvo en camino de pecadores, Ni en silla de escarnecedores se ha sentado;",
            textoEs?.get(0),
        )

        vm.alternarIdioma()
        assertEquals("en", vm.state.value.idioma)
        val textoEn = vm.state.value.textoMostrado
        assertEquals(
            "Happy is the man that has not walked in the counsel of the wicked, " +
                "Nor stood in the way of sinners, nor sat in the seat of the scornful.",
            textoEn?.get(0),
        )
        assertTrue("el texto debe cambiar de verdad, no quedar igual", textoEs != textoEn)
    }

    @Test
    fun `toggle de idioma cambia el texto mostrado — Salmo 23 (famoso, distinto largo)`() {
        val vm = nuevoViewModel()
        vm.abrir(23)
        val salmo23 = vm.state.value.salmoAbierto!!
        val esperadoEs = salmo23.traduccion("es")!!.texto!!
        val esperadoEn = salmo23.traduccion("en")!!.texto!!

        assertEquals(esperadoEs, vm.state.value.textoMostrado)
        vm.alternarIdioma()
        assertEquals(esperadoEn, vm.state.value.textoMostrado)
        assertTrue(esperadoEs != esperadoEn)
    }

    @Test
    fun `toggle de idioma cambia el texto mostrado — Salmo 150 (el ultimo, caso borde)`() {
        val vm = nuevoViewModel()
        vm.abrir(150)
        val salmo150 = vm.state.value.salmoAbierto!!
        val esperadoEs = salmo150.traduccion("es")!!.texto!!
        val esperadoEn = salmo150.traduccion("en")!!.texto!!

        assertEquals(esperadoEs, vm.state.value.textoMostrado)
        vm.alternarIdioma()
        assertEquals(esperadoEn, vm.state.value.textoMostrado)
        assertTrue(esperadoEs != esperadoEn)

        // alternar de vuelta debe volver exacto al es original
        vm.alternarIdioma()
        assertEquals(esperadoEs, vm.state.value.textoMostrado)
    }

    // ── Fallback a español cuando falta "en" ─────────────────────────
    //
    // NINGÚN salmo real carece de "en" (150/150 confirmados con
    // traducción completa esta sesión) — este Salmo es un DATO
    // SINTÉTICO construido a mano solo para forzar el path de
    // fallback, no viene del corpus. Verifica paridad con
    // Dictionary.textoResuelto(preferido:) de IdiomaContenido.swift
    // (iOS): idioma pedido → español → null si ninguno existe.

    private val salmoSinteticoSinIngles = Salmo(
        id = "salmo_test_sintetico",
        numero = 9999,
        titulo = "Salmo de prueba (sintético, no es del corpus)",
        hebreo = listOf("שָׁלוֹם"),
        traducciones = mapOf(
            "es" to TraduccionContenido(texto = listOf("Paz")),
            // Sin entrada "en" a propósito.
        ),
    )

    @Test
    fun `con fallback, pedir en para un salmo sin traduccion en debe caer a espanol (paridad con iOS)`() {
        val vm = SalmosViewModel(SalmosRepository(), salmosIniciales = listOf(salmoSinteticoSinIngles))
        vm.abrir(9999)
        vm.alternarIdioma() // el USUARIO sigue pidiendo "en"

        // El idioma PEDIDO no cambia -- el toggle sigue reflejando "en".
        assertEquals("en", vm.state.value.idioma)

        // Pero el texto MOSTRADO cae a espanol, igual que
        // textoResuelto(preferido:) en iOS.
        assertEquals(listOf("Paz"), vm.state.value.textoMostrado)

        val resuelto = vm.state.value.textoResueltoActual!!
        assertEquals("en", resuelto.pedido)
        assertEquals("es", resuelto.idioma)
        assertTrue("esFallback debe ser true", resuelto.esFallback)
    }

    @Test
    fun `si tampoco hay espanol, textoMostrado es null (sin datos que mostrar)`() {
        val salmoSinNadaEnElIdiomaPedidoNiEnEspanol = Salmo(
            id = "salmo_test_sin_nada",
            numero = 9998,
            titulo = "Salmo de prueba (sintético, sin es ni en)",
            hebreo = listOf("שָׁלוֹם"),
            traducciones = emptyMap(),
        )
        val vm = SalmosViewModel(
            SalmosRepository(),
            salmosIniciales = listOf(salmoSinNadaEnElIdiomaPedidoNiEnEspanol),
        )
        vm.abrir(9998)
        assertNull(vm.state.value.textoMostrado)
        assertNull(vm.state.value.textoResueltoActual)
    }
}
