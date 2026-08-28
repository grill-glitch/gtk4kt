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
    val tag: String? = null,
    val description: String? = null,
    val collapsed: Boolean? = null,
    val maxSidebarWidth: Double? = null,
    val columns: Int? = null,
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

// Captured by application{} on first run; used by the Recomposer to
// regenerate JSON without re-entering application{} (which would block).
@Volatile private var firstJsonPath: String = ""
@Volatile private var firstWindowTitle: String = "gtk4kt"
@Volatile private var firstWindowWidth: Int = 800
@Volatile private var firstWindowHeight: Int = 600

// The most recent widget tree used to build the JSON. The Recomposer
// re-uses this tree (it can't re-evaluate the user's DSL, only replay the
// captured structure).
@Volatile private var lastCommittedTree: List<WidgetNode>? = null

/**
 * Phase 6-5: rebuild the UI from the last committed tree.
 * Called by the Recomposer thread when state changes.
 */
fun rebuildUi() {
    val tree = lastCommittedTree ?: return
    val json = buildJson(tree, firstWindowTitle, firstWindowWidth, firstWindowHeight)
    val f = java.io.File(firstJsonPath)
    if (f.parentFile == null) return
    f.writeText(json)
    GTKNative.gtkSetUiJsonPath(firstJsonPath)
    GTKNative.gtkRebuildUi()
    System.err.println("[gtk4kt] rebuildUi: epoch=" + Recomposer.epoch)
}

