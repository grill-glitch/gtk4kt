# gtk4kt — Kotlin Declarative GTK4 UI Framework — Architecture

> **Phase 1 of 6** — Research, Validation & Architecture Design

---

## 1. Executive Summary

**Goal**: Build a Kotlin-first declarative UI framework for Linux/GNOME using GTK4 + Libadwaita as the native backend. Users write Compose-style Kotlin DSL; the framework produces real GTK4 widgets with native rendering, accessibility, and theming.

**Current implementation**: Kotlin/JVM (Kotlin 2.4.10) + JDK 21 Panama FFI + Rust cdylib + GTK4
> **Status**: Phase 1 PoC ✅ verified working — GTK window opens, displays label+button, runs main loop.

**Why Kotlin/JVM + Panama**:
- JDK 21 Panama FFI (`Linker`, `SymbolLookup`) provides zero-dependency native interop without JNI boilerplate
- Kotlin/JVM gives us a stable, familiar toolchain; no Kotlin/Native toolchain complexity
- Rust cdylib (`libgtk4kt_native.so`) exposes safe C-compatible API via `#[no_mangle] extern "C"`
- Panama FFI calls native functions directly from Kotlin with full type safety

**Why Rust over C**:
- gtk-rs provides idiomatic, memory-safe GTK4 bindings; widget lifetimes handled by Rust ownership
- Rust's `cdylib` compiles to a native `.so` with clean C ABI
- C shim replaced by pure Rust — no manual `dlopen`/`dlsym` in C

**Why NOT pure Kotlin/JVM + JNI**:
- JNI requires `System.loadLibrary()` + `native` keyword + generated JNI stubs — heavyweight for daily UI work
- JNI threadAttach/detach complexity; global reference management

**Why NOT Kotlin/Native + C interop**:
- Kotlin/Native toolchain adds complexity (LLVM, customstdlib)
- We already have a working Kotlin/JVM setup; Panama bridges the gap without K/N

---

## 2. GTK4 Architecture

### 2.1 Widget Hierarchy

GTK4 uses a parent-child widget tree (not the CSS box model as the primary layout API).

```
GObject
  └── GtkWidget
        ├── GtkWindow (GtkWidget)
        │     ├── GtkApplicationWindow (GtkWindow)
        │     │     └── AdwApplicationWindow (GtkApplicationWindow)
        │     ├── GtkDialog (GtkWindow)
        │     └── GtkPopover (GtkWindow)
        ├── GtkBox (GtkWidget)
        ├── GtkGrid (GtkWidget)
        ├── GtkColumnView (GtkWidget)
        └── ... (all UI widgets)
```

**Key invariant**: A `GtkWidget` can only have ONE parent. Setting a child on a parent REMOVES it from any previous parent.

### 2.2 Widget Lifecycle & Ownership

GTK4 uses **floating reference** (fref) counting. Most widgets start with refcount=1 and a fref. When a widget is added to a parent, the parent holds a strong ref and the fref is cleared.

**Rust gtk-rs wraps this**:
```rust
let button = gtk::Button::new(); // strong ref owned by Rust
window.set_child(Some(&button));  // GTK takes ownership, Rust still has strong ref
// button is dropped here — GTK still owns it
```

**Kotlin equivalent**: The Kotlin wrapper holds the `GObject` pointer. When the Kotlin object is garbage collected, it calls `g_object_unref()`. GTK widget tree holds the parent-child references.

### 2.3 Layout System

GTK4 uses a **layout manager** model (GTK3 used size_allocate vfuncs).

```
GtkWidget
  └── GtkLayoutManager (interface)
        ├── GtkBoxLayout
        ├── GtkGridLayout
        ├── GtkCenterLayout
        ├── GtkBinLayout
        ├── GtkConstraintLayout
        └── GtkFixedLayout
```

Layout happens automatically via the layout manager. Widgets measure themselves (gtk_widget_measure()) and the parent allocates size via gtk_widget_allocate().

### 2.4 Signal System

GTK uses GObject signals (not Qt-style slots).

