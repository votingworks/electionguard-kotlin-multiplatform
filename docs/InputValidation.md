# 🗳 Input Validation
_last changed: Jan 17, 2023_

The election manifest and each input plaintext ballot are expected to be validated before being passed to the 
EKM library. 

Input Validation is orthogonal to cryptographic verification.
This document summarizes required (non-crypto) validations.

A specific validation is referenced as for example: Manifest.B.5, Ballot.A.2.1, etc.

## Manifest

Manifest Validation can be run to catch problems while developing the Manifest.

Manifest Validation must be run when accepting Input Ballots to be encrypted.

For additional safety, Manifest Validation may be run during other workflow steps.


### A. Referential integrity

<img src="./images/ReferentialIntegrity.svg" alt="ReferentialIntegrity" width="800"/>

1. Referential integrity of BallotStyle geopoliticalUnitIds.
    * For each BallotStyle, all geopoliticalUnitIds reference a GeopoliticalUnit in Manifest.geopoliticalUnitIds

2. Referential integrity of Candidate partyId.
    * For each Candidate, the partyId is null or references a Party in Manifest.parties
    
3. Referential integrity of ContestDescription geopoliticalUnitId.
    * For each ContestDescription, the geopoliticalUnitId references a GeopoliticalUnit in Manifest.geopoliticalUnitIds    

4. Referential integrity of SelectionDescription candidateId.
    * For each SelectionDescription, the candidateId references a Candidate in Manifest.candidates    

5. Every ContestDescription geopoliticalUnitId exists in some BallotStyle
   * For each ContestDescription, the geopoliticalUnitId exists in at least one Manifest.ballotStyles.geopoliticalUnitIds

### B. Duplication

1. All ContestDescription have a unique contestId.   

2. All ContestDescription have a unique sequenceOrder.  

3. Within a ContestDescription, all SelectionDescription have a unique selectionId.

4. Within a ContestDescription, all SelectionDescription have a unique sequenceOrder.

5. Within a ContestDescription, all SelectionDescription have a unique candidateId.

6. All SelectionDescription have a unique selectionId within the election

### C. Contest VoteVariationType

1. A ContestDescription has VoteVariationType = n_of_m, one_of_m, or approval.

2. A one_of_m contest has votes_allowed == 1.

3. A n_of_m contest has 0 < votes_allowed <= number of selections in the contest. 

4. An approval contest has votes_allowed == number of selections in the contest.


## Input Ballot

Input Ballot Validation can be run to catch problems while developing the Manifest; a ballot for each ballot style
should be generated and tested.

Input Ballots are generated external to the electionguard library, so Input Ballot Validation must be run on each
ballot, before accepting the ballot for encryption. 

If an Input Ballot fails validation, it is annotated as to why it failed, and placed in the invalid ballot directory for examination.

### A. Referential integrity

1. A PlaintextBallot's ballotStyleId must match a BallotStyle in Manifest.ballotStyles.

2. For each PlaintextBallot.Contest, the contestId must match a ContestDescription.contestId in Manifest.contests.

   2.1 The PlaintextBallot.Contest and matching ContestDescription must have matching sequenceOrder.

3. The matching ContestDescription's geopoliticalUnitId must be listed in the PlaintextBallot's BallotStyle.geopoliticalUnitIds.

4. Within the PlaintextBallot.Contest and matching ContestDescription, each Selection.selectionId must match a SelectionDescription.selectionId.

   4.1 The PlaintextBallot.Selection and matching SelectionDescription must have matching sequenceOrder.

### B. Duplication

1. All PlaintextBallot.Contests have a unique contestId.   

2. Within a PlaintextBallot.Contest, all Selections have a unique selectionId.

3. Within a PlaintextBallot.Contest, all Selections have a unique sequenceOrder.

### C. Voting limits

1. Each PlaintextBallot.Selection must have a vote whose value is 0 or 1.


