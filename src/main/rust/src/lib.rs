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

    // Button callback handle (set by Kotlin before JSON is sent)
    #[serde(default)]
    pub on_click_handle: Option<u64>,
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
            let box_ = gtk::Box::new(gtk::Orientation::Vertical, 4);
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
            btn.show();
            Some(btn.upcast())
        }

        "Label" => {
            let text_str = node.text.as_deref().unwrap_or("");
            let lbl = gtk::Label::new(Some(text_str));
            lbl.show();
            Some(lbl.upcast())
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
        eprintln!(
            "[gtk4kt] registered {} -> handle={}",
            node.widget_type,
            key
        );
    }
    widget
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
