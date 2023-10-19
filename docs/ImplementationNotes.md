# Implementation Notes for spec 2.0

_draft 8/07/2023_

IN PROGRESS

### 3.1.3 Sequence numbers (indices)
* Sequence numbers must be unique with their containing object, and are used for canonical ordering.
  1. Contests within ballots
  2. Selections within contests
* Sequence numbers are matched across plaintext, encrypted and decrypted ballots, and tallies.

### 3.1.4 Election manifest bytes

The _original_ manifest file must be kept as part of the election record. Whenever you need the manifest internally,
you must check that the manifest bytes match the config.manifestHash, and then reparse it. Do not serialize your own 
version of the manifest, unless you can verify that it matches the original one.

## Historical Notes

- EKM uses Kotlin's "multiplatform" features to support both JVM platforms (including
  Android) and "native" platforms, including iOS. The primary
  difference between these is how big integers are handled. On the JVM, we
  use [java.math.BigInteger](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/math/BigInteger.html).
  On native platforms, we use [Microsoft HACL*](https://www.microsoft.com/en-us/research/publication/hacl-a-verified-modern-cryptographic-library/).

- EKM uses an optimized encoding of an encrypted ElGamal counter, proposed by [Pereira](https://perso.uclouvain.be/olivier.pereira/). Where
  ElectionGuard 1.0 defines $\mathrm{Encrypt}(g^a, r, m) = \left(g^r, g^{ar}g^m\right)$,
  EKM instead defines $\mathrm{Encrypt}(g^a, r, m) = \left(g^r, g^{a(r + m)}\right)$.
  This allows for one fewer exponentiation per encryption. EKM includes corresponding
  changes in its Chaum-Pedersen proofs and discrete-log engine to support this.

- EKM further optimizes the Chaum-Pedersen proofs with a space-optimization from [Boneh and Shoup](http://toc.cryptobook.us/) that allows
  the larger elements-mod-p to be elided from the proofs because they can be recomputed by the verifier.

- EKM includes a Chaum-Pedersen "range proof", which is a proof that a ciphertext
  corresponds to a plaintext from 0 to a given constant. These are a generalization of the
  earlier 0-or-1 disjunctive proofs. The size of the proof will be
  linear with respect to the size of the constant, and when the constant is "1",
  the proof will be the same size as the original disjunctive proof.

- EKM defines the result of a hash function as a 256-bit unsigned integer `UInt256` type rather than a `ElementModQ`
  type. This simplifies the code in a variety of ways. The `UInt256` type is used in a number of other contexts,
  like HMAC keys, allowing those implementations to be independent of the ElGamal group parameters.
  For serialization, the output of `UInt256` type is identical to `ElementModQ`, so this is an internal
  difference compatible with other implementations.

- Pereira's "pow-radix" precomputation, used for common bases like
  the group generator or a public key, replaces modular exponentiation with
  table lookups and multiplies. We support three table sizes. Batch computations
  might then use larger tables and gain greater speedups, while memory-limited
  computations would use smaller tables.

- In this table, we transform the numbers to [Montgomery form](https://en.wikipedia.org/wiki/Montgomery_modular_multiplication), allowing us
  to avoid an expensive modulo operation after each multiply. HACL* has native support
  for this transformation, resulting in significant speedups.
  We also get modest speedups within the JVM.

- ElectionGuard defines two sets of global parameters: using either 3072-bit
  or 4096-bit modular arithmetic. EKM supports both sets of parameters as well as a
  "tiny" set of 32-bit parameters, used to radically speed up the unit tests.

- All the code in EKM is thread-safe, and it's mostly functional. This
  means that you can easily use libraries like [Java Streams](https://docs.oracle.com/javase/tutorial/collections/streams/parallelism.html)
  to do parallel operations on a multicore computer. Similarly, for a voting
  machine, you might take advantage of [Kotlin's coroutines](https://kotlinlang.org/docs/coroutines-guide.html)
  to run an encryption in the background without creating lag on the UI thread.

### What about JavaScript?

We tried to include JavaScript, which you can see at the tag [BEFORE_REMOVING_JS](https://github.com/danwallach/electionguard-kotlin-multiplatform/releases/tag/BEFORE_REMOVING_JS). This
used a big integer library called [kt-math](https://github.com/gciatto/kt-math),
but the performance was not acceptable.

Instead, please check out [ElectionGuard-TypeScript](https://github.com/danwallach/ElectionGuard-TypeScript),
which is fully compatible with ElectionGuard 1.0 and runs very efficiently,
taking advantage of the built-in `bigint` type of modern JavaScript engines.
If we were going to bring back Kotlin/JS as an EKM target platform, we'd probably borrow the "core"
cryptographic classes from ElectionGuard-TypeScript, and build up the rest
of the ballot abstractions in Kotlin.

We note that the Kotlin team is actively developing a [WebAssembly backend](https://youtrack.jetbrains.com/issue/KT-46773).
If this ultimately supports foreign function calls to C functions, then the
"native" version of EKM, including HACL* for big integer arithmetic, could potentially run in a JavaScript WASM engine.
Alternatively, the Kotlin team is working on portable support for [BigInteger and
BigDecimal](https://youtrack.jetbrains.com/issue/KT-20912/BigDecimalBigInteger-types-in-Kotlin-stdlib), which we
could use here when it's ready.

### API differences from ElectionGuard-Python

The biggest and most notable difference is the use of [GroupContext](../egklib/src/commonMain/kotlin/electionguard/core/GroupCommon.kt)
instances. A `GroupContext` provides
all the necessary state to do computation with a group, replacing a series of global variables, in
the Python code, with instance variables inside the group context. You get a group context by calling
[productionGroup()](../egklib/src/commonMain/kotlin/electionguard/core/Group.kt)
with an optional parameter specifying how much precomputation (and memory use) you're willing to tolerate
in response for more acceleration of the cryptographic primitives. There's also a [tinyGroup()](../egklib/src/commonTest/kotlin/electionguard/core/TinyGroup.kt), only
available to unit tests, that operates with 32-bit primes rather than the original 256 or 4096-bit primes. This
allows the unit tests to run radically faster, and in some cases even discover corner case bugs that would
be unlikely to manifest if the tests were operating with a production group.

As a general rule, we try to use Kotlin's language features to make the code
simpler and cleaner. For example, we use Kotlin's operator overloading such
that math operations on `ElementModP` and `ElementModQ` can be written with
infix notation rather than function calls. We also implemented many
[extension functions](https://kotlinlang.org/docs/extensions.html), so if you have a value of any EKM type and you type
a period, the IDE's autocomplete menu should offer you a variety of useful
methods.