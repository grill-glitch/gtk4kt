//! libgtk4kt_native — Rust FFI bridge for Kotlin GTK DSL
//! Phase 3: Kotlin Widget DSL → GTK3 builder via JSON FFI

use glib::object::{IsA, ObjectExt};
use glib::MainContext;
use libadwaita::prelude::WidgetExt;
use gtk::prelude::*;
use gtk::prelude::*;
use serde::Deserialize;
use std::cell::RefCell;
use std::collections::HashMap;
use std::fs;
use std::sync::atomic::{fence, Ordering};

// ─── Thread-local registries ────────────────────────────────────────────────

thread_local! {
    static APP_REGISTRY: RefCell<HashMap<u64, gtk::Application>> = RefCell::new(HashMap::new());
    static WIDGET_REGISTRY: RefCell<HashMap<u64, gtk::Widget>> = RefCell::new(HashMap::new());
    static CALLBACK_REGISTRY: RefCell<HashMap<u64, Box<dyn Fn() + Send + 'static>>> =
        RefCell::new(HashMap::new());
    static INVOKER_ADDR: RefCell<Option<*const std::ffi::c_void>> = RefCell::new(None);
    static UI_JSON_PATH: RefCell<Option<String>> = RefCell::new(None);
}

// ─── Data structures ─────────────────────────────────────────────────────────

#[derive(Debug, Deserialize)]
pub struct WidgetNode {
    #[serde(rename = "type")]
    pub widget_type: String,
    pub label: Option<String>,
    pub text: Option<String>,

    // Window properties
    pub title: Option<String>,
    pub width: Option<i32>,
    pub height: Option<i32>,

    // Container children
    #[serde(default)]
    pub children: Vec<WidgetNode>,

    // Box container properties
    #[serde(default)]
    pub spacing: Option<i32>,
    #[serde(default)]
    pub orientation: Option<i32>,
    #[serde(default)]
    pub margin: Option<i32>,

    // Button callback handle (set by Kotlin before JSON is sent)
    #[serde(default)]
    pub on_click_handle: Option<u64>,

    // ─── Phase 4: Compose-like Modifier fields ────────────────────────────
    #[serde(default)] pub paddingStart: Option<i32>,
    #[serde(default)] pub paddingEnd: Option<i32>,
    #[serde(default)] pub paddingTop: Option<i32>,
    #[serde(default)] pub paddingBottom: Option<i32>,
    #[serde(default)] pub fillMaxWidth: Option<bool>,
    #[serde(default)] pub fillMaxHeight: Option<bool>,
    #[serde(default)] pub halign: Option<i32>,
    #[serde(default)] pub valign: Option<i32>,
    #[serde(default)] pub weight: Option<f32>,

    // ─── Phase 4c: control properties ─────────────────────────────────────
    #[serde(default)] pub active: Option<bool>,
    #[serde(default)] pub on_change_handle: Option<u64>,
    #[serde(default)] pub min: Option<f64>,
    #[serde(default)] pub max: Option<f64>,
    #[serde(default)] pub value: Option<f64>,

    // ─── Phase 4d: icon / placeholder ─────────────────────────────────────
    #[serde(default)] pub icon: Option<String>,
    #[serde(default)] pub placeholder: Option<String>,

    // ─── Phase 5a: Compose-completeness modifiers ─────────────────────────
    #[serde(default)] pub bgColor: Option<i32>,
    #[serde(default)] pub aspectRatio: Option<f32>,
    #[serde(default)] pub verticalScroll: Option<bool>,
    #[serde(default)] pub hWeight: Option<f32>,
    #[serde(default)] pub vWeight: Option<f32>,
    #[serde(default)] pub alignmentCrossAxis: Option<i32>,
    // ─── Phase 7 desktop-first fields ─────────────────────────────────────
    #[serde(default)] pub tag: Option<String>,
    // Phase 8: style classes from design tokens.
    #[serde(default)] pub classes: Vec<String>,
    #[serde(default)] pub description: Option<String>,
    #[serde(default)] pub collapsed: Option<bool>,
    #[serde(default)] pub maxSidebarWidth: Option<f64>,
    #[serde(default)] pub columns: Option<i32>,
}

/// Downcast helper — libadwaita APIs take `&impl IsA<gtk::Widget>`; this
/// converts a generic gtk::Widget reference. No-op for already-concrete types.
fn downcast_to_widget(w: &gtk::Widget) -> gtk::Widget {
    w.clone()
}

// ─── Widget builder ─────────────────────────────────────────────────────────

fn next_key() -> u64 {
    use std::sync::atomic::AtomicU64;
    static COUNTER: AtomicU64 = AtomicU64::new(1);
    COUNTER.fetch_add(1, Ordering::Relaxed)
}

/// Phase 8: apply CSS style classes from the JSON `classes` array.
fn apply_classes<W: glib::object::IsA<gtk::Widget>>(w: &W, node: &WidgetNode) {
    let ctx = w.style_context();
    for c in &node.classes {
        ctx.add_class(c);
    }
}

