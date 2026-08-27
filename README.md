# gtk4kt

> **Kotlin-first declarative GTK4 UI framework for Linux/GNOME**

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-orange.svg)](https://kotlinlang.org)

**gtk4kt** lets Kotlin developers write native Linux desktop UI with a declarative,
Compose-inspired API — powered by GTK4 and libadwaita under the hood.

```kotlin
fun main() = application("org.example.App") {
    window("My App", width = 800, height = 600) {
        column(spacing = 16) {
            label("Hello, GTK4!")
            button("Click me!") {
                println("clicked")
            }
        }
    }
}
```

Read this in: [English](README.md) · [简体中文](README.zh-CN.md) · [Русский](README.ru-RU.md)

---

## Architecture

```
Kotlin/JVM
     │
     ▼
  DSL API   ← Compose-inspired Kotlin DSL
     │
     ▼
GTKNative   ← JNI binding layer
     │
     ▼
jni_bridge (C shim) ← dlsym calls into Rust
     │
     ▼
gtk4kt-native (Rust cdylib) ← gtk-rs → GTK4/libadwaita
     │
     ▼
GTK4 / libadwaita → Linux / GNOME
```

**Ownership model**: Kotlin holds references (raw u64 pointers) to GTK objects.
GTK manages the actual object lifetime. No refcount juggling from Kotlin.

**Thread model**: All GTK operations must run on the GTK main thread.
A `Dispatchers.Main`-equivalent backed by GLib main context is planned.

---

## Build

### Prerequisites

- **Kotlin 2.4+** with JVM 21
- **Rust 1.75+** (for the native layer)
- **GTK 4.16+** and **libadwaita 1.9+** (system libraries)
- **gcc** (to compile the C JNI shim)

### Build steps

```bash
# 1. Build the Rust cdylib
cd src/main/rust
cargo build --release
cd ../..

# 2. Build the C JNI shim
gcc -shared -fPIC -O2 \
  -I$JAVA_HOME/include -I$JAVA_HOME/include/linux \
  -o src/main/resources/libjni_bridge.so \
  src/main/c/jni_bridge.c -ldl -lpthread

# 3. Build Kotlin
./gradlew installDist
```

### Run

```bash
cd build/install/gtk4kt
LD_LIBRARY_PATH=lib GDK_BACKEND=x11 bin/gtk4kt
```

---

## Project Status

**Phase 1 — Research & PoC** ✅ COMPLETE

- [x] GTK4 widget hierarchy (widget → container → layout)
- [x] Rust gtk-rs 0.11 + gtk4 0.11 + libadwaita 0.11
- [x] Kotlin → C shim → Rust cdylib → GTK4 chain verified
- [x] Minimal PoC: window + label + button

**Phase 2 — Declarative Runtime** 🚧 IN PROGRESS

- [ ] State management (lightweight reactive state)
- [ ] Composition & recomposition
- [ ] Node identity & reconciliation (diff-based GTK updates)
- [ ] Lifecycle management

**Phase 3 — Foundation Components** 📋 TODO

- [ ] Text, Button, TextField, CheckBox, Switch, Slider
- [ ] Row, Column, Box layout
- [ ] Scroll, List, Dialog, Window

**Phase 4 — Libadwaita** 📋 TODO

- [ ] AdwApplication, AdwApplicationWindow
- [ ] NavigationView, PreferencesPage
- [ ] Adaptive layouts & breakpoints

---

## Key Design Decisions

### Why Rust, not C?

GTK's C API is verbose and manual memory management is error-prone.
Rust's gtk-rs bindings provide zero-cost, safe wrappers around GTK objects
while keeping the native performance and full GTK API surface.

### Why C shim, not direct JNI from Kotlin?

JNI cannot call Rust functions directly — JVM expects C ABI.
The C shim (`libjni_bridge.so`) uses `dlsym` to dynamically resolve
Rust functions at runtime, avoiding a hard JNI↔Rust dependency chain.

### Why not Kotlin/Native?

Kotlin/Native would eliminate the JVM entirely, but:
- Kotlin/Native C interop is less mature than JNI
- Kotlin/Native's current state management (Kotlin/Native coroutines, memory model)
  adds complexity for UI frameworks
- Keeping JVM enables existing Kotlin ecosystem compatibility
- A future KN native variant can be explored once the API stabilizes

### Why not gtk-kn?

[gtk-kn](https://gtk-kn.gitlab.io/gtk-kn/) is a Kotlin/Native project with GObject-introspection
bindings. It doesn't provide a Compose-style declarative UI runtime.
gtk4kt focuses on the **declarative DSL** layer + GTK4 state reconciliation.

---

## Alternatives Considered

| Approach | Why Rejected |
|---|---|
| Compose Desktop (Skiko) | Skiko + Mesa + OpenJDK 21 ABI incompatibility; unresolvable |
| GTK3 + Java/Kotlin | Missing libadwaita, no native Wayland fractional scaling |
| Qt + Kotlin | Not GTK/GNOME; different ecosystem |
| WebView2 | Not native GTK; performance overhead; accessibility gaps |
| Direct JNI to C GTK wrappers | C manual memory management; no ownership safety |

---

## Roadmap

See [ARCHITECTURE.md](docs/ARCHITECTURE.md) for full technical analysis and phased plan.

---

## License

Apache 2.0 — see [LICENSE](LICENSE)
