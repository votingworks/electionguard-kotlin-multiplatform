@file:OptIn(ExperimentalSerializationApi::class, ExperimentalSerializationApi::class)

package electionguard.publish

import com.github.michaelbull.result.*
import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.decrypt.DecryptingTrustee
import electionguard.decrypt.DecryptingTrusteeIF
import electionguard.json.CoefficientsJson
import electionguard.json.ConstantsJson
import electionguard.json.ContextJson
import electionguard.json.DecryptedTallyJson
import electionguard.json.DecryptingTrusteeJson
import electionguard.json.ElectionManifestJson
import electionguard.json.EncryptedTallyJson
import electionguard.json.GuardianJson
import electionguard.json.PlaintextBallotJson
import electionguard.json.SubmittedBallotJson
import electionguard.json.import
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import mu.KotlinLogging
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Predicate

private val logger = KotlinLogging.logger("ConsumerJson")

actual class ConsumerJson actual constructor(val topDir: String, val group: GroupContext): Consumer {
    private val jsonPaths = ElectionRecordJsonPaths(topDir)

    init {
        if (!Files.exists(Path.of(topDir))) {
            throw RuntimeException("Not existent directory $topDir")
        }
    }

    actual override fun topdir(): String {
        return this.topDir
    }

    actual override fun readElectionConfig(): Result<ElectionConfig, String> {
        return readElectionConfig(jsonPaths.electionConstantsPath(), jsonPaths.manifestPath())
    }

    actual override fun readElectionInitialized(): Result<ElectionInitialized, String> {
        val config = readElectionConfig()
        if (config is Err) {
            return Err(config.error)
        }
        return readElectionInitialized(jsonPaths.electionContextPath(), config.unwrap())
    }

    actual override fun readTallyResult(): Result<TallyResult, String> {
        val init = readElectionInitialized()
        if (init is Err) {
            return Err(init.error)
        }
        return readTallyResult(jsonPaths.encryptedTallyPath(), init.unwrap())
    }

    actual override fun readDecryptionResult(): Result<DecryptionResult, String> {
        val tally = readTallyResult()
        if (tally is Err) {
            return Err(tally.error)
        }

        return readDecryptionResult(jsonPaths.decryptedTallyPath(), tally.unwrap())
    }

    // all submitted ballots, with filter
    actual override fun iterateEncryptedBallots(
        filter: ((EncryptedBallot) -> Boolean)?
    ): Iterable<EncryptedBallot> {
        val dirname = jsonPaths.encryptedBallotDir()
        if (!Files.exists(Path.of(dirname))) {
            return emptyList()
        }
        return Iterable { EncryptedBallotIterator(dirname, group, filter) }
    }

    actual override fun hasEncryptedBallots(): Boolean {
        return Files.exists(Path.of(jsonPaths.encryptedBallotDir()))
    }

    // only EncryptedBallot that are CAST
    actual override fun iterateCastBallots(): Iterable<EncryptedBallot> {
        val dirname = jsonPaths.encryptedBallotDir()
        if (!Files.exists(Path.of(dirname))) {
            return emptyList()
        }
        val filter = Predicate<EncryptedBallot> { it.state == EncryptedBallot.BallotState.CAST }
        return Iterable { EncryptedBallotIterator(dirname, group, filter) }
    }

    // only EncryptedBallot that are SPOILED
    actual override fun iterateSpoiledBallots(): Iterable<EncryptedBallot> {
        val dirname = jsonPaths.encryptedBallotDir()
        if (!Files.exists(Path.of(dirname))) {
            return emptyList()
        }
        val filter = Predicate<EncryptedBallot> { it.state == EncryptedBallot.BallotState.SPOILED }
        return Iterable { EncryptedBallotIterator(dirname, group, filter) }
    }

    // decrypted spoiled ballots
    actual override fun iterateDecryptedBallots(): Iterable<DecryptedTallyOrBallot> {
        val dirname = jsonPaths.decryptedBallotDir()
        if (!Files.exists(Path.of(dirname))) {
            return emptyList()
        }
        return Iterable { SpoiledBallotTallyIterator(dirname, group) }
    }

    // plaintext ballots in given directory, with filter
    actual override fun iteratePlaintextBallots(
        ballotDir: String,
        filter: ((PlaintextBallot) -> Boolean)?
    ): Iterable<PlaintextBallot> {
        if (!Files.exists(Path.of(ballotDir))) {
            return emptyList()
        }
        return Iterable { PlaintextBallotIterator(ballotDir, filter) }
    }

    // trustee in given directory for given guardianId
    actual override fun readTrustee(trusteeDir: String, guardianId: String): DecryptingTrusteeIF {
        val filename = jsonPaths.decryptingTrusteePath(trusteeDir, guardianId)
        return readTrustee(filename).unwrap()
    }

    //////// The low level reading functions for protobuf

    private fun readElectionConfig(constantsFile: String, manifestFile: String): Result<ElectionConfig, String> {
        return try {
            var constants: ElectionConstants
            FileInputStream(constantsFile).use { inp ->
                val json = Json.decodeFromStream<ConstantsJson>(inp)
                constants = json.import()
            }

            var manifest: Manifest
            FileInputStream(manifestFile).use { inp ->
                val json = Json.decodeFromStream<ElectionManifestJson>(inp)
                manifest = json.import()
            }

            Ok(ElectionConfig(
                constants,
                manifest,
                1, // LOOK we dont know
                1, // LOOK we dont know
            ))
        } catch (e: Exception) {
            Err(e.message ?: "readElectionConfig $constantsFile failed")
        }
    }

    private fun readElectionInitialized(contextFile: String, config: ElectionConfig): Result<ElectionInitialized, String> {
        return try {
            val (guardians, errors) = readGuardians().partition()
            if (errors.isNotEmpty()) {
                return Err(errors.joinToString("\n"))
            } else {
                var context: ElectionInitialized
                FileInputStream(contextFile).use { inp ->
                    val json = Json.decodeFromStream<ContextJson>(inp)
                    context = json.import(group, config, guardians)
                }
                Ok(context)
            }
        } catch (e: Exception) {
            Err(e.message ?: "readElectionInitialized $contextFile failed")
        }
    }

    private fun readGuardians(): List<Result<Guardian, String>> {
        val dir = File(jsonPaths.guardianDir())
        return dir.listFiles().map {
            FileInputStream(it).use { inp ->
                val json = Json.decodeFromStream<GuardianJson>(inp)
                json.import(group)
            }
        }
    }

    private fun readTallyResult(filename: String, init: ElectionInitialized): Result<TallyResult, String> {
        return try {
            FileInputStream(filename).use { inp ->
                val json = Json.decodeFromStream<EncryptedTallyJson>(inp)
                val tallyResult = json.import(group)
                if (tallyResult is Err)
                    Err(tallyResult.unwrapError())
                else
                    Ok(TallyResult(init, tallyResult.unwrap(), emptyList(), emptyList()))
            }
        } catch (e: Exception) {
            Err(e.message ?: "readTallyResult $filename failed")
        }
    }

    private fun readDecryptionResult(filename: String, tallyResult: TallyResult): Result<DecryptionResult, String> {
        // all the coefficients in a map in one file
        var lagrangeCoordinates : List<LagrangeCoordinate>
        FileInputStream(jsonPaths.lagrangePath()).use { inp ->
            val json = Json.decodeFromStream<CoefficientsJson>(inp)
            lagrangeCoordinates = json.import(group)
        }

        return try {
            FileInputStream(filename).use { inp ->
                val json = Json.decodeFromStream<DecryptedTallyJson>(inp)
                val dtallyResult = json.import(group)
                if (dtallyResult is Err)
                    Err(dtallyResult.unwrapError())
                else
                    Ok(DecryptionResult(tallyResult, dtallyResult.unwrap(), lagrangeCoordinates))
            }
        } catch (e: Exception) {
            Err(e.message ?: "readDecryptionResult $filename failed")
        }
    }

    private class PlaintextBallotIterator(
        dirname: String,
        private val filter: Predicate<PlaintextBallot>?
    ) : AbstractIterator<PlaintextBallot>() {
        val fileList = File(dirname).listFiles() ?: throw RuntimeException()
        var idx = 0

        override fun computeNext() {
            while (idx < fileList.size) {
                val file = fileList[idx++]
                FileInputStream(file).use { inp ->
                    val json = Json.decodeFromStream<PlaintextBallotJson>(inp)
                    val ballotResult = json.import()
                    if (ballotResult is Err) {
                        logger.atWarn().log { "Failed to open ${file} error = ${ballotResult.unwrapError()}" }
                    } else {
                        val ballot = ballotResult.unwrap()
                        if (filter == null || filter.test(ballot)) {
                            setNext(ballot)
                            return
                        }
                    }
                }
            }
            return done()
        }
    }

    private class EncryptedBallotIterator(
        dirname: String,
        private val group: GroupContext,
        private val filter: Predicate<EncryptedBallot>?,
    ) : AbstractIterator<EncryptedBallot>() {
        val fileList = File(dirname).listFiles() ?: throw RuntimeException()
        var idx = 0

        override fun computeNext() {
            while (idx < fileList.size) {
                val file = fileList[idx++]
                FileInputStream(file).use { inp ->
                    val json = Json.decodeFromStream<SubmittedBallotJson>(inp)
                    val ballotResult = json.import(group)
                    if (ballotResult is Err) {
                        logger.atWarn().log { "Failed to open ${file} error = ${ballotResult.unwrapError()}" }
                    } else {
                        val ballot = ballotResult.unwrap()
                        if (filter == null || filter.test(ballot)) {
                            setNext(ballot)
                            return
                        }
                    }
                }
            }
            return done()
        }
    }

    private class SpoiledBallotTallyIterator(
        dirname: String,
        private val group: GroupContext,
    ) : AbstractIterator<DecryptedTallyOrBallot>() {
        val fileList = File(dirname).listFiles() ?: throw RuntimeException()
        var idx = 0

        override fun computeNext() {
            while (idx < fileList.size) {
                val file = fileList[idx++]
                FileInputStream(file).use { inp ->
                    val json = Json.decodeFromStream<DecryptedTallyJson>(inp)
                    val tallyResult = json.import(group)
                    if (tallyResult is Err) {
                        logger.atWarn().log { "Failed to open ${file} error = ${tallyResult.unwrapError()}" }
                    } else {
                        setNext(tallyResult.unwrap())
                        return
                    }
                }
            }
            return done()
        }
    }

    private fun readTrustee(filename: String): Result<DecryptingTrustee, String> {
        return try {
            FileInputStream(filename).use { inp ->
                val json = Json.decodeFromStream<DecryptingTrusteeJson>(inp)
                val trusteeResult = json.import(group)
                if (trusteeResult is Err)
                    Err(trusteeResult.unwrapError())
                else
                    Ok(trusteeResult.unwrap())
            }
        } catch (e: Exception) {
            Err(e.message ?: "readDecryptionResult $filename failed")
        }
    }
}