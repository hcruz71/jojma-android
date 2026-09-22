package com.hacz.jojmakabbalah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hacz.jojmakabbalah.corpus.Kavana
import com.hacz.jojmakabbalah.corpus.Salmo
import com.hacz.jojmakabbalah.corpus.SalmosRepository
import com.hacz.jojmakabbalah.corpus.TraduccionResuelta
import com.hacz.jojmakabbalah.festividades.AppFestividades
import com.hacz.jojmakabbalah.hub.HubScreen
import com.hacz.jojmakabbalah.hub.ModuloHub
import com.hacz.jojmakabbalah.oraciones.AppOraciones
import com.hacz.jojmakabbalah.salmos.SalmosViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                JojmaApp()
            }
        }
    }
}

/// Raíz de navegación real (2026-09-21): Hub con las 6 puntas de la
/// estrella como punto de entrada. Salmos es el único módulo con
/// pantallas reales (AppSalmos, su propio NavHost anidado); Oraciones
/// tiene repositorio/modelo/tests pero TODAVÍA sin pantallas — por
/// ahora es placeholder igual que los otros 3 módulos sin construir.
/// Perfil/Configuración también son placeholder (en iOS, Perfil es un
/// .sheet modal — acá se simplifica a una ruta más del mismo NavHost).
@Composable
fun JojmaApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "hub") {
        composable("hub") {
            HubScreen(
                onModuloClick = { modulo ->
                    val ruta = when (modulo) {
                        ModuloHub.SALMOS -> "salmos"
                        ModuloHub.ORACIONES -> "oraciones"
                        ModuloHub.NOMBRES -> "placeholder/72 Nombres de D-ios"
                        ModuloHub.FESTIVIDADES -> "festividades"
                        ModuloHub.MEDITACIONES -> "placeholder/Meditaciones"
                        ModuloHub.SHABBAT -> "placeholder/Shabbat"
                    }
                    navController.navigate(ruta)
                },
                onCentroClick = { navController.navigate("placeholder/Actual") },
                onPerfilClick = { navController.navigate("placeholder/Perfil") },
                onConfigClick = { navController.navigate("placeholder/Configuración") },
            )
        }
        composable("salmos") { AppSalmos() }
        composable("oraciones") { AppOraciones() }
        composable("festividades") { AppFestividades() }
        composable("placeholder/{titulo}") { entrada ->
            PlaceholderScreen(
                titulo = entrada.arguments?.getString("titulo") ?: "",
                onVolver = { navController.popBackStack() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(titulo: String, onVolver: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titulo) },
                navigationIcon = {
                    Text("←", modifier = Modifier.padding(horizontal = 16.dp).clickable { onVolver() })
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("$titulo — próximamente", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun AppSalmos() {
    val viewModel: SalmosViewModel = viewModel { SalmosViewModel(SalmosRepository()) }
    val estado by viewModel.state.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(Unit) { viewModel.cargar() }

    // El NavHost refleja `estado.pantalla` — al navegar con el
    // NavController disparamos el cambio de estado en el ViewModel,
    // que es la fuente de verdad real (testeada aparte).
    NavHost(navController = navController, startDestination = "lista") {
        composable("lista") {
            ListaSalmosScreen(
                salmos = estado.salmos,
                cargando = estado.cargando,
                error = estado.error,
                onSalmoClick = { numero ->
                    viewModel.abrir(numero)
                    navController.navigate("detalle/$numero")
                },
            )
        }
        composable("detalle/{numero}") {
            val salmo = estado.salmoAbierto
            if (salmo != null) {
                DetalleSalmoScreen(
                    salmo = salmo,
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
fun ListaSalmosScreen(
    salmos: List<Salmo>,
    cargando: Boolean,
    error: String?,
    onSalmoClick: (Int) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Salmos") }) }) { padding ->
        Surface(modifier = Modifier.fillMaxSize().padding(padding)) {
            when {
                cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Text("Descargando 150 salmos…", modifier = Modifier.padding(top = 16.dp))
                    }
                }
                error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: $error", color = MaterialTheme.colorScheme.error)
                }
                else -> LazyColumn {
                    items(salmos, key = { it.numero }) { salmo ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSalmoClick(salmo.numero) }
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                        ) {
                            Text(salmo.titulo, style = MaterialTheme.typography.titleMedium)
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

/// 3 fases del botón Kavaná (flame.fill en iOS), cicladas con cada tap:
/// apagado → angel (resalta nombre del ángel + aplicación) → nombre
/// (resalta el nombre hebreo del ángel + aplicación) → apagado. Global
/// a toda la pantalla — igual que en SalmoDetalleView.swift, NO es un
/// control por versículo (el efecto visual solo aparece en los
/// versículos que traen `kavanot`, pero el control que lo enciende es
/// uno solo para toda la pantalla).
private enum class FaseKavana { APAGADO, ANGEL, NOMBRE }

private fun FaseKavana.siguiente(): FaseKavana = when (this) {
    FaseKavana.APAGADO -> FaseKavana.ANGEL
    FaseKavana.ANGEL -> FaseKavana.NOMBRE
    FaseKavana.NOMBRE -> FaseKavana.APAGADO
}

/// Detalle de un Salmo — intercala hebreo[i] con traducción/transliteración[i]
/// versículo por versículo, en vez de dos bloques separados. Mismo patrón
/// visual que SalmoDetalleView.swift (iOS): una fila = un VStack agrupando
/// numeral + hebreo + (kavaná) + translit + traducción, con separación mayor
/// entre filas que entre las líneas internas de una fila.
///
/// Toggles de transliteración/traducción/kavaná son GLOBALES a la pantalla
/// (mismo criterio que iOS: @AppStorage allá, remember{} local acá — sin
/// persistencia entre sesiones todavía, eso no forma parte de este cambio).
/// Sin audio/TTS ni comentarios — fuera de alcance de esta corrección.
///
/// TODO(karaoke): Karaoke de lectura (TTS + resaltado por palabra) —
/// arquitectura portable confirmada 1:1 desde iOS
/// (UtteranceProgressListener.onRangeStart equivale a
/// willSpeakRangeOfSpeechString de LectorSalmoViewModel.swift), pero
/// bloqueado por disponibilidad de voz hebrea: el dispositivo de
/// prueba (Samsung A54) usa motor TTS Samsung sin hebreo instalado;
/// Google TTS está presente pero no activo como motor default.
/// Requiere que el usuario cambie el motor TTS del sistema y/o
/// descargue paquete de voz hebrea de Google — fuente de
/// fragmentación mayor que iOS, donde Apple controla el motor único.
/// Verificado empíricamente 2026-09-22 (TextToSpeech.isLanguageAvailable
/// devolvió LANG_NOT_SUPPORTED, 0 voces "he" encontradas, defaultEngine
/// = com.samsung.SMT). Pendiente para fase posterior del proyecto, NO
/// bloqueante para el MVP de Salmos+Oraciones.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleSalmoScreen(
    salmo: Salmo,
    idioma: String,
    textoResuelto: TraduccionResuelta?,
    onAlternarIdioma: () -> Unit,
    onVolver: () -> Unit,
) {
    val tieneIngles = salmo.traduccion("en") != null
    val tieneKavanot = salmo.kavanot.isNotEmpty()

    var mostrarTranslit by remember(salmo.id) { mutableStateOf(true) }
    var mostrarTraduccion by remember(salmo.id) { mutableStateOf(false) }
    var faseKavana by remember(salmo.id) { mutableStateOf(FaseKavana.APAGADO) }

    val translitLineas = salmo.traducciones[idioma]?.transliteracion ?: emptyList()
    val fuente = salmo.traducciones[idioma]?.fuente ?: salmo.traducciones["es"]?.fuente

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(salmo.titulo) },
                navigationIcon = {
                    Text(
                        "←",
                        modifier = Modifier.padding(horizontal = 16.dp).clickable { onVolver() },
                    )
                },
                actions = {
                    if (tieneIngles) {
                        TextButton(onClick = onAlternarIdioma) {
                            Text(if (idioma == "es") "EN" else "ES")
                        }
                    }
                    TextButton(onClick = { mostrarTranslit = !mostrarTranslit }) {
                        Text("Aa", color = if (mostrarTranslit) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { mostrarTraduccion = !mostrarTraduccion }) {
                        Text("🌐", color = if (mostrarTraduccion) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (tieneKavanot) {
                        TextButton(onClick = { faseKavana = faseKavana.siguiente() }) {
                            Text("🔥", color = if (faseKavana == FaseKavana.APAGADO) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary)
                        }
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            contentPadding = PaddingValues(vertical = 20.dp),
        ) {
            itemsIndexed(salmo.hebreo, key = { i, _ -> i }) { i, hebreoLinea ->
                val kavanaDeEsteVerso = if (faseKavana != FaseKavana.APAGADO) {
                    salmo.kavanot.find { it.versiculo == i + 1 }
                } else null

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = "${i + 1}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    val agrandado = kavanaDeEsteVerso != null
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Text(
                            text = hebreoLinea,
                            fontSize = if (agrandado) 30.sp else 24.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (kavanaDeEsteVerso != null) {
                        EtiquetaKavana(kavanaDeEsteVerso, faseKavana)
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

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.padding(bottom = 12.dp))
                    Text("Explicación", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = salmo.explicacion ?: "Explicación próximamente",
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    )
                    if (fuente != null) {
                        Text(
                            text = fuente,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/// Etiqueta bajo el versículo con kavaná — replica etiquetaKavana() de
/// SalmoDetalleView.swift: fase .angel muestra nombre + aplicación en
/// latín; fase .nombre muestra el nombre hebreo del ángel (aislado con
/// U+2068/U+2069 para que el bidi no rompa la dirección del bloque
/// latino que lo rodea) + aplicación.
@Composable
private fun EtiquetaKavana(k: Kavana, fase: FaseKavana) {
    val texto = when (fase) {
        FaseKavana.ANGEL -> "${k.nombre}  ·  ${k.aplicacion}"
        FaseKavana.NOMBRE -> "⁨${k.nombreDios}⁩  ·  ${k.aplicacion}"
        FaseKavana.APAGADO -> ""
    }
    Text(
        text = texto,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
}
