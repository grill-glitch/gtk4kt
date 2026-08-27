plugins {
    kotlin("jvm")
    `java-library`
    application
}

group = "org.librelab"
version = "0.1.0"

application {
    mainClass.set("org.librelab.gtk4kt.examples.HelloKt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        freeCompilerArgs.addAll("--enable-preview")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // No external JNA — pure JDK 21 Panama FFI
}

// --enable-preview needed for JDK 21 Foreign Function API (Panama)
tasks.named<JavaExec>("run") {
    jvmArgs("--enable-preview", "-Djava.library.path=lib")
}

tasks.register<Copy>("copyNativeLibs") {
    from("src/main/rust/target/release/libgtk4kt_native.so") {
        rename { "libgtk4kt_native.so" }
    }
    into(layout.buildDirectory.dir("install/gtk4kt/lib"))
}

tasks.named("assemble") {
    dependsOn("copyNativeLibs")
}
