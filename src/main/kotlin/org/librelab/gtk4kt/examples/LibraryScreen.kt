package org.librelab.gtk4kt.examples

import org.librelab.gtk4kt.*
import org.librelab.gtk4kt.runtime.Modifier

/**
 * Phase 7: Desktop-first port of ika's LibraryScreen (Compose) → gtk4kt DSL.
 *
 * Desktop patterns used:
 *   - NavigationSplitView: sidebar + content, collapses when window is narrow
 *   - HeaderBar: title + actions (no FAB!)
 *   - ListBox + ListBoxRow: flat, dense list of games (no Card-wrapping)
 *   - StatusPage: only for true empty states (no game root picked, etc.)
 *   - PreferencesPage + PreferencesGroup + ActionRow: settings screen
 */
fun main() {
    application("org.gtk4kt.ika.library") {
        LibraryScreen()
    }
}

/** Minimal Game data class — placeholder for ika's full Game data class. */
data class Game(val name: String, val typeLabel: String, val iconPath: String? = null)

/** Coarse states ika's ViewModel exposes. */
enum class LibraryState { EmptySetup, EmptyAfterRoot, HasGames }

/** Top-level navigation pages (mirrors ika's sidebar entries). */
enum class NavPage { Library, Recent, Settings, About }

/**
 * LibraryScreen — desktop window.
 *
 * Architecture:
 *   ApplicationWindow
 *     └── NavigationSplitView (sidebar | content)
 *           ├── sidebar: Sidebar(ListBox of NavigationPages)
 *           └── content: Stack(switched by sidebar selection)
 *                 ├── Library page (HasGames → ListBox of games)
 *                 ├── Recent page
 *                 ├── Settings page (PreferencesPage + ActionRow)
 *                 └── About page (StatusPage)
 */
fun LibraryScreen() {
    val state = LibraryState.HasGames
    val activeNavPage = remember("navPage", NavPage.Settings)
    val settingsRoot = remember("settingsRoot", "/home/user/games")
    val settingsAutoScan = remember("settingsAutoScan", true)
    val settingsShowRecent = remember("settingsShowRecent", true)

    Scaffold(title = "ika", width = 400, height = 600) {
        body {
            NavigationSplitView(
                maxSidebarWidth = 240.0,
                sidebar = {
                    Sidebar {
                        Text("ika", modifier = Modifier.Empty.padding(12, 16))
                        Divider()
                        NavigationPage("Library", Icons.Outlined.Games, "library") {
                            activeNavPage.setValue(NavPage.Library)
                        }
                        NavigationPage("Recent", Icons.Outlined.History, "recent") {
                            activeNavPage.setValue(NavPage.Recent)
                        }
                        NavigationPage("Settings", Icons.Outlined.Settings, "settings") {
                            activeNavPage.setValue(NavPage.Settings)
                        }
                        NavigationPage("About", Icons.Outlined.Info, "about") {
                            activeNavPage.setValue(NavPage.About)
                        }
                    }
                },
                content = {
                    when (activeNavPage.value) {
                        NavPage.Library -> LibraryContent(state)
                        NavPage.Recent -> RecentContent()
                        NavPage.Settings -> SettingsContent(
                            settingsRoot,
                            settingsAutoScan,
                            settingsShowRecent,
                            onRootChange = { settingsRoot.setValue(it) },
                            onAutoScanChange = { settingsAutoScan.setValue(it) },
                            onShowRecentChange = { settingsShowRecent.setValue(it) },
                        )
                        NavPage.About -> AboutContent()
                    }
                },
            )
        }
    }
}

// ─── Library page ──────────────────────────────────────────────────────────

private fun BoxBuilder.LibraryContent(state: LibraryState) {
    when (state) {
        LibraryState.EmptySetup -> SetupPage()
        LibraryState.EmptyAfterRoot -> EmptyAfterRootPage()
        LibraryState.HasGames -> HasGamesLibrary()
    }
}

