package com.hacz.jojmakabbalah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hacz.jojmakabbalah.corpus.Salmo
import com.hacz.jojmakabbalah.corpus.SalmosRepository

/// Primera pieza (2026-09-20): descarga UN salmo real desde Firebase
/// Storage, lo parsea, y lo muestra — confirma que la conexión a la
/// nube + el esquema traducciones.*/es/en funcionan de punta a punta.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PantallaSalmo(
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
        }
    }
}

private sealed interface EstadoSalmo {
    data object Cargando : EstadoSalmo
    data class Listo(val salmo: Salmo) : EstadoSalmo
    data class Error(val mensaje: String) : EstadoSalmo
}

@Composable
fun PantallaSalmo(modifier: Modifier = Modifier) {
    var estado by remember { mutableStateOf<EstadoSalmo>(EstadoSalmo.Cargando) }
    val repo = remember { SalmosRepository() }

    LaunchedEffect(Unit) {
        estado = try {
            EstadoSalmo.Listo(repo.descargarSalmo(1))
        } catch (e: Exception) {
            EstadoSalmo.Error(e.message ?: e.toString())
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        when (val s = estado) {
            is EstadoSalmo.Cargando -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text("Descargando desde Firebase Storage…", modifier = Modifier.padding(top = 16.dp))
            }

            is EstadoSalmo.Error -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Error: ${s.mensaje}", color = MaterialTheme.colorScheme.error)
            }

            is EstadoSalmo.Listo -> ContenidoSalmo(s.salmo)
        }
    }
}

@Composable
private fun ContenidoSalmo(salmo: Salmo) {
    var idioma by remember { mutableStateOf("es") }
    val trad = salmo.traduccion(idioma)
    val tieneIngles = salmo.traduccion("en") != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            text = salmo.titulo,
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "Idioma: $idioma",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        if (tieneIngles) {
            Button(
                onClick = { idioma = if (idioma == "es") "en" else "es" },
                modifier = Modifier.padding(bottom = 20.dp),
            ) {
                Text(if (idioma == "es") "Switch to English" else "Cambiar a español")
            }
        }

        Text(
            text = "HEBREO",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        salmo.hebreo.forEach { linea ->
            Text(
                text = linea,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxSize().padding(bottom = 8.dp),
            )
        }

        Text(
            text = idioma.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
        )
        trad?.texto?.forEach { linea ->
            Text(
                text = linea,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp),
            )
        } ?: Text("(sin traducción para \"$idioma\")")
    }
}
