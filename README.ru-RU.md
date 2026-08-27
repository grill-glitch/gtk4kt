# gtk4kt

> **Kotlin-first декларативный GTK4 UI-фреймворк для Linux/GNOME**

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-orange.svg)](https://kotlinlang.org)

**gtk4kt** позволяет Kotlin-разработчикам писать нативные Linux-приложения
с декларативным, Compose-подобным API — под капотом GTK4 и libadwaita.

```kotlin
fun main() = application("org.example.App") {
    window("Моё приложение", width = 800, height = 600) {
        column(spacing = 16) {
            label("Привет, GTK4!")
            button("Нажми меня!") {
                println("нажато")
            }
        }
    }
}
```

Читайте на: [English](README.md) · [简体中文](README.zh-CN.md) · [Русский](README.ru-RU.md)

---

## Архитектура

```
Kotlin/JVM
     │
     ▼
  DSL API   ← Compose-подобный Kotlin DSL
     │
     ▼
 GTKNative  ← JNI binding layer
     │
     ▼
jni_bridge (C shim) ← dlsym вызовы в Rust
     │
     ▼
gtk4kt-native (Rust cdylib) ← gtk-rs → GTK4/libadwaita
     │
     ▼
GTK4 / libadwaita → Linux / GNOME
```

**Модель владения**: Kotlin хранит ссылки (сырые u64 указатели) на GTK-объекты.
GTK сам управляет временем жизни объектов. Никакого ручного подсчёта ссылок из Kotlin.

---

## Сборка

### Зависимости

- **Kotlin 2.4+**, JVM 21
- **Rust 1.75+** (нативный слой)
- **GTK 4.16+** и **libadwaita 1.9+** (системные библиотеки)
- **gcc** (компиляция C JNI shim)

### Шаги сборки

```bash
# 1. Собрать Rust cdylib
cd src/main/rust
cargo build --release
cd ../..

# 2. Собрать C JNI shim
gcc -shared -fPIC -O2 \
  -I$JAVA_HOME/include -I$JAVA_HOME/include/linux \
  -o src/main/resources/libjni_bridge.so \
  src/main/c/jni_bridge.c -ldl -lpthread

# 3. Собрать Kotlin
./gradlew installDist
```

### Запуск

```bash
cd build/install/gtk4kt
LD_LIBRARY_PATH=lib GDK_BACKEND=x11 bin/gtk4kt
```

---

## Статус проекта

**Фаза 1 — Исследование и PoC** ✅ ГОТОВО

- [x] GTK4 иерархия виджетов
- [x] Rust gtk-rs 0.11 + gtk4 0.11 + libadwaita 0.11
- [x] Kotlin → C shim → Rust cdylib → GTK4 проверено
- [x] Минимальный PoC: window + label + button

**Фаза 2 — Декларативный runtime** 🚧 В РАБОТЕ

- [ ] Управление состоянием
- [ ] Композиция и рекомпозиция
- [ ] Идентификация узлов и сверка (diff-based GTK updates)
- [ ] Управление жизненным циклом

**Фаза 3 — Базовые компоненты** 📋 ЗАПЛАНИРОВАНО

- [ ] Text, Button, TextField, CheckBox, Switch, Slider
- [ ] Row, Column, Box layout
- [ ] Scroll, List, Dialog, Window

**Фаза 4 — libadwaita** 📋 ЗАПЛАНИРОВАНО

- [ ] AdwApplication, AdwApplicationWindow
- [ ] NavigationView, PreferencesPage
- [ ] Адаптивная вёрстка и breakpoints

---

## Лицензия

Apache 2.0 — см. [LICENSE](LICENSE)
