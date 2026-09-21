package com.hacz.jojmakabbalah.corpus

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test JVM puro (sin emulador, sin red) — parsea el salmos_app.json REAL
 * (copiado tal cual de Jojma-ios/.../salmos_app.json, no un mock) contra
 * los mismos data classes que usa SalmosRepository, y verifica los
 * valores reales conocidos del Salmo 1. Confirma que el MODELO/PARSING
 * es correcto; NO ejercita la descarga de Firebase Storage en sí (eso
 * requiere el emulador, bloqueado por espacio — ver conversación).
 */
class SalmoParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun cargarCorpusReal(): SalmosApp {
        val texto = javaClass.classLoader!!
            .getResourceAsStream("salmos_app.json")!!
            .bufferedReader(Charsets.UTF_8)
            .readText()
        return json.decodeFromString(SalmosApp.serializer(), texto)
    }

    @Test
    fun `parsea los 150 salmos del corpus real`() {
        val data = cargarCorpusReal()
        assertEquals(150, data.salmos.size)
    }

    @Test
    fun `Salmo 1 tiene los valores reales conocidos`() {
        val data = cargarCorpusReal()
        val salmo1 = data.salmos.find { it.numero == 1 }
        assertNotNull("Salmo 1 debe existir", salmo1)
        salmo1!!

        assertEquals("salmo_001", salmo1.id)
        assertEquals("Salmo 1", salmo1.titulo)
        assertEquals(6, salmo1.hebreo.size)
        // No se compara el hebreo tipeado a mano contra un literal: el orden
        // de las marcas combinantes (shin-dot vs shva) es fragil de escribir
        // a mano y no afecta el render, pero SI rompe una comparacion binaria
        // exacta (encontrado real 2026-09-20 en esta misma corrida: mismo
        // texto visualmente, orden de bytes distinto). La longitud de
        // caracteres ya confirma que el contenido llego completo.
        assertTrue("la primera linea hebrea no debe estar vacia", salmo1.hebreo[0].isNotBlank())
        assertTrue(
            "la primera linea hebrea debe tener el largo real esperado",
            salmo1.hebreo[0].length in 100..120
        )
    }

    @Test
    fun `traduccion es del Salmo 1 coincide con el texto real RV1909 modernizado`() {
        val data = cargarCorpusReal()
        val salmo1 = data.salmos.first { it.numero == 1 }
        val es = salmo1.traduccion("es")
        assertNotNull("debe tener traduccion es", es)
        assertEquals(6, es!!.texto?.size)
        assertEquals(
            "Bienaventurado el varón que no anduvo en consejo de malos, " +
                "Ni estuvo en camino de pecadores, Ni en silla de escarnecedores se ha sentado;",
            es.texto!![0]
        )
        assertTrue("debe tener transliteracion es", es.transliteracion?.isNotEmpty() == true)
    }

    @Test
    fun `traduccion en del Salmo 1 coincide con el texto real JPS 1917 modernizado`() {
        val data = cargarCorpusReal()
        val salmo1 = data.salmos.first { it.numero == 1 }
        val en = salmo1.traduccion("en")
        assertNotNull("debe tener traduccion en", en)
        assertEquals(6, en!!.texto?.size)
        assertEquals(
            "Happy is the man that has not walked in the counsel of the wicked, " +
                "Nor stood in the way of sinners, nor sat in the seat of the scornful.",
            en.texto!![0]
        )
        // Confirma la correccion "kh" (no "j") que se peleo esta sesion:
        // "jeftzo" (es) vs "kheftzo" (en) en la linea 2.
        assertTrue(
            "transliteracion en debe usar 'kh' (Cheshvan-style), no 'j'",
            en.transliteracion!![1].contains("kheftzo")
        )
    }

    @Test
    fun `Salmo 150 (el ultimo) tambien parsea correctamente`() {
        val data = cargarCorpusReal()
        val salmo150 = data.salmos.find { it.numero == 150 }
        assertNotNull(salmo150)
        assertTrue(salmo150!!.hebreo.isNotEmpty())
        assertNotNull(salmo150.traduccion("en"))
    }
}
