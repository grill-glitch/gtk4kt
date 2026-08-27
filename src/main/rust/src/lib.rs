//! gtk4kt-native — Rust cdylib exposing GTK4 to Kotlin/JVM via JNA

use std::cell::RefCell;
use std::collections::HashMap;
use std::ffi::{c_char, c_int};
use std::time::{SystemTime, UNIX_EPOCH};

use gtk::prelude::*;

const NULL_PTR: u64 = u64::MAX;

thread_local! {
    static APP_REGISTRY: RefCell<HashMap<u64, gtk::Application>> =
        RefCell::new(HashMap::new());
    static WIDGET_REGISTRY: RefCell<HashMap<u64, gtk::Widget>> =
        RefCell::new(HashMap::new());
}

fn next_key() -> u64 {
    SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_nanos() as u64
}

#[no_mangle]
pub extern "C" fn gtk_bridge_init() -> c_int {
    if gtk::init().is_ok() { 0 } else { -1 }
}

#[no_mangle]
pub extern "C" fn gtk_bridge_application_new(app_id: *const c_char, _flags: c_int) -> u64 {
    let app_id_str = unsafe { std::ffi::CStr::from_ptr(app_id) }
        .to_str()
        .unwrap_or("org.gtk4kt.Application");
    let app = gtk::Application::new(Some(app_id_str), gtk::gio::ApplicationFlags::default());
    let key = next_key();
    APP_REGISTRY.with(|r| r.borrow_mut().insert(key, app));
    key
}

#[no_mangle]
pub extern "C" fn gtk_bridge_application_run(app_ptr: u64) -> c_int {
    let app = APP_REGISTRY.with(|r| r.borrow().get(&app_ptr).cloned());
    let app = match app {
        Some(a) => a,
        None => return -1,
    };

    app.connect_activate(move |app| {
        let window = gtk::ApplicationWindow::new(app);
        window.set_title(Some("gtk4kt"));
        window.set_default_size(800, 600);
        let label = gtk::Label::new(Some("Hello from gtk4kt!"));
        window.set_child(Some(&label));
        window.present();

        let key = next_key();
        WIDGET_REGISTRY.with(|r| {
            r.borrow_mut().insert(key, window.upcast::<gtk::Widget>());
        });
    });

    let args: Vec<&str> = vec![];
    app.run_with_args(&args);
    0
}

#[no_mangle]
pub extern "C" fn gtk_bridge_main_quit() {}

#[no_mangle]
pub extern "C" fn gtk_bridge_window_new() -> u64 {
    let window = gtk::Window::new();
    let key = next_key();
    WIDGET_REGISTRY.with(|r| {
        r.borrow_mut().insert(key, window.upcast::<gtk::Widget>());
    });
    key
}

#[no_mangle]
pub extern "C" fn gtk_bridge_window_set_title(window_ptr: u64, title: *const c_char) {
    let title_str = unsafe { std::ffi::CStr::from_ptr(title) }
        .to_str()
        .unwrap_or("");
    WIDGET_REGISTRY.with(|r| {
        if let Some(w) = r.borrow().get(&window_ptr) {
            if let Some(win) = w.downcast_ref::<gtk::Window>() {
                win.set_title(Some(title_str));
            }
        }
    });
}

#[no_mangle]
pub extern "C" fn gtk_bridge_window_set_default_size(window_ptr: u64, width: c_int, height: c_int) {
    WIDGET_REGISTRY.with(|r| {
        if let Some(w) = r.borrow().get(&window_ptr) {
            if let Some(win) = w.downcast_ref::<gtk::Window>() {
                win.set_default_size(width, height);
            }
        }
    });
}

#[no_mangle]
pub extern "C" fn gtk_bridge_window_present(window_ptr: u64) {
    WIDGET_REGISTRY.with(|r| {
        if let Some(w) = r.borrow().get(&window_ptr) {
            if let Some(win) = w.downcast_ref::<gtk::Window>() {
                win.present();
            }
        }
    });
}

#[no_mangle]
pub extern "C" fn gtk_bridge_window_destroy(window_ptr: u64) {
    WIDGET_REGISTRY.with(|r| {
        r.borrow_mut().remove(&window_ptr);
    });
}

