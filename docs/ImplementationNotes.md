# Implementation Notes for spec 1.9

_draft 6/25/2023_

IN PROGRESS

### Sequence numbers
* Sequence numbers must be unique with their containing object, and are used for canonical ordering.
  1. Contests within ballots
  2. Selections within contests
* Sequence numbers are matched across plaintext, encrypted and decrypted ballots and tallies.

### 3.1.4 Election manifest bytes

The _original_ manifest file must be kept as part of the election record. Whenever you need the manifest internally,
you must check that the manifest bytes matches the config.manifestHash, and then reparse it. Do not serialize your own 
version of the manifest, unless you can verify that it matches the original one.