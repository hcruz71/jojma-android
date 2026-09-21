package com.hacz.jojmakabbalah.corpus

import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json

/**
 * Descarga salmos_app.json desde Firebase Storage (corpus/, lectura
 * pública sin auth — ver storage.rules: match /corpus/{allPaths=**}
 * { allow read: if true; }) y lo parsea contra el esquema real.
 *
 * Primera pieza (2026-09-20): sin caché local todavía, descarga
 * completa cada vez — el archivo real pesa ~1.3MB. Cachear/sincronizar
 * incremental (equivalente a ContentSyncService.swift) es trabajo
 * posterior, no de esta pieza.
 */
class SalmosRepository {

    private val json = Json { ignoreUnknownKeys = true }

    /** Máximo de bytes a descargar — salmos_app.json real pesa
     *  ~1.3MB (verificado contra el archivo local); 5MB da margen
     *  amplio sin ser un límite absurdo. */
    private val maxBytes: Long = 5L * 1024 * 1024

    suspend fun descargarSalmo(numero: Int): Salmo {
        val ref = Firebase.storage.reference.child("corpus/salmos_app.json")
        val bytes = ref.getBytes(maxBytes).await()
        val texto = bytes.toString(Charsets.UTF_8)
        val data = json.decodeFromString(SalmosApp.serializer(), texto)
        return data.salmos.find { it.numero == numero }
            ?: error("Salmo $numero no encontrado en salmos_app.json (${data.salmos.size} salmos descargados)")
    }
}
