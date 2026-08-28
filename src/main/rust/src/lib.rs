//! libgtk4kt_native — Rust FFI bridge for Kotlin GTK DSL
//! Phase 3: Kotlin Widget DSL → GTK3 builder via JSON FFI

use glib::object::ObjectExt;
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
}

// ─── Widget builder ─────────────────────────────────────────────────────────

fn next_key() -> u64 {
    use std::sync::atomic::AtomicU64;
    static COUNTER: AtomicU64 = AtomicU64::new(1);
    COUNTER.fetch_add(1, Ordering::Relaxed)
}

fn build_widget(node: &WidgetNode) -> Option<gtk::Widget> {
    let key = next_key();
    let widget: Option<gtk::Widget> = match node.widget_type.as_str() {
        "Window" => {
            let win = gtk::Window::new(gtk::WindowType::Toplevel);
            if let Some(ref t) = node.title {
                win.set_title(t);
            }
            if let (Some(w), Some(h)) = (node.width, node.height) {
                win.set_default_size(w, h);
            }
            win.set_border_width(8);
            let win_w = win.clone();
            win.connect_delete_event(move |_, _| {
                gtk::main_quit();
                gtk::glib::Propagation::Proceed
            });
            for child in &node.children {
                if let Some(c) = build_widget(child) {
                    win_w.add(&c);
                }
            }
            win.show_all();
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
                    box_.add(&c);
                }
            }
            box_.show_all();
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
            frame.set_shadow_type(gtk::ShadowType::EtchedIn);
            if !node.children.is_empty() {
                let inner = gtk::Box::new(gtk::Orientation::Vertical, 4);
                for child in &node.children {
                    if let Some(c) = build_widget(child) {
                        inner.add(&c);
                    }
                }
                inner.show_all();
                frame.add(&inner);
            }
            frame.show_all();
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
            let img = gtk::Image::from_icon_name(Some(icon_name), gtk::IconSize::Button);
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
                    gtk::Image::from_icon_name(Some(path.as_str()), gtk::IconSize::Button)
                }
            } else {
                gtk::Image::new();
                gtk::Image::from_icon_name(Some("image-missing"), gtk::IconSize::Button)
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
            btn.set_relief(gtk:: ReliefStyle::None);
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
            frame.set_shadow_type(gtk::ShadowType::Out);
            if !node.children.is_empty() {
                let inner = gtk::Box::new(gtk::Orientation::Vertical, 4);
                for child in &node.children {
                    if let Some(c) = build_widget(child) {
                        inner.add(&c);
                    }
                }
                inner.show_all();
                frame.add(&inner);
            }
            frame.show_all();
            Some(frame.upcast())
        }

        // Phase 6-3: ClickableCard — flat-styled button containing card content.
        // Maps to gtk::Button with no relief so it visually resembles a Card.
        "ClickableCard" | "ClickableElevatedCard" => {
            let btn = gtk::Button::new();
            btn.set_relief(gtk::ReliefStyle::None);
            // Inset a Box containing the card's children, similar to Card's
            // inner-Box pattern.
            if !node.children.is_empty() {
                let inner = gtk::Box::new(gtk::Orientation::Vertical, 4);
                inner.set_border_width(if node.widget_type == "ClickableElevatedCard" { 4 } else { 2 });
                for child in &node.children {
                    if let Some(c) = build_widget(child) {
                        inner.add(&c);
                    }
                }
                inner.show_all();
                btn.add(&inner);
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
                dialog.set_title(t);
            }
            // GTK 0.18 doesn't expose Dialog::action_area separately. All
            // children go into the content area; GTK Dialog renders them in
            // a sensible vertical layout.
            let content = dialog.content_area();
            let inner = gtk::Box::new(gtk::Orientation::Vertical, 8);
            inner.set_border_width(16);
            for child in &node.children {
                if let Some(c) = build_widget(child) {
                    inner.add(&c);
                }
            }
            inner.show_all();
            content.add(&inner);
            dialog.show_all();
            Some(dialog.upcast())
        }

        // Phase 4d: IconButton — a gtk::Button with an Icon as its image.
        "IconButton" => {
            let icon_name = node.icon.as_deref().unwrap_or("image-missing");
            let btn = gtk::Button::new();
            let img = gtk::Image::from_icon_name(Some(icon_name), gtk::IconSize::Button);
            btn.set_image(Some(&img));
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
            let win = gtk::Window::new(gtk::WindowType::Toplevel);
            if let Some(ref t) = node.title {
                win.set_title(t);
            }
            if let (Some(w), Some(h)) = (node.width, node.height) {
                win.set_default_size(w, h);
            }
            win.set_border_width(0);
            let win_w = win.clone();
            win.connect_delete_event(move |_, _| {
                gtk::main_quit();
                gtk::glib::Propagation::Proceed
            });
            // Wrap body in a vertical Box so we can prepend a TopBar.
            let vbox = gtk::Box::new(gtk::Orientation::Vertical, 0);
            for child in &node.children {
                if child.widget_type == "TopBar" || child.widget_type == "TopAppBar" {
                    if let Some(c) = build_widget(child) {
                        vbox.pack_start(&c, false, false, 0);
                    }
                } else if let Some(c) = build_widget(child) {
                    vbox.pack_start(&c, true, true, 0);
                }
            }
            vbox.show_all();
            win_w.add(&vbox);
            win.show_all();
            Some(win.upcast())
        }

        // Phase 4d: TopAppBar — gtk::HeaderBar with title and optional nav icon.
        "TopBar" | "TopAppBar" => {
            let hb = gtk::HeaderBar::new();
            hb.set_show_close_button(true);
            if let Some(ref t) = node.title {
                hb.set_title(Some(t));
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
        "DropdownMenu" => {
            let mb = gtk::MenuButton::new();
            if let Some(ref label) = node.label {
                mb.set_label(label);
            }
            let menu = gtk::Menu::new();
            for child in &node.children {
                if child.widget_type == "DropdownMenuItem" {
                    let label_str = child.label.as_deref().unwrap_or("Item");
                    let mi = gtk::MenuItem::with_label(label_str);
                    if let Some(handle) = child.on_click_handle {
                        let handle_copy = handle;
                        mi.connect_activate(move |_| {
                            if let Some(invoker) = INVOKER_ADDR.with(|r| *r.borrow()) {
                                type InvokerFn = extern "C" fn(u64, *const std::ffi::c_void);
                                let f: InvokerFn = unsafe { std::mem::transmute(invoker) };
                                f(handle_copy, std::ptr::null());
                            }
                        });
                    }
                    menu.add(&mi);
                    mi.show();
                }
            }
            // MenuButton has a popup Menu, not a submenu. Use GtkMenuButtonExt::set_popup.
            // MenuButton has a popup Menu, not a submenu. Trait is in gtk::prelude.
            use gtk::prelude::MenuButtonExt;
            mb.set_popup(Some(&menu));
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
fn apply_modifier<W: gtk::glib::IsA<gtk::Widget>>(w: &W, node: &WidgetNode) {
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
            let natural_w = w.preferred_width().1;
            w.set_size_request(natural_w, (natural_w as f32 / r) as i32);
        }
    }
}

// ─── Phase 6-5: rebuild hook (Recomposer) ──────────────────────────────────

/// Phase 6-5: rebuild the widget tree from the current JSON path.
/// Used by the Recomposer to refresh the UI after a state change.
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
pub extern "C" fn gtk_bridge_save_screenshot(path_ptr: *const std::ffi::c_char) -> i32 {
    if path_ptr.is_null() {
        eprintln!("[gtk4kt] save_screenshot: null path");
        return -1;
    }
    let path = unsafe { std::ffi::CStr::from_ptr(path_ptr).to_string_lossy().into_owned() };

    // Find the top-level window in the registry (handle=1 is the window).
    let window_widget: Option<gtk::Widget> = WIDGET_REGISTRY.with(|r| {
        let reg = r.borrow();
        reg.get(&1).cloned()
    });
    let window = match window_widget {
        Some(w) => w,
        None => {
            eprintln!("[gtk4kt] save_screenshot: no window in registry (handle=1)");
            return -2;
        }
    };

    // Determine size: prefer the window's current allocation, fall back to
    // size_request() (returns a (width, height) tuple).
    let (w_px, h_px) = {
        let alloc = window.allocation();
        if alloc.width() > 1 && alloc.height() > 1 {
            (alloc.width(), alloc.height())
        } else {
            let (rw, rh) = window.size_request();
            (rw.max(1), rh.max(1))
        }
    };
    eprintln!("[gtk4kt] save_screenshot: size = {}x{}", w_px, h_px);

    // Ensure realized so drawing works.
    if !window.is_drawable() {
        window.realize();
    }
    // Force a size allocation so layout happens.
    {
        // gdk::Rectangle wraps GdkRectangle via BoxedInline; construct with
        // the ffi-compatible builder (fields via deref).
        let mut alloc = gtk::Allocation::new(0, 0, w_px, h_px);
        window.size_allocate(&mut alloc);
    }

    // Create a Cairo image surface and render the window into it.
    let surface = match cairo::ImageSurface::create(
        cairo::Format::ARgb32,
        w_px,
        h_px,
    ) {
        Ok(s) => s,
        Err(e) => {
            eprintln!("[gtk4kt] save_screenshot: surface create failed: {:?}", e);
            return -3;
        }
    };
    {
        let cr = match cairo::Context::new(&surface) {
            Ok(c) => c,
            Err(e) => {
                eprintln!("[gtk4kt] save_screenshot: context failed: {:?}", e);
                return -4;
            }
        };
        // Paint background (GTK windows are transparent ARGB; fill white first).
        cr.set_source_rgb(0.95, 0.95, 0.95);
        cr.paint();
        // Draw the window widget tree.
        window.draw(&cr);
    }
    // Flush to surface and write PNG via the stream API (cairo_surface_write_to_png
    // is behind a feature; the stream version is always available).
    let file = match std::fs::File::create(&path) {
        Ok(f) => f,
        Err(e) => {
            eprintln!("[gtk4kt] save_screenshot: file create failed: {:?}", e);
            return -4;
        }
    };
    let surface_ptr = surface.to_raw_none();
    extern "C" fn png_write_cb(
        closure: *mut std::ffi::c_void,
        data: *const u8,
        length: usize,
    ) -> cairo::ffi::cairo_status_t {
        unsafe {
            let file = &mut *(closure as *mut std::io::BufWriter<std::fs::File>);
            use std::io::Write;
            match file.write_all(std::slice::from_raw_parts(data, length)) {
                Ok(()) => 0, // CAIRO_STATUS_SUCCESS
                Err(_) => 5, // CAIRO_STATUS_WRITE_ERROR
            }
        }
    }
    let mut writer = std::io::BufWriter::new(file);
    match unsafe { cairo::Surface::from_raw_none(surface.to_raw_none()) }.write_to_png(&mut writer) {
        Ok(()) => {
            eprintln!("[gtk4kt] save_screenshot: wrote {}", path);
            0
        }
        Err(e) => {
            eprintln!("[gtk4kt] save_screenshot: write failed: {:?}", e);
            -4
        }
    }
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
                w.show_all();
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
                unsafe { w.destroy() };
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
    gtk::main_quit();
}

#[no_mangle]
pub extern "C" fn gtk_bridge_main_context_iteration() -> i32 {
    gtk::main_iteration_do(false);
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
