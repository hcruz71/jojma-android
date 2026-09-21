package com.hacz.jojmakabbalah.corpus

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Test JVM puro (sin emulador, sin red) — parsea 3 bloques REALES del
 * corpus de oraciones (copiados tal cual de Jojma-ios/.../, no mocks),
 * elegidos a propósito por variedad de esquema:
 *   - bloque1_birkot_hashajar.json: esquema "oraciones" (bloques 1-10)
 *   - bloque4_amida.json: esquema "oraciones", bloque más grande usado
 *   - bloque11_shabbat.json: esquema "atomos" (bloque11+) — Y trae el
 *     caso real de placeholder «ref_id: ...» que este test verifica
 *     que se DETECTA (no que se resuelve — ver TODO en Modelos.kt).
 */
class OracionesParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun cargar(nombreArchivo: String): List<Atomo> {
        val texto = javaClass.classLoader!!
            .getResourceAsStream("$nombreArchivo.json")!!
            .bufferedReader(Charsets.UTF_8)
            .readText()
        return json.decodeFromString(BloqueOraciones.serializer(), texto).items
    }

    @Test
    fun `bloque1_birkot_hashajar parsea los 13 atomos reales (esquema oraciones)`() {
        val atomos = cargar("bloque1_birkot_hashajar")
        assertEquals(13, atomos.size)
        val modeAni = atomos.find { it.id == "mode_ani" }
        assertNotNull(modeAni)
        assertEquals("Modé Ani", modeAni!!.nombre)
        assertEquals(2, modeAni.hebreo.size)
    }

    @Test
    fun `bloque4_amida parsea los 19 atomos reales (esquema oraciones)`() {
        val atomos = cargar("bloque4_amida")
        assertEquals(19, atomos.size)
        assertEquals("avot", atomos[0].id)
    }

    @Test
    fun `bloque11_shabbat parsea los 8 atomos reales (esquema atomos, distinto de oraciones)`() {
        val atomos = cargar("bloque11_shabbat")
        assertEquals(8, atomos.size)
    }

    @Test
    fun `el placeholder ref_id real se DETECTA correctamente (no se resuelve todavia)`() {
        val atomos = cargar("bloque11_shabbat")
        val kidush = atomos.find { it.id == "kidush_leil_shabbat" }
        assertNotNull(kidush)

        val lineas = kidush!!.hebreo
        // linea 0 es hebreo real, linea 1 es el placeholder real conocido
        assertFalse(esPlaceholderRefId(lineas[0]))
        assertTrue(esPlaceholderRefId(lineas[1]))
        assertEquals("«ref_id: vaijulu»", lineas[1])
        assertEquals("vaijulu", idReferenciado(lineas[1]))

        // Confirma que NO se resuelve (id != null pero sigue siendo
        // el placeholder crudo en traducciones tambien -- mismo criterio
        // que translit.py, que deja la linea intacta).
        val esTexto = kidush.traduccion("es")?.texto
        assertEquals("«ref_id: vaijulu»", esTexto?.get(1))
    }

    @Test
    fun `idReferenciado da null para una linea de hebreo normal`() {
        assertNull(idReferenciado("מוֹדֶה אֲנִי לְפָנֶיךָ מֶלֶךְ חַי וְקַיָּם"))
    }

    // ── Fallback de idioma — mismo comportamiento que Salmo ──────────

    @Test
    fun `fallback a espanol funciona igual para Atomo (dato SINTETICO, reutiliza textoResuelto compartido)`() {
        // NO se duplica la logica de fallback: Atomo usa exactamente
        // el mismo textoResuelto(preferido:) de Modelos.kt que ya se
        // probo con Salmo -- este test confirma que funciona igual
        // para Atomo sin reescribir nada.
        val atomoSinteticoSinIngles = Atomo(
            id = "atomo_test_sintetico",
            nombre = "Oracion de prueba (sintetica, no es del corpus)",
            hebreo = listOf("שָׁלוֹם"),
            traducciones = mapOf(
                "es" to TraduccionContenido(texto = listOf("Paz")),
                // sin "en" a proposito
            ),
        )

        val resuelto = atomoSinteticoSinIngles.traducciones.textoResuelto("en")
        assertNotNull(resuelto)
        assertEquals(listOf("Paz"), resuelto!!.lineas)
        assertEquals("en", resuelto.pedido)
        assertEquals("es", resuelto.idioma)
        assertTrue(resuelto.esFallback)
    }
}
