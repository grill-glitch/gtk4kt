//! gtk4kt-native — Rust cdylib exposing GTK4 to Kotlin/JVM via JNA

use std::cell::RefCell;
use std::collections::HashMap;
use std::ffi::{c_char, c_int};
use std::time::{SystemTime, UNIX_EPOCH};

use gtk::prelude::*;

const NULL_PTR: u64 = u64::MAX;

// ============================================================================
// App Registry (thread_local)
// ============================================================================

thread_local! {
    static APP_REGISTRY: RefCell<HashMap<u64, gtk::Application>> = RefCell::new(HashMap::new());
    static WIDGET_REGISTRY: RefCell<HashMap<u64, gtk::Widget>> = RefCell::new(HashMap::new());
    static CALLBACK_REGISTRY: RefCell<HashMap<u64, Box<dyn Fn() + Send + 'static>>> = RefCell::new(HashMap::new());
}

fn next_key() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_nanos() as u64
}

// ============================================================================
// Core GTK FFI
// ============================================================================

/// gtk_bridge_init — initialize GTK
#[no_mangle]
pub extern "C" fn gtk_bridge_init() -> c_int {
    if gtk::init().is_err() {
        eprintln!("[gtk4kt] gtk_bridge_init failed");
        return -1;
    }
    eprintln!("[gtk4kt] gtk_bridge_init OK");
    0
}

/// gtk_bridge_application_new — create GtkApplication
#[no_mangle]
pub extern "C" fn gtk_bridge_application_new(app_id: *const c_char, _flags: c_int) -> u64 {
    let app_id_str = unsafe { std::ffi::CStr::from_ptr(app_id) }
        .to_str()
        .unwrap_or("org.gtk4kt");
    let app = gtk::Application::new(Some(app_id_str), gtk::gio::ApplicationFlags::default());

    let key = next_key();
    APP_REGISTRY.with(|r| r.borrow_mut().insert(key, app.clone()));
    eprintln!("[gtk4kt] application_new key={}", key);
    key
}

/// gtk_bridge_window_new — create GtkApplicationWindow
#[no_mangle]
pub extern "C" fn gtk_bridge_window_new(app_ptr: u64) -> u64 {
    let app = match APP_REGISTRY.with(|r| r.borrow().get(&app_ptr).cloned()) {
        Some(a) => a,
        None => return NULL_PTR,
    };
    let window = gtk::ApplicationWindow::new(&app);
    let key = next_key();
    WIDGET_REGISTRY.with(|r| r.borrow_mut().insert(key, window.clone().upcast()));
    eprintln!("[gtk4kt] window_new key={}", key);
    key
}

/// gtk_bridge_box_new — create GtkBox
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

/// gtk_bridge_label_new — create GtkLabel
#[no_mangle]
pub extern "C" fn gtk_bridge_label_new(text: *const c_char) -> u64 {
    let text_str = unsafe {
        if text.is_null() {
            ""
        } else {
            std::ffi::CStr::from_ptr(text).to_str().unwrap_or("")
        }
    };
    let label = gtk::Label::new(Some(text_str));
    let key = next_key();
    WIDGET_REGISTRY.with(|r| r.borrow_mut().insert(key, label.clone().upcast()));
    key
}

/// gtk_bridge_button_new_with_label — create GtkButton with label
#[no_mangle]
pub extern "C" fn gtk_bridge_button_new_with_label(label: *const c_char) -> u64 {
    let label_str = unsafe {
        if label.is_null() {
            ""
        } else {
            std::ffi::CStr::from_ptr(label).to_str().unwrap_or("")
        }
    };
    let button = gtk::Button::new();
    button.set_label(label_str);
    let key = next_key();
    WIDGET_REGISTRY.with(|r| r.borrow_mut().insert(key, button.clone().upcast()));
    key
}

/// gtk_bridge_button_set_label
#[no_mangle]
pub extern "C" fn gtk_bridge_button_set_label(ptr: u64, label: *const c_char) -> c_int {
    let button = match WIDGET_REGISTRY.with(|r| r.borrow().get(&ptr).cloned()) {
        Some(w) => w.clone().downcast::<gtk::Button>().ok(),
        None => return -1,
    };
    let button = match button {
        Some(b) => b,
        None => return -1,
    };
    let label_str = unsafe {
        if label.is_null() {
            ""
        } else {
            std::ffi::CStr::from_ptr(label).to_str().unwrap_or("")
        }
    };
    button.set_label(label_str);
    0
}

