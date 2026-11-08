package app.pechedebutant.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import app.pechedebutant.data.RigDiagram

/**
 * Simple rig schematic, drawn in Compose with lines, circles and boxes.
 * No photos, no external images. Top is the rod / main line, bottom is the lead.
 * Read top to bottom, like the rig hangs in the water.
 */
@Composable
fun RigSchematic(diagram: RigDiagram, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    val accent = MaterialTheme.colorScheme.tertiary
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val top = 12f
        val bottom = h - 12f

        // Rod tip marker.
        drawCircle(color = accent, radius = 10f, center = Offset(cx, top + 6f))
        drawLine(
            color = lineColor,
            start = Offset(cx, top + 16f),
            end = Offset(cx, bottom),
            strokeWidth = 4f
        )

        fun hookAt(x: Float, y: Float) {
            // Hook: a small open circle with a short shank.
            drawLine(
                color = lineColor,
                start = Offset(x, y - 18f),
                end = Offset(x, y + 6f),
                strokeWidth = 4f
            )
            drawCircle(
                color = lineColor,
                radius = 12f,
                center = Offset(x, y + 10f),
                style = Stroke(width = 4f)
            )
        }

        fun leadAt(y: Float) {
            // Lead: a filled box.
            drawCircle(color = accent, radius = 16f, center = Offset(cx, y))
        }

        fun beadAt(y: Float) {
            drawCircle(color = accent, radius = 7f, center = Offset(cx, y))
        }

        when (diagram) {
            RigDiagram.SLIDING -> {
                // Sliding lead near the top, bead, swivel, long leader, one hook.
                leadAt(top + 44f)
                beadAt(top + 72f)
                drawCircle(color = lineColor, radius = 7f, center = Offset(cx, top + 96f))
                hookAt(cx, bottom - 24f)
            }
            RigDiagram.PATERNOSTER_2 -> {
                // Two snoods left and right, lead at the bottom.
                drawLine(
                    color = lineColor,
                    start = Offset(cx, top + 56f),
                    end = Offset(cx - 70f, top + 56f),
                    strokeWidth = 4f
                )
                hookAt(cx - 70f, top + 92f)
                drawLine(
                    color = lineColor,
                    start = Offset(cx, top + 126f),
                    end = Offset(cx + 70f, top + 126f),
                    strokeWidth = 4f
                )
                hookAt(cx + 70f, top + 162f)
                leadAt(bottom - 10f)
            }
            RigDiagram.PATERNOSTER_3 -> {
                // Three snoods, small lead at the bottom.
                drawLine(
                    color = lineColor,
                    start = Offset(cx, top + 44f),
                    end = Offset(cx - 70f, top + 44f),
                    strokeWidth = 4f
                )
                hookAt(cx - 70f, top + 76f)
                drawLine(
                    color = lineColor,
                    start = Offset(cx, top + 100f),
                    end = Offset(cx + 70f, top + 100f),
                    strokeWidth = 4f
                )
                hookAt(cx + 70f, top + 132f)
                drawLine(
                    color = lineColor,
                    start = Offset(cx, top + 156f),
                    end = Offset(cx - 70f, top + 156f),
                    strokeWidth = 4f
                )
                hookAt(cx - 70f, top + 188f)
                leadAt(bottom - 6f)
            }
            RigDiagram.BASS -> {
                // Sliding lead, bead, swivel, very long leader, one strong hook.
                leadAt(top + 40f)
                beadAt(top + 64f)
                drawCircle(color = lineColor, radius = 7f, center = Offset(cx, top + 88f))
                hookAt(cx, bottom - 24f)
            }
            RigDiagram.DORADE -> {
                // Sliding lead, bead, swivel, leader, strong hook with bait ball.
                leadAt(top + 44f)
                beadAt(top + 72f)
                drawCircle(color = lineColor, radius = 7f, center = Offset(cx, top + 96f))
                hookAt(cx, bottom - 36f)
                // Bait (crab / mussel) shown as a filled circle on the hook.
                drawCircle(color = accent, radius = 14f, center = Offset(cx, bottom - 30f))
            }
        }
    }
}

/** Short legend for the schematic, shown under the drawing. */
@Composable
fun RigLegend() {
    androidx.compose.material3.Text(
        text = "Top: rod and main line. Filled circle: lead. Small circle: bead or swivel. Open hook: hook. Read top to bottom.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
