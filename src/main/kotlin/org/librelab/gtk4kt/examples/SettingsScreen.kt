package org.librelab.gtk4kt.examples

import org.librelab.gtk4kt.*
import org.librelab.gtk4kt.runtime.Modifier

/**
 * Port of ika's SettingsScreen (Compose) → gtk4kt (DSL).
 *
 * Mirrors `~/ika/app/src/main/java/org/librelab/ika/ui/SettingsScreen.kt`
 * one-for-one, replacing Compose widgets with gtk4kt equivalents. Android
 * source unchanged — this is a parallel implementation in the desktop module.
 */
fun main() {
    application("org.gtk4kt.ika.settings") {
        Scaffold(title = "Settings", width = 480, height = 720) {
            topBar {
                IconButton(Icons.Outlined.ArrowBack) {
                    System.err.println("[Settings] back clicked")
                }
            }
            body {
                SectionTitle("Appearance")
                DropdownRow(
                    label = "Theme",
                    value = "System",
                    options = listOf(
                        "system" to "System",
                        "light" to "Light",
                        "dark" to "Dark",
                    ),
                    onSelect = { v -> System.err.println("[Settings] theme → $v") },
                )
                BoolRow(
                    label = "Dynamic color",
                    desc = "Use Material You colors from wallpaper",
                    value = true,
                    onValue = { v -> System.err.println("[Settings] dynamicColor → $v") },
                )

                Spacer(Modifier.height(16))

                SectionTitle("General")
                DropdownRow(
                    label = "Game orientation",
                    value = "Auto",
                    options = listOf(
                        "auto" to "Auto",
                        "left" to "Left-handed",
                        "right" to "Right-handed",
                    ),
                    onSelect = { v -> System.err.println("[Settings] orientation → $v") },
                )
                OutlinedButton("Pick game folder", modifier = Modifier.fillMaxWidth()) {
                    System.err.println("[Settings] open folder picker")
                }
                OutlinedTextField(value = "8086", placeholder = "Web game port",
                    modifier = Modifier.fillMaxWidth()) { v ->
                    System.err.println("[Settings] web port → $v")
                }
                BoolRow(
                    label = "Show toast on engine error",
                    value = true,
                    onValue = { v -> System.err.println("[Settings] toastOnError → $v") },
                )

                Spacer(Modifier.height(16))

                SectionTitle("KR2")
                BoolRow(
                    label = "Ignore focus events",
                    value = false,
                    onValue = { v -> System.err.println("[Settings] ignoreFocus → $v") },
                )
                BoolRow(
                    label = "Use old GetBaseAddress API",
                    value = false,
                    onValue = { v -> System.err.println("[Settings] oldGetBaseAddress → $v") },
                )

                Spacer(Modifier.height(16))

                SectionTitle("ONS")
                BoolRow(
                    label = "Ignore screen cutout",
                    value = false,
                    onValue = { v -> System.err.println("[Settings] onsIgnoreCutout → $v") },
                )
                BoolRow(
                    label = "Stretch to full screen",
                    value = true,
                    onValue = { v -> System.err.println("[Settings] onsStretchFull → $v") },
                )
                BoolRow(
                    label = "Disable video playback",
                    value = false,
                    onValue = { v -> System.err.println("[Settings] onsDisableVideo → $v") },
                )
                DropdownRow(
                    label = "Text encoding",
                    value = "utf-8",
                    options = listOf("gbk", "sjis", "utf-8", "utf-16").map { it to it },
                    onSelect = { v -> System.err.println("[Settings] onsEncoding → $v") },
                )

                row(spacing = 8) {
                    Text("Sharpness value", modifier = Modifier.weight(1f))
                    Text("0.0", modifier = Modifier.weight(1f).alignEnd())
                }
                Slider(
                    value = 0f,
                    min = 0f,
                    max = 30f,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { v -> System.err.println("[Settings] onsSharpnessValue → $v") },
                )

                Spacer(Modifier.height(16))
                Divider()
                Spacer(Modifier.height(8))
                Text("About: Ika engine settings (gtk4kt port)",
                    modifier = Modifier.Empty.padding(24))
            }
        }
    }
}

// ───── Helpers mirroring SettingsScreen's private composables ─────

private fun BoxBuilder.SectionTitle(text: String) {
    Text(text, modifier = Modifier.Empty.padding(16, 8))
    Divider()
}

private fun BoxBuilder.BoolRow(
    label: String,
    desc: String? = null,
    value: Boolean,
    onValue: (Boolean) -> Unit,
) {
    row(spacing = 8, modifier = Modifier.fillMaxWidth().padding(0, 8)) {
        column(modifier = Modifier.weight(1f)) {
            Text(label)
            if (desc != null) {
                Text(desc, modifier = Modifier.Empty.padding(0, 4))
            }
        }
        Switch(checked = value, onCheckedChange = onValue)
    }
}

private fun BoxBuilder.DropdownRow(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
) {
    row(spacing = 8, modifier = Modifier.fillMaxWidth().padding(0, 8)) {
        Text(label, modifier = Modifier.weight(1f))
        DropdownMenu(label = value) {
            options.forEach { (key, labelText) ->
                DropdownMenuItem(label = labelText) {
                    onSelect(key)
                }
            }
        }
    }
}