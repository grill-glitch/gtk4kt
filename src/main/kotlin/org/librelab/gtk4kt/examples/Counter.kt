package org.librelab.gtk4kt.examples

import org.librelab.gtk4kt.*
import org.librelab.gtk4kt.runtime.Modifier

/**
 * Counter example rewritten in Compose-like gtk4kt syntax.
 *
 * This mirrors how ika's Compose screens (LibraryScreen, SettingsScreen)
 * would be ported: Column/Row/Text/Button/Card/Switch/Slider + Modifier.
 */
fun main() {
    application("org.gtk4kt.counter") {
        window("gtk4kt Counter", width = 380, height = 460) {
            CounterScreen()
        }
    }
}

private fun WindowBuilder.CounterScreen() {
    var count = 0
    var auto = true

    column(spacing = 12, modifier = Modifier.padding(16).fillMaxWidth()) {
        Text("Counter", modifier = Modifier.fillMaxWidth().alignCenterHorizontally())
        Divider()

        Card(modifier = Modifier.fillMaxWidth()) {
            Text("Count: $count", modifier = Modifier.padding(16).fillMaxWidth().alignCenterHorizontally())
        }

        Spacer(Modifier.height(8))

        row(spacing = 8) {
            Button("+1", modifier = Modifier.weight(1f)) {
                count += 1
                System.err.println("[CounterScreen] count → $count")
            }
            Button("-1", modifier = Modifier.weight(1f)) {
                count -= 1
                System.err.println("[CounterScreen] count → $count")
            }
        }

        Spacer(Modifier.height(8))

        row(spacing = 8) {
            Text("Auto-fire", modifier = Modifier.weight(1f))
            Switch(checked = auto) { on ->
                auto = on
                System.err.println("[CounterScreen] auto → $on")
            }
        }

        OutlinedButton("Reset", modifier = Modifier.fillMaxWidth()) {
            count = 0
            System.err.println("[CounterScreen] reset")
        }
    }
}