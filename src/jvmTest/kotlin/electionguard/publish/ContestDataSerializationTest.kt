package electionguard.publish

import electionguard.protogen.ContestData
import org.junit.jupiter.api.Test
import pbandk.decodeFromByteBuffer
import pbandk.encodeToStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.test.assertEquals

/*
message ContestData {
  enum Vote {
    normal = 0;
    null_vote = 1;
    under_vote = 2;
  }
  Vote vote = 1;
  repeated uint32 over_votes = 2;  // list of selection sequence_number for this contest
  repeated fixed32 write_ins = 3; // list of write_in string hashcodes
}
 */
class ContestDataSerializationTest {

    @Test
    fun serializeContestData() {
        println("\nbytes  ContestData")
        doOne( ContestDataPojo(listOf(), listOf()))
        doOne( ContestDataPojo(listOf(), listOf(), ContestData.Status.NULL_VOTE))
        doOne( ContestDataPojo(listOf(), listOf(), ContestData.Status.UNDER_VOTE))

        doOne( ContestDataPojo(listOf(1,2,3), listOf()))
        doOne( ContestDataPojo(listOf(1,2,3,4), listOf()))
        doOne( ContestDataPojo(listOf(111,211,311), listOf()))
        doOne( ContestDataPojo(listOf(111,211,311,411), listOf()))
        doOne( ContestDataPojo(listOf(111,211,311,411, 511), listOf()))

        doOne( ContestDataPojo(listOf(1,2,3,4), listOf(1,2,3,4)))
        doOne( ContestDataPojo(listOf(1,2,3,4), listOf(100,200,300,400)))
        doOne( ContestDataPojo(listOf(1,2,3,4), listOf(1000,2000,3000,4000)))
        doOne( ContestDataPojo(listOf(1,2,3,4), listOf(10000,20000,30000,40000)))
        doOne( ContestDataPojo(listOf(1,2,3,4), listOf(100000,200000,300000,400000)))
        doOne( ContestDataPojo(listOf(1,2,3,4), listOf(1000000,2000000,3000000,4000000)))
        doOne( ContestDataPojo(listOf(1,2,3,4), listOf(1000000,2000000,3000000,4000000,5000000)))
        doOne( ContestDataPojo(listOf(1,2,3,4), listOf(1000000,-2000000,3000000,4000000,5000000)))

        doOne( ContestDataPojo(listOf(1,2,3,4), listOf(
            "1000000".hashCode(),
            "a string".hashCode(),
            "a long string ".hashCode(),
            "a longer longer longer string".hashCode(),
            "01234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789".hashCode(),
        )))
        println()
    }

    fun doOne(pojo : ContestDataPojo) {
        val proto = pojo.publish()
        val bb = serialize(proto)
        val protoRoundtrip = deserialize(bb)
        val roundtrip = protoRoundtrip.import()

        assertEquals( roundtrip, pojo)
        println("${bb.limit()} = $roundtrip")
    }

    fun ContestDataPojo.publish(): electionguard.protogen.ContestData {
        return electionguard.protogen
            .ContestData(
                this.voteEnum,
                this.overvotes,
                listOf("fake"), // this.writeIns,
            )
    }

    fun electionguard.protogen.ContestData.import(): ContestDataPojo {
        return ContestDataPojo(
                this.overVotes,
             listOf(), // this.writeIns,
             this.vote,
            )
    }

    fun serialize(proto: pbandk.Message) : ByteBuffer {
        val bb = ByteArrayOutputStream()
        proto.encodeToStream(bb)
        return ByteBuffer.wrap(bb.toByteArray())
    }

   fun deserialize(buffer: ByteBuffer) : electionguard.protogen.ContestData {
       return electionguard.protogen.ContestData.decodeFromByteBuffer(buffer)
    }

}

data class ContestDataPojo (
    val overvotes: List<Int>,
    val writeIns: List<Int>,
    val voteEnum: ContestData.Status = ContestData.Status.NORMAL,
)

