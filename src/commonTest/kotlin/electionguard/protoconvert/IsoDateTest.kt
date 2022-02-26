package electionguard.protoconvert

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.UtcOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class IsoDateTest {

    @Test
    fun parseOffsetDateTime() {
        val datestr = "2020-03-01T08:00:00-05:00"
        // val date = LocalDateTime.parse(datestr) // fails

        // val utcDate: UtcOffset = UtcOffset.parse(datestr) // fails

        // assertEquals(date.toString(), datestr)
    }
}