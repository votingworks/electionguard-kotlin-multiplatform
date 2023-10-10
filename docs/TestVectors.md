# 🗳 Test Vectors (proposed)

draft 6/27/2023

* These are JSON files that give inputs and expected outputs for the purpose of testing interoperability between implementations.
* The JSON formats are ad-hoc. Suggestions for improvements are welcome!
* The Kotlin code generates and reads back the JSON test vectors. Follow it to find the implementation of that feature. 
  Note that its just my implementation, not guaranteed to be right.

| Name                   | JSON file                                                                               | Kotlin code                                                                                       |
|------------------------|-----------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------|
| Parameters             | [JSON](../egklib/src/commonTest/data/testvectors/ParametersTestVector.json)             | [code](../egklib/src/jvmTest/kotlin/electionguard/testvectors/ParametersTestVector.kt)            |
| KeyCeremony            | [JSON](../egklib/src/commonTest/data/testvectors/KeyCeremonyTestVector.json)            | [code](../egklib/src/jvmTest/kotlin/electionguard/testvectors/KeyCeremonyTestVector.kt)           |
| ShareEncryption        | [JSON](../egklib/src/commonTest/data/testvectors/ShareEncryptionTestVector.json)        | [code](../egklib/src/jvmTest/kotlin/electionguard/testvectors/ShareEncryptionTestVector.kt)       |
| BallotEncryption       | [JSON](../egklib/src/commonTest/data/testvectors/BallotEncryptionTestVector.json)       | [code](../egklib/src/jvmTest/kotlin/electionguard/testvectors/BallotEncryptionTestVector.kt)      |
| ConfirmationCode       | [JSON](../egklib/src/commonTest/data/testvectors/ConfirmationCodeTestVector.json)       | [code](../egklib/src/jvmTest/kotlin/electionguard/testvectors/ConfirmationCodeTestVector.kt)      |
| BallotChaining         | [JSON](../egklib/src/commonTest/data/testvectors/BallotChainingTestVector.json)         | [code](../egklib/src/jvmTest/kotlin/electionguard/testvectors/BallotChainingTestVector.kt)        |
| BallotAggregation      | [JSON](../egklib/src/commonTest/data/testvectors/BallotAggregationTestVector.json)      | [code](../egklib/src/jvmTest/kotlin/electionguard/testvectors/BallotAggregationTestVector.kt)     |
| TallyDecryption        | [JSON](../egklib/src/commonTest/data/testvectors/TallyDecryptionTestVector.json)        | [code](../egklib/src/jvmTest/kotlin/electionguard/testvectors/TallyDecryptionTestVector.kt)       |
| TallyPartialDecryption | [JSON](../egklib/src/commonTest/data/testvectors/TallyPartialDecryptionTestVector.json) | [code](../egklib/src/jvmTest/kotlin/electionguard/testvectors/TallyDecryptionTestVector.kt)       |
| PreEncryption          | [JSON](../egklib/src/commonTest/data/testvectors/PreEncryptionTestVector.json)          | [code](../egklib/src/jvmTest/kotlin/electionguard/testvectors/PreEncryptionTestVector.kt)         |
| PreEncryptionRecorded  | [JSON](../egklib/src/commonTest/data/testvectors/PreEncryptionRecordedTestVector.json)  | [code](../egklib/src/jvmTest/kotlin/electionguard/testvectors/PreEncryptionRecordedTestVector.kt) |
| DecryptWithNonce       | [JSON](../egklib/src/commonTest/data/testvectors/DecryptWithNonceTestVector.json)       | [code](../egklib/src/jvmTest/kotlin/electionguard/testvectors/DecryptWithNonceTestVector.kt)      |
| DecryptBallot          | [JSON](../egklib/src/commonTest/data/testvectors/DecryptBallotTestVector.json)          | [code](../egklib/src/jvmTest/kotlin/electionguard/testvectors/DecryptBallotTestVector.kt)         |
| ... moar soon          |                                                                                         |                                                                                                   |