```c
// C signal connection
g_signal_connect(button, "clicked", G_CALLBACK(on_clicked), user_data);

// "clicked" is the detailed signal name
// Callback signature: void (*)(GtkButton*, gpointer)
```

Callbacks are **not** type-safe at the C level. gtk-rs generates typed closures:
```rust
button.connect_clicked(|button| { /* button is &gtk::Button */ });
```

For Kotlin interop: Kotlin lambda → converted to a Rust closure → registered via gtk-rs → called by GTK → calls back into Kotlin.

### 2.5 GTK Main Loop

GTK requires ALL UI operations on the main thread (GTK thread). The main loop runs via `gtk_main()` or `GtkApplication::run()`.

```
GTK Main Thread
  └── GLib MainContext (default)
        └── GSource / GMainLoop
              └── UI event loop (pointer, keyboard, draw, etc.)
```

Calling GTK APIs from a background thread is **undefined behavior** (can deadlock or corrupt state).

Kotlin coroutines MUST dispatch to the GTK main context for all UI operations.

---

## 3. Libadwaita Architecture

Libadwaita provides building blocks for modern GNOME apps:

### 3.1 Key Widgets

```
AdwApplication           — Application-level state and lifecycle
  └── AdwApplicationWindow — Window with libadwaita styling
        ├── AdwToolbarView   — Header bar + content + optional bottom bar
        │     ├── AdwHeaderBar
        │     └── AdwViewStack
        ├── AdwNavigationView — Stack-based navigation with transitions
        │     └── AdwNavigationPage
        ├── AdwPreferencesPage — Settings page layout
        │     ├── AdwPreferencesGroup
        │     └── AdwActionRow / AdwSwitchRow / AdwComboRow / AdwEntryRow
        └── AdwBreakpoint     — Responsive breakpoint conditions
```

### 3.2 Adwaita API Style

Libadwaita uses the same GObject pattern as GTK:
```c
adw_application_window_new(GTK_APPLICATION(app));
adw_toolbar_view_new();
adw_navigation_view_new();
```

### 3.3 Available Headers (91 total)

Core for our framework:
- `adw-application.h`, `adw-application-window.h`
- `adw-toolbar-view.h`, `adw-header-bar.h`
- `adw-navigation-view.h`, `adw-navigation-page.h`
- `adw-view-stack.h`, `adw-view-switcher.h`
- `adw-preferences-page.h`, `adw-preferences-group.h`
- `adw-action-row.h`, `adw-switch-row.h`, `adw-combo-row.h`, `adw-entry-row.h`
- `adw-dialog.h`, `adw-alert-dialog.h`, `adw-message-dialog.h`
- `adw-breakpoint.h`, `adw-clamp.h`, `adw-clamp-layout.h`
- `adw-toast.h`, `adw-toast-overlay.h`
- `adw-animation.h`, `adw-banner.h`
- `adw-carousel.h`, `adw-tab-view.h`
- `adw-avatar.h`, `adw-button-content.h`
- `adw-view-switcher-title.h`, `adw-view-switcher-sidebar.h`

---

## 4. GObject Introspection (GIR)

GObject Introspection generates machine-readable metadata from C headers:
```
C header → GIR XML → Bindings (Python, JavaScript, Rust, Kotlin/Native, ...)
```

The tool is `g-ir-compiler` (gobject-introspection).

gtk4, libadwaita-1, gobject-2.0, glib-2.0 all provide `.typelib` files:
```
/usr/lib/girepository-1.0/gtk4.typelib
/usr/lib/girepository-1.0/libadwaita-1.typelib
/usr/lib/girepository-1.0/GObject-2.0.typelib
```

gtk-kn uses this to generate Kotlin/Native bindings. gtk-rs uses `gir` tool to generate Rust bindings from GIR.

---

## 5. Kotlin/Native GTK Bindings

### 5.1 gtk-kn Project

**Status**: Active (2024), fully GIR-driven generation.

What it provides:
- Kotlin/Native bindings for GTK4, Gio, Adwaita, Pango, GdkPixbuf, etc.
- Kotlin DSL for building UI (similar to GtkBuilder but in Kotlin)
- Signal connects as Kotlin lambdas
- Widget construction via `GtkBuilder` or direct constructor

