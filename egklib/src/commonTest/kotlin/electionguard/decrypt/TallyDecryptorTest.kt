package electionguard.decrypt

import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.core.*

import electionguard.keyceremony.KeyCeremonyTrustee
import electionguard.util.ErrorMessages
import electionguard.util.Stats
import kotlin.test.*

private val group = productionGroup()

class TallyDecryptorTest {
    internal val triple : Triple<TallyDecryptor, Guardians, List<DecryptingTrusteeIF>> = makeTallyDecryptor(2, 2)
    val extendedBaseHash = triple.first.extendedBaseHash
    val publicKey = triple.first.publicKey
    val ciphertext: ElGamalCiphertext = 1.encrypt(publicKey)
    val tally: EncryptedTally = makeTallyForSingleCiphertext(ciphertext, extendedBaseHash)

    @Test
    fun testShareNotFound() {
        val (tallyDecryptor, _, _) = triple
        val decryptions = AllDecryptions()
        val errs = ErrorMessages("TallyDecryptorTest")

        tallyDecryptor.decryptTally(tally, decryptions, Stats(), errs)
        assertContains(errs.toString(), "'Contest1#@Selection1' share not found")
    }

    @Test
    fun testGood() {
        val (tallyDecryptor, guardians, dtrustees) = triple

        // 2
        val decryptor = DecryptorDoerre(
            group,
            extendedBaseHash,
            publicKey,
            guardians,
            dtrustees,
        )
        val decryptedTally = with(decryptor) { tally.decrypt(ErrorMessages("TallyDecryptorTest2")) }
        assertNotNull(decryptedTally)

        // 3
        val decryptions = AllDecryptions()
        decrypt(tallyDecryptor, tally, dtrustees, decryptions)
        val decryptedTally2 = tallyDecryptor.decryptTally(tally, decryptions, Stats(), ErrorMessages("TallyDecryptorTest3"))
        assertNotNull(decryptedTally2)
    }

    @Test
    fun testBadShareM() {
        val (tallyDecryptor, _, dtrustees) = triple
        val decryptions = AllDecryptions()
        val errs = ErrorMessages("testBadShare")
        decrypt(tallyDecryptor, tally, dtrustees, decryptions)

        // mess it up
        val share: DecryptionResults = decryptions.shares["Contest1#@Selection1"]!!
        share.M = group.ONE_MOD_P

        tallyDecryptor.decryptTally(tally, decryptions, Stats(), errs)
        assertContains(errs.toString(), "verifySelection error on Contest1/Selection1")
        println("testBadShareM: $errs")
    }

    @Test
    fun testBadResponse() {
        val (tallyDecryptor, _, dtrustees) = triple
        val decryptions = AllDecryptions()
        val errs = ErrorMessages("testBadShare")
        decrypt(tallyDecryptor, tally, dtrustees, decryptions)

        // mess it up
        val share: DecryptionResults = decryptions.shares["Contest1#@Selection1"]!!
        share.responses["trustee1"] = group.ONE_MOD_Q

        tallyDecryptor.decryptTally(tally, decryptions, Stats(), errs)
        assertContains(errs.toString(), "verifySelection error on Contest1/Selection1")
        println("testBadResponse: $errs")
    }

    @Test
    fun testChangePartialDecryption() {
        val (tallyDecryptor, _, dtrustees) = triple
        val decryptions = AllDecryptions()
        val errs = ErrorMessages("testBadShare")
        decrypt(tallyDecryptor, tally, dtrustees, decryptions)

        // mess it up
        val share: DecryptionResults = decryptions.shares["Contest1#@Selection1"]!!
        val partialDecryption : PartialDecryption = share.shares["guardian2"]!!
        share.shares["guardian2"] = partialDecryption.copy(a = group.ONE_MOD_P)

        // this does not fail, because it was changed after the decryption was made
        assertNotNull(tallyDecryptor.decryptTally(tally, decryptions, Stats(), errs))
    }

    @Test
    fun testBadPartialDecryption() {
        val (tallyDecryptor, _, dtrustees) = triple
        val decryptions = AllDecryptions()
        val errs = ErrorMessages("testBadShare")
        decrypt(tallyDecryptor, tally, dtrustees, decryptions, true)

        tallyDecryptor.decryptTally(tally, decryptions, Stats(), errs)
        println("testBadPartialDecryption: $errs")
        assertContains(errs.toString(), "verifySelection error on Contest1/Selection1")
        assertContains(errs.toString(), "checkIndividualResponses has errors:")
        assertContains(errs.toString(), "ai != ai' dont match for guardian2")
    }

