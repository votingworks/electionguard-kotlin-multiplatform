package electionguard.verifier

import electionguard.ballot.ElectionRecordAllData
import electionguard.core.ConstantChaumPedersenProofKnownNonce
import electionguard.core.DisjunctiveChaumPedersenProofKnownNonce
import electionguard.core.ElGamalPublicKey
import electionguard.core.ElementModQ
import electionguard.core.GenericChaumPedersenProof
import electionguard.core.GroupContext
import electionguard.core.hasValidSchnorrProof
import electionguard.core.isValid
import electionguard.core.toElementModQ

// quick proof verification - not necessarily the verification spec
class Verifier(val group: GroupContext, val electionRecord: ElectionRecordAllData) {
    val publicKey: ElGamalPublicKey = ElGamalPublicKey(electionRecord.context.jointPublicKey)
    val cryptoBaseHash: ElementModQ =
        electionRecord.context.cryptoExtendedBaseHash.toElementModQ(group)

    fun verifyGuardianPublicKey(): Boolean {
        var allValid = true
        for (guardian in electionRecord.guardianRecords) {
            var guardianOk = true
            guardian.coefficientProofs
                .forEachIndexed { index, proof ->
                    val publicKey = ElGamalPublicKey(guardian.coefficientCommitments[index])
                    val validProof = publicKey.hasValidSchnorrProof(cryptoBaseHash, proof)
                    guardianOk = guardianOk && validProof
                }
            println("Guardian ${guardian.guardianId} ok = ${guardianOk}")
            allValid = allValid && guardianOk
        }
        return allValid
    }

    fun verifySubmittedBallots(): Boolean {
        var allValid = true
        var nballots = 0

        for (ballot in electionRecord.submittedBallots) {
            nballots++
            var ncontests = 0
            var nselections = 0
            for (contest in ballot.contests) {
                ncontests++
                val proof: ConstantChaumPedersenProofKnownNonce = contest.proof
                val valid =
                    proof.isValid(
                        contest.ciphertextAccumulation,
                        ElGamalPublicKey(electionRecord.context.jointPublicKey),
                        electionRecord.context.cryptoExtendedBaseHash.toElementModQ(group),
                    )

                for (selection in contest.selections) {
                    nselections++
                    val sproof: DisjunctiveChaumPedersenProofKnownNonce = selection.proof
                    val svalid =
                        sproof.isValid(
                            selection.ciphertext,
                            ElGamalPublicKey(electionRecord.context.jointPublicKey),
                            electionRecord.context.cryptoExtendedBaseHash.toElementModQ(group),
                        )
                    allValid = allValid && svalid
                }

                println(
                    "Ballot '${ballot.ballotId}' valid $valid; ncontests = $ncontests nselections" +
                        " = $nselections"
                )
                allValid = allValid && valid
            }
        }
        return allValid
    }

    fun verifyDecryptedTally(): Boolean {
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
                        val guardian =
                            electionRecord.guardianRecords
                                .find { it.guardianId.equals(share.guardianId) }
                        val guardianKey = guardian?.guardianPublicKey ?: group.G_MOD_P
                        val svalid =
                            sproof.isValid(
                                group.G_MOD_P,
                                guardianKey,
                                message.pad,
                                share.share,
                                arrayOf(cryptoBaseHash, guardianKey, message.pad, message.data),
                                // section 7
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
        println(
            "\nTally '${tally.tallyId}' valid $allValid; ncontests = $ncontests; nselections = " +
                "$nselections; nshares = $nshares\n"
        )
        return allValid
    }
}