**What we can learn from gtk-kn**:
- GIR → Kotlin/Native code generation approach
- Kotlin lambda → C closure conversion
- Memory management (who owns what)
- Coroutine integration with GLib main context

**What gtk-kn does NOT solve** (and we must):
- Declarative UI runtime (Compose-style state → recomposition → GTK tree update)
- Minimal reconciliation
- Modifier system
- Compose Multiplatform's slot-based layout system adaptation to GTK

### 5.2 Kotlin/Native C Interop

Kotlin/Native has direct C interop without JNI:

```kotlin
@CImport("gtk-4/gtk.h")
external fun gtk_button_new(): kotlinx.cinterop.COpaquePointer

@CImport("gtk-4/gtk.h")
external fun gtk_button_set_label(button: COpaquePointer, label: CString?)

// CString from Kotlin String
```

But this is **manual** and **verbose**. We need gtk-kn or gtk-rs to make this ergonomic.

### 5.3 Kotlin/Native Memory Model

Kotlin/Native has its own garbage collector (Kotlin/Native memory manager, since 1.6.0). It does NOT use JVM heap. This is fundamentally different from Kotlin/JVM.

- Kotlin/Native objects are **memory-managed** (traced GC, not refcount)
- `COpaquePointer` (raw C pointer) is **unmanaged**
- `memScoped` / `alloc` / `placementNew` for scoped C allocation
- `cstr` to convert Kotlin String to C `char*`
- `StableRef.create(obj)` to get a stable `gpointer` for C callbacks

For GTK: Kotlin wrapper holds `COpaquePointer` to GTK object. When Kotlin object is GC'd, we need to call `g_object_unref()` — but the GC doesn't automatically do this. We must use `StableRef` or `memScoped` with explicit cleanup, or use Kotlin/Native's `attachConcurrentFinalizer`.

---

## 6. Rust GTK Bindings (gtk-rs)

### 6.1 Status

Stable, mature, well-tested. Used in production (many GNOME apps written in Rust).

**Crate versions on this system**:
```
gtk4 = "0.11.4"      (crates.io)
libadwaita-1 = "0.4.0"  (crates.io — outdated)
```

### 6.2 Signal/Slot Pattern

```rust
button.connect_clicked(|button| {
    println!("Clicked!");
});
```

The closure borrows `button`. For Kotlin callbacks, we need the closure to call back into Kotlin via a global reference or FFI.

### 6.3 Builder Pattern

GTK4-rs uses a builder pattern:
```rust
let window = gtk::ApplicationWindow::builder()
    .application(&app)
    .title("Hello")
    .default_width(350)
    .build();
```

### 6.4 Object Lifetime

Rust owns GTK objects strongly:
```rust
let window = gtk::ApplicationWindow::new(&app); // strong ref
// window is dropped → g_object_unref() called automatically
```

---

## 7. Kotlin ↔ Rust Communication

### 7.1 Option A: C ABI + Rust cdylib + K/N

```
Kotlin/Native
      │
      │ dlopen / dlsym ("libgtk_bridge.so")
      ▼
Rust cdylib (C ABI)
      │
      ▼
gtk-rs / GTK4 / libadwaita
```

**Pros**:
- Clean C ABI boundary
- Rust can be a static cdylib
- K/N has built-in support for C interop
- Kotlin code calls Rust functions as `external fun` declarations

**Cons**:
- Need to define a stable C API boundary
- Kotlin callbacks → Rust closures requires `StableRef` + trampolines
- Double FFI: Kotlin↔C↔Rust

### 7.2 Option B: Kotlin/Native + gtk-kn (no Rust)

```
Kotlin/Native
      │
      │ GObject Introspection
      ▼
gtk-kn (K/N bindings for GTK4)
      │
      ▼
GTK4 / libadwaita
```

**Pros**:
- No Rust dependency for GTK calls
- Single language (Kotlin)
- Direct GTK access from Kotlin

**Cons**:
- gtk-kn is still maturing (2023-2024 active development)
- Declarative runtime still needs to be built on top
- No Rust advantage for performance-critical parts
- Memory management (finalizers) less explicit than Rust