/*
// strawman
message ContestData {
  enum Vote {
    normal = 0;
    null_vote = 1;
    under_vote = 2;
  }
  Vote vote = 1;
  repeated uint32 over_votes = 2;  // list of selection sequence_number for this contest
  repeated sint32 write_ins = 3; // list of write_in ids
}

bytes  ContestData
0 = ContestDataPojo(overvotes=[], writeIns=[], voteEnum=ContestData.Vote.normal(value=0))
2 = ContestDataPojo(overvotes=[], writeIns=[], voteEnum=ContestData.Vote.null_vote(value=1))
2 = ContestDataPojo(overvotes=[], writeIns=[], voteEnum=ContestData.Vote.under_vote(value=2))
5 = ContestDataPojo(overvotes=[1, 2, 3], writeIns=[], voteEnum=ContestData.Vote.normal(value=0))
6 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[], voteEnum=ContestData.Vote.normal(value=0))
7 = ContestDataPojo(overvotes=[111, 211, 311], writeIns=[], voteEnum=ContestData.Vote.normal(value=0))
9 = ContestDataPojo(overvotes=[111, 211, 311, 411], writeIns=[], voteEnum=ContestData.Vote.normal(value=0))
11 = ContestDataPojo(overvotes=[111, 211, 311, 411, 511], writeIns=[], voteEnum=ContestData.Vote.normal(value=0))
12 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[1, 2, 3, 4], voteEnum=ContestData.Vote.normal(value=0))
16 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[100, 200, 300, 400], voteEnum=ContestData.Vote.normal(value=0))
16 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[1000, 2000, 3000, 4000], voteEnum=ContestData.Vote.normal(value=0))
20 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[10000, 20000, 30000, 40000], voteEnum=ContestData.Vote.normal(value=0))
20 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[100000, 200000, 300000, 400000], voteEnum=ContestData.Vote.normal(value=0))
23 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[1000000, 2000000, 3000000, 4000000], voteEnum=ContestData.Vote.normal(value=0))
27 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[1000000, 2000000, 3000000, 4000000, 5000000], voteEnum=ContestData.Vote.normal(value=0))
27 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[1000000, -2000000, 3000000, 4000000, 5000000], voteEnum=ContestData.Vote.normal(value=0))
33 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[1958013297, -1007761232, 1587084778, 513679497, -865692041], voteEnum=ContestData.Vote.normal(value=0))

  repeated fixed32 write_ins = 3; // list of write_in ids

bytes  ContestData
0 = ContestDataPojo(overvotes=[], writeIns=[], voteEnum=ContestData.Vote.normal(value=0))
2 = ContestDataPojo(overvotes=[], writeIns=[], voteEnum=ContestData.Vote.null_vote(value=1))
2 = ContestDataPojo(overvotes=[], writeIns=[], voteEnum=ContestData.Vote.under_vote(value=2))
5 = ContestDataPojo(overvotes=[1, 2, 3], writeIns=[], voteEnum=ContestData.Vote.normal(value=0))
6 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[], voteEnum=ContestData.Vote.normal(value=0))
7 = ContestDataPojo(overvotes=[111, 211, 311], writeIns=[], voteEnum=ContestData.Vote.normal(value=0))
9 = ContestDataPojo(overvotes=[111, 211, 311, 411], writeIns=[], voteEnum=ContestData.Vote.normal(value=0))
11 = ContestDataPojo(overvotes=[111, 211, 311, 411, 511], writeIns=[], voteEnum=ContestData.Vote.normal(value=0))
24 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[1, 2, 3, 4], voteEnum=ContestData.Vote.normal(value=0))
24 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[100, 200, 300, 400], voteEnum=ContestData.Vote.normal(value=0))
24 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[1000, 2000, 3000, 4000], voteEnum=ContestData.Vote.normal(value=0))
24 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[10000, 20000, 30000, 40000], voteEnum=ContestData.Vote.normal(value=0))
24 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[100000, 200000, 300000, 400000], voteEnum=ContestData.Vote.normal(value=0))
24 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[1000000, 2000000, 3000000, 4000000], voteEnum=ContestData.Vote.normal(value=0))
28 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[1000000, 2000000, 3000000, 4000000, 5000000], voteEnum=ContestData.Vote.normal(value=0))
28 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[1000000, -2000000, 3000000, 4000000, 5000000], voteEnum=ContestData.Vote.normal(value=0))
28 = ContestDataPojo(overvotes=[1, 2, 3, 4], writeIns=[1958013297, -1007761232, 1587084778, 513679497, -865692041], voteEnum=ContestData.Vote.normal(value=0))
 */

/*
  repeated uint32 write_ins = 4; // list of write_in ids

bytes  ContestData
0 = ContestDataPojo(overvotes=[], underVote=false, nullVote=false, writeIns=[])
6 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[])
12 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1, 2, 3, 4])
14 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=true, nullVote=false, writeIns=[1, 2, 3, 4])
16 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=true, nullVote=true, writeIns=[1, 2, 3, 4])
15 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[100, 200, 300, 400])
16 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1000, 2000, 3000, 4000])
19 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[10000, 20000, 30000, 40000])
20 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[100000, 200000, 300000, 400000])
22 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1000000, 2000000, 3000000, 4000000])
26 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1000000, 2000000, 3000000, 4000000, 5000000])
28 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1000000, -2000000, 3000000, 4000000, 5000000])
33 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1958013297, -1007761232, 1587084778, 513679497, -865692041])

  repeated sint32 write_ins = 4; // list of write_in ids

bytes  ContestData
0 = ContestDataPojo(overvotes=[], underVote=false, nullVote=false, writeIns=[])
6 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[])
12 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1, 2, 3, 4])
14 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=true, nullVote=false, writeIns=[1, 2, 3, 4])
16 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=true, nullVote=true, writeIns=[1, 2, 3, 4])
16 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[100, 200, 300, 400])
16 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1000, 2000, 3000, 4000])
20 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[10000, 20000, 30000, 40000])
20 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[100000, 200000, 300000, 400000])
23 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1000000, 2000000, 3000000, 4000000])
27 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1000000, 2000000, 3000000, 4000000, 5000000])
27 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1000000, -2000000, 3000000, 4000000, 5000000])
33 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1958013297, -1007761232, 1587084778, 513679497, -865692041])

  repeated int32 write_ins = 4; // list of write_in ids

bytes  ContestData
0 = ContestDataPojo(overvotes=[], underVote=false, nullVote=false, writeIns=[])
6 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[])
12 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1, 2, 3, 4])
14 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=true, nullVote=false, writeIns=[1, 2, 3, 4])
16 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=true, nullVote=true, writeIns=[1, 2, 3, 4])
15 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[100, 200, 300, 400])
16 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1000, 2000, 3000, 4000])
19 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[10000, 20000, 30000, 40000])
20 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[100000, 200000, 300000, 400000])
22 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1000000, 2000000, 3000000, 4000000])
26 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1000000, 2000000, 3000000, 4000000, 5000000])
33 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1000000, -2000000, 3000000, 4000000, 5000000])
43 = ContestDataPojo(overvotes=[1, 2, 3, 4], underVote=false, nullVote=false, writeIns=[1958013297, -1007761232, 1587084778, 513679497, -865692041])

 */