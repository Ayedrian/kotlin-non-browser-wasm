# kotlin-non-browser-wasm

A small Kotlin/Wasm experiment targeting the WASI (WebAssembly System Interface) environment. The program reads lines from stdin and writes them back with a prefix, running headless in a non-browser Wasm runtime.

## Requirements

- JDK 17+ — required by Gradle 9. I've tested it with JDK 21.
- Wasmtime — I chose Wasmtime as the non-browser runtime used to execute the compiled binary.

The gradle wrapper should automatically download the correct version (9.2.1).

## Running

```sh
./gradlew runWasm
```

This compiles the Kotlin source to a `.wasm` binary and executes it with Wasmtime.

## Notes
- Added the console=plain option in gradle.properties, since Gradle's progress UI interfered/didn't look good with keyboard input.
- The `wasmWasi` target in Kotlin 2.3.21 is in beta, it is functional but not yet stable.
- This project intentionally has no browser target, it's a focused WASI experiment.
