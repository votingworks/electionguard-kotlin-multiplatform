package electionguard.json

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.unwrap
import electionguard.core.productionGroup
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.junit.jupiter.api.Test
import java.nio.file.FileSystems
import java.nio.file.Path
import kotlin.test.assertTrue

class ElectionManifestTest {
    val topdir = "src/commonTest/data/testElectionRecord/egrust"

    var fileSystem = FileSystems.getDefault()
    var fileSystemProvider = fileSystem.provider()
    val group = productionGroup()
    val jsonReader = Json { explicitNulls = false; ignoreUnknownKeys = true }

    @Test
    fun test() {
        val manifest = readElectionManifest("$topdir/public/election_manifest_pretty.json")
        assertTrue(manifest is Ok)
        println("ElectionManifest = ${manifest.unwrap()}")
    }

    private fun readElectionManifest(filename: String): Result<ElectionManifestJsonR, String> =
        try {
            var manifest: ElectionManifestJsonR
            val path = Path.of(filename)
            fileSystemProvider.newInputStream(path).use { inp ->
                manifest = jsonReader.decodeFromStream<ElectionManifestJsonR>(inp)
            }
            Ok(manifest)
        } catch (e: Exception) {
            e.printStackTrace()
            Err(e.message ?: "readElectionManifest $filename error")
        }
}