private fun BoxBuilder.HasGamesLibrary() {
    column {
        // Toolbar — primary actions live in the HeaderBar, not as a FAB.
        HeaderBar(title = "Library") {
            IconButton(Icons.Outlined.Search) {
                System.err.println("[Library] search clicked")
            }
            IconButton(Icons.Outlined.Settings) {
                System.err.println("[Library] settings clicked")
            }
        }
        // Content: dense ListBox of game rows (flat — no Card wrapping).
        ListBox {
            val games = listOf(
                Game("Fate/stay night", "Visual Novel", null),
                Game("Higurashi", "Visual Novel", null),
                Game("Steins;Gate", "Visual Novel", null),
                Game("VA-11 Hall-A", "Visual Novel", null),
                Game("Rakuen", "Adventure", null),
                Game("NieR: Automata", "Action RPG", null),
                Game("Hollow Knight", "Metroidvania", null),
                Game("Celeste", "Platformer", null),
                Game("Undertale", "RPG", null),
                Game("DOOM (1993)", "FPS", null),
            )
            for (game in games) {
                ListBoxRow(onClick = { System.err.println("[Library] open ${game.name}") }) {
                    Icon(Icons.Outlined.Games, modifier = Modifier.size(24, 24))
                    column(modifier = Modifier.weight(1f).padding(0, 8)) {
                        Text(game.name)
                        Text(game.typeLabel)
                    }
                    Text("Yesterday", modifier = Modifier.Empty.padding(0, 8))
                    IconButton(Icons.Outlined.Play) {
                        System.err.println("[Library] play ${game.name}")
                    }
                }
            }
        }
    }
}

private fun BoxBuilder.SetupPage() {
    StatusPage(
        icon = Icons.Outlined.FolderOpen,
        title = "No game library set up",
        description = "Pick a folder where your games live and ika will scan it for engines.",
    ) {
        Button("Pick game folder") {
            System.err.println("[Library] pick game root")
        }
    }
}

private fun BoxBuilder.EmptyAfterRootPage() {
    StatusPage(
        icon = Icons.Outlined.Storage,
        title = "No games found",
        description = "The selected folder doesn't contain any supported games.",
    ) {
        Button("Pick a different folder") {
            System.err.println("[Library] pick game root")
        }
    }
}

// ─── Recent page ───────────────────────────────────────────────────────────

private fun BoxBuilder.RecentContent() {
    column {
        HeaderBar(title = "Recent") {}
        Text(
            "Recently played games will appear here.",
            modifier = Modifier.Empty.padding(16, 8),
        )
    }
}

// ─── Settings page (PreferencesPage + PreferencesGroup + ActionRow) ─────────

private fun BoxBuilder.SettingsContent(
    root: org.librelab.gtk4kt.State<String>,
    autoScan: org.librelab.gtk4kt.State<Boolean>,
    showRecent: org.librelab.gtk4kt.State<Boolean>,
    onRootChange: (String) -> Unit,
    onAutoScanChange: (Boolean) -> Unit,
    onShowRecentChange: (Boolean) -> Unit,
) {
    PreferencesPage {
        PreferencesGroup(
            title = "Library",
            description = "Where ika looks for games on your computer.",
        ) {
            ActionRow(
                title = "Game folder",
                description = root.value,
                icon = Icons.Outlined.FolderOpen,
            ) {
                Button("Choose...") {
                    System.err.println("[Library] choose folder clicked")
                }
            }
        }
        PreferencesGroup(title = "Behaviour") {
            ActionRow(
                title = "Scan on startup",
                description = "Detect engines when ika opens.",
            ) {
                Switch(checked = autoScan.value, onCheckedChange = onAutoScanChange)
            }
            ActionRow(
                title = "Show recent games",
                description = "Display recently launched games on the Library page.",
            ) {
                Switch(checked = showRecent.value, onCheckedChange = onShowRecentChange)
            }
        }
        PreferencesGroup(title = "About") {
            ActionRow(title = "Version", description = "ika 0.1.0 (gtk4kt port)") {}
        }
    }
}

// ─── About page (StatusPage) ───────────────────────────────────────────────

private fun BoxBuilder.AboutContent() {
    StatusPage(
        icon = Icons.Outlined.Games,
        title = "ika",
        description = "An offline game launcher for visual novels and other engines.\n\n" +
            "GTK port using gtk4kt.",
    ) {}
}