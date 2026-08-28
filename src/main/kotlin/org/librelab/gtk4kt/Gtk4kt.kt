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
    val title: String? = null,
    val children: List<WidgetNode>? = null,
    val onClick: Long? = null,
    val spacing: Int? = null,
    val orientation: Int? = null,
    val width: Int? = null,
    val height: Int? = null,
    val margin: Int? = null,
    // Phase 4c: control properties
    val active: Boolean? = null,
    val onChange: Long? = null,
    val min: Double? = null,
    val max: Double? = null,
    val value: Double? = null,
    // Phase 4d: icon / placeholder / text input
    val icon: String? = null,
    val placeholder: String? = null,
    val text: String? = null,
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

private val callbackMap = mutableMapOf<Long, (Long) -> Unit>()
private var nextCallbackId = 1L

private fun registerCallback(fn: () -> Unit): Long {
    val id = nextCallbackId++
    callbackMap[id] = { _ -> fn() }
    System.err.println("[gtk4kt] callback registered: id=$id")
    return id
}

/** Register a callback that receives the raw value from Rust (0/1 for switch, int for slider). */
private fun registerValueCallback(fn: (Long) -> Unit): Long {
    val id = nextCallbackId++
    callbackMap[id] = fn
    System.err.println("[gtk4kt] value callback registered: id=$id")
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
        GTKNative.gtkRegisterInvoker { handle, value ->
            callbackMap[handle]?.invoke(value)
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

// ============================================================================
// DropdownMenuBuilder — items for a Compose-like DropdownMenu
// ============================================================================

/** Holds (label, onClick) pairs collected inside a `DropdownMenu { ... }` block. */
class DropdownMenuBuilder {
    internal val items = mutableListOf<DropdownItem>()

    /** Compose-like `DropdownMenuItem` — a single menu entry. */
    fun DropdownMenuItem(label: String, onClick: (() -> Unit)? = null) {
        items.add(DropdownItem(label, onClick))
    }

    fun dropdownMenuItem(label: String, onClick: (() -> Unit)? = null) {
        DropdownMenuItem(label, onClick)
    }
}

internal data class DropdownItem(val label: String, val onClick: (() -> Unit)?) {
    fun toWidgetNode(): WidgetNode {
        return WidgetNode(type = "DropdownMenuItem", label = label)
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
     * Compose-like Card — a bordered container with elevation-like shadow.
     * `block` receives a BoxBuilder scope for its content.
     */
    fun Card(modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder()
        b.block()
        children.add(
            WidgetNode("Card", modifierJson = modifier.toJsonFields(), children = b.children.toList())
        )
    }

    fun card(modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) = Card(modifier, block)

    /**
     * Compose-like Surface — same as Card for now (Frame container).
     */
    fun Surface(modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder()
        b.block()
        children.add(
            WidgetNode("Surface", modifierJson = modifier.toJsonFields(), children = b.children.toList())
        )
    }

    fun surface(modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) = Surface(modifier, block)

    /**
     * Compose-like Divider — a horizontal line. Use orientation 0 for vertical.
     */
    fun Divider(modifier: Modifier = Modifier.Empty, vertical: Boolean = false) {
        children.add(
            WidgetNode("Divider", orientation = if (vertical) 0 else 1, modifierJson = modifier.toJsonFields())
        )
    }

    fun divider(modifier: Modifier = Modifier.Empty, vertical: Boolean = false) = Divider(modifier, vertical)

    /**
     * Compose-like Switch — boolean toggle. `onCheckedChange` receives the new state.
     */
    fun Switch(
        checked: Boolean = false,
        modifier: Modifier = Modifier.Empty,
        onCheckedChange: ((Boolean) -> Unit)? = null,
    ) {
        val handleId = onCheckedChange?.let { registerValueCallback { v -> onCheckedChange(v != 0L) } } ?: 0L
        children.add(
            WidgetNode("Switch", active = checked, modifierJson = modifier.toJsonFields(),
                onChange = handleId.takeIf { it != 0L })
        )
    }

    fun switch(
        checked: Boolean = false,
        modifier: Modifier = Modifier.Empty,
        onCheckedChange: ((Boolean) -> Unit)? = null,
    ) = Switch(checked, modifier, onCheckedChange)

    /**
     * Compose-like Slider — range input. `onValueChange` receives the new value.
     * min/max default to 0..100.
     */
    fun Slider(
        value: Float = 0f,
        modifier: Modifier = Modifier.Empty,
        onValueChange: ((Float) -> Unit)? = null,
        min: Float = 0f,
        max: Float = 100f,
    ) {
        val handleId = onValueChange?.let { registerValueCallback { v -> onValueChange(v.toFloat()) } } ?: 0L
        children.add(
            WidgetNode("Slider", modifierJson = modifier.toJsonFields(),
                onChange = handleId.takeIf { it != 0L },
                min = min.toDouble(), max = max.toDouble(), value = value.toDouble())
        )
    }

    fun slider(
        value: Float = 0f,
        modifier: Modifier = Modifier.Empty,
        onValueChange: ((Float) -> Unit)? = null,
        min: Float = 0f,
        max: Float = 100f,
    ) = Slider(value, modifier, onValueChange, min, max)

    /**
     * Compose-like Icon — a stock GTK icon (e.g. "go-previous").
     * Maps to gtk::Image::from_icon_name.
     */
    fun Icon(icon: String, modifier: Modifier = Modifier.Empty) {
        children.add(WidgetNode("Icon", icon = icon, modifierJson = modifier.toJsonFields()))
    }

    fun icon(icon: String, modifier: Modifier = Modifier.Empty) = Icon(icon, modifier)

    /**
     * Compose-like IconButton — a button with an icon (no label).
     * `onClick` fires the Kotlin callback.
     */
    fun IconButton(
        icon: String,
        modifier: Modifier = Modifier.Empty,
        onClick: (() -> Unit)? = null,
    ) {
        val handleId = onClick?.let { registerCallback(it) } ?: 0L
        children.add(
            WidgetNode("IconButton", icon = icon, modifierJson = modifier.toJsonFields(),
                onClick = handleId.takeIf { it != 0L })
        )
    }

    fun iconButton(
        icon: String,
        modifier: Modifier = Modifier.Empty,
        onClick: (() -> Unit)? = null,
    ) = IconButton(icon, modifier, onClick)

    /**
     * Compose-like OutlinedTextField — a single-line text entry with placeholder.
     * `onValueChange` fires whenever the user types.
     */
    fun OutlinedTextField(
        value: String = "",
        placeholder: String = "",
        modifier: Modifier = Modifier.Empty,
        onValueChange: ((String) -> Unit)? = null,
    ) {
        val handleId = onValueChange?.let { registerValueCallback { _ -> /* text retrieval in Phase 5 */ } } ?: 0L
        children.add(
            WidgetNode("OutlinedTextField", text = value, placeholder = placeholder,
                modifierJson = modifier.toJsonFields(),
                onChange = handleId.takeIf { it != 0L })
        )
    }

    fun outlinedTextField(
        value: String = "",
        placeholder: String = "",
        modifier: Modifier = Modifier.Empty,
        onValueChange: ((String) -> Unit)? = null,
    ) = OutlinedTextField(value, placeholder, modifier, onValueChange)

    fun TextField(
        value: String = "",
        placeholder: String = "",
        modifier: Modifier = Modifier.Empty,
        onValueChange: ((String) -> Unit)? = null,
    ) = OutlinedTextField(value, placeholder, modifier, onValueChange)

    /**
     * Compose-like DropdownMenu — a button that pops up a list of items.
     * Use the `DropdownMenuItem { label; onClick }` builder DSL inside the block.
     */
    fun DropdownMenu(
        label: String,
        modifier: Modifier = Modifier.Empty,
        block: DropdownMenuBuilder.() -> Unit,
    ) {
        val mb = DropdownMenuBuilder()
        mb.block()
        children.add(
            WidgetNode("DropdownMenu", label = label, modifierJson = modifier.toJsonFields(),
                children = mb.items.map { it.toWidgetNode() })
        )
    }

    fun dropdownMenu(
        label: String,
        modifier: Modifier = Modifier.Empty,
        block: DropdownMenuBuilder.() -> Unit,
    ) = DropdownMenu(label, modifier, block)

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
// DropdownMenuBuilder — items for a Compose-like DropdownMenu
// ============================================================================

// ============================================================================
// Scaffold — Compose-like top-level Scaffold/TopAppBar (replaces Window)
// ============================================================================

/**
 * Compose-like `Scaffold { topBar = { ... }; body = { ... } }`.
 *
 * Provides a top-bar (optional) plus a body builder. Children of topBar become
 * a gtk::HeaderBar; children of body go into the main content area.
 *
 * Usage:
 * ```
 * Scaffold(title = "Settings", width = 400, height = 600) {
 *     topBar {
 {
 *         IconButton("go-previous") { onBack() }
 {
 *     }
 *     body {
 {
 *         Column { Text("Hello") }
 {
 *     }
 {
 * }
 *
 * @param block receives a ScaffoldScope with topBar() and body() helpers.
 */
fun Scaffold(
    title: String = "gtk4kt",
    width: Int = 800,
    height: Int = 600,
    block: ScaffoldScope.() -> Unit,
) {
    val scope = ScaffoldScope().apply(block)
    // Build the JSON tree:
    //   Scaffold { TopBar { ... } , [body widget] }
    val children = mutableListOf<WidgetNode>()
    if (scope.topBarChildren.isNotEmpty()) {
        children.add(WidgetNode("TopAppBar", title = title, children = scope.topBarChildren))
    }
    // Body becomes a single Column wrapping all body children (use a Box).
    val bodyNode = WidgetNode(
        type = "Box",
        orientation = 1,
        spacing = 0,
        children = scope.bodyChildren,
    )
    children.add(bodyNode)

    val winNode = WidgetNode(
        type = "Scaffold",
        title = title,
        width = width,
        height = height,
        children = children,
    )

    // Push through the existing application{} pipeline.
    pendingJsonTree = listOf(winNode)
}

class ScaffoldScope {
    internal val topBarChildren = mutableListOf<WidgetNode>()
    internal val bodyChildren = mutableListOf<WidgetNode>()

    /** Compose-like `ScaffoldScope.topBar { ... }` — HeaderBar contents. */
    fun topBar(block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder()
        b.block()
        topBarChildren.addAll(b.children)
    }

    /** Compose-like `ScaffoldScope.body { ... }` — main content. */
    fun body(modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder()
        b.block()
        // Modifier is stored on the body box's modifierJson — applied by Rust apply_modifier.
        bodyChildren.addAll(b.children.map { it })
    }
}

// ============================================================================
// Icons namespace — Compose-like material icons → GTK stock icon names
// ============================================================================

/**
 * Compose-like `Icons.Outlined.<name>` namespace, mapped to GTK stock icon names.
 * Most Android Compose material icons have a 1:1 GTK equivalent in the standard
 * icon theme; if a name is missing, GTK falls back to "image-missing".
 */
object Icons {
    object Outlined {
        val ArrowBack = "go-previous"
        val FolderOpen = "folder-open"
        val Settings = "preferences-system"
        val Search = "system-search"
        val Add = "list-add"
        val Delete = "edit-delete"
        val Close = "window-close"
        val Check = "object-select"
        val Info = "dialog-information"
        val Warning = "dialog-warning"
        val Home = "go-home"
        val Refresh = "view-refresh"
        val Play = "media-playback-start"
        val Pause = "media-playback-pause"
        val Stop = "media-playback-stop"
    }
}

// ============================================================================
// JSON serialization
// ============================================================================

private fun buildJson(nodes: List<WidgetNode>, title: String, width: Int, height: Int): String {
    val sb = StringBuilder()
    // If the root is a Scaffold, use it directly (it carries its own title/width/height).
    // Otherwise wrap in a Window (Compose-style application{ window{...} }).
    if (nodes.size == 1 && nodes[0].type == "Scaffold") {
        appendNode(sb, nodes[0])
        return sb.toString()
    }
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
    // Phase 4d: control properties
    node.active?.let { sb.append(",\"active\":$it") }
    node.onChange?.let { sb.append(",\"on_change_handle\":$it") }
    node.min?.let { sb.append(",\"min\":$it") }
    node.max?.let { sb.append(",\"max\":$it") }
    node.value?.let { sb.append(",\"value\":$it") }
    // Phase 4d: icon / placeholder / text
    node.icon?.let { sb.append(",\"icon\":\"${escJson(it)}\"") }
    node.placeholder?.let { sb.append(",\"placeholder\":\"${escJson(it)}\"") }
    node.text?.let { sb.append(",\"text\":\"${escJson(it)}\"") }
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