fun application(appId: String, block: ApplicationScope.() -> Unit) {
    val scope = ApplicationScope().apply(block)
    ensureRecomposer()  // start background rebuild thread

    // Write UI JSON to temp file
    val jsonTree = pendingJsonTree ?: emptyList()
    val json = buildJson(jsonTree, scope.windowTitle, scope.windowWidth, scope.windowHeight)
    val jsonFile = java.io.File.createTempFile("gtk4kt_ui_", ".json")
    jsonFile.writeText(json)
    System.err.println("[gtk4kt] UI JSON written to: ${jsonFile.absolutePath}")

    pendingJsonTree = null
    lastCommittedTree = jsonTree
    firstJsonPath = jsonFile.absolutePath
    firstWindowTitle = scope.windowTitle
    firstWindowWidth = scope.windowWidth
    firstWindowHeight = scope.windowHeight

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

    /**
     * Compose-like `Box` — a container that overlays children (stack-style).
     * Maps to gtk::Box (Vertical) with all children sharing the same area via
     * each child's `halign=fill, valign=fill` modifier; children stack with
     * later ones on top.
     *
     * Use case: layering overlays, badges, etc.
     */
    fun Box(modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder()
        b.block()
        // Force each child to fill the Box; later children overlay earlier ones.
        val stacked = b.children.map { child ->
            child.copy(modifierJson = child.modifierJson + ",\"halign\":3,\"valign\":3")
        }
        children.add(
            WidgetNode("Box", orientation = 1, spacing = 0, modifierJson = modifier.toJsonFields(),
                children = stacked)
        )
    }

    fun box(modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) = Box(modifier, block)
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
     * Compose-like `Card { ... }` with onClick — clickable card. Maps to a
     * flat-styled gtk::Button so it looks like a Card, not a regular Button.
     */
    fun Card(
        onClick: () -> Unit,
        modifier: Modifier = Modifier.Empty,
        block: BoxBuilder.() -> Unit,
    ) {
        val handleId = registerCallback(onClick)
        val b = BoxBuilder()
        b.block()
        children.add(
            WidgetNode("ClickableCard", modifierJson = modifier.toJsonFields(),
                onClick = handleId.takeIf { it != 0L },
                children = b.children.toList())
        )
    }

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
     * Compose-like `Image` — show an image from a path or stock icon.
     * Falls back to the placeholder icon if path is null.
     */
    fun Image(
        path: String? = null,
        fallbackIcon: String = "image-missing",
        modifier: Modifier = Modifier.Empty,
    ) {
        children.add(WidgetNode("Image", icon = path ?: fallbackIcon, modifierJson = modifier.toJsonFields()))
    }

    fun image(
        path: String? = null,
        fallbackIcon: String = "image-missing",
        modifier: Modifier = Modifier.Empty,
    ) = Image(path, fallbackIcon, modifier)

    /**
     * Compose-like `CircularProgressIndicator` — animated spinner.
     * Phase 5a: rendered as a gtk::Spinner (the closest GTK analog).
     */
    fun CircularProgressIndicator(modifier: Modifier = Modifier.Empty) {
        children.add(WidgetNode("Spinner", modifierJson = modifier.toJsonFields()))
    }

    fun circularProgressIndicator(modifier: Modifier = Modifier.Empty) = CircularProgressIndicator(modifier)

    @Deprecated(message = "ElevatedCard is Material 3 mobile design. Use ListBox rows in PreferencesGroup or simple Card. Phase 7 desktop-first.")
    /**
     * Compose-like `ElevatedCard` — Card with stronger shadow.
     * For Phase 5a, identical to Card; future phases will add elevation classes.
     */
    fun ElevatedCard(modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder()
        b.block()
        children.add(
            WidgetNode("ElevatedCard", modifierJson = modifier.toJsonFields(),
                children = b.children.toList())
        )
    }

    fun elevatedCard(modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) =
        ElevatedCard(modifier, block)

    /**
     * Compose-like `ElevatedCard { ... }` with onClick — clickable variant.
     */
    fun ElevatedCard(
        onClick: () -> Unit,
        modifier: Modifier = Modifier.Empty,
        block: BoxBuilder.() -> Unit,
    ) {
        val handleId = registerCallback(onClick)
        val b = BoxBuilder()
        b.block()
        children.add(
            WidgetNode("ClickableElevatedCard", modifierJson = modifier.toJsonFields(),
                onClick = handleId.takeIf { it != 0L },
                children = b.children.toList())
        )
    }

    /**
     * Compose-like `TextButton` — minimal-styling button (no padding around label).
     * For Phase 5a, identical to Button; future phases will style.
     */
    fun TextButton(
        label: String,
        modifier: Modifier = Modifier.Empty,
        onClick: (() -> Unit)? = null,
    ) {
        val handleId = onClick?.let { registerCallback(it) } ?: 0L
        children.add(
            WidgetNode("TextButton", label = label, modifierJson = modifier.toJsonFields(),
                onClick = handleId.takeIf { it != 0L })
        )
    }

    fun textButton(
        label: String,
        modifier: Modifier = Modifier.Empty,
        onClick: (() -> Unit)? = null,
    ) = TextButton(label, modifier, onClick)

    @Deprecated(message = "FloatingActionButton is Material 3 mobile design. Use HeaderBar primary action instead (Phase 7).")
    /**
     * Compose-like `FloatingActionButton` — circular button typically for
     * primary actions. Phase 5a: rendered as a styled Button (icon or label).
     */
    // ─── Phase 7 desktop-first widgets ─────────────────────────────────────

    /** Compose-like NavigationSplitView. child[0] = sidebar slot, child[1] = content slot. */
    fun NavigationSplitView(
        modifier: Modifier = Modifier.Empty,
        maxSidebarWidth: Double = 240.0,
        collapsed: Boolean = false,
        sidebar: BoxBuilder.() -> Unit,
        content: BoxBuilder.() -> Unit,
    ) {
        val sb = BoxBuilder(); sb.sidebar()
        val cb = BoxBuilder(); cb.content()
        children.add(
            WidgetNode(
                type = "NavigationSplitView",
                modifierJson = modifier.toJsonFields(),
                maxSidebarWidth = maxSidebarWidth,
                collapsed = collapsed,
                children = listOf(
                    WidgetNode(type = "Sidebar", children = sb.children.toList()),
                    // Wrap content in a Stack so it can switch pages.
                    WidgetNode(type = "Box", orientation = 1, spacing = 0, children = cb.children.toList()),
                ),
            ),
        )
    }

    /** Sidebar (within NavigationSplitView sidebar slot). */
    fun Sidebar(modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder(); b.block()
        children.add(WidgetNode(type = "Sidebar", modifierJson = modifier.toJsonFields(), children = b.children.toList()))
    }

    /** NavigationPage — sidebar list item. */
    fun NavigationPage(
        title: String,
        icon: String,
        tag: String,
        onClick: () -> Unit,
    ) {
        val handleId = registerCallback(onClick)
        children.add(
            WidgetNode(
                type = "NavigationPage",
                title = title,
                icon = icon,
                tag = tag,
                onClick = handleId.takeIf { it != 0L },
            ),
        )
    }

    /** PreferencesPage — flat group layout for settings. */
    fun PreferencesPage(modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder(); b.block()
        children.add(WidgetNode(type = "PreferencesPage", modifierJson = modifier.toJsonFields(), children = b.children.toList()))
    }

    /** PreferencesGroup — labeled section within a PreferencesPage. */
    fun PreferencesGroup(
        title: String? = null,
        description: String? = null,
        block: BoxBuilder.() -> Unit,
    ) {
        val b = BoxBuilder(); b.block()
        children.add(
            WidgetNode(
                type = "PreferencesGroup",
                title = title,
                description = description,
                children = b.children.toList(),
            ),
        )
    }

    /** ActionRow — title + subtitle + suffix control. */
    fun ActionRow(
        title: String? = null,
        description: String? = null,
        icon: String? = null,
        block: BoxBuilder.() -> Unit,
    ) {
        val b = BoxBuilder(); b.block()
        children.add(
            WidgetNode(
                type = "ActionRow",
                title = title,
                description = description,
                icon = icon,
                children = b.children.toList(),
            ),
        )
    }

    /** ListBox — flat list container. */
    fun ListBox(modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder(); b.block()
        children.add(WidgetNode(type = "ListBox", modifierJson = modifier.toJsonFields(), children = b.children.toList()))
    }

    /** ListBoxRow — single row in a ListBox. Optional onClick makes it activatable. */
    fun ListBoxRow(
        onClick: () -> Unit = {},
        modifier: Modifier = Modifier.Empty,
        block: BoxBuilder.() -> Unit,
    ) {
        val handleId = registerCallback(onClick)
        val b = BoxBuilder(); b.block()
        children.add(
            WidgetNode(
                type = "ListBoxRow",
                modifierJson = modifier.toJsonFields(),
                onClick = handleId.takeIf { it != 0L },
                children = b.children.toList(),
            ),
        )
    }

    /** GridView — multi-column flow layout (uses GtkFlowBox internally). */
    fun GridView(columns: Int = 2, modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder(); b.block()
        children.add(
            WidgetNode(
                type = "GridView",
                columns = columns,
                modifierJson = modifier.toJsonFields(),
                children = b.children.toList(),
            ),
        )
    }

    /** StatusPage — empty-state widget (icon + title + description + optional action). */
    fun StatusPage(
        icon: String? = null,
        title: String? = null,
        description: String? = null,
        modifier: Modifier = Modifier.Empty,
        action: (BoxBuilder.() -> Unit)? = null,
    ) {
        val b = BoxBuilder()
        if (action != null) b.action()
        children.add(
            WidgetNode(
                type = "StatusPage",
                icon = icon,
                title = title,
                description = description,
                modifierJson = modifier.toJsonFields(),
                children = b.children.toList(),
            ),
        )
    }

    /** Toast — non-blocking notification (emulated via GtkInfoBar in gtk3). */
    fun Toast(message: String, timeoutMs: Int = 3000, modifier: Modifier = Modifier.Empty) {
        children.add(
            WidgetNode(
                type = "Toast",
                title = message,
                value = timeoutMs.toDouble(),
                modifierJson = modifier.toJsonFields(),
            ),
        )
    }

    /** HeaderBar — desktop top toolbar with title + actions. */
    fun HeaderBar(title: String, modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder(); b.block()
        children.add(
            WidgetNode(
                type = "HeaderBar",
                title = title,
                modifierJson = modifier.toJsonFields(),
                children = b.children.toList(),
            ),
        )
    }

        fun FloatingActionButton(
        content: String,
        modifier: Modifier = Modifier.Empty,
        onClick: (() -> Unit)? = null,
    ) {
        val handleId = onClick?.let { registerCallback(it) } ?: 0L
        children.add(
            WidgetNode("FloatingActionButton", label = content, modifierJson = modifier.toJsonFields(),
                onClick = handleId.takeIf { it != 0L })
        )
    }

    fun floatingActionButton(
        content: String,
        modifier: Modifier = Modifier.Empty,
        onClick: (() -> Unit)? = null,
    ) = FloatingActionButton(content, modifier, onClick)

    /**
     * Compose-like `LazyColumn { items(list) { item -> ... } }` — a scrollable
     * column with virtualized children. For Phase 5a, renders the first 50
     * children eagerly (no virtualization yet — Phase 5b will use GtkListBox).
     *
     * `items` is a helper inside the LazyListScope that takes a List<T> and a
     * lambda to render each item.
     */
    fun LazyColumn(
        modifier: Modifier = Modifier.Empty,
        block: LazyListScope.() -> Unit,
    ) {
        val scope = LazyListScope()
        scope.block()
        // Flatten to Box children. Real virtualization deferred to Phase 5b.
        val rendered = scope.items.map { it.render() }
        children.add(
            WidgetNode("Box", orientation = 1, spacing = 4, modifierJson = modifier.toJsonFields(),
                children = rendered)
        )
    }

    fun lazyColumn(
        modifier: Modifier = Modifier.Empty,
        block: LazyListScope.() -> Unit,
    ) = LazyColumn(modifier, block)

    /**
     * Compose-like `LazyRow` — same as LazyColumn but horizontal.
     */
    fun LazyRow(
        modifier: Modifier = Modifier.Empty,
        block: LazyListScope.() -> Unit,
    ) {
        val scope = LazyListScope()
        scope.block()
        val rendered = scope.items.map { it.render() }
        children.add(
            WidgetNode("Box", orientation = 0, spacing = 4, modifierJson = modifier.toJsonFields(),
                children = rendered)
        )
    }

    fun lazyRow(
        modifier: Modifier = Modifier.Empty,
        block: LazyListScope.() -> Unit,
    ) = LazyRow(modifier, block)

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

    /**
     * Compose-like `Box` — overlay-style container. Inside BoxBuilder scope,
     * also available for nested overlays.
     */
    fun Box(modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) {
        val b = BoxBuilder()
        b.block()
        val stacked = b.children.map { child ->
            child.copy(modifierJson = child.modifierJson + ",\"halign\":3,\"valign\":3")
        }
        children.add(
            WidgetNode("Box", orientation = 1, spacing = 0, modifierJson = modifier.toJsonFields(),
                children = stacked)
        )
    }

    fun box(modifier: Modifier = Modifier.Empty, block: BoxBuilder.() -> Unit) = Box(modifier, block)
}

