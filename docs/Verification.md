# Verification

_last update 9/07/2023_

ElectionGuard-Kotlin-Multiplatform library fully implements the **Verifier** section 6 of the ElectionGuard specification.
Use the classes in the _egklib/src/commonMain/kotlin/electionguard/verifier_ package in your own program, 
or the existing CLI program.

## Run Verifier Command Line Interface

See _Building a library fat jar_ in [GettingStarted](GettingStarted.md), then run the verifier like:

```
/usr/lib/jvm/jdk-19/bin/java \
    -Dfile.encoding=UTF-8 -Dsun.stdout.encoding=UTF-8 -Dsun.stderr.encoding=UTF-8 \
    -jar devhome/egk-webapps/egklib/build/libs/egklib-2.0.0-all.jar \
    -in /path/to/election_record
```

The help output of that program is:

```` 
Usage: RunVerifier options_list
Options: 
    --inputDir, -in -> Directory containing input election record (always required) { String }
    --nthreads, -nthreads -> Number of parallel threads to use { Int }
    --showTime, -time [false] -> Show timing 
    --help, -h -> Usage info 
````
