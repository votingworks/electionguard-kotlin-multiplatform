package electionguard.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import electionguard.core.productionGroup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.Test
import java.nio.file.FileSystems
import java.nio.file.Path

class ElectionParametersTest {
    var fileSystem = FileSystems.getDefault()
    var fileSystemProvider = fileSystem.provider()
    val group = productionGroup()
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true }

    @Test
    fun test() {
        val params = readElectionParameters("/home/stormy/dev/github/electionguard-rust/working/public/election_parameters.json")
        println("ElectionParameters = $params")
    }

    private fun readElectionParameters(filename: String): Result<ElectionParameters, String> =
        try {
            var electionParams: ElectionParameters
            val path = Path.of(filename)
            fileSystemProvider.newInputStream(path).use { inp ->
                val json = jsonReader.decodeFromStream<ElectionParametersJsonR>(inp)
                electionParams = json.import()
            }
            Ok(electionParams)
        } catch (e: Exception) {
            e.printStackTrace()
            Err(e.message ?: "readElectionParameters $filename error")
        }
}