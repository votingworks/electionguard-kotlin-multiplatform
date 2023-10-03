package electionguard.decrypt

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrap
import electionguard.ballot.EncryptedBallot
import electionguard.cli.RunTrustedTallyDecryption
import electionguard.core.*
import electionguard.input.ValidationMessages
import electionguard.publish.readElectionRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PepTest {
    val group = productionGroup()

    @Test
    fun testPepSimple() {
        //runPepSimple(1, 1, 1, 1, true)
        //runPepSimple(1, 1, 1, 0, false)
        runPepSimple(2, 2, 1, 1, true)
        //runPepSimple(3, 3, 1, 1, true)
        runPepSimple(3, 2, 1, 1, true)
        //runPepSimple(5, 5, 1, 1, true)
        //runPepSimple(8, 5, 1, 1, true)
        runPepSimple(8, 5, 1, 0, false)
    }

    fun runPepSimple(
        nguardians: Int,
        quorum: Int,
        numerator: Int,
        denominator: Int,
        expectEq: Boolean,
        show: Boolean = true
    ) {
        println("runPepSimple n = $quorum / $nguardians")
        val cakeEgkDecryption = makeCakeEgkDecryption(group, nguardians, quorum, (1..quorum).toList())
        val publicKeyG = ElGamalPublicKey(cakeEgkDecryption.publicKey)

        val egkPep = PepSimple(
            group,
            cakeEgkDecryption.extendedBaseHash,
            publicKeyG,
            cakeEgkDecryption.decryptor.guardians,
            cakeEgkDecryption.dtrustees,
        )


        val enc1 = numerator.encrypt(publicKeyG) // (g^ξ, K^(σ+ξ))
        val enc2 = denominator.encrypt(publicKeyG) // (g^ξ', K^(σ'+ξ'))
        val ballot1 = makeBallotForSingleCiphertext(group, enc1)
        val ballot2 = makeBallotForSingleCiphertext(group, enc2)

        group.showAndClearCountPowP()
        val resultPep = egkPep.doEgkPep(ballot1, ballot2)
        val n = nguardians
        val nd = egkPep.decryptor.ndGuardians
        val q = egkPep.decryptor.quorum
        val nenc = 1
        val expect = (12 + 8 * nd) * nenc + n * n * q // simple
        println(" after doEgkPep ${group.showAndClearCountPowP()} expect = $expect")

        assertTrue(resultPep is Ok)
        val pep: BallotPEP = resultPep.unwrap()
        if (show) {
            val sel = pep.contests[0].selections[0]
            print(" $numerator / $denominator, doEgkPep isEqual = ${pep.isEq} T = ${sel.T.toStringShort()}")
            val dvoteg = publicKeyG.dLog(sel.T)
            println(" dvote = $dvoteg")
        }
        assertEquals(expectEq, pep.isEq)
    }

    @Test
    fun testPepTrusted() {
        runPepTrusted(1, 1, 1, 1, true)
        runPepTrusted(1, 1, 1, 0, false)
        runPepTrusted(2, 2, 1, 1, true)
        runPepTrusted(3, 3, 1, 1, true)
        runPepTrusted(3, 2, 1, 1, true)
        runPepTrusted(5, 5, 1, 1, true)
        runPepTrusted(8, 5, 1, 1, true)
        runPepTrusted(8, 5, 1, 0, false)
    }

    fun runPepTrusted(
        nguardians: Int,
        quorum: Int,
        numerator: Int,
        denominator: Int,
        expectEq: Boolean,
        show: Boolean = true
    ) {
        println("runPepTrusted n = $quorum / $nguardians")
        val cakeEgkDecryption = makeCakeEgkDecryption(group, nguardians, quorum, (1..quorum).toList())
        val publicKeyG = ElGamalPublicKey(cakeEgkDecryption.publicKey)

        val egkPep = PepTrusted(
            group,
            cakeEgkDecryption.extendedBaseHash,
            publicKeyG,
            cakeEgkDecryption.decryptor.guardians,
            cakeEgkDecryption.dtrustees,
            nguardians
        )

        val enc1 = numerator.encrypt(publicKeyG) // (g^ξ, K^(σ+ξ))
        val enc2 = denominator.encrypt(publicKeyG) // (g^ξ', K^(σ'+ξ'))

        val ballot1 = makeBallotForSingleCiphertext(group, enc1)
        val ballot2 = makeBallotForSingleCiphertext(group, enc2)

        group.showAndClearCountPowP()
        val resultPep = egkPep.doEgkPep(ballot1, ballot2)
        val n = nguardians
        val nd = egkPep.decryptor.ndGuardians
        val q = egkPep.decryptor.quorum
        val nenc = 1
        val expect = (8 + 16 * nd) * nenc + n * n * q // simple
        println(" after doEgkPep ${group.showAndClearCountPowP()} expect = $expect")

        assertTrue(resultPep is Ok)
        val pep: BallotPEP = resultPep.unwrap()
        if (show) {
            val sel = pep.contests[0].selections[0]
            print(" $numerator / $denominator, doEgkPep isEqual = ${pep.isEq} T = ${sel.T.toStringShort()}")
            val dvoteg = publicKeyG.dLog(sel.T)
            println(" dvote = $dvoteg")
        }
        assertEquals(expectEq, pep.isEq)
    }

    @Test
    fun testPepFull() {
        runPepFull(1, 1, 1, 1, true)
         runPepFull(1, 1, 1, 0, false)
        runPepFull(2, 2, 1, 1, true)
        runPepFull(3, 3, 1, 1, true)
        runPepFull(3, 2, 1, 1, true)
        runPepFull(5, 5, 1, 1, true)
        runPepFull(8, 5, 1, 1, true)
         runPepFull(8, 5, 1, 0, false)
    }

    fun runPepFull(
        nguardians: Int,
        quorum: Int,
        numerator: Int,
        denominator: Int,
        expectEq: Boolean,
        show: Boolean = true
    ) {
        println("runPepTrusted n = $quorum / $nguardians")
        val cakeEgkDecryption = makeCakeEgkDecryption(group, nguardians, quorum, (1..quorum).toList())
        val publicKeyG = ElGamalPublicKey(cakeEgkDecryption.publicKey)

        //    val group: GroupContext,
        //    val extendedBaseHash: UInt256,
        //    val jointPublicKey: ElGamalPublicKey,
        //    val guardians: Guardians, // all guardians
        //    decryptingTrustees: List<DecryptingTrusteeIF>, // the trustees available to decrypt
        val egkPep = DecryptorFull(
            group,
            cakeEgkDecryption.extendedBaseHash,
            publicKeyG,
            cakeEgkDecryption.decryptor.guardians,
            cakeEgkDecryption.dtrustees,
        )

        val enc1 = numerator.encrypt(publicKeyG) // (g^ξ, K^(σ+ξ))
        val enc2 = denominator.encrypt(publicKeyG) // (g^ξ', K^(σ'+ξ'))
        val ratio = makeRatioEncryption(enc1, enc2)

        group.showAndClearCountPowP()
        val resultPep = egkPep.decrypt(ratio)
        val n = nguardians
        val nd = egkPep.ndGuardians
        val q = egkPep.quorum
        val nenc = 1
        // (7+6*nd)*nd
        val expect = (7 + 5 * nd) * nd * nenc + n * n * q // simple
        println(" after doEgkPep ${group.showAndClearCountPowP()} expect = $expect")
    }

    fun makeBallotForSingleCiphertext(group: GroupContext, ciphertext: ElGamalCiphertext): EncryptedBallot {
        val selection =
            EncryptedBallot.Selection("Selection1", 1, ciphertext, generateRangeChaumPedersenProofKnownNonce(group))
        //     data class Contest(
        //        override val contestId: String, // matches ContestDescription.contestIdd
        //        override val sequenceOrder: Int, // matches ContestDescription.sequenceOrder
        //        val votesAllowed: Int, // matches ContestDescription.votesAllowed
        //        val contestHash: UInt256, // eq 58
        //        override val selections: List<Selection>,
        //        val proof: ChaumPedersenRangeProofKnownNonce,
        //        val contestData: HashedElGamalCiphertext,
        //        val preEncryption: PreEncryption? = null, // pre-encrypted ballots only
        //    ) : EncryptedBallotIF.Contest  {
        val contest = EncryptedBallot.Contest(
            "Contest1", 1, 1, UInt256.random(),
            listOf(selection), generateRangeChaumPedersenProofKnownNonce(group), generateHashedCiphertext(group)
        )
        //     override val ballotId: String,
        //    val ballotStyleId: String,  // matches a Manifest.BallotStyle
        //    val encryptingDevice: String,
        //    val timestamp: Long,
        //    val codeBaux: ByteArray, // Baux in spec 2.0.0 eq 58
        //    val confirmationCode: UInt256, // tracking code = H(B) eq 58
        //    override val contests: List<Contest>,
        //    override val state: BallotState,
        return EncryptedBallot(
            "ballotId", "ballotStyleId", "device11", 0, ByteArray(0), UInt256.random(),
            listOf(contest), EncryptedBallot.BallotState.CAST
        )
    }

    private fun makeRatioEncryption(
        enc1: ElGamalCiphertext,
        enc2: ElGamalCiphertext,
    ): ElGamalCiphertext {
        val alpha = (enc1.pad div enc2.pad)
        val beta = (enc1.data div enc2.data)
        return ElGamalCiphertext(alpha, beta)
    }
}