    internal fun makeTallyDecryptor(
        nguardians: Int,
        quorum: Int,
    ): Triple<TallyDecryptor, Guardians, List<DecryptingTrusteeIF>> {

        // Run our own KeyCeremony
        val ktrustees: List<KeyCeremonyTrustee> = List(nguardians) {
            val seq = it + 1
            KeyCeremonyTrustee(group, "guardian$seq", seq, nguardians, quorum)
        }.sortedBy { it.xCoordinate }
        // exchange PublicKeys
        ktrustees.forEach { t1 ->
            ktrustees.forEach { t2 ->
                t1.receivePublicKeys(t2.publicKeys().unwrap())
            }
        }
        // exchange SecretKeyShares
        ktrustees.forEach { t1 ->
            ktrustees.filter { it.id != t1.id }.forEach { t2 ->
                t2.receiveEncryptedKeyShare(t1.encryptedKeyShareFor(t2.id).unwrap())
            }
        }
        ktrustees.forEach { it.isComplete() }
        val guardians = Guardians(group, ktrustees.map { makeGuardian(it) })

        val extendedBaseHash = UInt256.random()
        val dtrustees: List<DecryptingTrusteeIF> = ktrustees.map { makeDoerreTrustee(it, extendedBaseHash) }
        val jointPublicKey = dtrustees.map { it.guardianPublicKey() }.reduce { a, b -> a * b }

        val lagrange = mutableListOf<LagrangeCoordinate>()
        for (trustee in dtrustees) {
            val present: List<Int> = // available trustees minus me
                dtrustees.filter { it.id() != trustee.id() }.map { it.xCoordinate() }
            val coeff: ElementModQ = group.computeLagrangeCoefficient(trustee.xCoordinate(), present)
            lagrange.add(LagrangeCoordinate(trustee.id(), trustee.xCoordinate(), coeff))
        }
        val lagrangeCoordinates: Map<String, LagrangeCoordinate> = lagrange.associateBy { it.guardianId }
        val tallyDecryptor =
            TallyDecryptor(group, extendedBaseHash, ElGamalPublicKey(jointPublicKey), lagrangeCoordinates, guardians)

        return Triple(tallyDecryptor, guardians, dtrustees)
    }

    internal fun decrypt(
            tallyDecryptor: TallyDecryptor,
            tally: EncryptedTally,
            decryptingTrustees: List<DecryptingTrusteeIF>,
            allDecryptions : AllDecryptions,
            messItUp : Boolean = false,
        ) {

        // must get the DecryptionResults from all trustees before we can do the challenges
        val trusteeDecryptions = mutableListOf<TrusteeDecryptions>()
        for (decryptingTrustee in decryptingTrustees) { // could be parallel
            trusteeDecryptions.add(tally.getPartialDecryptionFromTrustee(decryptingTrustee, false))
        }
        if (messItUp) {
            val decryptionResult : DecryptionResult = trusteeDecryptions[1].shares["Contest1#@Selection1"]!!
            val partialDecryptionMessed : PartialDecryption = decryptionResult.share.copy(a = group.ONE_MOD_P)
            trusteeDecryptions[1].shares["Contest1#@Selection1"] = decryptionResult.copy( share = partialDecryptionMessed)
        }
        trusteeDecryptions.forEach { allDecryptions.addTrusteeDecryptions(it) }

        // compute M for each DecryptionResults over all the shares from available guardians
        for ((selectionKey, dresults) in allDecryptions.shares) {
            // TODO if nguardians = 1, can set weightedProduct = Mi.
            // lagrange weighted product of the shares, M = Prod(M_i^w_i) mod p; spec 2.0.0, eq 68
            val weightedProduct = with(group) {
                dresults.shares.map { (key, value) ->
                    val coeff = tallyDecryptor.lagrangeCoordinates[key] ?: throw IllegalArgumentException()
                    value.Mi powP coeff.lagrangeCoefficient
                }.multP()
            }

            val T = dresults.ciphertext.data / weightedProduct
            dresults.tally = publicKey.dLog(T, 100)
                ?: throw RuntimeException("dLog not found on $selectionKey")
            dresults.M = weightedProduct

            // compute the collective challenge, needed for the collective proof; spec 2.0.0 eq 70
            val a: ElementModP = with(group) { dresults.shares.values.map { it.a }.multP() } // Prod(ai)
            val b: ElementModP = with(group) { dresults.shares.values.map { it.b }.multP() } // Prod(bi)
            // "collective challenge" c = H(HE ; 0x30, K, A, B, a, b, M ) ; spec 2.0.0 eq 71
            dresults.collectiveChallenge = hashFunction(
                extendedBaseHash.bytes,
                0x30.toByte(),
                publicKey.key,
                dresults.ciphertext.pad,
                dresults.ciphertext.data,
                a, b, weightedProduct
            )
        }

        // now that we have the collective challenges, gather the individual challenges for both decryption and
        // contestData from each trustee, to construct the proofs.
        val trusteeChallengeResponses = mutableListOf<TrusteeChallengeResponses>()
        for (trustee in decryptingTrustees) {
            trusteeChallengeResponses.add(allDecryptions.challengeTrustee(trustee, tallyDecryptor.lagrangeCoordinates[trustee.id()]))
        }
        trusteeChallengeResponses.forEach { challengeResponses ->
            challengeResponses.results.forEach {
                require(allDecryptions.addChallengeResponse(challengeResponses.id, it))
            }
        }
    }

