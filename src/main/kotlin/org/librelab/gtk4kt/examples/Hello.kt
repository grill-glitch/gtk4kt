package org.librelab.gtk4kt.examples

import org.librelab.gtk4kt.*

/**
 * Minimal "Hello World" example demonstrating gtk4kt API.
 */
fun main() {
    application("org.gtk4kt.example") {
        window("gtk4kt — Hello", width = 400, height = 200) {
            val box = column(spacing = 16) {
                label("Hello from gtk4kt!")
                label("Kotlin → Rust → GTK4")
            }
            child(box)
        }
    }
}
