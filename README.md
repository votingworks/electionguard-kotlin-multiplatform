# Kotlin-Bignum
This is an experimental attempt to create a multiplatform Kotlin library for cryptographic bignum arithmetic.
In particular, the goal is to support [ElectionGuard](https://github.com/microsoft/electionguard) implementations
across multiple platforms, including iOS, Android, and in the browser.

ElectionGuard is generally sensitive to the performance of bignum arithmetic, particularly modular exponentation, so
even though there are pure-Kotlin implementations of bignum arithmetic, they are nowhere near as fast as the
native implementations. So what's in here?

- For the JVM, we're using `java.math.BigInteger`. This seems to do modular exponentiation at roughly half the
  speed of GnuMP, which is still impressive. On Android, `BigInteger` ultimately uses the native code that's part
  of the Android TLS implementation, so that should be fast.
- For native code, we're currently using HACL*, which is native C code that was generated from a formally verified toolchain.
  It uses all the magic compiler intrinsics to take advantage of modern 64-bit hardware, and is used by Microsoft's
  [electionguard-cpp](ihttps://github.com/microsoft/electionguard-cpp/tree/main/src/kremlin) implementation.
- For the browser, we're currently using [kt-math](https://github.com/gciatto/kt-math) because it works everywhere.
- The ultimate goal for the browser will be to use either [gmp-wasm](https://github.com/Daninet/gmp-wasm), which
  is a WebAssembly port of the GMP library, or [hacl-wasm](https://www.npmjs.com/package/hacl-wasm), which doesn't
  currently export its bignum implementation, but maybe it will in the future. At least right now, Kotlin's ability to translate
  the TypeScript headers from gmp-wasm is not working right, so this is likely going to require a bunch of work
  just to write the frontend interfaces.
