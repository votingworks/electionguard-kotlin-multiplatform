# Cryptography Notes for spec 2.0

_8/07/2023_

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