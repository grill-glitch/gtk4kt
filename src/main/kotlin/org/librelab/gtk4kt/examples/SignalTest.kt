package org.librelab.gtk4kt.examples

import com.sun.jna.*
import java.io.File

/**
 * Signal pipeline test — verifies Rust → Kotlin callback.
 *
 * Uses JNA to create a real upcall: Kotlin passes a native function pointer to Rust,
 * Rust stores it in INVOKER_ADDR, GTK signal fires → Rust calls the pointer → Kotlin runs.
 */
fun main() {
    println("[SignalTest] Loading native lib...")

    val lib = NativeLibrary.getInstance("gtk4kt_native")

    // 1. gtk_bridge_init
    val initFn = lib.getFunction("gtk_bridge_init")
    val initR = initFn.invokeInt(arrayOf<Any>())
    println("[SignalTest] gtkInit result: $initR")
    if (initR != 0) {
        println("[SignalTest] ❌ gtkInit failed")
        return
    }

    // 2. Create a JNA upcall and get its native function pointer
    val callbackInvoked = java.util.concurrent.atomic.AtomicBoolean(false)

    // Create a concrete callback class (implements the C function pointer interface)
    val callback = object : GTKInvokerCallback {
        override fun invoke(handle: Long, ptr: Pointer?) {
            println("[SignalTest] ✅ Kotlin callback invoked! handle=$handle")
            callbackInvoked.set(true)
        }
    }

    // Get a native function pointer for this callback
    val fp = CallbackReference.getFunctionPointer(callback)

    // Get the raw address via reflection
    val peerField = java.lang.reflect.Field::class.java.getDeclaredField("peer")
    peerField.isAccessible = true
    val fpAddr = peerField.getLong(fp)
    println("[SignalTest] Upcall function pointer: 0x${java.lang.Long.toHexString(fpAddr)}")

    // 3. gtk_bridge_register_invoker(fpAddr)
    val regFn = lib.getFunction("gtk_bridge_register_invoker")
    regFn.invokeVoid(arrayOf(fpAddr))
    println("[SignalTest] Invoker registered")

    // 4. gtk_bridge_signal_test(0)
    // Rust creates Button, connects signal → Kotlin callback → emits → writes result
    val signalFn = lib.getFunction("gtk_bridge_signal_test")
    val signalR = signalFn.invokeInt(arrayOf<Any>(0))
    println("[SignalTest] gtkSignalTest result: $signalR")

    Thread.sleep(200)

    // 5. Verify
    val content = try {
        File("/tmp/signal_test_result.txt").readText().trim()
    } catch (e: Exception) {
        "FILE_NOT_FOUND"
    }
    println("[SignalTest] Marker: '$content'")
    println("[SignalTest] Callback invoked flag: ${callbackInvoked.get()}")

    if (callbackInvoked.get() || content == "CALLED") {
        println("[SignalTest] ✅ CALLBACK PIPELINE VERIFIED!")
    } else {
        println("[SignalTest] ❌ FAIL — callback not invoked. Result: $content")
    }
}

/** JNA callback matching Rust's invoker signature: void invoke(u64 handle, void* ptr). */
interface GTKInvokerCallback : Callback {
    fun invoke(handle: Long, ptr: Pointer?)
}
