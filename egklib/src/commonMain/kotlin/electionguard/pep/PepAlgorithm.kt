package electionguard.pep

import com.github.michaelbull.result.Result
import electionguard.ballot.EncryptedBallot

interface PepAlgorithm {
    fun testEquivalent(ballot1: EncryptedBallot, ballot2: EncryptedBallot): Result<Boolean, String>

    fun doEgkPep(ballot1: EncryptedBallot, ballot2: EncryptedBallot): Result<BallotPep, String>
}