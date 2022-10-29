# 🗳 Election Record KMP serialization (proposed specification)

draft 10/29/2022

1. This is version 1.52 of Election Record, corresponding to spec v 1.52
2. All fields must be present unless marked as optional.
3. A missing (optional) String should be internally encoded as null (not empty string), to agree with python hashing.
4. proto_version = 1.52.0 [MAJOR.MINOR.PATCH](https://semver.org/)

## common.proto 
[schema](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/src/commonMain/proto/common.proto)

#### message ContestData

| Name       | Type             | Notes                                    |
|------------|------------------|------------------------------------------|
| status     | enum Status      | normal, null_vote, over_vote, under_vote |
| over_votes | List\<uint32\>   | list of selection sequence_order         |
| write_ins  | List\<string\>   | write in candidates(s)                   |

#### message ElementModQ

| Name  | Type  | Notes                                           |
|-------|-------|-------------------------------------------------|
| value | bytes | unsigned, big-endian, 0 left-padded to 32 bytes |

#### message ElementModP

| Name  | Type   | Notes                                            |
|-------|--------|--------------------------------------------------|
| value | bytes  | unsigned, big-endian, 0 left-padded to 512 bytes |

#### message ElGamalCiphertext

| Name | Type        | Notes   |
|------|-------------|---------|
| pad  | ElementModP | g^R     |
| data | ElementModP | K^(V+R) |

#### message GenericChaumPedersenProof

| Name      | Type           | Notes   |
|-----------|----------------|---------|
| challenge | ElementModQ    |         |
| response  | ElementModQ    |         |

#### message HashedElGamalCiphertext

| Name     | Type        | Notes |
|----------|-------------|-------|
| pad      | ElementModP | C_0   |
| data     | bytes       | C_1   |
| mac      | UInt256     | C_2   |

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
[schema](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/src/commonMain/proto/manifest.proto)

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
| name                | InternationalizedText      | optional                     |
| contact_information | ContactInformation         | optional                     |
| crypto_hash         | UInt256                    | optional                     |

#### message AnnotatedString

| Name       | Type   | Notes |
|------------|--------|-------|
| annotation | string |       |
| value      | string |       |

#### message BallotStyle

| Name                  | Type           | Notes                                   |
|-----------------------|----------------|-----------------------------------------|
| ballot_style_id       | string         |                                         |
| geopolitical_unit_ids | List\<string\> | GeoPoliticalUnit.geopolitical_unit_id   |
| party_ids             | List\<string\> | optional matches Party.party_id         |
| image_uri             | string         | optional                                |

#### message Candidate

| Name         | Type                  | Notes                           |
|--------------|-----------------------|---------------------------------|
| candidate_id | string                |                                 |
| name         | InternationalizedText |                                 |
| party_id     | string                | optional matches Party.party_id |
| image_uri    | string                | optional                        |
| is_write_in  | bool                  |                                 |

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
| contact_information  | ContactInformation     | optional |

#### message InternationalizedText

| Name | Type             | Notes |
|------|------------------|-------|
| text | List\<Language\> |       |

#### message Language

| Name     | Type   | Notes |
|----------|--------|-------|
| value    | string |       |
| language | string |       |

#### message Party

| Name         | Type                  | Notes    |
|--------------|-----------------------|----------|
| party_id     | string                |          |
| name         | InternationalizedText |          |
| abbreviation | string                | optional |
| color        | string                | optional |
| logo_uri     | string                | optional |

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
| ballot_title         | InternationalizedText        | optional                                      |
| ballot_subtitle      | InternationalizedText        | optional                                      |
| primary_party_ids    | List\<string\>               | optional, match Party.party_id                |
| crypto_hash          | UInt256                      | optional                                      |

#### message SelectionDescription

| Name           | Type     | Notes                          |
|----------------|----------|--------------------------------|
| selection_id   | string   |                                |
| sequence_order | uint32   | unique within contest          |
| candidate_id   | string   | matches Candidate.candidate_id |
| crypto_hash    | UInt256  | optional                       |


## election_record.proto
[schema](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/src/commonMain/proto/election_record.proto)

#### message ElectionConfig

| Name                | Type                  | Notes                |
|---------------------|-----------------------|----------------------|
| proto_version       | string                | proto schema version |
| constants           | ElectionConstants     |                      |
| manifest            | Manifest              |                      |
| number_of_guardians | uint32                | n                    |
| quorum              | uint32                | k                    |
| metadata            | map\<string, string\> | arbitrary            |

#### message ElectionConstants

| Name        | Type    | Notes                             |
|-------------|---------|-----------------------------------|
| name        | string  |                                   |
| large_prime | bytes   | bigint is unsigned and big-endian |
| small_prime | bytes   | bigint is unsigned and big-endian |
| cofactor    | bytes   | bigint is unsigned and big-endian |
| generator   | bytes   | bigint is unsigned and big-endian |

#### message ElectionInitialized

| Name                      | Type                  | Notes     |
|---------------------------|-----------------------|-----------|
| config                    | ElectionConfig        |           |
| elgamal_public_key        | ElementModP           | K         |
| manifest_hash             | UInt256               |           |
| crypto_base_hash          | UInt256               | Q         |
| crypto_extended_base_hash | UInt256               | Qbar      |
| guardians                 | List\<Guardian\>      | i = 1..n  |
| metadata                  | map\<string, string\> | arbitrary |

#### message Guardian

| Name                    | Type                 | Notes                                 |
|-------------------------|----------------------|---------------------------------------|
| guardian_id             | string               |                                       |
| x_coordinate            | uint32               | x_coordinate in the polynomial, ℓ = i |
| coefficient_proofs      | List\<SchnorrProof\> | j = 0..k-1                            |

#### message TallyResult

| Name            | Type                  | Notes               |
|-----------------|-----------------------|---------------------|
| election_init   | ElectionInitialized   |                     |
| encrypted_tally | EncryptedTally        |                     |
| ballot_ids      | List\<string\>        | included ballot ids |
| tally_ids       | List\<string\>        | included tally ids  |
| metadata        | map\<string, string\> |                     |

#### message DecryptionResult

| Name                  | Type                        | Notes |
|-----------------------|-----------------------------|-------|
| tally_result          | TallyResult                 |       |
| decrypted_tally       | DecryptedTallyOrBallot      |       |
| lagrange_coefficients | List\<LagrangeCoordinate\>  |       |
| metadata              | map<string, string>         |       |

#### message LagrangeCoordinate

| Name                 | Type        | Notes                             |
|----------------------|-------------|-----------------------------------|
| guardian_id          | string      |                                   |
| x_coordinate         | string      | x_coordinate in the polynomial, ℓ |
| lagrange_coefficient | ElementModQ | w_ℓ, see 10A                      |


## plaintext_ballot.proto
[schema](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/src/commonMain/proto/plaintext_ballot.proto)

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

| Name                     | Type   | Notes                               |
|--------------------------|--------|-------------------------------------|
| selection_id             | string | SelectionDescription.selection_id   |
| sequence_order           | uint32 | SelectionDescription.sequence_order |
| vote                     | uint32 |                                     |


## encrypted_ballot.proto
[schema](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/src/commonMain/proto/encrypted_ballot.proto)

#### message EncryptedBallot

| Name              | Type                             | Notes                            |
|-------------------|----------------------------------|----------------------------------|
| ballot_id         | string                           | PlaintextBallot.ballot_id        |
| ballot_style_id   | string                           | BallotStyle.ballot_style_id      |
| manifest_hash     | UInt256                          | Manifest.crypto_hash             |
| code_seed         | UInt256                          | optional?                        |
| code              | UInt256                          | tracking code, H_i               |
| contests          | List\<EncryptedBallotContest\>   |                                  |
| timestamp         | int64                            | seconds since the unix epoch UTC |
| crypto_hash       | UInt256                          |                                  |
| state             | enum BallotState                 | CAST, SPOILED                    |

#### message EncryptedBallotContest

| Name                   | Type                             | Notes                             |
|------------------------|----------------------------------|-----------------------------------|
| contest_id             | string                           | ContestDescription.contest_id     |
| sequence_order         | uint32                           | ContestDescription.sequence_order |
| contest_hash           | UInt256                          | ContestDescription.crypto_hash    |                                                                     |
| selections             | List\<EncryptedBallotSelection\> |                                   |
| crypto_hash            | UInt256                          |                                   |
| proof                  | RangeProof                       | proof of votes <= limit           |
| encrypted_contest_data | HashedElGamalCiphertext          |                                   |

#### message EncryptedBallotSelection

| Name                     | Type              | Notes                               |
|--------------------------|-------------------|-------------------------------------|
| selection_id             | string            | SelectionDescription.selection_id   |
| sequence_order           | uint32            | SelectionDescription.sequence_order |
| selection_hash           | UInt256           | SelectionDescription.crypto_hash    |
| ciphertext               | ElGamalCiphertext |                                     |
| crypto_hash              | UInt256           |                                     |
| proof                    | RangeProof        | proof vote = 0 or 1                 |

#### message RangeProof

| Name      | Type                              | Notes |
|-----------|-----------------------------------|-------|
| proof     | List\<GenericChaumPedersenProof\> |       |


## encrypted_tally.proto
[schema](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/src/commonMain/proto/encrypted_tally.proto)

#### message EncryptedTally

| Name     | Type                            | Notes |
|----------|---------------------------------|-------|
| tally_id | string                          |       |
| contests | List\<EncryptedTallyContest\>   |       | 

#### message EncryptedTallyContest

| Name                     | Type                             | Notes                             |
|--------------------------|----------------------------------|-----------------------------------|
| contest_id               | string                           | ContestDescription.contest_id     |
| sequence_order           | uint32                           | ContestDescription.sequence_order |
| contest_description_hash | UInt256                          | ContestDescription.crypto_hash    |
| selections               | List\<EncryptedTallySelection\>  |                                   |

#### message EncryptedTallySelection

| Name                       | Type              | Notes                                                 |
|----------------------------|-------------------|-------------------------------------------------------|
| selection_id               | string            | SelectionDescription.selection_id                     |
| sequence_order             | uint32            | SelectionDescription.sequence_order                   |
| selection_description_hash | UInt256           | SelectionDescription.crypto_hash                      |
| ciphertext                 | ElGamalCiphertext | accumulation over all cast ballots for this selection |


## decrypted_tally.proto
[schema](https://github.com/danwallach/electionguard-kotlin-multiplatform/blob/main/src/commonMain/proto/decrypted_tally.proto)

### message DecryptedTallyOrBallot

| Name     | Type                     | Notes                |
|----------|--------------------------|----------------------|
| id       | string                   | tallyId or ballotId  |
| contests | List\<DecryptedContest\> |                      |

#### message DecryptedContest

| Name                   | Type                         | Notes                            |
|------------------------|------------------------------|----------------------------------|
| contest_id             | string                       | ContestDescription.contest_id    |
| selections             | List\<DecryptedSelection\>   |                                  |
| decrypted_contest_data | DecryptedContestData         | optional, ballot decryption only |

#### message DecryptedSelection

| Name         | Type                        | Notes                             |
|--------------|-----------------------------|-----------------------------------|
| selection_id | string                      | SelectionDescription.selection_id |
| tally        | int                         | decrypted vote count              |
| value        | ElementModP                 | g^tally or M in the spec          |
| message      | ElGamalCiphertext           | encrypted vote count              |
| proof        | GenericChaumPedersenProof   |                                   |

#### message DecryptedContestData

| Name                   | Type                      | Notes                                     |
|------------------------|---------------------------|-------------------------------------------|
| contest_data           | ContestData               |                                           |
| encrypted_contest_data | HashedElGamalCiphertext   | see 3.3.3. matches EncryptedBallotContest |
| proof                  | GenericChaumPedersenProof |                                           |
| beta                   | ElementModP               |                                           |

