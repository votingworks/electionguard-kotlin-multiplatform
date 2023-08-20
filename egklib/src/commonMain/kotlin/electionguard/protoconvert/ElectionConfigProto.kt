package electionguard.protoconvert

import com.github.michaelbull.result.*
import electionguard.ballot.*
import pbandk.ByteArr

fun electionguard.protogen.ElectionConfig.import(): Result<ElectionConfig, String> {
    val electionConstants = this.constants?.import() ?: Err("Null ElectionConstants")
    val parameterHash = this.parameterBaseHash?.import() ?: Err("Null parameterBaseHash")
    val manifestHash = manifestHash?.import() ?: Err("Null manifestHash")
    val electionHash = this.electionBaseHash?.import() ?: Err("Null electionBaseHash")

    val errors = getAllErrors(electionConstants, parameterHash, manifestHash, electionHash)
    if (errors.isNotEmpty()) {
        return Err(errors.joinToString("\n"))
    }

    return Ok(ElectionConfig(
        this.specVersion,
        electionConstants.unwrap(),
        this.numberOfGuardians,
        this.quorum,
        parameterHash.unwrap(),
        manifestHash.unwrap(),
        electionHash.unwrap(),
        this.manifestBytes.array,
        this.chainConfirmationCodes,
        this.configBaux0.array,
        this.metadata.associate { it.key to it.value },
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
        protocolVersion,
        constants.publishProto(),
        this.numberOfGuardians,
        this.quorum,
        this.parameterBaseHash.publishProto(),
        this.manifestHash.publishProto(),
        this.electionBaseHash.publishProto(),
        ByteArr(this.manifestBytes),
        this.chainConfirmationCodes,
        ByteArr(this.configBaux0),
        this.metadata.entries.map { electionguard.protogen.ElectionConfig.MetadataEntry(it.key, it.value) },
    )

private fun ElectionConstants.publishProto() =
    electionguard.protogen.ElectionConstants(
        this.name,
        ByteArr(this.largePrime),
        ByteArr(this.smallPrime),
        ByteArr(this.cofactor),
        ByteArr(this.generator),
    )