// ============================================================================
// DropdownMenuBuilder — items for a Compose-like DropdownMenu
// ============================================================================

// ============================================================================
// LazyListScope — Compose-like items DSL
// ============================================================================

/** Collects deferred-rendered items for LazyColumn/LazyRow. */
class LazyListScope {
    internal val items = mutableListOf<LazyItem>()

    /** Compose-like `items(list, key) { item -> ... }` — renders one widget per item. */
    fun <T> items(list: List<T>, block: BoxBuilder.(T) -> Unit) {
        for (item in list) {
            val b = BoxBuilder()
            b.block(item)
            items.add(LazyItem(b.children))
        }
    }

    /** Single-item variant. */
    fun <T> item(value: T, block: BoxBuilder.(T) -> Unit) {
        items(listOf(value), block)
    }
}

internal data class LazyItem(val children: List<WidgetNode>) {
    fun render(): WidgetNode {
        // Wrap children in a Box so the lazy list acts as a single child in its parent.
        return WidgetNode(type = "Box", orientation = 1, spacing = 0, children = children)
    }
}

// ============================================================================
// State primitives (Phase 6-4) — Compose-like remember/mutableStateOf
// ============================================================================
//
// gtk4kt UI is mostly static (one-shot JSON tree). For interactive patterns
// like dropdown menus, we need a tiny state store that:
//  - is keyed by call-site (Compose's `remember { ... }` lifecycle)
//  - propagates changes back to Rust (so the JSON can be rebuilt)
//
// Phase 6-4 implementation:
//  - State holds current value + listener callbacks
//  - setValue() invokes listeners (registered by build-time invocations)
//  - rebuilds are NOT automatic; callers must call `setValue` then trigger
//    a refresh (Phase 6-5 wires this to gtk_main_iteration tick).
//
// This is intentionally minimal — Phase 6-5 will move to proper reactive
// primitives when the integration cost is justified.

