package org.librelab.gtk4kt.examples

import org.librelab.gtk4kt.*
import org.librelab.gtk4kt.runtime.*

/**
 * Example: gtk4kt declarative UI with state + callbacks.
 *
 * Shows: WidgetNode tree → JSON → Rust activate handler → GTK widgets
 *        Kotlin onClick callbacks invoked by Rust via MethodHandle
 */
fun main() {
    application("org.gtk4kt.example") {
        window("gtk4kt Counter", width = 320, height = 240) {
            CounterApp()
        }
    }
}

/**
 * A composable counter app — an extension on WindowBuilder
 * so all DSL functions (column, label, button) are in scope.
 */
fun WindowBuilder.CounterApp() {
    // Note: state tracking via WidgetNode tree is static (no reactive recomposition yet).
    // Each state update requires rebuilding the tree. Phase 4 adds reactive recomposition.
    column(spacing = 16) {
        label("Count: 0")

        button("Click me!") {
            // Callback invoked by Rust when button is clicked
            System.err.println("[CounterApp] button clicked!")
        }

        button("Reset") {
            System.err.println("[CounterApp] reset clicked!")
        }
    }
}
