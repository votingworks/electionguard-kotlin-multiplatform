# 🗳 Election Record JSON directory and file layout

draft 7/15/2023

## Public Election Record files

````
topdir/
    constants.json
    election_config.json
    election_initialized.json
    encrypted_tally.json
    manifest.json
    tally.json
    encrypted_ballots/
       deviceName1/
          ballot_chain.json
          eballot-ballotId1.json
          eballot-ballotId2.json
          eballot-ballotId3.json
          ...
        deviceName2/
          eballot-ballotId1.json
          eballot-ballotId2.json
          eballot-ballotId3.json
        deviceName3/
        ...
    challenged_ballots/
       dballot-<ballotId>.json
       dballot-<ballotId>.json
       dballot-<ballotId>.json
       ...
````    

One or more of the above files may be present, depending on the output stage.


| Name                      | Type                    | Workflow stage      |
|---------------------------|-------------------------|---------------------|
| constants.json            | ElectionConstantsJson   | start               |
| manifest.json             | ManifestJson            | start               |
| election_config.json      | ElectionConfigJson      | key ceremony input  |
| election_initialized.json | ElectionInitializedJson | key ceremony output |
| eballot-\<ballotId>.json  | EncryptedBallotJson     | encryption output   |
| encrypted_tally.json      | EncryptedTallyJson      | tally output        |
| tally.json                | DecryptedTallyJson      | decryption output   |
| dballot-\<ballotId>.json  | DecryptedBallotJson     | decryption output   |

* The encrypted_ballots directory contain all ballots, cast or challenged.
* The challenged_ballots directory contain only challenged ballots.
* DecryptedTallyJson and DecryptedBallotJson use the same schema (DecryptedTallyOrBallotJson)

## Private files
encrypt

These files are not part of the election record, but are generated for internal use.
In production, these must be stored in a secure place.

### KeyCeremony

Each trustee maintains their own private copy of their crypto data. They must keep this data secret, to ensure the
security of the election.

````
private_data/trustees/
    decryptingTrustee-{trusteeName}.json
````    

### Encryption

During the encryption stage, input plaintext ballots are checked for consistency against the manifest. 
The ones that fail are not encrypted, and are placed in a private directory for examination by election officials.

````
private_data/input/
       pballot-<ballotId>.json
       pballot-<ballotId>.json
       ...
````    
