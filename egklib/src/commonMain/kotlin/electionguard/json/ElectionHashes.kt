package electionguard.json

import electionguard.core.UInt256
import electionguard.util.ErrorMessages
import kotlinx.serialization.Serializable

/* hashes.json
{
  "h_p": "H(BAD5EEBFE2C98C9031BA8C36E7E4FB76DAC20665FD3621DF33F3F666BEC9AC0D)",
  "h_m": "H(2FE7EA3C2E3C42F88647B4727254F960F1BB7B0D00A6A60C21D2F8984F5090B7)",
  "h_b": "H(3800F226CB90A422D293BDC0F35D3C6767C2945D8C957F29CD0EAC2EAC6394B1)"
}
 */

@Serializable
data class ElectionHashesJsonR(
    val h_p : UInt256JsonR,
    val h_m : UInt256JsonR,
    val h_b : UInt256JsonR,
)

data class ElectionHashes(
    val Hp : UInt256,
    val Hm : UInt256,
    val Hb : UInt256,
)

fun ElectionHashesJsonR.import(errs : ErrorMessages) : ElectionHashes? {
    val h_p = this.h_p.import() ?: errs.addNull("malformed h_p") as UInt256?
    val h_m = this.h_m.import() ?: errs.addNull("malformed h_m") as UInt256?
    val h_b = this.h_b.import() ?: errs.addNull("malformed h_b") as UInt256?
    return if (errs.hasErrors()) null else
        ElectionHashes(h_p!!, h_m!!, h_b!!)
}

/* hashes_ext.json
{
  "h_e": "H(B1321998DB34667B3BD17021665D38274510BE53975496DA2EF3CBB38E799869)"
}
 */

@Serializable
data class ElectionHashesExtJsonR(
    val h_e : UInt256JsonR,
)

fun ElectionHashesExtJsonR.import() = this.h_e.import()