#[no_mangle]
pub extern "C" fn gtk_bridge_label_new(text: *const c_char) -> u64 {
    let text_str = unsafe { std::ffi::CStr::from_ptr(text) }
        .to_str()
        .unwrap_or("");
    let label = gtk::Label::new(Some(text_str));
    let key = next_key();
    WIDGET_REGISTRY.with(|r| {
        r.borrow_mut().insert(key, label.upcast::<gtk::Widget>());
    });
    key
}

#[no_mangle]
pub extern "C" fn gtk_bridge_label_set_text(label_ptr: u64, text: *const c_char) {
    let text_str = unsafe { std::ffi::CStr::from_ptr(text) }
        .to_str()
        .unwrap_or("");
    WIDGET_REGISTRY.with(|r| {
        if let Some(w) = r.borrow().get(&label_ptr) {
            if let Some(l) = w.downcast_ref::<gtk::Label>() {
                l.set_text(text_str);
            }
        }
    });
}

#[no_mangle]
pub extern "C" fn gtk_bridge_label_get_text(label_ptr: u64) -> *mut c_char {
    let result = WIDGET_REGISTRY.with(|r| {
        r.borrow()
            .get(&label_ptr)
            .and_then(|w| w.downcast_ref::<gtk::Label>())
            .map(|l| l.text())
    });
    match result {
        Some(t) => std::ffi::CString::new(t)
            .map_or(std::ptr::null_mut(), |s| std::ffi::CString::into_raw(s)),
        None => std::ffi::CString::new("")
            .map_or(std::ptr::null_mut(), |s| std::ffi::CString::into_raw(s)),
    }
}

#[no_mangle]
pub extern "C" fn gtk_bridge_button_new_with_label(label: *const c_char) -> u64 {
    let label_str = unsafe { std::ffi::CStr::from_ptr(label) }
        .to_str()
        .unwrap_or("");
    let button = gtk::Button::with_label(label_str);
    let key = next_key();
    WIDGET_REGISTRY.with(|r| {
        r.borrow_mut().insert(key, button.upcast::<gtk::Widget>());
    });
    key
}

#[no_mangle]
pub extern "C" fn gtk_bridge_button_set_label(button_ptr: u64, label: *const c_char) {
    let label_str = unsafe { std::ffi::CStr::from_ptr(label) }
        .to_str()
        .unwrap_or("");
    WIDGET_REGISTRY.with(|r| {
        if let Some(w) = r.borrow().get(&button_ptr) {
            if let Some(b) = w.downcast_ref::<gtk::Button>() {
                b.set_label(label_str);
            }
        }
    });
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
    WIDGET_REGISTRY.with(|r| {
        r.borrow_mut().insert(key, box_.upcast::<gtk::Widget>());
    });
    key
}

#[no_mangle]
pub extern "C" fn gtk_bridge_box_append(box_ptr: u64, child_ptr: u64) {
    WIDGET_REGISTRY.with(|r| {
        let b = r.borrow().get(&box_ptr).cloned();
        let c = r.borrow().get(&child_ptr).cloned();
        if let (Some(bx), Some(ch)) = (b, c) {
            if let Some(bl) = bx.downcast_ref::<gtk::Box>() {
                bl.append(&ch);
            }
        }
    });
}

#[no_mangle]
pub extern "C" fn gtk_bridge_widget_show(widget_ptr: u64) {
    WIDGET_REGISTRY.with(|r| {
        if let Some(w) = r.borrow().get(&widget_ptr) {
            w.show();
        }
    });
}

#[no_mangle]
pub extern "C" fn gtk_bridge_widget_destroy(widget_ptr: u64) {
    WIDGET_REGISTRY.with(|r| {
        if let Some(w) = r.borrow_mut().remove(&widget_ptr) {
            w.unparent();
        }
    });
}

#[no_mangle]
pub extern "C" fn gtk_bridge_widget_set_margin(widget_ptr: u64, margin: c_int) {
    WIDGET_REGISTRY.with(|r| {
        if let Some(w) = r.borrow().get(&widget_ptr) {
            w.set_margin_start(margin);
            w.set_margin_end(margin);
            w.set_margin_top(margin);
            w.set_margin_bottom(margin);
        }
    });
}