class State<T>(initial: T) {
    @Volatile
    private var current: T = initial
    private val listeners = mutableListOf<(T) -> Unit>()

    val value: T get() = current

    fun setValue(new: T) {
        current = new
        // Mark Recomposer dirty so the background thread re-renders the tree.
        Recomposer.markDirty()
        listeners.toList().forEach { it(new) }
    }

    internal fun addListener(l: (T) -> Unit) {
        listeners.add(l)
    }

    internal fun removeListener(l: (T) -> Unit) {
        listeners.remove(l)
    }

    companion object {
        // Global state store: keyed by call-site identity. Garbage-collected
        // when no widget holds a reference. Phase 6-4: simple HashMap; Phase
        // 6-5 will move to scoped keys tied to widget lifecycle.
        private val store = mutableMapOf<String, State<*>>()

        @Suppress("UNCHECKED_CAST")
        fun <T> get(key: String, initial: T): State<T> {
            val existing = store[key]
            if (existing != null) return existing as State<T>
            val created = State(initial)
            store[key] = created
            return created
        }
    }
}

/**
 * Compose-like `remember(key) { ... }` — store/retrieve a state value across
 * recompositions. Key should be unique per call site; the framework doesn't
 * have a Recomposer yet, but the state survives so callbacks can mutate it.
 */
