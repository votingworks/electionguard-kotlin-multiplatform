package electionguard.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.ballot.Guardian
import electionguard.core.GroupContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("Guardian")
data class GuardianJson(
    val guardian_id: String,
    val sequence_order: Int,
    val election_public_key: ElementModPJson,
    val election_commitments: List<ElementModPJson>,
    val election_proofs: List<SchnorrProofJson>,
)

fun Guardian.publish() = GuardianJson(
    this.guardianId,
    this.xCoordinate,
    this.publicKey().publish(),
    this.coefficientCommitments().map { it.publish() },
    this.coefficientProofs.map { it.publish() }
)

fun GuardianJson.import(group: GroupContext): Result<Guardian, String> {
    val proofs = this.election_proofs.map { it.import(group) }
    val allgood = proofs.map { it != null }.reduce { a, b -> a && b }

    return if (!allgood) {
        Err("Guardian ${this.guardian_id} import proofs failed")
    } else
        Ok(Guardian(
            this.guardian_id,
            this.sequence_order,
            proofs.map { it!! }
        ))
}