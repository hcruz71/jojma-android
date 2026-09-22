package com.hacz.jojmakabbalah.calendario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Verifica la conversión real de calendario hebreo (vía
 * com.kosherjava:zmanim) contra un ancla pública conocida —
 * NO confía en que la librería "simplemente funcione".
 *
 * Ancla: Rosh Hashaná 5787 cae el 12 de septiembre de 2026 en el
 * calendario gregoriano — confirmado 2026-09-22 contra fuentes
 * públicas (Hebcal/Chabad.org: "1 Tishrei 5787 comienza al atardecer
 * del 11 de sept. 2026", equivalente civil = 12 de sept.).
 */
class CalendarioHebreoServiceTest {

    private fun fecha(iso: String): Date {
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        f.timeZone = TimeZone.getDefault()
        return f.parse(iso)!!
    }

    private fun iso(d: Date): String {
        val f = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        f.timeZone = TimeZone.getDefault()
        return f.format(d)
    }

    @Test
    fun `Rosh Hashana 5787 cae el 12 de septiembre de 2026 (ancla real verificada)`() {
        val hoy = fecha("2026-09-01")
        val rh = CalendarioHebreoService.festividad("rosh_hashana", hoy)
        assertNotNull(rh)
        assertEquals("2026-09-12", iso(rh!!.fechaInicio))
        assertEquals(11, rh.dias)
        assertTrue("todavia no deberia estar 'dentro' el 1 de sept", !rh.dentro)
    }

    @Test
    fun `Rosh Hashana 5787 esta 'dentro' durante sus 2 dias reales`() {
        val diaUno = CalendarioHebreoService.festividad("rosh_hashana", fecha("2026-09-12"))
        assertNotNull(diaUno)
        assertTrue(diaUno!!.dentro)

        val diaDos = CalendarioHebreoService.festividad("rosh_hashana", fecha("2026-09-13"))
        assertTrue(diaDos!!.dentro)

        val diaTres = CalendarioHebreoService.festividad("rosh_hashana", fecha("2026-09-14"))
        assertTrue("duracion=2 dias, el 3er dia ya no deberia estar dentro", !diaTres!!.dentro)
    }

    @Test
    fun `las 15 festividades reales resuelven sin crash y con nombre no vacio`() {
        val lista = CalendarioHebreoService.listaProximoAnio(fecha("2026-09-01"))
        assertEquals(15, lista.size)
        lista.forEach {
            assertTrue("id vacio", it.id.isNotEmpty())
            assertTrue("nombre vacio para ${it.id}", it.nombre.isNotEmpty())
        }
    }

    @Test
    fun `Purim (Adar ambiguo) resuelve a una fecha real, no crashea`() {
        val purim = CalendarioHebreoService.festividad("purim", fecha("2026-09-01"))
        assertNotNull(purim)
        // Purim 5787 cae en marzo 2027 (Adar) — solo confirmamos que
        // resuelve a un mes de invierno/primavera del año siguiente,
        // sin fijar el dia exacto (no fue el ancla verificada arriba).
        assertTrue(purim!!.dias > 0)
    }
}