fun <T> remember(key: String, initial: T): State<T> = State.get(key, initial)

/**
 * Compose-like `mutableStateOf(initial)` — returns a `State<T>` with property
 * delegate support. Usage:
 *   var menuOpen by remember("menuOpen") { mutableStateOf(false) }
 *   // ...
 *   if (menuOpen) DropdownMenu(...) { ... }
 *
 * Phase 6-4: missing the recomposition trigger (Phase 6-5).
 */
fun <T> mutableStateOf(initial: T): State<T> = State.get("__inline_${initial.hashCode()}_${System.nanoTime()}", initial)

// ============================================================================
// Phase 6-5: Reactive rebuild — the Recomposer
// ============================================================================
//
// gtk4kt's fundamental limitation up to Phase 6-4 was that state mutations
// had no effect on the rendered widget tree — the JSON was built once.
// This Phase adds the minimal "Recomposer": a global state-changed flag that
// triggers a fresh JSON build + a Rust-side widget tree refresh.
//
// Mechanism:
//  1. State.setValue() sets a global "dirty" flag (atomic).
//  2. A dedicated background thread polls the flag and re-runs
//     `application{}` to produce a fresh JSON.
//  3. The new JSON is sent to Rust via gtk_bridge_set_ui_json_path
//     + gtk_bridge_application_run to rebuild the widget tree.
//  4. Each iteration increments a global `rebuildEpoch` for debugging.
//
// This is intentionally naive (rebuild the whole tree). Phase 6-6 will
// switch to incremental updates via widget IDs if perf becomes an issue.

object Recomposer {
    @Volatile
    var dirty: Boolean = false
        private set

    @Volatile
    var epoch: Long = 0L
        private set

    private val dirtyLock = Any()

    /** Called by State.setValue when state changes. */
    internal fun markDirty() {
        synchronized(dirtyLock) { dirty = true }
    }

    /** Called by the background thread before each rebuild. */
    internal fun consumeDirty(): Boolean = synchronized(dirtyLock) {
        if (dirty) { dirty = false; true } else false
    }

    /** Bump epoch (for debug / verification). */
    internal fun bumpEpoch() { epoch++ }
}

/**
 * Background thread that polls Recomposer.dirty and triggers rebuilds.
 * Started once by the first application{} invocation.
 */
