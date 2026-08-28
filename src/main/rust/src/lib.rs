//! gtk4kt-native — Rust cdylib: Kotlin DSL → GTK3 via Panama FFI

use std::cell::RefCell;
use std::collections::HashMap;
use std::ffi::{c_char, c_int};
use std::fs;
use std::path::PathBuf;
use std::time::{SystemTime, UNIX_EPOCH};

use gtk::prelude::*;
use serde::Deserialize;

thread_local! {
    static LAST_BUTTON_PTR: RefCell<Option<u64>> = RefCell::new(None);
    static APP_REGISTRY: RefCell<HashMap<u64, gtk::Application>> = RefCell::new(HashMap::new());
    static WIDGET_REGISTRY: RefCell<HashMap<u64, gtk::Widget>> = RefCell::new(HashMap::new());
    static CALLBACK_REGISTRY: RefCell<HashMap<u64, i64>> = RefCell::new(HashMap::new());
    static UI_JSON_PATH: RefCell<Option<PathBuf>> = RefCell::new(None);
    static INVOKER_ADDR: RefCell<Option<*const std::ffi::c_void>> = RefCell::new(None);
}

#[derive(Debug, Deserialize)]
struct WidgetNode {
    #[serde(rename = "type")]
    widget_type: String,
    #[serde(default)]
    id: Option<String>,
    #[serde(default)]
    label: Option<String>,
    #[serde(default)]
    children: Option<Vec<WidgetNode>>,
    #[serde(rename = "on_click", default)]
    on_click: Option<i64>,
    #[serde(default)]
    spacing: Option<i32>,
    #[serde(default)]
    orientation: Option<i32>,
    #[serde(default)]
    width: Option<i32>,
    #[serde(default)]
    height: Option<i32>,
}

fn next_key() -> u64 {
    SystemTime::now().duration_since(UNIX_EPOCH).unwrap().as_nanos() as u64
}

fn build_widget(node: &WidgetNode) -> Option<u64> {
    match node.widget_type.as_str() {
        "Window" => {
            let window = gtk::Window::new(gtk::WindowType::Toplevel);
            let key = next_key();
            WIDGET_REGISTRY.with(|r| r.borrow_mut().insert(key, window.clone().upcast()));
            if let Some(ref title) = node.label {
                window.set_title(title);
            }
            let w = node.width.unwrap_or(800);
            let h = node.height.unwrap_or(600);
            window.set_default_size(w, h);
            if let Some(ref children) = node.children {
                for child in children {
                    if let Some(child_ptr) = build_widget(child) {
                        let child_widget = WIDGET_REGISTRY.with(|r| r.borrow().get(&child_ptr).cloned().unwrap());
                        window.add(&child_widget);
                    }
                }
            }
            window.show();
            eprintln!("[gtk4kt] Window shown: key={}", key);
            Some(key)
        }
        "Box" => {
            let orient = node.orientation.unwrap_or(1);
            let spacing = node.spacing.unwrap_or(0);
            let orient_val = if orient == 0 {
                gtk::Orientation::Horizontal
            } else {
                gtk::Orientation::Vertical
            };
            let box_ = gtk::Box::new(orient_val, spacing);
            let key = next_key();
            WIDGET_REGISTRY.with(|r| r.borrow_mut().insert(key, box_.clone().upcast()));
            if let Some(ref children) = node.children {
                for child in children {
                    if let Some(child_ptr) = build_widget(child) {
                        let child_widget = WIDGET_REGISTRY.with(|r| r.borrow().get(&child_ptr).cloned().unwrap());
                        box_.pack_start(&child_widget, false, false, 0);
                    }
                }
            }
            Some(key)
        }
        "Label" => {
            let text = node.label.as_deref().unwrap_or("");
            let label = gtk::Label::new(Some(text));
            let key = next_key();
            WIDGET_REGISTRY.with(|r| r.borrow_mut().insert(key, label.clone().upcast()));
            Some(key)
        }
        "Button" => {
            let text = node.label.as_deref().unwrap_or("");
            let button = gtk::Button::with_label(text);
            let key = next_key();
            WIDGET_REGISTRY.with(|r| r.borrow_mut().insert(key, button.clone().upcast()));
            if let Some(handle) = node.on_click {
                CALLBACK_REGISTRY.with(|r| r.borrow_mut().insert(key, handle));
                // Connect clicked signal → JVM invoker (Panama upcall stub)
                let ptr_copy = key;
                button.connect_clicked(move |_| {
                    let handle_opt = CALLBACK_REGISTRY.with(|r| r.borrow().get(&ptr_copy).copied());
                    let invoker_opt = INVOKER_ADDR.with(|r| *r.borrow());
                    if let (Some(handle), Some(invoker)) = (handle_opt, invoker_opt) {
                        eprintln!("[gtk4kt] button clicked: ptr={}, handle={}", ptr_copy, handle);
                        unsafe {
                            type InvokerFn = extern "C" fn(i64, u64) -> i32;
                            let fn_ptr: InvokerFn = std::mem::transmute(invoker);
                            fn_ptr(handle, ptr_copy);
                        }
                    }
                });
                eprintln!("[gtk4kt] button registered + clicked connected: handle={}", handle);
            }
            LAST_BUTTON_PTR.with(|r| *r.borrow_mut() = Some(key));
            Some(key)
        }
        _ => {
            eprintln!("[gtk4kt] build_widget: unknown type '{}'", node.widget_type);
            None
        }
    }
}

