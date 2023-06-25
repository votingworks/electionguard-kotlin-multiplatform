# 🗳 Election Record serialization for private classes

draft 6/14/2023

## trustees.proto

#### message DecryptingTrustee

Information passed from the KeyCeremonyTrustee to the DecryptingTrustee.
Only the secret_key is actually private. One could store that separately and securely, and add it in when decrypting.

| Name                  | Type        | Notes                                  |
|-----------------------|-------------|----------------------------------------|
| guardian_id           | string      |                                        |
| guardian_x_coordinate | uint32      | > 0                                    |
| public_key            | ElementModP |                                        |
| key_share             | ElementModQ | share of the election key, P(i), eq 66 |

#### message EncryptedKeyShare

| Name                 | Type                    | Notes                                       |
|----------------------|-------------------------|---------------------------------------------|
| owner_xcoord         | uint256                 | guardian i (owns the polynomial Pi) x coord |
| polynomial_owner     | string                  | guardian i (owns the polynomial Pi) name    |
| secret_share_for     | string                  | The Id of the guardian to receive this (ℓ)  |
| encrypted_coordinate | HashedElGamalCiphertext | El (Pj(ℓ)) eq 17                            |

#### message KeyShare

| Name             | Type                    | Notes                                      |
|------------------|-------------------------|--------------------------------------------|
| polynomial_owner | string                  | guardian j (owns the polynomial Pj)        |
| secret_share_for | string                  | The Id of the guardian to receive this (ℓ) |
| coordinate       | HashedElGamalCiphertext | Pj(ℓ)                                      |

