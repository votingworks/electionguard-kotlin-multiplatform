package electionguard.show

import electionguard.ballot.DecryptedTallyOrBallot
import electionguard.ballot.ElectionConstants
import electionguard.ballot.Guardian
import electionguard.ballot.LagrangeCoordinate
import electionguard.ballot.Manifest
import electionguard.core.Base16.toHex
import electionguard.core.GroupContext
import electionguard.core.SchnorrProof
import electionguard.core.productionGroup
import electionguard.publish.ElectionRecord
import electionguard.publish.electionRecordFromConsumer
import electionguard.publish.makeConsumer
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.required

/**
 * Show the election record in inputDir.
 */
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
        description = "[constants,manifest,guardians,lagrange]"
    )
    val details by parser.option(
        ArgType.Boolean,
        shortName = "details",
        description = "show details"
    )
    parser.parse(args)
    val showSet = if (show == null) emptySet() else show!!.split(",").toSet()

    runElectionRecordShow(productionGroup(), inputDir, showSet, details ?: false)
}

fun runElectionRecordShow(group: GroupContext, inputDir: String, showSet: Set<String>, details : Boolean) {
    val consumer = makeConsumer(inputDir, group)
    val electionRecord = electionRecordFromConsumer(consumer)
    println("RunElectionRecord from $inputDir, stage = ${electionRecord.stage()}\n")

    val config = electionRecord.config()
    println(" config numberOfGuardians = ${config.numberOfGuardians}")
    println(" config quorum  = ${config.quorum}")
    println(" config metadata  = ${config.metadata}")
    if (showSet.contains("constants")) {
        print(" ${config.constants.show()}")

    }
    if (showSet.contains("manifest")) {
        print(" ${config.manifest.show(details)}")
    }
    println()

    if (electionRecord.stage() == ElectionRecord.Stage.CONFIG) {
        return
    }

    val init = electionRecord.electionInit()
    if (init != null) {
        println(" init numberOfGuardians = ${init.config.numberOfGuardians}")
        println(" init quorum  = ${init.config.quorum}")
        println(" init nguardians = ${init.guardians.size}")
        println(" init metadata  = ${init.metadata}")
        if (showSet.contains("guardians")) {
            print(init.guardians.showGuardians(details))
        }
        println()
    }

    if (electionRecord.stage() == ElectionRecord.Stage.INIT) {
        return
    }

    val ecount = consumer.iterateEncryptedBallots{ true}.count()
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
        println(" decryptedTally available=${dtally.lagrangeCoordinates.size}")
        println(" decryptedTally ${dtally.decryptedTally.show(details, config.manifest)}")
        if (showSet.contains("lagrange")) {
            print(dtally.lagrangeCoordinates.showLagrange())
        }
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

fun Manifest.show(details : Boolean): String {
    val builder = StringBuilder(5000)
    builder.appendLine("Manifest scopeId=${this.electionScopeId} type=${this.electionType} spec=${this.specVersion}")
    builder.appendLine("  gpus ${this.geopoliticalUnits}")
    builder.appendLine("  styles ${this.ballotStyles}")
    builder.append(this.contests.showContests(details))
    return builder.toString()
}

fun List<Manifest.ContestDescription>.showContests(details : Boolean): String {
    val builder = StringBuilder(5000)
    this.sortedBy { it.contestId }.forEach { contest ->
        if (details) {
            builder.appendLine("  Contest ${contest.contestId}")
            contest.selections.sortedBy { it.selectionId }.forEach {
                builder.appendLine("   ${it.selectionId} ${it.sequenceOrder} ${it.candidateId}")
            }
            builder.appendLine()
        } else {
            builder.append("  ${contest.contestId}")
            contest.selections.sortedBy { it.selectionId }.forEach {
                builder.append("   ${it.selectionId} (${it.candidateId})")
            }
            builder.appendLine()
        }
    }
    return builder.toString()
}

fun List<Guardian>.showGuardians(details : Boolean): String {
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

fun SchnorrProof.show(): String {
    val builder = StringBuilder(5000)
    builder.append("SchnorrProof key=${this.publicKey.toStringShort()}")
    builder.append(" challenge=${this.challenge}")
    builder.append(" response=${this.response}")
    return builder.toString()
}

fun DecryptedTallyOrBallot.show(details : Boolean, manifest: Manifest): String {
    val builder = StringBuilder(5000)
    builder.appendLine(" DecryptedTallyOrBallot $id")
    contests.sortedBy { it.contestId }.forEach { contest ->
        if (details) {
            builder.appendLine("  Contest ${contest.contestId}")
            contest.selections.sortedBy { -it.tally }.forEach {
                val candidate = manifest.selectionCandidate["${contest.contestId}/${it.selectionId}"] ?: "unknown"
                builder.appendLine("   $candidate (${it.selectionId}) = ${it.tally}")
            }
        } else {
            builder.append("  ${contest.contestId}")
            contest.selections.sortedBy { -it.tally }.forEach {
                val candidate = manifest.selectionCandidate["${contest.contestId}/${it.selectionId}"] ?: "unknown"
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
