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
 * Phase 6-1: just SetupScreen + LibraryScreen stub. Other composables
 * are added as we hit them in the port.
 */
fun main() {
    application("org.gtk4kt.ika.library") {
        window(title = "ika (gtk4kt port)", width = 480, height = 720) {
            // Phase 6-1/6-2: switch between SetupScreen (no game root picked)
            // and EmptyState (root picked, no games yet). Real LibraryScreen
            // uses a ViewModel.state to decide which; we just hardcode here.
            LibraryScreen(
                state = LibraryState.EmptySetup,
                onPickGameRoot = { System.err.println("[Library] pick game root") },
                onAddGame = { System.err.println("[Library] add game") },
                onOpenSettings = { System.err.println("[Library] settings") },
            )
        }
    }
}

/** Coarse states ika's ViewModel exposes to LibraryScreen. */
enum class LibraryState { EmptySetup, EmptyAfterRoot, HasGames }

/**
 * LibraryScreen top-level: Scaffold with TopAppBar + body depending on state.
 *
 * Compose source (overview):
 * ```
 * @Composable
 * fun LibraryScreen(viewModel, modifier = Modifier) {
 *     val settings by viewModel.settings.collectAsState()
 *     when {
 *         settings.gameRoot.isEmpty() -> SetupScreen(...)
 *         viewModel.games.isEmpty()    -> EmptyState(...)
 *         else                          -> ...recents grid...
 *     }
 * }
 * ```
 */
private fun WindowBuilder.LibraryScreen(
    state: LibraryState,
    onPickGameRoot: () -> Unit,
    onAddGame: () -> Unit,
    onOpenSettings: () -> Unit,
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
                LibraryState.HasGames -> EmptyState(onAddGame) // Phase 6-7 will replace
            }
        }
    }
}

// ─── SetupScreen (Compose port) ─────────────────────────────────────────

/**
 * SetupScreen — empty-state screen asking the user to pick a game library folder.
 *
 * Compose source:
 * ```
 * Column(
 *     modifier = modifier.padding(32.dp).fillMaxSize(),
 *     horizontalAlignment = Alignment.CenterHorizontally,
 *     verticalArrangement = Arrangement.Center,
 * ) {
 *     Icon(Icons.Outlined.FolderOpen, ..., modifier = Modifier.size(72.dp), tint = ...)
 *     Spacer(Modifier.height(16.dp))
 *     Text(setup_title, ..., textAlign = TextAlign.Center)
 *     Spacer(Modifier.height(8.dp))
 *     Text(setup_desc, ..., textAlign = TextAlign.Center)
 *     Spacer(Modifier.height(24.dp))
 *     Button(onClick = onPick) { Icon(...); Spacer(width=8.dp); Text(setup_pick) }
 * }
 * ```
 */
private fun WindowBuilder.SetupScreen(onPick: () -> Unit) {
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

/**
 * EmptyState — "no games found" screen.
 *
 * Compose source:
 * ```
 * Column(
 *     modifier = modifier.padding(32.dp).fillMaxSize(),
 *     horizontalAlignment = Alignment.CenterHorizontally,
 *     verticalArrangement = Arrangement.Center,
 * ) {
 *     Icon(Icons.Outlined.Storage, ..., modifier = Modifier.size(72.dp), tint = ...)
 *     Spacer(Modifier.height(16.dp))
 *     Text(library_empty, ..., textAlign = TextAlign.Center)
 *     Spacer(Modifier.height(24.dp))
 *     Button(onClick = onAdd) { Icon(Icons.Outlined.Add); Spacer; Text(library_add) }
 * }
 * ```
 *
 * Differs from SetupScreen in:
 *  - Icon: Icons.Outlined.Storage (not FolderOpen)
 *  - Single text block (no body description)
 *  - Button label "Add game" (not "Pick game folder")
 */
private fun WindowBuilder.EmptyState(onAdd: () -> Unit) {
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
