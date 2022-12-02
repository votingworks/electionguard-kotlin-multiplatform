package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.getAllErrors
import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import pbandk.ByteArr

fun electionguard.protogen.ElectionConfig.import(): Result<ElectionConfig, String> {
    val electionConstants = this.constants?.import() ?: Err("Null ElectionConstants")
    val manifest = this.manifest?.import() ?: Err("Null Manifest")

    val errors = getAllErrors(electionConstants, manifest)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(ElectionConfig(
        electionConstants.unwrap(),
        manifest.unwrap(),
        this.numberOfGuardians,
        this.quorum,
        this.metadata.associate { it.key to it.value }
    ))
}

private fun electionguard.protogen.ElectionConstants.import(): Result<ElectionConstants, String> {
    return Ok(
        ElectionConstants(
            this.name,
            this.largePrime.array,
            this.smallPrime.array,
            this.cofactor.array,
            this.generator.array,
        )
    )
}

////////////////////////////////////////////////////////

fun ElectionConfig.publishProto() =
    electionguard.protogen.ElectionConfig(
        constants.publishProto(),
        manifest.publishProto(),
        this.numberOfGuardians,
        this.quorum,
        this.metadata.entries.map { electionguard.protogen.ElectionConfig.MetadataEntry(it.key, it.value) }
    )

private fun ElectionConstants.publishProto() =
    electionguard.protogen.ElectionConstants(
        this.name,
        ByteArr(this.largePrime),
        ByteArr(this.smallPrime),
        ByteArr(this.cofactor),
        ByteArr(this.generator),
    )