package electionguard.protoconvert

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.ballot.*
import pbandk.ByteArr

fun importElectionConfig(config: electionguard.protogen.ElectionConfig?): Result<ElectionConfig, String> {
    if (config == null) {
        return Err("Null ElectionConfig")
    }
    val electionConstants = convertConstants(config.constants)
    val manifest = importManifest(config.manifest)

    if (electionConstants is Err || manifest is Err) {
        return Err("Missing fields in ElectionConfig")
    }

    return Ok(ElectionConfig(
        config.protoVersion,
        electionConstants.unwrap(),
        manifest.unwrap(),
        config.numberOfGuardians,
        config.quorum,
        config.metadata.associate {it.key to it.value}
    ))
}

private fun convertConstants(
    constants: electionguard.protogen.Constants?
): Result<Constants, String> {
    if (constants == null) {
        return Err("Null Constants")
    }
    return Ok(Constants(
        constants.name,
        constants.largePrime.array,
        constants.smallPrime.array,
        constants.cofactor.array,
        constants.generator.array,
    ))
}

////////////////////////////////////////////////////////

fun ElectionConfig.publishElectionConfig(): electionguard.protogen.ElectionConfig {
    return electionguard.protogen.ElectionConfig(
        protoVersion,
        constants.publishConstants(),
        manifest.publishManifest(),
        this.numberOfGuardians,
        this.quorum,
        this.metadata.entries.map { electionguard.protogen.ElectionConfig.MetadataEntry(it.key, it.value)}
    )
}

private fun Constants.publishConstants(): electionguard.protogen.Constants {
    return electionguard.protogen
        .Constants(
            this.name,
            ByteArr(this.largePrime),
            ByteArr(this.smallPrime),
            ByteArr(this.cofactor),
            ByteArr(this.generator),
        )
}