# 🗳 Test Vectors (proposed)

draft 6/17/2023

* These are JSON files that give inputs and expected outputs for the purpose of testing interoperability between implementations.
* The JSON formats are ad-hoc. Suggestions for improvements are welcome!
* The Kotlin code generates and reads back the JSON test vectors. From there you can find the implementation of that feature.


| Name            | JSON file                                                                        | Kotlin code                                                                                 |
|-----------------|----------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------|
| Parameters      | [json](../egklib/src/commonTest/data/testvectors/ParametersTestVector.json)      | [code](../egklib/src/jvmTest/kotlin/electionguard/testvectors/ParametersTestVector.kt)      |
| KeyCeremony     | [json](../egklib/src/commonTest/data/testvectors/KeyCeremonyTestVector.json)     | [code](../egklib/src/jvmTest/kotlin/electionguard/testvectors/KeyCeremonyTestVector.kt)     |
| ShareEncryption | [code](../egklib/src/commonTest/data/testvectors/ShareEncryptionTestVector.json) | [code](../egklib/src/jvmTest/kotlin/electionguard/testvectors/ShareEncryptionTestVector.kt) |
| ... moar soon   |                                                                                  |                                                                                             |