### 7.3 Option C: Kotlin/JVM + JNI + Rust cdylib

Same as Option A but Kotlin/JVM instead of K/N.

**Pros**: Stability of JVM
**Cons**: JNI overhead, thread management complexity, JVM Skiko issues

### 7.4 Decision: Option A (K/N + Rust cdylib)

**Rationale**:
- Rust is already installed (1.94.0-nightly)
- gtk-rs is the stable, production-proven GTK binding
- K/N + Rust cdylib + C ABI is the cleanest communication path
- K/N avoids the Skiko/JVM problems entirely
- Rust gives us safe GTK object lifetime management without manual refcounting in Kotlin
- The bridge is explicit and debuggable

---

## 8. Declarative UI Runtime Design

### 8.1 Node Model

```
@Composable fun App() = Column {
    Text("Hello")
    Button("Click") { ... }
}

→ Node Tree (immutable description)
    ColumnNode(id=1, children=[TextNode(id=2), ButtonNode(id=3)])
        TextNode(id=2, text="Hello")
        ButtonNode(id=3, label="Click", onClick=...)
```

### 8.2 Reconciliation

When state changes, we recompose the tree and diff:

```
Old Node Tree          New Node Tree        Action
─────────────────────────────────────────────────────────
Column(id=1)           Column(id=1)         Keep (same identity)
  Text(id=2, "0")        Text(id=2, "1")    Update props (same identity)
  Button(id=3)           Button(id=4)       Remove old, Insert new (key change)
```

Key-based identity tracking enables efficient GTK tree updates:
- Keep GTK widgets that stay
- Remove GTK widgets that disappear
- Update GTK widget props that change
- Add GTK widgets that appear

NOT: destroy everything and recreate

### 8.3 State

```kotlin
// Framework provides:
fun <T> state(initial: T): State<T>

// User writes:
var count by state(0)

// Which desugars to:
var count by remember { state(0) }
```

State<T> is a Kotlin property delegate with:
- `get()`: read current value, track dependency
- `set(value)`: write new value, schedule recomposition

### 8.4 Modifier System

GTK layout is NOT CSS Flexbox. GTK has:
- `GtkBox` (linear, homogeneous/heterogeneous children)
- `GtkGrid` (2D grid)
- `GtkCenterBox` (center child with prefix/suffix)
- `GtkStack` (layered, one visible at a time)
- `GtkOverlay` (stacked on top)
- `GtkScrolledWindow` (provides scrolling container)

Modifier → Kotlin Layout → GTK LayoutManager:

```kotlin
// User writes:
Column(
    modifier = Modifier.padding(16).fillWidth()
) { ... }

// Framework translates to:
val gtkBox = GtkBox(orientation=VERTICAL)
gtkBox.setLayoutManager(GtkBoxLayout()) // default
gtkBox.setSpacing(16)
// "fillWidth" → GtkBoxLayout hexpand=true

for (child in children) {
    gtkBox.append(child.gtkWidget)
}
```

### 8.5 Event Handling

```
GTK "clicked" signal
      │
      ▼
Rust closure (trampoline)
      │
      ▼
Kotlin callback (via StableRef)
      │
      ▼
User lambda { ... }
      │
      ▼
State update
      │
      ▼
Schedule recomposition
      │
      ▼
GTK widget update
```

---

## 9. Rejected Architectures

### 9.1 "Widget Wrapper Classes" (KButton, KLabel, KWindow...)

**Rejected**: Creates a 1:1 wrapper for every GTK widget, losing the declarative composition advantage. Every widget gets its own identity, making reconciliation essentially "destroy and recreate." No shared state propagation, no modifier composition, no layout DSL.

### 9.2 Compose Desktop → GTK Backend

**Rejected**: We exhaustively tried. Compose Desktop 1.11.1 + Skiko 0.144.6 + OpenJDK 21 + Mesa = GPU context NPE in SkiaLayer initialization. The Skiko native library loading has unresolvable ABI issues in this specific environment. Even with SkikoLoader workaround (pre-loading via System.load()), the software renderer fallback (SwiftShader) also fails with the same NPE. The root cause is Skiko's rendering pipeline assumes GPU drivers that this environment doesn't provide correctly.

