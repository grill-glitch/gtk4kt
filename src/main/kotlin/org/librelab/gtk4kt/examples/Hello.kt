package org.librelab.gtk4kt.examples

import org.librelab.gtk4kt.*
import org.librelab.gtk4kt.runtime.*

/**
 * Example demonstrating gtk4kt's declarative UI with state and callbacks.
 *
 * Shows: @Composable, MutableState, remember{}, onClick
 */
fun main() {
    application("org.gtk4kt.example") {
        window("gtk4kt Counter", width = 320, height = 200) {
            CounterApp()
        }
    }
}

/**
 * A composable counter app.
 * State changes trigger recomposition — the label text updates automatically.
 */
@Composable
fun CounterApp() {
    val count: MutableState<Int> = remember { mutableStateOf(0) }

    column(spacing = 16) {
        label("Count: $count")

        button("Click me!") {
            count.update { it + 1 }
        }

        button("Reset") {
            count.update { 0 }
        }
    }
}