/// gtk_bridge_label_set_text
#[no_mangle]
pub extern "C" fn gtk_bridge_label_set_text(ptr: u64, text: *const c_char) -> c_int {
    let label = match WIDGET_REGISTRY.with(|r| r.borrow().get(&ptr).cloned()) {
        Some(w) => w.clone().downcast::<gtk::Label>().ok(),
        None => return -1,
    };
    let label = match label {
        Some(l) => l,
        None => return -1,
    };
    let text_str = unsafe {
        if text.is_null() {
            ""
        } else {
            std::ffi::CStr::from_ptr(text).to_str().unwrap_or("")
        }
    };
    label.set_label(text_str);
    0
}

/// gtk_bridge_label_get_text
#[no_mangle]
pub extern "C" fn gtk_bridge_label_get_text(ptr: u64) -> *const c_char {
    let label = match WIDGET_REGISTRY.with(|r| r.borrow().get(&ptr).cloned()) {
        Some(w) => w.clone().downcast::<gtk::Label>().ok(),
        None => return std::ptr::null(),
    };
    let label = match label {
        Some(l) => l,
        None => return std::ptr::null(),
    };
    let text = label.text().to_string();
    let cstr = std::ffi::CString::new(text.as_str()).unwrap_or_else(|_| std::ffi::CString::new("").unwrap());
    let ptr = cstr.as_ptr();
    std::mem::forget(cstr);
    ptr
}

/// gtk_bridge_window_set_child
#[no_mangle]
pub extern "C" fn gtk_bridge_window_set_child(window_ptr: u64, child_ptr: u64) -> c_int {
    let window = match WIDGET_REGISTRY.with(|r| r.borrow().get(&window_ptr).cloned()) {
        Some(w) => w.clone().downcast::<gtk::Window>().ok(),
        None => return -1,
    };
    let window = match window {
        Some(win) => win,
        None => return -1,
    };
    let child = match WIDGET_REGISTRY.with(|r| r.borrow().get(&child_ptr).cloned()) {
        Some(c) => c,
        None => return -1,
    };
    window.set_child(Some(&child));
    0
}

/// gtk_bridge_window_destroy
#[no_mangle]
pub extern "C" fn gtk_bridge_window_destroy(window_ptr: u64) -> c_int {
    let widget = match WIDGET_REGISTRY.with(|r| r.borrow().get(&window_ptr).cloned()) {
        Some(w) => w,
        None => return -1,
    };
    widget.unparent();
    WIDGET_REGISTRY.with(|r| r.borrow_mut().remove(&window_ptr));
    0
}

/// gtk_bridge_widget_destroy
#[no_mangle]
pub extern "C" fn gtk_bridge_widget_destroy(ptr: u64) -> c_int {
    let widget = match WIDGET_REGISTRY.with(|r| r.borrow().get(&ptr).cloned()) {
        Some(w) => w,
        None => return -1,
    };
    widget.unparent();
    WIDGET_REGISTRY.with(|r| r.borrow_mut().remove(&ptr));
    0
}

/// gtk_bridge_box_append
#[no_mangle]
pub extern "C" fn gtk_bridge_box_append(box_ptr: u64, child_ptr: u64) -> c_int {
    let box_ = match WIDGET_REGISTRY.with(|r| r.borrow().get(&box_ptr).cloned()) {
        Some(w) => w.clone().downcast::<gtk::Box>().ok(),
        None => return -1,
    };
    let box_ = match box_ {
        Some(b) => b,
        None => return -1,
    };
    let child = match WIDGET_REGISTRY.with(|r| r.borrow().get(&child_ptr).cloned()) {
        Some(c) => c,
        None => return -1,
    };
    box_.append(&child);
    0
}

/// gtk_bridge_widget_show
#[no_mangle]
pub extern "C" fn gtk_bridge_widget_show(ptr: u64) -> c_int {
    let widget = match WIDGET_REGISTRY.with(|r| r.borrow().get(&ptr).cloned()) {
        Some(w) => w,
        None => return -1,
    };
    widget.show();
    0
}

