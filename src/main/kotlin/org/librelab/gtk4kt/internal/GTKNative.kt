@file:Suppress("unused", "UNUSED_PARAMETER")

package org.librelab.gtk4kt.internal

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.nio.charset.StandardCharsets

/**
 * JDK 21 Panama FFI binding for libgtk4kt_native.so (Rust cdylib).
 *
 * Key design: Arena.ofAuto() is NOT persistent — it can be GC'd.
 * PersistentArena holds the REAL persistent Arena for the upcall stub lifetime.
 */
object GTKNative {
    private val linker = Linker.nativeLinker()
    private val lookup: SymbolLookup = SymbolLookup.loaderLookup()
    // Session arena for cstr() strings — short-lived, OK to GC between calls
    private val session = Arena.ofAuto()

    // Global methodHandle cache: handleId (Long) → MethodHandle (persistent)
    private val methodHandleCache = mutableMapOf<Long, MethodHandle>()

    init {
        val libPath = System.getProperty("java.library.path", "")
        val soFile = if (libPath.isNotEmpty()) "$libPath/libgtk4kt_native.so" else "libgtk4kt_native.so"
        try {
            System.load(soFile)
        } catch (e: UnsatisfiedLinkError) {
            System.loadLibrary("gtk4kt_native")
        }
        System.err.println("[GTKNative] loaded: $soFile")
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private fun sym(name: String): MemorySegment =
        lookup.find(name).orElseThrow { RuntimeException("symbol not found: $name") }

    private fun cstr(s: String): MemorySegment {
        val bytes = ("$s\u0000").toByteArray(StandardCharsets.UTF_8)
        val native = session.allocate(bytes.size.toLong())
        native.copyFrom(MemorySegment.ofArray(bytes))
        return native
    }

    private fun downcall(name: String, desc: FunctionDescriptor): MethodHandle =
        linker.downcallHandle(sym(name), desc)

    // ========================================================================
    // Cached handles
    // ========================================================================

    private val hInit = downcall("gtk_bridge_init", FunctionDescriptor.of(ValueLayout.JAVA_INT))
    private val hAppNew = downcall("gtk_bridge_application_new", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS))
    private val hAppRun = downcall("gtk_bridge_application_run", FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG))
    private val hAppQuit = downcall("gtk_bridge_application_quit", FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG))
    private val hSetUiPath = downcall("gtk_bridge_set_ui_json_path", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS))
    private val hWinNew = downcall("gtk_bridge_window_new", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG))
    private val hWinTitle = downcall("gtk_bridge_window_set_title", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS))
    private val hWinSize = downcall("gtk_bridge_window_set_default_size", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT))
    private val hWinChild = downcall("gtk_bridge_window_set_child", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG))
    private val hWinPresent = downcall("gtk_bridge_window_present", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG))
    private val hBoxNew = downcall("gtk_bridge_box_new", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT))
    private val hBoxAppend = downcall("gtk_bridge_box_append", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG))
    private val hLabelNew = downcall("gtk_bridge_label_new", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS))
    private val hLabelSet = downcall("gtk_bridge_label_set_text", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS))
    private val hBtnNew = downcall("gtk_bridge_button_new_with_label", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS))
    private val hBtnSet = downcall("gtk_bridge_button_set_label", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.ADDRESS))
    private val hWidgetShow = downcall("gtk_bridge_widget_show", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG))
    private val hWidgetDestroy = downcall("gtk_bridge_widget_destroy", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG))
    private val hWidgetMargin = downcall("gtk_bridge_widget_set_margin", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT))
    private val hWidgetSizeReq = downcall("gtk_bridge_widget_set_size_request", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT))
    private val hWidgetHalign = downcall("gtk_bridge_widget_set_halign", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT))
    private val hWidgetValign = downcall("gtk_bridge_widget_set_valign", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT))
    private val hWidgetHexpand = downcall("gtk_bridge_widget_set_hexpand", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT))
    private val hWidgetVexpand = downcall("gtk_bridge_widget_set_vexpand", FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT))
    private val hBuildUi = downcall("gtk_bridge_builder_build_ui", FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_LONG))
    private val hRegInvoker = downcall("gtk_bridge_register_method_invoker", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS))

    // ========================================================================
    // GTK Init & Application
    // ========================================================================

    fun gtkInit(): Int = hInit.invoke() as Int

    fun gtkApplicationNew(appId: String): Long = hAppNew.invoke(cstr(appId)) as Long

    fun gtkSetUiJsonPath(path: String): Int = hSetUiPath.invoke(cstr(path)) as Int

    fun gtkApplicationRun(appPtr: Long) {
        // ALL GTK operations run on a single dedicated thread.
        // System.getProperty carries the JSON path from the main thread.
        val jsonPath = System.getProperty("gtk4kt.ui.json.path", "")
        val gtkThread = Thread {
            gtkInit()  // Initialize GTK on this background thread
            if (jsonPath.isNotEmpty()) {
                gtkSetUiJsonPath(jsonPath)
            }
            registerMethodInvoker()
            System.err.println("[gtk4kt] gtkApplicationRun: calling native gtk_main()...")
            gtkMainLoop(0)  // blocks until gtk_main_quit()
            System.err.println("[gtk4kt] gtkApplicationRun: gtk_main() returned")
        }.apply { start() }
        gtkThread.join()
    }

    // Internal: gtk_main() native call (blocking) — must be called on the GTK thread
    private fun gtkMainLoop(appPtr: Long) {
        hAppRun.invoke(appPtr, 0L)
    }

    fun gtkApplicationQuit(appPtr: Long): Int = hAppQuit.invoke(appPtr) as Int

    // ========================================================================
    // Window
    // ========================================================================

    fun gtkWindowNew(appPtr: Long): Long = hWinNew.invoke(appPtr) as Long

    fun gtkWindowSetTitle(windowPtr: Long, title: String): Int =
        hWinTitle.invoke(windowPtr, cstr(title)) as Int

    fun gtkWindowSetDefaultSize(windowPtr: Long, w: Int, h: Int): Int =
        hWinSize.invoke(windowPtr, w, h) as Int

    fun gtkWindowSetChild(windowPtr: Long, childPtr: Long): Int =
        hWinChild.invoke(windowPtr, childPtr) as Int

    fun gtkWindowPresent(windowPtr: Long): Int = hWinPresent.invoke(windowPtr) as Int

    // ========================================================================
    // Box
    // ========================================================================

    fun gtkBoxNew(orientation: Int, spacing: Int): Long =
        hBoxNew.invoke(orientation, spacing) as Long

    fun gtkBoxAppend(boxPtr: Long, childPtr: Long): Int =
        hBoxAppend.invoke(boxPtr, childPtr) as Int

    // ========================================================================
    // Label
    // ========================================================================

    fun gtkLabelNew(text: String): Long = hLabelNew.invoke(cstr(text)) as Long

    fun gtkLabelSetText(labelPtr: Long, text: String): Int =
        hLabelSet.invoke(labelPtr, cstr(text)) as Int

    // ========================================================================
    // Button
    // ========================================================================

    fun gtkButtonNewWithLabel(label: String): Long =
        hBtnNew.invoke(cstr(label)) as Long

    fun gtkButtonSetLabel(buttonPtr: Long, label: String): Int =
        hBtnSet.invoke(buttonPtr, cstr(label)) as Int

    // ========================================================================
    // Widget lifecycle & properties
    // ========================================================================

    fun gtkWidgetShow(widgetPtr: Long): Int = hWidgetShow.invoke(widgetPtr) as Int

    fun gtkWidgetDestroy(widgetPtr: Long): Int = hWidgetDestroy.invoke(widgetPtr) as Int

    fun gtkWidgetSetMargin(widgetPtr: Long, margin: Int): Int =
        hWidgetMargin.invoke(widgetPtr, margin) as Int

    fun gtkWidgetSetSizeRequest(widgetPtr: Long, w: Int, h: Int): Int =
        hWidgetSizeReq.invoke(widgetPtr, w, h) as Int

    fun gtkWidgetSetHalign(widgetPtr: Long, align: Int): Int =
        hWidgetHalign.invoke(widgetPtr, align) as Int

    fun gtkWidgetSetValign(widgetPtr: Long, align: Int): Int =
        hWidgetValign.invoke(widgetPtr, align) as Int

    fun gtkWidgetSetHexpand(widgetPtr: Long, expand: Boolean): Int =
        hWidgetHexpand.invoke(widgetPtr, if (expand) 1 else 0) as Int

    fun gtkWidgetSetVexpand(widgetPtr: Long, expand: Boolean): Int =
        hWidgetVexpand.invoke(widgetPtr, if (expand) 1 else 0) as Int

    // ========================================================================
    // JSON UI Builder (Phase 3)
    // ========================================================================

    fun gtkBuildUi(json: String, appPtr: Long): Long =
        hBuildUi.invoke(cstr(json), appPtr) as Long

    // ========================================================================
    // MethodHandle invocation (Rust → Kotlin upcall)
    // ========================================================================

    // PersistentArena: keeps the upcall stub alive for the JVM process lifetime.
    // If this is ever GC'd, the upcall stub pointer becomes dangling → SIGSEGV.
    object PersistentArena {
        var session: Arena? = null
        var registered: Boolean = false
    }

    private val kotlinLinker = Linker.nativeLinker()

    @Suppress("UNUSED")
    private fun invokeMethodFromRust(handleId: Long, widgetPtr: Long): Int {
        val mh = methodHandleCache[handleId]
        if (mh == null) {
            System.err.println("[GTKNative] no handle for id=$handleId")
            return -1
        }
        return try {
            mh.invokeWithArguments(widgetPtr)
            0
        } catch (e: Throwable) {
            System.err.println("[GTKNative] invokeMethod failed: $e")
            -1
        }
    }

    fun registerMethodHandle(handleId: Long, target: MethodHandle) {
        methodHandleCache[handleId] = target
    }

    private fun registerMethodInvoker() {
        if (PersistentArena.registered) return
        try {
            PersistentArena.session = Arena.ofAuto()
            val mh = MethodHandles.lookup().findVirtual(
                GTKNative::class.java,
                "invokeMethodFromRust",
                MethodType.methodType(Int::class.java, Long::class.java, Long::class.java)
            ).bindTo(this)

            val fdesc = FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_LONG
            )
            val stub = kotlinLinker.upcallStub(mh, fdesc, PersistentArena.session!!)
            hRegInvoker.invoke(stub)
            PersistentArena.registered = true
            System.err.println("[GTKNative] MethodHandle invoker registered")
        } catch (e: Throwable) {
            System.err.println("[GTKNative] registerMethodInvoker failed: $e")
            e.printStackTrace()
        }
    }

    // ========================================================================
    // Test helpers (for UI verification only)
    // ========================================================================

    fun gtkTestClick(widgetPtr: Long): Int {
        val sym = lookup.find("gtk_bridge_test_click").orElseThrow()
        val h = linker.downcallHandle(sym, FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG))
        return h.invoke(widgetPtr) as Int
    }

    fun gtkMainQuit() {
        val sym = lookup.find("gtk_bridge_main_quit").orElseThrow()
        val h = linker.downcallHandle(sym, FunctionDescriptor.ofVoid())
        h.invoke()
    }

    // ========================================================================
    // Constants
    // ========================================================================

    const val NULL_PTR: Long = -1L
    const val ORIENTATION_HORIZONTAL: Int = 0
    const val ORIENTATION_VERTICAL: Int = 1
    const val ALIGN_START: Int = 0
    const val ALIGN_CENTER: Int = 1
    const val ALIGN_END: Int = 2
    const val ALIGN_FILL: Int = 3
}
