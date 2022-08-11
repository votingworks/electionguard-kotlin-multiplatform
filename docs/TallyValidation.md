# 🗳 EKM Tally Validation
_last changed: Aug 10, 2022_

## Encrypted Tally

Both the encrypted and decrypted tallies may be validated against the manifest,
and against each other for consistency.

Encrypted Tally Validation may be run as part of Verification 11.

### A. Referential integrity with Manifest

1. For each EncryptedTally.Contest in the tally, the contestId must match a ContestDescription.contestId in Manifest.contests.

   1.1 The EncryptedTally.Contest contestDescriptionHash must match the corresponding ContestDescription.cryptoHash.
   
2. Within the EncryptedTally.Contest and matching ContestDescription, each EncryptedTally.Selection.selectionId must match a SelectionDescription.selectionId.
   
   2.1 The EncryptedTally.Selection selectionDescriptionHash must match the corresponding SelectionDescription.cryptoHash.

### B. Duplication

1. Each EncryptedTally.Contest must have a unique contestId.

2. Within a EncryptedTally.Contest, all EncryptedTally.Selection have a unique selectionId. 


## Plaintext Tally

Plaintext Tally Validation may be run as part of verification.

### A. Referential integrity with Manifest

1. For each PlaintextTally.Contest in the tally, the contestId must match a ContestDescription.contestId in Manifest.contests.
   
2. Within the PlaintextTally.Contest and matching ContestDescription, each PlaintextTally.Selection.selectionId must match a SelectionDescription.selectionId.

### B. Referential integrity with Ciphertext Tally

1. For each PlaintextTally.Contest in the tally, the contestId must match a EncryptedTally.Contest.contestId.
   
2. Within the PlaintextTally.Contest and matching EncryptedTally.Contest, each PlaintextTally.Selection.selectionId must match a EncryptedTally.Selection.selectionId.

   2.1 The PlaintextTally.Selection.message must compare equal to the EncryptedTally.Selection.message.
   
### C. Duplication

1. All PlaintextTally.Contest must have a unique contestId. 

   1.1 In the contests map, the key must match the value.object_id.  (JSON only)

2. Within a PlaintextTally.Contest, all PlaintextTally.Selection have a unique selectionId. 

    2.1 In the selections map, the key must match the value.object_id. (JSON only)

### D. PartialDecryption

1. All Selection shares have a selectionId matching the Selection.selectionId.

2. Within a PlaintextTally.Selection, the PartialDecryption's have a unique guardianId. 

3. There are _nguardians_ PartialDecryptions in the PlaintextTally.Selection.

### E. RecoveredPartialDecryption (when PartialDecryption contains non-empty RecoveredPartialDecryptions)

1. Each RecoveredPartialDecryption has a non-null share.

2. Each RecoveredPartialDecryption has a unique decryptingGuardianId. 

3. Each RecoveredPartialDecryption has a missingGuardianId matching the PartialDecryption's guardianId.

4. There are _navailable_ RecoveredPartialDecryptions. Note that navailable may be greater than k = quorum.
 