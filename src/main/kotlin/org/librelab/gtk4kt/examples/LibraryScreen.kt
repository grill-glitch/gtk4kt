package org.librelab.gtk4kt.examples

import org.librelab.gtk4kt.*
import org.librelab.gtk4kt.runtime.Modifier
import org.librelab.gtk4kt.runtime.AlignmentHorizontal

/**
 * Phase 6: Port of ika's LibraryScreen (Compose) → gtk4kt DSL.
 *
 * Mirrors `~/ika/app/src/main/java/org/librelab/ika/ui/LibraryScreen.kt`.
 * Each helper corresponds 1:1 to its Compose counterpart. As Compose APIs
 * missing from gtk4kt surface during translation, they are added to the
 * framework immediately (logged in commit messages).
 *
 * Phase 6-3: add RecentCard + GameCover + GameCard stubs. Other composables
 * are added as we hit them in the port.
 */
fun main() {
    // application{} overload that takes a Scaffold directly (no separate
    // window{} block). This is the canonical Compose-style entry point.
    application("org.gtk4kt.ika.library") {
        LibraryScreen(
            state = LibraryState.HasGames,
            onPickGameRoot = { System.err.println("[Library] pick game root") },
            onAddGame = { System.err.println("[Library] add game") },
            onOpenSettings = { System.err.println("[Library] settings") },
            onOpenGame = { g -> System.err.println("[Library] open ${g.name}") },
            onMoreGame = { g -> System.err.println("[Library] more ${g.name}") },
        )
    }
}

/** Minimal Game data class — placeholder for ika's full Game data class. */
data class Game(val name: String, val typeLabel: String, val iconPath: String? = null)

/** Coarse states ika's ViewModel exposes to LibraryScreen. */
enum class LibraryState { EmptySetup, EmptyAfterRoot, HasGames }

/**
 * LibraryScreen top-level entry point — calls Scaffold{} directly.
 *
 * Compose source (overview):
 * ```
 * @Composable
 * fun LibraryScreen(viewModel, modifier = Modifier) {
 *     val settings by viewModel.settings.collectAsState()
 *     when {
 *         settings.gameRoot.isEmpty() -> SetupScreen(...)
 *         viewModel.games.isEmpty()    -> EmptyState(...)
 *         else                          -> ...recents row + games grid...
 *     }
 * }
 * ```
 *
 * Phase 6-3: stub `HasGames` branch with a single RecentCard + GameCard sample.
 * Real games grid comes in 6-7.
 */
fun LibraryScreen(
    state: LibraryState,
    onPickGameRoot: () -> Unit,
    onAddGame: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenGame: (Game) -> Unit,
    onMoreGame: (Game) -> Unit,
) {
    Scaffold(title = "ika (gtk4kt port)", width = 480, height = 720) {
        topBar {
            IconButton(Icons.Outlined.Settings) {
                System.err.println("[Library] settings clicked")
                onOpenSettings()
            }
        }
        body {
            when (state) {
                LibraryState.EmptySetup -> SetupScreen(onPickGameRoot)
                LibraryState.EmptyAfterRoot -> EmptyState(onAddGame)
                LibraryState.HasGames -> {
                    column {
                        Text("Recent games", modifier = Modifier.Empty.padding(16, 8))
                        val game = Game("Fate/stay night", "VN", null)
                        RecentCard(game, onClick = { onOpenGame(game) })
                        Spacer(Modifier.height(8))
                        Text("All games", modifier = Modifier.Empty.padding(16, 8))
                        val game2 = Game("Higurashi", "VN", null)
                        GameCard(game2, onClick = { onOpenGame(game2) },
                            onMore = { onMoreGame(game2) })
                    }
                }
            }
        }
    }
}

// ─── SetupScreen (Compose port, Phase 6-1) ────────────────────────────────

private fun BoxBuilder.SetupScreen(onPick: () -> Unit) {
    column(modifier = Modifier.fillMaxSize().padding(32)) {
        Spacer(Modifier.weight(1f))
        Icon(Icons.Outlined.FolderOpen, modifier = Modifier.size(72, 72))
        Spacer(Modifier.height(16))
        Text(
            "Set up your game library",
            modifier = Modifier.fillMaxWidth().align(AlignmentHorizontal.CenterHorizontally),
        )
        Spacer(Modifier.height(8))
        Text(
            "Pick a folder where your games live and I'll scan it for engines.",
            modifier = Modifier.fillMaxWidth().align(AlignmentHorizontal.CenterHorizontally),
        )
        Spacer(Modifier.height(24))
        Button("Pick game folder") { onPick() }
        Spacer(Modifier.weight(1f))
    }
}

// ─── EmptyState (Compose port, Phase 6-2) ──────────────────────────────────

private fun BoxBuilder.EmptyState(onAdd: () -> Unit) {
    column(modifier = Modifier.fillMaxSize().padding(32)) {
        Spacer(Modifier.weight(1f))
        Icon(Icons.Outlined.Storage, modifier = Modifier.size(72, 72))
        Spacer(Modifier.height(16))
        Text(
            "No games found",
            modifier = Modifier.fillMaxWidth().align(AlignmentHorizontal.CenterHorizontally),
        )
        Spacer(Modifier.height(24))
        Button("Add game") { onAdd() }
        Spacer(Modifier.weight(1f))
    }
}

