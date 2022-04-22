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

    if (electionInitialized is Err || ciphertextTally == null) {
        return Err("Failed to translate election record from proto, missing fields")
    }

    return Ok(TallyResult(
        electionInitialized.unwrap(),
        ciphertextTally.unwrap(),
        tally.metadata.associate {it.key to it.value}
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

private fun importDecryptingGuardian(guardian: electionguard.protogen.DecryptingGuardian): DecryptingGuardian {
    return DecryptingGuardian(
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
        this.metadata.entries.map { electionguard.protogen.TallyResult.MetadataEntry(it.key, it.value)}
    )
}

fun DecryptionResult.publishDecryptionResult(): electionguard.protogen.DecryptionResult {
    return electionguard.protogen.DecryptionResult(
        this.tallyResult.publishTallyResult(),
        this.decryptedTally.publishPlaintextTally(),
        this.decryptingGuardians.map { it.publishDecryptingGuardian() },
        this.metadata.entries.map { electionguard.protogen.DecryptionResult.MetadataEntry(it.key, it.value)}
    )
}

private fun DecryptingGuardian.publishDecryptingGuardian(): electionguard.protogen.DecryptingGuardian {
    return electionguard.protogen.DecryptingGuardian(
        this.guardianId,
        this.xCoordinate,
        this.lagrangeCoordinate,
    )
}