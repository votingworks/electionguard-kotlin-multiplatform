# 🗳 Election Record KMP serialization (proposed specification)

draft 7/15/2023

**Table of Contents**
 
<!-- TOC -->
* [🗳 Election Record KMP serialization (proposed specification)](#-election-record-kmp-serialization-proposed-specification)
  * [common.proto](#commonproto)
      * [message ContestData](#message-contestdata)
      * [message ElementModQ](#message-elementmodq)
      * [message ElementModP](#message-elementmodp)
      * [message ElGamalCiphertext](#message-elgamalciphertext)
      * [message ChaumPedersenProof](#message-chaumpedersenproof)
      * [message HashedElGamalCiphertext](#message-hashedelgamalciphertext)
      * [message SchnorrProof](#message-schnorrproof)
      * [message UInt256](#message-uint256)
  * [manifest.proto](#manifestproto)
      * [message Manifest](#message-manifest)
      * [message BallotStyle](#message-ballotstyle)
      * [message Candidate](#message-candidate)
      * [message ContactInformation](#message-contactinformation)
      * [message GeopoliticalUnit](#message-geopoliticalunit)
      * [message Language](#message-language)
      * [message Party](#message-party)
      * [message ContestDescription](#message-contestdescription)
      * [message SelectionDescription](#message-selectiondescription)
  * [election_record.proto](#electionrecordproto)
      * [message ElectionConfig](#message-electionconfig)
      * [message ElectionConstants](#message-electionconstants)
      * [message ElectionInitialized](#message-electioninitialized)
      * [message Guardian](#message-guardian)
      * [message EncryptedBallotChain](#message-encryptedballotchain)
      * [message TallyResult](#message-tallyresult)
      * [message DecryptionResult](#message-decryptionresult)
  * [plaintext_ballot.proto](#plaintextballotproto)
      * [message PlaintextBallot](#message-plaintextballot)
      * [message PlaintextBallotContest](#message-plaintextballotcontest)
      * [message PlaintextBallotSelection](#message-plaintextballotselection)
  * [encrypted_ballot.proto](#encryptedballotproto)
      * [message EncryptedBallot](#message-encryptedballot)
      * [message EncryptedBallotContest](#message-encryptedballotcontest)
      * [message EncryptedBallotSelection](#message-encryptedballotselection)
      * [message ChaumPedersenRangeProofKnownNonce](#message-chaumpedersenrangeproofknownnonce)
      * [message PreEncryption](#message-preencryption)
      * [message PreEncryptionVector](#message-preencryptionvector)
  * [encrypted_tally.proto](#encryptedtallyproto)
      * [message EncryptedTally](#message-encryptedtally)
      * [message EncryptedTallyContest](#message-encryptedtallycontest)
      * [message EncryptedTallySelection](#message-encryptedtallyselection)
  * [decrypted_tally.proto](#decryptedtallyproto)
    * [message DecryptedTallyOrBallot](#message-decryptedtallyorballot)
    * [message DecryptedContest](#message-decryptedcontest)
    * [message DecryptedSelection](#message-decryptedselection)
    * [message DecryptedContestData](#message-decryptedcontestdata)
<!-- TOC -->

## common.proto

[schema](../egklib/src/commonMain/proto/common.proto)

#### message ContestData

| Name       | Type           | Notes                                    |
|------------|----------------|------------------------------------------|
| status     | enum Status    | normal, null_vote, over_vote, under_vote |
| over_votes | List\<uint32\> | list of selection sequence_order         |
| write_ins  | List\<string\> | write in candidates(s)                   |

#### message ElementModQ

| Name  | Type  | Notes                                           |
|-------|-------|-------------------------------------------------|
| value | bytes | unsigned, big-endian, 0 left-padded to 32 bytes |

#### message ElementModP

| Name  | Type  | Notes                                            |
|-------|-------|--------------------------------------------------|
| value | bytes | unsigned, big-endian, 0 left-padded to 512 bytes |

#### message ElGamalCiphertext

| Name | Type        | Notes   |
|------|-------------|---------|
| pad  | ElementModP | g^R     |
| data | ElementModP | K^(V+R) |

#### message ChaumPedersenProof

| Name      | Type        | Notes |
|-----------|-------------|-------|
| challenge | ElementModQ | c     |
| response  | ElementModQ | v     |

#### message HashedElGamalCiphertext

| Name | Type        | Notes |
|------|-------------|-------|
| c0   | ElementModP |       |
| c1   | bytes       |       |
| c2   | UInt256     |       |

#### message SchnorrProof

| Name       | Type        | Notes |
|------------|-------------|-------|
| public_key | ElementModP | K_ij  |
| challenge  | ElementModQ | c_ij  |
| response   | ElementModQ | v_ij  |

#### message UInt256

| Name  | Type  | Notes                                           |
|-------|-------|-------------------------------------------------|
| value | bytes | unsigned, big-endian, 0 left-padded to 32 bytes |

## manifest.proto

[schema](../egklib/src/commonMain/proto/manifest.proto)

#### message Manifest

| Name                | Type                       | Notes                        |
|---------------------|----------------------------|------------------------------|
| election_scope_id   | string                     |                              |
| spec_version        | string                     | the reference SDK version    |
| election_type       | enum ElectionType          |                              |
| start_date          | string                     | ISO 8601 formatted date/time |
| end_date            | string                     | ISO 8601 formatted date/time |
| geopolitical_units  | List\<GeopoliticalUnit\>   |                              |
| parties             | List\<Party\>              |                              |
| candidates          | List\<Candidate\>          |                              |
| contests            | List\<ContestDescription\> |                              |
| ballot_styles       | List\<BallotStyle\>        |                              |
| name                | Language                   | optional                     |
| contact_information | ContactInformation         | optional                     |

#### message BallotStyle

| Name                  | Type           | Notes                                 |
|-----------------------|----------------|---------------------------------------|
| ballot_style_id       | string         |                                       |
| geopolitical_unit_ids | List\<string\> | GeoPoliticalUnit.geopolitical_unit_id |
| party_ids             | List\<string\> | optional matches Party.party_id       |
| image_uri             | string         | optional                              |

#### message Candidate

| Name         | Type   | Notes                           |
|--------------|--------|---------------------------------|
| candidate_id | string |                                 |
| name         | string |                                 |
| party_id     | string | optional matches Party.party_id |
| image_uri    | string | optional                        |
| is_write_in  | bool   |                                 |

#### message ContactInformation

| Name         | Type           | Notes    |
|--------------|----------------|----------|
| name         | string         | optional |
| address_line | List\<string\> | optional |
| email        | List\<string\> | optional |
| phone        | List\<string\> | optional |

#### message GeopoliticalUnit

| Name                 | Type                   | Notes    |
|----------------------|------------------------|----------|
| geopolitical_unit_id | string                 |          |
| name                 | string                 |          |
| type                 | enum ReportingUnitType |          |
| contact_information  | string                 | optional |

#### message Language

| Name     | Type   | Notes |
|----------|--------|-------|
| value    | string |       |
| language | string |       |

#### message Party

| Name         | Type   | Notes    |
|--------------|--------|----------|
| party_id     | string |          |
| name         | string |          |
| abbreviation | string | optional |
| color        | string | optional |
| logo_uri     | string | optional |

#### message ContestDescription

| Name                 | Type                         | Notes                                         |
|----------------------|------------------------------|-----------------------------------------------|
| contest_id           | string                       |                                               |
| sequence_order       | uint32                       | unique within manifest                        |
| geopolitical_unit_id | string                       | matches GeoPoliticalUnit.geopolitical_unit_id |
| vote_variation       | enum VoteVariationType       |                                               |
| number_elected       | uint32                       |                                               |
| votes_allowed        | uint32                       |                                               |
| name                 | string                       |                                               |
| selections           | List\<SelectionDescription\> |                                               |
| ballot_title         | string                       | optional                                      |
| ballot_subtitle      | string                       | optional                                      |

#### message SelectionDescription

| Name           | Type    | Notes                          |
|----------------|---------|--------------------------------|
| selection_id   | string  |                                |
| sequence_order | uint32  | unique within contest          |
| candidate_id   | string  | matches Candidate.candidate_id |

## election_record.proto

[schema](../egklib/src/commonMain/proto/election_record.proto)

#### message ElectionConfig

| Name                     | Type                  | Notes                                          |
|--------------------------|-----------------------|------------------------------------------------|
| spec_version             | string                | "v2.0.0"                                       |
| constants                | ElectionConstants     |                                                |
| number_of_guardians      | uint32                | n                                              |
| quorum                   | uint32                | k                                              |
| election_date            | string                | k                                              |
| jurisdiction_info        | string                | k                                              |
| parameter_base_hash      | UInt256               | Hp                                             |
| manifest_hash            | UInt256               | Hm                                             |
| election_base_hash       | UInt256               | He                                             |
| manifest_bytes           | bytes                 |                                                |
| baux0                    | bytes                 | B_aux,0 from eq 59,60                          |
| voting_device            | string                | voting device information, section 3.4.3, 3.7  |
| chain_confirmation_codes | bool                  |                                                |
| metadata                 | map\<string, string\> | arbitrary                                      |

#### message ElectionConstants

| Name        | Type   | Notes                             |
|-------------|--------|-----------------------------------|
| name        | string |                                   |
| large_prime | bytes  | bigint is unsigned and big-endian |
| small_prime | bytes  | bigint is unsigned and big-endian |
| cofactor    | bytes  | bigint is unsigned and big-endian |
| generator   | bytes  | bigint is unsigned and big-endian |

#### message ElectionInitialized

| Name               | Type                  | Notes     |
|--------------------|-----------------------|-----------|
| config             | ElectionConfig        |           |
| joint_public_key   | ElementModP           | K         |
| extended_base_hash | UInt256               | He        |
| guardians          | List\<Guardian\>      | i = 1..n  |
| metadata           | map\<string, string\> | arbitrary |

#### message Guardian

| Name               | Type                 | Notes                                 |
|--------------------|----------------------|---------------------------------------|
| guardian_id        | string               |                                       |
| x_coordinate       | uint32               | x_coordinate in the polynomial, ℓ = i |
| coefficient_proofs | List\<SchnorrProof\> | j = 0..k-1                            |

#### message EncryptedBallotChain

| Name                    | Type                  | Notes               |
|-------------------------|-----------------------|---------------------|
| encrypting_device       | string                |                     |
| baux0                   | bytes                 | eq 59               |
| ballot_ids              | List\<string\>        | ballot ids in order |
| last_confirmation_code  | UInt256               | last in chain       |
| chaining                | bool                  | if chaining         |
| closing_hash            | UInt256               | eq 62               |
| metadata                | map\<string, string\> | arbitrary           |

#### message TallyResult

| Name            | Type                  | Notes               |
|-----------------|-----------------------|---------------------|
| election_init   | ElectionInitialized   |                     |
| encrypted_tally | EncryptedTally        |                     |
| tally_ids       | List\<string\>        | included tally ids  |
| metadata        | map\<string, string\> |                     |

#### message DecryptionResult

| Name                 | Type                       | Notes |
|----------------------|----------------------------|-------|
| tally_result         | TallyResult                |       |
| decrypted_tally      | DecryptedTallyOrBallot     |       |
| metadata             | map<string, string>        |       |

## plaintext_ballot.proto

[schema](../egklib/src/commonMain/proto/plaintext_ballot.proto)

#### message PlaintextBallot

| Name            | Type                           | Notes                              |
|-----------------|--------------------------------|------------------------------------|
| ballot_id       | string                         | unique input ballot id             |
| ballot_style_id | string                         | BallotStyle.ballot_style_id        |
| contests        | List\<PlaintextBallotContest\> |                                    |
| errors          | string                         | optional, eg for an invalid ballot |

#### message PlaintextBallotContest

| Name           | Type                             | Notes                             |
|----------------|----------------------------------|-----------------------------------|
| contest_id     | string                           | ContestDescription.contest_id     |
| sequence_order | uint32                           | ContestDescription.sequence_order |
| selections     | List\<PlaintextBallotSelection\> |                                   |
| write_ins      | List\<string\>                   | optional                          |

#### message PlaintextBallotSelection

| Name           | Type   | Notes                               |
|----------------|--------|-------------------------------------|
| selection_id   | string | SelectionDescription.selection_id   |
| sequence_order | uint32 | SelectionDescription.sequence_order |
| vote           | uint32 |                                     |

## encrypted_ballot.proto

[schema](../egklib/src/commonMain/proto/encrypted_ballot.proto)

#### message EncryptedBallot

| Name              | Type                           | Notes                                         |
|-------------------|--------------------------------|-----------------------------------------------|
| ballot_id         | string                         | PlaintextBallot.ballot_id                     |
| ballot_style_id   | string                         | BallotStyle.ballot_style_id                   |
| voting_device     | string                         | voting device information, section 3.4.3, 3.7 |
| confirmation_code | UInt256                        | tracking code, H_i                            |
| code_baux         | bytes                          | B_aux in eq 96                                |
| contests          | List\<EncryptedBallotContest\> |                                               |
| timestamp         | int64                          | seconds since the unix epoch UTC              |
| state             | enum BallotState               | CAST, SPOILED                                 |
| is_preencrypt     | bool                           |                                               |

#### message EncryptedBallotContest

| Name                   | Type                               | Notes                              |
|------------------------|------------------------------------|------------------------------------|
| contest_id             | string                             | ContestDescription.contest_id      |
| sequence_order         | uint32                             | ContestDescription.sequence_order  |
| votes_allowed          | uint32                             | ContestDescription.votes_allowed   |
| contest_hash           | UInt256                            | eq 58                              |
| selections             | List\<EncryptedBallotSelection\>   |                                    |
| proof                  | ChaumPedersenRangeProofKnownNonce  | proof of votes <= limit            |
| encrypted_contest_data | HashedElGamalCiphertext            |                                    |
| pre_encryption         | PreEncryption                      |                                    |

#### message EncryptedBallotSelection

| Name           | Type                              | Notes                               |
|----------------|-----------------------------------|-------------------------------------|
| selection_id   | string                            | SelectionDescription.selection_id   |
| sequence_order | uint32                            | SelectionDescription.sequence_order |
| encrypted_vote | ElGamalCiphertext                 |                                     |
| proof          | ChaumPedersenRangeProofKnownNonce | proof vote = 0 or 1                 |

#### message ChaumPedersenRangeProofKnownNonce

| Name  | Type                       | Notes |
|-------|----------------------------|-------|
| proof | List\<ChaumPedersenProof\> |       |

#### message PreEncryption

| Name                 | Type                    | Notes                                           |
|----------------------|-------------------------|-------------------------------------------------|
| preencryption_hash   | UInt256                 | eq 95                                           |
| all_selection_hashes | List\<UInt256\>         | size = nselections + limit ; sorted numerically |
| selected_vectors     | List\<SelectionVector\> | size = limit ; sorted numerically               |

#### message PreEncryptionVector

| Name            | Type                      | Notes                                              |
|-----------------|---------------------------|----------------------------------------------------|
| selection_hash  | UInt256                   | eq 93                                              |
| short_code      | String                    |                                                    |
| selected_vector | List\<ElGamalCiphertext\> | Ej, size = nselections, in order by sequence_order |

## encrypted_tally.proto

[schema](../egklib/src/commonMain/proto/encrypted_tally.proto)

#### message EncryptedTally

| Name            | Type                          | Notes               |
|-----------------|-------------------------------|---------------------|
| tally_id        | string                        |                     |
| contests        | List\<EncryptedTallyContest\> |                     | 
| cast_ballot_ids | List\<string\>                | included ballot ids |

#### message EncryptedTallyContest

| Name                     | Type                            | Notes                             |
|--------------------------|---------------------------------|-----------------------------------|
| contest_id               | string                          | ContestDescription.contest_id     |
| sequence_order           | uint32                          | ContestDescription.sequence_order |
| selections               | List\<EncryptedTallySelection\> |                                   |

#### message EncryptedTallySelection

| Name            | Type              | Notes                                                 |
|-----------------|-------------------|-------------------------------------------------------|
| selection_id    | string            | SelectionDescription.selection_id                     |
| sequence_order  | uint32            | SelectionDescription.sequence_order                   |
| encrypted_vote  | ElGamalCiphertext | accumulation over all cast ballots for this selection |

## decrypted_tally.proto

[schema](../egklib/src/commonMain/proto/decrypted_tally.proto)

### message DecryptedTallyOrBallot

| Name     | Type                     | Notes               |
|----------|--------------------------|---------------------|
| id       | string                   | tallyId or ballotId |
| contests | List\<DecryptedContest\> |                     |

### message DecryptedContest

| Name                   | Type                       | Notes                            |
|------------------------|----------------------------|----------------------------------|
| contest_id             | string                     | ContestDescription.contest_id    |
| selections             | List\<DecryptedSelection\> |                                  |
| decrypted_contest_data | DecryptedContestData       | optional, ballot decryption only |

### message DecryptedSelection

| Name           | Type               | Notes                             |
|----------------|--------------------|-----------------------------------|
| selection_id   | string             | SelectionDescription.selection_id |
| tally          | int                | decrypted vote count              |
| b_over_m       | ElementModP        | T = (B/M), eq 65                  |
| encrypted_vote | ElGamalCiphertext  | encrypted vote count              |
| proof          | ChaumPedersenProof |                                   |

### message DecryptedContestData

| Name                   | Type                    | Notes                                     |
|------------------------|-------------------------|-------------------------------------------|
| contest_data           | ContestData             |                                           |
| encrypted_contest_data | HashedElGamalCiphertext | see 3.3.3. matches EncryptedBallotContest |
| proof                  | ChaumPedersenProof      |                                           |
| beta                   | ElementModP             | β = C0^s mod p                            |

