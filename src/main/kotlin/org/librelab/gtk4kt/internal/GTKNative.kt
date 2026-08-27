@file:Suppress("unused", "UNUSED_PARAMETER")

package org.librelab.gtk4kt.internal

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle

/**
 * Kotlin binding for libgtk4kt_native.so via JDK 21 Panama Foreign Function API.
 * Architecture: Kotlin/JVM → Panama → Rust extern "C" → GTK4
 *
 * No JNI C shim, no JNA. Pure JDK 21 API.
 */

object GTKNative {

    private val arena = Arena.ofAuto()
    private val linker = Linker.nativeLinker()

    // Load via System.load (library must be in java.library.path or LD_LIBRARY_PATH)
    init {
        val candidates = listOf(
            "${System.getProperty("user.dir")}/lib/libgtk4kt_native.so",
            "/home/bigbang/gtk4kt/build/install/gtk4kt/lib/libgtk4kt_native.so",
        )
        for (c in candidates) {
            val f = java.io.File(c)
            if (f.exists()) {
                System.load(f.absolutePath)
                break
            }
        }
    }

    // loaderLookup() finds symbols in libraries loaded via System.load()
    private val lookup: SymbolLookup = SymbolLookup.loaderLookup()

    private fun sym(name: String): MemorySegment =
        lookup.find(name).orElseThrow { UnsatisfiedLinkError("undefined symbol: $name") }

    private fun cstr(s: String): MemorySegment = arena.allocateUtf8String(s)

    // ========================================================================
    // Raw downcall handles
    // ========================================================================

    private val hInit = linker.downcallHandle(
        sym("gtk_bridge_init"),
        FunctionDescriptor.of(ValueLayout.JAVA_INT)
    )
    private val hAppNew = linker.downcallHandle(
        sym("gtk_bridge_application_new"),
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
    )
    private val hAppRun = linker.downcallHandle(
        sym("gtk_bridge_application_run"),
        FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG)
    )
    private val hQuit = linker.downcallHandle(
        sym("gtk_bridge_main_quit"),
        FunctionDescriptor.ofVoid()
    )
    private val hWinNew = linker.downcallHandle(
        sym("gtk_bridge_window_new"),
        FunctionDescriptor.of(ValueLayout.JAVA_LONG)
    )
    private val hWinSetTitle = linker.downcallHandle(
        sym("gtk_bridge_window_set_title"),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
    )
    private val hWinSetDefaultSize = linker.downcallHandle(
        sym("gtk_bridge_window_set_default_size"),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
    )
    private val hWinPresent = linker.downcallHandle(
        sym("gtk_bridge_window_present"),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG)
    )
    private val hWinDestroy = linker.downcallHandle(
        sym("gtk_bridge_window_destroy"),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG)
    )
    private val hLabelNew = linker.downcallHandle(
        sym("gtk_bridge_label_new"),
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
    )
    private val hLabelSetText = linker.downcallHandle(
        sym("gtk_bridge_label_set_text"),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
    )
    private val hLabelGetText = linker.downcallHandle(
        sym("gtk_bridge_label_get_text"),
        FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.JAVA_LONG)
    )
    private val hBtnNew = linker.downcallHandle(
        sym("gtk_bridge_button_new_with_label"),
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
    )
    private val hBtnSetLabel = linker.downcallHandle(
        sym("gtk_bridge_button_set_label"),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.ADDRESS)
    )
    private val hBoxNew = linker.downcallHandle(
        sym("gtk_bridge_box_new"),
        FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
    )
    private val hBoxAppend = linker.downcallHandle(
        sym("gtk_bridge_box_append"),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG)
    )
    private val hWidgetShow = linker.downcallHandle(
        sym("gtk_bridge_widget_show"),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG)
    )
    private val hWidgetDestroy = linker.downcallHandle(
        sym("gtk_bridge_widget_destroy"),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG)
    )
    private val hWidgetSetMargin = linker.downcallHandle(
        sym("gtk_bridge_widget_set_margin"),
        FunctionDescriptor.ofVoid(ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT)
    )

    // ========================================================================
    // Kotlin wrappers
    // ========================================================================

    fun gtkInit(): Int = hInit.invoke() as Int

    fun gtkApplicationNew(appId: String?, flags: Int = 0): Long =
        hAppNew.invoke(if (appId != null) cstr(appId) else MemorySegment.NULL, flags) as Long

    fun gtkApplicationRun(appPtr: Long): Int = hAppRun.invoke(appPtr) as Int

    fun gtkMainQuit() { hQuit.invoke() }

    fun gtkWindowNew(): Long = hWinNew.invoke() as Long

    fun gtkWindowSetTitle(windowPtr: Long, title: String) =
        hWinSetTitle.invoke(windowPtr, cstr(title))

    fun gtkWindowSetDefaultSize(windowPtr: Long, width: Int, height: Int) =
        hWinSetDefaultSize.invoke(windowPtr, width, height)

    fun gtkWindowPresent(windowPtr: Long) = hWinPresent.invoke(windowPtr)

    fun gtkWindowDestroy(windowPtr: Long) = hWinDestroy.invoke(windowPtr)

    fun gtkLabelNew(text: String): Long = hLabelNew.invoke(cstr(text)) as Long

    fun gtkLabelSetText(labelPtr: Long, text: String) =
        hLabelSetText.invoke(labelPtr, cstr(text))

    fun gtkLabelGetText(labelPtr: Long): String? {
        val seg = hLabelGetText.invoke(labelPtr) as MemorySegment
        return if (seg == MemorySegment.NULL) null else seg.getUtf8String(0)
    }

    fun gtkButtonNewWithLabel(label: String): Long =
        hBtnNew.invoke(cstr(label)) as Long

    fun gtkButtonSetLabel(buttonPtr: Long, label: String) =
        hBtnSetLabel.invoke(buttonPtr, cstr(label))

    fun gtkBoxNew(orientation: Int, spacing: Int): Long =
        hBoxNew.invoke(orientation, spacing) as Long

    fun gtkBoxAppend(boxPtr: Long, childPtr: Long) =
        hBoxAppend.invoke(boxPtr, childPtr)

    fun gtkWidgetShow(widgetPtr: Long) = hWidgetShow.invoke(widgetPtr)

    fun gtkWidgetDestroy(widgetPtr: Long) = hWidgetDestroy.invoke(widgetPtr)

    fun gtkWidgetSetMargin(widgetPtr: Long, margin: Int) =
        hWidgetSetMargin.invoke(widgetPtr, margin)

    // ========================================================================
    // Constants
    // ========================================================================

    const val ORIENTATION_HORIZONTAL = 0
    const val ORIENTATION_VERTICAL = 1
    const val NULL_PTR: Long = -1L
}
