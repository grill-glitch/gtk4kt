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
            LibraryScreen()
        }
    }
}

private fun WindowBuilder.LibraryScreen() {
    // LibraryScreen top-level: Scaffold { TopAppBar; body shows SetupScreen
    // (empty state, no games yet) }.
    Scaffold(title = "ika (gtk4kt port)", width = 480, height = 720) {
        topBar {
            IconButton(Icons.Outlined.Settings) {
                System.err.println("[Library] settings clicked")
            }
        }
        body {
            SetupScreen(
                onPick = { System.err.println("[Library] pick game root clicked") },
                modifier = Modifier.fillMaxSize()
            )
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
 *
 * Known gaps in gtk4kt (will be addressed during porting):
 *   - No `dp` unit conversion (using pixel values)
 *   - No `tint` on Icon
 *   - No `MaterialTheme.typography` styles
 *   - No `Text(textAlign = ...)` — using Modifier.align() workaround
 */
private fun WindowBuilder.SetupScreen(
    onPick: () -> Unit,
    modifier: Modifier = Modifier.Empty,
) {
    // Compose's Column + horizontalAlignment/verticalArrangement don't have
    // direct 1:1 mappings in gtk4kt yet. We emulate by:
    //  - fillMaxSize on the Column (mod)
    //  - weight(1f) on children to make them equal-height
    //  - Spacers with weight(1f) for the Arrangement.Center effect
    column(modifier = modifier.fillMaxSize()) {
        // Top spacer pushes content to center (Arrangement.Center approximation)
        Spacer(Modifier.weight(1f))

        // Icons.Outlined.FolderOpen at 72px
        Icon(Icons.Outlined.FolderOpen, modifier = Modifier.size(72, 72))
        Spacer(Modifier.height(16))

        // Centered Text (uses Modifier.align on cross-axis, not TextAlign.Center
        // yet — that's a Phase 6 gap)
        Text(
            "Set up your game library",
            modifier = Modifier.Empty.fillMaxWidth().align(AlignmentHorizontal.CenterHorizontally),
        )
        Spacer(Modifier.height(8))
        Text(
            "Pick a folder where your games live and I'll scan it for engines.",
            modifier = Modifier.Empty.fillMaxWidth().align(AlignmentHorizontal.CenterHorizontally),
        )
        Spacer(Modifier.height(24))

        // Button (no Icon prefix inside for simplicity — full Button with
        // Icon-leading layout will be added when we port GameCard which
        // exercises that pattern)
        Button("Pick game folder") { onPick() }

        // Bottom spacer pushes content to center
        Spacer(Modifier.weight(1f))
    }
}
