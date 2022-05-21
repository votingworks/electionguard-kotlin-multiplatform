# Command Line Programs

last update 5/20/2022

## Run Trusted KeyCeremony

This has access to all the trustees, so is only used for testing, or in a use case of trust.

````
Usage: RunTrustedKeyCeremony options_list
Options: 
    --inputDir, -in -> Directory containing input election record (always required) { String }
    --trusteeDir, -trustees -> Directory to write private trustees (always required) { String }
    --outputDir, -out -> Directory to write output election record (always required) { String }
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
Usage: RunBatchEncryption.kexe options_list
Options: 
    --inputDir, -in -> Directory containing input ElectionInitialized.protobuf file (always required) { String }
    --ballotDir, -ballots -> Directory to read Plaintext ballots from (always required) { String }
    --outputDir, -out -> Directory to write output election record (always required) { String }
    --invalidDir, -invalid -> Directory to write invalid input ballots to { String }
    --fixedNonces, -fixed -> Encrypt with fixed nonces and timestamp 
    --nthreads, -nthreads -> Number of parallel threads to use { Int }
    --createdBy, -createdBy -> who created { String }
    --help, -h -> Usage info 
````

input:
*  _inputDir_/electionInitialized.protobuf
*  _ballotDir_/plaintextBallots.protobuf

output:
* _outputDir_/encryptedBallots.protobuf
* _invalidDir_/plaintextBallots.protobuf

#### Timing

Timing on Intel(R) Xeon(R) CPU E5-1650 v3 @ 3.50GHz,
Linux jlc 5.13.0-40-generic #45~20.04.1-Ubuntu SMP Mon Apr 4 09:38:31 UTC 2022 x86_64 x86_64 x86_64 GNU/Linux

single threaded:

````
Encryption with nthreads = 1 took 63702 millisecs for 100 ballots = 637 msecs/ballot
    12500 total encryptions = 125 per ballot = 5.09616 millisecs/encryption
````

With 12 threads (Intellij/jvm):

````
Encryption with nthreads = 12 took 11789 millisecs for 100 ballots = 118 msecs/ballot
    12500 total encryptions = 125 per ballot = 0.94312 millisecs/encryption
````

With 12 threads (IntelliJ/native):

````
Encryption with nthreads = 12 took 20949 millisecs for 100 ballots = 209 msecs/ballot
    12500 total encryptions = 125 per ballot = 1.67592 millisecs/encryption
````

Native executable:

````
$ build/bin/native/RunBatchEncryptionReleaseExecutable/RunBatchEncryption.kexe   -in src/commonTest/data/runWorkflowAllAvailable   -ballots src/commonTest/data/runWorkflowAllAvailable/private_data/input   -out testOut/RunBatchEncryption   -nthreads 12

Encryption with nthreads = 12 took 14155 millisecs for 100 ballots = 142 msecs/ballot
    12500 total encryptions = 125 per ballot = 1.1324 millisecs/encryption
````

## Run Accumulate Tally

````
Usage: RunAccumulateTally options_list
Options: 
    --inputDir, -in -> Directory containing input election record and encrypted ballots (always required) { String }
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

## Run Trusted Decryption

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
    --createdBy, -createdBy -> who created { String }
    --help, -h -> Usage info 
````

The decryptSpoiledList may be:
1. a comma-delimited (no spaces) list of ballot Ids referencing encryptedBallots.protobuf
2. a fully-qualified filename of a text file containing ballot Ids (one per line) referencing encryptedBallots.protobuf
3. omitted, then decrypt the ballots in encryptedBallots.protobuf that have been marked SPOILED.


input:
*  _inputDir_/tallyResult.protobuf
*  _inputDir_/encryptedBallots.protobuf

output:
* _outputDir_/spoiledBallotTallies.protobuf

#### timing

Decrypt ballots with nthreads = 11 took 245 secs for 100 ballots = 2.45464 secs/ballot

## Run Verifier

```` 
Usage: RunVerifier options_list
Options: 
    --inputDir, -in -> Directory containing input election record (always required) { String }
    --help, -h -> Usage info 
````

input:
*  _inputDir_/decryptionResult.protobuf
*  _inputDir_/spoiledBallotTallies.protobuf (optional)

output:
* stdout