### 9.3 Pure Kotlin/JVM + JNI + GTK4

**Rejected**: JNI with GTK4 requires significant boilerplate (javah, JNIEXPORT, RegisterNatives, threadAttach/detach). JVM's garbage collector doesn't know about GTK object lifetimes. More importantly, JVM is the source of the Skiko problem. Going pure JVM doesn't solve that.

### 9.4 GTK3 rather than GTK4

**Rejected**: GTK4's layout manager model is a significant improvement over GTK3's sizeAllocate/vfunc model. GTK4's widget attachment API (`gtk_window_set_child`, `gtk_box_append`) is cleaner. GTK4's gesture and event controller system is more powerful. Libadwaita is designed for GTK4. There's no reason to target the older API.

---

## 10. Candidate Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Kotlin/JVM Application                        │
│                                                                  │
│   application("org.gtk4kt.example") {                            │
│       window {                                                   │
│           Box(Orientation.Vertical) {                           │
│               Label("Hello from gtk4kt!")                        │
│               Button("Click me!")                               │
│           }                                                      │
│       }                                                          │
│   }                                                              │
└──────────────────────┬──────────────────────────────────────────┘
                       │ JDK 21 Panama FFI (java.lang.foreign)
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│              Rust cdylib (libgtk4kt_native.so)                  │
│                                                                  │
│   gtk_bridge_init()           → gtk::init_check()               │
│   gtk_bridge_application_new() → gtk::Application::new()         │
│   gtk_bridge_window_new()     → gtk::ApplicationWindow::new()   │
│   gtk_bridge_box_new()        → gtk::Box::new()                  │
│   gtk_bridge_button_new()     → gtk::Button::new()               │
│   gtk_bridge_label_new()      → gtk::Label::new()                 │
│   gtk_bridge_window_set_child() → gtk::Window::set_child()        │
│   gtk_bridge_box_append()      → gtk::Box::append()               │
│   gtk_bridge_application_run() → gtk::Application::run_with_args()│
│                                                                  │
│   WidgetRegistry (thread_local HashMap<u64, gtk::Widget>)         │
│   ApplicationRegistry (thread_local HashMap<u64, gtk::Application>)│
│   ── manages GTK widget lifetimes, avoids recreating widgets      │
│                                                                  │
└──────────────────────┬──────────────────────────────────────────┘
                       │ gtk-rs 0.11 safe wrappers + direct FFI
                       ▼
┌─────────────────────────────────────────────────────────────────┐
│                    GTK4 / Libadwaita                            │
│                                                                  │
│   AdwApplication   AdwApplicationWindow   AdwToolbarView          │
│   GtkBox           GtkButton              GtkLabel                │
│   GtkGrid          GtkSignal              GObject Refcount       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 10.1 gtk4kt Application Entry Point

```kotlin
fun main() {
    GTKNative.init()

    val app = GTKNative.applicationNew("org.gtk4kt.example", 0)
    val window = GTKNative.windowNew(app)

    val box = GTKNative.boxNew(GTKNative.ORIENTATION_VERTICAL)
    val label = GTKNative.labelNew("Hello from gtk4kt!")
    val button = GTKNative.buttonNew("Click me!")

    GTKNative.boxAppend(box, label)
    GTKNative.boxAppend(box, button)
    GTKNative.windowSetChild(window, box)
    GTKNative.windowSetTitle(window, "gtk4kt")

    GTKNative.buttonConnectClicked(button) {
        println("Button clicked!")
    }

    GTKNative.applicationRun(app)
}
```

### 10.2 Rust cdylib API Surface (v1)

