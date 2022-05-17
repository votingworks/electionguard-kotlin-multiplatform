# 🗳 Election Record serialization (proposed specification)

draft 5/16/2022 for proto_version = 2.0.0 (MAJOR.MINOR.PATCH)

Notes

1. All fields must be present unless marked as optional.
2. A missing (optional) String should be internally encoded as null (not empty string), to agree with python hashing.
3. Proto_version uses [semantic versioning](https://semver.org/)

## common.proto

### message ElementModQ

| Name  | Type  | Notes                                           |
|-------|-------|-------------------------------------------------|
| value | bytes | unsigned, big-endian, 0 left-padded to 32 bytes |

### message ElementModP

| Name  | Type   | Notes                                            |
|-------|--------|--------------------------------------------------|
| value | bytes  | unsigned, big-endian, 0 left-padded to 512 bytes |

### message ElGamalCiphertext

| Name | Type        | Notes |
|------|-------------|-------|
| pad  | ElementModP |       |
| data | ElementModP |       |

### message GenericChaumPedersenProof

| Name      | Type           | Notes   |
|-----------|----------------|---------|
| challenge | ElementModQ    |         |
| response  | ElementModQ    |         |

### message HashedElGamalCiphertext

| Name     | Type        | Notes |
|----------|-------------|-------|
| c0       | ElementModP |       |
| c1       | bytes       |       |
| c2       | UInt256     |       |
| numBytes | uint32      |       |

### message SchnorrProof

| Name       | Type        | Notes   |
|------------|-------------|---------|
| challenge  | ElementModQ |         |
| response   | ElementModQ |         |

### message UInt256

| Name  | Type  | Notes                                      |
|-------|-------|--------------------------------------------|
| value | bytes | unsigned, big-endian, 0 padded to 32 bytes |


## election_record.proto

### message ElectionConfig

| Name                | Type                | Notes                |
|---------------------|---------------------|----------------------|
| proto_version       | string              | proto schema version |
| constants           | ElectionConstants   | key ceremony         |
| manifest            | Manifest            | key ceremony         |
| number_of_guardians | uint32              |                      |
| quorum              | uint32              |                      |
| metadata            | map<string, string> |                      |

### message ElectionConstants

| Name        | Type    | Notes                             |
|-------------|---------|-----------------------------------|
| name        | string  |                                   |
| large_prime | bytes   | bigint is unsigned and big-endian |
| small_prime | bytes   | bigint is unsigned and big-endian |
| cofactor    | bytes   | bigint is unsigned and big-endian |
| generator   | bytes   | bigint is unsigned and big-endian |

### message ElectionInitialized

| Name                      | Type                | Notes |
|---------------------------|---------------------|-------|
| config                    | ElectionConfig      |       |
| elgamal_public_key        | ElementModP         |       |
| manifest_hash             | UInt256             |       |
| crypto_base_hash          | UInt256             | Q     |
| crypto_extended_base_hash | UInt256             | Qbar  |
| guardians                 | List\<Guardian\>    |       |
| metadata                  | map<string, string> |       |

### message GuardianRecord

| Name                    | Type                 | Notes                          |
|-------------------------|----------------------|--------------------------------|
| guardian_id             | string               |                                |
| x_coordinate            | uint32               | x_coordinate in the polynomial |
| coefficient_commitments | List\<ElementModP\>  |                                |
| coefficient_proofs      | List\<SchnorrProof\> |                                |

### message TallyResult

| Name             | Type                | Notes |
|------------------|---------------------|-------|
| election_init    | ElectionInitialized |       |
| ciphertext_tally | CiphertextTally     |       |
| ballot_ids       | List<string>        |       |
| tally_ids        | List<string>        |       |

### message DecryptionResult

| Name                 | Type                      | Notes |
|----------------------|---------------------------|-------|
| tally_result         | TallyResult               |       |
| decrypted_tally      | PlaintextTally            |       |
| decrypting_guardians | List\<AvailableGuardian\> |       |
| metadata             | map<string, string>       |       |

### message AvailableGuardian

| Name                 | Type        | Notes                          |
|----------------------|-------------|--------------------------------|
| guardian_id          | string      |                                |
| x_coordinate         | string      | x_coordinate in the polynomial |
| lagrange_coefficient | ElementModQ |                                |


## manifest.proto

### message Manifest

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
| name                | InternationalizedText      | optional                     |
| contact_information | ContactInformation         | optional                     |
| crypto_hash         | UInt256                    | optional                     |

### message AnnotatedString

| Name       | Type   | Notes |
|------------|--------|-------|
| annotation | string |       |
| value      | string |       |

### message BallotStyle

| Name                  | Type           | Notes                                         |
|-----------------------|----------------|-----------------------------------------------|
| ballot_style_id       | string         |                                               |
| geopolitical_unit_ids | List\<string\> | matches GeoPoliticalUnit.geopolitical_unit_id |
| party_ids             | List\<string\> | optional matches Party.party_id               |
| image_uri             | string         | optional                                      |

### message Candidate

| Name         | Type                  | Notes                           |
|--------------|-----------------------|---------------------------------|
| candidate_id | string                |                                 |
| name         | InternationalizedText |                                 |
| party_id     | string                | optional matches Party.party_id |
| image_uri    | string                | optional                        |
| is_write_in  | bool                  |                                 |

### message ContactInformation

| Name         | Type           | Notes    |
|--------------|----------------|----------|
| name         | string         | optional |
| address_line | List\<string\> | optional |
| email        | List\<string\> | optional |
| phone        | List\<string\> | optional |

### message GeopoliticalUnit

| Name                 | Type                   | Notes    |
|----------------------|------------------------|----------|
| geopolitical_unit_id | string                 |          |
| name                 | string                 |          |
| type                 | enum ReportingUnitType |          |
| contact_information  | ContactInformation     | optional |

### message InternationalizedText

| Name | Type             | Notes |
|------|------------------|-------|
| text | List\<Language\> |       |

### message Language

| Name     | Type   | Notes |
|----------|--------|-------|
| value    | string |       |
| language | string |       |

### message Party

| Name         | Type                  | Notes    |
|--------------|-----------------------|----------|
| party_id     | string                |          |
| name         | InternationalizedText |          |
| abbreviation | string                | optional |
| color        | string                | optional |
| logo_uri     | string                | optional |

### message ContestDescription

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
| ballot_title         | InternationalizedText        | optional                                      |
| ballot_subtitle      | InternationalizedText        | optional                                      |
| primary_party_ids    | List\<string\>               | optional, match Party.party_id                |
| crypto_hash          | UInt256                      | optional                                      |

### message SelectionDescription

| Name           | Type     | Notes                          |
|----------------|----------|--------------------------------|
| selection_id   | string   |                                |
| sequence_order | uint32   | unique within contest          |
| candidate_id   | string   | matches Candidate.candidate_id |
| crypto_hash    | UInt256  | optional                       |


## plaintext_ballot.proto

### message PlaintextBallot

| Name            | Type                           | Notes                               |
|-----------------|--------------------------------|-------------------------------------|
| ballot_id       | string                         | unique input ballot id              |
| ballot_style_id | string                         | matches BallotStyle.ballot_style_id |
| contests        | List\<PlaintextBallotContest\> |                                     |
| errors          | string                         | optional eg an invalid ballot       |

### message PlaintextBallotContest

| Name           | Type                             | Notes                             |
|----------------|----------------------------------|-----------------------------------|
| contest_id     | string                           | ContestDescription.contest_id     |
| sequence_order | uint32                           | ContestDescription.sequence_order |
| selections     | List\<PlaintextBallotSelection\> |                                   |

### message PlaintextBallotSelection

| Name                     | Type   | Notes                               |
|--------------------------|--------|-------------------------------------|
| selection_id             | string | SelectionDescription.selection_id   |
| sequence_order           | uint32 | SelectionDescription.sequence_order |
| vote                     | uint32 |                                     |
| extended_data            | string | optional                            |


## ciphertext_ballot.proto

### message SubmittedBallot

| Name              | Type                            | Notes                            |
|-------------------|---------------------------------|----------------------------------|
| ballot_id         | string                          | PlaintextBallot.ballot_id        |
| ballot_style_id   | string                          | BallotStyle.ballot_style_id      |
| manifest_hash     | UInt256                         | Manifest.crypto_hash             |
| code_seed         | UInt256                         |                                  |
| code              | UInt256                         |                                  |
| contests          | List\<CiphertextBallotContest\> |                                  |
| timestamp         | int64                           | seconds since the unix epoch UTC |
| crypto_hash       | UInt256                         |                                  |
| state             | enum BallotState                | CAST, SPOILED                    |

### message CiphertextBallotContest

| Name                    | Type                              | Notes                                     |
|-------------------------|-----------------------------------|-------------------------------------------|
| contest_id              | string                            | matches ContestDescription.contest_id     |
| sequence_order          | uint32                            | matches ContestDescription.sequence_order |
| contest_hash            | UInt256                           | matches ContestDescription.crypto_hash    |                                                                     |
| selections              | List\<CiphertextBallotSelection\> |                                           |
| crypto_hash             | UInt256                           |                                           |
| proof                   | ConstantChaumPedersenProof        |                                           |

### message CiphertextBallotSelection

| Name                     | Type                          | Notes                                       |
|--------------------------|-------------------------------|---------------------------------------------|
| selection_id             | string                        | matches SelectionDescription.selection_id   |
| sequence_order           | uint32                        | matches SelectionDescription.sequence_order |
| selection_hash           | UInt256                       | matches SelectionDescription.crypto_hash    |
| ciphertext               | ElGamalCiphertext             |                                             |
| crypto_hash              | UInt256                       |                                             |
| is_placeholder_selection | bool                          |                                             |
| proof                    | DisjunctiveChaumPedersenProof |                                             |
| extended_data            | HashedElGamalCiphertext       | optional                                    |

### message ConstantChaumPedersenProof

| Name      | Type                      | Notes |
|-----------|---------------------------|-------|
| constant  | uint32                    |       |
| proof     | GenericChaumPedersenProof |       |

### message DisjunctiveChaumPedersenProof

| Name      | Type                      | Notes |
|-----------|---------------------------|-------|
| challenge | ElementModQ               |       |
| proof0    | GenericChaumPedersenProof |       |
| proof1    | GenericChaumPedersenProof |       |


## ciphertext_tally.proto

### message CiphertextTally

| Name     | Type                           | Notes                                                             |
|----------|--------------------------------|-------------------------------------------------------------------|
| tally_id | string                         | when decrypted spoiled ballots, matches SubmittedBallot.ballot_id |
| contests | List\<CiphertextTallyContest\> |                                                                   | 

### message CiphertextTallyContest

| Name                     | Type                             | Notes                             |
|--------------------------|----------------------------------|-----------------------------------|
| contest_id               | string                           | ContestDescription.contest_id     |
| sequence_order           | uint32                           | ContestDescription.sequence_order |
| contest_description_hash | UInt256                          | ContestDescription.crypto_hash    |
| selections               | List\<CiphertextTallySelection\> |                                   |

### message CiphertextTallySelection

| Name                       | Type              | Notes                               |
|----------------------------|-------------------|-------------------------------------|
| selection_id               | string            | SelectionDescription.selection_id   |
| sequence_order             | uint32            | SelectionDescription.sequence_order |
| selection_description_hash | UInt256           | SelectionDescription.crypto_hash    |
| ciphertext                 | ElGamalCiphertext |                                     |


## plaintext_tally.proto

### message PlaintextTally

| Name     | Type                          | Notes                                                             |
|----------|-------------------------------|-------------------------------------------------------------------|
| tally_id | string                        | when decrypted spoiled ballots, matches SubmittedBallot.ballot_id |
| contests | List\<PlaintextTallyContest\> |                                                                   |

### message PlaintextTallyContest

| Name       | Type                            | Notes                         |
|------------|---------------------------------|-------------------------------|
| contest_id | string                          | ContestDescription.contest_id |
| selections | List\<PlaintextTallySelection\> |                               |

### message PlaintextTallySelection

| Name                | Type                        | Notes                                   |
|---------------------|-----------------------------|-----------------------------------------|
| selection_id        | string                      | SelectionDescription.selection_id       |
| tally               | int                         | decrypted vote count                    |
| value               | ElementModP                 | g^tally or M in the spec                |
| message             | ElGamalCiphertext           | encrypted vote count                    |
| partial_decryptions | List\<PartialDecryption\>   | direct or recovered, nguardians of them |

### message PartialDecryption

| Name            | Type                               | Notes          |
|-----------------|------------------------------------|----------------|
| selection_id    | string                             |                |
| guardian_id     | string                             |                |
| share           | ElementModP                        | M_i            |
| proof           | GenericChaumPedersenProof          | only direct    |
| recovered_parts | List\<MissingPartialDecryption\>   | only recovered |

### message MissingPartialDecryption

| Name                   | Type                      | Notes |
|------------------------|---------------------------|-------|
| decrypting_guardian_id | string                    |       |
| missing_guardian_id    | string                    |       |
| share                  | ElementModP               | M_il  |
| recovery_key           | ElementModP               |       |
| proof                  | GenericChaumPedersenProof |       |