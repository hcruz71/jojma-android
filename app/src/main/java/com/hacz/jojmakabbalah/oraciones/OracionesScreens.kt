package com.hacz.jojmakabbalah.oraciones

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.hacz.jojmakabbalah.corpus.Atomo
import com.hacz.jojmakabbalah.corpus.OracionesRepository
import com.hacz.jojmakabbalah.corpus.esPlaceholderRefId

/// Orden litúrgico real — copiado literalmente de
/// OracionesListaView.categoriasOrdenadas (iOS). "Secuencias" (bloque
/// aparte, con su propio modelo Secuencia/SecuenciaView) NO se porta
/// en este pase — TODO(oraciones-secuencias) explícito, fuera de
/// alcance del reporte 2026-09-21.
private val CATEGORIAS_ORDENADAS = listOf(
    "preliminar_kabbalistico", "birkot_hashajar", "pesukei_dezimra",
    "shema_uvirjoteha", "amida", "cierre_tefila",
    "nucleo_diario", "mistico", "braja_acto", "brajot", "berajot", "bendicion_especial",
    "arvit",
    "shabbat", "shabat", "shabbat_festividad",
    "festividades", "rosh_hashana", "yom_kipur", "tisha_beav",
    "januca", "purim", "sucot", "omer",
    "torah_incrustada",
)

private fun nombreLegible(cat: String): String = when (cat) {
    "preliminar_kabbalistico" -> "Preliminares kabbalísticos"
    "birkot_hashajar" -> "Birkot HaShajar"
    "pesukei_dezimra" -> "Pesukei deZimrá"
    "shema_uvirjoteha" -> "Shemá y sus berajot"
    "amida" -> "Amidá"
    "cierre_tefila" -> "Cierre de la tefila"
    "nucleo_diario" -> "Núcleo diario"
    "mistico" -> "Místicas"
    "braja_acto" -> "Bendiciones"
    "brajot" -> "Berajot"
    "berajot" -> "Berajot"
    "bendicion_especial" -> "Especiales"
    "arvit" -> "Arvit"
    "shabbat" -> "Shabat"
    "shabat" -> "Shabat"
    "shabbat_festividad" -> "Shabat y festividades"
    "festividades" -> "Festividades"
    "rosh_hashana" -> "Rosh Hashaná"
    "yom_kipur" -> "Yom Kipur"
    "tisha_beav" -> "Tishá BeAv"
    "januca" -> "Janucá"
    "purim" -> "Purim"
    "sucot" -> "Sukot"
    "omer" -> "Sefirat HaOmer"
    "torah_incrustada" -> "Torá incrustada"
    else -> cat.replaceFirstChar { it.uppercase() }
}

private fun tituloPara(a: Atomo): String =
    if (a.porCompletar) "${a.nombre} · Por completar" else a.nombre

private fun subtituloPara(a: Atomo): String =
    a.nota?.takeIf { it.isNotEmpty() } ?: (a.nombreHebreo ?: "")

