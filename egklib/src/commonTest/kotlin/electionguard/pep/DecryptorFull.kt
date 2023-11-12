package electionguard.pep

import electionguard.ballot.LagrangeCoordinate
import electionguard.core.*
import electionguard.decrypt.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Fully distributed decryption - experimental for PEP
 */
class DecryptorFull(
    val group: GroupContext,
    val extendedBaseHash: UInt256,
    val jointPublicKey: ElGamalPublicKey,
    val guardians: Guardians, // all guardians
    decryptingTrustees: List<DecryptingTrusteeIF>, // the trustees available to decrypt
) {
    val nguardians = guardians.guardians.size // number of guardinas
    val ndGuardians = decryptingTrustees.size // number of decrypting guardinas
    val quorum = guardians.guardians[0].coefficientCommitments().size

    val fullGuardians = decryptingTrustees.map { FullGuardian(it) }
    val publicKeys : Map<String, ElementModP>
    val lagrangeCoordinates: Map<String, LagrangeCoordinate>

    val messenger = TrustedMessenger(fullGuardians)

    init {
        // build the lagrangeCoordinates once and for all
        val dguardians = mutableListOf<LagrangeCoordinate>()
        for (trustee in decryptingTrustees) {
            val present: List<Int> = // available trustees minus me
                decryptingTrustees.filter { it.id() != trustee.id() }.map { it.xCoordinate() }
            val coeff: ElementModQ = group.computeLagrangeCoefficient(trustee.xCoordinate(), present)
            dguardians.add(LagrangeCoordinate(trustee.id(), trustee.xCoordinate(), coeff))
        }
        this.lagrangeCoordinates = dguardians.associateBy { it.guardianId }
        this.publicKeys = decryptingTrustees.associate { it.id() to it.guardianPublicKey() }
    }

    var first = false
    fun decrypt(ciphertext: ElGamalCiphertext) {
        val startDecrypt = getSystemTimeInMillis()

        // step 1
        for (fullGuardian in fullGuardians) {
            fullGuardian.step1(ciphertext)
        }

        // step 2
        for (fullGuardian in fullGuardians) {
            fullGuardian.step2()
        }

        // step 3
        for (fullGuardian in fullGuardians) {
            fullGuardian.step3()
        }

        // step 4
        for (fullGuardian in fullGuardians) {
            fullGuardian.step4()
        }
    }

    ////////////////////////////////////////////////////////////////
    inner class TrustedMessenger(val trustees: List<FullGuardian>) {

        fun send(pds : PartialDecryption) {
            trustees.forEach {
                it.receivePartialDecryption( pds)
            }
        }

        fun send(car : ChallengeAndResponse) {
            trustees.forEach {
                it.receiveChallengeAndResponse(car)
            }
        }

    }

    inner class ChallengeAndResponse(
        val id: String, // trustee id
        val challenge: ElementModQ,
        val ci: ElementModQ,
        val response: ElementModQ,
    )

    inner class WorkingValues (val id: String) {
        val partialDecryptions = mutableMapOf<String, PartialDecryption>()
        val challengeResponses = mutableMapOf<String, ChallengeAndResponse>()

        var M : ElementModP? = null
        var T : ElementModP? = null

        fun weightedProduct(): ElementModP =
            with(group) {
                partialDecryptions.map { (key, value) ->
                    val coeff = lagrangeCoordinates[key] ?: throw IllegalArgumentException()
                    value.Mi powP coeff.lagrangeCoefficient
                }.multP()
            }
    }

    inner class FullGuardian(val trustee: DecryptingTrusteeIF) {
        val work = WorkingValues(trustee.id())
        var ciphertext: ElGamalCiphertext = ElGamalCiphertext(group.ZERO_MOD_P, group.ZERO_MOD_P) // fake

        fun id() = trustee.id()

        fun step1(ciphertext: ElGamalCiphertext) {
            this.ciphertext = ciphertext
            val mypd = trustee.decrypt(group, listOf(ciphertext.pad))[0]
            messenger.send(mypd)
        }

        fun receivePartialDecryption(pds: PartialDecryption) {
            work.partialDecryptions[pds.guardianId] = pds
        }

        fun receiveChallengeAndResponse(car: ChallengeAndResponse) {
            work.challengeResponses[car.id] = car
        }

        fun step2() {
            require(work != null)

            val weightedProduct = work.weightedProduct()
            work.T = ciphertext.data / weightedProduct
            work.M = weightedProduct

            // compute the collective challenge, needed for the collective proof; spec 2.0.0 eq 70
            val a: ElementModP = with(group) { work.partialDecryptions.values.map { it.a }.multP() } // Prod(ai)
            val b: ElementModP = with(group) { work.partialDecryptions.values.map { it.b }.multP() } // Prod(bi)
            // "collective challenge" c = H(HE ; 0x30, K, A, B, a, b, M ) ; spec 2.0.0 eq 71
            val c = hashFunction(
                extendedBaseHash.bytes,
                0x30.toByte(),
                jointPublicKey.key,
                ciphertext.pad,
                ciphertext.data,
                a, b, weightedProduct
            ).toElementModQ(group)

            val pd = work.partialDecryptions[id()] ?: throw IllegalArgumentException()
            val coeff = lagrangeCoordinates[id()] ?: throw IllegalArgumentException()
            val ci = coeff.lagrangeCoefficient * c
            val challenge = ChallengeRequest("", ci, pd.u)
            val response = trustee.challenge(group, listOf(challenge))[0]

            val car = ChallengeAndResponse(id(), c, ci, response.response)
            messenger.send(car)
        }

        //    3. DGi: when recieved all (ci, vi)
        //      For each Gj, verify the response:    // 4 * nd
        //        aj' = g^vj * g^P(j)^cj mod p
        //        bj' = A^vj * Mj^cj mod p
        //      verify that ai' = ai and bi' = bi, Otherwise, reject.
        fun step3() {
            work.partialDecryptions.values.forEach {
                val pd = work.partialDecryptions[it.guardianId]!!
                val cr = work.challengeResponses[it.guardianId]!!

                //  aj' = g^vj * g^P(j)^cj mod p
                val inner = guardians.getGexpP(it.guardianId) // inner factor
                val ajp = group.gPowP(cr.response) * (inner powP cr.ci)
                assertEquals(it.a, ajp)

                // bj' = A^vj * Mj^cj mod p
                val bjp = (ciphertext.pad powP cr.response) * (pd.Mi powP cr.ci)
                assertEquals(it.b, bjp)
            }
        }

        //  v = Sum_di(vi) mod q for i in {DGi}
        // 	check ChaumPedersenProof(c, v).verifyDecryption(g, K, A, B, T) is true  // 4
        // 	publish (A, B, T, ChaumPedersenProof(c, v)) to BB
        fun step4() {
            val v: ElementModQ = with(group) { work.challengeResponses.values.map { it.response }.addQ() }
            val cr = work.challengeResponses[id()]!!
            val proof = ChaumPedersenProof(cr.challenge, v)
            val ok = proof.verifyDecryption(extendedBaseHash, jointPublicKey.key, ciphertext, work.T!!)
            assertTrue(ok)
            // return PublishValues(work.ciphertext, work.T!!, proof)
        }
    }
}

data class PublishValues(val ciphertext: ElGamalCiphertext, val T : ElementModP, val proof : ChaumPedersenProof)