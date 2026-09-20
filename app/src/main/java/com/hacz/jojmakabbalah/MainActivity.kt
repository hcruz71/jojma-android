package com.hacz.jojmakabbalah

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/// Actividad vacía — Fase 0 (bootstrap). Solo confirma que Compose +
/// Firebase (google-services) compilan y arrancan; sin lógica de negocio.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Saludo(
                        nombre = "Jojmá",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Saludo(nombre: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Text(text = "Hola, $nombre")
    }
}

@Preview(showBackground = true)
@Composable
fun SaludoPreview() {
    MaterialTheme {
        Saludo("Jojmá")
    }
}
