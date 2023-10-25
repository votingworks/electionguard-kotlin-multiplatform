package electionguard.publish

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrapError
import electionguard.ballot.*
import electionguard.core.ElementModP
import electionguard.core.GroupContext
import electionguard.core.UInt256


fun readElectionRecord(group : GroupContext, topDir: String) : ElectionRecord {
    val consumerIn = makeConsumer(group, topDir)
    return readElectionRecord(consumerIn)
}

// there must at least be a config record
fun readElectionRecord(consumer: Consumer) : ElectionRecord {
    var decryptionResult : DecryptionResult? = null
    var tallyResult : TallyResult? = null
    var init : ElectionInitialized? = null
    val config : ElectionConfig
    var stage : ElectionRecord.Stage

    val readDecryptionResult = consumer.readDecryptionResult()
    if (readDecryptionResult is Ok) {
        decryptionResult = readDecryptionResult.value
        tallyResult = decryptionResult.tallyResult
        init = tallyResult.electionInitialized
        config = init.config
        stage = ElectionRecord.Stage.DECRYPTED
    } else {
        if (!readDecryptionResult.unwrapError().contains("file does not exist")) {
            throw RuntimeException(readDecryptionResult.unwrapError())
        }
        val readTallyResult = consumer.readTallyResult()
        if (readTallyResult is Ok) {
            tallyResult = readTallyResult.value
            init = tallyResult.electionInitialized
            config = init.config
            stage = ElectionRecord.Stage.TALLIED
        } else {
            if (!readTallyResult.unwrapError().contains("file does not exist")) {
                throw RuntimeException(readTallyResult.unwrapError())
            }
            val readInitResult = consumer.readElectionInitialized()
            if (readInitResult is Ok) {
                init = readInitResult.value
                config = init.config
                stage = ElectionRecord.Stage.INIT
            } else {
                if (!readInitResult.unwrapError().contains("file does not exist")) {
                    throw RuntimeException(readInitResult.unwrapError())
                }
                val readConfigResult = consumer.readElectionConfig()
                if (readConfigResult is Ok) {
                    config = readConfigResult.value
                    stage = ElectionRecord.Stage.CONFIG
                } else {
                    throw RuntimeException(readConfigResult.unwrapError())
                }
            }
        }
    }

    // Always has to be a config and the original manifest bytes, from which the manifest is parsed
    require(config != null) { "no election config file found in ${consumer.topdir()}" }
    require(config.manifestHash == manifestHash(config.parameterBaseHash, config.manifestBytes)) {
        "config.manifestHash fails to match ${consumer.topdir()}"
    }
    val manifest : Manifest = consumer.makeManifest(config.manifestBytes)

    if (stage == ElectionRecord.Stage.INIT && consumer.hasEncryptedBallots()) {
        stage = ElectionRecord.Stage.ENCRYPTED
    }
    return ElectionRecordImpl(consumer, stage, decryptionResult, tallyResult, init, config, manifest)
}

private class ElectionRecordImpl(val consumer: Consumer,
                                 val stage: ElectionRecord.Stage,
                                 val decryptionResult : DecryptionResult?,
                                 val tallyResult : TallyResult?,
                                 val init : ElectionInitialized?,
                                 val config : ElectionConfig,
                                 val manifest: Manifest
) : ElectionRecord {

    override fun stage(): ElectionRecord.Stage {
        return stage
    }

    override fun topdir(): String {
        return consumer.topdir()
    }

    override fun isJson(): Boolean {
        return consumer.isJson()
    }

    override fun constants(): ElectionConstants {
        return config.constants
    }

    override fun manifest(): Manifest {
        return manifest
    }

    override fun manifestBytes(): ByteArray {
        return config.manifestBytes
    }

    override fun numberOfGuardians(): Int {
        return config.numberOfGuardians
    }

    override fun quorum(): Int {
        return config.quorum
    }

    override fun config(): ElectionConfig {
        return config
    }

    override fun parameterBaseHash(): UInt256 {
        return config.parameterBaseHash
    }

    override fun electionBaseHash(): UInt256 {
        return config.electionBaseHash
    }

    override fun extendedBaseHash(): UInt256? {
        return init?.extendedBaseHash
    }

    override fun jointPublicKey(): ElementModP? {
        return init?.jointPublicKey
    }

    override fun guardians(): List<Guardian> {
        return init?.guardians ?: emptyList()
    }

    override fun electionInit(): ElectionInitialized? {
        return init
    }

    override fun encryptingDevices(): List<String> {
        return consumer.encryptingDevices()
    }

    override fun readEncryptedBallotChain(device: String) : Result<EncryptedBallotChain, String> {
        return consumer.readEncryptedBallotChain(device)
    }

    override fun encryptedBallots(device: String, filter: ((EncryptedBallot) -> Boolean)?): Iterable<EncryptedBallot> {
        return consumer.iterateEncryptedBallots(device, filter)
    }

    override fun encryptedAllBallots(filter : ((EncryptedBallot) -> Boolean)?): Iterable<EncryptedBallot> {
        return consumer.iterateAllEncryptedBallots(filter)
    }

    override fun encryptedTally(): EncryptedTally? {
        return tallyResult?.encryptedTally
    }

    override fun tallyResult(): TallyResult? {
        return tallyResult
    }

    override fun decryptedTally(): DecryptedTallyOrBallot? {
        return decryptionResult?.decryptedTally
    }

    override fun decryptedBallots(): Iterable<DecryptedTallyOrBallot> {
        return consumer.iterateDecryptedBallots()
    }

    override fun decryptionResult(): DecryptionResult? {
        return decryptionResult
    }
}