package electionguard.verifier

import electionguard.ballot.ElectionRecord
import electionguard.ballot.SubmittedBallot
import electionguard.core.ConstantChaumPedersenProofKnownNonce
import electionguard.core.DisjunctiveChaumPedersenProofKnownNonce
import electionguard.core.ElGamalCiphertext
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.GroupContext
import electionguard.core.encryptedSum
import electionguard.core.hasValidSchnorrProof
import electionguard.core.isValid
import electionguard.core.toElementModQ

// quick proof verification - not necessarily the verification spec
class Verifier(val group: GroupContext, val electionRecord: ElectionRecord) {
    val publicKey: ElGamalPublicKey
    val cryptoBaseHash: ElementModQ

    init {
        if (electionRecord.context == null) {
            throw IllegalStateException("electionRecord.context is null")
        }
        publicKey = ElGamalPublicKey(electionRecord.context.jointPublicKey)
        cryptoBaseHash = electionRecord.context.cryptoExtendedBaseHash.toElementModQ(group)
    }

    fun verifyGuardianPublicKey(): Boolean {
        if (electionRecord.guardianRecords == null) {
            return false
        }
        var allValid = true
        for (guardian in electionRecord.guardianRecords) {
            var guardianOk = true
            guardian.coefficientProofs.forEachIndexed { index, proof ->
                val publicKey = ElGamalPublicKey(guardian.coefficientCommitments[index])
                val validProof = publicKey.hasValidSchnorrProof(proof)
                guardianOk = guardianOk && validProof
            }
            println("Guardian ${guardian.guardianId} ok = ${guardianOk}")
            allValid = allValid && guardianOk
        }
        return allValid
    }

    fun verifySubmittedBallots(ballots: Iterable<SubmittedBallot>): Boolean {
        if (electionRecord.context == null) {
            return false
        }

        // LOOK add multithreading
        var allValid = true
        var nballots = 0
        for (ballot in ballots) {
            nballots++
            var bvalid = true
            var ncontests = 0
            var nselections = 0
            println("Ballot '${ballot.ballotId}'")
            for (contest in ballot.contests) {
                ncontests++
                // recalculate ciphertextAccumulation
                val texts: List<ElGamalCiphertext> = contest.selections.map { it.ciphertext }
                val ciphertextAccumulation: ElGamalCiphertext = texts.encryptedSum()
                // test that the proof is correct
                val proof: ConstantChaumPedersenProofKnownNonce = contest.proof
                var cvalid = proof.isValid(
                    ciphertextAccumulation,
                    ElGamalPublicKey(electionRecord.context.jointPublicKey),
                    electionRecord.context.cryptoExtendedBaseHash.toElementModQ(group),
                )

                for (selection in contest.selections) {
                    nselections++
                    val sproof: DisjunctiveChaumPedersenProofKnownNonce = selection.proof
                    val svalid = sproof.isValid(
                        selection.ciphertext,
                        ElGamalPublicKey(electionRecord.context.jointPublicKey),
                        electionRecord.context.cryptoExtendedBaseHash.toElementModQ(group),
                    )
                    cvalid = cvalid && svalid
                }
                println("     Contest '${contest.contestId}' valid $cvalid")
                bvalid = bvalid && cvalid
            }
            allValid = allValid && bvalid
            println("   valid $bvalid; ncontests = $ncontests nselections = $nselections")
        }
        return allValid
    }

    fun verifyDecryptedTally(): Boolean {
        if (electionRecord.guardianRecords == null) {
            return false
        }
        if (electionRecord.decryptedTally == null) {
            return false
        }

        var allValid = true
        var ncontests = 0
        var nselections = 0
        var nshares = 0
        val tally = electionRecord.decryptedTally
        for (contest in tally.contests.values) {
            ncontests++

            for (selection in contest.selections.values) {
                nselections++
                val message = selection.message

                for (share in selection.shares) {
                    nshares++
                    val sproof: GenericChaumPedersenProof? = share.proof
                    if (sproof != null) {
                        val guardian = electionRecord.guardianRecords.find { it.guardianId.equals(share.guardianId)}
                        val guardianKey = guardian?.guardianPublicKey ?: group.G_MOD_P
                        val svalid = sproof.isValid(
                            group.G_MOD_P,
                            guardianKey,
                            message.pad,
                            share.share,
                            arrayOf(cryptoBaseHash, guardianKey, message.pad, message.data), // section 7
                            arrayOf(share.share)
                        )
                        if (!svalid) {
                            println("Fail guardian $guardian share proof $sproof")
                        }
                        allValid = allValid && svalid
                    }
                }
            }
        }
        println("\nTally '${tally.tallyId}' valid $allValid; ncontests = $ncontests; nselections = $nselections; nshares = $nshares\n")
        return allValid
    }
}