package org.librelab.gtk4kt.examples

import org.librelab.gtk4kt.internal.GTKNative

/**
 * End-to-end test: Kotlin → Rust → GTK signal → Rust upcall → Kotlin callback.
 * No JSON, no DSL — just the raw signal pipeline.
 */
fun main() {
    val gtkThread = Thread {
        GTKNative.gtkInit()
        // Register invoker with a no-op callback. If the signal fires, the
        // trampoline should print "[GTK] callback trampoline invoked" inside Rust.
        GTKNative.gtkRegisterInvoker { handle, _ ->
            System.err.println("[Kotlin] invoker dispatched: handle=$handle")
        }
        System.err.println("[SignalClickTest] calling gtk_bridge_signal_test...")
        GTKNative.gtkSignalTest()
        System.err.println("[SignalClickTest] gtk_bridge_signal_test returned")
        System.err.println("[SignalClickTest] joined")
    }.apply {
        name = "GTK-SignalTest"
        isDaemon = false
        start()
    }
    gtkThread.join()
}