# 🗳 Election Record serialization (proposed specification)

draft 4/12/2022 for proto_version = 2.0.0 (MAJOR.MINOR.PATCH)

This covers only the election record, and not any serialized classes used in remote procedure calls 
or private data.

Notes

1. All fields must be present unless marked as optional.
2. Proto_version uses [semantic versioning](https://semver.org/)

## common.proto

### message ElementModQ, ElementModP

| Name  | Type  | Notes                                           |
|-------|-------|-------------------------------------------------|
| value | bytes | bigint is variable length, unsigned, big-endian |

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
| public_key | ElementModP |         |
| challenge  | ElementModQ |         |
| response   | ElementModQ |         |

### message UInt256

| Name  | Type   | Notes                                     |
|-------|--------|-------------------------------------------|
| value | bytes  | bigint is 32 bytes, unsigned, big-endian  |


## election_record.proto

### message ElectionRecord

| Name                | Type                      | Notes                |
|---------------------|---------------------------|----------------------|
| proto_version       | string                    | proto schema version |
| constants           | ElectionConstants         | key ceremony         |
| manifest            | Manifest                  | key ceremony         |
| context             | ElectionContext           | key ceremony         |
| guardian_records    | List\<GuardianRecord\>    | key ceremony         |
| devices             | List\<EncryptionDevice\>  | key ceremony ??      |
| ciphertext_tally    | CiphertextTally           | accumulate tally     |
| plaintext_tally     | PlaintextTally            | decrypt tally        |
| available_guardians | List\<AvailableGuardian\> | decrypt tally        |

### message AvailableGuardian

| Name                    | Type   | Notes                          |
|-------------------------|--------|--------------------------------|
| guardian_id             | string |                                |
| x_coordinate            | string | x_coordinate in the polynomial |
| lagrange_coordinate_int | sint32 |                                |

### message ElectionConstants

| Name        | Type    | Notes                             |
|-------------|---------|-----------------------------------|
| name        | string  |                                   |
| large_prime | bytes   | bigint is unsigned and big-endian |
| small_prime | bytes   | bigint is unsigned and big-endian |
| cofactor    | bytes   | bigint is unsigned and big-endian |
| generator   | bytes   | bigint is unsigned and big-endian |

### message ElectionContext

| Name                      | Type                  | Notes    |
|---------------------------|-----------------------|----------|
| number_of_guardians       | uint32                |          |
| quorum                    | uint32                |          |
| elgamal_public_key        | ElementModP           |          |
| commitment_hash           | UInt256               |          |
| manifest_hash             | UInt256               |          |
| crypto_base_hash          | UInt256               |          |
| crypto_extended_base_hash | UInt256               |          |
| extended_data             | map\<string, string\> | optional |

### message EncryptionDevice

| Name        | Type   | Notes                                  |
|-------------|--------|----------------------------------------|
| device_id   | int64  | was uuid LOOK maybe just use a string? |
| session_id  | int64  |                                        |
| launch_code | int64  |                                        |
| location    | string |                                        |

### message GuardianRecord

| Name                    | Type                 | Notes                          |
|-------------------------|----------------------|--------------------------------|
| guardian_id             | string               |                                |
| x_coordinate            | uint32               | x_coordinate in the polynomial |
| guardian_public_key     | ElementModP          |                                |
| coefficient_commitments | List\<ElementModP\>  |                                |
| coefficient_proofs      | List\<SchnorrProof\> |                                |

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
| sequence_order       | uint32                       | deterministic sorting                         |
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
| sequence_order | uint32   | deterministic sorting          |
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

| Name           | Type                             | Notes                                     |
|----------------|----------------------------------|-------------------------------------------|
| contest_id     | string                           | matches ContestDescription.contest_id     |
| sequence_order | uint32                           | matches ContestDescription.sequence_order |
| selections     | List\<PlaintextBallotSelection\> |                                           |

### message PlaintextBallotSelection

| Name                     | Type    | Notes                                       |
|--------------------------|---------|---------------------------------------------|
| selection_id             | string  | matches SelectionDescription.selection_id   |
| sequence_order           | uint32  | matches SelectionDescription.sequence_order |
| vote                     | uint32  |                                             |
| is_placeholder_selection | bool    |                                             |
| extended_data            | string? | optional                                    |

## ciphertext_ballot.proto

### message SubmittedBallot

| Name              | Type                            | Notes                               |
|-------------------|---------------------------------|-------------------------------------|
| ballot_id         | string                          | matches PlaintextBallot.ballot_id   |
| ballot_style_id   | string                          | matches BallotStyle.ballot_style_id |
| manifest_hash     | UInt256                         | matches Manifest.crypto_hash        |
| code_seed         | UInt256                         |                                     |
| code              | UInt256                         |                                     |
| contests          | List\<CiphertextBallotContest\> |                                     |
| timestamp         | int64                           | seconds since the unix epoch UTC    |
| crypto_hash       | UInt256                         |                                     |
| state             | enum BallotState                | CAST, SPOILED                       |

### message CiphertextBallotContest

| Name                    | Type                              | Notes                                     |
|-------------------------|-----------------------------------|-------------------------------------------|
| contest_id              | string                            | matches ContestDescription.contest_id     |
| sequence_order          | uint32                            | matches ContestDescription.sequence_order |
| contest_hash            | UInt256                           | matches ContestDescription.crypto_hash    |                                                                     |
| selections              | List\<CiphertextBallotSelection\> |                                           |
| ciphertext_accumulation | ElGamalCiphertext                 |                                           |
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

### message ConstantChaumPedersenProofElGamalCiphertext

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

| Name                     | Type                             | Notes                                     |
|--------------------------|----------------------------------|-------------------------------------------|
| contest_id               | string                           | matches ContestDescription.contest_id     |
| sequence_order           | uint32                           | matches ContestDescription.sequence_order |
| contest_description_hash | UInt256                          | matches ContestDescription.crypto_hash    |
| selections               | List\<CiphertextTallySelection\> |                                           |

### message CiphertextTallySelection|

| Name                       | Type              | Notes                                       |
|----------------------------|-------------------|---------------------------------------------|
| selection_id               | string            | matches SelectionDescription.selection_id   |
| sequence_order             | uint32            | matches SelectionDescription.sequence_order |
| selection_description_hash | UInt256           | matches SelectionDescription.crypto_hash    |
| ciphertext                 | ElGamalCiphertext |                                             |

## plaintext_tally.proto

### message PlaintextTally

| Name     | Type                          | Notes                                                             |
|----------|-------------------------------|-------------------------------------------------------------------|
| tally_id | string                        | when decrypted spoiled ballots, matches SubmittedBallot.ballot_id |
| contests | List\<PlaintextTallyContest\> |                                                                   |

### message PlaintextTallyContest

| Name       | Type                            | Notes                                  |
|------------|---------------------------------|----------------------------------------|
| contest_id | string                          | matches ContestDescription.contest_id. |
| selections | List\<PlaintextTallySelection\> |                                        |

### message PlaintextTallySelection

| Name         | Type                                  | Notes                                     |
|--------------|---------------------------------------|-------------------------------------------|
| selection_id | string                                | matches SelectionDescription.selection_id |
| tally        | int                                   |                                           |
| value        | ElementModP                           |                                           |
| message      | ElGamalCiphertext                     |                                           |
| shares       | List\<CiphertextDecryptionSelection\> |                                           |

### message CiphertextDecryptionSelection

| Name            | Type                                             | Notes |
|-----------------|--------------------------------------------------|-------|
| selection_id    | string                                           |       |
| guardian_id     | string                                           |       |
| share           | ElementModP                                      |       |
| proof           | GenericChaumPedersenProof                        |       |
| recovered_parts | List\<CiphertextCompensatedDecryptionSelection\> |       |

### message CiphertextCompensatedDecryptionSelection

| Name                | Type                      | Notes |
|---------------------|---------------------------|-------|
| selection_id        | string                    |       |
| guardian_id         | string                    |       |
| missing_guardian_id | string                    |       |
| share               | ElementModP               |       |
| recovery_key        | ElementModP               |       |
| proof               | GenericChaumPedersenProof |       |