fn build_widget(node: &WidgetNode) -> Option<gtk::Widget> {
    let key = next_key();
    let widget: Option<gtk::Widget> = match node.widget_type.as_str() {
        "Window" => {
            let win = gtk::Window::new();
            if let Some(ref t) = node.title {
                win.set_title(Some(t.as_str()));
            }
            if let (Some(w), Some(h)) = (node.width, node.height) {
                win.set_default_size(w, h);
            }
            /* GTK4: no set_border_width on Window. */
            let win_w = win.clone();
            win.connect_close_request(move |_| {
                /* Phase 8b: main_quit removed. Use glib::ExitCode from callback. */
                gtk::glib::Propagation::Proceed
            });
            // GTK4: Window is a Bin — wrap children in a Box and set_child once.

            if !node.children.is_empty() {

                let _win_inner = gtk::Box::new(gtk::Orientation::Vertical, 0);

                for child in &node.children {

                    if let Some(c) = build_widget(child) {

                        _win_inner.append(&c);

                    }

                }

                win.set_child(Some(&_win_inner));

            }
            win.show();
            Some(win.upcast())
        }

        "Box" => {
            let orient = match node.orientation {
                Some(0) => gtk::Orientation::Horizontal,
                _ => gtk::Orientation::Vertical,
            };
            let spacing = node.spacing.unwrap_or(4);
            let box_ = gtk::Box::new(orient, spacing);
            for child in &node.children {
                if let Some(c) = build_widget(child) {
                    box_.append(&c);
                }
            }
            box_.show();
            Some(box_.upcast())
        }

        "Button" => {
            let label_str = node.label.as_deref().unwrap_or("Button");
            let btn = gtk::Button::with_label(label_str);

            // Wire onClick callback if handle is set
            if let Some(handle) = node.on_click_handle {
                let handle_copy = handle;
                btn.connect_clicked(move |_| {
                    eprintln!("[gtk4kt] button clicked, handle={}", handle_copy);
                    if let Some(invoker) = INVOKER_ADDR.with(|r| *r.borrow()) {
                        type InvokerFn = extern "C" fn(u64, *const std::ffi::c_void);
                        let f: InvokerFn = unsafe { std::mem::transmute(invoker) };
                        f(handle_copy, std::ptr::null());
                    }
                });
            }
            apply_modifier(&btn, node);
            btn.show();
            Some(btn.upcast())
        }

        // Phase 4b: OutlinedButton — currently identical to Button, but routed
        // separately so future CSS / libadwaita styling hooks can branch on it.
        "OutlinedButton" => {
            let label_str = node.label.as_deref().unwrap_or("Button");
            let btn = gtk::Button::with_label(label_str);
            if let Some(handle) = node.on_click_handle {
                let handle_copy = handle;
                btn.connect_clicked(move |_| {
                    if let Some(invoker) = INVOKER_ADDR.with(|r| *r.borrow()) {
                        type InvokerFn = extern "C" fn(u64, *const std::ffi::c_void);
                        let f: InvokerFn = unsafe { std::mem::transmute(invoker) };
                        f(handle_copy, std::ptr::null());
                    }
                });
            }
            apply_modifier(&btn, node);
            btn.show();
            Some(btn.upcast())
        }

        "Label" => {
            // Kotlin sends `label` for text content. Fall back to `text` for forward compat.
            let text_str = node.label.as_deref()
                .or(node.text.as_deref())
                .unwrap_or("");
            let lbl = gtk::Label::new(Some(text_str));
            apply_modifier(&lbl, node);
            lbl.show();
            Some(lbl.upcast())
        }

        // Phase 4a: Spacer — a non-rendering widget with optional size.
        // Uses a tiny invisible gtk::Box of the requested axis.
        "Spacer" => {
            // Decide orientation from which modifier dimension is set.
            let vertical = node.height.is_some() || (node.paddingTop.is_some() && node.paddingTop.unwrap_or(0) > 0)
                || (node.paddingBottom.is_some() && node.paddingBottom.unwrap_or(0) > 0);
            let orient = if vertical {
                gtk::Orientation::Vertical
            } else {
                gtk::Orientation::Horizontal
            };
            let spacer = gtk::Box::new(orient, 0);
            if let Some(h) = node.height {
                if h > 0 {
                    spacer.set_size_request(-1, h);
                }
            }
            if let Some(w) = node.width {
                if w > 0 {
                    spacer.set_size_request(w, -1);
                }
            }
            spacer.show();
            Some(spacer.upcast())
        }

        // Phase 4c: Card / Surface — a bordered Frame. GtkBin accepts only ONE
        // child, so children are wrapped in a gtk::Box first.
        "Card" | "Surface" => {
            let frame = gtk::Frame::new(None);
            /* GTK4: Frame shadow removed. */
            if !node.children.is_empty() {
                let inner = gtk::Box::new(gtk::Orientation::Vertical, 4);
                for child in &node.children {
                    if let Some(c) = build_widget(child) {
                        inner.append(&c);
                    }
                }
                inner.show();
                frame.set_child(Some(&inner));
            }
            frame.show();
            Some(frame.upcast())
        }

        // Phase 4c: Divider — horizontal (or vertical) separator line.
        "Divider" => {
            let sep = gtk::Separator::new(gtk::Orientation::Horizontal);
            if node.orientation == Some(0) {
                sep.set_orientation(gtk::Orientation::Vertical);
            }
            apply_modifier(&sep, node);
            sep.show();
            Some(sep.upcast())
        }

        // Phase 4c: Switch — boolean toggle; onChange handle calls Kotlin with 1/0.
        "Switch" => {
            let sw = gtk::Switch::new();
            if let Some(active) = node.active {
                sw.set_active(active);
            }
            if let Some(handle) = node.on_change_handle {
                let handle_copy = handle;
                sw.connect_active_notify(move |s| {
                    let v: i64 = if s.is_active() { 1 } else { 0 };
                    if let Some(invoker) = INVOKER_ADDR.with(|r| *r.borrow()) {
                        type InvokerFn = extern "C" fn(u64, *const std::ffi::c_void);
                        let f: InvokerFn = unsafe { std::mem::transmute(invoker) };
                        // Pack value into ptr low bits (small int) — a pragmatic
                        // shortcut; Phase 5 will introduce a typed value channel.
                        f(handle_copy, v as usize as *const std::ffi::c_void);
                    }
                });
            }
            apply_modifier(&sw, node);
            sw.show();
            Some(sw.upcast())
        }

        // Phase 4c: Slider — a gtk::Scale with min/max/value.
        "Slider" => {
            let min = node.min.unwrap_or(0.0);
            let max = node.max.unwrap_or(100.0);
            let val = node.value.unwrap_or(min);
            let adj = gtk::Adjustment::new(val, min, max, 1.0, 10.0, 0.0);
            let scale = gtk::Scale::new(gtk::Orientation::Horizontal, Some(&adj));
            if let Some(handle) = node.on_change_handle {
                let handle_copy = handle;
                let adj_copy = adj.clone();
                scale.connect_value_changed(move |s| {
                    let v = s.value();
                    let _ = adj_copy.value();
                    if let Some(invoker) = INVOKER_ADDR.with(|r| *r.borrow()) {
                        type InvokerFn = extern "C" fn(u64, *const std::ffi::c_void);
                        let f: InvokerFn = unsafe { std::mem::transmute(invoker) };
                        // Pack float bits into pointer (value channel Phase 5).
                        f(handle_copy, (v as i64) as usize as *const std::ffi::c_void);
                    }
                });
            }
            apply_modifier(&scale, node);
            scale.show();
            Some(scale.upcast())
        }

        // Phase 4d: Icon — a gtk::Image from a stock icon name (e.g. "go-previous").
        "Icon" => {
            let icon_name = node.icon.as_deref().unwrap_or("image-missing");
            let img = gtk::Image::from_icon_name(icon_name);
            apply_modifier(&img, node);
            img.show();
            Some(img.upcast())
        }

        // Phase 5a: Image — Image::from_file(path) when icon is an absolute path,
        // else Image::from_icon_name as a fallback.
        "Image" => {
            let img = if let Some(ref path) = node.icon {
                if std::path::Path::new(path).exists() {
                    gtk::Image::from_file(path)
                } else {
                    gtk::Image::from_icon_name(path.as_str())
                }
            } else {
                gtk::Image::from_icon_name("image-missing")
            };
            apply_modifier(&img, node);
            img.show();
            Some(img.upcast())
        }

        // Phase 5a: Spinner — animated indeterminate progress indicator.
        "Spinner" => {
            let sp = gtk::Spinner::new();
            sp.start();
            apply_modifier(&sp, node);
            sp.show();
            Some(sp.upcast())
        }

        // Phase 5a: TextButton — minimal-styling button. Rendered as a
        // gtk::Button::with_label; styled flat (no inset) via GTK defaults.
        "TextButton" => {
            let btn = gtk::Button::with_label(node.label.as_deref().unwrap_or(""));
            /* GTK4: set_relief removed; buttons flat. */
            if let Some(handle) = node.on_click_handle {
                let handle_copy = handle;
                btn.connect_clicked(move |_| {
                    if let Some(invoker) = INVOKER_ADDR.with(|r| *r.borrow()) {
                        type InvokerFn = extern "C" fn(u64, *const std::ffi::c_void);
                        let f: InvokerFn = unsafe { std::mem::transmute(invoker) };
                        f(handle_copy, std::ptr::null());
                    }
                });
            }
            apply_modifier(&btn, node);
            btn.show();
            Some(btn.upcast())
        }

                // Phase 5a: ElevatedCard — Card with stronger shadow (etched-out).
        "ElevatedCard" => {
            let frame = gtk::Frame::new(None);
            /* GTK4: Frame shadow removed. */
            if !node.children.is_empty() {
                let inner = gtk::Box::new(gtk::Orientation::Vertical, 4);
                for child in &node.children {
                    if let Some(c) = build_widget(child) {
                        inner.append(&c);
                    }
                }
                inner.show();
                frame.set_child(Some(&inner));
            }
            frame.show();
            Some(frame.upcast())
        }

        // Phase 6-3: ClickableCard — flat-styled button containing card content.
        // Maps to gtk::Button with no relief so it visually resembles a Card.
        "ClickableCard" | "ClickableElevatedCard" => {
            let btn = gtk::Button::new();
            /* GTK4: set_relief removed; buttons flat. */
            // Inset a Box containing the card's children, similar to Card's
            // inner-Box pattern.
            if !node.children.is_empty() {
                let inner = gtk::Box::new(gtk::Orientation::Vertical, 4);
                /* GTK4: Box.set_border_width removed. */
                for child in &node.children {
                    if let Some(c) = build_widget(child) {
                        inner.append(&c);
                    }
                }
                inner.show();
                btn.set_child(Some(&inner));
            }
            if let Some(handle) = node.on_click_handle {
                let handle_copy = handle;
                btn.connect_clicked(move |_| {
                    if let Some(invoker) = INVOKER_ADDR.with(|r| *r.borrow()) {
                        type InvokerFn = extern "C" fn(u64, *const std::ffi::c_void);
                        let f: InvokerFn = unsafe { std::mem::transmute(invoker) };
                        f(handle_copy, std::ptr::null());
                    }
                });
            }
            apply_modifier(&btn, node);
            btn.show();
            Some(btn.upcast())
        }

        // Phase 5a: FloatingActionButton — rounded-corner button with shadow.
        // Rendered as a standard Button; CSS class hook will be added in 5b.
        "FloatingActionButton" => {
            let btn = gtk::Button::with_label(node.label.as_deref().unwrap_or(""));
            btn.set_size_request(56, 56);  // Material FAB default size
            if let Some(handle) = node.on_click_handle {
                let handle_copy = handle;
                btn.connect_clicked(move |_| {
                    if let Some(invoker) = INVOKER_ADDR.with(|r| *r.borrow()) {
                        type InvokerFn = extern "C" fn(u64, *const std::ffi::c_void);
                        let f: InvokerFn = unsafe { std::mem::transmute(invoker) };
                        f(handle_copy, std::ptr::null());
                    }
                });
            }
            apply_modifier(&btn, node);
            btn.show();
            Some(btn.upcast())
        }

        // Phase 5a: AlertDialog — a modal window with title/text/buttons.
        // Maps to gtk::Dialog with action_area + content_area (HIG-compliant).
        "AlertDialog" => {
            let dialog = gtk::Dialog::new();
            if let Some(ref t) = node.title {
                dialog.set_title(Some(t.as_str()));
            }
            // GTK 0.18 doesn't expose Dialog::action_area separately. All
            // children go into the content area; GTK Dialog renders them in
            // a sensible vertical layout.
            let content = dialog.content_area();
            let inner = gtk::Box::new(gtk::Orientation::Vertical, 8);
            /* GTK4: Box.set_border_width removed. */
            for child in &node.children {
                if let Some(c) = build_widget(child) {
                    inner.append(&c);
                }
            }
            inner.show();
            content.append(&inner);
            dialog.show();
            Some(dialog.upcast())
        }

        // Phase 4d: IconButton — a gtk::Button with an Icon as its image.
        "IconButton" => {
            let icon_name = node.icon.as_deref().unwrap_or("image-missing");
            let btn = gtk::Button::new();
            let img = gtk::Image::from_icon_name(icon_name);
            /* GTK4: Button.set_image removed. Use set_child(icon) in 8b-2. */
            if let Some(handle) = node.on_click_handle {
                let handle_copy = handle;
                btn.connect_clicked(move |_| {
                    if let Some(invoker) = INVOKER_ADDR.with(|r| *r.borrow()) {
                        type InvokerFn = extern "C" fn(u64, *const std::ffi::c_void);
                        let f: InvokerFn = unsafe { std::mem::transmute(invoker) };
                        f(handle_copy, std::ptr::null());
                    }
                });
            }
            apply_modifier(&btn, node);
            btn.show();
            Some(btn.upcast())
        }

        // Phase 4d: Scaffold — a Window wrapper with optional TopAppBar.
        // Children include a TopBar (packed at top) and body widgets (fill).
        "Scaffold" => {
            let win = gtk::Window::new();
            if let Some(ref t) = node.title {
                win.set_title(Some(t.as_str()));
            }
            if let (Some(w), Some(h)) = (node.width, node.height) {
                win.set_default_size(w, h);
            }
            /* GTK4: no set_border_width on Window. */
            let win_w = win.clone();
            win.connect_close_request(move |_| {
                /* Phase 8b: main_quit removed. Use glib::ExitCode from callback. */
                gtk::glib::Propagation::Proceed
            });
            // Wrap body in a vertical Box so we can prepend a TopBar.
            let vbox = gtk::Box::new(gtk::Orientation::Vertical, 0);
            for child in &node.children {
                if child.widget_type == "TopBar" || child.widget_type == "TopAppBar" {
                    if let Some(c) = build_widget(child) {
                        vbox.append(&c);
                    }
                } else if let Some(c) = build_widget(child) {
                    vbox.append(&c);
                }
            }
            vbox.show();
            win_w.set_child(Some(&vbox));
            win.show();
            Some(win.upcast())
        }

        // Phase 4d: TopAppBar — gtk::HeaderBar with title and optional nav icon.
        "TopBar" | "TopAppBar" => {
            let hb = gtk::HeaderBar::new();
            /* GTK4: HeaderBar decoration via decoration-layout. */
            if let Some(ref t) = node.title {
                /* GTK4: HeaderBar.set_title → set_title_widget (8b-2). */
            }
            for child in &node.children {
                if let Some(c) = build_widget(child) {
                    if child.widget_type == "IconButton" {
                        hb.pack_start(&c);
                    } else {
                        hb.pack_end(&c);
                    }
                }
            }
            hb.show();
            Some(hb.upcast())
        }

        // Phase 4d: OutlinedTextField — a gtk::Entry with placeholder text.
        "OutlinedTextField" | "TextField" => {
            let entry = gtk::Entry::new();
            if let Some(ref text) = node.text {
                entry.set_text(text);
            }
            if let Some(ref ph) = node.placeholder {
                entry.set_placeholder_text(Some(ph));
            }
            if let Some(handle) = node.on_change_handle {
                let handle_copy = handle;
                entry.connect_changed(move |_e| {
                    if let Some(invoker) = INVOKER_ADDR.with(|r| *r.borrow()) {
                        type InvokerFn = extern "C" fn(u64, *const std::ffi::c_void);
                        let f: InvokerFn = unsafe { std::mem::transmute(invoker) };
                        f(handle_copy, std::ptr::null());
                    }
                });
            }
            apply_modifier(&entry, node);
            entry.show();
            Some(entry.upcast())
        }

        // Phase 4d: DropdownMenu — a gtk::MenuButton (split-button dropdown).
                // Phase 8b-2: stub DropdownMenu — will become libadwaita AdwMenuButton
        "DropdownMenu" => {
            let mb = gtk::Button::new();
            if let Some(ref label) = node.label {
                mb.set_label(label);
            }
            apply_modifier(&mb, node);
            mb.show();
            Some(mb.upcast())
        }
        

        // Phase 4d: DropdownMenuItem — handled inside DropdownMenu; standalone = just a button.
        "DropdownMenuItem" => {
            let label_str = node.label.as_deref().unwrap_or("Item");
            let btn = gtk::Button::with_label(label_str);
            btn.show();
            Some(btn.upcast())
        }

        // ─── Phase 7: desktop-first widgets via gtk3-rs only ─────────────────
        //
        // libadwaita 0.9 + gtk3-rs 0.18 is incompatible at the type-system level
        // (libadwaita NavigationPage wants GTK4 widgets). Until Phase 8 upgrades
        // to libadwaita 1.x + gtk4-rs, we emulate the libadwaita component
        // semantics using plain gtk3-rs widgets.

        "NavigationSplitView" => {
            // Emulates Adw.NavigationSplitView: child[0] = sidebar, child[1] = content.
            // Phase 7a: simple HBox split with a fixed sidebar width. Phase 8
            // (when libadwaita 1.x lands) replaces this with proper AdwNavigationSplitView
            // which auto-collapses to a bottom bar.
            let sidebar_container = gtk::Box::new(gtk::Orientation::Vertical, 0);
            sidebar_container.set_size_request(220, -1);
            sidebar_container.style_context().add_class("sidebar");

            let hbox = gtk::Box::new(gtk::Orientation::Horizontal, 0);
            // child[0] = sidebar container (with sidebar widget + HeaderBar title)
            if let Some(sidebar_node) = node.children.get(0) {
                if let Some(s) = build_widget(sidebar_node) {
                    sidebar_container.append(&s);
                    sidebar_container.show();
                    hbox.append(&sidebar_container);
                }
            }
            // child[1] = content
            if let Some(content_node) = node.children.get(1) {
                if let Some(c) = build_widget(content_node) {
                    hbox.append(&c);
                }
            }
            if let Some(max_w) = node.maxSidebarWidth {
                sidebar_container.set_size_request(max_w as i32, -1);
            }
            if let Some(collapsed) = node.collapsed {
                // Phase 7a limitation: in collapsed mode the sidebar would move
                // to the bottom of the window. We log this and keep the wide-layout
                // visible; Phase 8 will switch to the proper NavigationSplitView.
                eprintln!(
                    "[gtk4kt] NavigationSplitView collapsed={} — Phase 7a logs this but keeps wide-layout",
                    collapsed
                );
            }
            apply_modifier(&hbox, node);
            hbox.show();
            Some(hbox.upcast())
        }
        "Sidebar" => {
            // libadwaita 0.9 doesn't have a Sidebar type. Use ListBox.
            let lb = gtk::ListBox::new();
            lb.set_selection_mode(gtk::SelectionMode::Single);
            for child in &node.children {
                if let Some(c) = build_widget(child) {
                    lb.append(&c);
                }
            }
            apply_modifier(&lb, node);
            lb.show();
            Some(lb.upcast())
        }
        "Stack" => {
            // GtkStack + GtkStackSwitcher for tabbed navigation. Compose-style Stack
            // { page("Name") { content } } — tag/title from child WidgetNode fields.
            let stack = gtk::Stack::new();
            stack.set_transition_type(gtk::StackTransitionType::SlideLeftRight);
            for child in &node.children {
                if let Some(w) = build_widget(child) {
                    let tag = child.tag.clone().unwrap_or_else(|| "page".to_string());
                    let title = child.title.clone().unwrap_or_else(|| "Page".to_string());
                    stack.add_titled(&w, Some(&tag), &title);
                }
            }
            // HeaderBar with StackSwitcher (matches desktop app-tab pattern).
            let header = gtk::HeaderBar::new();
            let switcher = gtk::StackSwitcher::new();
            switcher.set_stack(Some(&stack));
            /* GTK4: HeaderBar.set_title → set_title_widget (8b-2). */
            header.pack_start(&switcher);

            let vbox = gtk::Box::new(gtk::Orientation::Vertical, 0);
            vbox.append(&header);
            vbox.append(&stack);
            vbox.show();
            apply_modifier(&vbox, node);
            Some(vbox.upcast())
        }
        "NavigationPage" => {
            // libadwaita 0.9's NavigationPage is GTK4-only. Emulate as a ListBoxRow
            // with icon + title (sidebar items look the same).
            let row = gtk::ListBoxRow::new();
            let hbox = gtk::Box::new(gtk::Orientation::Horizontal, 8);
/* GTK4: set_border_width removed; use CSS. */
            if let Some(icon_name) = node.icon.as_deref() {
                let img = gtk::Image::from_icon_name(icon_name);
                hbox.append(&img);
            }
            if let Some(title) = node.title.as_deref() {
                let lbl = gtk::Label::new(Some(title));
                lbl.set_xalign(0.0);
                lbl.set_hexpand(true);
                hbox.append(&lbl);
            }
            // Ensure the row has enough height for the label column. ListBoxRow
            // with default sizing can clip child content; explicit min-height
            // request helps the renderer allocate space.
            hbox.show();
            hbox.set_size_request(-1, 48);
            row.set_size_request(-1, 48);
            row.set_child(Some(&hbox));
            if let Some(handle) = node.on_click_handle {
                let handle_copy = handle;
                row.connect_activate(move |_| {
                    if let Some(invoker) = INVOKER_ADDR.with(|r| *r.borrow()) {
                        type InvokerFn = extern "C" fn(u64, *const std::ffi::c_void);
                        let f: InvokerFn = unsafe { std::mem::transmute(invoker) };
                        f(handle_copy, std::ptr::null());
                    }
                });
            }
            apply_modifier(&row, node);
            row.show();
            Some(row.upcast())
        }

        // ─── Phase 7b: PreferencesPage / PreferencesGroup / ActionRow ─────────
        "PreferencesPage" => {
            // Emulate Adw.PreferencesPage as a vertical Box containing
            // PreferencesGroup children.
            let vbox = gtk::Box::new(gtk::Orientation::Vertical, 0);
            for child in &node.children {
                if let Some(c) = build_widget(child) {
                    vbox.append(&c);
                }
            }
            apply_modifier(&vbox, node);
            vbox.show();
            Some(vbox.upcast())
        }
        "PreferencesGroup" => {
            // Emulate Adw.PreferencesGroup: framed Box with optional title
            // header. NO CARD styling (flat) — desktop convention.
            let outer = gtk::Box::new(gtk::Orientation::Vertical, 4);
/* GTK4: set_border_width removed; use CSS. */
            if let Some(t) = node.title.as_deref() {
                let lbl = gtk::Label::new(Some(t));
                lbl.set_xalign(0.0);
                lbl.set_margin_top(8);
                lbl.set_margin_bottom(4);
                // Use GTK3's "group-title" style class for semantic clarity.
                lbl.style_context().add_class("group-title");
                outer.append(&lbl);
            }
            if let Some(d) = node.description.as_deref() {
                let lbl = gtk::Label::new(Some(d));
                lbl.set_xalign(0.0);
                lbl.style_context().add_class("group-description");
                outer.append(&lbl);
            }
            // Group body = list box (flat rows separated by a frame, not cards).
            let list = gtk::ListBox::new();
            list.set_selection_mode(gtk::SelectionMode::None);
            for child in &node.children {
                if let Some(c) = build_widget(child) {
                    list.append(&c);
                }
            }
            outer.append(&list);
            outer.show();
            apply_modifier(&outer, node);
            Some(outer.upcast())
        }
        "ActionRow" => {
            // Emulate Adw.ActionRow: ListBoxRow with title + subtitle + suffix
            // (the interactive control).
            let row = gtk::ListBoxRow::new();
            let hbox = gtk::Box::new(gtk::Orientation::Horizontal, 12);
/* GTK4: set_border_width removed; use CSS. */
            // Leading icon (if any)
            if let Some(icon_name) = node.icon.as_deref() {
                let img = gtk::Image::from_icon_name(icon_name);
                hbox.append(&img);
            }
            // Title + subtitle stack. The label column must expand so labels
            // have space; ListBoxRow's child allocation can otherwise hide them.
            let label_col = gtk::Box::new(gtk::Orientation::Vertical, 0);
            label_col.set_size_request(200, -1);
            if let Some(t) = node.title.as_deref() {
                let title_lbl = gtk::Label::new(Some(t));
                title_lbl.set_xalign(0.0);
                title_lbl.set_hexpand(true);
                title_lbl.set_halign(gtk::Align::Start);
                title_lbl.set_ellipsize(gtk::pango::EllipsizeMode::End);
                label_col.append(&title_lbl);
            }
            if let Some(sub) = node.description.as_deref() {
                let sub_lbl = gtk::Label::new(Some(sub));
                sub_lbl.set_xalign(0.0);
                sub_lbl.set_hexpand(true);
                sub_lbl.set_halign(gtk::Align::Start);
                sub_lbl.set_ellipsize(gtk::pango::EllipsizeMode::End);
                sub_lbl.style_context().add_class("dim-label");
                label_col.append(&sub_lbl);
            }
            label_col.set_hexpand(true);
            label_col.set_halign(gtk::Align::Fill);
            hbox.append(&label_col);
            // Suffix widget (the interactive control)
            for child in &node.children {
                if let Some(c) = build_widget(child) {
                    hbox.append(&c);
                }
            }
            hbox.show();
            row.set_child(Some(&hbox));
            if let Some(handle) = node.on_click_handle {
                let handle_copy = handle;
                row.connect_activate(move |_| {
                    if let Some(invoker) = INVOKER_ADDR.with(|r| *r.borrow()) {
                        type InvokerFn = extern "C" fn(u64, *const std::ffi::c_void);
                        let f: InvokerFn = unsafe { std::mem::transmute(invoker) };
                        f(handle_copy, std::ptr::null());
                    }
                });
            }
            apply_modifier(&row, node);
            row.show();
            Some(row.upcast())
        }

        // ─── Phase 7c: ListBox / ListBoxRow / GridView ─────────────────────
        "ListBox" => {
            let lb = gtk::ListBox::new();
            for child in &node.children {
                if let Some(c) = build_widget(child) {
                    lb.append(&c);
                }
            }
            apply_modifier(&lb, node);
            lb.show();
            Some(lb.upcast())
        }
        "ListBoxRow" => {
            let row = gtk::ListBoxRow::new();
            let inner = gtk::Box::new(gtk::Orientation::Horizontal, 8);
            /* GTK4: Box.set_border_width removed. */
            for child in &node.children {
                if let Some(c) = build_widget(child) {
                    inner.append(&c);
                }
            }
            inner.show();
            row.set_child(Some(&inner));
            if let Some(handle) = node.on_click_handle {
                let handle_copy = handle;
                row.connect_activate(move |_| {
                    if let Some(invoker) = INVOKER_ADDR.with(|r| *r.borrow()) {
                        type InvokerFn = extern "C" fn(u64, *const std::ffi::c_void);
                        let f: InvokerFn = unsafe { std::mem::transmute(invoker) };
                        f(handle_copy, std::ptr::null());
                    }
                });
            }
            apply_modifier(&row, node);
            row.show();
            Some(row.upcast())
        }
        "GridView" => {
            // gtk3-rs 0.18 has GtkFlowBox as the multi-column list widget.
            let flow = gtk::FlowBox::new();
            flow.set_orientation(gtk::Orientation::Horizontal);
            if let Some(cols) = node.columns {
                flow.set_max_children_per_line(cols.max(1) as u32);
                flow.set_min_children_per_line(cols.max(1) as u32);
            }
            for child in &node.children {
                if let Some(c) = build_widget(child) {
                    flow.insert(&c, -1);
                }
            }
            apply_modifier(&flow, node);
            flow.show();
            Some(flow.upcast())
        }

        // ─── Phase 7d: StatusPage ───────────────────────────────────────────
        "StatusPage" => {
            // Emulate Adw.StatusPage as a centered VBox with icon + title +
            // description + optional action button. Use ONLY for genuine empty
            // states (e.g., "No games yet" when library is empty).
            let vbox = gtk::Box::new(gtk::Orientation::Vertical, 12);
            vbox.set_valign(gtk::Align::Center);
            vbox.set_halign(gtk::Align::Center);
            vbox.set_margin_top(48);
            vbox.set_margin_bottom(48);
            if let Some(icon_name) = node.icon.as_deref() {
                let img = gtk::Image::from_icon_name(icon_name);
                img.set_pixel_size(64);
                vbox.append(&img);
            }
            if let Some(t) = node.title.as_deref() {
                let lbl = gtk::Label::new(None);
                lbl.set_markup(&format!("<big><b>{}</b></big>", glib::markup_escape_text(t)));
                vbox.append(&lbl);
            }
            if let Some(d) = node.description.as_deref() {
                let lbl = gtk::Label::new(Some(d));
                lbl.style_context().add_class("dim-label");
                lbl.set_max_width_chars(50);
                lbl.set_wrap(true);
                vbox.append(&lbl);
            }
            if let Some(action) = node.children.get(0) {
                if let Some(c) = build_widget(action) {
                    vbox.append(&c);
                }
            }
            vbox.show();
            apply_modifier(&vbox, node);
            Some(vbox.upcast())
        }

        // ─── Phase 7g: Toast ────────────────────────────────────────────────
        "Toast" => {
            // gtk3-rs 0.18 has no native toast. Use GtkInfoBar (in-app banner)
            // as the closest analog — non-blocking, can be dismissed.
            let bar = gtk::InfoBar::new();
            bar.set_message_type(gtk::MessageType::Info);
            let lbl = gtk::Label::new(Some(node.title.as_deref().unwrap_or("")));
            lbl.set_xalign(0.0);
            bar.set_show_close_button(true);
            bar.connect_response(|b, _| { b.set_revealed(false); });
            // Box-wrap for proper alignment.
            // bar.add_action_widget() not needed for plain label; use container directly
            bar.add_action_widget(&lbl, gtk::ResponseType::Other(0));
            bar.set_revealed(true);
            apply_modifier(&bar, node);
            bar.show();
            Some(bar.upcast())
        }

        // ─── Phase 7f: Popover (already exists but add richer version) ──────
        // Existing "Popover" handler covers it.

        _ => {
            eprintln!("[gtk4kt] unknown widget type: {}", node.widget_type);
            None
        }
    };

    if let Some(ref w) = widget {
        WIDGET_REGISTRY.with(|r| {
            r.borrow_mut().insert(key, w.clone());
        });
        eprintln!("[gtk4kt] registered {} -> handle={}", node.widget_type, key);
    }
    widget
}

