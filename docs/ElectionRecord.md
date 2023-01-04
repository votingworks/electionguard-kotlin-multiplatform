# 🗳 Election Record protobuf directory and file layout

draft 1/4/2023 for proto_version = 2.0.1

## Public Election Record files

````
topdir/
    electionConfig.protobuf
    electionInitialized.protobuf
    encryptedBallots.protobuf
    tallyResult.protobuf
    decryptionResult.protobuf
    spoiledBallotTallies.protobuf
````    

One or more of the above files may be present, depending on the output stage.


| Name                          | Type                    | Workflow stage      |
|-------------------------------|-------------------------|---------------------|
| electionConfig.protobuf       | ElectionConfig          | start               |
| electionInitialized.protobuf  | ElectionInitialized     | key ceremony output |
| encryptedBallots.protobuf     | EncryptedBallot*        | encryption output   |
| tallyResult.protobuf          | TallyResult             | tally output        |
| decryptionResult.protobuf     | DecryptionResult        | decryption output   |
| spoiledBallotTallies.protobuf | DecryptedTallyOrBallot* | decryption output   |

(*) The files encryptedBallots.protobuf and spoiledBallotTallies.protobuf contain multiple length-delimited proto messages. 
This allows to read messages one at a time, rather than all into memory at once.
See writeDelimitedTo() and parseDelimitedFrom() methods for varint length-delimited messages.

## Private files

These files are not part of the election record, but are generated for internal use.
In production, these must be stored in a secure place.

### KeyCeremony

Each trustee maintains their own private copy of their crypto data. They must keep this data secret, to ensure the
security of the election.

````
private_data/trustees/
    decryptingTrustee-{trusteeName}.protobuf
````    

### Encryption

During the encryption stage, input plaintext ballots are checked for consistency against the manifest. 
The ones that fail are not encrypted, and are placed in a private directory for examination by election officials.

````
private_data/input/
    plaintextBallots.protobuf
````    

The file __plaintextBallots.protobuf__ contains multiple PlaintextBallot messages, written with length-delimited messages.