/// Raíz de navegación del módulo Oraciones — mismo patrón que
/// AppSalmos() en MainActivity.kt: NavHost anidado propio + su propio
/// ViewModel, montado como UNA pieza dentro del NavHost exterior del
/// Hub.
@Composable
fun AppOraciones() {
    val viewModel: OracionesViewModel = viewModel { OracionesViewModel(OracionesRepository()) }
    val estado by viewModel.state.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(Unit) { viewModel.cargar() }

    NavHost(navController = navController, startDestination = "lista") {
        composable("lista") {
            OracionesListaScreen(
                atomos = estado.atomos,
                cargando = estado.cargando,
                error = estado.error,
                onAtomoClick = { atomo ->
                    viewModel.abrir(atomo.id)
                    navController.navigate("detalle/${atomo.id}")
                },
            )
        }
        composable("detalle/{id}") {
            val atomo = estado.atomoAbierto
            if (atomo != null) {
                OracionDetalleScreen(
                    atomo = atomo,
                    idioma = estado.idioma,
                    textoResuelto = estado.textoResueltoActual,
                    onAlternarIdioma = viewModel::alternarIdioma,
                    onVolver = {
                        viewModel.volverALaLista()
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OracionesListaScreen(
    atomos: List<Atomo>,
    cargando: Boolean,
    error: String?,
    onAtomoClick: (Atomo) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Oraciones") }) }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("Descargando oraciones…", modifier = Modifier.padding(top = 16.dp))
                    }
                }
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 16.dp),
                ) {
                    CATEGORIAS_ORDENADAS.forEach { cat ->
                        val lista = atomos.filter { it.categoria == cat }
                        if (lista.isNotEmpty()) {
                            item(key = "header_$cat") {
                                Text(
                                    text = nombreLegible(cat),
                                    style = MaterialTheme.typography.headlineSmall,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                                )
                            }
                            itemsIndexed(lista, key = { _, a -> a.id }) { _, a ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAtomoClick(a) }
                                        .padding(vertical = 12.dp),
                                ) {
                                    Text(tituloPara(a), style = MaterialTheme.typography.titleMedium)
                                    val sub = subtituloPara(a)
                                    if (sub.isNotEmpty()) {
                                        Text(
                                            sub,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

/// Detalle de una oración — mismo patrón de intercalado hebreo[i] +
/// traducción/transliteración[i] que DetalleSalmoScreen, adaptado a
/// Atomo: header con nombre + nombre_hebreo (Atomo lo tiene, Salmo
/// no), sin Kavaná (Oraciones no tiene ese feature ni en iOS), sin
/// toggle qere/ketiv (nicho — solo Eijá lo usa, fuera de alcance).
///
/// Referencias sin resolver (esRef && hebreo vacío): tarjeta
/// placeholder en vez de crashear o mostrar nada — incluye las que
/// apuntan a salmos (`esRefASalmo`), que en iOS SÍ redirigen al
/// lector nativo pero acá NO (ver TODO en Modelos.kt).
///
/// Placeholder inline `«ref_id: ...»` dentro de una línea de hebreo
/// normal: esa línea puntual se reemplaza por un aviso, el resto del
/// átomo se renderiza normal.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OracionDetalleScreen(
    atomo: Atomo,
    idioma: String,
    textoResuelto: com.hacz.jojmakabbalah.corpus.TraduccionResuelta?,
    onAlternarIdioma: () -> Unit,
    onVolver: () -> Unit,
) {
    val tieneIngles = atomo.traduccion("en") != null
    var mostrarTranslit by remember(atomo.id) { mutableStateOf(true) }
    var mostrarTraduccion by remember(atomo.id) { mutableStateOf(false) }

    val translitLineas = atomo.traducciones[idioma]?.transliteracion ?: emptyList()
    val esReferenciaSinResolver = atomo.esRef && atomo.hebreo.isEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(atomo.nombre) },
                navigationIcon = {
                    Text("←", modifier = Modifier.padding(horizontal = 16.dp).clickable { onVolver() })
                },
                actions = {
                    if (tieneIngles) {
                        TextButton(onClick = onAlternarIdioma) {
                            Text(if (idioma == "es") "EN" else "ES")
                        }
                    }
                    if (!esReferenciaSinResolver) {
                        TextButton(onClick = { mostrarTranslit = !mostrarTranslit }) {
                            Text("Aa", color = if (mostrarTranslit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { mostrarTraduccion = !mostrarTraduccion }) {
                            Text("🌐", color = if (mostrarTraduccion) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (!atomo.nombreHebreo.isNullOrEmpty()) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                            Text(
                                text = atomo.nombreHebreo,
                                fontSize = 22.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (atomo.porCompletar) {
                        Text(
                            "Por completar",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (esReferenciaSinResolver) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "Contenido compartido — pendiente de resolver",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (atomo.esRefASalmo) {
                            Text(
                                "(Referencia a Salmos — navegación directa todavía no implementada en Android)",
                                style = MaterialTheme.typography.labelSmall,
                                fontStyle = FontStyle.Italic,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(atomo.hebreo, key = { i, _ -> i }) { i, hebreoLinea ->
                    val esPlaceholder = esPlaceholderRefId(hebreoLinea)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (esPlaceholder) {
                            Text(
                                text = "— contenido compartido, pendiente de resolver —",
                                style = MaterialTheme.typography.bodySmall,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                Text(
                                    text = hebreoLinea,
                                    fontSize = 24.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            if (mostrarTranslit && i < translitLineas.size) {
                                Text(
                                    text = translitLineas[i],
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }

                            if (mostrarTraduccion) {
                                if (textoResuelto != null && i < textoResuelto.lineas.size) {
                                    Text(
                                        text = textoResuelto.lineas[i],
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                } else {
                                    Text(
                                        text = "(sin traducción)",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!atomo.explicacion.isNullOrEmpty() || atomo.fuente != null) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                        if (!atomo.explicacion.isNullOrEmpty()) {
                            Text("Explicación", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = atomo.explicacion,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                            )
                        }
                        if (atomo.fuente != null) {
                            Text(
                                text = atomo.fuente,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
