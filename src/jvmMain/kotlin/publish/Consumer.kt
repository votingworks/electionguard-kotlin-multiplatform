package publish

import electionguard.ballot.ElectionRecord
import electionguard.ballot.ElectionRecordAllData
import electionguard.core.GroupContext
import electionguard.protoconvert.importElectionRecord
import pbandk.decodeFromStream
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class Consumer(val publisher: Publisher, val groupContext : GroupContext) {

    companion object {
        fun fromElectionRecord(electionRecordDir: String, groupContext : GroupContext): Consumer {
            return Consumer(Publisher(Path.of(electionRecordDir), Publisher.Mode.readonly), groupContext)
        }
    }

    @Throws(IOException::class)
    fun readElectionRecord(): ElectionRecordAllData {
        val electionRecord: ElectionRecord
        if (Files.exists(publisher.electionRecordProtoPath())) {
            electionRecord = readElectionRecordProto()
        } else {
            throw FileNotFoundException(
                java.lang.String.format(
                    "No election record found in %s",
                    publisher.electionRecordProtoPath()
                )
            )
        }

        return ElectionRecordAllData(electionRecord, null, null)
    }

    @Throws(IOException::class)
    fun readElectionRecordProto(): ElectionRecord {
        var proto : electionguard.protogen.ElectionRecord
        val filename = publisher.electionRecordProtoPath().toString()
        FileInputStream(filename).use { inp -> proto = electionguard.protogen.ElectionRecord.decodeFromStream(inp) }
        return proto.importElectionRecord(groupContext)
    }

}