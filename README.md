# Electionguard-Kotlin-Multiplatform
This is an experimental attempt to create a multiplatform Kotlin implementation of 
[ElectionGuard](https://github.com/microsoft/electionguard), capable of running
"everywhere" (Android, iOS, Unix native, JavaScript-in-browser).

ElectionGuard is generally sensitive to the performance of bignum arithmetic, particularly modular exponentation, so
even though there are pure-Kotlin implementations of bignum arithmetic, they are nowhere near as fast as the
native implementations. So what's in here?

- For the JVM, we're using `java.math.BigInteger`. This seems to do modular exponentiation at roughly half the
  speed of GnuMP, which is still impressive. On Android, `BigInteger` ultimately uses the native code that's part
  of the Android TLS implementation, so that should be fast.
- For native code, we're currently using HACL*, which is native C code that was generated from a formally verified toolchain.
  It uses all the magic compiler intrinsics to take advantage of modern 64-bit hardware, and is used by Microsoft's
  [electionguard-cpp](ihttps://github.com/microsoft/electionguard-cpp/tree/main/src/kremlin) implementation.
- For the browser, we're currently using [kt-math](https://github.com/gciatto/kt-math) because it "works" everywhere,
  but wow it's slow. (Also, it's unclear that we're getting the `crypto` library correctly on the browser. That's how
  we get good randomness. Seems to work fine on Node.)

## Fast crypto on the browser?
The current `kt-math` code is unacceptably slow, like two seconds for a single ElGamal encryption! (Node.js, version 16.)
It's unclear that we can specifically blame `kt-math` for this, since it runs at maybe 1/3 the speed of BigInteger
(~33 ElGamal encryptions per second) when it's on the JVM. That would be a factor of 60 slowdown when running in the browser.
That's nightmarish.

The ultimate goal for the browser will be to use either [gmp-wasm](https://github.com/Daninet/gmp-wasm), which
is a WebAssembly port of the GMP library, or [hacl-wasm](https://www.npmjs.com/package/hacl-wasm), which doesn't
currently export its bignum implementation, but maybe it will in the future. At least right now, Kotlin's ability to translate
the TypeScript headers from gmp-wasm is not working right, so this is likely going to require a bunch of work
just to write the frontend interfaces.

Branches of note:
- `gmp-wasm-v2`: an attempt to use the `gmp-wasm` package
- `sjcl`: an attempt to use the Stanford JavaScript Crypto Library

The `gmp-wasm-v2` branch is probably better developed and more likely to be performant, but it
doesn't even reliably run the tests, instead crashing `node` instead. That's not good.

Maybe the right answer is to use the [Rice student's port of ElectionGuard to TypeScript](https://github.com/Xin128/ElectionGuard-COMP413/)
for browser-related stuff, while this repo works for virtually everything else.