```rust
// Widget creation
pub extern "C" fn gtk_bridge_application_new(app_id: *const c_char, flags: u32) -> u64;
pub extern "C" fn gtk_bridge_window_new(app: u64) -> u64;
pub extern "C" fn gtk_bridge_box_new(orientation: i32 /* HORIZONTAL=0, VERTICAL=1 */) -> u64;
pub extern "C" fn gtk_bridge_button_new(label: *const c_char) -> u64;
pub extern "C" fn gtk_bridge_label_new(text: *const c_char) -> u64;

// Widget hierarchy
pub extern "C" fn gtk_bridge_window_set_child(window: u64, child: u64);
pub extern "C" fn gtk_bridge_box_append(box: u64, child: u64);
pub extern "C" fn gtk_bridge_box_set_spacing(box: u64, spacing: i32);
pub extern "C" fn gtk_bridge_box_set_halign(box: u64, align: i32); // 0=fill, 1=start, 2=center, 3=end
pub extern "C" fn gtk_bridge_box_set_valign(box: u64, align: i32);
pub extern "C" fn gtk_bridge_widget_set_hexpand(widget: u64, expand: bool);
pub extern "C" fn gtk_bridge_widget_set_vexpand(widget: u64, expand: bool);
pub extern "C" fn gtk_bridge_widget_set_size_request(widget: u64, w: i32, h: i32);
pub extern "C" fn gtk_bridge_widget_set_margin(widget: u64, margin: i32); // all sides

// Signal / callback
// callback_id is a StableRef-created gpointer
pub extern "C" fn gtk_bridge_button_connect_clicked(
    button: u64, 
    callback_id: u64  // passed as gpointer to Rust closure
) -> u64;  // returns handler_id

// Lifecycle
pub extern "C" fn gtk_bridge_widget_destroy(widget: u64);
pub extern "C" fn gtk_bridge_widget_unref(widget: u64);
pub extern "C" fn gtk_bridge_main_quit();

// Adwaita
pub extern "C" fn gtk_bridge_toolbar_view_new() -> u64;
pub extern "C" fn gtk_bridge_toolbar_view_set_top_bar(tv: u64, bar: u64);
pub extern "C" fn gtk_bridge_toolbar_view_set_content(tv: u64, content: u64);
pub extern "C" fn gtk_bridge_header_bar_new() -> u64;
pub extern "C" fn gtk_bridge_header_bar_set_title_widget(bar: u64, title: u64);
```

---

## 11. Build System

### 11.1 Project Structure

```
gtk4kt/                    ← Gradle project (Kotlin/JVM)
├── build.gradle.kts        ← Kotlin JVM app, Java 21, no Android
├── settings.gradle.kts
├── gradlew / gradlew.bat
└── src/main/
    ├── kotlin/org/librelab/gtk4kt/
    │   ├── Gtk4kt.kt              ← Public DSL API
    │   ├── examples/Hello.kt      ← PoC entry point
    │   └── internal/
    │       └── GTKNative.kt       ← JDK 21 Panama FFI binding
    ├── rust/
    │   ├── Cargo.toml             ← gtk4 0.11, glib 0.22, gio 0.22, cdylib
    │   └── src/lib.rs             ← Rust cdylib (extern "C" functions)
    └── resources/
        └── libgtk4kt_native.so    ← Built artifact (gitignored)
```

### 11.2 Build Process

1. `./gradlew installDist` compiles Kotlin and packages JAR
2. `cp target/release/libgtk4kt_native.so build/install/gtk4kt/lib/`
3. Or: `./gradlew copyNativeLibs` (custom Copy task)
4. Run: `java --enable-preview -Djava.library.path=lib -cp lib/* org.librelab.gtk4kt.examples.HelloKt`
5. Rust cdylib loaded via `System.load("/absolute/path/libgtk4kt_native.so")` at runtime

### 11.3 Kotlin/Native Toolchain

Kotlin/Native compiler (konan) is bundled with Gradle plugin:
```kotlin
plugins {
    kotlin("multiplatform") version "2.4.10"
}
```

BUT: Kotlin/Native for x86_64-linux requires a specific LLVM backend. We need to verify that:
- The Kotlin/Native LLVM backend can target x86_64-linux
- The Kotlin/Native memory manager works with our GTK callback model

**RISK**: Kotlin/Native LLVM 17+ may have target compatibility issues with this system's glibc.

