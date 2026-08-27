# gtk4kt

> **Kotlin 优先的声明式 GTK4 UI 框架，面向 Linux/GNOME**

[![License: Apache-2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.10-orange.svg)](https://kotlinlang.org)

gtk4kt 让 Kotlin 开发者用声明式、Compose 风格的 API 编写原生 Linux 桌面 UI，底层由 GTK4 + libadwaita 驱动。

```kotlin
fun main() = application("org.example.App") {
    window("我的应用", width = 800, height = 600) {
        column(spacing = 16) {
            label("你好，GTK4！")
            button("点我！") {
                println("点击了")
            }
        }
    }
}
```

本文档其他语言版本：[English](README.md) · [简体中文](README.zh-CN.md) · [Русский](README.ru-RU.md)

---

## 架构

```
Kotlin/JVM
     │
     ▼
  DSL API   ← Compose 风格的 Kotlin DSL
     │
     ▼
 GTKNative  ← JNI 绑定层
     │
     ▼
jni_bridge (C 垫片) ← dlsym 调用 Rust
     │
     ▼
gtk4kt-native (Rust cdylib) ← gtk-rs → GTK4/libadwaita
     │
     ▼
GTK4 / libadwaita → Linux / GNOME
```

**所有权模型**：Kotlin 持有 GTK 对象的引用（原始 u64 指针）。
GTK 管理实际的对象生命周期。Kotlin 无需手动 refcount。

**线程模型**：所有 GTK 操作必须在 GTK 主线程执行。
计划提供基于 GLib main context 的 `Dispatchers.Main` 等效实现。

---

## 构建

### 前置依赖

- **Kotlin 2.4+**，JVM 21
- **Rust 1.75+**（native 层）
- **GTK 4.16+** 和 **libadwaita 1.9+**（系统库）
- **gcc**（编译 C JNI 垫片）

### 构建步骤

```bash
# 1. 构建 Rust cdylib
cd src/main/rust
cargo build --release
cd ../..

# 2. 构建 C JNI 垫片
gcc -shared -fPIC -O2 \
  -I$JAVA_HOME/include -I$JAVA_HOME/include/linux \
  -o src/main/resources/libjni_bridge.so \
  src/main/c/jni_bridge.c -ldl -lpthread

# 3. 构建 Kotlin
./gradlew installDist
```

### 运行

```bash
cd build/install/gtk4kt
LD_LIBRARY_PATH=lib GDK_BACKEND=x11 bin/gtk4kt
```

---

## 项目状态

**第一阶段 — 调研与 PoC** ✅ 完成

- [x] GTK4 widget 层级（widget → container → layout）
- [x] Rust gtk-rs 0.11 + gtk4 0.11 + libadwaita 0.11
- [x] Kotlin → C 垫片 → Rust cdylib → GTK4 链路验证
- [x] 最小 PoC：window + label + button

**第二阶段 — 声明式运行时** 🚧 进行中

- [ ] 状态管理（轻量响应式状态）
- [ ] 组合与重组合
- [ ] 节点标识与调和（基于差异的 GTK 更新）
- [ ] 生命周期管理

**第三阶段 — 基础组件** 📋 计划中

- [ ] Text、Button、TextField、CheckBox、Switch、Slider
- [ ] Row、Column、Box 布局
- [ ] Scroll、List、Dialog、Window

**第四阶段 — libadwaita** 📋 计划中

- [ ] AdwApplication、AdwApplicationWindow
- [ ] NavigationView、PreferencesPage
- [ ] 自适应布局与断点

---

## 关键设计决策

### 为什么用 Rust 而不是 C？

GTK C API 冗长，手动内存管理易出错。Rust 的 gtk-rs 绑定在提供零成本安全封装的同时，
保留原生性能和完整 GTK API。

### 为什么用 C 垫片而不是直接 JNI？

JNI 无法直接调用 Rust 函数——JVM 期望 C ABI。
C 垫片（`libjni_bridge.so`）通过 `dlsym` 在运行时动态解析 Rust 函数，
避免了 JNI↔Rust 的硬依赖链。

---

## 许可

Apache 2.0 — 见 [LICENSE](LICENSE)
