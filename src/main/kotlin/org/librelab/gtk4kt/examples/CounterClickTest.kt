package org.librelab.gtk4kt.examples

import org.librelab.gtk4kt.*

/**
 * Phase 3-4 counter example: DSL + Kotlin state + Rust GTK + callback.
 *
 * Builds a Window with a button via the Kotlin DSL → JSON → Rust GTK.
 * Then fires ALL UI-built buttons synchronously via gtk_bridge_signal_test.
 * The Kotlin callback for each button runs through the JNA trampoline.
 */
fun main() {
    // Use the standard application{} flow which spawns the GTK thread.
    application("org.gtk4kt.counter.click") {
        window("gtk4kt Counter", width = 320, height = 200) {
            CounterClickUi()
        }
    }
    // application{} blocks until quit. The GTK thread inside it will fire the
    // signal test synchronously after building the UI (via buildJson callback).
    System.err.println("[CounterClickTest] main done")
}

private fun WindowBuilder.CounterClickUi() {
    var count = 0

    column(spacing = 16) {
        label("Count: $count")

        button("Click me (callback should fire)") {
            count += 1
            System.err.println("[CounterClickUi] click → count=$count")
        }
    }
}