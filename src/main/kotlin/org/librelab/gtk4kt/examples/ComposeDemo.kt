package org.librelab.gtk4kt.examples

import org.librelab.gtk4kt.*
import org.librelab.gtk4kt.runtime.Modifier

/**
 * Phase 4a/4b verification: Compose-like DSL surface.
 *
 * Exercises: Text, Button, OutlinedButton, Spacer, Row, Column,
 * Modifier (padding/fillMaxWidth/height/width), label backward-compat.
 */
fun main() {
    application("org.gtk4kt.compose") {
        window("gtk4kt Compose-like Demo", width = 420, height = 420) {
            ComposeDemo()
        }
    }
}

private fun WindowBuilder.ComposeDemo() {
    column(spacing = 12, modifier = Modifier.padding(16)) {
        // Compose-style Text
        Text("Phase 4 Compose-like DSL", modifier = Modifier.fillMaxWidth())

        Text("This is a plain Text with padding.", modifier = Modifier.padding(4))

        // Spacer with height
        Spacer(Modifier.height(8))

        // Row with a Spacer width
        row(spacing = 8) {
            Button("Left", modifier = Modifier.weight(1f)) {
                System.err.println("[ComposeDemo] Left clicked")
            }
            Spacer(Modifier.width(8))
            Button("Right", modifier = Modifier.weight(1f)) {
                System.err.println("[ComposeDemo] Right clicked")
            }
        }

        Spacer(Modifier.height(12))

        // OutlinedButton
        OutlinedButton("Outlined Action", modifier = Modifier.fillMaxWidth()) {
            System.err.println("[ComposeDemo] Outlined clicked")
        }

        Spacer(Modifier.height(12))

        // Aligned text
        Text("Right-aligned", modifier = Modifier.fillMaxWidth().alignEnd())
        Text("Center-aligned", modifier = Modifier.fillMaxWidth().alignCenterHorizontally())

        Spacer(Modifier.height(12))

        // Phase 4c: Card
        Card(modifier = Modifier.fillMaxWidth()) {
            Text("Card content", modifier = Modifier.padding(8))
            Divider()
            Text("More card content", modifier = Modifier.padding(8))
        }

        Spacer(Modifier.height(12))

        // Phase 4c: Surface with a row
        Surface(modifier = Modifier.fillMaxWidth()) {
            row(spacing = 8) {
                Text("Surface label", modifier = Modifier.weight(1f))
                Switch(checked = true) { on ->
                    System.err.println("[ComposeDemo] switch → $on")
                }
            }
        }

        Spacer(Modifier.height(12))

        // Phase 4c: Slider (use explicit onValueChange param to avoid trailing-lambda confusion)
        Slider(
            value = 30f,
            min = 0f,
            max = 100f,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = { v -> System.err.println("[ComposeDemo] slider → $v") },
        )
    }
}