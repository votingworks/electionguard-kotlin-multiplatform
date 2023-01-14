## Remote Key Ceremony Trustee

last update 1/11/2023

The __KeyCeremonyRemoteTrustee__ command line program starts a web server for the purpose of creating 
_DecryptingTrustees_'s, as part of the *RunRemoteKeyCeremony* workflow.

KeyCeremonyRemoteTrustee is in a process separate from the _RunRemoteKeyCeremony_ admin process, 
in order to keep the Trustee's secret information secret.

````
Usage: KeyCeremonyRemoteTrustee options_list
Options: 
    --sslKeyStore, -keystore -> file path of the keystore file (always required) { String }
    --keystorePassword, -kpwd -> password for the entire keystore (always required) { String }
    --electionguardPassword, -epwd -> password for the electionguard entry (always required) { String }
    --trustees, -trusteeDir -> trustee output directory (always required) { String }
    --serverPort, -port -> listen on this port, default = 11183 { Int }
    --help, -h -> Usage info 

````

The _trusteeDir_ is the directory where the Trustee state needed for decryption will be written. 
It may be relative to the working directory, or it may be an absolute path. 
For each Trustee, a file _*decryptingTrustee-\<trusteeName>.protobuf*_ is written to that directory.

Example:

````
java -jar keyceremonytrustee/build/libs/keyceremonytrustee-all.jar \
     -keystore keystore.jks \
     -kpwd ksPassword \
     -epwd egPassword \
     -trusteeDir testOut/RunRemoteKeyCeremonyTest/private_data/trustees
````

The program exits with a 0 on success, > 0 on failure.
Typical (successful) output looks like:

````
Command java -classpath build/libs/electionguard-java-0.9.1-SNAPSHOT-all.jar com.sunya.electionguard.keyceremony.KeyCeremonyRemoteTrustee \
    -name JohnLCaron -out /local/secret/place
---StdOut---
*** KeyCeremonyRemote localhost:17111 with args JohnLCaron localhost:22458
    response JohnLCaron-3 3 2 
---- KeyCeremonyRemoteService started, listening on 22458 ----
---StdErr---
Apr 05, 2021 8:38:39 AM com.sunya.electionguard.keyceremony.KeyCeremonyRemoteTrustee receivePublicKeys
INFO: KeyCeremonyRemoteTrustee receivePublicKeys remoteTrustee-1
Apr 05, 2021 8:38:39 AM com.sunya.electionguard.keyceremony.KeyCeremonyRemoteTrustee receivePublicKeys
INFO: KeyCeremonyRemoteTrustee receivePublicKeys remoteTrustee-2
Apr 05, 2021 8:38:39 AM com.sunya.electionguard.keyceremony.KeyCeremonyRemoteTrustee sendPublicKeys
INFO: KeyCeremonyRemoteTrustee sendPublicKeys JohnLCaron-3
Apr 05, 2021 8:38:39 AM com.sunya.electionguard.keyceremony.KeyCeremonyRemoteTrustee verifyPartialKeyBackup
INFO: KeyCeremonyRemoteTrustee verifyPartialKeyBackup remoteTrustee-1
Apr 05, 2021 8:38:39 AM com.sunya.electionguard.keyceremony.KeyCeremonyRemoteTrustee verifyPartialKeyBackup
INFO: KeyCeremonyRemoteTrustee verifyPartialKeyBackup remoteTrustee-2
Apr 05, 2021 8:38:39 AM com.sunya.electionguard.keyceremony.KeyCeremonyRemoteTrustee sendPartialKeyBackup
INFO: KeyCeremonyRemoteTrustee sendPartialKeyBackup remoteTrustee-1
Apr 05, 2021 8:38:39 AM com.sunya.electionguard.keyceremony.KeyCeremonyRemoteTrustee sendPartialKeyBackup
INFO: KeyCeremonyRemoteTrustee sendPartialKeyBackup remoteTrustee-2
Apr 05, 2021 8:38:39 AM com.sunya.electionguard.keyceremony.KeyCeremonyRemoteTrustee sendJointPublicKey
INFO: KeyCeremonyRemoteTrustee sendJointPublicKey JohnLCaron-3
Apr 05, 2021 8:38:39 AM com.sunya.electionguard.keyceremony.KeyCeremonyRemoteTrustee saveState
INFO: KeyCeremonyRemoteTrustee saveState JohnLCaron-3
Apr 05, 2021 8:38:39 AM com.sunya.electionguard.keyceremony.KeyCeremonyRemoteTrustee finish
INFO: KeyCeremonyRemoteTrustee finish ok = true
*** shutting down gRPC server since JVM is shutting down
*** server shut down
---Done status = true
````

## Security Issues

The output contains the DecryptingTrustees' secret key share in plaintext, and so this process must be run in
a secure way, and the output kept private. The details of that are unspecified here.