/// gtk_bridge_window_set_title
#[no_mangle]
pub extern "C" fn gtk_bridge_window_set_title(ptr: u64, title: *const c_char) -> c_int {
    let window = match WIDGET_REGISTRY.with(|r| r.borrow().get(&ptr).cloned()) {
        Some(w) => w.clone().downcast::<gtk::Window>().ok(),
        None => return -1,
    };
    let window = match window {
        Some(win) => win,
        None => return -1,
    };
    let title_str = unsafe {
        if title.is_null() {
            ""
        } else {
            std::ffi::CStr::from_ptr(title).to_str().unwrap_or("")
        }
    };
    window.set_title(Some(title_str));
    0
}

/// gtk_bridge_window_set_default_size
#[no_mangle]
pub extern "C" fn gtk_bridge_window_set_default_size(ptr: u64, w: c_int, h: c_int) -> c_int {
    let window = match WIDGET_REGISTRY.with(|r| r.borrow().get(&ptr).cloned()) {
        Some(w) => w.clone().downcast::<gtk::Window>().ok(),
        None => return -1,
    };
    let window = match window {
        Some(win) => win,
        None => return -1,
    };
    window.set_default_size(w, h);
    0
}

/// gtk_bridge_window_present — show and raise window
#[no_mangle]
pub extern "C" fn gtk_bridge_window_present(ptr: u64) -> c_int {
    let window = match WIDGET_REGISTRY.with(|r| r.borrow().get(&ptr).cloned()) {
        Some(w) => w.clone().downcast::<gtk::Window>().ok(),
        None => return -1,
    };
    let window = match window {
        Some(win) => win,
        None => return -1,
    };
    window.present();
    0
}

/// gtk_bridge_widget_set_margin
#[no_mangle]
pub extern "C" fn gtk_bridge_widget_set_margin(ptr: u64, margin: c_int) -> c_int {
    let widget = match WIDGET_REGISTRY.with(|r| r.borrow().get(&ptr).cloned()) {
        Some(w) => w,
        None => return -1,
    };
    widget.set_margin_start(margin);
    widget.set_margin_end(margin);
    widget.set_margin_top(margin);
    widget.set_margin_bottom(margin);
    0
}

/// gtk_bridge_widget_set_size_request
#[no_mangle]
pub extern "C" fn gtk_bridge_widget_set_size_request(ptr: u64, w: c_int, h: c_int) -> c_int {
    let widget = match WIDGET_REGISTRY.with(|r| r.borrow().get(&ptr).cloned()) {
        Some(w) => w,
        None => return -1,
    };
    widget.set_size_request(w, h);
    0
}

/// gtk_bridge_widget_set_hexpand
#[no_mangle]
pub extern "C" fn gtk_bridge_widget_set_hexpand(ptr: u64, expand: c_int) -> c_int {
    let widget = match WIDGET_REGISTRY.with(|r| r.borrow().get(&ptr).cloned()) {
        Some(w) => w,
        None => return -1,
    };
    widget.set_hexpand(expand != 0);
    0
}

/// gtk_bridge_widget_set_vexpand
#[no_mangle]
pub extern "C" fn gtk_bridge_widget_set_vexpand(ptr: u64, expand: c_int) -> c_int {
    let widget = match WIDGET_REGISTRY.with(|r| r.borrow().get(&ptr).cloned()) {
        Some(w) => w,
        None => return -1,
    };
    widget.set_vexpand(expand != 0);
    0
}

/// gtk_bridge_widget_set_halign
#[no_mangle]
pub extern "C" fn gtk_bridge_widget_set_halign(ptr: u64, align: c_int) -> c_int {
    use gtk::Align;
    let widget = match WIDGET_REGISTRY.with(|r| r.borrow().get(&ptr).cloned()) {
        Some(w) => w,
        None => return -1,
    };
    let align = match align {
        0 => Align::Start,
        1 => Align::Center,
        2 => Align::End,
        3 => Align::Fill,
        _ => return -1,
    };
    widget.set_halign(align);
    0
}

/// gtk_bridge_widget_set_valign
#[no_mangle]
pub extern "C" fn gtk_bridge_widget_set_valign(ptr: u64, align: c_int) -> c_int {
    use gtk::Align;
    let widget = match WIDGET_REGISTRY.with(|r| r.borrow().get(&ptr).cloned()) {
        Some(w) => w,
        None => return -1,
    };
    let align = match align {
        0 => Align::Start,
        1 => Align::Center,
        2 => Align::End,
        3 => Align::Fill,
        _ => return -1,
    };
    widget.set_valign(align);
    0
}