#[no_mangle]
pub extern "C" fn gtk_bridge_init() -> c_int {
    // GTK initialization is deferred to gtk_bridge_application_run on the GTK thread.
    // This exists only for API compatibility — actual init happens there.
    0
}

#[no_mangle]
pub extern "C" fn gtk_bridge_application_new(app_id: *const c_char) -> u64 {
    let app_id_str = unsafe { std::ffi::CStr::from_ptr(app_id).to_string_lossy().into_owned() };
    eprintln!("[gtk4kt] gtk_bridge_application_new: id={}", app_id_str);
    // GTK3 GApplication creation (kept for API compat; standalone mode doesn't use it)
    let app = gtk::Application::new(Some(&app_id_str), gtk::gio::ApplicationFlags::default());
    let key = next_key();
    APP_REGISTRY.with(|r| r.borrow_mut().insert(key, app));
    key
}

#[no_mangle]
pub extern "C" fn gtk_bridge_application_quit(app_ptr: u64) -> c_int {
    if let Some(app) = APP_REGISTRY.with(|r| r.borrow().get(&app_ptr).cloned()) {
        app.quit();
        return 0;
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_set_ui_json_path(path: *const c_char) -> c_int {
    let path_str = unsafe { std::ffi::CStr::from_ptr(path).to_string_lossy().into_owned() };
    UI_JSON_PATH.with(|r| *r.borrow_mut() = Some(PathBuf::from(path_str)));
    eprintln!("[gtk4kt] gtk_bridge_set_ui_json_path: OK");
    0
}

#[no_mangle]
pub extern "C" fn gtk_bridge_builder_build_ui(_json: *const c_char, _app_ptr: u64) -> u64 {
    // Legacy entry point — JSON is now read from file in gtk_bridge_application_run
    eprintln!("[gtk4kt] gtk_bridge_builder_build_ui (legacy stub)");
    0
}

#[no_mangle]
pub extern "C" fn gtk_bridge_register_method_invoker(invoker_addr: *const std::ffi::c_void) {
    INVOKER_ADDR.with(|r| *r.borrow_mut() = Some(invoker_addr));
    eprintln!("[gtk4kt] gtk_bridge_register_method_invoker: OK");
}

#[no_mangle]
pub extern "C" fn gtk_bridge_register_invoker(invoker_addr: *const std::ffi::c_void) {
    INVOKER_ADDR.with(|r| *r.borrow_mut() = Some(invoker_addr));
    eprintln!("[gtk4kt] gtk_bridge_register_invoker: addr={:?}", invoker_addr);
}

#[no_mangle]
pub extern "C" fn gtk_bridge_application_run(_app_ptr: u64, _latch_addr: u64) -> c_int {
    // NOTE: GTK is already initialized by gtk_bridge_init() on the JVM main thread.
    // Do NOT call gtk::init() here — it would panic with "two different threads".
    let json_path = UI_JSON_PATH.with(|r| r.borrow().clone());
    if let Some(ref path) = json_path {
        match fs::read_to_string(path) {
            Ok(json) => {
                eprintln!("[gtk4kt] reading JSON ({} bytes)", json.len());
                if let Ok(root) = serde_json::from_str::<WidgetNode>(&json) {
                    let _ = build_widget(&root);
                    eprintln!("[gtk4kt] UI built OK");
                }
            }
            Err(e) => eprintln!("[gtk4kt] JSON read error: {}", e),
        }
    }
    eprintln!("[gtk4kt] entering gtk_main()");
    gtk::main();
    eprintln!("[gtk4kt] gtk_main() returned");
    0
}

#[no_mangle]
pub extern "C" fn gtk_bridge_main_quit() {
    eprintln!("[gtk4kt] gtk_bridge_main_quit");
    gtk::main_quit();
}

#[no_mangle]
pub extern "C" fn gtk_bridge_window_new() -> u64 {
    let window = gtk::Window::new(gtk::WindowType::Toplevel);
    let key = next_key();
    WIDGET_REGISTRY.with(|r| r.borrow_mut().insert(key, window.clone().upcast()));
    eprintln!("[gtk4kt] gtk_bridge_window_new: key={}", key);
    key
}

#[no_mangle]
pub extern "C" fn gtk_bridge_window_set_title(window_ptr: u64, title: *const c_char) -> c_int {
    let title_str = unsafe { std::ffi::CStr::from_ptr(title).to_string_lossy().into_owned() };
    if let Some(w) = WIDGET_REGISTRY.with(|r| r.borrow().get(&window_ptr).cloned()) {
        if let Ok(win) = w.downcast::<gtk::Window>() {
            win.set_title(&title_str);
            return 0;
        }
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_window_set_default_size(window_ptr: u64, width: c_int, height: c_int) -> c_int {
    if let Some(w) = WIDGET_REGISTRY.with(|r| r.borrow().get(&window_ptr).cloned()) {
        if let Ok(win) = w.downcast::<gtk::Window>() {
            win.set_default_size(width, height);
            return 0;
        }
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_window_present(window_ptr: u64) -> c_int {
    if let Some(w) = WIDGET_REGISTRY.with(|r| r.borrow().get(&window_ptr).cloned()) {
        if let Ok(win) = w.downcast::<gtk::Window>() {
            win.show();
            return 0;
        }
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_window_set_child(window_ptr: u64, child_ptr: u64) -> c_int {
    let child = WIDGET_REGISTRY.with(|r| r.borrow().get(&child_ptr).cloned());
    if let (Some(w), Some(c)) = (
        WIDGET_REGISTRY.with(|r| r.borrow().get(&window_ptr).cloned()),
        child,
    ) {
        if let Ok(win) = w.downcast::<gtk::Window>() {
            win.add(&c);
            return 0;
        }
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_box_new(orientation: c_int, spacing: c_int) -> u64 {
    let orient = if orientation == 0 {
        gtk::Orientation::Horizontal
    } else {
        gtk::Orientation::Vertical
    };
    let box_ = gtk::Box::new(orient, spacing);
    let key = next_key();
    WIDGET_REGISTRY.with(|r| r.borrow_mut().insert(key, box_.clone().upcast()));
    key
}

#[no_mangle]
pub extern "C" fn gtk_bridge_box_append(box_ptr: u64, child_ptr: u64) -> c_int {
    let child = WIDGET_REGISTRY.with(|r| r.borrow().get(&child_ptr).cloned());
    if let (Some(w), Some(c)) = (
        WIDGET_REGISTRY.with(|r| r.borrow().get(&box_ptr).cloned()),
        child,
    ) {
        if let Ok(b) = w.downcast::<gtk::Box>() {
            b.pack_start(&c, false, false, 0);
            return 0;
        }
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_label_new(text: *const c_char) -> u64 {
    let text_str = unsafe { std::ffi::CStr::from_ptr(text).to_string_lossy().into_owned() };
    let label = gtk::Label::new(Some(&text_str));
    let key = next_key();
    WIDGET_REGISTRY.with(|r| r.borrow_mut().insert(key, label.clone().upcast()));
    key
}

#[no_mangle]
pub extern "C" fn gtk_bridge_label_set_text(label_ptr: u64, text: *const c_char) -> c_int {
    let text_str = unsafe { std::ffi::CStr::from_ptr(text).to_string_lossy().into_owned() };
    if let Some(w) = WIDGET_REGISTRY.with(|r| r.borrow().get(&label_ptr).cloned()) {
        if let Ok(l) = w.downcast::<gtk::Label>() {
            l.set_text(&text_str);
            return 0;
        }
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_button_new_with_label(label: *const c_char) -> u64 {
    let label_str = unsafe { std::ffi::CStr::from_ptr(label).to_string_lossy().into_owned() };
    let button = gtk::Button::with_label(&label_str);
    let key = next_key();
    WIDGET_REGISTRY.with(|r| r.borrow_mut().insert(key, button.clone().upcast()));
    key
}

#[no_mangle]
pub extern "C" fn gtk_bridge_button_set_label(button_ptr: u64, label: *const c_char) -> c_int {
    let label_str = unsafe { std::ffi::CStr::from_ptr(label).to_string_lossy().into_owned() };
    if let Some(w) = WIDGET_REGISTRY.with(|r| r.borrow().get(&button_ptr).cloned()) {
        if let Ok(b) = w.downcast::<gtk::Button>() {
            b.set_label(&label_str);
            return 0;
        }
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_button_set_on_clicked(button_ptr: u64, handle_id: i64) -> c_int {
    if let Some(w) = WIDGET_REGISTRY.with(|r| r.borrow().get(&button_ptr).cloned()) {
        if let Ok(button) = w.downcast::<gtk::Button>() {
            CALLBACK_REGISTRY.with(|r| r.borrow_mut().insert(button_ptr, handle_id));
            let ptr_copy = button_ptr;
            button.connect_clicked(move |_| {
                let handle_opt = CALLBACK_REGISTRY.with(|r| r.borrow().get(&ptr_copy).copied());
                let invoker_opt = INVOKER_ADDR.with(|r| *r.borrow());
                if let (Some(handle), Some(invoker)) = (handle_opt, invoker_opt) {
                    eprintln!("[gtk4kt] button clicked: ptr={}, handle={}", ptr_copy, handle);
                    unsafe {
                        type InvokerFn = extern "C" fn(i64, u64) -> i32;
                        let fn_ptr: InvokerFn = std::mem::transmute(invoker);
                        fn_ptr(handle, ptr_copy);
                    }
                }
            });
            return 0;
        }
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_widget_destroy(widget_ptr: u64) -> c_int {
    if let Some(w) = WIDGET_REGISTRY.with(|r| r.borrow().get(&widget_ptr).cloned()) {
        unsafe { w.destroy() };
        WIDGET_REGISTRY.with(|r| r.borrow_mut().remove(&widget_ptr));
        return 0;
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_widget_show(widget_ptr: u64) -> c_int {
    if let Some(w) = WIDGET_REGISTRY.with(|r| r.borrow().get(&widget_ptr).cloned()) {
        w.show();
        return 0;
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_widget_unparent(widget_ptr: u64) -> c_int {
    if let Some(w) = WIDGET_REGISTRY.with(|r| r.borrow().get(&widget_ptr).cloned()) {
        if let Some(p) = w.parent() {
            if let Ok(c) = p.downcast::<gtk::Container>() {
                c.remove(&w);
                return 0;
            }
        }
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_widget_set_margin(widget_ptr: u64, margin: c_int) -> c_int {
    if let Some(w) = WIDGET_REGISTRY.with(|r| r.borrow().get(&widget_ptr).cloned()) {
        w.set_margin_start(margin);
        w.set_margin_end(margin);
        w.set_margin_top(margin);
        w.set_margin_bottom(margin);
        return 0;
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_widget_set_size_request(widget_ptr: u64, width: c_int, height: c_int) -> c_int {
    if let Some(wi) = WIDGET_REGISTRY.with(|r| r.borrow().get(&widget_ptr).cloned()) {
        wi.set_size_request(width, height);
        return 0;
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_widget_set_hexpand(widget_ptr: u64, expand: c_int) -> c_int {
    if let Some(w) = WIDGET_REGISTRY.with(|r| r.borrow().get(&widget_ptr).cloned()) {
        w.set_hexpand(expand != 0);
        return 0;
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_widget_set_vexpand(widget_ptr: u64, expand: c_int) -> c_int {
    if let Some(w) = WIDGET_REGISTRY.with(|r| r.borrow().get(&widget_ptr).cloned()) {
        w.set_vexpand(expand != 0);
        return 0;
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_widget_set_halign(widget_ptr: u64, align: c_int) -> c_int {
    let align_val = match align {
        0 => gtk::Align::Fill,
        1 => gtk::Align::Start,
        2 => gtk::Align::Center,
        3 => gtk::Align::End,
        _ => gtk::Align::Fill,
    };
    if let Some(w) = WIDGET_REGISTRY.with(|r| r.borrow().get(&widget_ptr).cloned()) {
        w.set_halign(align_val);
        return 0;
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_widget_set_valign(widget_ptr: u64, align: c_int) -> c_int {
    let align_val = match align {
        0 => gtk::Align::Fill,
        1 => gtk::Align::Start,
        2 => gtk::Align::Center,
        3 => gtk::Align::End,
        _ => gtk::Align::Fill,
    };
    if let Some(w) = WIDGET_REGISTRY.with(|r| r.borrow().get(&widget_ptr).cloned()) {
        w.set_valign(align_val);
        return 0;
    }
    -1
}

#[no_mangle]
pub extern "C" fn gtk_bridge_main_iteration() -> c_int {
    gtk::main_iteration();
    0
}

#[no_mangle]
pub extern "C" fn gtk_bridge_main_context_iteration() -> c_int {
    gtk::main_iteration_do(false);
    0
}

#[no_mangle]
pub extern "C" fn gtk_bridge_test_click(button_ptr: u64) -> c_int {
    if let Some(w) = WIDGET_REGISTRY.with(|r| r.borrow().get(&button_ptr).cloned()) {
        if let Ok(b) = w.downcast::<gtk::Button>() {
            b.emit_clicked();
            return 0;
        }
    }
    -1
}

/// Return the last button pointer registered (test helper).
pub extern "C" fn gtk_bridge_get_first_button_ptr() -> u64 {
    let ptr = LAST_BUTTON_PTR.with(|r| r.borrow().unwrap_or(0));
    eprintln!("[gtk4kt] gtk_bridge_get_first_button_ptr: returning {}", ptr);
    ptr
}