// ─── RecentCard (Compose port, Phase 6-3) ────────────────────────────────

/**
 * RecentCard — horizontal game card for the "Recent games" row.
 *
 * Compose source:
 * ```
 * ElevatedCard(onClick = onClick, modifier = Modifier.width(220.dp)) {
 *     GameCover(game, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
 *     Column(Modifier.padding(10.dp)) {
 *         Text(game.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
 *         Text(stringResource(game.typeEnum().labelRes),
 *              color = MaterialTheme.colorScheme.onSurfaceVariant)
 *     }
 * }
 * ```
 *
 * Phase 6-3 notes:
 *  - ElevatedCard.onClick → gtk::Button with ReliefStyle::None (ClickableElevatedCard)
 *  - Modifier.width(220) → fixed width via Modifier.size(w,h)
 *  - GameCover is a stub (Phase 6-3) — Phase 6-5 adds real Image rendering
 *  - maxLines/overflow not yet in Text (Phase 6-5)
 */
private fun BoxBuilder.RecentCard(game: Game, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.size(220, 200),
    ) {
        GameCover(game, modifier = Modifier.fillMaxWidth())
        column(modifier = Modifier.fillMaxWidth().padding(10)) {
            Text(game.name)
            Text(game.typeLabel,
                modifier = Modifier.fillMaxWidth().padding(0, 4))
        }
    }
}

// ─── GameCover (stub — Phase 6-3, real impl in 6-5) ────────────────────────

/**
 * GameCover — loads game's icon and displays it in 16:9 aspect ratio.
 *
 * Compose source:
 * ```
 * Box(modifier.clip(RoundedCornerShape(12.dp)).background(...)) {
 *     Image(bitmap = bmp.asImageBitmap(), contentScale = ContentScale.Crop)
 *     // fallback if bmp is null: Icon(Icons.Outlined.Games, ...)
 * }
 * ```
 *
 * Phase 6-3 stub: renders Icons.Outlined.Games centered (no bitmap loading).
 * Phase 6-5: real impl via loadGameIcon() + Image(path = ...).
 */
private fun BoxBuilder.GameCover(game: Game, modifier: Modifier = Modifier.Empty) {
    Box(modifier = modifier) {
        // In Compose, an Icon fallback or an Image — both are centered.
        Icon(
            Icons.Outlined.Games,
            modifier = Modifier.fillMaxSize().align(AlignmentHorizontal.CenterHorizontally),
        )
        // No clip / background yet (Phase 6-5).
    }
}

// ─── GameCard (Compose port, Phase 6-4) ───────────────────────────────────

/**
 * GameCard — wider game card with a "more" menu.
 *
 * Compose source:
 * ```
 * Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
 *     GameCover(game, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
 *     Row(modifier = Modifier.fillMaxWidth().padding(...),
 *         verticalAlignment = Alignment.CenterVertically) {
 *         Column(Modifier.weight(1f)) {
 *             Text(game.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
 *             Text(stringResource(game.typeEnum().labelRes))
 *         }
 *         Box { IconButton(onClick = { menuOpen = true }) { Icon(MoreVert) } }
 *     }
 * }
 * ```
 *
 * Phase 6-3 stub: shows the structure but without DropdownMenu (Phase 6-4).
 */
private fun BoxBuilder.GameCard(
    game: Game,
    onClick: () -> Unit,
    onMore: () -> Unit,
) {
    // Compose source:
    //   var menuOpen by remember { mutableStateOf(false) }
    //   Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
    //     GameCover(game, Modifier.fillMaxWidth().aspectRatio(16f / 9f))
    //     Row(modifier = ..., verticalAlignment = Alignment.CenterVertically) {
    //       Column(Modifier.weight(1f)) {
    //         Text(game.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
    //         Text(stringResource(game.typeEnum().labelRes))
    //       }
    //       Box { IconButton(onClick = { menuOpen = true }) { Icon(MoreVert) }
    //             DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
    //               DropdownMenuItem("Launch", ...)
    //               DropdownMenuItem("Delete", ...)
    //             }
    //       }
    //     }
    //   }
    //
    // Phase 6-4 limitation: gtk4kt has `State<T>` (added this round) but
    // no reactive recomposition — the menu is always shown rather than
    // gated by `menuOpen`. Phase 6-5 will add reactive rebuilds.
    val menuOpen = remember("gameCard_menuOpen_${'$'}{game.name}", false)

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        GameCover(game, modifier = Modifier.fillMaxWidth())
        row(
            spacing = 4,
            modifier = Modifier.fillMaxWidth().padding(12, 8),
        ) {
            column(modifier = Modifier.weight(1f)) {
                Text(game.name)
                Text(game.typeLabel)
            }
            // Dropdown trigger — IconButton + (always-visible) DropdownMenu.
            // Phase 6-5: gate on menuOpen.value.
            Box {
                IconButton(Icons.Outlined.MoreVert) {
                    menuOpen.setValue(!menuOpen.value)
                }
                if (menuOpen.value) {
                    DropdownMenu(label = "Actions") {
                        DropdownMenuItem("Launch") { System.err.println("[GameCard] launch ${'$'}{game.name}") }
                        DropdownMenuItem("Delete") { System.err.println("[GameCard] delete ${'$'}{game.name}") }
                    }
                }
            }
        }
    }
}