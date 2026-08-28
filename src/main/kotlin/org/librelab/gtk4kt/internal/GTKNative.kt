package org.librelab.gtk4kt.internal

import com.sun.jna.*
import java.io.File

/**
 * Kotlin FFI bridge to libgtk4kt_native.so using JNA.
 * All GTK operations MUST happen on a single dedicated thread.
 */
object GTKNative {
    private val lib: NativeLibrary by lazy {
        val path = findLibraryPath()
        System.err.println("[GTKNative] Loading: $path")
        NativeLibrary.getInstance(path)
    }

    private fun f(name: String) = lib.getFunction(name)

    // ─── GTK initialization & main loop ─────────────────────────────────────

    /**
     * Initialize GTK and run the GTK main loop on a dedicated thread.
     * GTK must ONLY be accessed from this thread.
     * @param wait if true, block until gtkMainQuit; if false, return immediately
     */
    fun gtkApplicationRun(appPtr: Long, latchAddr: Long) {
        f("gtk_bridge_application_run").invokeInt(arrayOf<Any>(appPtr, latchAddr))
    }

    // ─── Kotlin → Rust downcalls ─────────────────────────────────────────────

    fun gtkInit(): Int = f("gtk_bridge_init").invokeInt(arrayOf<Any>())

    fun gtkSignalTest(): Int = f("gtk_bridge_signal_test").invokeInt(arrayOf<Any>(0))

    fun gtkSetUiJsonPath(path: String) {
        f("gtk_bridge_set_ui_json_path").invokeVoid(arrayOf(path))
    }

    /**
     * Phase 6-5: rebuild UI from the currently-set JSON path.
     */
    fun gtkRebuildUi() {
        f("gtk_bridge_rebuild_ui").invokeInt(arrayOf<Any>())
    }

    /**
     * Phase 8-3: load a custom CSS theme at startup. Called once before any
     * widgets are built; the CSS is applied via GtkCssProvider with priority
     * USER+2 (overrides theme defaults but keeps engine defaults).
     */
    fun gtkLoadTheme(css: String) {
        f("gtk_bridge_load_theme").invokeInt(arrayOf<Any>(css))
    }

    private fun gtkMainLoop() {
        while (true) {
            gtkMainIteration()
        }
    }

    fun gtkMainIteration(): Unit {
        f("gtk_bridge_main_context_iteration").invokeInt(arrayOf<Any>())
    }

    fun gtkMainQuit() {
        f("gtk_bridge_main_quit").invokeVoid(arrayOf<Any>())
    }

    // ─── Kotlin → Rust upcall registration ──────────────────────────────────

    /**
     * Register a Kotlin callback so Rust can call back into Kotlin.
     * Uses JNA upcall mechanism.
     */
    fun gtkRegisterInvoker(callback: (handle: Long, value: Long) -> Unit) {
        // Create a concrete JNA callback implementation
        val invoker = object : Callback {
            @Throws(Throwable::class)
            fun invoke(handle: Long, ptr: Pointer?) {
                // Extract raw address from Pointer (peer field is protected in JNA).
                val value = if (ptr == null) 0L else {
                    try {
                        val f = Pointer::class.java.getDeclaredField("peer")
                        f.isAccessible = true
                        f.getLong(ptr)
                    } catch (e: Exception) {
                        0L
                    }
                }
                callback(handle, value)
            }
        }
        val fp = CallbackReference.getFunctionPointer(invoker)
        val peerField = com.sun.jna.Pointer::class.java.getDeclaredField("peer")
        peerField.isAccessible = true
        val fpAddr = peerField.getLong(fp)
        f("gtk_bridge_register_invoker").invokeVoid(arrayOf(fpAddr))
        System.err.println("[GTKNative] gtkRegisterInvoker: ok")
    }

    // ─── Library path resolution ─────────────────────────────────────────────

    private fun findLibraryPath(): String {
        // 1. Honor explicit env var (set by launcher / IDE).
        System.getenv("GTK4KT_LIB")?.let { p ->
            val f = File(p)
            if (f.exists()) return f.absolutePath
        }
        // 2. Honor java.library.path (jna.library.path also tried by JNA internally).
        for (p in System.getProperty("java.library.path", "").split(File.pathSeparator)) {
            if (p.isBlank()) continue
            val f = File(p, "libgtk4kt_native.so")
            if (f.exists()) return f.absolutePath
        }
        // 3. Search cwd and a few well-known install layouts.
        val candidates = listOf(
            File("lib/libgtk4kt_native.so"),
            File("build/install/gtk4kt/lib/libgtk4kt_native.so"),
            File("/usr/local/lib/libgtk4kt_native.so"),
        )
        for (c in candidates) if (c.exists()) return c.absolutePath
        // 4. Last resort: bare name, hope LD_LIBRARY_PATH covers it.
        return "libgtk4kt_native.so"
    }
}