**MITIGATION**: If K/N proves problematic, fall back to Kotlin/JVM with Panama FFM (Java 22+) for the Rust cdylib bridge. Panama avoids JNI's threadAttach complexity and is the recommended path forward for JVM+Native interop.

### 11.4 Rust cdylib

```toml
[package]
name = "gtk-bridge"
version = "0.1.0"
edition = "2021"

[lib]
crate-type = ["cdylib"]  # produces .so

[dependencies]
gtk = { package = "gtk4", version = "0.11" }
adwaita = { package = "libadwaita", version = "0.4", features = ["v1_9"] }
glib = { version = "0.22" }
```

---

## 12. Risk Register

| Risk | Severity | Likelihood | Mitigation |
|---|---|---|---|
| Kotlin/Native LLVM target issues | High | Medium | Fall back to K/JVM + Panama |
| GTK widget identity tracking complexity | High | High | Keep identity simple: each Kotlin node = 1 GTK widget, no pooling |
| Rust callback → Kotlin closure lifetime | High | High | StableRef + ConcurrentFinalizer |
| GTK main loop + Kotlin coroutine integration | Medium | Medium | GLib MainContext dispatcher for coroutines |
| gtk-rs version mismatch with system GTK4 | Medium | Low | gtk-rs targets GTK 4.16+; system has 4.22.4 — should be compatible |
| libadwaita-1 Rust crate outdated | Medium | Medium | Version 0.4 targets v1_4; system has 1.9.3 — may need to use direct FFI for newer Adwaita widgets |
| Modifier → GTK layout mapping is lossy | Medium | Medium | Document mapping precisely; provide escape hatch to raw GTK |
| K/N + Rust interop build complexity | Medium | High | Use Gradle `cargo()` plugin or `exec()`; keep bridge minimal |

---

## 13. PoC Plan

### Phase 1 (this document): ✅ Research & Architecture

### Phase 2: Minimal PoC — Kotlin/Native + Rust cdylib + GTK4

Goal: Prove the communication path works.

**Step 2.1**: Create Rust cdylib with minimal API
- `gtk_bridge_init()` → calls `gtk_init()`
- `gtk_bridge_application_new(id)` → returns `AdwApplication*` as `u64`
- `gtk_bridge_window_new(app_ptr)` → returns `AdwApplicationWindow*` as `u64`
- `gtk_bridge_button_new(label)` → returns `GtkButton*` as `u64`
- `gtk_bridge_label_new(text)` → returns `GtkLabel*` as `u64`
- `gtk_bridge_box_append(box, child)` 
- `gtk_bridge_button_connect_clicked(button, callback_id)`
- `gtk_bridge_main_run(app)`
- `gtk_bridge_widget_unref(widget)`

**Step 2.2**: Kotlin/Native test program
```kotlin
@CImport("gtk_bridge.h")
external fun gtkBridgeInit()
external fun gtkBridgeApplicationNew(id: CString): ULong
external fun gtkBridgeWindowNew(app: ULong): ULong
external fun gtkBridgeButtonNew(label: CString): ULong
external fun gtkBridgeLabelNew(text: CString): ULong
external fun gtkBridgeBoxAppend(box: ULong, child: ULong): Unit
external fun gtkBridgeButtonConnectClicked(button: ULong, callbackId: ULong): Unit
external fun gtkBridgeMainRun(app: ULong): Unit
external fun gtkBridgeWidgetUnref(widget: ULong): Unit

fun main() {
    gtkBridgeInit()
    val app = gtkBridgeApplicationNew(cstrOf("org.test.Poc"))
    val window = gtkBridgeWindowNew(app)
    val button = gtkBridgeButtonNew(cstrOf("Click me!"))
    
    // Attach callback via StableRef
    val callback = stableRefOf { println("clicked!") }
    gtkBridgeButtonConnectClicked(button, callback.asCPointer().toUWord())
    
    gtkBridgeBoxAppend(window, button)
    gtkBridgeMainRun(app)
}
```

**Step 2.3**: Verify it compiles, links, and shows a window.

### Phase 3: Declarative Runtime (minimal)

