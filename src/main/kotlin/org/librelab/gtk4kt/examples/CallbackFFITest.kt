package org.librelab.gtk4kt.examples

import org.librelab.gtk4kt.internal.GTKNative
import java.io.File

/**
 * Direct FFI callback test — bypasses the DSL to test the raw pipeline:
 *   GTK button emit_clicked → Panama upcall → Kotlin println
 *
 * Uses the same JSON file mechanism as the DSL, then triggers a synthetic click.
 */
fun main() {
    System.err.println("[CallbackFFITest] Starting")

    // Create JSON UI with a button that has an onClick handler
    val json = """
    {
        "type": "Window",
        "label": "FFI Test",
        "width": 200,
        "height": 100,
        "children": [
            {
                "type": "Button",
                "label": "Test Button",
                "on_click": 42
            }
        ]
    }
    """.trimIndent()

    val jsonFile = File.createTempFile("gtk4kt_ffi_test_", ".json")
    jsonFile.writeText(json)
    System.err.println("[CallbackFFITest] JSON written to: ${jsonFile.absolutePath}")

    // Set UI path (gtkApplicationRun registers the invoker internally)
    GTKNative.gtkSetUiJsonPath(jsonFile.absolutePath)
    System.err.println("[CallbackFFITest] UI path set, running GTK (blocks)...")

    // gtkApplicationRun runs GTK entirely on one background thread: init + build + gtk_main().
    // It blocks until gtk_main_quit() is called.
    val gtkThread = Thread {
        GTKNative.gtkApplicationRun(0)
        System.err.println("[CallbackFFITest] GTK thread: gtk_main() returned")
    }.apply { start() }

    // Wait for window to be built
    Thread.sleep(1500)

    // Get the button pointer
    val buttonPtr = GTKNative.gtkGetFirstButtonPtr()
    System.err.println("[CallbackFFITest] First button ptr = $buttonPtr")

    if (buttonPtr != 0L) {
        System.err.println("[CallbackFFITest] === EMITTING CLICK ===")
        val result = GTKNative.gtkTestClick(buttonPtr)
        System.err.println("[CallbackFFITest] gtkTestClick result: $result")
    } else {
        System.err.println("[CallbackFFITest] ERROR: no button found")
    }

    Thread.sleep(500)
    GTKNative.gtkMainQuit()
    Thread.sleep(500)
    jsonFile.delete()
    System.err.println("[CallbackFFITest] Done")
}
