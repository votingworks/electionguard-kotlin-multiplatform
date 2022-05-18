# Command Line Programs

draft 5/17/2022 for proto_version = 2.0.0

## Generate electionConfig

## Run Batch Encryption

````
Usage: RunBatchEncryption.kexe options_list
Options:
--inputDir, -in -> Directory containing input election record (always required) { String }
--ballotDir, -ballots -> Directory to read Plaintext ballots from (always required) { String }
--outputDir, -out -> Directory to write output election record (always required) { String }
--invalidDir, -invalidBallots -> Directory to write invalid Plaintext ballots to { String }
--fixedNonces, -fixedNonces -> Encrypt with fixed nonces and timestamp
--nthreads, -nthreads -> Number of parellel threads to use { Int }
--help, -h -> Usage info 
````

Native executable gives output:

````
$ build/bin/native/RunBatchEncryptionReleaseExecutable/RunBatchEncryption.kexe   -in src/commonTest/data/runWorkflowAllAvailable   -ballots src/commonTest/data/runWorkflowAllAvailable/private_data/input   -out testOut/RunBatchEncryption   -nthreads 11
RunBatchEncryption starting
   input= src/commonTest/data/runWorkflowAllAvailable
   ballots = src/commonTest/data/runWorkflowAllAvailable/private_data/input
   output = testOut/RunBatchEncryption
Encryption with nthreads = 11 took 14774 millisecs for 100 ballots = 148 msecs/ballot
    12500 total encryptions = 125 per ballot = 1.18192 millisecs/encryption
````
