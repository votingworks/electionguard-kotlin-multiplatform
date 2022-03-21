# 🗳 Election Record serialization (proposed specification)

draft 3/21/2022 for proto_version = 2.0.0 (MAJOR.MINOR.PATCH)

This covers only the election record, and not any serialized classes used in remote procedure calls.

Notes

1. All fields must be present unless marked as optional.
2. Proto_version uses [semantic versioning](https://semver.org/)

## common.proto

### class GenericChaumPedersenProof

| Name      | Type           | Notes   |
|-----------|----------------|---------|
| challenge | ElementModQ    |         |
| response  | ElementModQ    |         |

### class ElementModQ, ElementModP

| Name  | JSON Name | Type  | Notes                                           |
|-------|-----------|-------|-------------------------------------------------|
| value | data      | bytes | bigint is variable length, unsigned, big-endian |

### class ElGamalCiphertext

| Name | JSON Name | Type        | Notes |
|------|-----------|-------------|-------|
| pad  |           | ElementModP |       |
| data |           | ElementModP |       |

### class SchnorrProof

| Name       | Type        | Notes   |
|------------|-------------|---------|
| public_key | ElementModP |         |
| challenge  | ElementModQ |         |
| response   | ElementModQ |         |

### class UInt256

| Name  | Type | Notes                                      |
|-------|------|--------------------------------------------|
| value | bytes | bigint is 32 bytes, unsigned, big-endian  |


## Election

### class election_record.proto

There is no python SDK version of this class.

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

### class AvailableGuardian

| Name                | Type        | Notes                          |
|---------------------|-------------|--------------------------------|
| guardian_id         | string      |                                |
| x_coordinate        | string      | x_coordinate in the polynomial |
| lagrange_coordinate | ElementModQ |                                |

### class ElectionConstants

| Name        | JSON Name | Type   | Notes                             |
|-------------|-----------|--------|-----------------------------------|
| name        |           | string | not present in JSON               |
| large_prime |           | bytes  | bigint is unsigned and big-endian |
| small_prime |           | bytes  | bigint is unsigned and big-endian |
| cofactor    |           | bytes  | bigint is unsigned and big-endian |
| generator   |           | bytes  | bigint is unsigned and big-endian |

### class ElectionContext

| Name                      | JSON Name | Type                  | Notes    |
|---------------------------|-----------|-----------------------|----------|
| number_of_guardians       |           | uint32                |          |
| quorum                    |           | uint32                |          |
| elgamal_public_key        |           | ElementModP           |          |
| commitment_hash           |           | UInt256               |          |
| manifest_hash             |           | UInt256               |          |
| crypto_base_hash          |           | UInt256               |          |
| crypto_extended_base_hash |           | UInt256               |          |
| extended_data             |           | map\<string, string\> | optional |

### class EncryptionDevice

| Name        | JSON Name | Type   | Notes                                  |
|-------------|-----------|--------|----------------------------------------|
| device_id   |           | int64  | was uuid LOOK maybe just use a string? |
| session_id  |           | int64  |                                        |
| launch_code |           | int64  |                                        |
| location    |           | string |                                        |

### class GuardianRecord

| Name                    | JSON Name            | Type                 | Notes                          |
|-------------------------|----------------------|----------------------|--------------------------------|
| guardian_id             |                      | string               |                                |
| x_coordinate            | sequence_order       | uint32               | x_coordinate in the polynomial |
| guardian_public_key     | election_public_key  | ElementModP          |                                |
| coefficient_commitments | election_commitments | List\<ElementModP\>  |                                |
| coefficient_proofs      | election_proofs      | List\<SchnorrProof\> |                                |

## manifest.proto

### class Manifest

| Name                | JSON Name | Type                       | Notes                           |
|---------------------|-----------|----------------------------|---------------------------------|
| election_scope_id   |           | string                     |                                 |
| spec_version        |           | string                     | Probably the python SDK version |
| election_type       | type      | enum ElectionType          |                                 |
| start_date          |           | string                     | ISO 8601 formatted date/time    |
| end_date            |           | string                     | ISO 8601 formatted date/time    |
| geopolitical_units  |           | List\<GeopoliticalUnit\>   |                                 |
| parties             |           | List\<Party\>              |                                 |
| candidates          |           | List\<Candidate\>          |                                 |
| contests            |           | List\<ContestDescription\> |                                 |
| ballot_styles       |           | List\<BallotStyle\>        |                                 |
| name                |           | InternationalizedText      | optional                        |
| contact_information |           | ContactInformation         | optional                        |
| crypto_hash         |           | UInt256                    | optional                        |

### class AnnotatedString

| Name       | JSON Name | Type   | Notes |
|------------|-----------|--------|-------|
| annotation |           | string |       |
| value      |           | string |       |

### class BallotStyle

| Name                  | JSON Name | Type           | Notes                                         |
|-----------------------|-----------|----------------|-----------------------------------------------|
| ballot_style_id       | object_id | string         |                                               |
| geopolitical_unit_ids |           | List\<string\> | matches GeoPoliticalUnit.geopolitical_unit_id |
| party_ids             |           | List\<string\> | optional matches Party.party_id               |
| image_uri             |           | string         | optional                                      |

### class Candidate

| Name         | JSON Name | Type                  | Notes                           |
|--------------|-----------|-----------------------|---------------------------------|
| candidate_id | object_id | string                |                                 |
| name         |           | InternationalizedText |                                 |
| party_id     |           | string                | optional matches Party.party_id |
| image_uri    |           | string                | optional                        |
| is_write_in  |           | bool                  |                                 |

### class ContactInformation

| Name         | JSON Name | Type           | Notes    |
|--------------|-----------|----------------|----------|
| name         |           | string         | optional |
| address_line |           | List\<string\> | optional |
| email        |           | List\<string\> | optional |
| phone        |           | List\<string\> | optional |

### class GeopoliticalUnit

| Name                 | JSON Name | Type                   | Notes    |
|----------------------|-----------|------------------------|----------|
| geopolitical_unit_id | object_id | string                 |          |
| name                 |           | string                 |          |
| type                 |           | enum ReportingUnitType |          |
| contact_information  |           | ContactInformation     | optional |

### class InternationalizedText

| Name | JSON Name | Type             | Notes |
|------|-----------|------------------|-------|
| text |           | List\<Language\> |       |

### class Language

| Name     | JSON Name | Type   | Notes |
|----------|-----------|--------|-------|
| value    |           | string |       |
| language |           | string |       |

### class Party

| Name         | JSON Name | Type                  | Notes    |
|--------------|-----------|-----------------------|----------|
| party_id     | object_id | string                |          |
| name         |           | InternationalizedText |          |
| abbreviation |           | string                | optional |
| color        |           | string                | optional |
| logo_uri     |           | string                | optional |

### class ContestDescription

| Name                 | JSON Name             | Type                         | Notes                                         |
|----------------------|-----------------------|------------------------------|-----------------------------------------------|
| contest_id           | object_id             | string                       |                                               |
| sequence_order       |                       | uint32                       | deterministic sorting                         |
| geopolitical_unit_id | electoral_district_id | string                       | matches GeoPoliticalUnit.geopolitical_unit_id |
| vote_variation       |                       | enum VoteVariationType       |                                               |
| number_elected       |                       | uint32                       |                                               |
| votes_allowed        |                       | uint32                       |                                               |
| name                 |                       | string                       |                                               |
| selections           | ballot_selections     | List\<SelectionDescription\> |                                               |
| ballot_title         |                       | InternationalizedText        | optional                                      |
| ballot_subtitle      |                       | InternationalizedText        | optional                                      |
| primary_party_ids    |                       | List\<string\>               | optional, match Party.party_id                |
| crypto_hash          |                       | UInt256                      | optional                                      |

### class SelectionDescription

| Name           | JSON Name | Type   | Notes                          |
|----------------|-----------|--------|--------------------------------|
| selection_id   | object_id | string |                                |
| sequence_order |           | uint32 | deterministic sorting          |
| candidate_id   |           | string | matches Candidate.candidate_id |
| crypto_hash          |                       | UInt256                      | optional                                      |

## plaintext_tally.proto

### class PlaintextTally

| Name     | JSON Name | Type                          | Notes                                                             |
|----------|-----------|-------------------------------|-------------------------------------------------------------------|
| tally_id | object_id | string                        | when decrypted spoiled ballots, matches SubmittedBallot.ballot_id |
| contests |           | List\<PlaintextTallyContest\> |                                                                   |

### class PlaintextTallyContest

| Name       | JSON Name | Type                            | Notes                                  |
|------------|-----------|---------------------------------|----------------------------------------|
| contest_id | object_id | string                          | matches ContestDescription.contest_id. |
| selections |           | List\<PlaintextTallySelection\> |                                        |

### class PlaintextTallySelection

| Name         | JSON Name | Type                                  | Notes                                     |
|--------------|-----------|---------------------------------------|-------------------------------------------|
| selection_id | object_id | string                                | matches SelectionDescription.selection_id |
| tally        |           | int                                   |                                           |
| value        |           | ElementModP                           |                                           |
| message      |           | ElGamalCiphertext                     |                                           |
| shares       |           | List\<CiphertextDecryptionSelection\> |                                           |

### class CiphertextDecryptionSelection

| Name            | JSON Name | Type                                             | Notes                          |
|-----------------|-----------|--------------------------------------------------|--------------------------------|
| selection_id    | object_id | string                                           | get_tally_shares_for_selection |
| guardian_id     |           | string                                           |                                |
| share           |           | ElementModP                                      |                                |
| proof           |           | ChaumPedersenProof                               |                                |
| recovered_parts |           | List\<CiphertextCompensatedDecryptionSelection\> |                                |

### class CiphertextCompensatedDecryptionSelection(ElectionObjectBase)

| Name                | JSON Name | Type               | Notes    |
|---------------------|-----------|--------------------|----------|
| selection_id        | object_id | string             | unneeded |
| guardian_id         |           | string             |          |
| missing_guardian_id |           | string             |          |
| share               |           | ElementModP        |          |
| recovery_key        |           | ElementModP        |          |
| proof               |           | ChaumPedersenProof |          |

## ciphertext_tally.proto

### class CiphertextTally

| Name     | JSON Name | Type                           | Notes                                                             |
|----------|-----------|--------------------------------|-------------------------------------------------------------------|
| tally_id | object_id | string                         | when decrypted spoiled ballots, matches SubmittedBallot.ballot_id |
| contests |           | List\<CiphertextTallyContest\> |                                                                   | 

### class CiphertextTallyContest

| Name                     | JSON Name        | Type                             | Notes                                     |
|--------------------------|------------------|----------------------------------|-------------------------------------------|
| contest_id               | object_id        | string                           | matches ContestDescription.contest_id     |
| sequence_order           |                  | uint32                           | matches ContestDescription.sequence_order |
| contest_description_hash | description_hash | UInt256                          | matches ContestDescription.crypto_hash    |
| selections               |                  | List\<CiphertextTallySelection\> |                                           |

### class CiphertextTallySelection|

| Name                       | JSON Name        | Type              | Notes                                       |
|----------------------------|------------------|-------------------|---------------------------------------------|
| selection_id               | object_id        | string            | matches SelectionDescription.selection_id   |
| sequence_order             |                  | uint32            | matches SelectionDescription.sequence_order |
| selection_description_hash | description_hash | UInt256           | matches SelectionDescription.crypto_hash    |
| ciphertext                 |                  | ElGamalCiphertext |                                             |

## plaintext_ballot.proto

### class PlaintextBallot

| Name            | JSON Name | Type                           | Notes                               |
|-----------------|-----------|--------------------------------|-------------------------------------|
| ballot_id       | object_id | string                         | unique input ballot id              |
| ballot_style_id | style_id  | string                         | matches BallotStyle.ballot_style_id |
| contests        |           | List\<PlaintextBallotContest\> |                                     |

### class PlaintextBallotContest

| Name           | JSON Name | Type                             | Notes                                     |
|----------------|-----------|----------------------------------|-------------------------------------------|
| contest_id     | object_id | string                           | matches ContestDescription.contest_id     |
| sequence_order |           | uint32                           | matches ContestDescription.sequence_order |
| selections     |           | List\<PlaintextBallotSelection\> |                                           |

### class PlaintextBallotSelection

| Name                     | JSON Name | Type         | Notes                                       |
|--------------------------|-----------|--------------|---------------------------------------------|
| selection_id             | object_id | string       | matches SelectionDescription.selection_id   |
| sequence_order           |           | uint32       | matches SelectionDescription.sequence_order |
| vote                     |           | uint32       |                                             |
| is_placeholder_selection |           | bool         |                                             |
| extended_data            |           | ExtendedData | optional                                    |

### class ExtendedData

| Name   | JSON Name | Type   | Notes |
|--------|-----------|--------|-------|
| value  |           | string |       |
| length |           | uint32 | why?  | 

## ciphertext_ballot.proto

### class SubmittedBallot

| Name              | JSON Name | Type                            | Notes                               |
|-------------------|-----------|---------------------------------|-------------------------------------|
| ballot_id         | object_id | string                          | matches PlaintextBallot.ballot_id   |
| ballot_style_id   | style_id  | string                          | matches BallotStyle.ballot_style_id |
| manifest_hash     |           | UInt256                         | matches Manifest.crypto_hash        |
| code_seed         |           | UInt256                         |                                     |
| code              |           | UInt256                         |                                     |
| contests          |           | List\<CiphertextBallotContest\> |                                     |
| timestamp         |           | int64                           | seconds since the unix epoch UTC    |
| crypto_hash       |           | UInt256                         |                                     |
| state             |           | enum BallotState                | CAST, SPOILED                       |

### class CiphertextBallotContest

| Name                    | JSON Name         | Type                              | Notes                                     |
|-------------------------|-------------------|-----------------------------------|-------------------------------------------|
| contest_id              | object_id         | string                            | matches ContestDescription.contest_id     |
| sequence_order          |                   | uint32                            | matches ContestDescription.sequence_order |
| contest_hash            | description_hash  | UInt256                           | matches ContestDescription.crypto_hash    |                                                                     |
| selections              | ballot_selections | List\<CiphertextBallotSelection\> |                                           |
| ciphertext_accumulation |                   | ElGamalCiphertext                 |                                           |
| crypto_hash             |                   | UInt256                           |                                           |
| proof                   |                   | ConstantChaumPedersenProof        |                                           |

### class CiphertextBallotSelection

| Name                     | JSON Name        | Type                          | Notes                                       |
|--------------------------|------------------|-------------------------------|---------------------------------------------|
| selection_id             | object_id        | string                        | matches SelectionDescription.selection_id   |
| sequence_order           |                  | uint32                        | matches SelectionDescription.sequence_order |
| selection_hash           | description_hash | UInt256                       | matches SelectionDescription.crypto_hash    |
| ciphertext               |                  | ElGamalCiphertext             |                                             |
| crypto_hash              |                  | UInt256                       |                                             |
| is_placeholder_selection |                  | bool                          |                                             |
| proof                    |                  | DisjunctiveChaumPedersenProof |                                             |
| extended_data            |                  | ElGamalCiphertext             | optional                                    |

### class ConstantChaumPedersenProof

| Name      | Type                      | Notes |
|-----------|---------------------------|-------|
| constant  | uint32                    |       |
| proof     | GenericChaumPedersenProof |       |

### class DisjunctiveChaumPedersenProof

| Name      | Type                      | Notes |
|-----------|---------------------------|-------|
| challenge | ElementModQ               |       |
| proof0    | GenericChaumPedersenProof |       |
| proof1    | GenericChaumPedersenProof |       |
