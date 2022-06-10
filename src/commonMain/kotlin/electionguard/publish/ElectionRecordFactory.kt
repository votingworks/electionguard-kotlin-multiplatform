package electionguard.publish

import com.github.michaelbull.result.Ok
import electionguard.ballot.DecryptingGuardian
import electionguard.ballot.DecryptionResult
import electionguard.ballot.ElectionConfig
import electionguard.ballot.ElectionConstants
import electionguard.ballot.ElectionInitialized
import electionguard.ballot.EncryptedBallot
import electionguard.ballot.EncryptedTally
import electionguard.ballot.Guardian
import electionguard.ballot.Manifest
import electionguard.ballot.PlaintextTally
import electionguard.ballot.TallyResult
import electionguard.core.ElementModP
import electionguard.core.UInt256

fun electionRecordFromConsumer(consumer: Consumer) : ElectionRecord {
    var decryptionResult : DecryptionResult? = null
    var tallyResult : TallyResult? = null
    var init : ElectionInitialized? = null
    var config : ElectionConfig? = null
    var stage : ElectionRecord.Stage? = null

    val decryption = consumer.readDecryptionResult()
    if (decryption is Ok) {
        decryptionResult = decryption.value
        tallyResult = decryptionResult.tallyResult
        init = tallyResult.electionInitialized
        config = init.config
        stage = ElectionRecord.Stage.DECRYPTED
    } else {
        val tally = consumer.readTallyResult()
        if (tally is Ok) {
            decryptionResult = null
            tallyResult = tally.value
            init = tallyResult.electionInitialized
            config = init.config
            stage = ElectionRecord.Stage.TALLIED
        } else {
            val initResult = consumer.readElectionInitialized()
            if (initResult is Ok) {
                decryptionResult = null
                tallyResult = null
                init = initResult.value
                config = init.config
                stage = ElectionRecord.Stage.INIT
            } else {
                val configResult = consumer.readElectionConfig()
                if (configResult is Ok) {
                    decryptionResult = null
                    tallyResult = null
                    init = null
                    config = configResult.value
                    stage = ElectionRecord.Stage.CONFIG
                }
            }
        }
    }
    if (stage == ElectionRecord.Stage.INIT && consumer.hasEncryptedBallots()) {
        stage = ElectionRecord.Stage.ENCRYPTED
    }
    return ElectionRecordImpl(consumer, stage!!, decryptionResult, tallyResult, init, config!!)
}

private class ElectionRecordImpl(val consumer: Consumer, val stage: ElectionRecord.Stage,
                                 val decryptionResult : DecryptionResult?,
                                 val tallyResult : TallyResult?,
                                 val init : ElectionInitialized?,
                                 val config : ElectionConfig) : ElectionRecord {

    override fun stage(): ElectionRecord.Stage {
        return stage
    }

    override fun topdir(): String {
        return consumer.topdir()
    }

    override fun protoVersion(): String {
        return config.protoVersion
    }

    override fun constants(): ElectionConstants {
        return config.constants
    }

    override fun manifest(): Manifest {
        return config.manifest
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

    override fun cryptoExtendedBaseHash(): UInt256? {
        return init?.cryptoExtendedBaseHash
    }

    override fun cryptoBaseHash(): UInt256? {
        return init?.cryptoBaseHash
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

    override fun encryptedBallots(filter : ((EncryptedBallot) -> Boolean)?): Iterable<EncryptedBallot> {
        return consumer.iterateEncryptedBallots(filter)
    }

    override fun encryptedTally(): EncryptedTally? {
        return tallyResult?.encryptedTally
    }

    override fun decryptedTally(): PlaintextTally? {
        return decryptionResult?.decryptedTally
    }

    override fun decryptingGuardians(): List<DecryptingGuardian> {
        return decryptionResult?.decryptingGuardians ?: emptyList()
    }

    override fun spoiledBallotTallies(): Iterable<PlaintextTally> {
        return consumer.iterateSpoiledBallotTallies()
    }
}