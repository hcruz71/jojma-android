package com.hacz.jojmakabbalah.corpus

import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

/**
 * Descarga un bloque de oraciones (corpus/{nombre}.json) desde
 * Firebase Storage y lo parsea con BloqueOraciones, que tolera los
 * dos esquemas reales del corpus ("oraciones" en bloques 1-10,
 * "atomos" en bloque11+/preservados — mismo criterio que
 * atomos_de() en scripts/translit.py).
 *
 * Mismo patrón que SalmosRepository — ver ese archivo para las
 * decisiones ya tomadas (sin caché local todavía, descarga completa
 * cada vez).
 */
class OracionesRepository {

    private val json = Json { ignoreUnknownKeys = true }

    /** 2MB de margen — el bloque real más grande de este corpus
     *  (bloque1_birkot_hashajar.json) pesa ~60KB. */
    private val maxBytes: Long = 2L * 1024 * 1024

    suspend fun descargarBloque(nombreArchivo: String): List<Atomo> {
        val ref = Firebase.storage.reference.child("corpus/$nombreArchivo.json")
        val bytes = ref.getBytes(maxBytes).await()
        val texto = bytes.toString(Charsets.UTF_8)
        val data = json.decodeFromString(BloqueOraciones.serializer(), texto)
        return data.items
    }

    /** Los 11 bloques numerados (liturgia diaria + Shabat) — el núcleo
     *  que cubre la mayoría de las 25 categorías de OracionesListaView.
     *  NO incluye los bloques por festividad (bloque_januca, bloque_purim,
     *  bloque_rosh_hashana, etc. — 18 archivos más) ni
     *  oraciones_preservados.json — fuera de alcance de este pase,
     *  ver reporte 2026-09-21. Ampliar esta lista es agregar strings,
     *  no cambiar lógica. */
    private val bloquesCore = listOf(
        "bloque1_birkot_hashajar", "bloque2_pesukei_dezimra", "bloque3_shema_uvirjoteha",
        "bloque4_amida", "bloque5_cierre_shajrit", "bloque6_minja", "bloque7_arvit",
        "bloque8_shabat", "bloque9_festividades", "bloque10_brajot", "bloque11_shabbat",
    )

    /** Descarga los bloques core en paralelo y los une en una sola lista,
     *  deduplicada por id (primero-gana, mismo criterio que
     *  OracionesService.swift). Un bloque individual que falle (404, red)
     *  no tumba el resto — se descarta silenciosamente vía runCatching,
     *  ya que perder UN bloque no debe dejar toda la sección de Oraciones
     *  en blanco. */
    suspend fun descargarTodo(): List<Atomo> = coroutineScope {
        bloquesCore
            .map { nombre -> async { runCatching { descargarBloque(nombre) }.getOrDefault(emptyList()) } }
            .map { it.await() }
            .flatten()
            .distinctBy { it.id }
    }
}