private val recomposerThread: Thread by lazy {
    Thread({
        while (true) {
            if (Recomposer.consumeDirty()) {
                // Trigger rebuild by re-running pendingJsonTree → JSON → Rust.
                // We delegate to the same code path application{} uses; we
                // can't re-call application{} (it would block). Instead, we
                // call a helper that recomputes JSON from pendingJsonTree and
                // re-runs gtk_bridge_application_run.
                try {
                    rebuildUi()
                    Recomposer.bumpEpoch()
                } catch (e: Throwable) {
                    System.err.println("[gtk4kt] Recomposer: rebuild failed: ${'$'}{e.message}")
                }
            }
            Thread.sleep(50)
        }
    }, "gtk4kt-Recomposer").apply {
        isDaemon = true
        start()
    }
}

/** Trigger initial spin-up of the recomposer. Called by application{}. */
internal fun ensureRecomposer() { recomposerThread }

// ============================================================================
// AlertDialog / MessageDialog — Compose-like Material3 AlertDialog +
// HIG-compliant GTK dialog pattern (see docs/gnome-dev-docs/hig/patterns/feedback/dialogs.md)
// ============================================================================

/**
 * Compose-like `AlertDialog` — a modal confirmation dialog.
 *  - `onDismissRequest` fires when user dismisses (Escape, outside-click, etc.)
 *  - `confirmButton` / `dismissButton` are the action buttons.
 *
 * HIG says: "Label the affirmative button with a specific imperative verb"
 * (e.g. "Save", "Delete") — generic "OK" is discouraged.
 */
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: DialogButton,
    modifier: Modifier = Modifier.Empty,
    title: String? = null,
    text: String? = null,
    dismissButton: DialogButton? = null,
) {
    val body = BoxBuilder().apply {
        if (text != null) {
            Text(text, modifier = Modifier.Empty.padding(8, 16))
        }
    }
    val children = buildList {
        title?.let { add(WidgetNode("Label", label = it, modifierJson = ",\"halign\":1,\"weight\":3")) }
        if (text != null) add(WidgetNode("Label", label = text, modifierJson = ",\"halign\":1"))
        // Buttons row
        val buttonRow = WidgetNode(
            type = "Box", orientation = 0, spacing = 8,
            children = buildList {
                add(WidgetNode("Button", label = confirmButton.label,
                    onClick = confirmButton.handleId.takeIf { it != 0L }))
                dismissButton?.let { add(WidgetNode("Button", label = it.label,
                    onClick = it.handleId.takeIf { it != 0L })) }
            },
            modifierJson = ",\"halign\":2,\"paddingEnd\":16,\"paddingTop\":8,\"paddingBottom\":16",
        )
        add(buttonRow)
    }
    val dialog = WidgetNode(
        type = "AlertDialog", title = title, modifierJson = modifier.toJsonFields(),
        children = children,
    )
    pendingJsonTree = listOf(dialog)
}

data class DialogButton(val label: String, val onClick: () -> Unit) {
    internal val handleId: Long = registerCallback(onClick)
}

// Dialog buttons share the same callbackMap — no separate registry needed.

/**
 * HIG-compliant `MessageDialog` (gtk_message_dialog_new):
 *   - 1-3 buttons: alert dialogs have between one and three buttons
 *   - Destructive actions get a confirmation dialog
 *
 * Phase 5a: same shape as AlertDialog but explicitly HIG-named.
 */
fun MessageDialog(
    title: String,
    message: String,
    confirmButton: DialogButton,
    modifier: Modifier = Modifier.Empty,
    dismissButton: DialogButton? = null,
) {
    AlertDialog(
        onDismissRequest = { dismissButton?.onClick?.invoke() },
        confirmButton = confirmButton,
        modifier = modifier,
        title = title,
        text = message,
        dismissButton = dismissButton,
    )
}

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
        val History = "document-open-recent"
        val Storage = "drive-harddisk"
        val Games = "applications-games"
        val Cloud = "weather-clouds"
        val MoreVert = "view-more"
        val PlayArrow = "media-playback-start"
        val RestartAlt = "view-refresh"
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
