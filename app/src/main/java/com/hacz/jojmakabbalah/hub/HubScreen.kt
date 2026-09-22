package com.hacz.jojmakabbalah.hub

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hacz.jojmakabbalah.R
import kotlin.math.min
import kotlin.math.roundToInt

/// Identificadores de las 6 puntas — mismo set que ModuloHub en
/// HubView.swift (iOS), sin `nombres`/`oraciones`/etc. en español
/// porque en Kotlin usamos convención de enums en mayúsculas.
enum class ModuloHub { NOMBRES, ORACIONES, SALMOS, FESTIVIDADES, MEDITACIONES, SHABBAT }

/// Geometría EXACTA del hexagrama del Hub — coordenadas copiadas
/// literalmente de HubGeom en HubView.swift (iOS), que a su vez las
/// copió de referencias/estrella_hub.html (viewBox 440×500, centro
/// 220,250). Verificado con las 6 puntas: son un hexagrama regular
/// (radio de punta 140, radio de base 80.83) con la primera punta
/// (Nombres) apuntando hacia arriba (-90°) y las siguientes cada 60°
/// en sentido horario — confirmado calculando los ángulos reales de
/// cada tip, no asumido.
object HubGeom {
    const val VIEWBOX_W = 440f
    const val VIEWBOX_H = 500f
    val center = Offset(220f, 250f)

    data class Punta(
        val modulo: ModuloHub,
        val tip: Offset,
        val base1: Offset,
        val base2: Offset,
        val label: String,
        val labelPos: Offset,
    )

    val puntas = listOf(
        Punta(ModuloHub.NOMBRES,
            tip = Offset(220f, 110f), base1 = Offset(179.59f, 180f), base2 = Offset(260.41f, 180f),
            label = "72 Nombres de D-ios", labelPos = Offset(220f, 86f)),
        Punta(ModuloHub.ORACIONES,
            tip = Offset(341.24f, 180f), base1 = Offset(260.41f, 180f), base2 = Offset(300.83f, 250f),
            label = "Oraciones", labelPos = Offset(372f, 158f)),
        Punta(ModuloHub.SALMOS,
            tip = Offset(341.24f, 320f), base1 = Offset(300.83f, 250f), base2 = Offset(260.41f, 320f),
            label = "Salmos", labelPos = Offset(372f, 348f)),
        Punta(ModuloHub.FESTIVIDADES,
            tip = Offset(220f, 390f), base1 = Offset(260.41f, 320f), base2 = Offset(179.59f, 320f),
            label = "Festividades", labelPos = Offset(220f, 418f)),
        Punta(ModuloHub.MEDITACIONES,
            tip = Offset(98.76f, 320f), base1 = Offset(179.59f, 320f), base2 = Offset(139.17f, 250f),
            label = "Meditaciones", labelPos = Offset(69f, 348f)),
        Punta(ModuloHub.SHABBAT,
            tip = Offset(98.76f, 180f), base1 = Offset(139.17f, 250f), base2 = Offset(179.59f, 180f),
            label = "Shabbat", labelPos = Offset(69f, 158f)),
    )

    val hexagono = listOf(
        Offset(300.83f, 250f), Offset(260.41f, 320f), Offset(179.59f, 320f),
        Offset(139.17f, 250f), Offset(179.59f, 180f), Offset(260.41f, 180f),
    )
}

/// Valores HEX reales extraídos de HubColor/HubPalette.noche en
/// HubView.swift (iOS) — no aproximados. Paleta día/noche dinámica y
/// auras por festividad quedan fuera de este pase (dependen de
/// NombreActualService/sunset, no portado todavía); acá solo se
/// replica la paleta .noche, que es la que se ve la mayor parte del
/// tiempo.
object HubColors {
    val indigo1 = Color(0xFF16204A)
    val indigo2 = Color(0xFF0A0F28)
    val indigo3 = Color(0xFF03050E)

