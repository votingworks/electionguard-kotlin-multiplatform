# Workflow and Command Line Programs

last update 8/09/2022

## Election workflow

1. Generate an ElectionConfig protobuf record. The following examples may be useful:
   1. A synthetic manifest is created in **electionguard.publish.PublisherTest.testWriteElectionConfig**(), 
      and an ElectionConfig protobuf is written out. 
   2. In the [electionguard-java](https://github.com/JohnLCaron/electionguard-java) repo, 
      **com.sunya.electionguard.input.MakeManifestForTesting** can read a version 1
      JSON manifest and write out a version 2 ElectionConfig protobuf.

2. KeyCeremony. An ElectionConfig record is needed as input, and an ElectionInitialized record is output. The following examples may be useful:
   1. **electionguard.keyceremony.RunTrustedKeyCeremony** is a CLI for testing, that will run locally in a single process, 
      generate test guardians, and run the key ceremony. 
   2. In the [electionguard-remote](https://github.com/JohnLCaron/electionguard-remote) repo,
      **electionguard.workflow.RunRemoteKeyCeremonyTest** is a CLI for testing, that will run a key ceremony
      with remote guardians over gRPC, on the same machine but in different processes. Private keys for each
      guardian are kept private and stored separately.
   3. In the [electionguard-remote](https://github.com/JohnLCaron/electionguard-remote) repo,
      **electionguard.keyceremony.RunRemoteKeyCeremony** and **electionguard.keyceremony.RunRemoteTrustee**
      are CLI programs with which a remote key ceremony can be run with guardians residing completely on separate machines, 
      communicating over gRPC from anywhere on the internet.

3. Create input plaintext ballots based on the manifest in ElectionConfig. The following examples may be useful:
    1. RunWorkflow uses RandomBallotProvider to generate random test ballots.

4. Batch Encryption. The following examples may be useful:
    1. **electionguard.encrypt.RunBatchEncryption** is a CLI that reads an ElectionInitialized record and input plaintext ballots, encrypts the
       ballots and writes out EncryptedBallot protobuf records. If any input plaintext ballot fails validation,
       it is annotated and written to a separate directory, and not encrypted.

5. Accumulate Tally. The following examples may be useful:
    1. **electionguard.tally.RunAccumulateTally** is a CLI that reads an ElectionInitialized record and EncryptedBallot records, sums the
       votes in the encrypted ballots and writes out a **EncryptedTally** protobuf record.

6. Decryption. The following examples may be useful:
    1. **electionguard.decrypt.RunTrustedTallyDecryption** is a CLI for testing, that will run locally in a single process, 
       that reads an EncryptedTally record and local 
       DecryptingTrustee records, decrypts the tally and writes out a **PlaintextTally** protobuf record.
    2. **electionguard.decrypt.RunTrustedBallotDecryption** is a CLI for testing, that will run locally in a single process,
      that reads a spoiled ballot record and local DecryptingTrustee records, decrypts the ballot and writes out a 
      **PlaintextTally** protobuf record that represents the decrypted spoiled ballot.
    3. In the [electionguard-remote](https://github.com/JohnLCaron/electionguard-remote) repo,
       **electionguard.workflow.RunRemoteDecryptionTest** is a CLI for testing, that will decrypt the EncryptedTally 
       (and optionally spoiled ballots)
       with remote guardians over gRPC, on the same machine but in different processes. Private keys for each
       guardian are kept private and stored separately.
    4. In the [electionguard-remote](https://github.com/JohnLCaron/electionguard-remote) repo,
       **electionguard.decrypt.RunRemoteDecryptor** and **electionguard.decrypt.RunRemoteDecryptingTrustee**
       are CLI programs with which one can decrypt with guardians residing completely on separate machines,
       communicating over gRPC from anywhere on the internet.

7. Run Verifier. The following examples may be useful:
    1. **electionguard.verify.VerifyElectionRecord** is a CLI that reads an election record and verifies it.

8. Complete test Workflow. The following examples may be useful:
   1. A complete test workflow can be run from electionguard.workflow.RunWorkflow in the commonTest module.
   2. A complete test remote workflow can be run from electionguard.workflow.RunRemoteWorkflowTest in the 
      [electionguard-remote](https://github.com/JohnLCaron/electionguard-remote) repo.


## Run Trusted KeyCeremony

This has access to all the trustees, so is only used for testing, or in a use case of trust.

````
Usage: RunTrustedKeyCeremony options_list
Options: 
    --inputDir, -in -> Directory containing input ElectionConfig record (always required) { String }
    --trusteeDir, -trustees -> Directory to write private trustees (always required) { String }
    --outputDir, -out -> Directory to write output ElectionInitialized record (always required) { String }
    --createdBy, -createdBy -> who created { String }
    --help, -h -> Usage info 
````

input:
*  _inputDir_/electionConfig.protobuf

output:
* _trusteeDir_/decryptingTrustee-_guardianId_.protobuf
* _outputDir_/electionInitialized.protobuf


## Run Batch Encryption

````
Usage: RunBatchEncryption options_list
Options: 
    --inputDir, -in -> Directory containing input ElectionInitialized.protobuf file (always required) { String }
    --ballotDir, -ballots -> Directory to read Plaintext ballots from (always required) { String }
    --outputDir, -out -> Directory to write output election record (always required) { String }
    --invalidDir, -invalid -> Directory to write invalid input ballots to { String }
    --fixedNonces, -fixed [false] -> Encrypt with fixed nonces and timestamp 
    --check, -check [None] -> Check encryption { Value should be one of [None, Verify, EncryptTwice] }
    --nthreads, -nthreads [11] -> Number of parallel threads to use { Int }
    --createdBy, -createdBy -> who created { String }
    --help, -h -> Usage info 
````

input:
*  _inputDir_/electionInitialized.protobuf
*  _ballotDir_/plaintextBallots.protobuf

output:
* _outputDir_/encryptedBallots.protobuf
* _invalidDir_/plaintextBallots.protobuf

## Run Accumulate Tally

````
Usage: RunAccumulateTally options_list
Options: 
    --inputDir, -in -> Directory containing input ElectionInitialized record and encrypted ballots (always required) { String }
    --outputDir, -out -> Directory to write output election record (always required) { String }
    --name, -name -> Name of accumulation { String }
    --createdBy, -createdBy -> who created { String }
    --help, -h -> Usage info 
````

Only CAST ballots are tallied.

input:
*  _inputDir_/electionInitialized.protobuf
*  _inputDir_/encryptedBallots.protobuf

output:
* _outputDir_/tallyResult.protobuf

#### Timing

````
AccumulateTally processed 100 ballots, took 1246 millisecs, 12 msecs per ballot
````

## Run Trusted Tally Decryption

This has access to all the trustees, so is only used for testing, or in a use case of trust.

````
Usage: RunTrustedDecryption options_list
Options: 
    --inputDir, -in -> Directory containing input election record (always required) { String }
    --trusteeDir, -trustees -> Directory to read private trustees (always required) { String }
    --outputDir, -out -> Directory to write output election record (always required) { String }
    --createdBy, -createdBy -> who created { String }
    --help, -h -> Usage info 
````

input:
*  _inputDir_/tallyResult.protobuf

output:
* _outputDir_/decryptionResult.protobuf


## Run Trusted Ballot Decryption

This has access to all the trustees, so is only used for testing, or in a use case of trust.

````
Usage: RunTrustedBallotDecryption options_list
Options: 
    --inputDir, -in -> Directory containing input election record (always required) { String }
    --trusteeDir, -trustees -> Directory to read private trustees (always required) { String }
    --outputDir, -out -> Directory to write output election record (always required) { String }
    --decryptSpoiledList, -spoiled -> decrypt spoiled ballots { String }
    --nthreads, -nthreads -> Number of parallel threads to use { Int }
    --help, -h -> Usage info 
````

The decryptSpoiledList may be:
1. a comma-delimited (no spaces) list of ballot Ids referencing encryptedBallots.protobuf
2. a fully-qualified filename of a text file containing ballot Ids (one per line) referencing encryptedBallots.protobuf
3. "All" -> decrypt all the ballots in encryptedBallots.protobuf
4. omitted -> decrypt the ballots in encryptedBallots.protobuf that have been marked SPOILED.


input:
*  _inputDir_/tallyResult.protobuf
*  _inputDir_/encryptedBallots.protobuf

output:
* _outputDir_/spoiledBallotTallies.protobuf

## Run Verifier

```` 
Usage: RunVerifier options_list
Options: 
    --inputDir, -in -> Directory containing input election record (always required) { String }
    --nthreads, -nthreads -> Number of parallel threads to use { Int }
    --showTime, -time [false] -> Show timing 
    --help, -h -> Usage info 
````

input:
*  _inputDir_/decryptionResult.protobuf
*  _inputDir_/spoiledBallotTallies.protobuf (optional)

output:
* stdout