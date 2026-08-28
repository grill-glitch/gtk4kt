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

/** Compose-like Alignment.Horizontal / Alignment.Vertical helpers. */
object AlignmentHorizontal {
    const val Start = Alignment.START
    const val CenterHorizontally = Alignment.CENTER
    const val End = Alignment.END
}
object AlignmentVertical {
    const val Top = Alignment.START
    const val CenterVertically = Alignment.CENTER
    const val Bottom = Alignment.END
}

/**
 * Compose-like `Arrangement` — controls how Column/Row distribute children.
 * Maps to GTK spacing and (for SpaceBetween etc) sentinel JSON hints that the
 * Rust side can interpret as gtk_box_set_* properties.
 */
object Arrangement {
    object Start { val v = 0 }
    object End { val v = 2 }
    object Top { val v = 0 }
    object Bottom { val v = 2 }
    object Center { val v = 1 }
    object SpaceBetween { val v = 3 }
    object SpaceEvenly { val v = 4 }
    object SpaceAround { val v = 5 }
}

/** Compose-like `Color` — packed ARGB Long. */
typealias Color = Long

object Colors {
    val Transparent: Color = 0x00000000
    val Black: Color = 0xFF000000.toLong()
    val White: Color = 0xFFFFFFFF.toLong()
    val Gray: Color = 0xFF808080.toLong()
    val Red: Color = 0xFFFF0000.toLong()
    val Green: Color = 0xFF00FF00.toLong()
    val Blue: Color = 0xFF0000FF.toLong()
    val Yellow: Color = 0xFFFFFF00.toLong()
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
    // Phase 5a additions
    private val bgColor: Int = -1,         // -1 = unset, otherwise packed ARGB
    private val aspectRatio: Float = 0f,    // 0 = unset
    private val verticalScroll: Boolean = false,
    private val hWeight: Float = 0f,        // horizontal weight (Row's column weight)
    private val vWeight: Float = 0f,        // vertical weight (Column's row weight)
    private val alignmentCrossAxis: Int = -1,  // Alignment.CenterVertically for Row; Horizontal for Column
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
        fun margin(all: Int): Modifier = Modifier().margin(all)

        fun fillMaxSize(): Modifier = Modifier().fillMaxSize()
        fun background(color: Long): Modifier = Modifier().background(color)
        fun aspectRatio(ratio: Float): Modifier = Modifier().aspectRatio(ratio)
        fun verticalScroll(): Modifier = Modifier().verticalScroll()
        fun align(alignment: Int): Modifier = Modifier().align(alignment)
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
        bgColor: Int = this.bgColor,
        aspectRatio: Float = this.aspectRatio,
        verticalScroll: Boolean = this.verticalScroll,
        hWeight: Float = this.hWeight,
        vWeight: Float = this.vWeight,
        alignmentCrossAxis: Int = this.alignmentCrossAxis,
    ) = Modifier(
        paddingStart, paddingEnd, paddingTop, paddingBottom,
        width, height, fillMaxWidth, fillMaxHeight,
        halign, valign, weight, margin,
        bgColor, aspectRatio, verticalScroll, hWeight, vWeight, alignmentCrossAxis,
    )

    // ─── Instance chain methods (Compose-like: Modifier.Empty.padding(16).fillMaxWidth()) ──

    /** Compose-like `padding(all: Int)`. */
    fun padding(all: Int): Modifier = copy(paddingStart = all, paddingEnd = all, paddingTop = all, paddingBottom = all)

    /** Compose-like `padding(horizontal: Int, vertical: Int)`. */
    fun padding(horizontal: Int, vertical: Int): Modifier = copy(
        paddingStart = horizontal, paddingEnd = horizontal,
        paddingTop = vertical, paddingBottom = vertical,
    )

    /** Compose-like `padding(start, top, end, bottom)`. */
    fun padding(start: Int, top: Int, end: Int, bottom: Int): Modifier = copy(
        paddingStart = start, paddingEnd = end,
        paddingTop = top, paddingBottom = bottom,
    )

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

    // ─── Phase 5a: Compose-like additions ─────────────────────────────────

    /** Compose-like `fillMaxSize()` — both fillMaxWidth + fillMaxHeight. */
    fun fillMaxSize(): Modifier = copy(fillMaxWidth = true, fillMaxHeight = true)

    /** Compose-like `background(Color)` — packed ARGB int (0xAARRGGBB). */
    fun background(color: Long): Modifier = copy(bgColor = color.toInt())

    /** Compose-like `aspectRatio(ratio)` — width/height. 0f to clear. */
    fun aspectRatio(ratio: Float): Modifier = copy(aspectRatio = ratio)

    /**
     * Compose-like `verticalScroll(state)` — wrap content in a ScrolledWindow.
     * Currently a no-op flag; GTK ScrolledWindow wrapping will be added
     * when the Box composable (5a-2) lands.
     */
    fun verticalScroll(): Modifier = copy(verticalScroll = true)

    /**
     * Compose-like `align(Alignment.CenterVertically)` etc.
     * Accepts Alignment.CENTER, START, END, FILL on the cross-axis.
     * Use `alignCrossAxis` to set the secondary alignment of a Row/Column child.
     */
    fun align(alignment: Int): Modifier = copy(alignmentCrossAxis = alignment)

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
        // Phase 5a additions
        if (bgColor >= 0) sb.append(",\"bgColor\":$bgColor")
        if (aspectRatio > 0f) sb.append(",\"aspectRatio\":$aspectRatio")
        if (verticalScroll) sb.append(",\"verticalScroll\":true")
        if (hWeight > 0f) sb.append(",\"hWeight\":$hWeight")
        if (vWeight > 0f) sb.append(",\"vWeight\":$vWeight")
        if (alignmentCrossAxis >= 0) sb.append(",\"alignmentCrossAxis\":$alignmentCrossAxis")
        return sb.toString()
    }
}