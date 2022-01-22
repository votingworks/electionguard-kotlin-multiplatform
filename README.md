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
  but wow it's slow. More on this below.

## Differences from ElectionGuard-Python

This code is intended to be compatible with the reference implementation in Python, while using Kotlin idioms,
where appropriate, to make the code easier to write and cleaner to use.

The biggest and most notable difference is the use of `GroupContext` instances. A `GroupContext` provides
all of the necessary state to do computation with a group, replacing a series of global variables, in 
the Python code, with instance variables inside the group context. You get a group context by calling `productionGroup()`
with an optional parameter specifying how much precomputation (and memory use) you're willing to tolerate
in response for more acceleration of the cryptographic primitives. There's also a `testGroup()`, only
available to unit tests, that operates with 32-bit primes rather than the original 256 or 4096-bit primes. This 
allows the unit tests to run radically faster, and in some cases even discover corner case bugs that would
be unlikely to manifest if the tests were operating with a production group.

If a future ElectionGuard were to, for example, offer different values for its core primes `p` and `q`,
or if it were to offer arithmetic over a very different cryptographic primitive like elliptic curves,
all of that would be abstracted away through the selection of the proper `GroupContext`. 

Generally speaking, instances of all the common types, like `ElementModP` and `ElementModQ` retain
internal pointers to the `GroupContext` in which they operate. This means that all the arithmetic
operations can be expressed naturally, with the usual `+` or `*` operator notation, and all the
context management happens under the hood. 

The expectation is that any program built using this library will call `productionGroup()` at startup
time and then retain the resulting group context, perhaps in a global variable, for use throughout
the remainder of the computation. Unit tests will instead call `testGroup()`, allowing them to run
much, much faster, and thus consider many more test cases in the same period of time.

## Fast crypto on the browser?
The current `kt-math` code is unacceptably slow, like two seconds for a single ElGamal encryption! (Node.js, version 16.)
It's unclear that we can specifically blame `kt-math` for this, since it runs at maybe 1/3 the speed of BigInteger
(~33 ElGamal encryptions per second) when it's on the JVM. That would be a factor of 60 slowdown when running in the browser.
That's too slow for production use.

The ultimate goal for the browser will be to use either [gmp-wasm](https://github.com/Daninet/gmp-wasm), which
is a WebAssembly port of the GMP library, or [hacl-wasm](https://www.npmjs.com/package/hacl-wasm), which doesn't
currently export its bignum implementation, but maybe it will in the future. At least right now, Kotlin's ability to translate
the TypeScript headers from gmp-wasm is not working right, so the general approach is to run the TypeScript importer, `dukat`,
from the command line, and then manually edit the resulting interface definitions.

Branches of note:
- `gmp-wasm-v3`: an attempt to use the `gmp-wasm` package; seems to trip bugs in both Kotlin/JS as well as bugs in `gmp-wasm`
- `sjcl-v2`: an attempt to use the Stanford JavaScript Crypto Library; runs more consistently than `gmp-wasm` but still has weird issues
- [Related bug discussion and links](https://github.com/danwallach/electionguard-kotlin-multiplatform/issues/9)

Maybe the right answer is to use the [Rice student's port of ElectionGuard to TypeScript](https://github.com/Xin128/ElectionGuard-COMP413/)
for browser-related stuff, while this repo works for virtually everything else. 

At least for now, the Kotlin/JS code is *correct*, in that all the unit tests pass, but it's not yet production-ready
due to the extreme performance costs of using it.
