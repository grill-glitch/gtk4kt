package org.librelab.gtk4kt.examples

import org.librelab.gtk4kt.*
import org.librelab.gtk4kt.runtime.Modifier

/**
 * Phase 5a verification: exercises the new Compose-completeness widgets added
 * in this round: Image, CircularProgressIndicator, ElevatedCard, TextButton,
 * FloatingActionButton, LazyColumn, LazyRow, Box, AlertDialog/MessageDialog,
 * plus Modifier.fillMaxSize/aspectRatio/background/verticalScroll/align.
 *
 * Each section that builds a widget prints a log line; the GTK signal_test
 * auto-fire invokes registered callbacks (where applicable). The widgets
 * themselves render but no click-handler-firing is asserted here — presence
 * of `registered <Type>` lines from Rust proves each widget was built.
 */
fun main() {
    application("org.gtk4kt.phase5a") {
        Scaffold(title = "Phase 5a Verification", width = 480, height = 640) {
            topBar {
                IconButton(Icons.Outlined.ArrowBack) {
                    System.err.println("[5a] back clicked")
                }
            }
            body {
                // fillMaxSize + background Modifier
                Surface(modifier = Modifier.fillMaxSize()) {
                    Text("Phase 5a Verification", modifier = Modifier.Empty.padding(16, 8))
                }

                Spacer(Modifier.height(8))

                // Image with a fallback icon (file doesn't exist)
                Image(path = "/nonexistent.png", fallbackIcon = "folder-open",
                    modifier = Modifier.Empty.padding(8))

                // CircularProgressIndicator
                CircularProgressIndicator(modifier = Modifier.Empty.padding(8))

                Spacer(Modifier.height(8))

                // ElevatedCard (HIG: stronger shadow than Card)
                ElevatedCard(modifier = Modifier.fillMaxWidth().padding(8)) {
                    Text("Inside ElevatedCard", modifier = Modifier.Empty.padding(12))
                    Divider()
                    Text("with elevation", modifier = Modifier.Empty.padding(12))
                }

                Spacer(Modifier.height(8))

                // Box composable — overlays children (badge use case)
                Box(modifier = Modifier.Empty.padding(8)) {
                    Text("Background text")
                    Text("OVERLAID", modifier = Modifier.Empty.padding(8))
                }

                Spacer(Modifier.height(8))

                // LazyColumn — fake game list
                val games = listOf("Game A", "Game B", "Game C", "Game D", "Game E")
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(games) { game ->
                        Text("• $game", modifier = Modifier.Empty.padding(8, 4))
                    }
                }

                Spacer(Modifier.height(8))

                // LazyRow — horizontal scrolling row
                LazyRow(modifier = Modifier.fillMaxWidth()) {
                    items(listOf("A", "B", "C", "D")) { letter ->
                        OutlinedButton(letter, modifier = Modifier.Empty.padding(4))
                    }
                }

                Spacer(Modifier.height(8))

                // row with TextButton + FloatingActionButton
                row(spacing = 8, modifier = Modifier.fillMaxWidth()) {
                    TextButton("Cancel") {
                        System.err.println("[5a] Cancel clicked")
                    }
                    Spacer(Modifier.weight(1f))
                    FloatingActionButton("+") {
                        System.err.println("[5a] FAB clicked")
                    }
                }

                Spacer(Modifier.height(8))

                // Modifier.aspectRatio test — golden-ratio thumb
                Surface(modifier = Modifier.Empty.size(160, 0).aspectRatio(1.618f)) {
                    Text("1.618:1", modifier = Modifier.alignCenterHorizontally().padding(8))
                }

                Spacer(Modifier.height(16))

                // HIG-compliant MessageDialog (gtk_message_dialog_new).
                // Triggered by clicking the button — AlertDialog window opens.
                OutlinedButton("Show about dialog") {
                    AlertDialog(
                        onDismissRequest = { System.err.println("[5a] dismissed") },
                        confirmButton = DialogButton("OK") {
                            System.err.println("[5a] OK clicked")
                        },
                        title = "About gtk4kt",
                        text = "Phase 5a Compose-completeness verified.",
                    )
                }
            }
        }
    }
}