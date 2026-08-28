@file:Suppress("unused", "UNUSED_PARAMETER")

package org.librelab.gtk4kt

import org.librelab.gtk4kt.internal.GTKNative
import org.librelab.gtk4kt.runtime.Modifier
import java.io.File

/** Serializable description of a GTK widget (serialized to JSON for Rust UI builder). */
data class WidgetNode(
    val type: String,
    val id: String? = null,
    val label: String? = null,
    val children: List<WidgetNode>? = null,
    val onClick: Long? = null,
    val spacing: Int? = null,
    val orientation: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val margin: Int? = null,
    /**
     * Optional Modifier (Compose-style) for this widget. Stored as raw JSON
     * fragment to append during serialization. Set via `modifier` parameter
     * on the widget builders (Text, Button, etc.).
     */
    val modifierJson: String = "",
)

// ============================================================================
// Callback registry
// ============================================================================

private val callbackMap = mutableMapOf<Long, () -> Unit>()
private var nextCallbackId = 1L

private fun registerCallback(fn: () -> Unit): Long {
    val id = nextCallbackId++
    callbackMap[id] = fn
    System.err.println("[gtk4kt] callback registered: id=$id")
    return id
}

// ============================================================================
// Application DSL
// ============================================================================

private var pendingJsonTree: List<WidgetNode>? = null

fun application(appId: String, block: ApplicationScope.() -> Unit) {
    val scope = ApplicationScope().apply(block)

    // Write UI JSON to temp file
    val jsonTree = pendingJsonTree ?: emptyList()
    val json = buildJson(jsonTree, scope.windowTitle, scope.windowWidth, scope.windowHeight)
    val jsonFile = java.io.File.createTempFile("gtk4kt_ui_", ".json")
    jsonFile.writeText(json)
    System.err.println("[gtk4kt] UI JSON written to: ${jsonFile.absolutePath}")

    pendingJsonTree = null

    // Start GTK on a non-daemon thread (gtkMain runs in background) and
    // block the main thread so JVM stays alive.
    val gtkThread = Thread {
        GTKNative.gtkInit();
        // Register the invoker so Rust can call back into Kotlin's onClick handlers.
        GTKNative.gtkRegisterInvoker { handle, _ ->
            callbackMap[handle]?.invoke()
        };
        // Register UI JSON path first; gtkApplicationRun reads it.
        GTKNative.gtkSetUiJsonPath(jsonFile.absolutePath);
        // Build the UI synchronously from the registered path. Returns 0 on success.
        GTKNative.gtkApplicationRun(appPtr = 0, latchAddr = 0);
        System.err.println("[gtk4kt] gtkApplicationRun returned");

        // Optional: auto-fire all UI-built buttons (debug aid for headless envs
        // where X11 window isn't visible). Set GTK4KT_AUTO_FIRE=0 to disable.
        val autoFire = System.getenv("GTK4KT_AUTO_FIRE") ?: "1"
        if (autoFire == "1") {
            System.err.println("[gtk4kt] AUTO_FIRE: firing all UI-built buttons");
            GTKNative.gtkSignalTest();
        }

        // Drive the GTK main loop until quit.
        while (!Thread.currentThread().isInterrupted) {
            GTKNative.gtkMainIteration();
            Thread.sleep(10);
        }
    }.apply {
        name = "GTK-Main";
        isDaemon = false;
        start();
    }
    System.err.println("[gtk4kt] application {}: GTK thread started, blocking main")
    gtkThread.join()
}

class ApplicationScope {
    internal var windowTitle = "gtk4kt"
    internal var windowWidth = 800
    internal var windowHeight = 600
    internal var windowContent: List<WidgetNode>? = null

    fun window(
        title: String = "gtk4kt",
        width: Int = 800,
        height: Int = 600,
        block: WindowBuilder.() -> Unit
    ) {
        windowTitle = title
        windowWidth = width
        windowHeight = height
        val wb = WindowBuilder()
        wb.block()
        windowContent = wb.children
        pendingJsonTree = windowContent
    }
}

class WindowBuilder {
    internal val children = mutableListOf<WidgetNode>()

    fun column(spacing: Int = 0, modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder()
        b.block()
        children.add(
            WidgetNode("Box", orientation = 1, spacing = spacing, modifierJson = modifier.toJsonFields(),
                children = b.children.toList())
        )
    }

    fun row(spacing: Int = 0, modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder()
        b.block()
        children.add(
            WidgetNode("Box", orientation = 0, spacing = spacing, modifierJson = modifier.toJsonFields(),
                children = b.children.toList())
        )
    }
}

class BoxBuilder {
    internal val children = mutableListOf<WidgetNode>()