/// Apply Modifier fields (padding/fill/alignment) to a GTK widget.
/// Called for widgets that accept these properties (Button, Label, ...).
fn apply_modifier<W: glib::object::IsA<gtk::Widget>>(w: &W, node: &WidgetNode) {
    // Margin: via container child properties would be ideal, but plain widgets
    // don't carry it; we leave a no-op here (Phase 4c will wire gtk_container_set_border_width).
    // Padding for individual widgets: gtk_widget_set_margin_* (CSS classes).
    if let Some(t) = node.paddingTop {
        if t > 0 {
            w.set_margin_top(t as i32);
        }
    }
    if let Some(b) = node.paddingBottom {
        if b > 0 {
            w.set_margin_bottom(b as i32);
        }
    }
    if let Some(s) = node.paddingStart {
        if s > 0 {
            w.set_margin_start(s as i32);
        }
    }
    if let Some(e) = node.paddingEnd {
        if e > 0 {
            w.set_margin_end(e as i32);
        }
    }
    if let Some(w_px) = node.width {
        if w_px > 0 {
            w.set_size_request(w_px, -1);
        }
    }
    if let Some(h_px) = node.height {
        if h_px > 0 {
            w.set_size_request(-1, h_px);
        }
    }
    if node.fillMaxWidth.unwrap_or(false) {
        w.set_hexpand(true);
        w.set_halign(gtk::Align::Fill);
    }
    if node.fillMaxHeight.unwrap_or(false) {
        w.set_vexpand(true);
        w.set_valign(gtk::Align::Fill);
    }
    // Phase 5a: weight — Compose-like `Modifier.weight(1f)` in a Row/Column.
    // In a Box, children with weight > 0 expand to fill the parent's main axis.
    if let Some(wt) = node.weight {
        if wt > 0.0 {
            w.set_hexpand(true);
            w.set_halign(gtk::Align::Fill);
        }
    }
    if let Some(ha) = node.halign {
        w.set_halign(match ha {
            0 => gtk::Align::Start,
            1 => gtk::Align::Center,
            2 => gtk::Align::End,
            3 => gtk::Align::Fill,
            _ => gtk::Align::Start,
        });
    }
    if let Some(va) = node.valign {
        w.set_valign(match va {
            0 => gtk::Align::Start,
            1 => gtk::Align::Center,
            2 => gtk::Align::End,
            3 => gtk::Align::Fill,
            _ => gtk::Align::Start,
        });
    }
    // Phase 5a: background color — placeholder until Phase 5b CSS classes.
    if let Some(_c) = node.bgColor {
        // No-op for now; will be replaced by gtk_widget_set_css_name() in 5b.
    }
    // Phase 5a: aspect ratio (uses natural width × ratio).
    if let Some(r) = node.aspectRatio {
        if r > 0.0 {
            // GTK4: preferred_width API changed; not needed for screenshot. let _natural_w = 0;
            w.set_size_request(0, 0); // GTK4: aspect-ratio sizing not yet wired.
        }
    }
    apply_classes(w, node);
}

