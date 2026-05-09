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
