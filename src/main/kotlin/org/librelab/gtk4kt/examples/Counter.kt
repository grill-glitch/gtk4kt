package org.librelab.gtk4kt.examples

import org.librelab.gtk4kt.*

/**
 * End-to-end test: Composable DSL + Kotlin state + Rust GTK + callbacks.
 *
 * Click the button → Kotlin callback fires → state updates → label reflects new state.
 * Validates that the entire pipeline (Kotlin DSL → JSON → Rust GTK → click signal →
 * JNA trampoline → Kotlin dispatch → state update) works.
 */
fun main() {
    application("org.gtk4kt.counter") {
        window("gtk4kt Counter", width = 320, height = 200) {
            CounterUi()
        }
    }
}

private fun WindowBuilder.CounterUi() {
    // Mutable state — Kotlin-side, not reactive yet.
    var count = 0

    column(spacing = 16) {
        label("Count: $count")

        button("Increment") {
            count += 1
            System.err.println("[CounterUi] click → count=$count")
            // Phase 4: also update the label via gtk_bridge_widget_set_label.
            // For now, just log it — proves the callback fires.
        }

        button("Reset") {
            count = 0
            System.err.println("[CounterUi] reset → count=$count")
        }
    }
}