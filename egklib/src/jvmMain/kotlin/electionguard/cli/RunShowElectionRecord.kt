package electionguard.cli

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getError
import com.github.michaelbull.result.unwrap
import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.ElectionConstants
import electionguard.ballot.Guardian
import electionguard.ballot.LagrangeCoordinate
import electionguard.ballot.Manifest
import electionguard.core.Base16.toHex
import electionguard.core.GroupContext
import electionguard.core.SchnorrProof
import electionguard.core.productionGroup
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.publish.Consumer
import electionguard.publish.ElectionRecord
import electionguard.publish.readElectionRecord
import electionguard.publish.makeConsumer
import electionguard.util.ErrorMessages
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

/** Show the election record in inputDir. */
class RunShowElectionRecord {

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val parser = ArgParser("RunElectionRecordShow")
            val inputDir by parser.option(
                ArgType.String,
                shortName = "in",
                description = "Directory containing input Election Record"
            ).required()
            val show by parser.option(
                ArgType.String,
                shortName = "show",
                description = "[all,constants,manifest,guardians,lagrange,trustees]"
            )
            val details by parser.option(
                ArgType.Boolean,
                shortName = "details",
                description = "show details"
            )
            val ballotStyle by parser.option(
                ArgType.String,
                description = "for just one ballot style",
                shortName = "ballotStyle"
            )
            parser.parse(args)

            val showSet = if (show == null) emptySet() else show!!.split(",").toSet()

