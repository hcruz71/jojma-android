package com.hacz.jojmakabbalah.festividades

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hacz.jojmakabbalah.calendario.EstadoFestividad
import com.hacz.jojmakabbalah.corpus.textoResuelto
import com.hacz.jojmakabbalah.oraciones.OracionDetalleScreen

/// Raíz de navegación de Festividades — mismo patrón que AppSalmos/
/// AppOraciones: NavHost anidado propio + ViewModel propio, montado
/// como UNA pieza en el NavHost exterior del Hub.
///
/// Ruta "oracion/{atomoId}" reutiliza OracionDetalleScreen (el mismo
/// componente de lectura intercalada de Salmos/Oraciones) para los
/// bloques_oracion de la festividad ABIERTA — poblados bajo demanda
/// en `atomosDeOracion`, no comparte el ViewModel de AppOraciones
/// (los bloques por festividad no están en `bloquesCore`).
@Composable
fun AppFestividades() {
    val viewModel: FestividadesViewModel = viewModel { FestividadesViewModel(FestividadesInfoRepository()) }
    val estado by viewModel.state.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(Unit) { viewModel.cargar() }

    NavHost(navController = navController, startDestination = "lista") {
        composable("lista") {
            FestividadesListaScreen(
                estados = estado.estados,
                cargando = estado.cargando && estado.infoPorId.isEmpty(),
                error = estado.error,
                onFestividadClick = { id ->
                    viewModel.abrir(id)
                    navController.navigate("detalle/$id")
                },
            )
        }
        composable("detalle/{id}") {
            val f = estado.estadoAbierto
            val info = estado.infoAbierta
            if (f != null) {
                LaunchedEffect(info?.bloquesOracion) {
                    info?.bloquesOracion?.let { viewModel.cargarBloquesOracion(it) }
                }
                FestividadDetalleScreen(
                    estado = f,
                    info = info,
                    atomosDeOracion = estado.atomosDeOracion,
                    onVolver = {
                        viewModel.volverALaLista()
                        navController.popBackStack()
                    },
                    onAtomoClick = { atomoId -> navController.navigate("oracion/$atomoId") },
                )
            }
        }
        composable("oracion/{atomoId}") { entrada ->
            val atomoId = entrada.arguments?.getString("atomoId")
            val atomo = estado.atomosDeOracion.find { it.id == atomoId }
            if (atomo != null) {
                var idioma by remember(atomo.id) { mutableStateOf("es") }
                OracionDetalleScreen(
                    atomo = atomo,
                    idioma = idioma,
                    textoResuelto = atomo.traducciones.textoResuelto(idioma),
                    onAlternarIdioma = { idioma = if (idioma == "es") "en" else "es" },
                    onVolver = { navController.popBackStack() },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FestividadesListaScreen(
    estados: List<EstadoFestividad>,
    cargando: Boolean,
    error: String?,
    onFestividadClick: (String) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Festividades") }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                cargando && estados.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                error != null && estados.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                }
                else -> LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
                    items(estados, key = { it.id }) { f ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onFestividadClick(f.id) }
                                .padding(vertical = 14.dp),
                        ) {
                            Text(f.nombre, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = subtituloEstado(f),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (f.dentro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

private fun subtituloEstado(f: EstadoFestividad): String = when {
    f.dentro -> "En curso hoy"
    f.dias == 0 -> "Hoy"
    f.dias == 1 -> "Mañana"
    else -> "En ${f.dias} días"
}

/// Detalle de festividad — acotado respecto a FestividadDetalleView.swift
/// (iOS): sin horario de ayuno (Zmanim/GPS, no portado), sin los 7 CTAs
/// de modo guiado por festividad (Januca/Tisha BeAv/Rosh Hashaná/Purim/
/// Sucot/Yom Kipur/Omer — subsistemas completos que no existen en
/// Android todavía). Conexiones se muestran como texto, sin navegación
/// funcional (destinos como nombreDetalle/shabbatMenu tampoco existen
/// todavía). Secretos se muestran sin el gating de FuentesService
/// (no portado — simplificación documentada, ver reporte 2026-09-22).
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FestividadDetalleScreen(
    estado: EstadoFestividad,
    info: FestividadInfo?,
    atomosDeOracion: List<com.hacz.jojmakabbalah.corpus.Atomo>,
    onVolver: () -> Unit,
    onAtomoClick: (String) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(estado.nombre) },
                navigationIcon = {
                    Text("←", modifier = Modifier.padding(horizontal = 16.dp).clickable { onVolver() })
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (info != null && info.nombreHebreo.isNotEmpty()) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = info.nombreHebreo,
                                fontSize = 22.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    if (info?.fechaHebrea?.isNotEmpty() == true) {
                        Text(info.fechaHebrea, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        "${estado.duracion} día${if (estado.duracion != 1) "s" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = subtituloEstado(estado),
                        style = MaterialTheme.typography.titleMedium,
                        color = if (estado.dentro) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            if (info == null) {
                item {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else {
                if (info.conexiones.isNotEmpty()) {
                    item { Text("Conexiones", style = MaterialTheme.typography.labelLarge) }
                    items(info.conexiones) { c ->
                        Text(
                            c.texto,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }

                if (atomosDeOracion.isNotEmpty()) {
                    item { Text("Oraciones", style = MaterialTheme.typography.labelLarge) }
                    items(atomosDeOracion, key = { it.id }) { a ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAtomoClick(a.id) }
                                .padding(vertical = 10.dp),
                        ) {
                            Text(a.nombre, style = MaterialTheme.typography.titleSmall)
                            val nh = a.nombreHebreo
                            if (!nh.isNullOrEmpty()) {
                                Text(nh, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        HorizontalDivider()
                    }
                } else if (info.bloquesOracion.isNotEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.padding(vertical = 12.dp))
                        }
                    }
                }

                item {
                    Column {
                        Text("Acerca de", style = MaterialTheme.typography.labelLarge)
                        Text(
                            info.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }

                if (info.secretos.isNotEmpty()) {
                    item {
                        Column {
                            Text("Secretos", style = MaterialTheme.typography.labelLarge)
                            info.secretos.forEach { s ->
                                Text(
                                    "• $s",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