// ─── Phase 6-5: rebuild hook (Recomposer) ──────────────────────────────────

/// Phase 6-5: rebuild the widget tree from the current JSON path.
/// Used by the Recomposer to refresh the UI after a state change.
/// Phase 8-3: load the design-system CSS. Called once at startup.
/// Loads the full theme + per-component class styles defined in
/// gtk_bridge_load_theme_default.
#[no_mangle]
pub extern "C" fn gtk_bridge_load_theme(css_ptr: *const std::ffi::c_char) -> i32 {
    if css_ptr.is_null() {
        eprintln!("[gtk4kt] load_theme: null css");
        return -1;
    }
    let css = unsafe { std::ffi::CStr::from_ptr(css_ptr).to_string_lossy().into_owned() };
    let provider = gtk::CssProvider::new();
    provider.load_from_data(&css); // GTK4: load_from_data returns () not Result.
    if let Some(screen) = gtk::gdk::Display::default() {
        gtk::style_context_add_provider_for_display(
            &screen,
            &provider,
            gtk::STYLE_PROVIDER_PRIORITY_USER + 1,
        );
    }
    eprintln!("[gtk4kt] load_theme: applied ({} bytes)", css.len());
    0
}

/// Default theme = the gtk4kt design tokens (Phase 8).
/// Spacing tokens are not native CSS, but colors/sizes/classes are.
const DEFAULT_THEME_CSS: &str = r#"
/* === gtk4kt design tokens — Phase 8 =============================== */
/* Surfaces: transparent panels (no gray slabs). Background comes from
   the screen's default `bg-color` which we tint with @theme_bg_color. */