    /**
     * Display a line of text. Compose-like API.
     * `modifier` carries padding / sizing / alignment hints.
     */
    fun Text(text: String, modifier: Modifier = Modifier.Empty, id: String? = null) {
        children.add(
            WidgetNode("Label", label = text, id = id, modifierJson = modifier.toJsonFields())
        )
    }

    /**
     * Compose-like `Text` overload accepting a String template directly.
     * Allows `Text("Count: $count")` syntax from Compose.
     */
    fun text(text: String, modifier: Modifier = Modifier.Empty, id: String? = null) {
        Text(text, modifier, id)
    }

    /** Compose-like `Label` (alias of `Text`). */
    fun Label(text: String, modifier: Modifier = Modifier.Empty, id: String? = null) {
        Text(text, modifier, id)
    }

    /** Backward-compat: lowercase `label` alias of `Text`. */
    fun label(text: String, id: String? = null) {
        Text(text, Modifier.Empty, id)
    }

    /**
     * Compose-like Button. `onClick` may be null (still creates a clickable
     * button, just no Kotlin callback wired).
     */
    fun Button(
        label: String,
        modifier: Modifier = Modifier.Empty,
        onClick: (() -> Unit)? = null,
    ) {
        val handleId = onClick?.let { registerCallback(it) } ?: 0L
        children.add(
            WidgetNode("Button", label = label, modifierJson = modifier.toJsonFields(),
                onClick = handleId.takeIf { it != 0L })
        )
    }

    /** Lowercase alias matching Compose. */
    fun button(label: String, modifier: Modifier = Modifier.Empty, onClick: (() -> Unit)? = null) {
        Button(label, modifier, onClick)
    }

    /** OutlinedButton variant (Phase 4b) — for now identical to Button. */
    fun OutlinedButton(
        label: String,
        modifier: Modifier = Modifier.Empty,
        onClick: (() -> Unit)? = null,
    ) {
        val handleId = onClick?.let { registerCallback(it) } ?: 0L
        children.add(
            WidgetNode("OutlinedButton", label = label, modifierJson = modifier.toJsonFields(),
                onClick = handleId.takeIf { it != 0L })
        )
    }

    /**
     * Compose-like Spacer. A Spacer with no modifier does nothing;
     * with `Modifier.height(8.dp)` it adds vertical gap, with
     * `Modifier.width(8.dp)` it adds horizontal gap.
     *
     * Maps to a GTK Box of the requested orientation with a fixed child size.
     */
    fun Spacer(modifier: Modifier = Modifier.Empty) {
        children.add(WidgetNode("Spacer", modifierJson = modifier.toJsonFields()))
    }

    fun spacer(modifier: Modifier = Modifier.Empty) = Spacer(modifier)

    fun column(spacing: Int = 0, modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder()
        b.block()
        children.add(
            WidgetNode("Box", orientation = 1, spacing = spacing, modifierJson = modifier.toJsonFields(),
                children = b.children.toList())
        )
    }

    fun row(spacing: Int = 0, modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder()
        b.block()
        children.add(
            WidgetNode("Box", orientation = 0, spacing = spacing, modifierJson = modifier.toJsonFields(),
                children = b.children.toList())
        )
    }
}

// ============================================================================
// JSON serialization
// ============================================================================

private fun buildJson(nodes: List<WidgetNode>, title: String, width: Int, height: Int): String {
    val sb = StringBuilder()
    // Top-level: Window containing Box with all children
    sb.append("{\"type\":\"Window\",\"title\":\"${escJson(title)}\",\"width\":$width,\"height\":$height,\"children\":[")
    nodes.forEachIndexed { i, n ->
        if (i > 0) sb.append(",")
        appendNode(sb, n)
    }
    sb.append("]}")
    return sb.toString()
}

private fun appendNode(sb: StringBuilder, node: WidgetNode) {
    sb.append("{\"type\":\"${node.type}\"")
    node.id?.let { sb.append(",\"id\":\"$it\"") }
    node.label?.let { sb.append(",\"label\":\"${escJson(it)}\"") }
    node.onClick?.let { sb.append(",\"on_click_handle\":$it") }
    node.spacing?.let { sb.append(",\"spacing\":$it") }
    node.orientation?.let { sb.append(",\"orientation\":$it") }
    node.width?.let { sb.append(",\"width\":$it") }
    node.height?.let { sb.append(",\"height\":$it") }
    node.margin?.let { sb.append(",\"margin\":$it") }
    // Modifier fields (padding/sizing/alignment) — already formatted as JSON fields
    if (node.modifierJson.isNotEmpty()) sb.append(node.modifierJson)
    if (!node.children.isNullOrEmpty()) {
        sb.append(",\"children\":[")
        node.children.forEachIndexed { i, c ->
            if (i > 0) sb.append(",")
            appendNode(sb, c)
        }
        sb.append("]")
    }
    sb.append("}")
}

private fun escJson(s: String): String = s
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
    .replace("\t", "\\t")
