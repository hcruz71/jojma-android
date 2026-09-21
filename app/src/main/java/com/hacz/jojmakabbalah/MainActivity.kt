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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hacz.jojmakabbalah.corpus.Salmo
import com.hacz.jojmakabbalah.corpus.SalmosRepository
import com.hacz.jojmakabbalah.salmos.SalmosViewModel

/// Segunda pieza (2026-09-21): lista navegable de los 150 Salmos +
/// detalle con selector de idioma real. La lógica de navegación/estado
/// vive en SalmosViewModel (testable en JVM); el NavHost de acá solo
/// la refleja hacia pantallas reales.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                AppSalmos()
            }
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
                    textoMostrado = estado.textoMostrado,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetalleSalmoScreen(
    salmo: Salmo,
    idioma: String,
    textoMostrado: List<String>?,
    onAlternarIdioma: () -> Unit,
    onVolver: () -> Unit,
) {
    val tieneIngles = salmo.traduccion("en") != null

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
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Text(
                text = "Idioma: $idioma",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            if (tieneIngles) {
                Button(onClick = onAlternarIdioma, modifier = Modifier.padding(bottom = 20.dp)) {
                    Text(if (idioma == "es") "Switch to English" else "Cambiar a español")
                }
            }

            Text("HEBREO", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(bottom = 4.dp))
            salmo.hebreo.forEach { linea ->
                Text(
                    text = linea,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                )
            }

            Text(
                text = idioma.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )
            textoMostrado?.forEach { linea ->
                Text(text = linea, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
            } ?: Text("(sin traducción para \"$idioma\")")
        }
    }
}
