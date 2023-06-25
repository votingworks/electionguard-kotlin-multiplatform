package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.partition
import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import electionguard.core.GroupContext

fun electionguard.protogen.TallyResult.import(group: GroupContext): Result<TallyResult, String> {

    val electionInitialized: Result<ElectionInitialized, String> = this.electionInit?.import(group) ?: Err("Null ElectionInitialized")
    val encryptedTally: Result<EncryptedTally, String> = this.encryptedTally?.import(group) ?: return Err("Null EncryptedTally")

    val errors = getAllErrors(electionInitialized, encryptedTally)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(TallyResult(
        electionInitialized.unwrap(),
        encryptedTally.unwrap(),
        this.ballotIds,
        this.tallyIds,
        this.metadata.associate { it.key to it.value }
    ))
}

fun electionguard.protogen.DecryptionResult.import(group: GroupContext): Result<DecryptionResult, String> {

    val tallyResult = this.tallyResult?.import(group) ?: Err("Null TallyResult")
    val decryptedTally = this.decryptedTally?.import(group) ?: Err("Null DecryptedTally")

    // val (guardians, gerrors) = this.lagrangeCoordinates.map { it.import(group) }.partition()

    val errors = getAllErrors(tallyResult, decryptedTally)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(DecryptionResult(
        tallyResult.unwrap(),
        decryptedTally.unwrap(),
        this.metadata.associate { it.key to it.value }
    ))
}

////////////////////////////////////////////////////////

fun TallyResult.publishProto() =
    electionguard.protogen.TallyResult(
        this.electionInitialized.publishProto(),
        this.encryptedTally.publishProto(),
        this.ballotIds,
        this.tallyIds,
        this.metadata.entries.map { electionguard.protogen.TallyResult.MetadataEntry(it.key, it.value) }
    )

fun DecryptionResult.publishProto() =
    electionguard.protogen.DecryptionResult(
        this.tallyResult.publishProto(),
        this.decryptedTally.publishProto(),
        this.metadata.entries.map { electionguard.protogen.DecryptionResult.MetadataEntry(it.key, it.value) }
    )