    val oroGrad = listOf(
        Color(0xFFFFF1B0), Color(0xFFECC25E), Color(0xFFB78321),
        Color(0xFF8A5F15), Color(0xFFC79330), Color(0xFFF4D277), Color(0xFFA9781F),
    )
    val puntaStroke = Color(0xFFFFE9A8).copy(alpha = 0.55f)

    val vidrio = listOf(Color(0xFF26305F), Color(0xFF121A3E), Color(0xFF070B1F))
    val rim = listOf(Color(0xFFFFF0AC), Color(0xFFC79330), Color(0xFFF4D277))

    val textoTenue = Color(0xFFCDB778)
    val textoActivo = Color(0xFFFFE9A8)
    val marcaTitulo = Color(0xFFCAA94F)
    val acentoVigente = Color(0xFFFFDF8E)
}

/// FrankRuhlLibre-VariableFont_wght.ttf real, copiado tal cual desde
/// Jojma-ios/.../Fonts/. Es una fuente variable (un solo archivo con
/// eje de peso) — se carga como peso Bold, que es el único usado en
/// el Hub (marca, hebreo del centro).
val FrankRuhlFamily = FontFamily(Font(R.font.frank_ruhl_libre, FontWeight.Bold))

/// Hub principal — estrella de 6 puntas dibujada con Canvas+Path
/// replicando HubGeom.puntas/hexagono de iOS. SIN la animación de
/// "encendido" al tocar (diferida a propósito, ver instrucción del
/// 2026-09-21: pulido no bloqueante) y SIN badge de festividad activa
/// (requiere portar CalendarioHebreoService — calendario hebreo +
/// roll de atardecer por GPS — no existe todavía en Android, ver
/// TODO en HubScreen()).
@Composable
fun HubScreen(
    onModuloClick: (ModuloHub) -> Unit,
    onCentroClick: () -> Unit,
    onPerfilClick: () -> Unit,
    onConfigClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(HubColors.indigo1, HubColors.indigo2, HubColors.indigo3),
                )
            ),
    ) {
        Text(
            "👤",
            fontSize = 20.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .clickableSimple(onPerfilClick),
        )
        Text(
            "⚙",
            fontSize = 22.sp,
            color = HubColors.textoTenue,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clickableSimple(onConfigClick),
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(top = 64.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Jojmá App",
                fontFamily = FrankRuhlFamily,
                fontSize = 30.sp,
                letterSpacing = 3.sp,
                color = HubColors.marcaTitulo,
            )
            // TODO(hub-festividad): banda "Hoy · {festividad}" / "{festividad}
            // · en N días" — requiere portar CalendarioHebreoService.swift
            // (calendario hebreo + roll de atardecer por GPS). Diferido a
            // propósito, ver reporte del 2026-09-21.
            Estrella(
                onModuloClick = onModuloClick,
                onCentroClick = onCentroClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 8.dp, vertical = 16.dp),
            )
        }
    }
}

/// clickable() simple sin ripple/indication — evita traer
/// androidx.compose.material.ripple solo para dos íconos de esquina.
private fun Modifier.clickableSimple(onClick: () -> Unit): Modifier =
    this.then(
        Modifier.pointerInput(Unit) {
            detectTapGestures(onTap = { onClick() })
        }
    )

