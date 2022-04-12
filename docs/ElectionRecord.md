# 🗳 Election Record protobuf directory and file layout

draft 4/12/2022 for proto_version = 2.0.0

## Public Election Record files

````
topdir/election_record
    electionRecord.protobuf
    spoiledBallotsTally.protobuf
    submittedBallots.protobuf
````    

There is a single ElectionRecord message stored in __electionRecord.protobuf__. Its size depends on the 
size of the Manifest, but it is otherwise fixed length. Note that some of the fields are optional, as they
are added at different stages of the workflow. The fields from the earlier stages are copied to the election
record of later stages.

| Name                | Type                      | Workflow stage   |
|---------------------|---------------------------|------------------|
| proto_version       | string                    | always present   |
| constants           | ElectionConstants         | key ceremony     |
| manifest            | Manifest                  | key ceremony     |
| context             | ElectionContext           | key ceremony     |
| guardian_records    | List\<GuardianRecord\>    | key ceremony     |
| devices             | List\<EncryptionDevice\>  | key ceremony ??  |
| ciphertext_tally    | CiphertextTally           | accumulate tally |
| plaintext_tally     | PlaintextTally            | decrypt tally    |
| available_guardians | List\<AvailableGuardian\> | decrypt tally    |


The file __submittedBallots.protobuf__ contains multiple SubmittedBallot messages, written as varint length-delimited messages.
There is one message for each SubmittedBallot, CAST or SPOILED.

The file __spoiledBallotsTally.protobuf__ contains multiple PlaintextTally messages, written as varint length-delimited messages.
There is one message for each SPOILED SubmittedBallot.

We will add more structure in the future to deal with large numbers of submitted ballots.

See writeDelimitedTo() and parseDelimitedFrom() methods for varint length-delimited messages.

## Private files

These files are not part of the election record, but are generated for internal use.

### KeyCeremony

Each trustee maintains their own private copy of their crypto data. They must keep this data secret, to ensure the
security of the election.

````
election_private_data
    trusteeName.protobuf
````    
The protobuf is in trustees.proto. It is generated during the key ceremony, seperately on behalf of each trustee.

### Encryption

During the encryption stage, input plaintext ballots may be checked for consistency against the manifest. 
The ones that fail are not encrypted, but are placed in a private directory for examination by election officials.

````
election_private_data
    invalid_ballots
        invalidBallots.protobuf
````    

The file __invalidBallots.protobuf__ contains multiple PlaintextBallot messages, written as varint length-delimited messages.