    private fun EncryptedTally.getPartialDecryptionFromTrustee(
        trustee: DecryptingTrusteeIF,
        isBallot: Boolean,
    ) : TrusteeDecryptions {

        val texts: MutableList<ElementModP> = mutableListOf()
        for (contest in this.contests) {
            if (isBallot && contest.contestData != null) {
                texts.add(contest.contestData!!.c0)
            }
            for (selection in contest.selections) {
                texts.add(selection.encryptedVote.pad)
            }
        }

        // decrypt all of them at once
        val results: List<PartialDecryption> = trustee.decrypt(group, texts)

        // Place the results into the TrusteeDecryptions
        val trusteeDecryptions = TrusteeDecryptions(trustee.id())
        var count = 0
        for (contest in this.contests) {
            if (isBallot && contest.contestData != null) {
                trusteeDecryptions.addContestDataResults(contest.contestId, contest.contestData!!, results[count++])
            }
            for (selection in contest.selections) {
                trusteeDecryptions.addDecryption(contest.contestId, selection.selectionId, selection.encryptedVote, results[count++])
            }
        }
        return trusteeDecryptions
    }

    fun AllDecryptions.challengeTrustee(trustee: DecryptingTrusteeIF, lagrangeCoordinate : LagrangeCoordinate?) : TrusteeChallengeResponses {
        // Get all the challenges from the shares for this trustee
        val wi = lagrangeCoordinate!!.lagrangeCoefficient
        val requests: MutableList<ChallengeRequest> = mutableListOf()
        for ((selectionKey, results) in this.shares) {
            val result = results.shares[trustee.id()] ?: throw IllegalStateException("missing ${trustee.id()}")
            // spec 2.0.0, eq 72
            val ci = wi * results.collectiveChallenge!!.toElementModQ(group)
            requests.add(ChallengeRequest(selectionKey, ci, result.u))
        }
        // Get all the challenges from the contestData
        for ((id, results) in this.contestData) {
            val result = results.shares[trustee.id()] ?: throw IllegalStateException("missing ${trustee.id()}")
            // spec 2.0.0, eq 72
            val ci = wi * results.collectiveChallenge!!.toElementModQ(group)
            requests.add(ChallengeRequest(id, ci, result.u))
        }

        // ask for all of them at once from the trustee
        val results: List<ChallengeResponse> = trustee.challenge(group, requests)
        return TrusteeChallengeResponses(trustee.id(), results)
    }

}


fun makeTallyForSingleCiphertext(ciphertext : ElGamalCiphertext, extendedBaseHash : UInt256) : EncryptedTally {
    val selection = EncryptedTally.Selection("Selection1", 1, ciphertext)
    val contest = EncryptedTally.Contest("Contest1", 1, listOf(selection))
    return EncryptedTally("tallyId", listOf(contest), emptyList(), extendedBaseHash)
}




