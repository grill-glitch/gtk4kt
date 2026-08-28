package org.librelab.gtk4kt.examples

import org.librelab.gtk4kt.*
import org.librelab.gtk4kt.runtime.Modifier
import org.librelab.gtk4kt.runtime.Design

/**
 * Phase 8 — Modern desktop UI of ika.
 *
 * Architecture (3-pane, responsive):
 *   AppShell (no visible chrome)
 *     ├── Sidebar     [240px on wide, hidden on narrow]
 *     ├── ContentStack (each page has its own HeaderBar)
 *     └── ToastOverlay
 *
 * Design tokens are in `Design` (Kotlin) and mirrored in
 * `DEFAULT_THEME_CSS` (Rust). Components don't hardcode colors.
 *
 * NO FloatingActionButton. NO single giant centered card. NO emoji icons.
 * NO random padding — all spacing comes from `Design.s1..s12`.
 */
fun main() {
    application("org.gtk4kt.ika.library") {
        LibraryScreen()
    }
}

/** Top-level navigation pages. Mirrors ika's sidebar entries. */
enum class NavPage(val title: String, val icon: String) {
    Library("Library", "applications-games"),
    Recent("Recent", "document-open-recent"),
    Settings("Settings", "preferences-system"),
    About("About", "dialog-information"),
}

/** Game data class (Phase 6 placeholder). */
data class Game(
    val name: String,
    val typeLabel: String,
    val lastPlayed: String,
    val accent: String, // bg color of the thumbnail — provides identity
)

fun LibraryScreen() {
    val activeNav = remember("navPage", NavPage.Settings)
    val games = remember("games", SAMPLE_GAMES)
    val settingsRoot = remember("settingsRoot", "/home/user/games")
    val settingsAutoScan = remember("settingsAutoScan", true)
    val settingsShowRecent = remember("settingsShowRecent", true)
    val selectedGame = remember("selectedGame", SAMPLE_GAMES.first())

    Scaffold(title = "ika", width = 1280, height = 800) {
        body {
            // AppShell = a Box containing Sidebar + ContentStack. We
            // implement as a flat HBox with no panel chrome.
            NavigationSplitView(
                maxSidebarWidth = Design.sidebarWidth.toDouble(),
                sidebar = {
                    Sidebar(Modifier.Empty) {
                        // Sidebar header (small caps "ika")
                        Text("ika", modifier = Modifier.Empty.classes(Design.CLS_SIDEBAR_HEADER))

                        // Nav items
                        for (page in NavPage.entries) {
                            valNavItem(
                                icon = page.icon,
                                title = page.title,
                                selected = activeNav.value == page,
                                onClick = { activeNav.setValue(page) },
                            )
                        }
                    }
                },
                content = {
                    ContentStack(
                        active = activeNav.value,
                        games = games.value,
                        selectedGame = selectedGame.value,
                        onSelectGame = { selectedGame.setValue(it) },
                        settingsRoot = settingsRoot,
                        settingsAutoScan = settingsAutoScan,
                        settingsShowRecent = settingsShowRecent,
                        onRootChange = { settingsRoot.setValue(it) },
                        onAutoScanChange = { settingsAutoScan.setValue(it) },
                        onShowRecentChange = { settingsShowRecent.setValue(it) },
                    )
                },
            )
        }
    }
}

// ─── Sidebar nav item ──────────────────────────────────────────────────────

private fun BoxBuilder.valNavItem(
    icon: String,
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    // Compose a single ListBoxRow with icon + label; class `sidebar-selected`
    // toggles the accent pill. No gray slab, no full-width bar.
    ListBoxRow(
        onClick = onClick,
        modifier = if (selected)
            Modifier.Empty.classes(Design.CLS_SIDEBAR_SELECTED)
        else Modifier.Empty,
    ) {
        Icon(icon, modifier = Modifier.size(18, 18))
        Spacer(modifier = Modifier.size(Design.s2, 0))
        Text(title, modifier = Modifier.Empty.classes(Design.CLS_TEXT_TITLE))
    }
}

// ─── Content stack (page router) ───────────────────────────────────────────

private fun BoxBuilder.ContentStack(
    active: NavPage,
    games: List<Game>,
    selectedGame: Game,
    onSelectGame: (Game) -> Unit,
    settingsRoot: org.librelab.gtk4kt.State<String>,
    settingsAutoScan: org.librelab.gtk4kt.State<Boolean>,
    settingsShowRecent: org.librelab.gtk4kt.State<Boolean>,
    onRootChange: (String) -> Unit,
    onAutoScanChange: (Boolean) -> Unit,
    onShowRecentChange: (Boolean) -> Unit,
) {
    column {
        when (active) {
            NavPage.Library -> LibraryPage(games, selectedGame, onSelectGame)
            NavPage.Recent -> RecentPage(games, onSelectGame)
            NavPage.Settings -> SettingsPage(
                settingsRoot, settingsAutoScan, settingsShowRecent,
                onRootChange, onAutoScanChange, onShowRecentChange,
            )
            NavPage.About -> AboutPage()
        }
    }
}

