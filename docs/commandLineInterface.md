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
