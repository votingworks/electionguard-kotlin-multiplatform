package electionguard.ballot

import electionguard.core.UInt256

fun makeManifest(cryptoHash: UInt256): Manifest {
    // val electionScopeId: String,
    //    val specVersion: String,
    //    val electionType: ElectionType,
    //    val startDate: String, // ISO 8601 formatted date/time
    //    val endDate: String, // ISO 8601 formatted date/time
    //    val geopoliticalUnits: List<GeopoliticalUnit>,
    //    val parties: List<Party>,
    //    val candidates: List<Candidate>,
    //    val contests: List<ContestDescription>,
    //    val ballotStyles: List<BallotStyle>,
    //    val name: InternationalizedText?,
    //    val contactInformation: ContactInformation?,
    //    val cryptoHash:
    return Manifest(
        "electionScopeId",
        "specVersion",
        Manifest.ElectionType.general,
        "startDate", "endDate",
        emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), null, null,
        cryptoHash
    )
}

fun makeContest(contest: Manifest.ContestDescription, selectionIdx: Int): PlaintextBallot.Contest {
    val selections: MutableList<PlaintextBallot.Selection> = ArrayList()
    selections.add(makeSelection(contest.selections.get(selectionIdx)))

        return PlaintextBallot.Contest(
            contest.contestId,
            contest.sequenceOrder,
            selections
        )
}

fun makeSelection(selection: Manifest.SelectionDescription): PlaintextBallot.Selection {
    return PlaintextBallot.Selection(
        selection.selectionId, selection.sequenceOrder,
        1,
    )
}