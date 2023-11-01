package electionguard.protoconvert

import electionguard.ballot.*
import electionguard.core.GroupContext
import electionguard.util.ErrorMessages

fun electionguard.protogen.TallyResult.import(group: GroupContext, errs : ErrorMessages): TallyResult? {

    val electionInitialized = this.electionInit?.import(group, errs.nested("ElectionInit"))
    val encryptedTally = if (this.encryptedTally == null) {
        errs.add("missing EncryptedTally")
        null
    } else {
        this.encryptedTally.import(group, errs.nested("EncryptedTally ${this.encryptedTally.tallyId}"))
    }

    return if (errs.hasErrors()) null
    else TallyResult(
            electionInitialized!!,
            encryptedTally!!,
            this.tallyIds,
            this.metadata.associate { it.key to it.value }
        )
}

fun electionguard.protogen.DecryptionResult.import(group: GroupContext, errs : ErrorMessages): DecryptionResult? {

    val tallyResult = this.tallyResult?.import(group, errs.nested("tallyResult"))
        ?: errs.addNull("missing tallyResult") as TallyResult?

    val decryptedTally = this.decryptedTally?.import(group, errs.nested("decryptedTally"))
        ?: errs.addNull("missing decryptedTally") as DecryptedTallyOrBallot?

    return if (errs.hasErrors()) null
    else DecryptionResult(
        tallyResult!!,
        decryptedTally!!,
        this.metadata.associate { it.key to it.value }
    )
}