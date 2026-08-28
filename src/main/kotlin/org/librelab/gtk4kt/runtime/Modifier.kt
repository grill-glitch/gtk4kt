package org.librelab.gtk4kt.runtime

// ============================================================================
// Alignment / fill constants (Compose-like)
// ============================================================================

/** Compose-like fill/align policy hints — mapped to GTK sizing properties. */
object FillPolicy {
    const val NONE = 0
    const val HORIZONTAL = 1
    const val VERTICAL = 2
    const val BOTH = 3
}

object Alignment {
    const val START = 0
    const val CENTER = 1
    const val END = 2
    const val FILL = 3
}

// ============================================================================
// Modifier — Compose-like chainable widget styling
// ============================================================================

/**
 * Compose-like chainable modifier for widget styling and layout.
 * Each call returns a NEW Modifier with the additional property applied.
 *
 * Usage (mirrors Compose):
 * ```
 * Text("Hello", modifier = Modifier.padding(16).fillMaxWidth())
 * Button("Ok", modifier = Modifier.fillMaxWidth().weight(1f))
 * Spacer(Modifier.height(8))
 * ```
 *
 * Property names intentionally match Compose: padding, size, width, height,
 * fillMaxWidth, fillMaxHeight, weight, alignCenterHorizontally, margin.
 */
class Modifier private constructor(
    private val paddingStart: Int = 0,
    private val paddingEnd: Int = 0,
    private val paddingTop: Int = 0,
    private val paddingBottom: Int = 0,
    private val width: Int = -1,
    private val height: Int = -1,
    private val fillMaxWidth: Boolean = false,
    private val fillMaxHeight: Boolean = false,
    private val halign: Int = -1,
    private val valign: Int = -1,
    private val weight: Float = 0f,
    private val margin: Int = -1,
) {
    companion object {
        /** Default empty modifier — no styling. */
        val Empty = Modifier()

        // ─── Compose-like factory functions on the companion ─────────────────

        /** Apply uniform padding to all sides. */
        fun padding(all: Int): Modifier = Modifier().copy(paddingStart = all, paddingEnd = all, paddingTop = all, paddingBottom = all)

        /** Apply horizontal padding (start + end). */
        fun paddingHorizontal(horizontal: Int): Modifier = Modifier().copy(paddingStart = horizontal, paddingEnd = horizontal)

        /** Apply vertical padding (top + bottom). */
        fun paddingVertical(vertical: Int): Modifier = Modifier().copy(paddingTop = vertical, paddingBottom = vertical)

        /** Fixed width (pixels). */
        fun width(width: Int): Modifier = Modifier().copy(width = width)

        /** Fixed height (pixels). */
        fun height(height: Int): Modifier = Modifier().copy(height = height)

        /** Fixed width + height (pixels). Use -1 to keep natural size on that axis. */
        fun size(width: Int, height: Int): Modifier = Modifier().copy(width = width, height = height)

        /** Compose-like fillMaxWidth(). */
        fun fillMaxWidth(): Modifier = Modifier().copy(fillMaxWidth = true)

        /** Compose-like fillMaxHeight(). */
        fun fillMaxHeight(): Modifier = Modifier().copy(fillMaxHeight = true)

        /** Align horizontally centered. */
        fun alignCenterHorizontally(): Modifier = Modifier().copy(halign = Alignment.CENTER)

        fun alignStart(): Modifier = Modifier().copy(halign = Alignment.START)
        fun alignEnd(): Modifier = Modifier().copy(halign = Alignment.END)
        fun alignCenter(): Modifier = Modifier().copy(halign = Alignment.CENTER, valign = Alignment.CENTER)
        fun alignTop(): Modifier = Modifier().copy(valign = Alignment.START)
        fun alignBottom(): Modifier = Modifier().copy(valign = Alignment.END)

        /** Weight for distribution inside Row/Column (like RowScope.weight). */
        fun weight(weight: Float): Modifier = Modifier().copy(weight = weight)

        /** Margin (all sides). */
        fun margin(all: Int): Modifier = Modifier().copy(margin = all)
    }

    private fun copy(
        paddingStart: Int = this.paddingStart,
        paddingEnd: Int = this.paddingEnd,
        paddingTop: Int = this.paddingTop,
        paddingBottom: Int = this.paddingBottom,
        width: Int = this.width,
        height: Int = this.height,
        fillMaxWidth: Boolean = this.fillMaxWidth,
        fillMaxHeight: Boolean = this.fillMaxHeight,
        halign: Int = this.halign,
        valign: Int = this.valign,
        weight: Float = this.weight,
        margin: Int = this.margin,
    ) = Modifier(
        paddingStart, paddingEnd, paddingTop, paddingBottom,
        width, height, fillMaxWidth, fillMaxHeight,
        halign, valign, weight, margin
    )

    // ─── Instance chain methods (Compose-like: Modifier.Empty.padding(16).fillMaxWidth()) ──

    fun padding(all: Int): Modifier = copy(paddingStart = all, paddingEnd = all, paddingTop = all, paddingBottom = all)

    fun paddingHorizontal(horizontal: Int): Modifier = copy(paddingStart = horizontal, paddingEnd = horizontal)

    fun paddingVertical(vertical: Int): Modifier = copy(paddingTop = vertical, paddingBottom = vertical)

    fun width(width: Int): Modifier = copy(width = width)

    fun height(height: Int): Modifier = copy(height = height)

    fun size(width: Int, height: Int): Modifier = copy(width = width, height = height)

    fun fillMaxWidth(): Modifier = copy(fillMaxWidth = true)

    fun fillMaxHeight(): Modifier = copy(fillMaxHeight = true)

    fun alignCenterHorizontally(): Modifier = copy(halign = Alignment.CENTER)

    fun alignStart(): Modifier = copy(halign = Alignment.START)
    fun alignEnd(): Modifier = copy(halign = Alignment.END)
    fun alignCenter(): Modifier = copy(halign = Alignment.CENTER, valign = Alignment.CENTER)
    fun alignTop(): Modifier = copy(valign = Alignment.START)
    fun alignBottom(): Modifier = copy(valign = Alignment.END)

    fun weight(weight: Float): Modifier = copy(weight = weight)

    fun margin(all: Int): Modifier = copy(margin = all)

    // ─── Apply (serialization) ───────────────────────────────────────────────

    /** Encode this modifier's properties as JSON fields to append to a widget node. */
    fun toJsonFields(): String {
        val sb = StringBuilder()
        if (paddingStart != 0) sb.append(",\"paddingStart\":$paddingStart")
        if (paddingEnd != 0) sb.append(",\"paddingEnd\":$paddingEnd")
        if (paddingTop != 0) sb.append(",\"paddingTop\":$paddingTop")
        if (paddingBottom != 0) sb.append(",\"paddingBottom\":$paddingBottom")
        if (width > 0) sb.append(",\"width\":$width")
        if (height > 0) sb.append(",\"height\":$height")
        if (fillMaxWidth) sb.append(",\"fillMaxWidth\":true")
        if (fillMaxHeight) sb.append(",\"fillMaxHeight\":true")
        if (halign >= 0) sb.append(",\"halign\":$halign")
        if (valign >= 0) sb.append(",\"valign\":$valign")
        if (weight > 0f) sb.append(",\"weight\":$weight")
        if (margin > 0) sb.append(",\"margin\":$margin")
        return sb.toString()
    }
}