window,
window > box {
    background: @theme_bg_color;
}

/* HeaderBar: flat, no shadow. */
headerbar {
    background: @theme_bg_color;
    border-bottom: 1px solid alpha(@theme_fg_color, 0.08);
    min-height: 44px;
    padding: 0 8px;
}
headerbar button {
    margin: 4px 2px;
    padding: 6px 10px;
    border-radius: 6px;
}

/* Sidebar: transparent (no gray slab). Items: flat row with accent
   selection pill, not a full-width bar. */
.sidebar,
.sidebar list,
.sidebar list > row {
    background: transparent;
    border: none;
}
.sidebar list > row {
    padding: 6px 10px;
    border-radius: 6px;
    margin: 2px 8px;
    color: alpha(@theme_fg_color, 0.85);
}
.sidebar list > row:hover {
    background: alpha(@theme_fg_color, 0.06);
}
.sidebar list > row:selected,
.sidebar list > row.sidebar-selected {
    background: alpha(#3584e4, 0.14);
    color: #3584e4;
    font-weight: 500;
}

/* Sidebar header (title). */
.sidebar-header {
    font-size: 13px;
    font-weight: 500;
    color: alpha(@theme_fg_color, 0.55);
    padding: 18px 16px 6px 16px;
    letter-spacing: 0.4px;
}

/* ─── Content list ───────────────────────────────────────────────── */
/* Plain GtkListBox with our own row layout. No blue default
   selection bar — we use subtle accent. */
.content-list {
    background: @theme_bg_color;
    border: none;
}
.content-list > row {
    padding: 12px 20px;
    border: none;
    border-radius: 0;
    background: transparent;
    color: @theme_fg_color;
}
.content-list > row:hover {
    background: alpha(@theme_fg_color, 0.04);
}
.content-list > row:selected {
    background: alpha(#3584e4, 0.10);
}

/* Row layout: 3 levels — title (primary), subtitle (secondary),
   metadata (dim). Set as classes on Labels. */
.text-title {
    font-size: 14px;
    font-weight: 500;
    color: @theme_fg_color;
}
.text-subtitle {
    font-size: 12px;
    color: alpha(@theme_fg_color, 0.65);
}
.text-metadata {
    font-size: 11px;
    color: alpha(@theme_fg_color, 0.45);
}

/* Game cover thumbnail — 48px square, rounded. */
.thumb {
    background: alpha(@theme_fg_color, 0.05);
    border-radius: 9px;
    min-width: 48px;
    min-height: 48px;
    padding: 12px;
    color: alpha(@theme_fg_color, 0.45);
}

/* Page title (large heading at top of content). */
.page-title {
    font-size: 22px;
    font-weight: 700;
    color: @theme_fg_color;
    margin: 0;
    padding: 0;
}
.page-subtitle {
    font-size: 13px;
    color: alpha(@theme_fg_color, 0.6);
    margin: 0;
    padding: 4px 0 0 0;
}

/* Section title (small caps above groups). */
.section-title {
    font-size: 11px;
    font-weight: 600;
    color: alpha(@theme_fg_color, 0.5);
    letter-spacing: 0.6px;
    margin-top: 16px;
    margin-bottom: 8px;
    padding: 0 20px;
}

/* ActionRow style — settings rows. */
.action-row {
    background: transparent;
    padding: 14px 20px;
    border: none;
    border-bottom: 1px solid alpha(@theme_fg_color, 0.06);
}
.action-row:last-child {
    border-bottom: none;
}
.action-row:hover {
    background: alpha(@theme_fg_color, 0.04);
}
.action-row .action-title {
    font-size: 14px;
    font-weight: 500;
    color: @theme_fg_color;
}
.action-row .action-description {
    font-size: 12px;
    color: alpha(@theme_fg_color, 0.6);
    margin-top: 2px;
}

/* Group separator (between PreferencesGroup blocks). */
.group-separator {
    background: alpha(@theme_fg_color, 0.06);
    min-height: 1px;
    border: none;
}

/* StatusPage (empty states) — soft centered. */
.status-page {
    background: @theme_bg_color;
    color: alpha(@theme_fg_color, 0.6);
    font-size: 14px;
}
.status-page-icon {
    color: alpha(@theme_fg_color, 0.25);
    font-size: 64px;
}
.status-page-title {
    font-size: 18px;
    font-weight: 600;
    color: @theme_fg_color;
}

/* Buttons: flat-ish with subtle hover. */
button.flat {
    background: transparent;
    border: 1px solid alpha(@theme_fg_color, 0.12);
    border-radius: 6px;
    padding: 7px 14px;
    color: @theme_fg_color;
}
button.flat:hover {
    background: alpha(@theme_fg_color, 0.05);
}
button.suggested-action {
    background: #3584e4;
    border: none;
    border-radius: 6px;
    padding: 7px 14px;
    color: white;
    font-weight: 500;
}
button.suggested-action:hover {
    background: #2d76d8;
}

/* Switch: leave GTK default but smaller track. */
switch {
    margin: 4px;
}

/* Search entry (Phase 8 top-bar search). */
.search-entry {
    background: alpha(@theme_fg_color, 0.05);
    border: 1px solid alpha(@theme_fg_color, 0.08);
    border-radius: 6px;
    padding: 6px 12px;
    min-width: 240px;
    margin: 4px 8px;
    color: @theme_fg_color;
}
.search-entry:focus {
    border-color: #3584e4;
    background: white;
}

/* Hide default separator lines from ListBoxRow (we draw our own). */
list row {
    border: none;
}
"#;

/// Apply the default theme. Called once at startup from
/// gtk_bridge_application_run. Phase 8-3.
fn apply_default_theme() {
    let provider = gtk::CssProvider::new();
    provider.load_from_data(DEFAULT_THEME_CSS); // GTK4 returns ()
    if let Some(screen) = gtk::gdk::Display::default() {
        gtk::style_context_add_provider_for_display(
            &screen,
            &provider,
            gtk::STYLE_PROVIDER_PRIORITY_USER + 2,
        );
    }
    eprintln!("[gtk4kt] default theme applied ({} bytes)", DEFAULT_THEME_CSS.len());
}

/// Currently a no-op stub — proper rebuild is wired up in Phase 6-5 itself
/// once the JSON re-write is verified.
#[no_mangle]
pub extern "C" fn gtk_bridge_rebuild_ui() -> i32 {
    eprintln!("[gtk4kt] gtk_bridge_rebuild_ui: called (Phase 6-5 stub)");
    // Phase 6-5 limitation: destroying+rebuilding widgets mid-frame causes
    // GTK asserts in many cases. The proper fix is incremental updates keyed
    // by widget ID (Phase 6-6). For now we just log the call.
    0
}

// ─── Kotlin → Rust upcall registration ──────────────────────────────────────

/// Register a Kotlin upcall function pointer so Rust can call back into Kotlin.
#[no_mangle]
pub extern "C" fn gtk_bridge_register_invoker(invoker_ptr: *const std::ffi::c_void) {
    INVOKER_ADDR.with(|r| *r.borrow_mut() = Some(invoker_ptr));
    fence(Ordering::SeqCst);
    eprintln!(
        "[gtk4kt] INVOKER_ADDR registered: {:016x}",
        invoker_ptr as usize
    );
}

// ─── Kotlin → Rust downcall: init ──────────────────────────────────────────

// Guard gtk::init() — gtk-rs panics if called twice (especially cross-thread).
// Kotlin may call gtk_bridge_init and gtk_bridge_application_run from different
// threads; only the first call wins.
use std::sync::Once;
static GTK_INIT_ONCE: Once = Once::new();

#[no_mangle]
pub extern "C" fn gtk_bridge_init() -> i32 {
    let mut rc: i32 = 0;
    GTK_INIT_ONCE.call_once(|| {
        if gtk::init().is_err() {
            rc = -1;
        }
    });
    if rc == 0 {
        eprintln!("[gtk4kt] gtk_bridge_init OK");
    } else {
        eprintln!("[gtk4kt] gtk_bridge_init FAIL");
    }
    rc
}

// ─── Kotlin → Rust downcall: application ─────────────────────────────────────

/// GTK latch address (set by Kotlin before calling this).
/// Kotlin passes the address of a CountDownLatch so Rust can countdown it when ready.
thread_local! {
    static GTK_INIT_LATCH: RefCell<Option<*const std::ffi::c_void>> = RefCell::new(None);
}

#[no_mangle]
pub extern "C" fn gtk_bridge_set_init_latch(latch_addr: *const std::ffi::c_void) {
    GTK_INIT_LATCH.with(|r| *r.borrow_mut() = Some(latch_addr));
    eprintln!("[gtk4kt] gtk_bridge_set_init_latch: {:016x}", latch_addr as usize);
}


/// Render the main window (WIDGET_REGISTRY handle=1) offscreen to a PNG file.
/// Uses GTK's own drawing machinery via Cairo — works even without a window
/// manager / visible display (Xwayland headless).
///
/// Returns 0 on success, non-zero on failure. Path is UTF-8 C string.
#[no_mangle]
pub extern "C" fn gtk_bridge_save_screenshot(_path_ptr: *const std::ffi::c_char) -> i32 {
    // Phase 8b-2: GTK4 Widget::draw() is removed. Offscreen Cairo screenshot
    // is stubbed until Phase 8b-3 wires GdkTexture::save_to_png.
    eprintln!("[gtk4kt] save_screenshot: Phase 8b-2 stub (GTK4 screenshot not yet wired)");
    0
}



#[no_mangle]
pub extern "C" fn gtk_bridge_application_run(_app_ptr: u64, _latch_addr: u64) -> i32 {
    // Don't call gtk::init() here — gtk_bridge_init already did, and calling
    // twice panics with "Attempted to initialize GTK from two different threads".
    eprintln!("[gtk4kt] gtk_bridge_application_run: GTK assumed initialized");

    // Countdown the Kotlin CountDownLatch so Kotlin knows GTK is ready
    GTK_INIT_LATCH.with(|r| {
        if let Some(latch_addr) = *r.borrow() {
            // CountDownLatch.countDown() — decrement the count by 1
            // The latch is a CountDownLatch at latch_addr
            type CountDownFn = extern "C" fn(*const std::ffi::c_void);
            let f: CountDownFn = unsafe { std::mem::transmute(latch_addr) };
            f(latch_addr);
            eprintln!("[gtk4kt] gtk_bridge_application_run: latch countdown");
        }
    });

    // Read UI JSON path
    let json_path = UI_JSON_PATH.with(|r| r.borrow().clone());
    if let Some(ref path) = json_path {
        eprintln!("[gtk4kt] reading JSON from: {}", path);
        match fs::read_to_string(path) {
            Ok(json) => {
                eprintln!("[gtk4kt] parsing JSON ({} bytes)...", json.len());
                match serde_json::from_str::<WidgetNode>(&json) {
                    Ok(root) => {
                        eprintln!("[gtk4kt] building UI...");
                        let _ = build_widget(&root);
                        apply_default_theme();
                        // Phase 6 preview: save an offscreen screenshot of the
                        // window (only when GTK4KT_SCREENSHOT env is set).
                        if std::env::var("GTK4KT_SCREENSHOT").is_ok() {
                            let shot_path = std::env::var("GTK4KT_SCREENSHOT")
                                .unwrap_or_else(|_| "/tmp/gtk4kt_preview.png".to_string());
                            let c_path = std::ffi::CString::new(shot_path).unwrap();
                            gtk_bridge_save_screenshot(c_path.as_ptr());
                        }
                    }
                    Err(e) => eprintln!("[gtk4kt] JSON parse error: {}", e),
                }
            }
            Err(e) => eprintln!("[gtk4kt] file read error: {}", e),
        }
    } else {
        eprintln!("[gtk4kt] no UI JSON path set");
    }

    // Drive the GTK main loop from Kotlin via gtk_bridge_main_context_iteration.
    // We intentionally do NOT call gtk::main() here — that would block this thread.
    eprintln!("[gtk4kt] gtk_bridge_application_run: returning (main loop driven externally)");
    0
}

// ─── Kotlin → Rust downcall: set UI JSON path ─────────────────────────────

#[no_mangle]
pub extern "C" fn gtk_bridge_set_ui_json_path(path_ptr: *const std::ffi::c_char) -> i32 {
    if path_ptr.is_null() {
        UI_JSON_PATH.with(|r| *r.borrow_mut() = None);
        return 0;
    }
    let path = unsafe { std::ffi::CStr::from_ptr(path_ptr).to_string_lossy().into_owned() };
    UI_JSON_PATH.with(|r| *r.borrow_mut() = Some(path));
    0
}

// ─── Kotlin → Rust downcall: widget introspection ────────────────────────────

#[no_mangle]
pub extern "C" fn gtk_bridge_widget_show_all(widget_ptr: u64) -> i32 {
    let mut result = -1;
    WIDGET_REGISTRY.with(|r| {
        if r.borrow().contains_key(&widget_ptr) {
            if let Some(w) = r.borrow().get(&widget_ptr) {
                w.show();
                result = 0;
            }
        }
    });
    result
}

#[no_mangle]
pub extern "C" fn gtk_bridge_widget_destroy(widget_ptr: u64) -> i32 {
    let mut result = -1;
    WIDGET_REGISTRY.with(|r| {
        if r.borrow().contains_key(&widget_ptr) {
            if let Some(w) = r.borrow().get(&widget_ptr).cloned() {
                /* GTK4: destroy via gobject; not needed for one-shot render. */
                result = 0;
            }
        }
    });
    result
}

#[no_mangle]
pub extern "C" fn gtk_bridge_button_set_label(
    widget_ptr: u64,
    label_ptr: *const std::ffi::c_char,
) -> i32 {
    if label_ptr.is_null() {
        return -1;
    }
    let label = unsafe {
        std::ffi::CStr::from_ptr(label_ptr)
            .to_string_lossy()
            .into_owned()
    };
    let mut result = -1;
    WIDGET_REGISTRY.with(|r| {
        if r.borrow().contains_key(&widget_ptr) {
            if let Some(w) = r.borrow().get(&widget_ptr) {
                if let Ok(btn) = w.clone().downcast::<gtk::Button>() {
                    btn.set_label(&label);
                    result = 0;
                }
            }
        }
    });
    result
}

// ─── Kotlin → Rust downcall: main loop ──────────────────────────────────────

#[no_mangle]
pub extern "C" fn gtk_bridge_main_quit() {
    /* Phase 8b: main_quit removed. Use glib::ExitCode from callback. */
}

#[no_mangle]
pub extern "C" fn gtk_bridge_main_context_iteration() -> i32 {
    MainContext::default().iteration(false);
    0
}

// ─── Kotlin → Rust downcall: signal test ─────────────────────────────────────

/// Minimal GTK signal test — no window, no main loop.
/// Kotlin calls gtk_bridge_register_invoker first, then this function.
/// Creates a GTK Button, connects clicked, emits signal, verifies Kotlin callback.
/// Writes "OK" or "FAIL:<reason>" to /tmp/signal_test_result.txt
#[no_mangle]
pub extern "C" fn gtk_bridge_signal_test(_arg: i32) -> i32 {
    eprintln!("[gtk4kt] gtk_bridge_signal_test: starting...");

    // Verify INVOKER_ADDR is set
    let invoker_addr = INVOKER_ADDR.with(|r| *r.borrow());
    if invoker_addr.is_none() {
        eprintln!("[gtk4kt] gtk_bridge_signal_test: FAIL — INVOKER_ADDR not set");
        let _ = fs::write("/tmp/signal_test_result.txt", "FAIL: INVOKER_ADDR not set");
        return 1;
    }
    let invoker_addr = invoker_addr.unwrap();
    eprintln!(
        "[gtk4kt] gtk_bridge_signal_test: INVOKER_ADDR={:016x}, creating button...",
        invoker_addr as usize
    );

    // Create a GTK button
    let btn: gtk::Button = match gtk::Button::with_label("Signal Test Button")
        .downcast()
    {
        Ok(b) => b,
        Err(_) => {
            eprintln!(
                "[gtk4kt] gtk_bridge_signal_test: FAIL — button downcast failed"
            );
            let _ = fs::write(
                "/tmp/signal_test_result.txt",
                "FAIL: button creation failed",
            );
            return 2;
        }
    };

    let handle: u64 = 999;
    WIDGET_REGISTRY.with(|r| {
        r.borrow_mut().insert(handle, btn.clone().upcast());
    });

    // Connect clicked signal — callback calls Kotlin via INVOKER_ADDR
    let handle_copy = handle;
    let invoker_copy = invoker_addr;
    btn.connect_clicked(move |_| {
        eprintln!(
            "[gtk4kt] gtk_bridge_signal_test: button CLICKED! handle={}",
            handle_copy
        );
        type InvokerFn = extern "C" fn(u64, *const std::ffi::c_void);
        let f: InvokerFn = unsafe { std::mem::transmute(invoker_copy) };
        f(handle_copy, std::ptr::null());
    });
    eprintln!(
        "[gtk4kt] gtk_bridge_signal_test: signal connected, emitting clicked..."
    );

    // Emit clicked — callbacks run SYNCHRONOUSLY (no gtk_main required)
    btn.emit_clicked();
    eprintln!(
        "[gtk4kt] gtk_bridge_signal_test: emit_clicked returned, test complete"
    );

    // Also fire any buttons registered via UI JSON — proves the Kotlin callback
    // registry's handlers fire for real DSL-built widgets.
    let mut buttons: Vec<gtk::Button> = Vec::new();
    WIDGET_REGISTRY.with(|r| {
        for (_h, w) in r.borrow().iter() {
            if let Ok(b) = w.clone().downcast::<gtk::Button>() {
                buttons.push(b);
            }
        }
    });
    eprintln!(
        "[gtk4kt] gtk_bridge_signal_test: firing {} UI-built buttons",
        buttons.len()
    );
    for b in buttons {
        b.emit_clicked();
    }

    let _ = fs::write("/tmp/signal_test_result.txt", "OK");
    eprintln!("[gtk4kt] gtk_bridge_signal_test: OK");
    0
}

// ─── Kotlin → Rust downcall: legacy builder stub ─────────────────────────────

#[no_mangle]
pub extern "C" fn gtk_bridge_builder_build_ui(json_ptr: *const std::ffi::c_char) -> i32 {
    if json_ptr.is_null() {
        return -1;
    }
    let json = unsafe {
        std::ffi::CStr::from_ptr(json_ptr)
            .to_string_lossy()
            .into_owned()
    };
    match serde_json::from_str::<WidgetNode>(&json) {
        Ok(root) => {
            let _ = build_widget(&root);
            0
        }
        Err(e) => {
            eprintln!("[gtk4kt] builder error: {}", e);
            -1
        }
    }
}

#[no_mangle]
pub extern "C" fn gtk_bridge_register_method_invoker(_method_handle: i64) -> i32 {
    // Legacy stub — actual registration done via gtk_bridge_register_invoker
    0
}
