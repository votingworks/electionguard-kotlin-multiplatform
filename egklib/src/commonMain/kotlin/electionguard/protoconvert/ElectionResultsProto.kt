package electionguard.protoconvert

import com.github.michaelbull.result.*
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
        this.tallyIds,
        this.metadata.associate { it.key to it.value }
    ))
}

fun electionguard.protogen.DecryptionResult.import(group: GroupContext): Result<DecryptionResult, String> {

    val tallyResult = this.tallyResult?.import(group) ?: Err("Null TallyResult")
    val decryptedTally = this.decryptedTally?.import(group) ?: Err("Null DecryptedTally")

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