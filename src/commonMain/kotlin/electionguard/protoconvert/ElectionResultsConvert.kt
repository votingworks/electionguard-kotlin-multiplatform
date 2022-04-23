package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.core.GroupContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger("ElectionConfigConvert")

fun GroupContext.importTallyResult(tally : electionguard.protogen.TallyResult?):
        Result<TallyResult, String> {

    if (tally == null) {
        return Err("Null TallyResult")
    }
    val electionInitialized = this.importElectionInitialized(tally.electionInit)
    val ciphertextTally = this.importCiphertextTally(tally.ciphertextTally)

    if (electionInitialized is Err || ciphertextTally is Err) {
        return Err("Failed to translate election record from proto, missing fields")
    }

    return Ok(TallyResult(
        this,
        electionInitialized.unwrap(),
        ciphertextTally.unwrap(),
        tally.ballotIds,
        tally.tallyIds,
    ))
}

fun GroupContext.importDecryptionResult(decrypt : electionguard.protogen.DecryptionResult):
        Result<DecryptionResult, String>  {
    val electionInitialized = this.importTallyResult(decrypt.tallyResult)
    val plaintextTally = this.importPlaintextTally(decrypt.decryptedTally)
    val guardians =
        decrypt.decryptingGuardians.map { importDecryptingGuardian(it) }

    if (electionInitialized is Err || plaintextTally is Err) {
        logger.error { "Failed to translate election record from proto, missing fields" }
        throw IllegalStateException(
            "Failed to translate election record from proto, missing fields"
        )
    }

    return Ok(DecryptionResult(
        electionInitialized.unwrap(),
        plaintextTally.unwrap(),
        guardians,
        decrypt.metadata.associate {it.key to it.value}
    ))
}

private fun importDecryptingGuardian(guardian: electionguard.protogen.AvailableGuardian): AvailableGuardian {
    return AvailableGuardian(
        guardian.guardianId,
        guardian.xCoordinate,
        guardian.lagrangeCoordinate,
    )
}

////////////////////////////////////////////////////////

fun TallyResult.publishTallyResult(): electionguard.protogen.TallyResult {
    return electionguard.protogen.TallyResult(
        this.electionIntialized.publishElectionInitialized(),
        this.ciphertextTally.publishCiphertextTally(),
        this.ballotIds,
        this.tallyIds,
    )
}

fun DecryptionResult.publishDecryptionResult(): electionguard.protogen.DecryptionResult {
    return electionguard.protogen.DecryptionResult(
        this.tallyResult.publishTallyResult(),
        this.decryptedTally.publishPlaintextTally(),
        this.availableGuardians.map { it.publishDecryptingGuardian() },
        this.metadata.entries.map { electionguard.protogen.DecryptionResult.MetadataEntry(it.key, it.value)}
    )
}

private fun AvailableGuardian.publishDecryptingGuardian(): electionguard.protogen.AvailableGuardian {
    return electionguard.protogen.AvailableGuardian(
        this.guardianId,
        this.xCoordinate,
        this.lagrangeCoordinate,
    )
}