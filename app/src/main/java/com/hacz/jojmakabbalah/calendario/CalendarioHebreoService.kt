package com.hacz.jojmakabbalah.calendario

import com.kosherjava.zmanim.hebrewcalendar.JewishDate
import java.time.ZoneId
import java.util.Calendar
import java.util.Date

/**
 * Puerto de CalendarioHebreoService.swift (iOS), acotado a lo que
 * necesita el módulo de Festividades: "¿qué festividad está en curso
 * o es la próxima?". Usa `com.kosherjava:zmanim` (JewishDate) para la
 * conversión hebreo→gregoriano — librería real, evaluada 2026-09-22
 * en vez de reimplementar el algoritmo desde cero (java.time no trae
 * calendario hebreo).
 *
 * Diferencias deliberadas respecto a iOS (documentadas, no bugs):
 * - Sin roll al atardecer (`fechaEfectiva` en iOS usa el sunset real
 *   vía GPS/NombreActualService, que no existe en Android). "Hoy" acá
 *   es la medianoche gregoriana local, sin ajuste solar — cerca del
 *   atardecer puede mostrar "en curso"/"próxima" unas horas distinto
 *   que iOS. TODO(calendario-sunset) si se porta NombreActualService.
 * - `JewishDate` usa numeración de meses base NISSAN=1 (Nissan=1,
 *   Tishrei=7...); iOS/Foundation usa base TISHREI=1. Cada entrada de
 *   `tabla` ya está mapeada al mes real de JewishDate — no reusar el
 *   `mes:Int` de iOS directamente, los esquemas NO coinciden.
 */
data class EstadoFestividad(
    val id: String,
    val nombre: String,
    val fechaInicio: Date,
    val duracion: Int,
    val dias: Int,
    val dentro: Boolean,
)

private data class Festividad(
    val id: String,
    val nombre: String,
    /** null = Adar ambiguo (Ester/Purim): resolver vía isJewishLeapYear. */
    val mes: Int?,
    val dia: Int,
    val duracion: Int,
)

object CalendarioHebreoService {

    /** 15 festividades — mismos id/nombre/mes/día/duración que
     *  CalendarioHebreoService.tabla (iOS), verificados contra el
     *  código real 2026-09-22. */
    private val tabla = listOf(
        Festividad("rosh_hashana", "Rosh Hashaná", JewishDate.TISHREI, 1, 2),
        Festividad("gedalya", "Tzom Gedalya", JewishDate.TISHREI, 3, 1),
        Festividad("yom_kipur", "Yom Kipur", JewishDate.TISHREI, 10, 1),
        Festividad("sukot", "Sukot", JewishDate.TISHREI, 15, 7),
        Festividad("shmini_atzeret", "Shminí Atzéret", JewishDate.TISHREI, 22, 2),
        Festividad("tenth_tevet", "Asará BeTevet", JewishDate.TEVES, 10, 1),
        Festividad("januca", "Janucá", JewishDate.KISLEV, 25, 8),
        Festividad("tu_bishvat", "Tu BiShvat", JewishDate.SHEVAT, 15, 1),
        Festividad("ester", "Taanit Ester", null, 13, 1),
        Festividad("purim", "Purim", null, 14, 1),
        Festividad("pesaj", "Pesaj", JewishDate.NISSAN, 15, 7),
        Festividad("lag_baomer", "Lag BaOmer", JewishDate.IYAR, 18, 1),
        Festividad("shavuot", "Shavuot", JewishDate.SIVAN, 6, 2),
        Festividad("tamuz", "Shivá Asar BeTamuz", JewishDate.TAMMUZ, 17, 1),
        Festividad("tisha_beav", "Tishá BeAv", JewishDate.AV, 9, 1),
    )

    private fun mesEfectivo(f: Festividad, year: Int): Int {
        if (f.mes != null) return f.mes
        val jd = JewishDate()
        jd.jewishYear = year
        return if (jd.isJewishLeapYear) JewishDate.ADAR_II else JewishDate.ADAR
    }

    private fun inicioDeMedianoche(d: Date): Date {
        val c = Calendar.getInstance()
        c.time = d
        c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
        return c.time
    }

    private fun diasEntre(desde: Date, hasta: Date): Int {
        val msPorDia = 24L * 60 * 60 * 1000
        return ((hasta.time - desde.time) / msPorDia).toInt()
    }

    private fun resolver(fest: Festividad, year: Int, hoyMid: Date): EstadoFestividad {
        val jd = JewishDate()
        jd.setJewishDate(year, mesEfectivo(fest, year), fest.dia)
        // JewishDate NO extiende Calendar y no tiene getGregorianCalendar()
        // (una fuente externa lo mencionaba, verificado FALSO contra el
        // .java real en GitHub 2026-09-22) — el método real es
        // getLocalDate(): java.time.LocalDate.
        val inicioLocalDate = jd.localDate
        val inicioDate = Date.from(inicioLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val inicioMid = inicioDeMedianoche(inicioDate)
        val finMid = Calendar.getInstance().apply {
            time = inicioMid
            add(Calendar.DAY_OF_YEAR, fest.duracion)
        }.time
        val diff = diasEntre(hoyMid, inicioMid)
        val dentro = hoyMid >= inicioMid && hoyMid < finMid
        return EstadoFestividad(fest.id, fest.nombre, inicioMid, fest.duracion, diff, dentro)
    }

    /** Lista del próximo año, "dentro" primero, luego por proximidad —
     *  mismo criterio que `listaProximoAnio()` en iOS. Cada festividad
     *  aparece una sola vez (la más cercana / la vigente). */
    fun listaProximoAnio(hoy: Date = Date()): List<EstadoFestividad> {
        val hoyMid = inicioDeMedianoche(hoy)
        val yearActual = JewishDate().jewishYear

        val candidatos = mutableListOf<EstadoFestividad>()
        for (fest in tabla) {
            for (offset in 0..1) {
                candidatos.add(resolver(fest, yearActual + offset, hoyMid))
            }
        }

        val porId = LinkedHashMap<String, EstadoFestividad>()
        for (est in candidatos) {
            if (est.dentro) {
                porId[est.id] = est
            } else if (est.dias >= 0) {
                val existente = porId[est.id]
                if (existente == null || (!existente.dentro && est.dias < existente.dias)) {
                    porId[est.id] = est
                }
            }
        }
        return porId.values.sortedWith(compareBy({ !it.dentro }, { it.dias }))
    }

    fun festividad(porId: String, hoy: Date = Date()): EstadoFestividad? =
        listaProximoAnio(hoy).find { it.id == porId }
}
