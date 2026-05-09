import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    kotlin("multiplatform") version "2.3.21" // latest version
}

repositories {
    mavenCentral()
}

kotlin {
    @OptIn(ExperimentalWasmDsl::class) // wasmWasi isn't stable yet so opt into experimental API
    wasmWasi { // wasmWasi as our only compilation target
        nodejs() // test environment, only supported option for WASI is Node.js, this doesn't affect our production build output
        binaries.executable() // make standalone executable .wasm (not a library etc.)
    }
}

tasks.register<Exec>("runWasm") {
    dependsOn("build")
    commandLine(
        "wasmtime", "run",
        // we need to enable three wasm proposals with -W flag since they're not enabled by Wasmtime by default
        "-W", "gc", // garbage collection
        "-W", "exceptions",
        "-W", "function-references", // typed function references for lambdas, higher-order functions etc.
        "build/compileSync/wasmWasi/main/productionExecutable/optimized/kotlin-non-browser-wasm.wasm"
    )
    // Gradle's JVM daemon has no real file descriptor 0, however it forwards stdin from the gradlew client into the
    // Java System.in stream (in other words Gradle reroutes System.in away from the standard fd 0 to the socket from the client).
    // Therefore connecting System.in to the Exec task's standardInput makes sure it gets sent to Wasmtime.
    // ProcessBuilder.inheritIO() didn't work here, as it it inherits the daemon's empty file descriptor 0 (Wasmtime just sees EOF)
    // --no-daemon also doesn't work as Gradle still spins up a JVM instance
    standardInput = System.`in`
}
