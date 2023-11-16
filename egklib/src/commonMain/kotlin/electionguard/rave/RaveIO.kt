package electionguard.rave

import electionguard.core.GroupContext
import electionguard.publish.Closeable

expect class RaveIO(pepDir: String, group: GroupContext, createNew: Boolean = false) {
    fun pepBallotSink(): PepBallotSinkIF

    /** Read all the PEP ratio ballots in the given directory. */
    fun iteratePepBallots(): Iterable<BallotPep>
}

interface PepBallotSinkIF : Closeable {
    fun writePepBallot(pepBallot: BallotPep)
}