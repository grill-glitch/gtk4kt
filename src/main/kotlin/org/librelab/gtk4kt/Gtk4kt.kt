@file:Suppress("unused", "UNUSED_PARAMETER")

package org.librelab.gtk4kt

import org.librelab.gtk4kt.internal.GTKNative
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
    GTKNative.gtkInit()
    val app = GTKNative.gtkApplicationNew(appId)
    require(app != GTKNative.NULL_PTR) { "failed to create GTK application" }

    val scope = ApplicationScope(app).apply(block)

    // Write UI JSON to temp file (Rust activate handler reads it after startup signal)
    val jsonTree = pendingJsonTree ?: emptyList()
    val json = buildJson(jsonTree, scope.windowTitle, scope.windowWidth, scope.windowHeight)
    val jsonFile = java.io.File.createTempFile("gtk4kt_ui_", ".json")
    jsonFile.writeText(json)
    System.err.println("[gtk4kt] UI JSON written to: ${jsonFile.absolutePath}")

    // Register JSON path with Rust (activate handler reads it)
    GTKNative.gtkSetUiJsonPath(jsonFile.absolutePath)

    pendingJsonTree = null

    // Start GTK — blocks on the JVM main thread (GTK requires gtk_main() on main thread)
    GTKNative.gtkApplicationRun(app)
    System.err.println("[gtk4kt] application {}: GTK exited, JVM continues")
}

class ApplicationScope(private val appPtr: Long) {
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

    fun label(text: String) {
        children.add(WidgetNode("Label", label = text))
    }

    fun button(label: String, onClick: (() -> Unit)? = null) {
        val handleId = onClick?.let { registerCallback(it) } ?: 0L
        children.add(WidgetNode("Button", label = label, onClick = handleId.takeIf { it != 0L }))
    }

    fun column(spacing: Int = 0, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder()
        b.block()
        children.add(WidgetNode("Box", orientation = 1, spacing = spacing, children = b.children.toList()))
    }

    fun row(spacing: Int = 0, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder()
        b.block()
        children.add(WidgetNode("Box", orientation = 0, spacing = spacing, children = b.children.toList()))
    }
}

class BoxBuilder {
    internal val children = mutableListOf<WidgetNode>()

    fun label(text: String) {
        children.add(WidgetNode("Label", label = text))
    }

    fun button(label: String, onClick: (() -> Unit)? = null) {
        val handleId = onClick?.let { registerCallback(it) } ?: 0L
        children.add(WidgetNode("Button", label = label, onClick = handleId.takeIf { it != 0L }))
    }

    fun column(spacing: Int = 0, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder()
        b.block()
        children.add(WidgetNode("Box", orientation = 1, spacing = spacing, children = b.children.toList()))
    }

    fun row(spacing: Int = 0, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder()
        b.block()
        children.add(WidgetNode("Box", orientation = 0, spacing = spacing, children = b.children.toList()))
    }
}

// ============================================================================
// JSON serialization
// ============================================================================

private fun buildJson(nodes: List<WidgetNode>, title: String, width: Int, height: Int): String {
    val sb = StringBuilder()
    // Top-level: Window containing Box with all children
    sb.append("{\"type\":\"Window\",\"label\":\"${escJson(title)}\",\"width\":$width,\"height\":$height,\"children\":[")
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
    node.onClick?.let { sb.append(",\"on_click\":$it") }
    node.spacing?.let { sb.append(",\"spacing\":$it") }
    node.orientation?.let { sb.append(",\"orientation\":$it") }
    node.width?.let { sb.append(",\"width\":$it") }
    node.height?.let { sb.append(",\"height\":$it") }
    node.margin?.let { sb.append(",\"margin\":$it") }
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