- Node data structure
- Composition (build node tree from @Composable functions)
- State<T> with `by` delegate
- Recomposition on state change
- Diff (simplified: rebuild children on recompose)
- GTK widget cache (id → GTK widget)

### Phase 4: Basic Components

- Window / AdwApplicationWindow
- Column / Row / Box (GtkBox)
- Text / Label
- Button / AdwActionRow
- Switch / AdwSwitchRow

### Phase 5: Libadwaita Components

- NavigationView / NavigationPage
- PreferencesPage / PreferencesGroup
- HeaderBar / ToolbarView
- Dialog / AlertDialog

### Phase 6: Real Application (ika main UI)

- Game list
- Settings
- Engine dispatch

---

## 14. Open Questions to Resolve in Phase 2

1. **Kotlin/Native stable callback mechanism**: Can we reliably pass a Kotlin lambda as a C callback to Rust, with correct memory lifetime, without leaking?
2. **GLib MainContext dispatcher**: Can we make a Kotlin CoroutineDispatcher that submits to GLib main context? This is essential for `launch { withContext(Dispatchers.GTK) { ... } }`.
3. **libadwaita-1 Rust crate version**: System libadwaita is 1.9.3, Rust crate is 0.4.0. Which Adwaita widgets work? Do we need direct FFI for newer widgets?
4. **Kotlin/Native memory model**: When Kotlin/Native GC collects a widget wrapper, we must call `g_object_unref`. Does K/N's `attachConcurrentFinalizer` work reliably for this?
5. **GTK widget disposal**: When a Kotlin node is removed from the composition tree, should we destroy the GTK widget immediately or keep it cached?

---

## 15. Verification Results (from actual experiments)

### 15.1 System Environment

```
Arch Linux x86_64
OpenJDK 21.0.12.1
Mesa 25.0.4 (radeonsi, AMD GPU)
GTK4: 4.22.4
libadwaita-1: 1.9.3
Rust: 1.94.0-nightly
Kotlin: 2.4.10 (via Gradle)
pkg-config available
gcc available (for C compilation)
```

### 15.2 GTK4/libadwaita C API Verification

```c
// Verified working:
adw_application_window_new(GTK_APPLICATION(app));  // returns GtkWidget*
gtk_window_set_child(GTK_WINDOW(window), content);
gtk_box_append(GTK_BOX(box), child);
gtk_button_set_label(GTK_BUTTON(button), "text");
g_signal_connect(button, "clicked", G_CALLBACK(cb), data);

// Verified API shape:
void gtk_widget_insert_after(GtkWidget *widget, GtkWidget *parent, GtkWidget *previous_sibling);
void gtk_window_set_titlebar(GtkWindow *window, GtkWidget *titlebar);  // NOT gtk_window_title_bar
void adw_toolbar_view_set_top_bar(AdwToolbarView *tv, GtkWidget *bar);
void adw_toolbar_view_set_content(AdwToolbarView *tv, GtkWidget *content);
void adw_application_window_set_content(AdwApplicationWindow *aw, GtkWidget *content);
```

### 15.3 C-compiled GTK4/libadwaita UI Verification

Compiled and ran:
```bash
gcc -shared -fPIC -o libika_ui.so \
  -I$JAVA_HOME/include/linux \
  $(pkg-config --cflags gtk4 libadwaita-1) \
  ika_ui.c \
  $(pkg-config --libs gtk4 libadwaita-1)

# Result: libika_ui.so loaded by Kotlin via JNI
# Window appeared (Adwaita-styled, 1280x720)
# Signal "clicked" connected and fired
# exit=124 (timeout killed GTK main loop)
```

### 15.4 libadwaita-1 Rust Crate Availability

```
libadwaita-1 Rust crate latest: 0.4.0 (crates.io)
System libadwaita: 1.9.3
Major version mismatch — Rust crate is YEARS behind
```

**Decision**: For Adwaita widgets not in the old Rust crate, use direct FFI from Rust cdylib or add Rust `extern "C"` declarations that call the GTK C API directly.

---

*Document version: 1.0.0 — ika GTK4/Kotlin Framework, Phase 1*
