package electionguard.verifier

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.ballot.EncryptedBallot
import electionguard.core.*

//////////////////////////////////////////////////////////////////////////////
// pre-encryption

// TODO specify sigma in manifest
private fun sigma(hash: UInt256): String = hash.toHex().substring(0, 5)

/*
Every step of verification that applies to traditional ElectionGuard ballots also applies to pre-
encrypted ballots – with the exception of the process for computing confirmation codes. However,
52there are some additional verification steps that must be applied to pre-encrypted ballots. Specifi-
cally, the following verifications should be done for every pre-encrypted cast ballot contained in the
election record.
    • The ballot confirmation code correctly matches the hash of all contest hashes on the ballot
    (listed sequentially).
    • Each contest hash correctly matches the hash of all selection hashes (including null selection
    hashes) within that contest (sorted within each contest).
    • All short codes shown to voters are correctly computed from selection hashes in the election
    record which are, in turn, correctly computed from the pre-encryption vectors published in
    the election record.
    • For contests with selection limit greater than 1, the selection vectors published in the election
    record match the product of the pre-encryptions associated with the short codes listed as
    selected.
The following verifications should be done for every pre-encrypted ballot listed in the election
record as uncast.
    • The ballot confirmation code correctly matches the hash of all contest hashes on the ballot
    (listed sequentially).
    • Each contest hash correctly matches the hash of all selection hashes (including null selection
    hashes) within that contest (sorted within each contest).
    • All short codes on the ballot are correctly computed from the selection hashes in the election
    record which are, in turn, correctly computed from the pre-encryption vectors published in
    the election record.
    • The decryptions of all pre-encryptions correspond to the plaintext values indicated in the
    election manifest.
 */

// Verification 18 (Validation of short codes in pre-encrypted ballots)
// An election verifier must confirm for every selectable option on every pre-encrypted ballot in the
//   election record that the short code ω displayed with the selectable option satisfies
//     (18.A) ω = Ω(ψ) where ψ is the selection hash associated with the selectable option.
//   Specifically, for cast ballots, this includes all short codes that are published in the election record
//   whose associated selection hashes correspond to selection vectors that are accumulated to form
//   tallies. For spoiled ballots, this includes all selection vectors on the ballot.
//    (18.B)  An election verifier must also confirm that for contests with selection limit greater than 1, the se-
//   lection vectors published in the election record match the product of the pre-encryptions associated
//   with the short codes listed as selected.
fun VerifyEncryptedBallots.verifyPreencryptionShortCodes(
    ballotId: String,
    contest: EncryptedBallot.Contest
): Result<Boolean, String> {
    val results = mutableListOf<Result<Boolean, String>>()

    if (contest.preEncryption == null) {
        results.add(Err("    18. Contest ${contest.contestId} for preencrypted '${ballotId}' has no preEncryption"))
        return results.merge()
    }
    val cv = contest.preEncryption
    val contestLimit = manifest.contestLimit(contest.contestId)
    val nselection = contest.selections.size

    require(contestLimit == cv.selectedVectors.size)
    require(contestLimit + nselection == cv.allSelectionHashes.size)

    // All short codes on the ballot are correctly computed from the pre-encrypted selections associated with each short code
    cv.selectedVectors.forEach { sv ->
        if (sv.shortCode != sigma(sv.selectionHash)) {
            results.add(Err("    18.A Contest ${contest.contestId} shortCode '${sv.shortCode}' has no match"))
        }
    }

    // Note that in a contest with a selection limit of one, the selection vector will be identical to one of
    // the pre-encryption selection vectors. However, when a contest has a selection limit greater than
    // one, the resulting selection vector will be a product of multiple pre-encryption selection vectors.

    val selectionVector: List<ElGamalCiphertext> = contest.selections.map { it.encryptedVote }
    require(contestLimit == cv.selectedVectors.size)

    // product of multiple pre-encryption selection vectors. component-wise I think
    for (idx in 0 until nselection) {
        val compList = cv.selectedVectors.map { it.encryptions[idx] }
        val sum = compList.encryptedSum()
        if (sum != selectionVector[idx]) {
            results.add(Err("    18.B Contest ${contest.contestId} (contestLimit=$contestLimit) selectionVector $idx does not match product"))
        }
    }

    return results.merge()
}

//  Verification 17 (Validation of confirmation codes in pre-encrypted ballots)
// An election verifier must confirm the following for each pre-encrypted ballot B.
//  (17.A) For each selection in each contest on the ballot and the corresponding selection vector
//    Ψi,m = ⟨E1 , E2 , . . . , Em ⟩ consisting of the selection encryptions Ej = (αj , βj ), the selection
//    hash ψi satisfies ψi = H(HE ; 0x40, K, α1 , β1 , α2 , β2 , . . . , αm , βm ).
//  (17.B) The contest hash χl for the contest with context index l for all 1 ≤ l ≤ mB has been
//    correctly computed from the selection hashes ψi as
//    χl = H(HE ; 0x41, l, K, ψσ(1) , ψσ(2) , . . . , ψσ(m+L) ),
//    where σ is a permutation and ψσ(1) < ψσ(2) < · · · < ψσ(m+L) .
//  (17.C) The ballot confirmation code H(B) has been correctly computed from the (sequentially
//    ordered) contest hashes and if specified in the election manifest file from the additional byte
//    array Baux as H(B) = H(HE ; 0x42, χ1 , χ2 , . . . , χmB , Baux ).
//  (17.D) There are no duplicate confirmation codes, i.e. among the set of submitted (cast and chal-
//    lenged) ballots, no two have the same confirmation code.
fun VerifyEncryptedBallots.verifyPreencryptedCode(ballot: EncryptedBallot): Result<Boolean, String> {
    val errors = mutableListOf<String>()

    val contestHashes = mutableListOf<UInt256>()
    for (contest in ballot.contests) {
        if (contest.preEncryption == null) {
            errors.add("    17. Contest ${contest.contestId} for preencrypted '${ballot.ballotId}' has no preEncryption")
            continue
        }
        val cv = contest.preEncryption
        for (sv in cv.selectedVectors) {
            val hashVector: List<ElementModP> = sv.encryptions.map { listOf(it.pad, it.data) }.flatten()
            val selectionHash = hashFunction(extendedBaseHash.bytes, 0x40.toByte(), jointPublicKey.key, hashVector)
            if (selectionHash != sv.selectionHash) {
                errors.add("    17.A. Incorrect selectionHash for selection shortCode=${sv.shortCode} contest=${contest.contestId} ballot='${ballot.ballotId}' ")
            }
        }

        // χl = H(HE ; 0x41, indc (Λl ), K, ψσ(1) , ψσ(2) , . . . , ψσ(m+L) ) ; 94
        val preencryptionHash = hashFunction(
            extendedBaseHash.bytes,
            0x41.toByte(),
            contest.sequenceOrder,
            jointPublicKey.key,
            cv.allSelectionHashes
        )
        if (preencryptionHash != cv.preencryptionHash) {
            errors.add("    17.B. Incorrect contestHash for ${contest.contestId} ballot='${ballot.ballotId}' ")
        }
        contestHashes.add(preencryptionHash)
    }

    val confirmationCode = hashFunction(extendedBaseHash.bytes, 0x42.toByte(), contestHashes, ballot.codeBaux)
    if (confirmationCode != ballot.confirmationCode) {
        errors.add("    17.C. Incorrect confirmationCode ballot='${ballot.ballotId}' ")
    }

    return if (errors.isEmpty()) Ok(true) else Err(errors.joinToString("\n"))
}