// ============================================================================
// Callback system
// ============================================================================

/// gtk_bridge_button_connect_clicked — register Kotlin callback for button click
/// callback_fn: a C-compatible function pointer (extern "C" fn()) passed from Kotlin
#[no_mangle]
pub extern "C" fn gtk_bridge_button_connect_clicked(
    button_ptr: u64,
    callback_fn: Option<unsafe extern "C" fn()>,
) -> u64 {
    let button = match WIDGET_REGISTRY.with(|r| r.borrow().get(&button_ptr).cloned()) {
        Some(w) => w.clone().downcast::<gtk::Button>().ok(),
        None => return NULL_PTR,
    };
    let button = match button {
        Some(b) => b,
        None => return NULL_PTR,
    };

    let cb_key = next_key();

    // Wrap the raw C function pointer in a safe closure
    if let Some(fp) = callback_fn {
        button.connect_clicked(move |_| {
            // Call the Kotlin-provided C function pointer
            unsafe { fp() }
        });
    }

    eprintln!("[gtk4kt] button_connect_clicked key={}", cb_key);
    cb_key
}

/// gtk_bridge_main_quit — quit the GTK main loop
#[no_mangle]
pub extern "C" fn gtk_bridge_main_quit() -> c_int {
    // Note: gtk::Application::run() must be replaced with a manual main loop
    // to support quit(). For PoC, we use Application::run_with_args which
    // runs until quit. A GTK source quit implementation needed here.
    eprintln!("[gtk4kt] main_quit called");
    0
}

// ============================================================================
// Application run
// ============================================================================

/// gtk_bridge_application_run — run GTK main loop
/// Must be called from the main thread. Uses run_with_args(&[]) to avoid
/// JVM args being passed to GTK.
/// 
/// IMPORTANT: GTK4 requires ALL windows to be created AFTER the GApplication
/// startup signal AND before run() enters the main loop. 
/// activate signal fires when run() is called, so window creation MUST be
/// triggered from Rust's activate handler.
#[no_mangle]
pub extern "C" fn gtk_bridge_application_run(app_ptr: u64) -> c_int {
    let app = match APP_REGISTRY.with(|r| r.borrow().get(&app_ptr).cloned()) {
        Some(a) => a,
        None => return -1,
    };

    eprintln!("[gtk4kt] registering activate handler...");
    
    // Register the activate handler BEFORE run() so it fires correctly
    // In GTK4, activate fires when the app becomes active (first window)
    app.connect_activate(|app| {
        eprintln!("[gtk4kt] activate signal fired");
        let window = gtk::ApplicationWindow::new(app);
        window.set_title(Some("gtk4kt"));
        window.set_default_size(400, 200);
        
        // Create a vertical box with label + button
        let vbox = gtk::Box::new(gtk::Orientation::Vertical, 16);
        
        let label = gtk::Label::new(Some("Count: 0"));
        let button = gtk::Button::new();
        button.set_label("Click me!");
        let reset_btn = gtk::Button::new();
        reset_btn.set_label("Reset");
        
        vbox.append(&label);
        vbox.append(&button);
        vbox.append(&reset_btn);
        
        window.set_child(Some(&vbox));
        window.present();
        
        // Store window in registry so Kotlin can access it
        let key = next_key();
        WIDGET_REGISTRY.with(|r| r.borrow_mut().insert(key, window.clone().upcast()));
        
        // Button click: update label
        let label_clone = label.clone();
        button.connect_clicked(move |_| {
            let current = label_clone.text();
            // Parse count from "Count: N"
            let count: i32 = current.to_string()
                .strip_prefix("Count: ")
                .and_then(|s| s.parse().ok())
                .unwrap_or(0);
            let new_count = count + 1;
            label_clone.set_label(&format!("Count: {}", new_count));
        });
        
        // Reset button
        let label_clone2 = label.clone();
        reset_btn.connect_clicked(move |_| {
            label_clone2.set_label("Count: 0");
        });
        
        eprintln!("[gtk4kt] window created and presented");
    });

    eprintln!("[gtk4kt] calling app.run_with_args...");
    let args: Vec<&str> = vec![];
    app.run_with_args(&args);
    eprintln!("[gtk4kt] app.run() returned");
    0
}