@Composable
private fun Estrella(
    onModuloClick: (ModuloHub) -> Unit,
    onCentroClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val canvasWpx = with(density) { maxWidth.toPx() }
        val canvasHpx = with(density) { maxHeight.toPx() }
        val scale = min(canvasWpx / HubGeom.VIEWBOX_W, canvasHpx / HubGeom.VIEWBOX_H)
        val offsetX = (canvasWpx - HubGeom.VIEWBOX_W * scale) / 2f
        val offsetY = (canvasHpx - HubGeom.VIEWBOX_H * scale) / 2f

        fun aCanvas(p: Offset) = Offset(p.x * scale + offsetX, p.y * scale + offsetY)

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val v = Offset((tap.x - offsetX) / scale, (tap.y - offsetY) / scale)
                        val punta = HubGeom.puntas.firstOrNull {
                            puntoEnTriangulo(v, it.tip, it.base1, it.base2)
                        }
                        if (punta != null) {
                            onModuloClick(punta.modulo)
                        } else if (puntoEnPoligono(v, HubGeom.hexagono)) {
                            onCentroClick()
                        }
                    }
                },
        ) {
            val hexPath = Path().apply {
                HubGeom.hexagono.forEachIndexed { i, pt ->
                    val c = aCanvas(pt)
                    if (i == 0) moveTo(c.x, c.y) else lineTo(c.x, c.y)
                }
                close()
            }
            drawPath(hexPath, brush = Brush.radialGradient(HubColors.vidrio))
            drawPath(hexPath, brush = Brush.linearGradient(HubColors.rim), style = Stroke(width = 2.4f * scale))

            HubGeom.puntas.forEach { punta ->
                val t = aCanvas(punta.tip)
                val b1 = aCanvas(punta.base1)
                val b2 = aCanvas(punta.base2)
                val path = Path().apply {
                    moveTo(t.x, t.y); lineTo(b1.x, b1.y); lineTo(b2.x, b2.y); close()
                }
                drawPath(path, brush = Brush.linearGradient(HubColors.oroGrad))
                drawPath(path, color = HubColors.puntaStroke, style = Stroke(width = 1f * scale))
            }
        }

        // Ancho generoso (no 110dp) + maxLines=1: replica el comportamiento
        // real de iOS, donde Text(...).position(p.labelPos) NO tiene ningún
        // ancho fijo y se renderiza en una sola línea a su ancho natural,
        // centrada en el punto. El label más largo ("72 Nombres de D-ios")
        // es el que reveló que 110dp forzaba wrap a 2 líneas e invadía la
        // punta de la estrella — bug real 2026-09-21, corregido acá sin
        // acortar el texto (iOS tampoco lo acorta en el Hub).
        val labelW = 200.dp
        HubGeom.puntas.forEach { punta ->
            val pos = aCanvas(punta.labelPos)
            Text(
                text = punta.label,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = HubColors.textoTenue,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .width(labelW)
                    .offset {
                        val halfW = with(density) { labelW.toPx() / 2f }
                        IntOffset((pos.x - halfW).roundToInt(), (pos.y - with(density) { 8.dp.toPx() }).roundToInt())
                    },
            )
        }

        // Centro: placeholder de "Actual" — el dato real (Nombre vigente,
        // guematría, cronómetro) depende de NombreActualService (GPS +
        // horario solar), no portado todavía. El tap SÍ navega.
        val centroPos = aCanvas(HubGeom.center)
        Text(
            text = "Actual",
            fontFamily = FrankRuhlFamily,
            fontSize = 18.sp,
            color = HubColors.acentoVigente,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .width(100.dp)
                .offset {
                    val halfW = with(density) { 50.dp.toPx() }
                    IntOffset((centroPos.x - halfW).roundToInt(), (centroPos.y - with(density) { 10.dp.toPx() }).roundToInt())
                },
        )
    }
}

private fun signo(p1: Offset, p2: Offset, p3: Offset): Float =
    (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)

private fun puntoEnTriangulo(pt: Offset, a: Offset, b: Offset, c: Offset): Boolean {
    val d1 = signo(pt, a, b)
    val d2 = signo(pt, b, c)
    val d3 = signo(pt, c, a)
    val neg = (d1 < 0) || (d2 < 0) || (d3 < 0)
    val pos = (d1 > 0) || (d2 > 0) || (d3 > 0)
    return !(neg && pos)
}

private fun puntoEnPoligono(pt: Offset, vertices: List<Offset>): Boolean {
    var dentro = false
    var j = vertices.size - 1
    for (i in vertices.indices) {
        val vi = vertices[i]
        val vj = vertices[j]
        if ((vi.y > pt.y) != (vj.y > pt.y)) {
            val xInterseccion = (vj.x - vi.x) * (pt.y - vi.y) / (vj.y - vi.y) + vi.x
            if (pt.x < xInterseccion) dentro = !dentro
        }
        j = i
    }
    return dentro
}
