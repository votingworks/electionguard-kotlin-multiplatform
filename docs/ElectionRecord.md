# 🗳 Election Record protobuf directory and file layout

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

he file __spoiledBallotsTally.protobuf__ contains multiple PlaintextTally messages, written as varint length-delimited messages.
There is one message for each SPOILED SubmittedBallot.

We will add more structure in the future to deal with large numbers of submitted ballots.

See writeDelimitedTo() and parseDelimitedFrom() methods for varint length-delimited messages. 
Currently available in the google's java protobuf library, but not in kotlin pbandk or google's kotlin library(?).


