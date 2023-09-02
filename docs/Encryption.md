# 🗳 Encryption Workflow

draft 9/02/2023

[Example Test Workflow](../egklib/src/jvmMain/kotlin/electionguard/cli/RunExampleEncryption.kt)

## 1. Create AddEncryptedBallot class

Each encrypting device has its own **AddEncryptedBallot** object, which is single-threaded only,
and must have exclusive write access to _outputDir/encrypted_ballots/deviceName_.

```
class AddEncryptedBallot(
    val group: GroupContext,
    val manifest: Manifest,
    val electionInit: ElectionInitialized,
    val deviceName : String, // the encrypting device name
    val outputDir: String, // write ballots to outputDir/encrypted_ballots/deviceName, must not have multiple writers to same directory
    val invalidDir: String, // write plaintext ballots that fail validation to this directory
    val isJson : Boolean, // must match election record serialization type
): Closeable 
```

## 2. Encrypt ballots with AddEncryptedBallot

Your election device generates a PlaintextBallot and calls AddEncryptedBallot to encrypt it:

````
    fun myEncryptBallot(ballot : PlaintextBallot) : UInt256 {
       val encryptResult: Result<CiphertextBallot, String> = addEncryptor.encrypt(ballot) // (1)
       if (encryptResult is Ok) {
           return (encryptResult.unwrap().confirmationCode) // (2)
       } else {
           println("${encryptResult.getError()}") // (3)
           // process error
       }
    }
````

   1. addEncryptor encrypts the ballot, or returns an error message.
   2. myEncryptBallot returns the confirmation code (it could also return the entire CiphertextBallot if needed).
   3. On an error, the ballot has been written to invalidDir, your system must decide what to do.

An error at this point is a misconfigured system, it's not possible for a voter to trigger an error.

AddEncryptedBallot caches the CiphertextBallot in local memory, waiting for the voter to decide to cast or challenge.
Your system now associates the PlaintextBallot with the returned confirmationCode (or the entire CiphertextBallot),
and allows the voter to cast or challenge

## 3. Voter casts the ballot

If the voter decides to cast the ballot:

````
    fun myCastBallot(confirmationCode : UInt256) : Boolean {
       val castResult = addEncryptor.cast(confirmationCode) // (1)
       if (castResult is Ok) {
           return true // (2)
       } else {
           println("${castResult.getError()}") // (3)
            // process error
            return false
      }
    }
````

   1. addEncryptor records the ballot as cast, or returns an error message.
   2. the ballot is successfully cast.
   3. An error occurs if the confirmationCode is incorrect, or has already been submitted.

## 4. Voter challenges the ballot

If the voter decides to challenge the ballot:

````
    fun myChallengeBallot(confirmationCode : UInt256) : Boolean {
       val challengeResult : Result<Boolean, String> = addEncryptor.challenge(confirmationCode) // (1)
       if (challengeResult is Ok) {
           return true // (2)
       } else {
           println("${challengeResult.getError()}") // (3)
            // process error
            return null
      }
    }
````

  1. addEncryptor records the ballot as challenged, or returns an error message.
  2. the ballot is successfully challenged, and is in the election record for further verification.
  3. An error occurs if the confirmationCode is incorrect, or has already been submitted.

## 5. Voter challenges the ballot and get an immediate decryption

If the voter decides to challenge the ballot and get an immediate decryption:

````
  fun myChallengeAndDecryptBallot(confirmationCode : UInt256) : PlaintextBallot? {

      val decryptResult = addEncryptor.challengeAndDecrypt(ccode) // (1)
      if (decryptResult is Ok) {
          return decryptResult.unwrap() // (2)
      } else {
          println("${decryptResult.getError()}") // (3)
          // process error
          return null
      }
    }
````

   1. addEncryptor records the ballot as challenged and decrypts it, or returns an error message.
   2. the ballot is successfully challenged, and is in the election record for further verification.
      myChallengeAndDecryptBallot returns the decrypted ballot, which can be compared to the original.
   3. An error occurs if the confirmationCode is incorrect, or has already been submitted.

## 6. Working with chained ballots

At configuration time, the election may be configured so that the confirmation codes are "chained" together
(see _Ballot Chaining_, section 3.4.3 of the 2.0.0 specification). Each device creates its own ballot chain.
A process using AddEncryptedBallot may start and stop, and the library will continue the ballot chain from where
it left off. Make sure there are never multiple writers at the same time for the same election record and device.