// ─── Library page ─────────────────────────────────────────────────────────

private fun BoxBuilder.LibraryPage(
    games: List<Game>,
    selectedGame: Game,
    onSelectGame: (Game) -> Unit,
) {
    column {
        // Page header (search + filter actions live here, not as a separate
        // HeaderBar — gives the page a unified top region).
        PageHeader(
            title = "Library",
            subtitle = "${games.size} games",
            trailing = {
                // Search entry + settings shortcut — flat, sit inline.
                OutlinedTextField(
                    placeholder = "Search…",
                    modifier = Modifier.Empty.classes(Design.CLS_SEARCH_ENTRY),
                    onValueChange = {},
                )
                Spacer(modifier = Modifier.size(Design.s2, 0))
                IconButton("preferences-system") {
                    System.err.println("[Library] settings clicked")
                }
            },
        )

        // Two-pane: list + selected detail. ListBox on the left, info card on
        // the right. Both share the available content width.
        row(spacing = 0, modifier = Modifier.fillMaxSize()) {
            // Game list — flat rows, every row carries the full identity.
            ListBox(modifier = Modifier.weight(1f).fillMaxHeight().classes(Design.CLS_CONTENT_LIST)) {
                for (game in games) {
                        GameListRow(
                            game = game,
                            selected = selectedGame == game,
                            onClick = { onSelectGame(game) },
                        )
                    }
            }
            // Detail pane — only visible when there's room (≥1500px). For
            // Phase 8 we'll always show it; Phase 8b will hide it below 1500.
            column(
                spacing = Design.s4,
                modifier = Modifier.size(360, 0).fillMaxHeight().padding(Design.s6, Design.s6, Design.s6, 0),
            ) {
                Spacer(modifier = Modifier.size(Design.s2, 0))
                Text("Selected", modifier = Modifier.Empty.classes(Design.CLS_SECTION_TITLE))
                Text(selectedGame.name, modifier = Modifier.Empty.classes(Design.CLS_PAGE_TITLE))
                Text(selectedGame.typeLabel, modifier = Modifier.Empty.classes(Design.CLS_PAGE_SUBTITLE))
                Spacer(modifier = Modifier.size(Design.s4, 0))
                Button("Play", onClick = { System.err.println("[Library] play ${selectedGame.name}") })
                Spacer(modifier = Modifier.size(Design.s2, 0))
                OutlinedButton("Details", onClick = { System.err.println("[Library] details ${selectedGame.name}") })
            }
        }
    }
}

// ─── Game list row — typography hierarchy ─────────────────────────────────

private fun BoxBuilder.GameListRow(
    game: Game,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val rowMod = if (selected) Modifier.Empty.classes("selected") else Modifier.Empty
    ListBoxRow(
        onClick = onClick,
        modifier = rowMod,
    ) {
        // Thumbnail (color-block with subtle icon — no emoji).
        Box(modifier = Modifier.size(Design.thumbSize, Design.thumbSize).classes(Design.CLS_THUMB)) {
            Icon("applications-games", modifier = Modifier.size(20, 20))
        }
        Spacer(modifier = Modifier.size(Design.s4, 0))

        // Title + secondary text stack.
        column(spacing = 2, modifier = Modifier.weight(1f)) {
            Text(game.name, modifier = Modifier.Empty.classes(Design.CLS_TEXT_TITLE))
            Text(game.typeLabel, modifier = Modifier.Empty.classes(Design.CLS_TEXT_SUBTITLE))
        }

        // Metadata (right-aligned, dim).
        Text(game.lastPlayed, modifier = Modifier.Empty.classes(Design.CLS_TEXT_METADATA))
        Spacer(modifier = Modifier.size(Design.s4, 0))

        // Trailing action (subtle). Hover state is implicit via row hover.
        IconButton("media-playback-start") {
            System.err.println("[Library] play ${game.name}")
        }
    }
}

// ─── PageHeader — consistent page chrome ──────────────────────────────────

