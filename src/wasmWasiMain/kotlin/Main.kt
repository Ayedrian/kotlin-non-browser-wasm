import kotlin.wasm.WasmImport
import kotlin.wasm.unsafe.*

/**
 * Kotlin's readLine() uses the standard library to access the OS's stdin
 * I couldn't find readLine in the WasmWasi stdlib so this code provides a custom solution
 * The WASI spec defines fd_read which we can use to write our own readLine method
 */

/** Import of external function fd_read
 * @param fd: the file descriptor to read from (0 = stdin, 1 = stdout, 2 = stderr)
 * @param iovsPtr: pointer to an array of IO vectors
 * @param iovsLen: how many IO vectors are in the array
 * @param numreadPtr: pointer to where WASI should write amount of bytes actually read
 * @return error number, 0 if the read was successful
 */
@OptIn(ExperimentalWasmInterop::class) // suppress warning about @WasmImport needing opt-in
@WasmImport("wasi_snapshot_preview1", "fd_read") // "wasi_snapshot_preview1" is WASI API version Kotlin currently targets
private external fun wasiRead(fd: Int, iovsPtr: Int, iovsLen: Int, numreadPtr: Int): Int

/**
 * Our own readLine() method implemented using fd_read and a scoped memory allocator
 * Allocates the minimal variables needed to write one byte using fd_read
 * Keeps reading until EOF or newline is reached and then returns the built string
 */
@OptIn(UnsafeWasmMemoryApi::class)
private fun readLineFromStdin(): String? {
    val builder = StringBuilder() // build our string byte by byte

    // alocate raw memory in Wasm's linear memory using a scoped memory allocator
    // this allocator makes allocation easy, but freeing a specific part isn't supported...
    // ... which is fine since we don't need it here for our use case (when we exit the lambda everything is gone)
    // fd_read expects a whole array of IO vectors, but we need only one to store our byte
    withScopedMemoryAllocator { allocator ->
        val buffer = allocator.allocate(1) // one byte read with each fd_read call
        val iov = allocator.allocate(8) // 8 byte struct for a single IO vector, needs 1) a pointer to the buffer and 2) the length
        val numread = allocator.allocate(4) // 4 bytes for 32-bit integer (count of how many bytes were read)
        iov.storeInt(buffer.address.toInt()) // pointer to buffer is address of our buffer byte
        (iov + 4).storeInt(1) // 1 byte buffer length
        while (true) {
            // read from stdin, point to our IO vector array (containing only one IO vector)
            if (wasiRead(0, iov.address.toInt(), 1, numread.address.toInt()) != 0) {
                return null;
            }
            // if 0 bytes were read (meaning EOF) -> return our string (null if empty)
            if (numread.loadInt() == 0) return if (builder.isEmpty()) null else builder.toString()
            val byte = buffer.loadByte()
            if (byte == '\n'.code.toByte()) return builder.toString() // newline completes a string
            builder.append(byte.toInt().toChar())
        }
    }
}

fun main() {
    while (true) {
        val line = readLineFromStdin() ?: break
        println("Wasm received: $line") // Kotlin's WasmWasi does implement println using WASI (with fd_write)
    }
}
