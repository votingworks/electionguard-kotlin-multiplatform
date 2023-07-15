# 🗳 Election Record protobuf directory and file layout

draft 7/15/2023

## Public Election Record files

````
topdir/
    electionConfig.protobuf
    electionInitialized.protobuf
    tallyResult.protobuf
    decryptionResult.protobuf
    challengedBallots.protobuf
    encrypted_ballots/
       deviceName1/
          ballotChain.protobuf
          eballot-ballotId1.protobuf
          eballot-ballotId2.protobuf
          eballot-ballotId3.protobuf
          ...
        deviceName2/
          ballotChain.protobuf
          eballot-ballotId1.protobuf
          eballot-ballotId2.protobuf
          eballot-ballotId3.protobuf
          ...   
        deviceName3/
        ...
        batchName1/
           encryptedBallots.protobuf
        batchName2/
           encryptedBallots.protobuf         
        batchName3/
        ...
          
````    

One or more of the above files may be present, depending on the output stage. Each output stage is self-contained, 
eg ElectionInitialized contains ElectionConfig, TallyResult contains ElectionInitialized, etc. This is different than 
the JSON election record.

| Name                         | Type                    | Workflow stage       |
|------------------------------|-------------------------|----------------------|
| electionConfig.protobuf      | ElectionConfig          | start                |
| electionInitialized.protobuf | ElectionInitialized     | key ceremony output  |
| ballotChain.protobuf         | EncryptedBallotChain    | ballot chaining info |
| eballot-\<ballotId>.protobuf | EncryptedBallot         | encrypted ballot     |
| encryptedBallots.protobuf    | EncryptedBallot*        | encrypted ballots    |
| tallyResult.protobuf         | TallyResult             | tally output         |
| decryptionResult.protobuf    | DecryptionResult        | decryption output    |
| challengedBallots.protobuf   | DecryptedTallyOrBallot* | decrypted ballots    |

(*) The files encryptedBallots.protobuf and spoiledBallotTallies.protobuf contain multiple length-delimited proto messages. 
This allows to read messages one at a time, rather than all into memory at once.
See writeDelimitedTo() and parseDelimitedFrom() methods for varint length-delimited messages.

## Private files

These files are not part of the election record, but are generated for internal use.
In production, these must be stored in a secure place.

| Name                                     | Type             | Workflow stage      |
|------------------------------------------|------------------|---------------------|
| decryptingTrustee-{trusteeName}.protobuf | Trustee          | key ceremony output |
| plaintextBallots.protobuf                | PlaintextBallot* | encryption output   |

(*) plaintextBallots.protobuf contains multiple length-delimited proto messages.

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
