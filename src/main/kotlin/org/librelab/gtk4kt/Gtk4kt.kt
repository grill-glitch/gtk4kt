@file:Suppress("unused", "UNUSED_PARAMETER")

package org.librelab.gtk4kt

import org.librelab.gtk4kt.internal.GTKNative

// DSL marker
@DslMarker
annotation class GTK4KT

/**
 * A reference to a GTK widget, held as a raw pointer (Long).
 * The underlying GTK object is managed by GTK; this is just a handle.
 */
@GTK4KT
class Widget(val ptr: Long) {
    init { require(ptr != GTKNative.NULL_PTR) { "null widget pointer" } }
}

/** A reference to a GTK Window. */
@GTK4KT
class WindowRef(val ptr: Long) {
    init { require(ptr != GTKNative.NULL_PTR) { "null window pointer" } }
}

/** GTK4 orientation for boxes. */
enum class Orientation {
    Horizontal, Vertical
}

/** GTK4 text alignment. */
enum class Align {
    Start, Center, End, Fill
}

// ========================================================================
// Application DSL
// ========================================================================

/**
 * Top-level application entry. Creates a GtkApplication and runs it.
 *
 * Usage:
 * ```
 * fun main() = application("org.example.App") {
 *     window("My App") {
 *         // ...
 *     }
 * }
 * ```
 */
fun application(appId: String, block: ApplicationScope.() -> Unit) {
    GTKNative.gtkInit()
    val app = GTKNative.gtkApplicationNew(appId)
    require(app != GTKNative.NULL_PTR) { "failed to create GTK application" }

    // The block creates widgets INSIDE the activate callback
    // We pass appPtr so window() can create GtkApplicationWindow with it
    ApplicationScope(app).apply {
        block()
    }

    // gtk_application_run blocks until quit; activate signal fires synchronously
    GTKNative.gtkApplicationRun(app)
}

@GTK4KT
class ApplicationScope(private val appPtr: Long) {
    internal var currentWindow: WindowRef? = null

    /**
     * Create a top-level window.
     *
     * Usage:
     * ```
     * window("Title", width = 800, height = 600) {
     *     // child widgets
     * }
     * ```
     */
    fun window(
        title: String = "",
        width: Int = 800,
        height: Int = 600,
        block: WindowScope.() -> Unit
    ): WindowRef {
        val win = GTKNative.gtkWindowNew(appPtr)
        require(win != GTKNative.NULL_PTR) { "failed to create window" }
        GTKNative.gtkWindowSetTitle(win, title)
        GTKNative.gtkWindowSetDefaultSize(win, width, height)

        val ws = WindowScope(win)
        ws.block()

        currentWindow = WindowRef(win)
        GTKNative.gtkWindowPresent(win)
        return currentWindow!!
    }
}

// ========================================================================
// Window DSL
// ========================================================================

@GTK4KT
class WindowScope(private val windowPtr: Long) {
    private val contentBox: Long = GTKNative.gtkBoxNew(GTKNative.ORIENTATION_VERTICAL, 0)

    init {
        GTKNative.gtkWidgetShow(contentBox)
    }

    /**
     * Set the child widget of this window.
     * Replaces any previously set child.
     */
    fun child(widget: Widget) {
        GTKNative.gtkWidgetShow(widget.ptr)
        GTKNative.gtkWindowSetChild(windowPtr, widget.ptr)
    }

    /**
     * Add a widget to the window content area.
     * The window uses a vertical box internally.
     */
    fun add(widget: Widget) {
        GTKNative.gtkBoxAppend(contentBox, widget.ptr)
    }
}

// ========================================================================
// Factory functions for widgets
// ========================================================================

/**
 * Create a Label widget.
 *
 * Usage:
 * ```
 * label("Hello, world!")
 * ```
 */
fun label(text: String): Widget {
    val ptr = GTKNative.gtkLabelNew(text)
    require(ptr != GTKNative.NULL_PTR) { "failed to create label" }
    return Widget(ptr).also { GTKNative.gtkWidgetShow(it.ptr) }
}

/**
 * Create a Button widget with an optional click handler.
 *
 * Usage:
 * ```
 * button("Click me!") {
 *     count.value = count.value + 1
 * }
 * ```
 */
fun button(label: String, onClicked: () -> Unit = {}): Widget {
    val ptr = GTKNative.gtkButtonNewWithLabel(label)
    require(ptr != GTKNative.NULL_PTR) { "failed to create button" }
    // TODO: wire up onClicked via Panama callback (Phase 2 onClick implementation)
    return Widget(ptr).also { GTKNative.gtkWidgetShow(it.ptr) }
}

/**
 * Create a Box container with vertical orientation.
 *
 * Usage:
 * ```
 * column(spacing = 8) {
 *     label("First")
 *     label("Second")
 * }
 * ```
 */
fun column(spacing: Int = 0, block: BoxScope.() -> Unit): Widget {
    val ptr = GTKNative.gtkBoxNew(GTKNative.ORIENTATION_VERTICAL, spacing)
    require(ptr != GTKNative.NULL_PTR) { "failed to create column box" }
    val box = BoxScope(ptr)
    box.block()
    return Widget(ptr).also { GTKNative.gtkWidgetShow(it.ptr) }
}

/**
 * Create a Box container with horizontal orientation.
 *
 * Usage:
 * ```
 * row(spacing = 4) {
 *     button("OK")
 *     button("Cancel")
 * }
 * ```
 */
fun row(spacing: Int = 0, block: BoxScope.() -> Unit): Widget {
    val ptr = GTKNative.gtkBoxNew(GTKNative.ORIENTATION_HORIZONTAL, spacing)
    require(ptr != GTKNative.NULL_PTR) { "failed to create row box" }
    val box = BoxScope(ptr)
    box.block()
    return Widget(ptr).also { GTKNative.gtkWidgetShow(it.ptr) }
}

@GTK4KT
class BoxScope(private val boxPtr: Long) {
    fun add(widget: Widget) {
        GTKNative.gtkBoxAppend(boxPtr, widget.ptr)
    }
}

/**
 * Apply modifier to a widget (currently a no-op stub).
 *
 * Usage:
 * ```
 * label("Hello").modifier(Modifier.padding(16).fillWidth())
 * ```
 */
fun Widget.modifier(block: Modifier.() -> Unit): Widget {
    val m = Modifier()
    m.block()
    // TODO: apply modifier properties to GTK widget
    return this
}

/**
 * Modifier for widget styling and layout.
 */
class Modifier {
    private var margin: Int = 0
    private var padding: Int = 0

    fun padding(all: Int) {
        padding = all
    }

    fun fillWidth() {
        // TODO: map to GTK size request / expand flags
    }

    fun fillHeight() {
        // TODO
    }

    internal fun applyTo(ptr: Long) {
        if (padding > 0) {
            GTKNative.gtkWidgetSetMargin(ptr, padding)
        }
    }
}