private fun BoxBuilder.PageHeader(
    title: String,
    subtitle: String? = null,
    trailing: (BoxBuilder.() -> Unit)? = null,
) {
    // Top region: large title on the left, optional subtitle below; actions
    // on the right. Single HBox that takes the full content width.
    row(
        spacing = Design.s4,
        modifier = Modifier.fillMaxWidth().padding(Design.s6, Design.s6, Design.s6, Design.s4),
    ) {
        column(spacing = 2, modifier = Modifier.weight(1f)) {
            Text(title, modifier = Modifier.Empty.classes(Design.CLS_PAGE_TITLE))
            if (subtitle != null) {
                Text(subtitle, modifier = Modifier.Empty.classes(Design.CLS_PAGE_SUBTITLE))
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}

// ─── Recent page ─────────────────────────────────────────────────────────

private fun BoxBuilder.RecentPage(games: List<Game>, onSelect: (Game) -> Unit) {
    column {
        PageHeader(title = "Recent", subtitle = "Last 5 played games")
        ListBox(modifier = Modifier.fillMaxSize().classes(Design.CLS_CONTENT_LIST)) {
            for (game in games.take(5)) {
                GameListRow(game, false, onClick = { onSelect(game) })
            }
        }
    }
}

// ─── Settings page — flat, separated rows ────────────────────────────────

private fun BoxBuilder.SettingsPage(
    root: org.librelab.gtk4kt.State<String>,
    autoScan: org.librelab.gtk4kt.State<Boolean>,
    showRecent: org.librelab.gtk4kt.State<Boolean>,
    onRootChange: (String) -> Unit,
    onAutoScanChange: (Boolean) -> Unit,
    onShowRecentChange: (Boolean) -> Unit,
) {
    column {
        PageHeader(title = "Settings", subtitle = "Customize how ika behaves")

        // One PreferencesGroup per concern, with a hairline between groups.
        // Each row is a flat "action-row" with title/subtitle/suffix.
        column {
            Text("Library", modifier = Modifier.Empty.classes(Design.CLS_SECTION_TITLE))
            SettingsRow(
                    title = "Game folder",
                    description = root.value,
                    icon = "folder-open",
                ) {
                    OutlinedButton("Choose…", onClick = {
                        System.err.println("[Library] choose folder")
                    })
                }
            Divider(modifier = Modifier.Empty.classes(Design.CLS_GROUP_SEPARATOR))
            Text("Behaviour", modifier = Modifier.Empty.classes(Design.CLS_SECTION_TITLE))
            SettingsRow(
                    title = "Scan on startup",
                    description = "Detect engines when ika opens.",
                ) {
                    Switch(checked = autoScan.value, onCheckedChange = onAutoScanChange)
                }
            Divider(modifier = Modifier.Empty.classes(Design.CLS_GROUP_SEPARATOR))
            SettingsRow(
                    title = "Show recent games",
                    description = "Display recently launched games on the Library page.",
                ) {
                    Switch(checked = showRecent.value, onCheckedChange = onShowRecentChange)
                }
            Divider(modifier = Modifier.Empty.classes(Design.CLS_GROUP_SEPARATOR))
            Text("About", modifier = Modifier.Empty.classes(Design.CLS_SECTION_TITLE))
            SettingsRow(
                    title = "Version",
                    description = "ika 0.1.0 · gtk4kt port",
                ) {}
        }
    }
}

private fun BoxBuilder.SettingsRow(
    title: String,
    description: String? = null,
    icon: String? = null,
    block: BoxBuilder.() -> Unit,
) {
    // Box rather than ListBoxRow for better control of internal layout.
    row(
        spacing = Design.s4,
        modifier = Modifier.fillMaxWidth().padding(Design.s4, Design.s3).classes(Design.CLS_ACTION_ROW),
    ) {
        if (icon != null) {
            Icon(icon, modifier = Modifier.size(20, 20))
        }
        column(spacing = 2, modifier = Modifier.weight(1f)) {
            Text(title, modifier = Modifier.Empty.classes(Design.CLS_ACTION_TITLE))
            if (description != null) {
                Text(description, modifier = Modifier.Empty.classes(Design.CLS_ACTION_DESCRIPTION))
            }
        }
        block()
    }
}

// ─── About page ──────────────────────────────────────────────────────────

private fun BoxBuilder.AboutPage() {
    StatusPage(
        icon = "applications-games",
        title = "ika",
        description = "An offline game launcher for visual novels and other engines.\nGTK port using gtk4kt.",
    ) {}
}

// ─── Sample data ──────────────────────────────────────────────────────────

private val SAMPLE_GAMES = listOf(
    Game("Fate/stay night", "Visual Novel",    "Yesterday", "#2c3e50"),
    Game("Higurashi",       "Visual Novel",    "2 days",    "#34495e"),
    Game("Steins;Gate",     "Visual Novel",    "3 days",    "#16a085"),
    Game("VA-11 Hall-A",    "Visual Novel",    "Last week", "#8e44ad"),
    Game("Rakuen",          "Adventure",       "Last week", "#d35400"),
    Game("NieR: Automata",  "Action RPG",      "2 weeks",   "#c0392b"),
    Game("Hollow Knight",   "Metroidvania",    "3 weeks",   "#2c3e50"),
    Game("Celeste",         "Platformer",      "Last month","#3498db"),
    Game("Undertale",       "RPG",             "Last month","#27ae60"),
    Game("DOOM (1993)",     "FPS",             "6 months",  "#7f8c8d"),
)