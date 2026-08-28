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
    // JNA for native calls
    implementation("net.java.dev.jna:jna:5.6.0")
}

// --enable-preview needed for JDK 21 Foreign Function API (Panama)
tasks.named<JavaExec>("run") {
    jvmArgs("--enable-preview", "-Djava.library.path=lib")
}

tasks.register<Copy>("copyNativeLibs") {
    // Clean any stale .so before installing (Gradle's installDist refuses if
    // the installDir already has content).
    val installDir = layout.buildDirectory.dir("install/gtk4kt/lib").get().asFile
    val stale = File(installDir, "libgtk4kt_native.so")
    if (stale.exists()) stale.delete()
    from("src/main/rust/target/release/libgtk4kt_native.so") {
        rename { "libgtk4kt_native.so" }
    }
    into(installDir)
}

tasks.named("assemble") {
    dependsOn("copyNativeLibs")
}

// installDist creates its own lib/ structure. We want our native .so copied
// into that lib/ dir AFTER installDist finishes, so installDist's own bookkeeping
// remains consistent (Gradle refuses to install into a non-empty dir).
tasks.named("installDist") {
    finalizedBy("copyNativeLibs")
}