            showElectionRecord(productionGroup(), inputDir, ShowSet(showSet), details ?: false, ballotStyle)
        }

        class ShowSet(val want: Set<String>) {
            fun has(show: String) = want.contains("all") || want.contains(show)
        }

        fun showElectionRecord(group: GroupContext, inputDir: String, showSet: ShowSet, details: Boolean, ballotStyle : String?) {
            val consumer = makeConsumer(group, inputDir)
            val electionRecord = readElectionRecord(consumer)
            println("ShowElectionRecord from $inputDir, stage = ${electionRecord.stage()}")
            if (ballotStyle != null) println("  for just ballot style='${ballotStyle}'")

            val config = electionRecord.config()
            println(" config numberOfGuardians = ${config.numberOfGuardians}")
            println(" config quorum  = ${config.quorum}")
            println(" config metadata  = ${config.metadata}")
            if (showSet.has("constants")) {
                print(" ${config.constants.show()}")

            }
            if (showSet.has("manifest")) {
                print(" ${electionRecord.manifest().show(details, ballotStyle)}")
            }
            println()

            if (electionRecord.stage() == ElectionRecord.Stage.CONFIG) {
                return
            }

            val init = electionRecord.electionInit()
            if (init != null) {
                println(" init guardians.size = ${init.guardians.size}")
                println(" init metadata  = ${init.metadata}")
                if (showSet.has("guardians")) {
                    print(init.guardians.showGuardians(details))
                }
                println()

                if (showSet.has("trustees")) {
                    val trusteeDir = "$inputDir/private_data/trustees"
                    print(init.guardians.showTrustees(consumer, trusteeDir))
                }
                println()
            }

            if (electionRecord.stage() == ElectionRecord.Stage.INIT) {
                return
            }

            val ecount = consumer.iterateAllEncryptedBallots { true }.count()
            println(" $ecount encryptedBallots")
            println()

            if (electionRecord.stage() == ElectionRecord.Stage.ENCRYPTED) {
                return
            }

            val tally = electionRecord.tallyResult()
            if (tally != null) {
                println(" encryptedTally ncontests = ${tally.encryptedTally.contests.size}")
                val nselections = tally.encryptedTally.contests.sumOf { it.selections.size }
                println(" encryptedTally nselections = $nselections")
                println(" metadata  = ${tally.metadata}")
                println()
            }

            if (electionRecord.stage() == ElectionRecord.Stage.TALLIED) {
                return
            }

            val dtally = electionRecord.decryptionResult()
            if (dtally != null) {
                // println(" decryptedTally available=${dtally.lagrangeCoordinates.size}")
                println(" decryptedTally ${dtally.decryptedTally.show(details, electionRecord.manifest())}")
                //if (showSet.has("lagrange")) {
                //    print(dtally.lagrangeCoordinates.showLagrange())
                // }
            }
        }

        fun ElectionConstants.show(): String {
            val builder = StringBuilder(5000)
            builder.appendLine("ElectionConstants")
            builder.appendLine("  LargePrime ${this.largePrime.toHex()}")
            builder.appendLine("  SmallPrime ${this.smallPrime.toHex()}")
            builder.appendLine("  Cofactor ${this.cofactor.toHex()}")
            builder.appendLine("  Generator ${this.generator.toHex()}")
            return builder.toString()
        }

        fun Manifest.show(details: Boolean, wantBallotStyle: String?): String {
            val builder = StringBuilder(5000)
            builder.appendLine("\nManifest scopeId=${this.electionScopeId} type=${this.electionType} spec=${this.specVersion}")
            builder.appendLine("  gpus: ${this.geopoliticalUnits}")
            if (wantBallotStyle == null ) {
                builder.appendLine("  styles: ${this.ballotStyles}\n")
            } else {
                val ballotStyle = this.ballotStyles.find {it.ballotStyleId == wantBallotStyle}
                if (ballotStyle == null) {
                    builder.appendLine("  NOT FOUND ballot style '$wantBallotStyle'")
                    return builder.toString()
                } else {
                    builder.appendLine("  ballot style: ${ballotStyle}")
                }
            }
            val wantContests = if (wantBallotStyle == null) this.contests else this.styleToContestsMap[wantBallotStyle]?: emptyList()
            builder.append(wantContests.showContests(details))
            return builder.toString()
        }

        fun List<Manifest.ContestDescription>.showContests(details: Boolean): String {
            val builder = StringBuilder(5000)
            this.forEach { contest ->
                if (details) {
                    builder.append("  Contest ${contest.sequenceOrder} '${contest.contestId}': geo=${contest.geopoliticalUnitId}, ")
                    if (contest.votesAllowed != 1 || contest.optionSelectionLimit != 1)  builder.append("limits=${contest.votesAllowed}/${contest.optionSelectionLimit}")
                    if (contest.voteVariation != Manifest.VoteVariationType.one_of_m) builder.append(", ${contest.voteVariation}")
                    builder.appendLine()
                    contest.selections.forEach {
                        builder.append("   ${it.sequenceOrder} '${it.selectionId}'")
                        if (it.candidateId != it.selectionId) builder.append(" cand=${it.candidateId}")
                        builder.appendLine()
                    }
                    builder.appendLine()
                } else {
                    builder.append("  ${contest.contestId}: ")
                    contest.selections.forEach {
                        builder.append(" ${it.candidateId} (${it.selectionId});")
                    }
                    builder.appendLine()
                }
            }
            return builder.toString()
        }

        fun List<Guardian>.showGuardians(details: Boolean): String {
            val builder = StringBuilder(5000)
            builder.appendLine(" Guardians")
            this.sortedBy { it.guardianId }.forEach { guardian ->
                builder.appendLine("  ${guardian.guardianId} xcoord=${guardian.xCoordinate} nproofs=${guardian.coefficientProofs.size}")
                if (details) {
                    guardian.coefficientProofs.forEach { proof ->
                        builder.appendLine("   ${proof.show()}")
                    }
                }
            }
            return builder.toString()
        }

        fun List<Guardian>.showTrustees(consumer: Consumer, trusteeDir: String): String {
            val builder = StringBuilder(5000)
            builder.appendLine(" Trustees")
            this.sortedBy { it.guardianId }.forEach { guardian ->
                val result: Result<DecryptingTrusteeIF, ErrorMessages> =
                    consumer.readTrustee(trusteeDir, guardian.guardianId)
                if (result is Ok) {
                    val trustee = result.unwrap()
                    builder.appendLine("  ${trustee.id()} xcoord=${trustee.xCoordinate()}")
                } else {
                    builder.appendLine(result.getError().toString())
                }
            }
            return builder.toString()
        }

        fun SchnorrProof.show(): String {
            val builder = StringBuilder(5000)
            builder.append("SchnorrProof key=${this.publicKey.toStringShort()}")
            builder.append(" challenge=${this.challenge}")
            builder.append(" response=${this.response}")
            return builder.toString()
        }

        fun DecryptedTallyOrBallot.show(details: Boolean, manifest: Manifest): String {
            val builder = StringBuilder(5000)
            builder.appendLine(" DecryptedTallyOrBallot $id")
            contests.sortedBy { it.contestId }.forEach { contest ->
                if (details) {
                    builder.appendLine("  Contest ${contest.contestId}")
                    contest.selections.sortedBy { -it.tally }.forEach {
                        val candidate =
                            manifest.selectionCandidate["${contest.contestId}/${it.selectionId}"] ?: "unknown"
                        builder.appendLine("   $candidate (${it.selectionId}) = ${it.tally}")
                    }
                } else {
                    builder.append("  ${contest.contestId}")
                    contest.selections.sortedBy { -it.tally }.forEach {
                        val candidate =
                            manifest.selectionCandidate["${contest.contestId}/${it.selectionId}"] ?: "unknown"
                        builder.append("   ${candidate} (${it.tally})")
                    }
                }
                builder.appendLine()
            }
            return builder.toString()
        }

        fun List<LagrangeCoordinate>.showLagrange(): String {
            val builder = StringBuilder(5000)
            builder.appendLine(" LagrangeCoordinates")
            this.sortedBy { it.guardianId }.forEach { lagrange ->
                builder.appendLine("  ${lagrange.guardianId} xcoord=${lagrange.xCoordinate} coefficient=${lagrange.lagrangeCoefficient}")
            }
            return builder.toString()
        }
    }
}