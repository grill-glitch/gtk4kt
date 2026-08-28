package org.librelab.gtk4kt.runtime

import org.librelab.gtk4kt.internal.GTKNative

// ============================================================================
// Alignment constants
// ============================================================================

/** GTK4 alignment / halign/valign values. */
object Alignment {
    const val START = 0
    const val CENTER = 1
    const val END = 2
    const val FILL = 3
}

// ============================================================================
// Modifier
// ============================================================================

/**
 * Chainable modifier for widget styling and layout.
 * Inspired by Compose Modifier — each call returns a new Modifier with
 * additional properties applied.
 *
 * Usage:
 * ```
 * widget(
 *     Modifier.padding(16).fillWidth().alignCenter()
 * )
 * ```
 */
class Modifier private constructor(
    private val padding: Int = 0,
    private val margin: Int = 0,
    private val hexpand: Boolean = false,
    private val vexpand: Boolean = false,
    private val halign: Int = -1,   // -1 = unset
    private val valign: Int = -1,
    private val sizeRequestW: Int = -1,
    private val sizeRequestH: Int = -1,
) {
    companion object {
        /** Empty modifier. */
        val Empty = Modifier()
    }

    // ========================================================================
    // Layout modifiers
    // ========================================================================

    /** Set padding on all sides (in pixels). */
    fun padding(all: Int): Modifier = Modifier(all, margin, hexpand, vexpand, halign, valign, sizeRequestW, sizeRequestH)

    /** Set margin on all sides (in pixels). */
    fun margin(all: Int): Modifier = Modifier(padding, all, hexpand, vexpand, halign, valign, sizeRequestW, sizeRequestH)

    /** Set horizontal expansion — widget takes all available horizontal space. */
    fun fillWidth(): Modifier = Modifier(padding, margin, true, vexpand, halign, valign, sizeRequestW, sizeRequestH)

    /** Set vertical expansion — widget takes all available vertical space. */
    fun fillHeight(): Modifier = Modifier(padding, margin, hexpand, true, halign, valign, sizeRequestW, sizeRequestH)

    /**
     * Set size request (fixed pixel size).
     * Pass -1 for either dimension to use natural size.
     */
    fun size(width: Int, height: Int): Modifier =
        Modifier(padding, margin, hexpand, vexpand, halign, valign, width, height)

    // ========================================================================
    // Alignment modifiers
    // ========================================================================

    fun alignStart(): Modifier = Modifier(padding, margin, hexpand, vexpand, Alignment.START, valign, sizeRequestW, sizeRequestH)
    fun alignCenter(): Modifier = Modifier(padding, margin, hexpand, vexpand, Alignment.CENTER, valign, sizeRequestW, sizeRequestH)
    fun alignEnd(): Modifier = Modifier(padding, margin, hexpand, vexpand, Alignment.END, valign, sizeRequestW, sizeRequestH)
    fun fill(): Modifier = Modifier(padding, margin, true, vexpand, Alignment.FILL, valign, sizeRequestW, sizeRequestH)

    // ========================================================================
    // Apply to GTK widget
    // ========================================================================

    /**
     * Apply all modifier properties to the widget identified by [ptr].
     * Called when the widget is first created.
     */
    fun applyTo(ptr: Long) {
        if (ptr == GTKNative.NULL_PTR || ptr == 0L) return

        // Margin
        if (margin > 0) {
            GTKNative.gtkWidgetSetMargin(ptr, margin)
        }

        // Padding (set as margin — GTK4 uses margin for inner spacing)
        if (padding > 0) {
            GTKNative.gtkWidgetSetMargin(ptr, padding)
        }

        // Size request
        if (sizeRequestW > 0 || sizeRequestH > 0) {
            val w = if (sizeRequestW <= 0) -1 else sizeRequestW
            val h = if (sizeRequestH <= 0) -1 else sizeRequestH
            GTKNative.gtkWidgetSetSizeRequest(ptr, w, h)
        }

        // Expansion
        if (hexpand) {
            GTKNative.gtkWidgetSetHexpand(ptr, hexpand)
        }
        if (vexpand) {
            GTKNative.gtkWidgetSetVexpand(ptr, vexpand)
        }

        // Alignment (only valid for box children — apply to box)
        if (halign >= 0) {
            GTKNative.gtkWidgetSetHalign(ptr, halign)
        }
        if (valign >= 0) {
            GTKNative.gtkWidgetSetValign(ptr, valign)
        }
    }
}

// ============================================================================
// Modifier factory functions (extension-style)
// ============================================================================

/** Create a Modifier with padding. */
fun Modifier.padding(all: Int): Modifier = this.padding(all)

/** Create a Modifier with margin. */
fun Modifier.margin(all: Int): Modifier = this.margin(all)

/** Fill parent width. */
fun Modifier.fillWidth(): Modifier = this.fillWidth()

/** Fill parent height. */
fun Modifier.fillHeight(): Modifier = this.fillHeight()

/** Fixed size. */
fun Modifier.size(width: Int, height: Int): Modifier = this.size(width, height)

/** Align center. */
fun Modifier.alignCenter(): Modifier = this.alignCenter()
