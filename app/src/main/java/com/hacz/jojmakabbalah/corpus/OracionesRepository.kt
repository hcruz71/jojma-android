package com.hacz.jojmakabbalah.corpus

import com.google.firebase.Firebase
import com.google.firebase.storage.storage
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
}
