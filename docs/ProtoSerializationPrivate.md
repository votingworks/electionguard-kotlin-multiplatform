# 🗳 Election Record serialization for private classes

draft 6/11/2022 for proto_version = 2.0.0 (MAJOR.MINOR.PATCH)

Notes

1. All fields must be present unless marked as optional.
2. A missing (optional) String should be internally encoded as null (not empty string), to agree with python hashing.
3. Proto_version uses [semantic versioning](https://semver.org/)

## trustees.proto

#### message DecryptingTrustee

Information passed from the KeyCeremonyTrustee to the DecryptingTrustee.
Only the secret_key is actually private. One could store that separately and securely, and add it in when decrypting.

| Name                    | Type                   | Notes   |
|-------------------------|------------------------|---------|
| guardian_id             | string                 |         |
| guardian_x_coordinate   | uint32                 |         |
| election_keypair        | ElGamalKeypair         | secret  |
| secret_key_shares       | List\<SecretKeyShare\> |         |
| coefficient_commitments | List\<CommitmentSet\>  | K_ij    |

#### message ElGamalKeypair

| Name              | Type          | Notes  |
|-------------------|---------------|--------|
| secret_key        | ElementModQ   | secret |
| public_key        | ElementModP   |        |

#### message CommitmentSet

| Name         | Type                 | Notes |
|--------------|----------------------|-------|
| guardian_id  | string               |       |
| commitments  | List\<ElementModP\>  | K_ij  |

#### message SecretKeyShare

| Name                             | Type                    | Notes      |
|----------------------------------|-------------------------|------------|
| generating_guardian_id           | string                  | i          |
| designated_guardian_id           | string                  | l          |
| designated_guardian_x_coordinate | uint32                  | ℓ          |
| encrypted_coordinate             | HashedElGamalCiphertext | El (Pi(ℓ)) |
