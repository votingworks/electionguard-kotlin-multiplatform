# 🗳 Tally Validation
_last changed: Jan 17, 2023_

<img src="./images/ReferentialIntegrity.svg" alt="ReferentialIntegrity" width="800"/>

## Encrypted Tally

Both the encrypted and decrypted tallies may be validated against the manifest,
and against each other for consistency.

Encrypted Tally Validation may be run as part of Verification 11.

### A. Referential integrity with Manifest

1. For each EncryptedTally.Contest in the tally, the contestId must match a ContestDescription.contestId in Manifest.contests.

   1.1 The EncryptedTally.Contest.contestHash must match the corresponding ContestDescription.contestHash.
   
2. Within the EncryptedTally.Contest and matching ContestDescription, each EncryptedTally.Selection.selectionId must match a SelectionDescription.selectionId.
   
   2.1 The EncryptedTally.Selection.selectionHash must match the corresponding SelectionDescription.selectionHash.

### B. Duplication

1. Each EncryptedTally.Contest must have a unique contestId.

2. Within a EncryptedTally.Contest, all EncryptedTally.Selection have a unique selectionId. 


## DecryptedBallotOrTally

DecryptedBallotOrTally validation may be run as part of verification.

### A. Referential integrity with Manifest

1. For each DecryptedTallyOrBallot.Contest in the tally, the contestId must match a ContestDescription.contestId in Manifest.contests.
   
2. Within the DecryptedTallyOrBallot.Contest and matching ContestDescription, each DecryptedTallyOrBallot.Selection.selectionId must match a SelectionDescription.selectionId.

### B. Referential integrity with EncryptedTally or EncryptedBallot

1. For each DecryptedTallyOrBallot.Contest in the tally, the contestId must match a Encrypted\[Tally|Ballot].Contest.contestId.
   
2. Within the DecryptedTallyOrBallot.Contest and matching Encrypted\[Tally|Ballot].Contest, each 
   DecryptedTallyOrBallot.Selection.selectionId must match a Encrypted\[Tally|Ballot].Selection.selectionId.

   2.1 The DecryptedTallyOrBallot.Selection.message must compare equal to the Encrypted\[Tally|Ballot].Selection.message.
   
### C. Duplication

1. All DecryptedTallyOrBallot.Contest must have a unique contestId. 

   1.1 In the contests map, the key must match the value.object_id.  (JSON only)

2. Within a DecryptedTallyOrBallot.Contest, all DecryptedTallyOrBallot.Selection have a unique selectionId. 

    2.1 In the selections map, the key must match the value.object_id. (JSON only)

### D. PartialDecryption LOOK probably not needed - waiting for 2.0 spec

1. All Selection shares have a selectionId matching the Selection.selectionId.

2. Within a DecryptedTallyOrBallot.Selection, the PartialDecryption's have a unique guardianId. 

3. There are _nguardians_ PartialDecryptions in the DecryptedTallyOrBallot.Selection.

### E. RecoveredPartialDecryption (when PartialDecryption contains non-empty RecoveredPartialDecryptions) LOOK probably not needed

1. Each RecoveredPartialDecryption has a non-null share.

2. Each RecoveredPartialDecryption has a unique decryptingGuardianId. 

3. Each RecoveredPartialDecryption has a missingGuardianId matching the PartialDecryption's guardianId.

4. There are _navailable_ RecoveredPartialDecryptions. Note that navailable may be greater than k = quorum.
 